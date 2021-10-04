@echo off

SET "exe=..\..\build_module\target\dannaco_irace.jar"

java -Xmx8192m -jar %exe% %*



