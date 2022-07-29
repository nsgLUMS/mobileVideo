package com.example.signalcapturer;

import android.Manifest;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.CpuUsageInfo;
import android.os.Handler;
import android.os.HardwarePropertiesManager;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.LoginFilter;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TimeUtils;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

public class backGroundService extends Service {

    // prefs
    private static final String SHARED_PREFERENCES_NAME = "prefs";
    private static final String IS_SURVEY_DONE_PREF = "isSurveyDone";
    private static final String IS_CONSENT_TAKEN_PREF = "isConsentTaken";
    private static final String FORM_AGE = "FORM_AGE", DEFAULT_FORM_AGE = "<12";
    private static final String FORM_GENDER = "FORM_GENDER", DEFAULT_FORM_GENDER = "Male";
    private static final String FORM_RADIO = "FORM_RADIO";
    private static final int DEFAULT_RADIO = -1;
    private static final String FORM_Q6 = "FORM_Q6", DEFAULT_FORM_Q6 = "";

    public static final String CHANNEL_ID = "backgroundServiceChannel";

    String logFile = "signalsLogger.txt";

    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "Channel_Id";

    private static boolean isStarted = false;
    private static final int delayIntervalForDetailedMemStats = 10;  // 10 seconds
//    private static final int CPU_LOG_DELAY = 5*1000;                // 2 minutes
    private static final long NETWORK_UPLOAD_DELAY = 15 * DateUtils.MINUTE_IN_MILLIS + 1000;     // 15 minutes
    private static final long RESTART_SERVICE_INTERVAL = 35 * DateUtils.MINUTE_IN_MILLIS; // 35 minutes

    private static boolean isDeviceInteractive;
    private static boolean isDeviceInPowerSave;
    private static boolean isDeviceBatteryLow;

//    private static final Handler handler = new Handler();
    private Handler handler;
    private JobScheduler fileUploadJob = null;
    private JobScheduler configUploadJob = null;

    private long timeAtServiceStart = 0;

    private StringBuilder tempLogs; // temp logs kept before writing to file
    private int secsElapsedSinceLastFileWrite = 0; // seconds elapsed since last file write
    private static final int INTERVAL_FOR_WRITING = 4 * 60; // interval for writing to file in seconds i.e. 4 minutes

