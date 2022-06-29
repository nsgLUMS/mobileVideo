# Footprint Experiments

## About

These were conducted on Nexus 5 with an automated script `run_footprint_exp.py`. The script logged pss at an interval of 4s.

## Usage

After reading ../../video-setup/mobile-utils/nexus5-utils/README.md, and starting the node memory pressure server, run the following:
```
cp ../../video-setup/mobile-utils/nexus5-utils/* ../../video-setup/abr-server/
cp run_footprint_exp.py ../../video-setup/abr-server/
cd ../../video-setup/abr-server/
mkdir results/pss_logs
python3 run_footprint_exp.py 192.168.1.177
```

## Results

The resulting logs of the experiment and the notebook used to parse them are present in the results directory.