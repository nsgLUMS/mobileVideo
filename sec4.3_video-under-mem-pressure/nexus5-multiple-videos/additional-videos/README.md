# Instructions to prepare additional videos for the experiment

## Download and prepare video files

1. Download the following videos from YouTube.
    1. Travel/Vlog: [`dubai.mkv`](https://youtu.be/BLL-kW_TpT4)
    1. Sports:      [`tennis.mkv`](https://youtu.be/lnoba3DZQZw)
    1. Gaming:      [`gaming.mkv`](https://youtu.be/Ek-gfQo6ryE)
    1. News:        [`news.mkv`](https://youtu.be/RIw7smlkIaU)
    1. Nature:      [`bali.mkv`](https://youtu.be/fajeL728XG8)

1. Place the downloaded video files in this folder and rename them to the names mentioned above. E.g. Nature video should be named `bali.mkv`.

1. Trim videos to be of max 3 minutes by running the following:
    ```
    ffmpeg -ss 00:00:00 -to 00:03:00  -i [video_name] -c copy [video_name]
    ```
    e.g.
    ```
    ffmpeg -ss 00:00:00 -to 00:03:00  -i bali.mkv -c copy bali.mkv
    ```

## Convert videos to DASH

Create DASH chunks and their manifest files through the provided scripts. Usage:
```
bash generate-dash-vids-30fps.sh [video_name_without_extension]
bash generate-dash-vids-60fps.sh [video_name_without_extension]
```
e.g.
```
bash generate-dash-vids-30fps.sh bali
bash generate-dash-vids-60fps.sh bali
```

## Host all video chunks on the server

Copy all chunk and manifest folders to the Apache2 server public file location:
```
sudo cp -r *_30fps /var/www/html/
sudo cp -r *_60fps /var/www/html/
sudo cp -r manifest-* /var/www/html/
```