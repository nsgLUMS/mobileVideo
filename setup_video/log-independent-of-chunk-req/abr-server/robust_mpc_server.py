#!/usr/bin/env python
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import SocketServer
import base64
import urllib
import sys
import os
import json
import time
from datetime import datetime
import socket
os.environ['CUDA_VISIBLE_DEVICES']=''

import numpy as np
import time
import itertools

################## ROBUST MPC ###################

S_INFO = 5  # bit_rate, buffer_size, rebuffering_time, bandwidth_measurement, chunk_til_video_end
S_LEN = 8  # take how many frames in the past
MPC_FUTURE_CHUNK_COUNT = 5
VIDEO_BIT_RATE = [300,750,1200,1850,2850,4300]  # Kbps
BITRATE_REWARD = [1, 2, 3, 12, 15, 20]
BITRATE_REWARD_MAP = {0: 0, 300: 1, 750: 2, 1200: 3, 1850: 12, 2850: 15, 4300: 20}
M_IN_K = 1000.0
BUFFER_NORM_FACTOR = 10.0
CHUNK_TIL_VIDEO_END_CAP = 48.0
TOTAL_VIDEO_CHUNKS = 44
DEFAULT_QUALITY = 3  # default video quality without agent
REBUF_PENALTY = 4.3  # 1 sec rebuffering -> this number of Mbps
SMOOTH_PENALTY = 1
TRAIN_SEQ_LEN = 100  # take as a train batch
MODEL_SAVE_INTERVAL = 100
RANDOM_SEED = 42
RAND_RANGE = 1000
SUMMARY_DIR = './results'
LOG_FILE = './results/log'
# in format of time_stamp bit_rate buffer_size rebuffer_time video_chunk_size download_time reward
NN_MODEL = None

CHUNK_COMBO_OPTIONS = []

IP_ADDRESS = '192.168.0.188'
RESOLUTIONS = {
    240:    0,
    360:    1,
    480:    2,
    720:    3,
    1080:   4,
    1440:   5,
    'x':    'x',
}

VIDEO_NAMES = [
    "tennis",
    "dubai",
    "dubai_x265",
    "news",
    "gaming",
    "animation",
    "sitcom",
    "bali"
]

# past errors in bandwidth
past_errors = []
past_bandwidth_ests = []

is_started = False

# video chunk sizes
size_video1 = [2354772, 2123065, 2177073, 2160877, 2233056, 1941625, 2157535, 2290172, 2055469, 2169201, 2173522, 2102452, 2209463, 2275376, 2005399, 2152483, 2289689, 2059512, 2220726, 2156729, 2039773, 2176469, 2221506, 2044075, 2186790, 2105231, 2395588, 1972048, 2134614, 2164140, 2113193, 2147852, 2191074, 2286761, 2307787, 2143948, 1919781, 2147467, 2133870, 2146120, 2108491, 2184571, 2121928, 2219102, 2124950, 2246506, 1961140, 2155012, 1433658]
size_video2 = [1728879, 1431809, 1300868, 1520281, 1472558, 1224260, 1388403, 1638769, 1348011, 1429765, 1354548, 1519951, 1422919, 1578343, 1231445, 1471065, 1491626, 1358801, 1537156, 1336050, 1415116, 1468126, 1505760, 1323990, 1383735, 1480464, 1547572, 1141971, 1498470, 1561263, 1341201, 1497683, 1358081, 1587293, 1492672, 1439896, 1139291, 1499009, 1427478, 1402287, 1339500, 1527299, 1343002, 1587250, 1464921, 1483527, 1231456, 1364537, 889412]
size_video3 = [1034108, 957685, 877771, 933276, 996749, 801058, 905515, 1060487, 852833, 913888, 939819, 917428, 946851, 1036454, 821631, 923170, 966699, 885714, 987708, 923755, 891604, 955231, 968026, 874175, 897976, 905935, 1076599, 758197, 972798, 975811, 873429, 954453, 885062, 1035329, 1026056, 943942, 728962, 938587, 908665, 930577, 858450, 1025005, 886255, 973972, 958994, 982064, 830730, 846370, 598850]
size_video4 = [668286, 611087, 571051, 617681, 652874, 520315, 561791, 709534, 584846, 560821, 607410, 594078, 624282, 687371, 526950, 587876, 617242, 581493, 639204, 586839, 601738, 616206, 656471, 536667, 587236, 590335, 696376, 487160, 622896, 641447, 570392, 620283, 584349, 670129, 690253, 598727, 487812, 575591, 605884, 587506, 566904, 641452, 599477, 634861, 630203, 638661, 538612, 550906, 391450]
size_video5 = [450283, 398865, 350812, 382355, 411561, 318564, 352642, 437162, 374758, 362795, 353220, 405134, 386351, 434409, 337059, 366214, 360831, 372963, 405596, 350713, 386472, 399894, 401853, 343800, 359903, 379700, 425781, 277716, 400396, 400508, 358218, 400322, 369834, 412837, 401088, 365161, 321064, 361565, 378327, 390680, 345516, 384505, 372093, 438281, 398987, 393804, 331053, 314107, 255954]
size_video6 = [181801, 155580, 139857, 155432, 163442, 126289, 153295, 173849, 150710, 139105, 141840, 156148, 160746, 179801, 140051, 138313, 143509, 150616, 165384, 140881, 157671, 157812, 163927, 137654, 146754, 153938, 181901, 111155, 153605, 149029, 157421, 157488, 143881, 163444, 179328, 159914, 131610, 124011, 144254, 149991, 147968, 161857, 145210, 172312, 167025, 160064, 137507, 118421, 112270]

