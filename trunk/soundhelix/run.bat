@echo off
if "%1" == "" java -jar SoundHelix.jar examples\SoundHelix-Piano.xml
if not "%1" == "" java -jar SoundHelix.jar %*
pause
