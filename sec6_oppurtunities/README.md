# Oppurtunities for improvement

## About

In this section, we explore the different oppurtunities to improve video QoE under memory pressure. We conduct experiments on Nokia 1 in which we change the frame rate during playback.

## Generating DASH video with multiple fps

Copy the dubai.mkv video into `./generate_videos`, and run the scripts to convert videos in DASH player video format. Read `./setup_video/README.md` for more context. `generate-dash-vids-<res>xfps.sh` script keeps video at a fixed resolution (<res>) but with various available FPS.

## Running the experiment

Follow `../setup_video` but

- use this multiple FPS video

- use the log-independent-of-chunk-req robust_mpc_server.py

- Run the abr server file by the following command:
    ```
    python2 robust_mpc_server.py [res]x x [logfile_suffix]
    ```
    E.g. for 480p with log file suffix `'normal_1'`:
    ```
    python2 robust_mpc_server.py 480x x normal_1
    ```

To apply memory pressure, we opened background applications, like how we did in Section 4.3.

## Results

You can find the logs and the notebook to parse data and plot the graphs.
