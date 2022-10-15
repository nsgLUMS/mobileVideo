mkdir dubai_1080xfps
for i in {0..3}
do
   mkdir dubai_1080xfps/$i
done

ffmpeg -i dubai.mkv -map 0:v:0  -map 0:v:0 -map 0:v:0 -map 0:v:0 \
-b:v:0  8000k  -minrate:v:0 8000k  -maxrate:v:0 8000k   -bufsize:v:0 4000k -s:v:0 1920x1080   -g:v:0 48 -x264opts:v:0 no-scenecut -filter:v:0  "scale=1980:1080" -filter:v:0 "fps=fps=24" -profile:v:0 high \
-b:v:1  8000k  -minrate:v:1 8000k  -maxrate:v:1 8000k   -bufsize:v:1 4000k -s:v:1 1920x1080   -g:v:1 60 -x264opts:v:1 no-scenecut -filter:v:1  "scale=1980:1080" -filter:v:1 "fps=fps=30" -profile:v:1 high \
-b:v:2  12000k  -minrate:v:2 12000k  -maxrate:v:2 12000k   -bufsize:v:2 6000k -s:v:2 1920x1080   -g:v:2 96 -x264opts:v:2 no-scenecut -filter:v:2  "scale=1980:1080" -filter:v:2 "fps=fps=48" -profile:v:2 high \
-b:v:3  12000k  -minrate:v:3 12000k  -maxrate:v:3 12000k   -bufsize:v:3 6000k -s:v:3 1920x1080   -g:v:3 120 -x264opts:v:3 no-scenecut -filter:v:3  "scale=1980:1080" -filter:v:3 "fps=fps=60" -profile:v:3 high \
-f dash -seg_duration 4   \
-init_seg_name 'dubai_1080xfps/$RepresentationID$/init-stream.mp4' \
-media_seg_name 'dubai_1080xfps/$RepresentationID$/$Number%05d$.m4s' \
-adaptation_sets "id=0,streams=v" Manifest_1080xfps.mpd