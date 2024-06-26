package com.example.pingmesafe.Activities;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.example.pingmesafe.R;
import com.example.pingmesafe.FireBase.UnSafe_Alert_Model;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Emergency_activity extends AppCompatActivity {

    AppCompatButton btn_SOS;
    AppCompatButton btn_call;
    AppCompatButton btn_sos_dialog_YES;
    AppCompatButton btn_sos_dialog_Cancel;
    AppCompatButton btn_back;
    double latitude;
    double longitude;
    private final String deviceName = android.os.Build.MODEL;
    private String SOS_message="";
    private String Time;
    private String SOSname;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private final DatabaseReference AlertsdatabaseReference = FirebaseDatabase.getInstance().getReference("unsafe alerts");
    private String alertID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_emergency);

        Time = getCurrentTime();

        btn_SOS = findViewById(R.id.btn_needHelp);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!fileExists("FireBaseAlretID.txt")) {
                    alertID = AlertsdatabaseReference.push().getKey();
                    createTextFile(Emergency_activity.this, "FireBaseAlretID.txt", alertID);
                } else {
                    StringBuilder content = new StringBuilder();
                    try {
                        FileInputStream fis = getApplicationContext().openFileInput("FireBaseAlretID.txt");
                        InputStreamReader isr = new InputStreamReader(fis);
                        BufferedReader br = new BufferedReader(isr);
                        String line;
                        while ((line = br.readLine()) != null) {
                            content.append(line);
                        }
                        br.close();
                        isr.close();
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    alertID = content.toString();
                }
            }
        }).start();


        btn_SOS.setOnClickListener(v -> {
            Dialog dialog_SOS = new Dialog(this);
            dialog_SOS.setContentView(R.layout.sos_dialog);

            btn_sos_dialog_YES = dialog_SOS.findViewById(R.id.btn_YES);
            btn_sos_dialog_Cancel = dialog_SOS.findViewById(R.id.btn_Cancel);

            btn_sos_dialog_YES.setOnClickListener(v1 -> {
                dialog_SOS.dismiss();
                ShowSOSMessageDialog();
            });
            btn_sos_dialog_Cancel.setOnClickListener(v1 -> {
                dialog_SOS.dismiss();
            });
            dialog_SOS.show();
        });

        btn_back = findViewById(R.id.btn_Back);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private String getCurrentTime(){
        final String[] time = new String[1];
        new Thread(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                time[0] = sdf.format(calendar.getTime());
            }
        }).start();
        return time[0];
    }

    private void createTextFile(Emergency_activity context, String fileName, String alertID) {
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(alertID.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean fileExists(String fileName) {
        File file = new File(getApplicationContext().getFilesDir(), fileName);
        return file.exists();
    }

    private void ShowSOSMessageDialog() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Dialog dialog_SOS_message = new Dialog(Emergency_activity.this);
                dialog_SOS_message.setContentView(R.layout.sos_dialog_message);

                dialog_SOS_message.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                EditText SOSMessage = dialog_SOS_message.findViewById(R.id.edt_SOSMsg);
                AppCompatButton btn_send_sos = dialog_SOS_message.findViewById(R.id.btn_send_sos);

                btn_send_sos.setOnClickListener(v1 -> {
                    if(!SOSMessage.getText().toString().isEmpty()){
                        SOS_message = SOSMessage.getText().toString().trim();
                        SOSname = GoogleSignIn.getLastSignedInAccount(Emergency_activity.this).getDisplayName();
                        getCurrentLocation();
                        SendSOSAlert();
                    }
                    dialog_SOS_message.dismiss();
                });
                dialog_SOS_message.show();
            }
        }).start();

    }

    private void SendSOSAlert() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AlertsdatabaseReference.child(alertID).setValue(new UnSafe_Alert_Model(latitude, longitude, SOSname, SOS_message, deviceName, Time));
            }
        }).start();
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            SendSOSAlert();
                        }
                    });
        }
    }
}