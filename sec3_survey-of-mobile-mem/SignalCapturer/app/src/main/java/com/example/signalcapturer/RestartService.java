package com.example.signalcapturer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class RestartService extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("RESTART_SERVICE_REC", "reached here");
        Intent service = new Intent(context, backGroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
    }
}
