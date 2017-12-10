#!/usr/bin/env python
# encoding: utf-8

import tweepy #https://github.com/tweepy/tweepy
import csv
import time
from urllib import urlopen
import os
import sys

#Twitter API credentials
consumer_key = "Enter Here"
consumer_secret = "Enter Here"
access_key = "Enter Here"
access_secret = "Enter Here"

#class for each post
class posts:
    def __init__(self):
        self.type = "" 
        self.comment_id = "" 
        self.date = ""
        self.content = ""
        self.image = 0
        self.video = 0
        self.link = 0
        self.mention = 0
        self.mention_list = []
        self.hashtag = 0
        self.hashtag_list = []
        self.like = 0
        self.retweet = 0
    
#class for each user
class user:
    def __init__(self):
        self.user_screen_name = ""
        self.user_name = "" 
        self.user_id = "" 
        self.user_description = ""
        self.user_location = ""
        self.user_verified = ""
        self.user_create = ""
        self.likes = 0
        self.video = 0
        self.photo = 0
        self.total_posts = 0
        self.followers = 0
        self.followers_list = []
        self.followings = 0
        self.followings_list = []
        self.list_member = 0
        self.list_subscription = 0
        self.posts = []
    
#get information for each user
def get_user_info(current, screen_name):
    auth = tweepy.OAuthHandler(consumer_key, consumer_secret)
    auth.set_access_token(access_key, access_secret)
    api = tweepy.API(auth, wait_on_rate_limit=True, wait_on_rate_limit_notify=True, compression=True)
    
    followers = []
    followings = []
    likes = []
    
    user = api.get_user(screen_name = screen_name)
    user_name = user.name
    user_id = user.id
    user_description = user.description
    user_location = user.location
    user_verified = user.verified
    user_create = user.created_at
    
    #print user
    current.user_name = user_name
    current.user_screen_name = screen_name
    current.user_id = user_id
    current.user_description = user_description
    current.user_location = user_location
    current.user_verified = user_verified
    current.user_create = user_create
    
    for page in tweepy.Cursor(api.followers_ids, screen_name=screen_name,count=200).pages():
        followers.extend(page)
    current.followers = len(followers)
    current.followers_list = followers

    for page in tweepy.Cursor(api.friends_ids, screen_name=screen_name,count=200).pages():
        followings.extend(page)
    current.followings = len(followings)
    current.followings_list = followings
    

    list_member = api.lists_memberships(screen_name = screen_name)
    current.list_member = len(list_member)
    
    list_subscription = api.lists_all(screen_name = screen_name)
    current.list_subscription = len(list_subscription)
        
    for page in tweepy.Cursor(api.favorites, screen_name=screen_name, count = 200).pages():
        likes.extend(page)
    current.likes = len(likes)
    
# function for get infomation of each tweet
def get_all_tweets(user):
	#Twitter only allows access to a users most recent 3240 tweets with this method
	
	#authorize twitter, initialize tweepy
    auth = tweepy.OAuthHandler(consumer_key, consumer_secret)
    auth.set_access_token(access_key, access_secret)
    api = tweepy.API(auth, wait_on_rate_limit=True, wait_on_rate_limit_notify=True, compression=True)
	
    screen_name = user.user_screen_name
	#make initial request for most recent tweets (200 is the maximum allowed count)
    for item in tweepy.Cursor(api.user_timeline, screen_name=screen_name, count = 200, tweet_mode="extended").items():
        user.total_posts = user.total_posts + 1
        post = posts()

        if item.in_reply_to_status_id is not None:  
            post.type = "Seed"
            post.comment_id = item.in_reply_to_status_id_str
        else:
            post.type = "Original"
        
        post.content = item.full_text
        post.date = item.created_at
        
        if hasattr(item, 'extended_entities'):
            for value in item.extended_entities["media"]: 
                if value["type"] == "photo":
                    user.photo = user.photo + 1
                    post.image = post.image + 1
                elif value["type"] == "video":
                    user.video = user.video + 1
                    post.video = post.video + 1
                    
        post.link = post.link + len(item.entities["urls"])
        post.mention = post.mention + len(item.entities["user_mentions"])
        if len(item.entities["user_mentions"]) > 0:
            for each in item.entities["user_mentions"]:
                post.mention_list.append(str(each["id"]))
                
        post.hashtag = post.hashtag + len(item.entities["hashtags"])
        if len(item.entities["hashtags"]) > 0:
            for each in item.entities["hashtags"]:
                post.hashtag_list.append(str(each['text'].encode('utf-8')))
        
        if item.favorite_count is not None: 
            post.like = item.favorite_count
        else:
            post.like = item.favorite_count
        
        post.retweet = item.retweet_count
        
        user.posts.append(post)        

