@echo off

SET "exe=..\..\optimization_environment_evaluation\target\OptimizationEnvironmentEvaluation-1.0-SNAPSHOT-jar-with-dependencies.jar"

java -Xmx8192m -jar %exe% %*



