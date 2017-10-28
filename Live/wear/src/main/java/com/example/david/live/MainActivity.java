package com.example.david.live;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;

public class MainActivity extends Activity{// implements SensorEventListener, GoogleApiClient.ConnectionCallbacks{
    private static final String START_ACTIVITY = "/start_activity";
    private static final String WEAR_MESSAGE_PATH = "/message";
    private GoogleApiClient mApiClient;

    private static final String TAG = MainActivity.class.getName();

    private TextView rate;
    private TextView accuracy;

    private PendingIntent pendingIntent;
    private AlarmManager alarmManager;

    private Button mStartButton, mStopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Intent alarm = new Intent(this, AlarmReceiver.class);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
            rate = (TextView) stub.findViewById(R.id.rate);
            rate.setText("Reading...");

            accuracy = (TextView) stub.findViewById(R.id.accuracy);
            mStartButton = (Button) findViewById(R.id.btn_startService);
            mStopButton = (Button) findViewById(R.id.btn_stopService);

            mStartButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                mStartButton.setText("Started");
                if(!isServiceRunning(ReceiveMessageService.class)){
                    Intent intent = new Intent(MainActivity.this, ReceiveMessageService.class);
                    startService(intent);
                }

                boolean alarmRunning = (PendingIntent.getBroadcast(MainActivity.this, 0, alarm, PendingIntent.FLAG_NO_CREATE) != null);
                if(alarmRunning == false) {
                    pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, alarm, 0);
                    alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 10000, pendingIntent);

                }
                }
            });
            mStopButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                mStopButton.setText("Stopped");
                Intent intent = new Intent(MainActivity.this, ReceiveMessageService.class);
                stopService(intent);

                boolean alarmRunning = (PendingIntent.getBroadcast(MainActivity.this, 0, alarm, PendingIntent.FLAG_CANCEL_CURRENT) != null);
                if(alarmRunning == false) {
                    pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, alarm, 0);
                    alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    alarmManager.cancel(pendingIntent);
                }
                }
            });
            }
        });
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
