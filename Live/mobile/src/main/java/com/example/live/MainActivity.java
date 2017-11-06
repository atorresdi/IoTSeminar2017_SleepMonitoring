package com.example.live;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.gson.JsonObject;

import static com.example.live.ServiceComm.IEX_ACTION;
import static com.example.live.ServiceComm.IEX_MESSAGE;
import static com.example.live.ServiceComm.isMyServiceRunning;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private final Context context = this;

    TextView textOut;

    boolean started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textOut = (TextView) findViewById(R.id.textOut);

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(ACTIVITY_NAME));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg;
                if (started) {
                    msg = "stopped";
                    ServiceComm.executeAction(context, WatchCommService.SERVICE_NAME, WatchCommService.ACT_STOP);
                    ServiceComm.executeAction(context, MicrophoneService.SERVICE_NAME, MicrophoneService.ACT_STOP);
                }
                else {
                    msg = "started";
                    ServiceComm.executeAction(context, WatchCommService.SERVICE_NAME, WatchCommService.ACT_START);
                    ServiceComm.executeAction(context, MicrophoneService.SERVICE_NAME, MicrophoneService.ACT_START);
                }
                started = !started;

                Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // start services
        if (!isMyServiceRunning(this, WatchCommService.class))
            startService(new Intent(this, WatchCommService.class));
        else
            Log.w(TAG, "WatchCommService running before onCreate()");

        if (!isMyServiceRunning(this, RPiCommService.class))
            startService(new Intent(this, RPiCommService.class));
        else
            Log.w(TAG, "RPiCommService running before onCreate()");

        if (!isMyServiceRunning(this, MicrophoneService.class))
            startService(new Intent(this, MicrophoneService.class));
        else
            Log.w(TAG, "MicrophoneService running before onCreate()");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ServiceComm.executeAction(context, WatchCommService.SERVICE_NAME, WatchCommService.ACT_TERMINATE);
        ServiceComm.executeAction(context, RPiCommService.SERVICE_NAME, RPiCommService.ACT_TERMINATE);
        ServiceComm.executeAction(context, MicrophoneService.SERVICE_NAME, MicrophoneService.ACT_TERMINATE);
        Log.i(TAG, "onDestroy");
    }

    /*************************************************************
     * communication with other services or activities
     *************************************************************/
    public static final String ACTIVITY_NAME = MainActivity.class.getSimpleName();
    public static final String ACT_PRINT = "print";

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
                    textOut.setText(msg);
                    break;
                default:
                    Log.w(TAG, "Unknown action");
                    break;
            }
        }
    };
}
