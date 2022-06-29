#!/bin/bash

npm install
sudo ufw enable
sudo ufw allow 4333
sudo ufw allow 4334
sudo ufw allow 4335

npm run start
xdg-open http://localhost:4333