    public backGroundService() { }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d("SERVICE", "Came to start a new service at " + java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()));

        tempLogs = new StringBuilder();

        // do your jobs here
        try{
//            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

            String androidID = Settings.System.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

            this.logFile = androidID + "_"+ Build.MANUFACTURER /* + android.os.Build.MODEL */ +  "_" + "signalsLogger.txt";
            this.logFile = this.logFile.replace(" ","");

            Log.d("FILEPATH", this.getExternalFilesDir(null).getAbsolutePath() + this.logFile);
        }
        catch (SecurityException e){
            Log.e("PHONE_ID", String.valueOf(e));
        }
        catch (Exception e) {
            Log.e("Error", String.valueOf(e));
        }

        if (!isStarted) {

            getForegroundStarted();

            isStarted = true;

            Log.d("SERVICE", "Starting new service jobs at " + java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()));
            timeAtServiceStart = System.currentTimeMillis();

            // fill the variables that are going to be changed by pending intents
            isDeviceInteractive = getDeviceInteractivityStatus(getApplicationContext());
            isDeviceInPowerSave = getDevicePowerSaveState();
            isDeviceBatteryLow = getLowBatteryStatus(getApplicationContext());

            // Job to upload mem logs and cpu logs
            final JobScheduler jobScheduler = (JobScheduler)getApplicationContext().getSystemService(JOB_SCHEDULER_SERVICE);
            ComponentName componentName = new ComponentName(this, sendToServer.class);
            PersistableBundle bundle = new PersistableBundle();
            bundle.putString("fileName", logFile);
            bundle.putString("deviceConfig", "false");
            JobInfo jobInfoObj = new JobInfo.Builder(1, componentName)
                    .setPeriodic(NETWORK_UPLOAD_DELAY)
                    .setExtras(bundle)
                    .setPersisted(true)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build();
            jobScheduler.schedule(jobInfoObj);

            final JobScheduler extraDataJob = saveExtraData();

            fileUploadJob = jobScheduler;
            configUploadJob = extraDataJob;

            handler = new Handler(Looper.getMainLooper());

            handler.post(periodicUpdate);

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            intentFilter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
            intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
            intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);

            this.registerReceiver(this.broadcastReceiver, intentFilter);

        }

        return START_STICKY; // super.onStartCommand(intent, flags, startId);
    }

    private boolean getDevicePowerSaveState() {
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        return powerManager.isPowerSaveMode();
    }

    private boolean getLowBatteryStatus(Context context) {

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        assert batteryStatus != null;

        boolean toReturn;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            toReturn = batteryStatus.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, true);
        } else {
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float)scale;

            int mLowBatteryWarningLevel = 15;
            try {
                Resources res = Resources.getSystem();
                int id = res.getIdentifier("config_lowBatteryWarningLevel", "integer", "android");
                mLowBatteryWarningLevel = getResources().getInteger(id);
                Log.d("ID", Integer.toString(mLowBatteryWarningLevel));
            } catch (Resources.NotFoundException e) {
                Log.e("lowBattWarnLvl", e.getMessage());
            }

            toReturn = !isCharging
                    && batteryPct <= mLowBatteryWarningLevel;
        }

        Log.d("isDeviceBatteryLow", String.format("%b", toReturn));

        return toReturn;

    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                switch (Objects.requireNonNull(intent.getAction())) {
                    case "android.intent.action.SCREEN_OFF":
                        Log.d("BROADCAST", "SCREEN_OFF");
                        writeBroadcastSignal("SCREEN_OFF");
                        isDeviceInteractive = false;
                        break;
                    case "android.intent.action.SCREEN_ON":
                        Log.d("BROADCAST", "SCREEN_ON");
                        writeBroadcastSignal("SCREEN_ON");
                        isDeviceInteractive = true;
                        break;
                    case "android.os.action.POWER_SAVE_MODE_CHANGED":
                        Log.d("BROADCAST", "POWER_SAVE_MODE_CHANGED");
                        writeBroadcastSignal("POWER_SAVE_MODE_CHANGED");
                        isDeviceInPowerSave = !isDeviceInPowerSave;
                        break;
                    case "android.intent.action.BATTERY_LOW":
                        Log.d("BROADCAST", "BATTERY_LOW");
                        writeBroadcastSignal("BATTERY_LOW");
                        isDeviceBatteryLow = true;
                        break;
                    case "android.intent.action.BATTERY_OKAY":
                        Log.d("BROADCAST", "BATTERY_OKAY");
                        writeBroadcastSignal("BATTERY_OKAY");
                        isDeviceBatteryLow = false;
                        break;
                }
            } catch (NullPointerException e) {
                Log.e("BROADCAST", e.getMessage());
            }
        }
    };

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        writeTempLogsToFile();
        emptyTempLogs();

        unregisterReceiver(broadcastReceiver);
        isStarted = false;
        Log.d("SERVICE", "onDestroy() was called");
        Log.d("SERVICE", "going to restart service");
        Intent restartService = new Intent("RestartService")
                .setPackage(getPackageName());
        sendBroadcast(restartService);
    }

    private void writeBroadcastSignal(String msg) {
        tempLogs.append("B:" + "\t" + System.currentTimeMillis() + "\t" + msg + "\n");
    }

    private final Runnable periodicUpdate = new Runnable () {
        public int counter = 0;
        public boolean stopHandler = false;
        public void run() {
            // scheduled another events to be in 10 seconds later
            if (!stopHandler) {

                handler.postDelayed(periodicUpdate, 1000);

    //            Log.d("MEM_STARTED", "MEMSAVE called");

                counter++;
                if (counter == delayIntervalForDetailedMemStats) {
                    counter = 0;
                    saveMemory(true);
    //                saveCPU(logFile, System.currentTimeMillis());
                    long currentTime = System.currentTimeMillis();
                    if (currentTime > timeAtServiceStart + RESTART_SERVICE_INTERVAL) {

                        // we need to stop now and restart
                        Log.d("SERVICE", "Stopping service at " + java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()));

                        // stop job
                        if (fileUploadJob != null) {
                            fileUploadJob.cancel(1);
                        }
                        // stop service

                        stopSelf();
                        Log.d("SERVICE", "This is logged after stopSelf()");
                        stopHandler = true;
                    }
                } else {
                    saveMemory(false);
                }

                if (++secsElapsedSinceLastFileWrite > INTERVAL_FOR_WRITING) {
                    secsElapsedSinceLastFileWrite = 0;
                    writeTempLogsToFile();
                    emptyTempLogs();

                    /*
                     * COMMENT THE BELOW BEFORE RELEASE, CPU INTENSIVE ACTIVITY
                     * */
//                    Log.i("FILE_WRITE", "Wrote to file, # of lines in file are " +
//                            getFileLength());
                }
            }
        }
    };

    private long getFileLength() {
        int lines = 0;
        try {
            String filename = getApplicationContext().getExternalFilesDir(null)
                    .getAbsolutePath() + this.logFile;
            Log.d("FILE_WRITE", "this.logFile = " + this.logFile);
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            while (reader.readLine() != null) lines++;
            reader.close();
        } catch (IOException e) {
            Log.e("FILE_WRITE", e.getMessage());
        }
        return lines;
    }

    private void emptyTempLogs() {
        Log.i("FILE_WRITE", "Going to empty tempLogs (len: " + tempLogs.length() + ")");
        tempLogs.setLength(0);
        Log.i("FILE_WRITE", "Emptied tempLogs (len: " + tempLogs.length() + ")");
    }

    private void writeTempLogsToFile() {
        String filename = getApplicationContext().getExternalFilesDir(null)
                .getAbsolutePath() + this.logFile;
        try {
            FileWriter fw = new FileWriter(filename, true);
            fw.write(tempLogs.toString());
            fw.close();
            Log.i("FILE_WRITE", "Wrote tempLogs (len: " + tempLogs.length() + ") to file");
        } catch (IOException exception) {
            Log.e("FILE_WRITE", exception.getMessage());
        }
    }

    private final static String NOTIFICATION_CHANNEL_ID = "Channel1";
    private final static int ONGOING_NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelName = "My Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);
        }
    }

    private void getForegroundStarted() {

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Running in the background")
                    .setSmallIcon(R.mipmap.signalcapturer_launcher)
                    .setContentIntent(pendingIntent)
                    .build();
        } else {
            notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Running in the background")
                    .setSmallIcon(R.mipmap.signalcapturer_launcher)
                    .setContentIntent(pendingIntent)
                    .build();
        }

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        saveTrimSignal(System.currentTimeMillis(), level);
    }

    public void saveTrimSignal(long unixTime, int trimLevel) {

        ActivityManager actManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        long totalMemory = memInfo.availMem;

        AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);

        String powerInfo = getIntFromBool(isDeviceInteractive) + "\t" + getIntFromBool(isDeviceInPowerSave) + "\t" + getIntFromBool(isDeviceBatteryLow);

        tempLogs.append("T:" + "\t" + unixTime + "\t" + trimLevel + "\t" +
                totalMemory + "\t" + powerInfo + "\t" + getIntFromBool(manager.isMusicActive()) + "\n");
        //            Log.e("TRIM_SIGNAL",mydate + "\t" + Long.toString(unixTime) + "\t" + trimLevelName);

    }

    public void saveMemory(boolean isDetailed){

        try {

            ActivityManager actManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            actManager.getMemoryInfo(memInfo);
            long totalMemory = memInfo.availMem;

//            List<ActivityManager.RunningServiceInfo> listApps = actManager.getRunningServices(10000);
//            List<ActivityManager.RunningAppProcessInfo> listproc = actManager.getRunningAppProcesses();

//            Log.e("Processes", listproc.size()+" - Service=>" + listApps.size());
//            Log.e("NumOfApps",("" + listApps.size()) + listApps.get(0).process);

//            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//                Parcelable.Creator<CpuUsageInfo> cpuInfo = CpuUsageInfo.CREATOR;
//                    CpuUsageInfo.getActive();
//
//            }

            long unixTime = System.currentTimeMillis();
//            String mydate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
            String filename = this.getExternalFilesDir(null)
                    .getAbsolutePath() + this.logFile;
//            Log.e("FILENAME",filename);
            int lastTrimLevel = getLastTrimLevel();


//            try {
//                Class clazz = Class.forName("com.android.internal.R.integer");
//                Field field = clazz.getDeclaredField("config_lowBatteryWarningLevel");
//                field.setAccessible(true);
//                int LowBatteryLevel = getApplicationContext().getResources().getInteger(field.getInt(null));
//                Log.d("LowBattery","warninglevel " + LowBatteryLevel);
//            } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
//                e.printStackTrace();
//            }

            AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
//            Log.d("MUSIC", "isMusicActive: " + manager.isMusicActive());

            String numberOfRunningServices = getNumberOfRunningServices(actManager);

            String powerInfo = getIntFromBool(isDeviceInteractive) + "\t" + getIntFromBool(isDeviceInPowerSave) + "\t" + getIntFromBool(isDeviceBatteryLow);

            String strToWrite =
                    unixTime + "\t" +
                    numberOfRunningServices + "\t" +
                    totalMemory + "\t" +
                    lastTrimLevel + "\t" +
                    powerInfo + "\t" +
                    getIntFromBool(manager.isMusicActive());


            if (isDetailed) {
                String detailedStats = getDetailedMemStatsString();
                strToWrite += "\t" + detailedStats;
                tempLogs.append("D:" + "\t" + strToWrite + "\n");
            } else {
                tempLogs.append("M:" + "\t" + strToWrite + "\n");
            }



//            getBatteryStats(getApplicationContext());
//
//            boolean isDeviceLocked = getDeviceLockStatus(getApplicationContext());
////            Log.d("DEVICE_LOCK", String.format("%b", isDeviceLocked));
//
//            boolean isDeviceInteractive = getDeviceInteractivityStatus(getApplicationContext());
//
//            Log.d("DEVICE_LOCK", String.format("isDeviceLocked: %b, isDeviceInteractive: %b", isDeviceLocked, isDeviceInteractive));
//            Log.d("BROADCAST_CONST", String.format("isDeviceInteractive: %b, isDeviceInPowerSave: %b, isDeviceBatteryLow: %b",
//                    isDeviceInteractive, isDeviceInPowerSave, isDeviceBatteryLow));
//
            String mydate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
            Log.d("MEMORY_LOG",mydate + " " + strToWrite + " " + filename);

        } catch (IOException ioe) {
            Log.e("WriteError",ioe.getMessage());
        }
    }

    private int getIntFromBool(boolean inp) {
        if (inp) return 1;
        return 0;
    }

    private String getNumberOfRunningServices(ActivityManager actManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return "-";
        } else {
            List<ActivityManager.RunningServiceInfo> listApps = actManager.getRunningServices(10000);
            return Integer.toString(listApps.size());
        }
    }

    private boolean getDeviceInteractivityStatus(Context applicationContext) {
        PowerManager powerManager = (PowerManager) applicationContext.getSystemService(Context.POWER_SERVICE);
        return powerManager.isInteractive();
    }

