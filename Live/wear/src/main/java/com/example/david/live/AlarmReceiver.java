package com.example.david.live;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by David on 22-Oct-17.
 */

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "SmartWatchComms";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm signalled");
        Intent background = new Intent(context, SmartWatchService.class);
        context.startService(background);
    }

}
