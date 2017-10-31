package com.example.david.live;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;

/**
 * Created by David on 16-Oct-17.
 */

public class SmartWatchService extends Service implements GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener{

    private static final String TAG = "SmartWatchComms";

    private boolean isRunning = false;

    private GoogleApiClient mApiClient;
    private static final String HR_MESSAGE_PATH = "/heart_rate";
    private static final String ACC_MESSAGE_PATH = "/accelerometer";

    File dir = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+("/Cell"));
    File hrfile, accfile;

    @Override
    public void onCreate(){
        Log.i(TAG, "Service onCreate");

        isRunning = true;
        hrfile = new File(dir, ("HRTrainingData.csv"));
        accfile = new File(dir, ("AccTrainingData.csv"));
    }
    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks( this )
                .build();

        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();
    }

    @Override
    public void onMessageReceived( final MessageEvent messageEvent ) {
        if( messageEvent.getPath().equalsIgnoreCase( HR_MESSAGE_PATH ) ) {
            Log.i(TAG, "Heart Rate MessageReceived");
            try {
                CSVWriter writer = new CSVWriter(new FileWriter(hrfile, true), ',');
                String[] line = {Long.toString(System.currentTimeMillis()), new String(messageEvent.getData())};

                writer.writeNext(line);
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if( messageEvent.getPath().equalsIgnoreCase( ACC_MESSAGE_PATH ) ) {
            Log.i(TAG, "Acceleration MessageReceived");
            try {
                CSVWriter writer = new CSVWriter(new FileWriter(accfile, true), ',');
                String temp = new String(messageEvent.getData());
                String[] acc = temp.split(",");
                String[] line = {Long.toString(System.currentTimeMillis()), acc[0], acc[1], acc[2]};

                writer.writeNext(line);
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener( mApiClient, this );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service onStartCommand");

        //Creating new thread for my service
        //Always write your long running tasks in a separate thread, to avoid ANR
        new Thread(new Runnable() {
            @Override
            public void run() {

                initGoogleApiClient();

                //Stop service once it finishes its task
                //stopSelf();
            }
        }).start();

        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        if ( mApiClient != null ) {
            Wearable.MessageApi.removeListener( mApiClient, this );
            if ( mApiClient.isConnected() ) {
                mApiClient.disconnect();
            }
        }
        if( mApiClient != null )
            mApiClient.unregisterConnectionCallbacks( this );
        isRunning = false;

        Log.i(TAG, "Service onDestroy");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