//    private boolean getDeviceLockStatus(Context applicationContext) {
//        KeyguardManager myKM = (KeyguardManager) applicationContext.getSystemService(Context.KEYGUARD_SERVICE);
//        return myKM.isKeyguardLocked();
//    }

//    private void getBatteryStats(Context context) {
//
//        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
//        Intent batteryStatus = context.registerReceiver(null, ifilter);
//
//        StringBuilder batteryStr = new StringBuilder();
//
//        // Are we charging / charged?
//        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
//        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
//                status == BatteryManager.BATTERY_STATUS_FULL;
//        batteryStr.append(String.format("isCharging: %b", isCharging));
//
//        // How are we charging?
//        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
//        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
//        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
//        batteryStr.append(String.format(", usbCharge: %b", usbCharge));
//        batteryStr.append(String.format(", acCharge: %b", acCharge));
//
//        // What is the battery percentage?
//        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
//        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
//        float batteryPct = level * 100 / (float)scale;
//        batteryStr.append(String.format(", batteryPct: %f", batteryPct));
//
//        Log.i("BATTERY", batteryStr.toString());
//    }

    private String getDetailedMemStatsString() throws FileNotFoundException {

        Map<String, String> memMap = getDetailedMemStatsObj();

        String[] keys = {
                "MemFree:",
                "Buffers:",
                "Cached:",
                "SwapCached:",
                "Active:",
                "Inactive:",
                "Active(anon):",
                "Inactive(anon):",
                "Active(file):",
                "Inactive(file):",
                "Unevictable:",
                "SwapTotal:",
                "SwapFree:",
                "Dirty:",
                "Writeback:",
                "AnonPages:",
                "Mapped:",
                "VmallocTotal:",
                "VmallocUsed:",
                "VmallocChunk:"
        };

        StringBuilder memStatsString = new StringBuilder();
        String memValue;
        for (String key: keys) {
            if ((memValue = memMap.get(key)) != null) {
                memStatsString.append(memValue);
            }
            memStatsString.append("\t");
        }

        // { DEBUGGING
//        StringBuilder text = new StringBuilder();
//        for (String key: memMap.keySet()) {
//            String value = memMap.get(key).toString();
//            text.append(key + ": " + value);
//            text.append("; ");
//        }
//        Log.i("memMap", text.toString());
        // DEBUGGING }

        return memStatsString.toString();

    }

    private Map<String, String> getDetailedMemStatsObj() throws FileNotFoundException {
        File memInfoFile = new File("/proc", "meminfo");
        BufferedReader br = new BufferedReader(new FileReader(memInfoFile));
        String line = null;
        Map <String, String> memMap = new HashMap<>();
        try {
            while((line = br.readLine()) != null) {
                String[] strs = line.split("\\s+");
                memMap.put(strs[0], strs[1]);
            }
        }
        catch (IOException e) {
            Log.e(TAG, "------ getStringFromInputStream " + e.getMessage());
        }
        finally {
            if(br != null) {
                try {
                    br.close();
                }
                catch (IOException e) {
                    Log.e(TAG, "------ getStringFromInputStream " + e.getMessage());
                }
            }
        }
        return memMap;
    }


    public String getTrimLevelName(int level) {
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE: {
                return "TRIM_MEMORY_RUNNING_MODERATE";
            }
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW: {
                return "TRIM_MEMORY_RUNNING_LOW";
            }
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:{
                return "TRIM_MEMORY_RUNNING_CRITICAL";
            }
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN: {
                return "TRIM_MEMORY_UI_HIDDEN";
            }
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND: {
                return "TRIM_MEMORY_BACKGROUND";
            }
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE: {
                return "TRIM_MEMORY_MODERATE";
            }
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE: {
                return "TRIM_MEMORY_COMPLETE";
            }
            case 0: {
                return "0";
            }
            default: {
                return "UNKNOWN_LEVEL";
            }
       }
    }

    private int getLastTrimLevel() {
        ActivityManager.RunningAppProcessInfo rapI = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(rapI);
        return rapI.lastTrimLevel;
    }

    public JobScheduler saveExtraData() {
        try {
            ActivityManager actManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            actManager.getMemoryInfo(memInfo);

//            String mydate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
            String filename = this.getExternalFilesDir(null)
                    .getAbsolutePath() + "Configuration_" + this.logFile;
//            Log.e("WriteError",filename);

            // getFormData() returns tab-separated string of form results
            String formData = getFormData();

            long unixTime = System.currentTimeMillis();

            String toWrite =
                    "C:\t" +
                    unixTime + "\t" +
                    formData + "\t" +
                    Build.MANUFACTURER + "\t" +
                    memInfo.totalMem + "\t" +
                    memInfo.threshold + "\t" +
                    actManager.isLowRamDevice() + "\t" +
                    Runtime.getRuntime().availableProcessors() + "\t" +
                    Build.VERSION.SDK_INT + "\n";

            FileWriter fw = new FileWriter(filename, true);
            fw.write(toWrite);
//                    formData +
////                    "Brand: " + Build.BRAND + "\n"+
////                         "Model: " + android.os.Build.MODEL + "\n" +
//                            "Manufacturer: " + Build.MANUFACTURER + "\n" +
////                            "Hardware: " + Build.HARDWARE + "\n" +
//                            "TotalMemory: " + memInfo.totalMem + "\n" +
//                            "Thresholds: "  + memInfo.threshold + "\n" +
//                            "isLowRAMDevice: " + actManager.isLowRamDevice() + "\n" +
//                            "numberOfCores: " + Runtime.getRuntime().availableProcessors() + "\n" +
//                            "APILevel: " + Build.VERSION.SDK_INT + "\n<CONFIG_END>\n"
//            );
            fw.close();
            JobScheduler jobScheduler = (JobScheduler)getApplicationContext()
                    .getSystemService(JOB_SCHEDULER_SERVICE);
            ComponentName componentName = new ComponentName(this,
                    sendToServer.class);
            PersistableBundle bundle = new PersistableBundle();
            bundle.putString("fileName", "Configuration_" + this.logFile);
            bundle.putString("deviceConfig", "true");

            JobInfo jobInfoObj = new JobInfo.Builder(2, componentName)
                    .setExtras(bundle)
                    .setPersisted(true)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build();
            jobScheduler.schedule(jobInfoObj);

            Log.i("jobSchedule", "Going to exit Job Schedule");
            return jobScheduler;

        } catch (IOException ioe) {
            Log.e("WriteError", ioe.getMessage());
            return null;
        }
    }

    private String getFormData() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        String age = prefs.getString(FORM_AGE, DEFAULT_FORM_AGE);
