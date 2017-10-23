package com.example.david.live;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by David on 16-Oct-17.
 */

public class ReceiveMessageService extends Service implements GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener{

    private static final String TAG = "SmartWatchComms";

    private boolean isRunning = false;

    private GoogleApiClient mApiClient;
    private static final String WEAR_MESSAGE_PATH = "/message";

    private Sensor mHeartRateSensor;
    private SensorManager mSensorManager;

    @Override
    public void onCreate(){
        Log.i(TAG, "Service onCreate");

        isRunning = true;

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
        Log.i(TAG, "Service MessageReceived");

        if( messageEvent.getPath().equalsIgnoreCase( WEAR_MESSAGE_PATH ) ) {
            Log.i(TAG, String.format("Message: %s", new String(messageEvent.getData())));
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            long[] vibrationPattern = {0, 500, 50, 300};
            //-1 - don't repeat
            final int indexInPatternToRepeat = -1;
            vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
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
