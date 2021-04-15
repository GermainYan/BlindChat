package com.example.myapplication;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MyService extends Service implements SensorEventListener  {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;
    private MediaRecorder recorder = null;
    private MediaPlayer player =null;
    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    //  private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private static final String LOG_TAG = "AudioRecording";
    private static String mFileName = null;
    private static String audioFilePath = null;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    //Recognise
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private String LOG_TAGsp = "VoiceRecognitionActivity";
    TextView xText , yText , zText , counterTv; // DECIDED TO LEAVE THEM IN CASE I WANT TO MODIFY AND TEST THE VALUES OF THE AXIS
    Button startButton , stopButton , resetButton;
    Sensor mySensor;
    SensorManager mySensorManager;
    int AlreadyRecorded =0 ;
    int numberOfHolesAndBumbs= 0;
    float accel;
    float accelCurrent;
    float accelLast;
    int shakeReset = 2500;
    long timeStamp;
    private StorageReference mStorageRef;
    private FirebaseAuth fbAuth;
    private FirebaseAuth.AuthStateListener authListener;
  private  FirebaseDatabase database;
  private DatabaseReference myRef;
 message message2;
 public String value_message="hello Guys";
    public MyService() {
    }
    @Override
    public void onCreate() {
        super.onCreate();

        database = FirebaseDatabase.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        fbAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        //myRef = database.getReference("message");
        database=FirebaseDatabase.getInstance();
        myRef = database.getReference().child("message");
        message2=new message(value_message);

        // CREATE SENSOR MANAGER

        int delay = 100000; //in microseconds equivalent to 0.1 sec

        this.mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // CREATE ACCELERATION SENSOR
        this.mySensor = this.mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        // REGISTERING OUR SENSOR

        mySensorManager.registerListener(this,mySensor,SensorManager.SENSOR_DELAY_UI);

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        onTaskRemoved(intent);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        //myRef = database.getReference("message");
        database=FirebaseDatabase.getInstance();
        myRef = database.getInstance().getReference().child("message");
        message2=new message(value_message);
        // CREATE SENSOR MANAGER
        this.mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // CREATE ACCELERATION SENSOR
        this.mySensor = this.mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // REGISTERING OUR SENSOR
        mySensorManager.registerListener(this,mySensor,SensorManager.SENSOR_DELAY_UI);




        // SETTING ACCELERATION VALUES
        accel = 0.00f;
        accelCurrent = SensorManager.GRAVITY_EARTH;
        accelLast = SensorManager.GRAVITY_EARTH;

        return START_STICKY;
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(),this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/AudioRecording.3gp";


        // STORING THE VALUES OF THE AXIS
        // event was previously sensorEvent
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER){
    // ACCELEROMETER LAST READ EQUAL TO THE CURRENT ONE
    accelLast = accelCurrent;
    // QUICK MAFS TO CALCULATE THE ACCELERATION
    accelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
    // DELTA BETWEEN THE CURRENT AND THE LAST READ OF THE ACCELEROMETER
    float delta = accelCurrent - accelLast;
    // QUICK MAFS TO CALCULATE THE ACCEL THAT WILL DECLARE IF IT SHAKED OR NOT
    accel = accel * 0.9f + delta;


    if (accel > 1) {
        final long timenow = System.currentTimeMillis();
        if(timeStamp + shakeReset  > timenow){
            return;
        }
        timeStamp = timenow;
        numberOfHolesAndBumbs++;


        }
    if (numberOfHolesAndBumbs>3){
        numberOfHolesAndBumbs=0;

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
// Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(500);
        }

//start recording
        //start recording
        try
        {
            Thread.sleep(500);
        }
        catch(InterruptedException e)
        {
            // this part is executed when an exception (in this example InterruptedException) occurs
        }
        //speech.startListening(recognizerIntent);

        {

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
               // Toast.makeText(getApplicationContext(), "Recording Started", Toast.LENGTH_LONG).show();


        }

        //Stop record

        //speech.stopListening();

      new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something here
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
               uploadAudio();
                //Toast.makeText(getApplicationContext(), "Recording Stopped after 10S", Toast.LENGTH_LONG).show();
                AlreadyRecorded++;

                Toast.makeText(getApplicationContext(), AlreadyRecorded+"", Toast.LENGTH_LONG).show();}

        }, 10000

        );

    }


}

    }

    private void uploadAudio() {



        audioFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + mFileName;
        Toast.makeText(getApplicationContext(), audioFilePath+"", Toast.LENGTH_LONG).show();
        //if there is a file to upload
        StorageReference riversRef = mStorageRef.child("audio").child("Audio ");
        //StorageReference audioFilePath=mStorageRef.child("Audio").child(fileName);




        Uri file = Uri.fromFile(new File(mFileName));
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        riversRef.putFile(file).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                String key=myRef.push().getKey();
                Toast.makeText(getApplicationContext(), "KEY:"+key, Toast.LENGTH_SHORT).show();
                myRef.child(key).setValue(message2);
            }
        });


    }




    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onResume() {

        mySensorManager.registerListener(this,mySensor,SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        mySensorManager.unregisterListener(this);
    }

    private void downloadAudio() {


        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource("https://firebasestorage.googleapis.com/v0/b/test-fde0b.appspot.com/o/audio%2FAudio%20?alt=media&token=48976734-a198-406a-a6e5-6bf0e6b73e1d");
            mPlayer.prepare();
            mPlayer.start();
          //  Toast.makeText(getApplicationContext(), "Recording Started Playing", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

    }





}










