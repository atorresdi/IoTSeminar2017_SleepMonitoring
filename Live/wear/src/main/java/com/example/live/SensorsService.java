package com.example.live;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by alvaro on 11/2/17.
 */

public class SensorsService extends Service implements SensorEventListener {

    private final int HR_VAL_NUM = 5;
    private final int ACC_SAMP_NUM = 5;
    private final int ACC_VAL_NUM = 10;

    private class HrData {
        public final String type = "hrData";
        public long[] timestamp = new long[HR_VAL_NUM];
        public int[] accuracy = new int[HR_VAL_NUM];
        public int[] heartRate = new int[HR_VAL_NUM];
    }

    private class AccData {
        public final String type = "accData";
        public long[] timestamp = new long[ACC_VAL_NUM];
        public float[] accX = new float[ACC_VAL_NUM];
        public float[] accY = new float[ACC_VAL_NUM];
        public float[] accZ = new float[ACC_VAL_NUM];
    }

    private final Context context = this;

    private int hrValIdx;
    private HrData hrData;

    private int accSampCount;
    private int accValIdx;
    private AccData accData;

    private boolean hrEnable = false;
    private final long HEART_RATE_TIME_ON = 1000*40;
    private final long HEART_RATE_TIME_OFF = 1000*10;

    private static final String TAG = SensorsService.class.getSimpleName();
    private SensorEventListener sEventListener = this;

    private SensorManager sensorManager;
    private Sensor sensorHeartRate;
    private Sensor sensorAcceleration;

    private Handler hrHandler;

    // periodic task for switching on and off the HR sensor
    Runnable heartRateRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (hrEnable)
                    sensorManager.unregisterListener(sEventListener, sensorHeartRate);
                else
                    sensorManager.registerListener(sEventListener, sensorHeartRate, SensorManager.SENSOR_DELAY_NORMAL);
                hrEnable = !hrEnable;
            } finally {
                long time_us;
                if (hrEnable) time_us = HEART_RATE_TIME_ON;
                else time_us = HEART_RATE_TIME_OFF;
                hrHandler.postDelayed(heartRateRunnable, time_us);
            }
        }
    };

    Runnable sendHrData = new Runnable() {
        @Override
        public void run() {
            Gson gson = new Gson();
            ServiceComm.executeAction(context, PhoneCommService.SERVICE_NAME,
                    PhoneCommService.ACT_SEND, gson.toJson(hrData));
        }
    };

    Runnable sendAccData = new Runnable() {
        @Override
        public void run() {
            Gson gson = new Gson();
            ServiceComm.executeAction(context, PhoneCommService.SERVICE_NAME,
                    PhoneCommService.ACT_SEND, gson.toJson(accData));
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        hrHandler = new Handler();
        hrData = new HrData();
        accData = new AccData();

        sensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        sensorHeartRate = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        sensorAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // register receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(SERVICE_NAME));

        Log.i(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        hrHandler.removeCallbacks(heartRateRunnable);
        sensorManager.unregisterListener(sEventListener);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                accData.accX[accValIdx] += sensorEvent.values[0];
                accData.accY[accValIdx] += sensorEvent.values[1];
                accData.accZ[accValIdx] += sensorEvent.values[2];

                accSampCount--;
                if (accSampCount <= 0) {
                    accSampCount = ACC_SAMP_NUM;
                    accData.timestamp[accValIdx] = System.currentTimeMillis();
                    accData.accX[accValIdx] /= ACC_SAMP_NUM;
                    accData.accY[accValIdx] /= ACC_SAMP_NUM;
                    accData.accZ[accValIdx] /= ACC_SAMP_NUM;

                    accValIdx++;
                    if (accValIdx >= ACC_VAL_NUM) {
                        accValIdx = 0;
                        sendAccData.run();
                    };
                }
                break;

            case Sensor.TYPE_HEART_RATE:
                if (sensorEvent.accuracy > 0) {
                    hrData.timestamp[hrValIdx] = System.currentTimeMillis();
                    hrData.accuracy[hrValIdx] = sensorEvent.accuracy;
                    hrData.heartRate[hrValIdx] = (int)sensorEvent.values[0];

                    hrValIdx++;
                    if (hrValIdx >= HR_VAL_NUM) {
                        hrValIdx = 0;
                        sendHrData.run();
                    }
                }
                break;

            default:
                Log.w(TAG, "Unknown Sensor event: " + sensorEvent.sensor.getName());
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /*************************************************************
     * communication with other services or activities
     *************************************************************/
    public static final String SERVICE_NAME = SensorsService.class.getSimpleName();
    public static final String ACT_START = "start";
    public static final String ACT_STOP = "stop";
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
                    startSensors();
                    ServiceComm.executeAction(context, MainActivity.ACTIVITY_NAME,
                            MainActivity.ACT_PRINT, "Started");
                    break;

                case ACT_STOP:
                    stopSensors();
                    ServiceComm.executeAction(context, MainActivity.ACTIVITY_NAME,
                            MainActivity.ACT_PRINT, "Stopped");
                    break;

                case ACT_TERMINATE:
                    stopSensors();
                    stopSelf();
                    break;

                default:
                    Log.w(TAG, "Unknown action");
                    break;
            }
        }
    };

    private void startSensors() {
        hrValIdx = 0;
        accSampCount = ACC_SAMP_NUM;
        accValIdx = 0;
        heartRateRunnable.run();
        sensorManager.registerListener(sEventListener, sensorAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void stopSensors() {
        hrHandler.removeCallbacks(heartRateRunnable);
        sensorManager.unregisterListener(sEventListener);
        hrEnable = false;
    }
}
