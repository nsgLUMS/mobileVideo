package com.example.signalcapturer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class BootCompleteReceiver extends BroadcastReceiver {

    private static final String SHARED_PREFERENCES_NAME = "prefs";
    private static final String IS_CONSENT_TAKEN_PREF = "isConsentTaken";

    @Override
    public void onReceive(Context context, Intent intent) {

        // confirm if the intent sent is of boot completed
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.i("SIGNAL_CAPTURER_BOOT", "Received ACTION_BOOT_COMPLETED in SignalCapturer");

            // check if consent has been taken.
            SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
            boolean isConsentTaken = prefs.getBoolean(IS_CONSENT_TAKEN_PREF, false);

            // if consent taken
            if (isConsentTaken) {
                Log.i("SIGNAL_CAPTURER_BOOT", "ACTION_BOOT_COMPLETED Consent is given");
                // start the logging service
                Intent service = new Intent(context, backGroundService.class);
//                        .putExtra("id", Settings.System.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(service);
                } else {
                    context.startService(service);
                }
            } else {
                Log.i("SIGNAL_CAPTURER_BOOT", "ACTION_BOOT_COMPLETED Consent isn't given");
            }
        }

    }
}
