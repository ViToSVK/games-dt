#!/bin/bash
JVM_OPTS="-ea -Xmx8g"
CLASS_PATH="-cp out/production/games-dt:lib/javabdd-1.0b2.jar"
MAIN="main.Main"

java $JVM_OPTS $CLASS_PATH $MAIN $1
