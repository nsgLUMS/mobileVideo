#!/bin/bash

# set to exit script on first error
set -e

adb shell input tap 150 977
sleep 2
adb shell input keyboard text "1"
sleep 2
adb shell input keyevent "KEYCODE_BACK"
sleep 2
adb shell input tap 513 1420
sleep 2