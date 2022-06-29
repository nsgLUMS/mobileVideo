#!/bin/bash

# set to exit script on first error
set -e

# open firefox
adb shell input tap 405 990
sleep 10
adb shell input tap 450 1688
sleep 5
adb shell input tap 460 1022
sleep 1
adb shell input keyboard text "http://$1/myindex_robustMPC.html"
sleep 2
