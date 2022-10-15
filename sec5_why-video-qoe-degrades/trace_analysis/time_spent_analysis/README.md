# Time-spent Analysis

## About

This folder contains the instructions/code/data to reproduce our measurements about the time kswapd and video client process threads spend in different processor states.

We opened our trace files in the [Perfetto Web UI](https://ui.perfetto.dev) and copied thread states from the timeline into csv files present in `./raw-CSVs`. We used trace_analysis to parse and plot the results.