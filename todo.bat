@ECHO OFF
SET currentDir=%~dp0
java -jar %currentDir%/target/cmdtodos-0.1.0.jar %*