//        String gender = prefs.getString(FORM_GENDER, DEFAULT_FORM_GENDER);
        int[] radioAnswers = { -1, -1, -1, -1, -1 };
        for (int i = 0; i < radioAnswers.length; ++i) {
            radioAnswers[i] = prefs.getInt(FORM_RADIO + Integer.toString(i + 1), DEFAULT_RADIO);
        }
        String q6 = prefs.getString(FORM_Q6, DEFAULT_FORM_Q6);



        StringBuilder toWrite = new StringBuilder(age + "\t" /*+ gender + "\t"*/);
        for (int radioAnswer : radioAnswers) {
            toWrite.append(radioAnswer).append("\t");
        }
        toWrite.append(q6);

        return toWrite.toString();
    }

//    public void saveFormData() {
//        try {
//            ActivityManager actManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
//
//            String filename = this.getExternalFilesDir(null)
//                    .getAbsolutePath() + "Form_" + this.logFile;
////            Log.e("WriteError",filename);
//
//            SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
//            String age = prefs.getString(FORM_AGE, DEFAULT_FORM_AGE);
//            String gender = prefs.getString(FORM_GENDER, DEFAULT_FORM_GENDER);
//            int[] radioAnswers = { -1, -1, -1, -1, -1 };
//            for (int i = 0; i < radioAnswers.length; ++i) {
//                radioAnswers[i] = prefs.getInt(FORM_RADIO + Integer.toString(i + 1), DEFAULT_RADIO);
//            }
//            String q6 = prefs.getString(FORM_Q6, DEFAULT_FORM_Q6);
//
//            long unixTime = System.currentTimeMillis();
//
//            StringBuilder toWrite = new StringBuilder("time: " + unixTime + "\n" +
//                    "age: " + age + "\n" +
//                    "gender: " + gender + "\n");
//            for (int i = 0; i < radioAnswers.length; ++i) {
//                toWrite.append("radio").append(i).append(": ").append(radioAnswers[i]).append("\n");
//            }
//            toWrite.append("q6: ").append(q6).append("\n<END>\n");
//
//            FileWriter fw = new FileWriter(filename, true);
//            fw.write(toWrite.toString());
//            fw.close();
//
//            JobScheduler jobScheduler = (JobScheduler)getApplicationContext()
//                    .getSystemService(JOB_SCHEDULER_SERVICE);
//            ComponentName componentName = new ComponentName(this,
//                    sendToServer.class);
//            PersistableBundle bundle = new PersistableBundle();
//            bundle.putString("fileName", "Form_" + this.logFile);
//            bundle.putString("deviceConfig", "true");
//
//            JobInfo jobInfoObj = new JobInfo.Builder(3, componentName)
//                    .setExtras(bundle)
//                    .setPersisted(true)
//                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
//                    .build();
//            jobScheduler.schedule(jobInfoObj);
//
//        } catch (IOException ioe) {
//            Log.e("WriteError", ioe.getMessage());
//        }
//    }

