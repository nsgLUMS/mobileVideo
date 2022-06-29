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

SAMPLE SQL:

select process.name as process, thread.name as thread, cpu, sum(dur) / 1e9 as cpu_sec
from sched inner join thread using(utid) inner join process using(upid)
where thread.name = 'kswapd0'
group by utid, cpu
order by cpu_sec desc limit 30;