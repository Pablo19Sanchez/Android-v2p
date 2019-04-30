package com.example.v2x_safety;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.v2x_safety.comm.LoginRegistrationAsyncTask;
import com.example.v2x_safety.comm.SendDatagramAsyncTask;
import com.example.v2x_safety.comm.UDPClient;
import com.example.v2x_safety.entity.MessageV2X;
import com.example.v2x_safety.entity.decisionClass;

import java.io.InputStream;
import java.util.Random;
import java.util.Timer;

public class modeActivity extends Activity implements SensorEventListener, LocationListener, GpsStatus.Listener, View.OnClickListener {

    // Sensors Variables
    private SensorManager sensorManager;
    private Sensor accelerometer, light, prox;
    private float acceleration, lightValue, proxValue;
    // Location Variables
    private LocationManager locationManager;
    private Location locationInfo;
    private float velocity;
    private double latitude, longitude;
    // Variables to make sensors work with screen blocked
    private PowerManager pm;
    private PowerManager.WakeLock mWakeLock;
    // Display Variables
    private TextView textView, textLoc, textVel, textProx, textWarn, bigText;
    private String mode, velDisplay, locationDisplay;
    // Decision variables
    private decisionClass decision;
    private int warning;
    private int level1, level2, level3;
    // Variable to check the acceleration
    private float[] lastData = new float[3];
    // Fixed Variables (to calculate acceleration)
    private static final float NOISE = (float) 2.0;
    private static final float GRAVITY = (float) 9.8;
    private static final int ENABLE_LOCATION = 789;
    // Variables to send the datagrams
    private String sessionID, keyUser, iv;
    private Handler mHandler;
    private SendDatagramAsyncTask datagramAsyncTask;
    private UDPClient udpClient;
    public static final int OK = 0;
    // Variable to send the BYE Message
    public static final int BYE = 1;
    private LoginRegistrationAsyncTask task;
    private Context context;

