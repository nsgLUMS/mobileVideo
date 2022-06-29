from threading import Thread
import subprocess
import signal
from queue import Queue, Empty
import sys
import os
import time
import requests

WAIT_INTERVAL_AFTER_RESTART = 60 # 60s
TIMEOUT_AFTER_VIDEO_START = 150 # 150s
TIMEOUT_AFTER_MEM_START = 300 # 300s
MAX_LAST_LOG_TIME = 10 # 10s

# communication with mem app node server
IP_ADDRESS = sys.argv[1]
PORT = 4333

proc1 = None

def abort():
    print('\nError... Aborting!')
    if proc1:
        proc1.send_signal(signal.SIGINT)
        time.sleep(5)
    try:
        sys.exit(0)
    except:
        os._exit(0)

def wait_for_server_to_end(q, proc):
    proc.communicate()
    print('server ended')
    q.put('server ended')

def wait_for_seconds(q, secs):
    time.sleep(secs)
    print('secs ended')
    q.put('secs ended')

def run(cmd):
    exit_code = os.system(cmd)
    if exit_code != 0:
        if cmd == 'bash turn_on_phone.sh':
            print('ADB not working, get it to work and press enter')
            input()
        else:
            abort()

def isStateReached(state_to_achieve, state_msg):
    if state_to_achieve == 'moderate':
        return state_msg == 'Moderate' or state_msg == 'Low' or state_msg == 'Critical'
    elif state_to_achieve == 'low':
        return state_msg == 'Low' or state_msg == 'Critical'
    elif state_to_achieve == 'critical':
        return state_msg == 'Critical'
    else:
        return False

def reach_memory_state(mem_state):
    
    print(f'Sending request {IP_ADDRESS}:{PORT}/approach/{mem_state}')
    r = requests.post(f'http://{IP_ADDRESS}:{PORT}/approach/{mem_state}')
    print(f'Response: {r.status_code}, {r.text}')
    
    q = Queue()

    # start the timeout thread
    print(f'Starting timeout thread for {TIMEOUT_AFTER_MEM_START}s...')
    Thread(target=wait_for_seconds, args=(q, TIMEOUT_AFTER_MEM_START)).start()

    app_data = {}

    while not ('stateMsg' in app_data and isStateReached(mem_state, app_data['stateMsg'])):        
        try:
            q.get(block=False)
            # timeout happened and we still havent reached the state
            print('Timeout occured for reaching mem state')
            return False
        except Empty:
            time.sleep(2)
            r = requests.post(f'http://{IP_ADDRESS}:{PORT}/get-app-data')
            app_data = r.json()
            print(f"Response: {r.status_code}, {app_data['stateMsg']}")

    return True
    

def has_memory_app_survived():
    print(f'Sending request {IP_ADDRESS}:{PORT}/get-app-data')
    r = requests.post(f'http://{IP_ADDRESS}:{PORT}/get-app-data')
    app_data = r.json()
    print(f"Response: {r.status_code}, {app_data['timeStamp']}")
    time_now = time.time()
    time_last_logged = float(app_data['timeStamp'])/1000
    if abs(time_now - time_last_logged) < MAX_LAST_LOG_TIME:
        return True
    else:
        return False

def get_mem_to_apply(mem_state):
    if mem_state == 'critical':
        return 1140
    elif mem_state == 'moderate':
        return 1090
    else:
        return 0

