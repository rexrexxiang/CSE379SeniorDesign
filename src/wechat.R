library(ngram)
library(tm)
library(readr)

# read uid from the first command line argument
args = commandArgs(trailingOnly=TRUE)
uid <- args[1]

# read in the stopwords
sw <- read_lines("src/stopwords.txt")
# read in the csv file
wechat <- read_csv(paste("WeChatT/CT_", uid, ".csv", sep=""))
# get the month value for each post
wechat$Month <- as.integer(format(as.Date(wechat$postTime, format = "%Y/%m/%d"), "%Y%m"))
# remove any non-english character
wechat$contentTranslated <- tolower(iconv(wechat$contentTranslated, "UTF-8", "latin1", sub = ""))
# remove any web link
wechat$contentTranslated <- gsub("http\\S+\\s*", "", wechat$contentTranslated)
# remove the stopwords
wechat$content_nostop <- removeWords(wechat$contentTranslated, sw)
# remove punctiation
wechat$content_nostop <- removePunctuation(wechat$content_nostop)
# substitude any extra spaces with a single space
wechat$content_nostop <- gsub("\\s+", " ", wechat$content_nostop)

# get posts data group by month
wechat_monthly <- aggregate(content_nostop ~ Month, data = wechat, FUN = function(x) paste(x, collapse=""))
wechat_monthly$wordcount_nostop <- lapply(wechat_monthly$content_nostop, function(x) wordcount(x))
wechat_monthly$wordcount_nostop <- as.integer(wechat_monthly$wordcount_nostop)
wechat_monthly$contentTranslated <- aggregate(contentTranslated ~ Month, data = wechat, FUN = function(x) paste(x, collapse=""))$contentTranslated
wechat_monthly$wordcount <- lapply(wechat_monthly$contentTranslated, function(x) wordcount(x))
wechat_monthly$wordcount <- as.integer(wechat_monthly$wordcount)
wechat_monthly$numpost <- aggregate(id ~ Month, data = wechat, FUN = length)$id
# compute word freqency and print out top ten
wechat_monthly$ngram <- lapply(wechat_monthly$content_nostop, function(x) if(wordcount(x) > 0) {ngram(x, n = 1)} else {NA})

for(r in 1:nrow(wechat_monthly)) {
  x <- wechat_monthly[r,]
  cat(format(as.Date(paste(as.character(x$Month), "01"), format = "%Y%m%d"), "%B %Y"))
  cat(" for wechat account ")
  cat(uid)
  cat("\nTotal posts in month = ")
  cat(x$numpost)
  cat("\nAverage word count per post (w/ stopwords) = ")
  cat(x$wordcount / x$numpost)
  cat("\nAverage word count per post (w/o stopwords) = ")
  cat(x$wordcount_nostop / x$numpost)
  cat("\n\n")
  if(!is.na(x$ngram)) {
    print(head(get.phrasetable(x$ngram[[1]]), n=10L))
    cat("\n")
  }
}

# LDA topic generation
library(dplyr)
library (rJava)
.jinit()
library(mallet)

numtopics <- 10  # number of topic to generate
label.length = 5 # number of words per topic

# standard mallet setup
mallet.instances <- mallet.import(as.character(wechat$id), wechat$contentTranslated, "src/stopwords.txt", token.regexp = "\\p{L}[\\p{L}\\p{P}]+\\p{L}")

topic.model <- MalletLDA(num.topics=numtopics)

topic.model$loadDocuments(mallet.instances)
topic.model$setAlphaOptimization(20, 50)
topic.model$train(1000)
topic.model$maximize(50)

doc.topics <- mallet.doc.topics(topic.model, smoothed=T, normalized=T)
topic.words <- mallet.topic.words(topic.model, smoothed=T, normalized=T)

# print out the topics
labels <- vector(length=numtopics)
for (topic.i in 1:numtopics) 
{
  labels[topic.i] <- gsub(",", ";", paste(as.vector(mallet.top.words(topic.model, topic.words[topic.i,], label.length)[,1]), collapse="_"))
  labels[topic.i] <- gsub("_", ",", labels[topic.i])
  labels[topic.i] <- gsub("\\s", "_", labels[topic.i])
}
topics.frame <- data.frame(doc.topics)
names(topics.frame) <- labels

means <- sapply(topics.frame, mean)
means <- sort(means, decreasing = TRUE)

cat("proportion\ttopics\n")
for (c in 1:numtopics) {
  cat(means[c]*100)
  cat("%\t")
  cat(names(means)[c])
  cat("\n")
}
quit(save = "no")