    // ACTIVITY METHODS
    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        // Obtain mode from Intent
        mode = intent.getStringExtra("MODE");
        sessionID = intent.getStringExtra("SESSIONID");
        keyUser = intent.getStringExtra("KEY");
        iv = intent.getStringExtra("IV");
        // Set Views
        setContentView(R.layout.mode_layout);
        textView = findViewById(R.id.ModeTextView);
        textLoc = findViewById(R.id.positionInfo);
        textVel = findViewById(R.id.velocityInfo);
        textProx = findViewById(R.id.proximityInfo);
        textWarn = findViewById(R.id.warningInfo);
        bigText = findViewById(R.id.messageInfo);
        // Set mode
        textView.setText(mode);
        // Start Activity
        startService(intent);
        context = getApplicationContext();
        // Handler of AsyncTask
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case OK:
                        // Indicate that a warning message was sent
                        if(warning == 1) level1++;
                        if(warning == 2) level2++;
                        if(warning == 3) level3++;
                        warning = 0;
                        datagramAsyncTask.cancel(true);
                        String messageDisplay = "Level1: " + Integer.toString(level1) + " / Level2: " + Integer.toString(level2) + " / Level3: " + Integer.toString(level3);
                        bigText.setText(messageDisplay);
                        break;
                    case BYE:
                        task.cancel(true);
                        Intent goBackIntent = new Intent(context, FirstActivity.class);
                        startActivity(goBackIntent);
                        break;
                }
            }
        };
        // Start listener button TEST
        (findViewById(R.id.buttonTest)).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start Sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        prox = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        // Start Listeners
        sensorManager.registerListener(this, accelerometer, 500);
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, prox, SensorManager.SENSOR_DELAY_NORMAL);
        // Start Partial WaveLock
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "v2x:partialblock");
        // Start decision maker
        decision = new decisionClass(mode);
        // Start variables
        acceleration = 0;
        lightValue = 0;
        velocity = 0;
        proxValue = 8;
        level1 = 0;
        level2 = 0;
        level3 = 0;
        // UDPClient
        udpClient = new UDPClient();
        // Check location permission and start location service if granted
        if(checkLocationPermission()){
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            // Check first location from the Network Provider (faster)
            locationInfo = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(locationInfo == null){
                latitude = 0;
                longitude = 0;
            } else {
                latitude = locationInfo.getLatitude();
                longitude = locationInfo.getLongitude();
            }
        } else {
            onDestroy();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mWakeLock.acquire();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, prox);
        sensorManager.unregisterListener(this, light);
        // Send message BYE
        InputStream keyin = this.getResources().openRawResource(R.raw.androidkey);
        Context context = getApplicationContext();
        MessageV2X byeMessage = new MessageV2X("BYE", sessionID);
            // We start the AsyncTask sending the Handler, our Message Class and the Certificate
        task = new LoginRegistrationAsyncTask(mHandler, byeMessage, keyin);
        task.execute();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (mWakeLock.isHeld()) mWakeLock.release();
    }

    // TEST METHOD
    public void onClick(View arg0){
        if (arg0 == findViewById(R.id.buttonTest)){
            Random rand = new Random();
            warning = rand.nextInt(3) + 1;
            sendWarningMessage();
        }
    }

    // SENSOR METHODS
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                acceleration = getAccelerometerValues(event);
                break;
            case Sensor.TYPE_LIGHT:
                lightValue = event.values[0];
                break;
            case Sensor.TYPE_PROXIMITY:
                proxValue = event.values[0];
                // Print message to user to be careful
                if(decision.checkProximity(proxValue)){
                    textProx.setText(R.string.checkRoad);
                } else textProx.setText(" ");
        }
        // Check warning
        if(decision.checkAcceleration(acceleration)) {
            warning = decision.levelWarning(acceleration, velocity, proxValue, lightValue);
        }
        if(warning != 0){
            //sendWarningMessage();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    // Obtaining the acceleration from the Accelerometer
    private float getAccelerometerValues(SensorEvent event) {
        float[] delta = new float[3];
        float acc;
        // Get Data from Sensor
        float[] data = event.values;
        // Check noise and calibrate
        delta[0] = Math.abs(lastData[0] - data[0]);
        delta[1] = Math.abs(lastData[1] - data[1]);
        delta[2] = Math.abs(lastData[2] - data[2]);
        if (delta[0] < NOISE) delta[0] = (float)0.0;
        if (delta[1] < NOISE) delta[1] = (float)0.0;
        if (delta[2] < NOISE) delta[2] = (float)0.0;
        lastData = data;
        // Calculate the final acceleration (removing GRAVITY)
        acc = (float) (Math.sqrt(Math.pow(data[0],2)+Math.pow(data[1],2)+Math.pow(data[2],2))) - GRAVITY;
        acc = Math.abs(acc);
        return acc;
    }

    // LOCATION METHODS
    @Override
    public void onLocationChanged(Location location) {
        if(location != null) {
            locationInfo = location;
            latitude = locationInfo.getLatitude();
            longitude = locationInfo.getLongitude();
            velocity = locationInfo.getSpeed() * 3.6f;
        }
        // We print velocity and location
        velDisplay = String.format("%.2f", velocity) + " km/h";
        textVel.setText(velDisplay);
        locationDisplay = String.format("%.2f",latitude) + " :: " + String.format("%.2f", longitude);
        textLoc.setText(locationDisplay);
    }

    @Override
    public void onGpsStatusChanged(int event) { }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }
    @Override
    public void onProviderEnabled(String provider) { }
    @Override
    public void onProviderDisabled(String provider) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_location_activation)
                .setMessage(R.string.text_location_activation)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(modeActivity.this, new String[]{"YES"}, ENABLE_LOCATION);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(modeActivity.this, new String[]{"NO"}, ENABLE_LOCATION);
                    }
                })
                .create()
                .show();
    }


    // Check permission of Location. It should have been done before, so here we just check
    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    // Checks the permissions granted by the user
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
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

    // METHOD TO SEND UDP MESSAGES
    private void sendWarningMessage(){
        MessageV2X message = new MessageV2X("WARNING", sessionID, mode, warning, latitude, longitude, context, keyUser, iv);
        datagramAsyncTask = new SendDatagramAsyncTask(message, mHandler, udpClient);
        datagramAsyncTask.execute();
    }
}