def start_pss_logging(run_num, fps, res, mem_state, q):
    file_name = f'results/pss_logs/{fps}fps{res}p_{run_num}{mem_state[0]}'
    file1 = file_name + '_base'
    file2 = file_name + '_tab0'
    file3 = file_name + '_media'
    file11 = file_name + '_base_status'
    file22 = file_name + '_tab0_status'
    file33 = file_name + '_media_status'
    os.system(f'echo "" > {file1}')
    os.system(f'echo "" > {file2}')
    os.system(f'echo "" > {file3}')
    os.system(f'echo "" > {file11}')
    os.system(f'echo "" > {file22}')
    os.system(f'echo "" > {file33}')

    # proc = subprocess.Popen('adb shell ps | grep "org.mozilla.firefox"', shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    # result, _ = proc.communicate()

    # result = [line.strip() for line in result.decode('utf-8').split('\n')]
    # pids = [(line.split()[1], line.split()[8]) for line in result if line != '' and len(line.split()) >= 9]
    # print(pids)
    # pid_dict = {}
    # for pid in pids:
    #     pid_dict[pid[1]] = pid[0]

    # os.system('adb shell ps | grep "org.mozilla.firefox"')

    while q.empty():

        os.system(f'adb shell dumpsys meminfo org.mozilla.firefox >> {file1}')
        os.system(f'date +%s >> {file1}')
        os.system(f'echo "******************************" >> {file1}')
        os.system(f'adb shell dumpsys meminfo org.mozilla.firefox:tab0 >> {file2}')
        os.system(f'date +%s >> {file2}')
        os.system(f'echo "******************************" >> {file2}')
        os.system(f'adb shell dumpsys meminfo org.mozilla.firefox:media >> {file3}')
        os.system(f'date +%s >> {file3}')
        os.system(f'echo "******************************" >> {file3}')

        try:
            proc = subprocess.Popen('adb shell ps | grep "org.mozilla.firefox"', shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            result, _ = proc.communicate()
            print(result)

            result = [line.strip() for line in result.decode('utf-8').split('\n')]
            pids = [(line.split()[1], line.split()[8]) for line in result if line != '' and len(line.split()) >= 9]
            print(pids)
            pid_dict = {}
            for pid in pids:
                pid_dict[pid[1].strip()] = pid[0].strip()
            
            if 'org.mozilla.firefox' in pid_dict:
                os.system(f'adb shell cat /proc/{pid_dict["org.mozilla.firefox"]}/status >> {file11}')
                os.system(f'date +%s >> {file11}')
                os.system(f'echo "******************************" >> {file11}')
            if 'org.mozilla.firefox:tab0' in pid_dict:
                os.system(f'adb shell cat /proc/{pid_dict["org.mozilla.firefox:tab0"]}/status >> {file22}')
                os.system(f'date +%s >> {file22}')
                os.system(f'echo "******************************" >> {file22}')
            if 'org.mozilla.firefox:media' in pid_dict:
                os.system(f'adb shell cat /proc/{pid_dict["org.mozilla.firefox:media"]}/status >> {file33}')
                os.system(f'date +%s >> {file33}')
                os.system(f'echo "******************************" >> {file33}')
        except Exception as err:
            print('error:', err)

        time.sleep(4)

def experiment(run_num, fps, res, mem_state, try_num):

    print('======================================================')
    print(f'        Run #{run_num} [{fps}fps {res}p | {mem_state}] Try #{try_num}       ')
    print('======================================================')

    # lets reboot
    print('Rebooting...')
    run('adb reboot')

    # start the python ABR server
    print('Starting ABR Server...')
    global proc1
    q = Queue()
    args = ['python', 'robust_mpc_server.py', f'{fps}', f'{res}', f'{run_num}{mem_state[0]}']
    proc1 = subprocess.Popen(args)
    Thread(target=wait_for_server_to_end, args=(q, proc1)).start()

    # wait for phone to wake up
    print(f'Waiting {WAIT_INTERVAL_AFTER_RESTART}s for phone to restart...')
    time.sleep(WAIT_INTERVAL_AFTER_RESTART)

    # turn on the phone
    print(f'Turning on the phone...')
    run('bash turn_on_phone.sh')

    # turn off SELinux
    print("Turning off SELinux...")
    run("adb shell getenforce")
    run("adb shell su -c \"setenforce 0\"")
    run("adb shell getenforce")

    # open memory pressure app
    print(f'Setting up mem pressure app...')
    run(f'bash set_up_mem_pressure_app.sh 1')

    # open firefox and new tab and type url
    print(f'Preparing Firefox...')
    run(f'bash ready_firefox.sh {IP_ADDRESS}')

    # /apply-pressure/:pressure
    print(f'Applying pressure of {get_mem_to_apply(mem_state)}')
    print(f'Sending request {IP_ADDRESS}:{PORT}/apply-pressure/{get_mem_to_apply(mem_state)}')
    r = requests.post(f'http://{IP_ADDRESS}:{PORT}/apply-pressure/{get_mem_to_apply(mem_state)}')
    print(f'Response: {r.status_code}, {r.text}')

    # sleeping for 10s
    print('Sleep for 10s...')
    time.sleep(10)
    
    # if mem_state != 'normal':
    #     # start to apply memory pressure to required state
    #     print(f'Starting to reach {mem_state} memory state...')
    #     success = reach_memory_state(mem_state)
    #     if not success:
    #         proc1.send_signal(signal.SIGINT)
    #         proc1 = None
    #         print('=================== Restarting run ===================')
    #         experiment(run_num, fps, res, mem_state, try_num + 1)

    # start playing video
    print(f'Starting to play video...')
    run('bash press_enter.sh')

    # pss_q = Queue()
    # Thread(target=start_pss_logging, args=(run_num, fps, res, mem_state, pss_q)).start()    

    # now video is playing
    # start the timeout thread
    print(f'Starting timeout thread for {TIMEOUT_AFTER_VIDEO_START}s...')
    Thread(target=wait_for_seconds, args=(q, TIMEOUT_AFTER_VIDEO_START)).start()

    # we will now wait for either the ABR server to close or the timeout to happen
    print(f'Waiting for ABR Server to end or for {TIMEOUT_AFTER_VIDEO_START}s...')
    msg = q.get()

    # pss_q.put('putting garbage')

    # if timeout occured, this means something unusual happened, we need to redo this experiment
    if msg == 'secs ended':
        print('=================== Restarting run ===================')
        proc1.send_signal(signal.SIGINT)
        proc1 = None
        experiment(run_num, fps, res, mem_state, try_num + 1)
    # else our video ran smooth (most prolly)!
    else:
        proc1 = None
        # we have to just check whether the mem app survived all along or not
        if not has_memory_app_survived():
            print('=================== Restarting run ===================')
            experiment(run_num, fps, res, mem_state, try_num + 1)
        else:
            # YAYY, our experiment most prolly went well
            print('======================================================')
            print(f'    Success: Run#{run_num} [{fps}fps {res}p | {mem_state}] Try #{try_num}   ')
            print('======================================================')

if __name__ == "__main__":
    
    try:

        for run_num in [1, 2, 3, 4, 5]:
            for fps in [60, 30]:
                for res in [1080, 720, 480, 360, 240]:
                    for mem_state in ['critical', 'moderate', 'normal']:
                        experiment(run_num, fps, res, mem_state, 1)

    except KeyboardInterrupt:
        print("Keyboard interrupted33.")
        abort()