package com.example.v2x_safety;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


import com.example.v2x_safety.comm.LoginRegistrationAsyncTask;
import com.example.v2x_safety.entity.MessageV2X;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;

public class FirstActivity extends Activity implements View.OnClickListener {

    public static final int NOK = 0;
    public static final int OK = 2;
    public static final int ERROR = 3;
    private static final int REQUEST_LOCATION = 123;
    private static final int ENABLE_LOCATION = 456;
    private static final String HELLO = "HELLO";

    private LocationManager locationManager = null;
    private Context context;
    private MessageV2X theMessage;
    private LoginRegistrationAsyncTask task;
    private Handler mHandler;
    private String mode, keyUser, sessionID, iv;
    private InputStream keyin;

    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_layout);
        // Start Listeners of the buttons
        (findViewById(R.id.walkingButton)).setOnClickListener(this);
        (findViewById(R.id.runningButton)).setOnClickListener(this);
        (findViewById(R.id.bikingButton)).setOnClickListener(this);
        // Check if location is activated and permissions up
        if(checkLocationPermission()){
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            checkLocationEnable();
        }
        context = getApplicationContext();
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case OK:
                        Toast.makeText(getApplicationContext(),"Connected to the service!",Toast.LENGTH_SHORT).show();
                        sessionID = msg.getData().getString("SESSIONID");
                        iv = msg.getData().getString("IV");
                        Intent intent = new Intent(context, modeActivity.class);
                        intent.putExtra("MODE",mode);
                        intent.putExtra("SESSIONID", sessionID);
                        intent.putExtra("KEY", keyUser);
                        intent.putExtra("IV", iv);
                        startActivity(intent);
                        break;
                    case NOK:
                        Toast.makeText(getApplicationContext(),"There was a problem with the server, try again",Toast.LENGTH_SHORT).show();
                        break;
                }
                try {
                    keyin.close();
                } catch (IOException e) {}
                task.cancel(true);
            }
        };
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        // We send BYE message and exit application.
        System.exit(1);
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    public void onClick(View arg0) {
        if (arg0 == findViewById(R.id.walkingButton)) {
            mode = "Walking";
        }
        if (arg0 == findViewById(R.id.runningButton)) {
            mode = "Running";
        }
        if (arg0 == findViewById(R.id.bikingButton)) {
            mode = "Biking";
        }
        keyUser = generateKey();
        keyin = this.getResources().openRawResource(R.raw.androidkey);
        theMessage = new MessageV2X(HELLO, keyUser);
        // We start the AsyncTask sending the Handler, our initialMessage Class and the Certificate
        task = new LoginRegistrationAsyncTask(mHandler, theMessage, keyin);
        task.execute();
    }

    // Method to check if the Location is Enabled
    public void checkLocationEnable(){
        boolean gps_enabled = false, network_enabled = false;

        if(locationManager == null)
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try{
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception ex){}
        try{
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch(Exception ex){}

        if(!gps_enabled && !network_enabled){
            // Show an explanation to the user
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_location_activation)
                    .setMessage(R.string.text_location_activation)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(FirstActivity.this, new String[]{"YES"}, ENABLE_LOCATION);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(FirstActivity.this, new String[]{"NO"}, ENABLE_LOCATION);
                        }
                    })
                    .create()
                    .show();
        }
    }

    // Check permission of Location
    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(FirstActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    // Checks the permissions granted by the user
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yes!
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        new AlertDialog.Builder(this)
                                .setTitle("Thanks")
                                .setMessage("Thanks!!")
                                .create()
                                .show();
                    }
                } else {
                    // permission denied, no! We kill the APP
                    onDestroy();
                }
                return;
            }
            case ENABLE_LOCATION: {
                if (grantResults.length > 0 && permissions[0].equals("YES")){
                    // permission granted, send user to activate location
                    Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(locationIntent);
                } else {
                    // permission denied, no! Kill APP
                    onDestroy();
                }
            }
        }
    }

    private String generateKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[16];
        secureRandom.nextBytes(key);
        String keyString = Base64.encodeToString(key, Base64.NO_PADDING);
        return keyString;
    }
}