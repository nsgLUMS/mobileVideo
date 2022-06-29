# Video Setup

## Overview

### About

We use [Pensieve](https://github.com/hongzimao/pensieve)'s DASH setup. We have edited it to log frame drop data. 

Here's an overview of how the setup works:

We serve a webpage containing a DASH video player on a LAN through our apache server running on a Linux machine. A mobile phone on the LAN requests this webpage on which our DASH player starts to play the video. Before each video chunk download, Pensive's DASH player sends a request to a Python ABR server running on our machine. This ABR server responds to it with the quality of video chunk the player needs to call next.* Along with this request, our player also sends the current** video playback statistics*** (incl. no. of frames shown/dropped), which are logged at the ABR server.

This approach to logging the video playback statistics at each chunk download is used throughout the study, except in section 4, where we wanted to log video playback statistics more frequently. The  `log-independent-of-chunk-req` directory contains alternate files for the video setup where playback data is logged at a fixed interval and stored at the client side until it is flushed to the ABR server at the next chunk request.

*\* For our study's scope, this ABR server responded with a fixed quality.*
<br>
*\*\* ±4s*
<br>
*\*\*\* These are obtained through the [VideoPlaybackQuality](https://developer.mozilla.org/en-US/docs/Web/API/VideoPlaybackQuality) API.*

### Directory Contents:
1. `client-files`:
    - `dash.all.min.js`: compiled DASH file.
    - `myindex_robustMPC.html`: webpage html file to play the DASH video player.
2. `videos`:
    - `generate-dash-vids-30fps.sh`: converts video to 30fps DASH-compatible video components.
    - `generate-dash-vids-60fps.sh`: converts video to 60fps DASH-compatible video components.
3. `abr-server`:
    - `robust_mpc_server.py`: the ABR server.
4. `log-independent-of-chunk-req`:
    - contains alternate files for the video setup where playback data is logged at a fixed interval and stored at the client side until it is flushed to the ABR server at the next chunk request.

## Installation

### General setup

Install apache server
```
sudo apt-get -y install apache2
```

You may need to allow ports from the firewall
```
sudo ufw enable
sudo ufw allow 80
sudo ufw allow 8333
```

Copy the client files to /var/www/html
```
sudo cp client-files/* /var/www/html/
```

### Generating DASH-compatible video

Enter directory
```
cd videos
```

Download video
```
wget https://www.dropbox.com/s/em7r3wx2ugcpnh6/dubai.mkv
```

Generate the videos:
```
bash generate-dash-vids-30fps.sh
bash generate-dash-vids-60fps.sh
```

Copy videos to /var/www/html:
```
sudo cp dubai_* /var/www/html/
```

Copy mainfests to ABR server:
```
mkdir abr-server/manifest
sudo cp Manifest* abr-server/manifest/
```

(The link to the alternate video used in the study (R2) is [this](https://www.dropbox.com/s/syj4ca9uuvuv4gp/laluna.mkv))

## Usage

Replace localhost with your linux machine's local IP address. e.g. if your IP address is 192.168.1.177, run: 
```
sudo sed 's+localhost+192.168.1.177+g' /var/www/html/dash.all.min.js > /var/www/html/dash.all.min.js
sed 's+localhost+192.168.1.177+g' abr-server/robust_mpc_server.py > abr-server/robust_mpc_server.py
```

Start the ABR server:
```
python2 robust_mpc_server.py [fps] [resolution] [logfile_suffix]
```
E.g., command for running 60fps 480p video:
```
python2 robust_mpc_server.py 60 480
```

Connect the phone you want to play the video on to the same LAN as your linux machine, and go here in the phone's browser:
```
http://<linux_IP_address>/myindex_robustMPC.html
```

The run will be logged in abr-server/results

