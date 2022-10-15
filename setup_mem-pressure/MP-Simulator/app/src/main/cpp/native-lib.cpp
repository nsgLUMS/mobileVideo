#include <jni.h>
#include <string>
#include <sys/resource.h>
#include <linux/resource.h>
#include <zconf.h>
#include <android/log.h>
#include <vector>
#include <fstream>
#include <dirent.h>

using namespace std;
static vector<string> s;

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_MySystemService_PassSizeToNative(
        JNIEnv* env,
        jobject instance,
        jint size,
        jboolean repeat) {
    // std::string hello = "Hello from C++";
    setuid(0);
    nice(-17);

//    int ArraySize=size;
//
    int id = 0;
//    pid_t pid = -1;
//    DIR *dir;
//    FILE *fp;
//    char filename[32];
//    char cmdline[256];
//    if(size==-1) {
//        const char *proces_name = env->GetStringUTFChars(proc_name, NULL);
//        // char *process_name="com.google.android.gm";
//
//        struct dirent *entry;
//
//        if (proces_name == NULL)
//            pid = -1;
//
//        dir = opendir("/proc");
//        if (dir == NULL)
//            pid = -1;
//
//        while ((entry = readdir(dir)) != NULL) {
//            id = atoi(entry->d_name);
//            if (id != 0) {
//                if(id==15406)
//                    __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "found:");
//
//                sprintf(filename, "/proc/%d/cmdline", id);
//                fp = fopen(filename, "r");
//                if (fp) {
//                    fgets(cmdline, sizeof(cmdline), fp);
//                    fclose(fp);
//
//                    if (strcmp(proces_name, cmdline) == 0) {
//                        /* process found */
//                        pid = id;
//                        break;
//                    }
//                }
//            }
//        }
//
//        closedir(dir);
//    }else {

//    FILE* file = fopen("/sdcard/hello.csv","wb");
//
//    if (file != NULL)
//    {

    //for(int i=0;i<10;i++) {

    if (size < 0) {
        while (size < 0 && s.size() > 0) {
            size += s.back().length();
            s.pop_back();
        }
        if (size > 0) {
            string sstr(size, ' ');
            s.push_back(sstr);
        }
    } else {
        if (repeat || s.size() == 0) {

            while (size >= 5242880) {
                //  if (size >= 104857600) {
                string sstr(5242880, ' ');
                s.push_back(sstr);
                size -= 5242880;
//            }else if(size < 104857600){
//                    //if(size >= 52428800) {
//                        string sstr(1048576, ' ');
//                        s.push_back(sstr);
//                        size -= 1048576;
////                    } else{
////                        string sstr(524288, ' ');
////                        s.push_back(sstr);
////                        size -= 524288;
////                    }
//            }
            }
            if (size < 5242880) {
                string sstr(size, ' ');
                s.push_back(sstr);

            }
        } else if (repeat == false && size == 0) {
            s.clear();
        }
    }

//    int pid = getpid();
//    const char *pid_char = to_string(pid).c_str();
//    char proc_dir[1024] = "su -c 'echo -17 > /proc/";
//    strcat(proc_dir, pid_char);
//    strcat(proc_dir, "/oom_adj'");
//    system(proc_dir);//String[] cmd2 = { "su","-c","toybox renice -n -20 -p "+pids[0]};
//
//    char proc_nice[1024] = "su -c toybox renice -n -20 -p ";
//    strcat(proc_nice, pid_char);
//    system(proc_nice);
    __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "Self_PSS: %d", s.size());

    // int hertz = sysconf(_SC_CLK_TCK);
//            int tSize = 0, resident = 0, share = 0;
//            int tSize1 = 0, resident1 = 0, share1 = 0;
//            ifstream buffer("/proc/self/statm");
//            buffer >> tSize >> resident >> share;
//            buffer.close();
//            const char * pid_char=to_string(id).c_str();
//            char proc_chrome_dir[1024]="/proc/";
//            strcat(proc_chrome_dir,pid_char);
//            strcat(proc_chrome_dir,"/statm");
//            ifstream buffer1(proc_chrome_dir);
//            buffer1 >> tSize1 >> resident1 >> share1;
//            buffer1.close();
//
//            long page_size_kb = sysconf(_SC_PAGE_SIZE) / 1024; // in case x86-64 is configured to use 2MB pages
//            double self_rss = resident * page_size_kb;
//
//            double self_shared_mem = share * page_size_kb;
//
//            double chrome_rss = resident1 * page_size_kb;
//
//            double chrome_shared_mem = share1 * page_size_kb;
//
//            __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "Self_PSS: %f \t Gmail_PSS: %f",(self_rss - self_shared_mem), (chrome_rss - chrome_shared_mem));
//            fprintf(file, "%f,%f\n",(self_rss - self_shared_mem), (chrome_rss - chrome_shared_mem));
//            //usleep(1000);
//        //}
//        fflush(file);
//        fclose(file);
    //}
//    }
    return ;//id;//env->NewStringUTF(hello.c_str());
}


