# System-level Trace Analysis through Perfetto

## Experimental Setup

We ran video under synthetic memory pressure the same way we did in §4.3. However, during video playback we recorded system traces through Perfetto.

## Recording traces through Perfetto

We used the [Perfetto UI](https://ui.perfetto.dev/#!/record) to get the command to record traces on the device's shell. We used the following exact steps/commands to record out trace.

- Open the Linux terminal and type the following to open the device shell
    ```
    adb shell
    ```
- Run the following on the device's shell to record the Perfetto trace
    ```
    perfetto \
    -c - --txt \
    -o /data/misc/perfetto-traces/trace_c202 \
    <<EOF

    buffers: {
        size_kb: 63488
        fill_policy: RING_BUFFER
    }
    buffers: {
        size_kb: 2048
        fill_policy: RING_BUFFER
    }
    data_sources: {
        config {
            name: "linux.process_stats"
            target_buffer: 1
            process_stats_config {
                scan_all_processes_on_start: true
            }
        }
    }
    data_sources: {
        config {
            name: "linux.ftrace"
            ftrace_config {
                ftrace_events: "sched/sched_switch"
                ftrace_events: "power/suspend_resume"
                ftrace_events: "sched/sched_wakeup"
                ftrace_events: "sched/sched_wakeup_new"
                ftrace_events: "sched/sched_waking"
                ftrace_events: "sched/sched_process_exit"
                ftrace_events: "sched/sched_process_free"
                ftrace_events: "task/task_newtask"
                ftrace_events: "task/task_rename"
                ftrace_events: "lowmemorykiller/lowmemory_kill"
                ftrace_events: "oom/oom_score_adj_update"
                ftrace_events: "ftrace/print"
                atrace_categories: "am"
                atrace_categories: "gfx"
                atrace_categories: "hal"
                atrace_categories: "sm"
                atrace_categories: "video"
                atrace_categories: "view"
                atrace_categories: "webview"
                atrace_categories: "wm"
                atrace_apps: "lmkd"
            }
        }
    }
    duration_ms: 40000
    write_into_file: true
    file_write_period_ms: 5000
    max_file_size_bytes: 500000000
    flush_period_ms: 30000
    incremental_state_config {
        clear_period_ms: 5000
    }

    EOF
    ```

## Our Recorded Traces

The Perfetto traces that we recorded and use in our analysis are present in `./traces`.

## Our Analysis

- `./time_spent_analysis` contains instructions/code/data to reproduce our measurements of the time spent by kswapd, video client processes in different processor states.
- `./preemption_analysis` contains instructions/code/data to reproduce our mmcqd and kswapd preemption analysis.