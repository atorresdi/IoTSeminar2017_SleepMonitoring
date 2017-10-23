package com.example.david.live;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;


public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks {
    private GoogleApiClient mApiClient;
    private static final String START_ACTIVITY = "/start_activity";
    private static final String WEAR_MESSAGE_PATH = "/message";

    private TextView mEditText;
    private Button mstartButton, mstopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        mEditText = (TextView) findViewById(R.id.input);

        mstartButton = (Button) findViewById(R.id.btn_startService);
        mstopButton = (Button) findViewById(R.id.btn_stopService);

        mstartButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mstartButton.setText("Start Pressed");
                Intent intent = new Intent(MainActivity.this, SmartWatchService.class);
                startService(intent);
                sendMessage( WEAR_MESSAGE_PATH, "Hello World" );
            }
        });
        mstopButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mstopButton.setText("Stop Pressed");
                Intent intent = new Intent(MainActivity.this, SmartWatchService.class);
                stopService(intent);
            }
        });

        initGoogleApiClient();
        mEditText.setText("Reading...");
    }

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .build();

        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) ) {
            mApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        sendMessage( START_ACTIVITY, "" );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mApiClient != null && !(mApiClient.isConnected() || mApiClient.isConnecting())){
            mApiClient.connect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
       super.onDestroy();
        mApiClient.disconnect();
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

                /*runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        mEditText.setText( "" );
                    }
                });*/
            }
        }).start();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
