#!/bin/bash

# set to exit script on first error
set -e

# adb shell input keyevent "KEYCODE_POWER"
# sleep 1
adb shell input swipe 600 1700 600 1000
sleep 2