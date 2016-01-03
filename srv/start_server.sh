
BBAPPROOT=../app/src/main/java

javac  Server.java  $BBAPPROOT/org/bobstuff/bobball/Network/*.java
java -classpath $BBAPPROOT:. Server  $1
#java -classpath $BBAPPROOT:. Server 1235

#redir add tcp:8477:1234
#redir add tcp:1234:8477
