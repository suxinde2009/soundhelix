rem start script
@echo off
if "%1" == "" java -jar SoundHelix.jar examples\SoundHelix-Popcorn.xml
if not "%1" == "" java -jar SoundHelix.jar %*
pause
