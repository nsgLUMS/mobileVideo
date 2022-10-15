# Memory Pressure Setup

This folder contains two applications used to apply/control memory pressure on mobile devices.

## MP-Simulator

This is a modified version of MP Simulator--an Android app developed in a recent work---that synthetically applies memory pressure on an Android device.

### Usage instructions

Open `./MP-Simulator` as a project in Android Studio. Connect the mobile device to your PC and run the application. Follow the application's intuitive UI to apply memory pressure.

## Node Memory Pressure Server

The second is a Node.js web application that can be used to remotely interact with MP Simulator using both an interactive Web UI and REST API calls.

### Usage instructions

To setup the server, open a terminal inside `./node-memory-pressure-server`, and run:
```
npm install
sudo ufw enable
sudo ufw allow 4333
sudo ufw allow 4334
sudo ufw allow 4335
```
After this, you can start the web server by running:
```
npm run start
xdg-open http://localhost:4333
```
