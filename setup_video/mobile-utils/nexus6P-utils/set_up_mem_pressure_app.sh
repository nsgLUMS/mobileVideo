#!/bin/bash

# set to exit script on first error
set -e

adb shell input tap 159 1534
sleep 2
adb shell input keyboard text $1
sleep 2
adb shell input keyevent "KEYCODE_BACK"
sleep 2
adb shell input tap 659 1684
sleep 2