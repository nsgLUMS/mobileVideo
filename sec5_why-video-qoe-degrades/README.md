# Why Video QoE Degrades under Memory Pressure?

## About

In this section we attempt to analyze why video performance may degrade under memory pressure. We use Nokia 1 for our experiments in this section.

### Initial Assessment

We first used `top` to make an intitial assessment of CPU utilizations of the system, memory daemons, and the firefox browser processes during video playback under different memory states. These results are used in Figure 13 of the paper.

### System-level Trace Analysis

Following this, we used Perfetto to conduct a system-level trace analysis. The `./trace_analysis` folder includes instructions on how to record traces on Perfetto using the same configurations we used. It further includes instructions for downloading the traces we recorded, scripts to parse information from them, and IPython notebooks to analyze them and plot graphs.

### Background Apps Analysis

We investigated the performance degradation observed in video streaming with background applications. The `./bg_apps` folder includes code, data, and instructions for reproducing Figure 14.