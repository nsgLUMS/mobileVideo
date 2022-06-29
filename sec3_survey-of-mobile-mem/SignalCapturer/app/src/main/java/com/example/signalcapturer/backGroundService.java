package com.example.signalcapturer;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.CpuUsageInfo;
import android.os.Handler;
import android.os.HardwarePropertiesManager;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.LoginFilter;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class backGroundService extends Service {
    String logFile = "signalsLogger.txt";
    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "Channel_Id";

    Handler handler = new Handler();

    public backGroundService(){

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        // do your jobs here
        try{
//            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

            this.logFile = intent.getExtras().getString("id") + "_"+ Build.BRAND + android.os.Build.MODEL +"_" +"signalsLogger.txt" ;
            this.logFile = this.logFile.replace(" ","");
        }
        catch (SecurityException e){
            Log.e("PHONE_ID", String.valueOf(e));
        }
        catch (Exception e) {
            Log.e("Error", String.valueOf(e));
        }
        startForeground();

        JobScheduler jobScheduler = (JobScheduler)getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        ComponentName componentName = new ComponentName(this,
                sendToServer.class);
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString("fileName", logFile);
        bundle.putString("deviceConfig", "false");
        JobInfo jobInfoObj = new JobInfo.Builder(1, componentName)
                .setPeriodic(15*60*1000)
                .setExtras(bundle)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build();
        jobScheduler.schedule(jobInfoObj);

        Timer timer = new Timer();
        //Set the schedule function
        timer.scheduleAtFixedRate(new TimerTask() {
                  @Override
                  public void run() {
                      // Magic here
                  }
              },
                0, 1000);   // 1000 Millisecond  = 1 second
        handler.post(periodicUpdate);
        saveExtraData();
        return super.onStartCommand(intent, flags, startId);
    }
    private Runnable periodicUpdate = new Runnable () {
        public void run() {
            // scheduled another events to be in 10 seconds later
            handler.postDelayed(periodicUpdate, 1000);
            saveMemory();
        }
    };
    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.signalCapturer";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = notificationBuilder.setOngoing(true)
                .setOngoing(true)
//                    .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running background")
                .setContentIntent(pendingIntent)
                .build();
        startForeground(2, notification);
    }
    private void startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyOwnForeground();
        } else {
            // If earlier version channel ID is not used
            // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
            //""

            Intent notificationIntent = new Intent(this, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                    NOTIF_CHANNEL_ID) // don't forget create a notification channel first
                    .setOngoing(true)
//                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Service is running background")
                    .setContentIntent(pendingIntent)
                    .build());
        }
    }
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        switch (level) {

//            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE: {
                saveLog( " Moderate Pressure");
                Log.e("SIGNAL","Moderate");
                break;
            }

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW: {
                saveLog( " Low Pressure");
                Log.e("SIGNAL","Low");
                break;
            }

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:{
                saveLog(" Critical Pressure");
                Log.e("SIGNAL","Critical");
                break;
            }

        }
    }

    public void saveLog(String fileContents) {
        try {
            String mydate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
            String filename = this.getExternalFilesDir(null)
                    .getAbsolutePath() + this.logFile;
//            Log.e("WriteError",filename);
            FileWriter fw = new FileWriter(filename, true);
            fw.write(mydate + " " + fileContents + "\n");
            fw.close();

        } catch (IOException ioe) {
            Log.e("WriteError",ioe.getMessage());
        }
    }
    public void saveMemory(){
        try {
            ActivityManager actManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            actManager.getMemoryInfo(memInfo);
            long totalMemory = memInfo.availMem/(1024*1024);

            List<ActivityManager.RunningServiceInfo> listApps = actManager.getRunningServices(10000);
            List<ActivityManager.RunningAppProcessInfo> listproc = actManager.getRunningAppProcesses();
//            Log.e("Processes", listproc.size()+" - Service=>" + listApps.size());
//            Log.e("NumOfApps",("" + listApps.size()) + listApps.get(0).process);

//            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//                Parcelable.Creator<CpuUsageInfo> cpuInfo = CpuUsageInfo.CREATOR;
//                    CpuUsageInfo.getActive();
//
//            }

            String mydate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
            String filename = this.getExternalFilesDir(null)
                    .getAbsolutePath() + this.logFile;
//            Log.e("WriteError",filename);
            FileWriter fw = new FileWriter(filename, true);
            fw.write(mydate + " " + totalMemory + " " + listApps.size() +  " " + getCPUDetails()  +"\n");
            Log.e("MEMORY LOG",mydate + " " + totalMemory + " " + listApps.size());
            fw.close();


        } catch (IOException ioe) {
            Log.e("WriteError",ioe.getMessage());
        }
    }
    public void saveExtraData() {
        try {
            ActivityManager actManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            actManager.getMemoryInfo(memInfo);

            String mydate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
            String filename = this.getExternalFilesDir(null)
                    .getAbsolutePath() + "Configuration_" + this.logFile;
//            Log.e("WriteError",filename);
            FileWriter fw = new FileWriter(filename, true);
            fw.write("Brand: " + Build.BRAND + "\n"+
                         "Model: " + android.os.Build.MODEL + "\n" +
                            "Manufacturer: " + Build.MANUFACTURER + "\n" +
                            "Hardware: " + Build.HARDWARE + "\n" +
                            "Total Memory: " + memInfo.totalMem/(1024*1024) + "\n" +
                            "Thresholds: "  + memInfo.threshold + "\n" +
                            "isLowRAMDevice: " + actManager.isLowRamDevice() + "\n"

            );
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

        } catch (IOException ioe) {
            Log.e("WriteError", ioe.getMessage());
        }
    }


    public static String getCPUDetails(){

//        ProcessBuilder processBuilder;
        String cpuDetails = "";
//        String[] DATA = {"/system/bin/cat", "/proc/stat"};
//        InputStream is;
//        Process process ;
//        byte[] bArray ;
//        bArray = new byte[1024];
//
//        try{
//            processBuilder = new ProcessBuilder(DATA);
//
//            process = processBuilder.start();
//
//            is = process.getInputStream();
//
//            while(is.read(bArray) != -1){
//                cpuDetails = cpuDetails + new String(bArray);   //Stroing all the details in cpuDetails
//            }
//            is.close();
//
//        } catch(IOException ex){
//            ex.printStackTrace();
//        }
//
//        String [] cpuInfo = cpuDetails.split("\n");
//        cpuDetails = "";
//        for (int i =0; i <cpuInfo.length; i ++   ){
//            if(cpuInfo.length > 3 && cpuInfo[i].substring(0,3).equals("cpu")) {
//                cpuDetails += cpuInfo[i] + " --- ";
//            }
//        }
        return cpuDetails;
    }



}