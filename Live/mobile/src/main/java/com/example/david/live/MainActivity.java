package com.example.david.live;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;


public class MainActivity extends Activity {//implements GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener {
    private GoogleApiClient mApiClient;
    private static final String START_ACTIVITY = "/start_activity";
    private static final String WEAR_MESSAGE_PATH = "/message";

    private ArrayAdapter<String> mAdapter;

    //private ListView mListView;
    private TextView mEditText;
    private Button mstartButton, mstopButton;
    /*File dir = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+("/Cell"));
    File file;*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        /*if(!dir.exists()) {
            if(dir.mkdir()); //directory is created;
        }
        file = new File(dir, ("TrainingData.csv"));*/

        //mListView = (ListView) findViewById(R.id.list_view);
        mEditText = (TextView) findViewById(R.id.input);

        mstartButton = (Button) findViewById(R.id.btn_startService);
        mstopButton = (Button) findViewById(R.id.btn_stopService);

        /*mAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_list_item_1 );
        mListView.setAdapter( mAdapter );*/

        mstartButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mstartButton.setText("Start Pressed");
                Intent intent = new Intent(MainActivity.this, SmartWatchService.class);
                startService(intent);
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

        //initGoogleApiClient();
        mEditText.setText("Reading...");
    }

    /*private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks( this )
                .build();

        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) ) {
            mApiClient.connect();
        }
    }*/

    /*@Override
    public void onMessageReceived( final MessageEvent messageEvent ) {
        runOnUiThread( new Runnable() {
        @Override
        public void run() {
            if( messageEvent.getPath().equalsIgnoreCase( WEAR_MESSAGE_PATH ) ) {
                mEditText.setText( new String(messageEvent.getData() ));

                try {
                    CSVWriter writer = new CSVWriter(new FileWriter(file, true), ',');
                    String[] line = {Long.toString(System.currentTimeMillis()), new String(messageEvent.getData() )};

                    writer.writeNext(line);
                    //textRssi.setText(file.getAbsolutePath());
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    });
    }*/

    /*@Override
    public void onConnected(Bundle bundle) {
        ////sendMessage( START_ACTIVITY, "" );
        //Wearable.MessageApi.addListener( mApiClient, this );
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        /*if (mApiClient != null && !(mApiClient.isConnected() || mApiClient.isConnecting())){
            mApiClient.connect();
        }*/
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
       /* if( mApiClient != null )
            mApiClient.unregisterConnectionCallbacks( this );*/
        super.onDestroy();
        //mApiClient.disconnect();
    }

    @Override
    protected void onStop() {
       /* if ( mApiClient != null ) {
            Wearable.MessageApi.removeListener( mApiClient, this );
            if ( mApiClient.isConnected() ) {
                mApiClient.disconnect();
            }
        }*/

        super.onStop();
    }

   /* private void sendMessage( final String path, final String text ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }

                *//*runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        mEditText.setText( "" );
                    }
                });*//*
            }
        }).start();
    }*/

    /*@Override
    public void onConnectionSuspended(int i) {

    }*/
}
