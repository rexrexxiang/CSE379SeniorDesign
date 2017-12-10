==== Software Requirement ====
* Java: version 8 or higher
* Python: version 2.7 (do not use version 3.x)
* R: version 3.4.2 or higher

==== Set up the environment ====
After installing the software, you need to set up the environment.
To set up the environment on windows 10, right click on the windows icon on the lower left corner,
select "System", on the left menu, select "Advanced system settings", then click on "Environment Variables...",
under user variables, select PATH, and click "Edit...", click on "New" to add the following paths to it:
* The python path, ex. C:\Python27 (you should find python.exe in this path)
* The pip path, ex. C:\Python27\Scripts (you should find pip.exe in this path)
* The java path, ex. C:\Program Files\Java\jdk1.8.0_151\bin (you should find javac.exe and jar.exe in this path)
* The R path, ex. C:\Program Files\R\R-3.4.2\bin (you should find R.exe and Rscript.exe in this path)
(You will need to open a new command prompt after changing the varibale for it to take effect)

==== Required Libraries ====
* Java: gson, commons-lang, commons-text, commons-csv (included in /lib folder)

* Python: tweepy, request, lxml
(Run these commands in the command prompt to install the libraries)
pip install tweepy
pip install request
pip install lxml

* R: ngram, tm, readr, dplyr, rJava, mallet
(type "R" in the command prompt, press enter, then enter the following lines to install the libraries)
install.packages("ngram")
install.packages("tm")
install.packages("readr")
install.packages("dplyr")
install.packages("rJava")
install.packages("mallet")
(you can enter "q()" to exit the R program)

==== Set up the program ====
double click on getJar.bat to run the script, this script will compile all the Java source code in /src folder to an executable jar, and the intermediate .class file will get deleted.
[!] This script generates the .class files in the /bin folder and the whole folder will get deleted after the jar is created,
do not create a /bin folder and put content inside, as it will get deleted and not recoverable

==== Running the program ====
simply double click on app.jar to run the program
[!] do not move the app.jar to another location, otherwise it will unable to find the libraries and scripts to perform the actions

==== Using the program ====

=== Data collection ===
1. select the checkbox on which platform you want to collect the data from
2. enter the filename (if in the same folder) or the path of file (if in remote folder) which contains the accounts you want to collect from. (Different platform requires different formats, see the included input.txt for example)
3. press "Data Collection" to start collecting
4. the result will get outputted in the Weibo, WeChat, Twitter folder respectively
[!] it is recommended to collect one platform at a time, if multiple platform is selected, they will start at the same time.
[!] the wechat collection uses a paid service, using all of the balence will result in failure of collection
[!] the wechat collection service as a daily limit, collection large amount of accounts might result in failure of collection

=== Translation ===
1. select the checkbox on which platform you want to collect the data from (Weibo and WeChat only, Twitter is ignored if checked)
2. enter the filename (if in the same folder) or the path of file (if in remote folder) which contains the accounts you want to translate the data. (the file format is the same as the data collection process)
3. press "Data Collection" to start collecting
4. the result will get outputted in the WeiboT and WeChatT
[!] it is recommended to translate one platform at a time, if multiple platform is selected, they will start at the same time.
[!] google translate api has a daily quota on how many data can get translated, you can remove this quota in the google developer console

=== Analysis ===
analysis does not take any file as input, it will look at the files in the Twitter, WeiboT, and WeChatT folder and perform analysis on all of them. The result is outputted in the WeiboA, WeChatA, TwitterA folder respectively
[!] The R script generates a lot of information messages through stderr, so it is not displayed on screen but saved in the R.log file. Refer to this file if the output is missing or corrupted. The R.log file will be regenerated everytime the app is opened and holds all R error and info output for that session (until you close the app). Please be aware you cannot recover error messages from the last session once you open a new one.
