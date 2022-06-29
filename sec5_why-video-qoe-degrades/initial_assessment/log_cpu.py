import os
import sys
import subprocess
# import re
import json

def get_pids():
    proc = subprocess.Popen('adb shell ps -A | grep "firefox\|kswapd\|lmkd\|example"', shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    result, _ = proc.communicate()

    result = [line.strip() for line in result.decode('utf-8').split('\n')]
    lines = [line for line in result if line != '']
    # pid_matches = [match.group(0) for line in lines if (match := re.match(r'^\d+', line))]
    pids = {}

    for line in lines:
        things = line.strip().split()
        pids[things[1]] = things[8]
    return pids

def log_cpu(exp_name):
    pids = get_pids()
    with open(f'cpu_logs/log_{exp_name}', 'w') as file:
        file.write(json.dumps(pids) + '\n')
    os.system(f'date +%s >> cpu_logs/log_{exp_name}')
    os.system(f'adb shell top -d 1 -u shell -p {",".join(pids)}  >> cpu_logs/log_{exp_name}')

if not os.path.exists(f'cpu_logs/log_{sys.argv[1]}.txt'):
    log_cpu(sys.argv[1])
else:
    print(f'Do you want to overwrite log_{sys.argv[1]}.txt? [y/n]:')
    if input() == 'y':
        log_cpu(sys.argv[1])
