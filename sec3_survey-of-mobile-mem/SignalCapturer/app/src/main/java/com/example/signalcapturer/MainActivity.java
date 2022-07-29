package com.example.signalcapturer;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentCallbacks2;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private static final String SHARED_PREFERENCES_NAME = "prefs";
    private static final String IS_SURVEY_DONE_PREF = "isSurveyDone";
    private static final String IS_CONSENT_TAKEN_PREF = "isConsentTaken";

//    private TextView mainActThankYouTitle, mainActThankYouNote, mainActLearnMore;
//    private Button buttonGrantPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        mainActThankYouTitle = findViewById(R.id.mainActThankYouTitle);
//        mainActThankYouNote = findViewById(R.id.mainActThankYouNote);
//        mainActLearnMore = findViewById(R.id.mainActLearnMore);
//        buttonGrantPermission = findViewById(R.id.buttonGrantPermission);

        // check if survey has not yet been done, or consent has not been taken.
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        boolean isSurveyDone = prefs.getBoolean(IS_SURVEY_DONE_PREF, false);
        boolean isConsentTaken = prefs.getBoolean(IS_CONSENT_TAKEN_PREF, false);

        // if survey not done or consent no taken, take it first.
        if (!isSurveyDone || !isConsentTaken) {
            // start the new activity
            startActivity(new Intent(this, StartupActivity.class));
            finish();
        }
        // else start the logging.
        else {

//            we do not need to request permissions for API > 19
//            requestAppPermissions();

            Intent serviceIntent = new Intent(this, backGroundService.class);
//                    .putExtra("id", Settings.System.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

        }

    }

//    private void reRequestPermission() {
//
//        // change layout
//        mainActThankYouTitle.setText("Can't log :(");
//        mainActThankYouNote.setText(R.string.need_permission_msg);
//        mainActLearnMore.setVisibility(View.GONE);
//        buttonGrantPermission.setVisibility(View.VISIBLE);
//
//    }
//
//    public void reGrantPermission(View view) {
//
//        // request permission
//        requestAppPermissions();
//
//    }
//
//    private void requestAppPermissions() {
//
//        if (hasReadPermissions() && hasWritePermissions()) {
//            return;
//        }
//
//        // get the permission from user:
//        ActivityCompat.requestPermissions(this,
//                new String[] {
//                        Manifest.permission.READ_EXTERNAL_STORAGE,
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE
//                },1 ); // your request code
//
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        boolean areAllPermissionGranted = true;
//        for (int result : grantResults) {
//            if (result == PackageManager.PERMISSION_DENIED) {
//                areAllPermissionGranted = false;
//                break;
//            }
//        }
//
//        if (!areAllPermissionGranted) {
//            reRequestPermission();
//        } else {
//            // change layout back
//            mainActThankYouTitle.setText("Thank you!");
//            mainActThankYouNote.setText(R.string.thank_you_note);
//            mainActLearnMore.setVisibility(View.VISIBLE);
//            buttonGrantPermission.setVisibility(View.GONE);
//        }
//    }
//
//    private boolean hasReadPermissions() {
//        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
//    }
//
//    private boolean hasWritePermissions() {
//        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
//    }

}
