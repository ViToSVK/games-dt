@echo off
set JVM_OPTS=-d64 -ea -Xmx8g
set CLASS_PATH=-cp bin/;lib/javabdd-1.0b2.jar
set MAIN=main.Main

java %JVM_OPTS% %CLASS_PATH% %MAIN% %*