def get_datetime():
    # date_time = datetime.now().strftime("%m/%d/%Y %I:%M:%S:%f %p")
    date_time = datetime.now().strftime("%m/%d/%Y %H:%M:%S:%f")
    return date_time + ' |'

def send_sock_msg():
    pass
    # f = open('./organicmem_nokia1_logs/vid-started', 'w')
    # f.write('something')
    # f.close()

def play_alarm():
    freqs = [400, 425, 450, 475, 600]
    for i in range(5):
        duration, freq = 0.5, freqs[i]
        os.system('play -nq -t alsa synth {} sine {}'.format(duration, freq))
        time.sleep(0.1)

def get_chunk_size(quality, index):
    if ( index < 0 or index > 48 ):
        return 0
    # note that the quality and video labels are inverted (i.e., quality 8 is highest and this pertains to video1)
    sizes = {5: size_video1[index], 4: size_video2[index], 3: size_video3[index], 2: size_video4[index], 1: size_video5[index], 0: size_video6[index]}
    return sizes[quality]

def make_request_handler(input_dict, fixed_quality=0):

    class Request_Handler(BaseHTTPRequestHandler):
        def __init__(self, *args, **kwargs):
            # talha-waheed:
            self.fixed_quality = fixed_quality
            # end-----
            self.input_dict = input_dict
            self.log_file = input_dict['log_file']
            #self.saver = input_dict['saver']
            self.s_batch = input_dict['s_batch']
            #self.a_batch = input_dict['a_batch']
            #self.r_batch = input_dict['r_batch']
            BaseHTTPRequestHandler.__init__(self, *args, **kwargs)

        def return_x_if_key_not_found(self, inp_dict, key):
            if key in inp_dict:
                return str(inp_dict[key])
            else:
                return 'NaN'

        def do_POST(self):

            # global is_started

            # content_length = int(self.headers['Content-Length'])
            # post_data = json.loads(self.rfile.read(content_length))
            # print post_data
            # print 'Frames drop %age: ' + str((float(post_data['droppedFrames'])/float(post_data['totalFrames']))*100) if float(post_data['totalFrames']) > 0 else '0'
            # print 'FPS: ' + str(post_data['frameRate'])
            # if post_data['lastRequest'] == 0 and is_started:
            #     print('crashed')
            #     os._exit(0)

            # is_started = True

            # # print('[')
            # # for log in post_data['playbackLog']:
            # #     if log != None:
            # #         print '\t{'
            # #         for i in log:
            # #             print '\t\t{}: {}'.format(i, log[i])
            # #         print '\t},'
            # # print(']')
            # # print '------------------------'
            # # print post_data
            # # print '========================\n'

            global is_started

            content_length = int(self.headers['Content-Length'])
            post_data = json.loads(self.rfile.read(content_length))
            # print post_data
            if post_data['lastRequest'] == 0 and is_started:
                print('crashed')
                os._exit(0)

            is_started = True

            # print('[')
            # for log in post_data['playbackLog']:
            #     if log != None:
            #         print '\t{'
            #         for i in log:
            #             print '\t\t{}: {}'.format(i, log[i])
            #         print '\t},'
            # print(']')
            # print '------------------------'
            # print post_data
            # print '------------------------'
            frames_drop_percentage = '{:.2f}'.format(((float(post_data['playbackLog'][-1]['droppedFrames'])/float(post_data['playbackLog'][-1]['totalFrames']))*100) if len(post_data['playbackLog']) > 0 and float(post_data['playbackLog'][-1]['totalFrames']) > 0 else 0.0)
            frames_drop_ratio = str(post_data['playbackLog'][-1]['droppedFrames']) + '/' + str(post_data['playbackLog'][-1]['totalFrames']) if len(post_data['playbackLog']) > 0 else '0/0'
            frames_per_second = '{:.2f}'.format((post_data['playbackLog'][-1]['frameRate']) if len(post_data['playbackLog']) > 0 else 0.0)
            time_elapsed = float(post_data['playbackLog'][-1]['timeElapsed']) if len(post_data['playbackLog']) > 0 else 0.0
            print '{} {}, {:.2f}s: {}fps\t{}%\t{}'.format(get_datetime(), str(post_data['lastRequest']), time_elapsed, frames_per_second, frames_drop_percentage, frames_drop_ratio)
            # print (get_datetime()) + 'frame_drop: ' + frames_drop_ratio + ' ' + frames_drop_percentage.zfill(6) + '% | fps: ' + frames_per_second

            if ( 'pastThroughput' in post_data ):
                # @Hongzi: this is just the summary of throughput/quality at the end of the load
                # so we don't want to use this information to send back a new quality
                print "Summary: ", post_data
            else:
                # option 1. reward for just quality
                # reward = post_data['lastquality']
                # option 2. combine reward for quality and rebuffer time
                #           tune up the knob on rebuf to prevent it more
                # reward = post_data['lastquality'] - 0.1 * (post_data['RebufferTime'] - self.input_dict['last_total_rebuf'])
                # option 3. give a fixed penalty if video is stalled
                #           this can reduce the variance in reward signal
                # reward = post_data['lastquality'] - 10 * ((post_data['RebufferTime'] - self.input_dict['last_total_rebuf']) > 0)

                # option 4. use the metric in SIGCOMM MPC paper
                rebuffer_time = float(post_data['RebufferTime'] -self.input_dict['last_total_rebuf'])

                # --linear reward--
                reward = VIDEO_BIT_RATE[post_data['lastquality']] / M_IN_K \
                        - REBUF_PENALTY * rebuffer_time / M_IN_K \
                        - SMOOTH_PENALTY * np.abs(VIDEO_BIT_RATE[post_data['lastquality']] -
                                                  self.input_dict['last_bit_rate']) / M_IN_K

                # --log reward--
                # log_bit_rate = np.log(VIDEO_BIT_RATE[post_data['lastquality']] / float(VIDEO_BIT_RATE[0]))   
                # log_last_bit_rate = np.log(self.input_dict['last_bit_rate'] / float(VIDEO_BIT_RATE[0]))

                # reward = log_bit_rate \
                #          - 4.3 * rebuffer_time / M_IN_K \
                #          - SMOOTH_PENALTY * np.abs(log_bit_rate - log_last_bit_rate)

                # --hd reward--
                # reward = BITRATE_REWARD[post_data['lastquality']] \
                #         - 8 * rebuffer_time / M_IN_K - np.abs(BITRATE_REWARD[post_data['lastquality']] - BITRATE_REWARD_MAP[self.input_dict['last_bit_rate']])

                self.input_dict['last_bit_rate'] = VIDEO_BIT_RATE[post_data['lastquality']]
                self.input_dict['last_total_rebuf'] = post_data['RebufferTime']

                # retrieve previous state
                if len(self.s_batch) == 0:
                    state = [np.zeros((S_INFO, S_LEN))]
                else:
                    state = np.array(self.s_batch[-1], copy=True)

                # compute bandwidth measurement
                video_chunk_fetch_time = post_data['lastChunkFinishTime'] - post_data['lastChunkStartTime']
                video_chunk_size = post_data['lastChunkSize']

                # compute number of video chunks left
                video_chunk_remain = TOTAL_VIDEO_CHUNKS - self.input_dict['video_chunk_coount']
                self.input_dict['video_chunk_coount'] += 1

                # dequeue history record
                state = np.roll(state, -1, axis=1)

                # this should be S_INFO number of terms
                try:
                    state[0, -1] = VIDEO_BIT_RATE[post_data['lastquality']] / float(np.max(VIDEO_BIT_RATE))
                    state[1, -1] = post_data['buffer'] / BUFFER_NORM_FACTOR
                    state[2, -1] = rebuffer_time / M_IN_K
                    state[3, -1] = float(video_chunk_size) / float(video_chunk_fetch_time) / M_IN_K  # kilo byte / ms
                    state[4, -1] = np.minimum(video_chunk_remain, CHUNK_TIL_VIDEO_END_CAP) / float(CHUNK_TIL_VIDEO_END_CAP)
                    curr_error = 0 # defualt assumes that this is the first request so error is 0 since we have never predicted bandwidth
                    if ( len(past_bandwidth_ests) > 0 ):
                        curr_error  = abs(past_bandwidth_ests[-1]-state[3,-1])/float(state[3,-1])
                    past_errors.append(curr_error)
                except ZeroDivisionError:
                    # this should occur VERY rarely (1 out of 3000), should be a dash issue
                    # in this case we ignore the observation and roll back to an eariler one
                    past_errors.append(0)
                    if len(self.s_batch) == 0:
                        state = [np.zeros((S_INFO, S_LEN))]
                    else:
                        state = np.array(self.s_batch[-1], copy=True)

                # # log wall_time, bit_rate, buffer_size, rebuffer_time, video_chunk_size, download_time, reward
                # self.log_file.write(str(time.time()) + '\t' +
                #                     str(VIDEO_BIT_RATE[post_data['lastquality']]) + '\t' +
                #                     self.return_x_if_key_not_found(post_data, 'buffer') + '\t' +
                #                     str(rebuffer_time / M_IN_K) + '\t' +
                #                     str(video_chunk_size) + '\t' +
                #                     str(video_chunk_fetch_time) + '\t' +
                #                     str(reward) + '\t' +
                #                     # str(json.dumps(post_data['playbackLog'])) + '\n')
                #                     self.return_x_if_key_not_found(post_data, 'timeFrame') + '\t' +
                #                     self.return_x_if_key_not_found(post_data, 'droppedFrames') + '\t' +
                #                     self.return_x_if_key_not_found(post_data, 'totalFrames') + '\t' +
                #                     self.return_x_if_key_not_found(post_data, 'frameRate') + '\n')
                # self.log_file.flush()

                # log wall_time, bit_rate, buffer_size, rebuffer_time, video_chunk_size, download_time, reward
                self.log_file.write(str(time.time()) + '\t' +
                                    str(VIDEO_BIT_RATE[post_data['lastquality']]) + '\t' +
                                    self.return_x_if_key_not_found(post_data, 'buffer') + '\t' +
                                    str(rebuffer_time / M_IN_K) + '\t' +
                                    str(video_chunk_size) + '\t' +
                                    str(video_chunk_fetch_time) + '\t' +
                                    str(reward) + '\t' +
                                    str(json.dumps(post_data['playbackLog'])) + '\n')
                                    # self.return_x_if_key_not_found(post_data, 'timeFrame') + '\t' +
                                    # self.return_x_if_key_not_found(post_data, 'droppedFrames') + '\t' +
                                    # self.return_x_if_key_not_found(post_data, 'totalFrames') + '\t' +
                                    # self.return_x_if_key_not_found(post_data, 'frameRate') + '\n')
                self.log_file.flush()

                # pick bitrate according to MPC           
                # first get harmonic mean of last 5 bandwidths
                past_bandwidths = state[3,-5:]
                while past_bandwidths[0] == 0.0:
                    past_bandwidths = past_bandwidths[1:]
                #if ( len(state) < 5 ):
                #    past_bandwidths = state[3,-len(state):]
                #else:
                #    past_bandwidths = state[3,-5:]
                bandwidth_sum = 0
                for past_val in past_bandwidths:
                    bandwidth_sum += (1/float(past_val))
                harmonic_bandwidth = 1.0/(bandwidth_sum/len(past_bandwidths))

                # future bandwidth prediction
                # divide by 1 + max of last 5 (or up to 5) errors
                max_error = 0
                error_pos = -5
                if ( len(past_errors) < 5 ):
                    error_pos = -len(past_errors)
                max_error = float(max(past_errors[error_pos:]))
                future_bandwidth = harmonic_bandwidth/(1+max_error)
                past_bandwidth_ests.append(harmonic_bandwidth)


                # future chunks length (try 4 if that many remaining)
                last_index = int(post_data['lastRequest'])
                future_chunk_length = MPC_FUTURE_CHUNK_COUNT
                if ( TOTAL_VIDEO_CHUNKS - last_index < 5 ):
                    future_chunk_length = TOTAL_VIDEO_CHUNKS - last_index

                # all possible combinations of 5 chunk bitrates (9^5 options)
                # iterate over list and for each, compute reward and store max reward combination
                max_reward = -100000000
                best_combo = ()
                start_buffer = float(post_data['buffer'])
                #start = time.time()
                for full_combo in CHUNK_COMBO_OPTIONS:
                    combo = full_combo[0:future_chunk_length]
                    # calculate total rebuffer time for this combination (start with start_buffer and subtract
                    # each download time and add 2 seconds in that order)
                    curr_rebuffer_time = 0
                    curr_buffer = start_buffer
                    bitrate_sum = 0
                    smoothness_diffs = 0
                    last_quality = int(post_data['lastquality'])
                    for position in range(0, len(combo)):
                        chunk_quality = combo[position]
                        index = last_index + position + 1 # e.g., if last chunk is 3, then first iter is 3+0+1=4
                        download_time = (get_chunk_size(chunk_quality, index)/1000000.)/future_bandwidth # this is MB/MB/s --> seconds
                        if ( curr_buffer < download_time ):
                            curr_rebuffer_time += (download_time - curr_buffer)
                            curr_buffer = 0
                        else:
                            curr_buffer -= download_time
                        curr_buffer += 4
                        
                        # linear reward
                        #bitrate_sum += VIDEO_BIT_RATE[chunk_quality]
                        #smoothness_diffs += abs(VIDEO_BIT_RATE[chunk_quality] - VIDEO_BIT_RATE[last_quality])

                        # log reward
                        # log_bit_rate = np.log(VIDEO_BIT_RATE[chunk_quality] / float(VIDEO_BIT_RATE[0]))
                        # log_last_bit_rate = np.log(VIDEO_BIT_RATE[last_quality] / float(VIDEO_BIT_RATE[0]))
                        # bitrate_sum += log_bit_rate
                        # smoothness_diffs += abs(log_bit_rate - log_last_bit_rate)

                        # hd reward
                        bitrate_sum += BITRATE_REWARD[chunk_quality]
                        smoothness_diffs += abs(BITRATE_REWARD[chunk_quality] - BITRATE_REWARD[last_quality])

                        last_quality = chunk_quality
                    # compute reward for this combination (one reward per 5-chunk combo)
                    # bitrates are in Mbits/s, rebuffer in seconds, and smoothness_diffs in Mbits/s
                    
                    # linear reward 
                    #reward = (bitrate_sum/1000.) - (4.3*curr_rebuffer_time) - (smoothness_diffs/1000.)

                    # log reward
                    # reward = (bitrate_sum) - (4.3*curr_rebuffer_time) - (smoothness_diffs)

                    # hd reward
                    reward = bitrate_sum - (8*curr_rebuffer_time) - (smoothness_diffs)

                    if ( reward > max_reward ):
                        max_reward = reward
                        best_combo = combo
                # send data to html side (first chunk of best combo)
                # send_data = 0 # no combo had reward better than -1000000 (ERROR) so send 0
                # if ( best_combo != () ): # some combo was good
                #     send_data = str(best_combo[0])

                send_data = str(self.fixed_quality)
                
                # print post_data['lastRequest']
                
                if self.fixed_quality == 'x':
                    if post_data['lastRequest'] < 8:
                        send_data = str(3) # 60 fps
                    elif post_data['lastRequest'] < 8 + 10:
                        send_data = str(0) # 24 fps
                    else:
                        send_data = str(2) # 48 fps

                end = time.time()
                #print "TOOK: " + str(end-start)

                end_of_video = False
                if ( post_data['lastRequest'] == TOTAL_VIDEO_CHUNKS ):
                    print('run ended')
                    # play_alarm()
                    # os.system("touch done")
                    os._exit(0)

                    send_data = "REFRESH"
                    end_of_video = True
                    self.input_dict['last_total_rebuf'] = 0
                    self.input_dict['last_bit_rate'] = self.fixed_quality if self.fixed_quality != 'x' else 3
                    self.input_dict['video_chunk_coount'] = 0
                    self.log_file.write('\n')  # so that in the log we know where video ends

                self.send_response(200)
                self.send_header('Content-Type', 'text/plain')
                self.send_header('Content-Length', len(send_data))
                self.send_header('Access-Control-Allow-Origin', "*")
                self.end_headers()
                self.wfile.write(send_data)

                # record [state, action, reward]
                # put it here after training, notice there is a shift in reward storage

                if end_of_video:
                    self.s_batch = [np.zeros((S_INFO, S_LEN))]
                else:
                    self.s_batch.append(state)

        def do_GET(self):
            print >> sys.stderr, 'GOT REQ'
            self.send_response(200)
            #self.send_header('Cache-Control', 'Cache-Control: no-cache, no-store, must-revalidate max-age=0')
            self.send_header('Cache-Control', 'max-age=3000')
            self.send_header('Content-Length', 20)
            self.end_headers()
            self.wfile.write("console.log('here');")

        def log_message(self, format, *args):
            return

    return Request_Handler


