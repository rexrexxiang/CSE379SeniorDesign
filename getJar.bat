mkdir bin
javac -cp lib/commons-csv-1.5.jar;lib/commons-lang3-3.6.jar;lib/commons-text-1.1.jar;lib/gson-2.8.2.jar -d bin src/*.java
cd bin
jar cvfm ../app.jar ../src/app.mf *.class
cd ..
rd /s /q bin