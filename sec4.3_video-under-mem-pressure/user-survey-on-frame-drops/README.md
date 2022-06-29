# User Survey to Quantify the Impact of Frame Drops on the Overall User Experience

## About

To quantify the impact of frame drops on the overall user experience, we conducted a user study with 96 participants. Users were recruited from a university campus via email and included students and staff. We recorded two samples of our video on Nokia 1 through `adb shell screenrecord`, each encoded at 60 FPS and 240p, but experiencing different memory pressure, resulting in 3% and 35% frame drops. Users were asked to rate their relative experiences of watching the two videos on a scale of 1-5 (with 5 denoting no noticeable difference between the two videos and 1 denoting that the second video was very annoying with respect to the first).

## Videos

Here are the links to the two videos shown to the users:
- [Video 1](https://www.dropbox.com/s/9p2u8fm51pxyq96/Video%201.mp4) (normal memory pressure - 3% frames dropped)
- [Video 2](https://www.dropbox.com/s/s2m5ldk0gb4eqlk/Video%202.mp4) (moderate memory pressure - 35% frames dropped)

## Results

The results of the survey are present in `Video Survey.csv`, while `plot_user_survey_results.ipynb` was used to plot graphs from them.