def run(server_class=HTTPServer, port=8333, log_file_path=LOG_FILE, fixed_quality=0):

    np.random.seed(RANDOM_SEED)

    if not os.path.exists(SUMMARY_DIR):
        os.makedirs(SUMMARY_DIR)

    # make chunk combination options
    for combo in itertools.product([0,1,2,3,4,5], repeat=5):
        CHUNK_COMBO_OPTIONS.append(combo)

    with open(log_file_path, 'wb') as log_file:

        # log_file.write('Time\tQuality\tBuffer\tRebuffering Time\tChunk Size\tChunk Fetch Time\tReward\ttimeFrame\tDropped Frames\tTotal Frames\tFrame Rate\n')

        s_batch = [np.zeros((S_INFO, S_LEN))]

        last_bit_rate = DEFAULT_QUALITY
        last_total_rebuf = 0
        # need this storage, because observation only contains total rebuffering time
        # we compute the difference to get

        video_chunk_count = 0

        input_dict = {'log_file': log_file,
                      'last_bit_rate': last_bit_rate,
                      'last_total_rebuf': last_total_rebuf,
                      'video_chunk_coount': video_chunk_count,
                      's_batch': s_batch}

        # interface to abr_rl server
        handler_class = make_request_handler(input_dict=input_dict, fixed_quality=fixed_quality)

        server_address = (IP_ADDRESS, port)
        httpd = server_class(server_address, handler_class)
        print ('Listening on port ' + str(port))
        httpd.serve_forever()

