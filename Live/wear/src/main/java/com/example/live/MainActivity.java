package com.example.live;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.TimerTask;

import static com.example.live.ServiceComm.IEX_ACTION;
import static com.example.live.ServiceComm.IEX_MESSAGE;
import static com.example.live.ServiceComm.isMyServiceRunning;

public class MainActivity extends WearableActivity {

    private final Context context = this;
    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView mTextView;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        mTextView = (TextView) findViewById(R.id.text);

        mTextView.setText("hello world");
        vibrator.vibrate(200);

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(ACTIVITY_NAME));

        // start services
        if (!isMyServiceRunning(this, PhoneCommService.class))
            startService(new Intent(this, PhoneCommService.class));
        else
            Log.w(TAG, "WatchCommService running before onCreate()");

        if (!isMyServiceRunning(this, SensorsService.class))
            startService(new Intent(this, SensorsService.class));
        else
            Log.w(TAG, "SensorsService running before onCreate()");

        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        ServiceComm.executeAction(this, PhoneCommService.SERVICE_NAME, PhoneCommService.ACT_TERMINATE);
        ServiceComm.executeAction(this, SensorsService.SERVICE_NAME, SensorsService.ACT_TERMINATE);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    /*************************************************************
     * communication with other services or activities
     *************************************************************/
    public static final String ACTIVITY_NAME = MainActivity.class.getSimpleName();
    public static final String ACT_PRINT = "print";
    public static final String ACT_VIBRATE = "vibrate";

    // callback for message reception from other service/activity
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String action = intent.getStringExtra(IEX_ACTION);
            Log.i(TAG, "Action: " + action);

            switch (action) {
                case ACT_PRINT:
                    String msg = intent.getStringExtra(IEX_MESSAGE);
                    mTextView.setText(msg);
                    break;
                case ACT_VIBRATE:
                    vibrator.vibrate(200);
                    break;
                default:
                    Log.w(TAG, "Unknown action");
                    break;
            }
        }
    };
}
