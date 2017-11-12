package com.example.live;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.JsonObject;

import java.util.Calendar;

import static com.example.live.ServiceComm.IEX_ACTION;
import static com.example.live.ServiceComm.IEX_MESSAGE;
import static com.example.live.ServiceComm.isMyServiceRunning;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private final Context context = this;

    /*Alarm*/
    AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    private TimePicker alarmTimePicker;
    private static MainActivity inst;
    //private TextView alarmTextView;
    Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    Ringtone ringtone;

    /*Option List*/
    static final String[] OPTIONS = new String[] {

            "SET ALARM","Option2"
    };
    ListView mListView;


    public static MainActivity instance() {
        return inst;
    }


    TextView textOut;

    boolean started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_set_main);
        /*Alarm*/
        alarmTimePicker = (TimePicker) findViewById(R.id.alarmTimePicker);
        //alarmTextView = (TextView) findViewById(R.id.alarmText);
        ToggleButton alarmToggle = (ToggleButton) findViewById(R.id.alarmToggle);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (alarmUri == null){
            // alert is null, using backup
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (alarmUri == null){
                // alert backup is null, using 2nd backup
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }
        ringtone = RingtoneManager.getRingtone(context, alarmUri);
        ringtone.setStreamType(AudioManager.STREAM_ALARM);
        //Toolbar toolbar1 = (Toolbar) findViewById(R.id.toolbar1);
        //setSupportActionBar(toolbar1);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        textOut = (TextView) findViewById(R.id.textOut);

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(ACTIVITY_NAME));



        /*Option List*/ //TODO: do a function with this list setup

        mListView = (ListView) findViewById(R.id.list);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, OPTIONS);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                int itemPosition     = position;

                // ListView Clicked item value
                String  itemValue    = (String) mListView.getItemAtPosition(position);

                // Show Alert
                //Toast.makeText(getApplicationContext(),
                //        "Position :"+itemPosition+"  ListItem : " +itemValue , Toast.LENGTH_LONG)
                //        .show();

                switch (itemPosition)
                {
                    case 0:
                        setContentView(R.layout.alarm_set_main);
                        Toolbar toolbar1 = (Toolbar) findViewById(R.id.toolbar1);
                        setSupportActionBar(toolbar1);
                        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        getSupportActionBar().setDisplayShowHomeEnabled(true);
                        toolbar1.setNavigationOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                setContentView(R.layout.activity_main);
                            }
                        });
                        break;

                }

            }

        });

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

    public void onToggleClicked(View view) {

        if (((ToggleButton) view).isChecked()) {
            Log.d("MyActivity", "Alarm On");
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
            calendar.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());
            Intent myIntent = new Intent(MainActivity.this, AlarmReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, myIntent, 0);
            alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);

            ringtone.stop();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

            setContentView(R.layout.activity_main);
            //setAlarmText("");
            Log.d("MyActivity", "Alarm Off");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }




    //public void setAlarmText(String alarmText) {
    //    alarmTextView.setText(alarmText);
    //}

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
        //alarmManager.cancel(pendingIntent);
        Log.i(TAG, "onDestroy");
    }

    /*************************************************************
     * communication with other services or activities
     *************************************************************/
    public static final String ACTIVITY_NAME = MainActivity.class.getSimpleName();
    public static final String ACT_PRINT = "print";
    public static final String ACT_ALARM = "alarm";



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
                case ACT_ALARM:
                    String msg2 = intent.getStringExtra(IEX_MESSAGE);
                    //textOut.setText("ALARM");

                    ringtone.play();
                    setContentView(R.layout.alarm_layout);

                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                    //getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);


                    //setContentView(R.layout.activity_main);

                    //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    break;
                default:
                    Log.w(TAG, "Unknown action");
                    break;
            }
        }
    };



}
