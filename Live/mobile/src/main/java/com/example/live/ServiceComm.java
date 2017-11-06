package com.example.live;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by alvaro on 11/2/17.
 */

public class ServiceComm {
    public static final String IEX_ACTION = "action";
    public static final String IEX_MESSAGE = "message";

    // request action to other service or activity
    public static void executeAction(Context context, String service_name, String action) {
        Intent intent = new Intent(service_name);
        intent.putExtra(IEX_ACTION, action);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    // request action to other service or activity including a message
    public static void executeAction(Context context, String service_name, String action, String msg) {
        Intent intent = new Intent(service_name);
        intent.putExtra(IEX_ACTION, action);
        intent.putExtra(IEX_MESSAGE, msg);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    // check whether a service is running or not
    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
