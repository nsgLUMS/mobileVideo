#!/bin/bash

# set to exit script on first error
set -e

# open firefox
adb shell input tap 435 1537
sleep 10
adb shell input tap 610 2308
sleep 10
adb shell input tap 425 1309
sleep 1
adb shell input keyboard text "http://$1/myindex_robustMPC.html"
sleep 2
