library(ngram)
library(tm)
library(readr)

# read uid from the first command line argument
args = commandArgs(trailingOnly=TRUE)
uid <- args[1]

# read in the stopwords
sw <- read_lines("src/stopwords.txt")
# read in the csv file
weibo <- read_csv(paste("WeiboT/BT_", uid, ".csv", sep=""))
# get the month value for each post
weibo$Month <- as.integer(format(as.Date(weibo$Date, format = "%Y-%m-%d"), "%Y%m"))
# remove any non-english character
weibo$Post_Translated <- tolower(iconv(weibo$Post_Translated, "UTF-8", "latin1", sub = ""))
# remove any web link
weibo$Post_Translated <- gsub('http\\S+\\s*', "", weibo$Post_Translated)
# remove the stopwords
weibo$Post_NoStopWords <- removeWords(weibo$Post_Translated, sw)
# remove punctiation
weibo$Post_NoStopWords <- removePunctuation(weibo$Post_NoStopWords)
# substitude any extra spaces with a single space
weibo$Post_NoStopWords <- gsub("\\s+", " ", weibo$Post_NoStopWords)

# get posts data group by month
weibo_monthly <- aggregate(Post_NoStopWords ~ Month, data = weibo, FUN = function(x) paste(x, collapse=""))
weibo_monthly$wordcount_nostop <- lapply(weibo_monthly$Post_NoStopWords, function(x) wordcount(x))
weibo_monthly$wordcount_nostop <- as.integer(weibo_monthly$wordcount_nostop)
weibo_monthly$Post_Translated <- aggregate(Post_Translated ~ Month, data = weibo, FUN = function(x) paste(x, collapse=""))$Post_Translated
weibo_monthly$wordcount <- lapply(weibo_monthly$Post_Translated, function(x) wordcount(x))
weibo_monthly$wordcount <- as.integer(weibo_monthly$wordcount)
weibo_monthly$numpost <- aggregate(Num ~ Month, data = weibo, FUN = length)$Num
# compute word freqency and print out top ten
weibo_monthly$ngram <- lapply(weibo_monthly$Post_NoStopWords, function(x) if(wordcount(x) > 0) {ngram(x, n = 1)} else {NA})

for(r in 1:nrow(weibo_monthly)) {
  x <- weibo_monthly[r,]
  cat(format(as.Date(paste(as.character(x$Month), "01"), format = "%Y%m%d"), "%B %Y"))
  cat(" for Weibo account ")
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
mallet.instances <- mallet.import(as.character(weibo$Num), weibo$Post_Translated, "src/stopwords.txt", token.regexp = "\\p{L}[\\p{L}\\p{P}]+\\p{L}")

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