//    private String getCpuDetails() {
//
//        File memInfoFile = new File("/proc", "stat");
//        BufferedReader br = null;
//
//        try {
//            br = new BufferedReader(new FileReader(memInfoFile));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            Log.e("CPU_DETAILS", e.getMessage());
//        }
//
//        StringBuilder cpuDetails = new StringBuilder();
//
//        try {
//            String line;
//            assert br != null;
//            while((line = br.readLine()) != null) {
//                String[] strs = line.split("\\s+");
//                if (strs.length > 1 && isCpuLineNeeded(strs[0])) {
//                    cpuDetails.append(line).append("\n");
//                }
//            }
//        }
//        catch (IOException e) {
//            Log.e(TAG, "------ getStringFromInputStream " + e.getMessage());
//        }
//        finally {
//            if(br != null) {
//                try {
//                    br.close();
//                }
//                catch (IOException e) {
//                    Log.e(TAG, "------ getStringFromInputStream " + e.getMessage());
//                }
//            }
//        }
//        return cpuDetails.toString();
//    }


//    private boolean isCpuLineNeeded(String key) {
//        // either it is some cpu info
//        if (key.matches("cpu\\d*")) return true;
//
//        // or it is some other fields we are interested in
//        List<String> dan = Arrays.asList("ctxt", "btime", "processes", "procs_running", "procs_blocked", "procs_blocked");
//        return dan.contains(key);
//    }


