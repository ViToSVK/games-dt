#!/bin/bash
JVM_OPTS="-d64 -ea -Xmx8g"
CLASS_PATH="-cp bin/:lib/javabdd-1.0b2.jar"
MAIN="main.Main"

java $JVM_OPTS $CLASS_PATH $MAIN $1