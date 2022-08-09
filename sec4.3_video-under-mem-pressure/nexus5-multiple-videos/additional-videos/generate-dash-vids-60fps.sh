# Prepare folder to put DASH chunks
mkdir $1_60fps
for i in {0..5}
do
   mkdir $1_60fps/$i
done

# Make DASH chunks 
ffmpeg -i $1.mkv -map 0:v:0  -map 0:v:0 -map 0:v:0 -map 0:v:0 -map 0:v:0 -map 0:v:0 \
-b:v:0   1000k    -minrate:v:0 1000k    -maxrate:v:0 1000k    -bufsize:v:0  500k  -s:v:0  426x240     -g:v:0  120 -x264opts:v:0  no-scenecut -filter:v:0   "scale=426:240"   -filter:v:0 "fps=fps=60" -profile:v:0 high \
-b:v:1   1500k   -minrate:v:1 1500k   -maxrate:v:1 1500k   -bufsize:v:1  750k  -s:v:1  640x360     -g:v:1  120 -x264opts:v:1  no-scenecut -filter:v:1   "scale=640:360"   -filter:v:1 "fps=fps=60" -profile:v:1 high \
-b:v:2   4000k   -minrate:v:2 4000k   -maxrate:v:2 4000k   -bufsize:v:2 2000k  -s:v:2  854x480     -g:v:2  120 -x264opts:v:2  no-scenecut -filter:v:2   "scale=854:480"   -filter:v:2 "fps=fps=60" -profile:v:2 high \
-b:v:3   7500k   -minrate:v:3 7500k   -maxrate:v:3 7500k   -bufsize:v:3  3750k -s:v:3  1280x720    -g:v:3  120 -x264opts:v:3  no-scenecut -filter:v:3   "scale=1280:720"  -filter:v:3 "fps=fps=60" -profile:v:3 high \
-b:v:4  12000k  -minrate:v:4 12000k  -maxrate:v:4 12000k   -bufsize:v:4 6000k -s:v:4 1920x1080   -g:v:4 120 -x264opts:v:4 no-scenecut -filter:v:4  "scale=1980:1080" -filter:v:4 "fps=fps=60" -profile:v:4 high \
-b:v:5  24000k -minrate:v:5 24000k -maxrate:v:5 24000k  -bufsize:v:5 12000k -s:v:5 2560x1440   -g:v:5 120 -x264opts:v:5 no-scenecut -filter:v:5  "scale=2560:1440" -filter:v:5 "fps=fps=60" -profile:v:5 high \
-f dash -seg_duration 4   \
-init_seg_name $1'_60fps/$RepresentationID$/init-stream.mp4' \
-media_seg_name $1'_60fps/$RepresentationID$/$Number%05d$.m4s' \
-adaptation_sets "id=0,streams=v" Manifest_60fps.mpd

# Put the created manifest file in a foler
mkdir manifest-$1
mv Manifest_60fps.mpd manifest-$1/