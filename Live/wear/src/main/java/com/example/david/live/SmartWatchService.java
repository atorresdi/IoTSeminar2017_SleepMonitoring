package com.example.david.live;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by David on 16-Oct-17.
 */

public class SmartWatchService extends Service implements SensorEventListener, GoogleApiClient.ConnectionCallbacks{

    private static final String TAG = "SmartWatchComms";

    private boolean isRunning = false;

    private GoogleApiClient mApiClient;
    private static final String START_ACTIVITY = "/start_activity";
    private static final String WEAR_MESSAGE_PATH = "/message";

    private Sensor mHeartRateSensor;
    private SensorManager mSensorManager;

    @Override
    public void onCreate(){
        Log.i(TAG, "Service onCreate");

        isRunning = true;

        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE); // using Sensor Lib (Samsung Gear Live)


        mSensorManager.registerListener(this, this.mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .build();

        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();
    }

    private void sendMessage( final String path, final String text ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }
                Log.d(TAG, "Sent Message");
                stopSelf();
            }
        }).start();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.values[0] > 0){
            Log.d(TAG, "sensor event: " + sensorEvent.accuracy + " = " + sensorEvent.values[0]);
            sendMessage( WEAR_MESSAGE_PATH, String.valueOf(sensorEvent.values[0]) );
        }
    }



    @Override
    public void onConnected(Bundle bundle) {
        sendMessage( START_ACTIVITY, "" );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service onStartCommand");
        initGoogleApiClient();

        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(TAG, "accuracy changed: " + i);
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        mApiClient.disconnect();
        isRunning = false;

        Log.i(TAG, "Service onDestroy");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
