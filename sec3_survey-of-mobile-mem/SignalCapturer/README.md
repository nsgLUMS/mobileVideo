# SignalCapturer

## About

This is the source code for SignalCapturer -- an Android application that surveys memory consumption patterns in real-world Android devices.

## Pre-built apk

The pre-built apk file can be found in the following path: `app/release/app-release.apk`. This is the build that was published on the Play Store for users to install.

## Build instructions

To build this app yourself, open `./` in Android Studio.

#### Note:
If you build this app yourself, the app will not be able to upload logs to our server. This is because for privacy and security reasons, the URL to where the logs must be uploaded in `app/src/main/java/com/example/signalcapturer/sendToServer.java` is replaced by an empty string. Please contact the authors for further correspondence.