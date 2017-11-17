package com.example.live;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import static android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED;
import static com.example.live.ServiceComm.IEX_ACTION;

/**
 * Created by alvaro on 11/4/17.
 */

public class MicrophoneService extends Service {

    private final Context context = this;
    private static final String TAG = MicrophoneService.class.getSimpleName();

    private final int SAMPLING_RATE = 16000;
    private final int OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4;
    private final int AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC;

    public static int AMPLITUDE_THRESHOLD = 5000;
    private final int RECORDING_PERIOD_MS = 1000*3;
    private final int SAMPLING_PERIOD_MS = 100;
    private final int SAMPLE_NUM = 10;
    private final int RESUME_RECORDING_DELAY_MS = 100;
    private final int MAX_DURATION_MS = 1000*3;
    private Handler handler;

    private boolean max_duration_reached_f = false;

    MediaRecorder.OnInfoListener onInfoListener = new MediaRecorder.OnInfoListener() {
        @Override
        public void onInfo(MediaRecorder mediaRecorder, int i, int i1) {

            Log.w(TAG, "onInfoListener: i = " + i + ", i1 = " + i1);
            if (i == MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                max_duration_reached_f = true;
            }
        }
    };

    MediaRecorder.OnErrorListener onErrorListener = new MediaRecorder.OnErrorListener() {
        @Override
        public void onError(MediaRecorder mediaRecorder, int i, int i1) {
            Log.w(TAG, "onErrorListener: i = " + i + ", i1 = " + i1);
        }
    };

    Runnable startRecordingRunnable = new Runnable() {
        @Override
        public void run() {
            (new Thread(recordingRunnable)).start();
            handler.postDelayed(this, RECORDING_PERIOD_MS);
        }
    };

    Runnable recordingRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

                MediaRecorder mRecorder;
                String mDirPath = getExternalCacheDir().getAbsolutePath() + "/";
                String mFileName = "audio" + System.currentTimeMillis() + ".m4a";

                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setAudioChannels(1); // mono
                mRecorder.setAudioSamplingRate(SAMPLING_RATE);
                mRecorder.setOutputFormat(OUTPUT_FORMAT);
                mRecorder.setAudioEncoder(AUDIO_ENCODER);
                mRecorder.setOutputFile(mDirPath + mFileName);
                mRecorder.setMaxDuration(MAX_DURATION_MS);
                mRecorder.setOnInfoListener(onInfoListener);
                mRecorder.setOnErrorListener(onErrorListener);

                max_duration_reached_f = false;
                mRecorder.prepare();
                mRecorder.start();

                boolean result = amplitudeIsAboveThreshold(mRecorder);
                if (result == true) {
                    handler.removeCallbacks(startRecordingRunnable); // avoid new call
                    while (!max_duration_reached_f) { // wait until the end of the recording
                        Thread.sleep(100);
                    }
                }

                mRecorder.stop();
                mRecorder.reset();
                mRecorder.release();

                if (result == true) {
                    ServiceComm.executeAction(context, RPiCommService.SERVICE_NAME,
                            RPiCommService.ACT_FILE, mFileName);
                    handler.postDelayed(startRecordingRunnable, RESUME_RECORDING_DELAY_MS); // schedule new recording
                } else {
                    File file = new File(getExternalCacheDir(), mFileName);
                    file.delete(); // delete file if amplitude is bellow threshold
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "recordingRunnable: exception mRecorder.prepare()");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, "recordingRunnable: exception sleep");
            }
        }
    };

    private boolean amplitudeIsAboveThreshold(MediaRecorder mRecorder) throws InterruptedException {
        int amplitude;
        mRecorder.getMaxAmplitude();

        for (int i = SAMPLE_NUM; i > 0; i--) {
            Thread.sleep(SAMPLING_PERIOD_MS);
            amplitude = mRecorder.getMaxAmplitude();
            ServiceComm.executeAction(context, MainActivity.ACTIVITY_NAME,
                    MainActivity.ACT_PRINT, "getMaxAmplitude: " + amplitude);
            if (amplitude >= AMPLITUDE_THRESHOLD)
                return true;
        }

        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(SERVICE_NAME));
        Log.i(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        handler.removeCallbacks(startRecordingRunnable);
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
    public static final String SERVICE_NAME = MicrophoneService.class.getSimpleName();
    public static final String ACT_START = "start";
    public static final String ACT_STOP = "stop";
    public static final String ACT_TERMINATE = "terminate";

    // callback for message reception from other service/activity
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String action = intent.getStringExtra(IEX_ACTION);
            Log.i(TAG, "Action: " + action);

            switch (action) {
                case ACT_START:
                    startRecordingRunnable.run();
                    break;

                case ACT_STOP:
                    handler.removeCallbacks(startRecordingRunnable);
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
}