def set_inits(vid, fps, res):
    # set the initial chunk in each quality to be of the **fixed** quality we want to play
    res_id = RESOLUTIONS[res]
    vid_folder = '/var/www/html/{}_{}fps'.format(vid, fps)
    vid_orig_inits_folder = '{}/orig_inits/{}'.format(vid_folder, res_id)
    for i in range(6 if res != 'x' else 4):
        os.system('sudo cp {}/00001.m4s {}/{}/'.format(vid_orig_inits_folder, vid_folder, i))
        os.system('sudo cp {}/init-stream.mp4 {}/{}/'.format(vid_orig_inits_folder, vid_folder, i))

def set_inits_for_same_res(vid, fps, start_id=3):
    pass
    # set the initial chunk in each quality to be of the **fixed** quality we want to play
    res_id = start_id
    vid_folder = '/var/www/html/{}_{}fps'.format(vid, fps)
    vid_orig_inits_folder = '{}/orig_inits/{}'.format(vid_folder, res_id)
    for i in range(4):
        os.system('sudo cp {}/00001.m4s {}/{}/'.format(vid_orig_inits_folder, vid_folder, i))
        # os.system('sudo cp {}/init-stream.mp4 {}/{}/'.format(vid_orig_inits_folder, vid_folder, i))

def main():
    if len(sys.argv) == 3:
        fps = sys.argv[1]
        if fps not in ['30', '60']:
            raise Exception('{}fps not avaiable'.format(fps))

        resolution = int(sys.argv[2])
        if resolution not in RESOLUTIONS:
            raise Exception('{}p not avaiable'.format(resolution))

        vid = int(sys.argv[2])
        if resolution not in RESOLUTIONS:
            raise Exception('{}p not avaiable'.format(resolution))

        os.system('sudo cp manifest/Manifest_{}fps.mpd /var/www/html/Manifest.mpd'.format(str(fps)))

        set_orig_ints(fps, resolution)

        log_file_path = '{}/log_video_{}fps{}p_{}'.format(SUMMARY_DIR, fps, str(resolution), str(time.time()))
        
        # DEFAULT_QUALITY=RESOLUTIONS[resolution]

        run(log_file_path=log_file_path, fixed_quality=RESOLUTIONS[resolution])

    elif len(sys.argv) == 4:
        fps = sys.argv[1]
        if fps not in ['30', '60']:
            raise Exception('{}fps not avaiable'.format(fps))

        resolution = int(sys.argv[2])
        if resolution not in RESOLUTIONS:
            raise Exception('{}p not avaiable'.format(resolution))

        os.system('sudo cp manifest/Manifest_{}fps.mpd /var/www/html/Manifest.mpd'.format(str(fps)))

        log_file_path = '{}/log_video_{}fps{}p_{}'.format(SUMMARY_DIR, fps, str(resolution), sys.argv[3])
        
        # if os.path.exists(log_file_path):
        #     print ('{} already exists, do you want to overwrite it? [y/n]:'.format(log_file_path))
        #     if raw_input() == 'y':
        #         run(log_file_path=log_file_path, fixed_quality=RESOLUTIONS[resolution])
        # else:
        run(log_file_path=log_file_path, fixed_quality=RESOLUTIONS[resolution]) 

    elif len(sys.argv) == 5:

        vid = sys.argv[1]
        if vid not in VIDEO_NAMES:
            raise Exception('{} video not available'.format(vid))

        fps = sys.argv[2]
        if fps not in ['30', '60']:
            raise Exception('{}fps not available'.format(fps))

        
        resolution = int(sys.argv[3]) if sys.argv[3] != 'x' else sys.argv[3]
        if resolution not in RESOLUTIONS:
            raise Exception('{}p not available'.format(resolution))

        # set the manifest for the video and fps
        os.system('sudo cp manifest-{}/Manifest_{}fps.mpd /var/www/html/Manifest.mpd'.format(vid, str(fps)))
        # set the inits for the video quality
        set_inits(vid, fps, resolution)

        log_file_path = '{}/log_video_{}_{}fps{}p_{}'.format(SUMMARY_DIR, vid, fps, str(resolution), sys.argv[4])
        
        # if os.path.exists(log_file_path):
        #     print ('{} already exists, do you want to overwrite it? [y/n]:'.format(log_file_path))
        #     if raw_input() == 'y':
        #         run(log_file_path=log_file_path, fixed_quality=RESOLUTIONS[resolution])
        # else:
        run(log_file_path=log_file_path, fixed_quality=RESOLUTIONS[resolution]) 
    
    elif len(sys.argv) == 6:

        vid = sys.argv[1]
        if vid not in VIDEO_NAMES:
            raise Exception('{} video not available'.format(vid))

        fps = sys.argv[2]
        if fps not in ['30', '48', '60', '1080x', '720x', '480x']:
            raise Exception('{}fps not available'.format(fps))

        resolution = int(sys.argv[3]) if sys.argv[3] != 'x' else sys.argv[3]
        if resolution not in RESOLUTIONS:
            raise Exception('{}p not available'.format(resolution))

        # set the manifest for the video and fps
        os.system('sudo cp manifest-{}/Manifest_{}fps.mpd /var/www/html/Manifest.mpd'.format(vid, str(fps)))
        # set the inits for the video quality
        set_inits(vid, fps, resolution)
        # set_inits_for_same_res(vid, fps, start_id=3)

        log_file_path = '{}/log_video_{}_{}fps{}p_{}'.format(sys.argv[5], vid, fps, str(resolution), sys.argv[4])
        
        # if os.path.exists(log_file_path):
        #     print ('{} already exists, do you want to overwrite it? [y/n]:'.format(log_file_path))
        #     if raw_input() == 'y':
        #         run(log_file_path=log_file_path, fixed_quality=RESOLUTIONS[resolution])
        # else:
        run(log_file_path=log_file_path, fixed_quality=RESOLUTIONS[resolution]) 

    else:
        print "args not passed correct"


if __name__ == "__main__":
    # os.system("rm done")
    try:
        main()
    except KeyboardInterrupt:
        print "Keyboard interrupted."
        try:
            sys.exit(0)
        except SystemExit:
            os._exit(0)
