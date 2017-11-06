package com.example.live;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.UnsupportedEncodingException;

/**
 * Created by alvaro on 11/2/17.
 */

public class WatchCommService extends Service implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {

    private final Context context = this;
    private static final String TAG = WatchCommService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        // register receiver and initialize GoogleApiClient
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(SERVICE_NAME));
        initGoogleApiClient();
        Log.i(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        mApiClient.disconnect();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*************************************************************
     * communication with other services or activities
     *************************************************************/
    public static final String SERVICE_NAME = WatchCommService.class.getSimpleName();
    public static final String ACT_START = "start";
    public static final String ACT_STOP = "stop";
    public static final String ACT_PRINT = "print";
    public static final String ACT_VIBRATE = "vibrate";
    public static final String ACT_TERMINATE = "terminate";

    // callback for message reception from other service/activity
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String action = intent.getStringExtra(ServiceComm.IEX_ACTION);
            Log.i(TAG, "Action: " + action);

            switch (action) {
                case ACT_START:
                    sendToWatch(PATH_SW_START, "");
                    break;
                case ACT_STOP:
                    sendToWatch(PATH_SW_STOP, "");
                    break;
                case ACT_PRINT:
                    String msg = intent.getStringExtra(ServiceComm.IEX_MESSAGE);
                    sendToWatch(PATH_SW_PRINT, msg);
                    break;
                case ACT_VIBRATE:
                    sendToWatch(PATH_SW_VIBRATE, "");
                    break;
                case ACT_TERMINATE:
                    stopSelf();
                    break;
                default:
                    Log.w(TAG, "Unknown action");
                    break;
            }
        }
    };

    /*************************************************************
     * communication with watch
     *************************************************************/
    private GoogleApiClient mApiClient;
    private static final String PATH_SW_START = "/start";
    private static final String PATH_SW_STOP = "/stop";
    private static final String PATH_SW_PRINT = "/print";
    private static final String PATH_SW_VIBRATE = "/vibrate";
    private static final String PATH_RPI = "/rpi";
    private static final String PATH_SMARTPHONE = "/smartphone";

    // initialization of the GoogleApiClient
    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addConnectionCallbacks(this)
                .addApi( Wearable.API )
                .build();
        if(mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting()))
            mApiClient.connect();
    }

    // send string to the watch
    private void sendToWatch(final String path, final String msg) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for (Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, msg.getBytes() ).await();
                }
            }
        }).start();
    }

    // include the GoogleApiClient as a listener
    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mApiClient, this);
        Log.i(TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended");
    }

    // callback for message reception from watch
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        Log.i(TAG, "path: " + path);
        switch (path) {
            case PATH_RPI:
                try {
                    ServiceComm.executeAction(context, RPiCommService.SERVICE_NAME,
                            RPiCommService.ACT_SEND, new String(messageEvent.getData(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    Log.e(TAG, "onMessageReceived: PATH_RPI exception (UTF-8)");
                }
                break;

            case PATH_SMARTPHONE:
                try {
                    ServiceComm.executeAction(context, MainActivity.ACTIVITY_NAME,
                            MainActivity.ACT_PRINT, new String(messageEvent.getData(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    Log.e(TAG, "onMessageReceived: PATH_SMARTPHONE exception (UTF-8)");
                }
                break;

            default:
                Log.w(TAG, "onMessageReceived: Unknown path");
        }
    }
}