# write output
def write_output(user):
    flag = 0
    with open("Twitter/User_info.csv", "rb") as f:
        csvreader = csv.DictReader(f, delimiter=",")
        for row in csvreader:
            if str(user.user_id) == row["user_id"]:
                flag = 1;
                
    if flag == 0:
        with open("Twitter/User_info.csv", "a") as csvfile:
            fieldnames = ['user_screen_name', 'user_name', 'user_id', 'user_description', 'user_location', 'user_verified', 'user_create', 'total_post', 'num_video', 'num_photo', 'followers', 'followers_list', 'followings', 'followings_list', 'list_member', 'list_subscription', 'likes']
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writerow({'user_screen_name': user.user_screen_name, 'user_name': user.user_name, 'user_id': str(user.user_id),'user_description': str(user.user_description.encode('utf-8')),'user_location': str(user.user_location.encode('utf-8')),'user_verified': user.user_verified,'user_create': user.user_create, 'total_post': str(user.total_posts), 'num_video': str(user.video), 'num_photo': str(user.photo), 'followers': user.followers, 'followers_list': user.followers_list, 'followings': str(user.followings),'followings_list': user.followings_list,'list_member': str(user.list_member),'list_subscription': user.list_subscription,'likes': user.likes})

    with open("Twitter/%s.csv"%user.user_id, "wb") as csvfile:
        fieldnames = ["num", "content", "date", "type", "comment_id", "num_image", "num_video", "num_link", "num_mention", "mention_list", "num_hashtag", "hashtag_list", "num_like", "num_retweet"]
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        i = 0
        for post in user.posts:
            writer.writerow({'num': str(i), 'content': str(post.content.encode('utf-8')), 'date': str(post.date),'type': str(post.type.encode('utf-8')),'comment_id': str(post.comment_id.encode('utf-8')), 'num_image': str(post.image),'num_video': str(post.video), 'num_link': str(post.link), 'num_mention': str(post.mention), 'mention_list': str(post.mention_list), 'num_hashtag': str(post.hashtag),'hashtag_list': str(post.hashtag_list),'num_like': str(post.like),'num_retweet': str(post.retweet)})
            i = i + 1
        
# main function
if __name__ == '__main__':
    if not os.path.exists("Twitter"):
        os.makedirs("Twitter")
    
    with open("Twitter/User_info.csv", "wb") as csvfile:
        fieldnames = ['user_screen_name', 'user_name', 'user_id', 'user_description', 'user_location', 'user_verified', 'user_create', 'total_post', 'num_video', 'num_photo', 'followers', 'followers_list', 'followings', 'followings_list', 'list_member', 'list_subscription', 'likes']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
	
    name_list = []
    #pass in the username of the account you want to download
    f = open(sys.argv[1],"r")
    for line in f:
        tmp = line.split("/")
        name = tmp[len(tmp) - 1]
        name_list.append(name)
        
    i = 1;
    for item in name_list:
        try:
            users = user()
            users.user_screen_name = item
            print "    Current:" + str(i) + " user: " + item
            sys.stdout.flush()
            i = i + 1
            get_user_info(users,item)
            get_all_tweets(users)
            write_output(users)
        except Exception,e:
            print "Error: ",e
            sys.stdout.flush()