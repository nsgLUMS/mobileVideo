# Why Video QoE Degrades under Memory Pressure?

## About

In this section we attempt to analyze why video performance may degrade under memory pressure. We use Nokia 1 for our experiments in this section.

### Initial Assessment

We first used `top` to make an intitial assessment of CPU utilizations of the system, memory daemons, and the firefox browser processes during video playback under different memory states. These results are used in Table 5 of the paper.

### System-level Trace Analysis

Following this, we used Perfetto to conduct a system-level trace analysis.