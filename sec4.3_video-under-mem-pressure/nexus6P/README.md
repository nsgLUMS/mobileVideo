# Video Streaming Under Memory Pressure on Nexus 6P

## About

These were conducted with an automated script `run_nexus6P_exp.py`.

## Usage

After reading ../../setup_video/mobile-utils/nexus6P-utils/README.md, and starting the node memory pressure server, run the following:
```
cp ../../setup_video/mobile-utils/nexus6P-utils/* ../../setup_video/abr-server/
cp run_nexus6P_exp.py ../../setup_video/abr-server/
cd ../../setup_video/abr-server/
python3 run_nexus6P_exp.py 192.168.1.177
```

## Results

The resulting logs of the experiment and the notebook used to parse them are present in the results directory.