//    public void saveCPU(String logFile, Long unixTime) {
//
//        Log.d("CAME", "hello");
//
//        String cpuLogFile = getExternalFilesDir(null) + "CPU_" + logFile;
//        String cpuDetails = getCPUDetails();
//
//        Log.d("CPU_DETAILS", cpuDetails + "s");
//
//        try {
//            FileWriter fw = new FileWriter(cpuLogFile, true);
//            fw.write("LOG_TIME: " + unixTime + "\n" + cpuDetails + "\n*****\n");
//            fw.close();
//        } catch (IOException e) {
//            Log.e("IO_CPU", e.getMessage());
//        }
//    }
//
//    private String getCPUDetails() {
//
////        String[] command = {"dumpsys cpuinfo"};
//
//        StringBuilder output = new StringBuilder();
//
//        Process p;
//        try {
//            p = Runtime.getRuntime().exec("dumpsys cpuinfo");
////            assert p != null;
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
//
//            // read the first two and the last line from dumpsys cpuinfo
//            int lineCount = 0;
//            String line = "";
//            String lastLine = line;
//            while ((line = reader.readLine()) != null) {
//                if (lineCount < 2) {
//                    output.append(line + "\n");
//                    lineCount++;
//                }
//                lastLine = line;
//            }
//            output.append(lastLine);
//            p.waitFor();
//            p.destroy();
//
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//            Log.e("CPU_DET", e.getMessage());
//        }
//
//        return output.toString();
//
//    }
//
//    public static String _getCPUDetails(){
//
////        ProcessBuilder processBuilder;
//        String cpuDetails = "";
////        String[] DATA = {"/system/bin/cat", "/proc/stat"};
////        InputStream is;
////        Process process ;
////        byte[] bArray ;
////        bArray = new byte[1024];
////
////        try{
////            processBuilder = new ProcessBuilder(DATA);
////
////            process = processBuilder.start();
////
////            is = process.getInputStream();
////
////            while(is.read(bArray) != -1){
////                cpuDetails = cpuDetails + new String(bArray);   //Stroing all the details in cpuDetails
////            }
////            is.close();
////
////        } catch(IOException ex){
////            ex.printStackTrace();
////        }
////
////        String [] cpuInfo = cpuDetails.split("\n");
////        cpuDetails = "";
////        for (int i =0; i <cpuInfo.length; i ++   ){
////            if(cpuInfo.length > 3 && cpuInfo[i].substring(0,3).equals("cpu")) {
////                cpuDetails += cpuInfo[i] + " --- ";
////            }
////        }
//        return cpuDetails;
//    }



}