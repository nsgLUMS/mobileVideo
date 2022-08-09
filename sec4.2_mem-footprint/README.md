# Memory Footprint Experiments (§4.2)

## About

These were conducted on Nexus 5 with an automated script `run_footprint_exp.py`. The script logged PSS at an interval of 4s.

## Usage

1. Read `../setup_video/mobile-utils/nexus5-utils/README.md` to setup your mobile device according to the automation script.
1. Start the node memory pressure server to coordinate memory pressure (see `../setup_mem-pressure/README.md`)
1. Prepare the setup:
    ```
    cp ../setup_video/mobile-utils/nexus5-utils/* ../setup_video/abr-server/
    cp run_footprint_exp.py ../setup_video/abr-server/
    cd ../setup_video/abr-server/
    mkdir results/pss_logs
    ```
1. Replacing `[IP_ADDRESS]` with your machine's IP address, run the following to begin the experiment:
    ```
    python3 run_footprint_exp.py [IP_ADDRESS]
    ```

## Results

The resulting logs of the experiment and the notebook used to parse them are present in the results directory.