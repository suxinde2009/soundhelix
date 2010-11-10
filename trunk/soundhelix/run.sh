#!/bin/bash
if [ "$1" = "" ]; then
  java -jar SoundHelix.jar examples/SoundHelix-Piano.xml
else
  java -jar SoundHelix.jar "$1" "$2"
fi
