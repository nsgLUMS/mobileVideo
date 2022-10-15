package com.example.myapplication;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import java.util.List;


public class MainActivity extends AppCompatActivity{

    // Used to load the 'native-lib' library on application startup.

    private static boolean started=MySystemService.isStatred;
    TextInputLayout period_layout;
    TextInputEditText pressure,process_name,period,output;
    RadioGroup radioGroup;
    TextView total_memory,process_pressure,total_pressure,process_pss,free_memory,repeat;
    Intent serviceIntent;
    Switch aSwitch;
     Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WAKE_LOCK)!= PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK},1);
        }
        serviceIntent = new Intent(getApplicationContext(), MySystemService.class);
        Bundle extras=getIntent().getExtras();

        if(extras!=null && extras.getInt("pressure",0)!=0)
        {
            Toast.makeText(getApplicationContext(),extras.toString(),Toast.LENGTH_LONG).show();
            int pres=extras.getInt("pressure",0);
            String proc=extras.getString("proc_name");
            int per=extras.getInt("period",0);
            int init_pers=extras.getInt("initial_pressure",0);
            String output_file=extras.getString("output");
            serviceIntent.putExtra("filename", output_file);
            serviceIntent.putExtra("duration", per);
            serviceIntent.putExtra("repeat", per==0?false:true);
            serviceIntent.putExtra("process", proc);
            serviceIntent.putExtra("pressure", pres);
            serviceIntent.putExtra("initial_pressure", init_pers);

            Toast.makeText(getApplicationContext(),pres + " , "+proc+" , "+per+" , "+output_file,Toast.LENGTH_LONG).show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            }else{
                startService(serviceIntent);
                finish();
            }
            //Starting the service
           // MySystemService.enqueueWork(getApplicationContext(), serviceIntent);            //textView.setText("Service is Running!");
//            /button.setText("Stop");
//            started=true;
        }
        button=findViewById(R.id.button);
        period_layout=findViewById(R.id.textInputLayout3);
        pressure=findViewById(R.id.pressure);
        process_name=findViewById(R.id.package_name);
        period=findViewById(R.id.period);
        output=findViewById(R.id.out_directory);
        radioGroup=findViewById(R.id.group);
        aSwitch=findViewById(R.id.switch1);
        repeat=findViewById(R.id.textView4);
//        process_pressure=findViewById(R.id.process_pressure);
//        total_pressure=findViewById(R.id.total_pressure);
//        process_pss=findViewById(R.id.process_pss);
//        free_memory=findViewById(R.id.free_memory);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    radioGroup.setVisibility(View.GONE);
                    repeat.setVisibility(View.GONE);
                    aSwitch.setText("%");
                }
                else {
                    radioGroup.setVisibility(View.VISIBLE);
                    repeat.setVisibility(View.VISIBLE);
                    aSwitch.setText("MB");

                }
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId==R.id.repeat_yes)
                {
                    period_layout.setVisibility(View.VISIBLE);
                }else
                {
                    period_layout.setVisibility(View.GONE);

                }
            }
        });


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!started) {


                    if (!MySystemService.isInstanceCreated()) {
                        if (!(output.getText().toString().isEmpty() || process_name.getText().toString().isEmpty() || pressure.getText().toString().isEmpty())) {
                            if (aSwitch.isChecked()) {
                                serviceIntent.putExtra("filename", output.getText().toString());
                                serviceIntent.putExtra("process", process_name.getText().toString());
                                serviceIntent.putExtra("pressure", Integer.parseInt(pressure.getText().toString()));
                                serviceIntent.putExtra("repeat", false);
                                serviceIntent.putExtra("duration", 1000);
                                serviceIntent.putExtra("initial_pressure", -1);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(serviceIntent);
                                }else{
                                    startService(serviceIntent);
                                }
                                finish();
                                //textView.setText("Service is Running!");
                                button.setText("Stop");
                                started = true;

                            } else {
                                boolean repeat = radioGroup.getCheckedRadioButtonId() == R.id.repeat_yes;
                                if (!(repeat && period.getText().toString().isEmpty())) {
                                    serviceIntent.putExtra("filename", output.getText().toString());
                                    serviceIntent.putExtra("duration", repeat ? Integer.parseInt(period.getText().toString()) : 1000);
                                    serviceIntent.putExtra("repeat", repeat);
                                    serviceIntent.putExtra("process", process_name.getText().toString());
                                    serviceIntent.putExtra("pressure", Integer.parseInt(pressure.getText().toString()));
                                    serviceIntent.putExtra("initial_pressure", Integer.parseInt(pressure.getText().toString()));


                                    //  startService(serviceIntent); //Starting the service
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        startForegroundService(serviceIntent);
                                    }else{
                                        startService(serviceIntent);
                                    }
                                    finish();
                                    //textView.setText("Service is Running!");
                                    button.setText("Stop");
                                    started = true;
                                } else
                                    Toast.makeText(getApplicationContext(), "Enter repetition period in ms", Toast.LENGTH_SHORT).show();
                            }
                        } else
                            Toast.makeText(getApplicationContext(), "Fill all required Fields", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    stopService(serviceIntent);
                    //textView.setText("No Service is Running! ");
                    started = false;
                    button.setText("Start");
                }

            }
        });

    }
}
