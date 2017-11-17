package com.example.live;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import static com.example.live.MainActivity.IP;
import static com.example.live.ServiceComm.IEX_ACTION;
import static com.example.live.ServiceComm.IEX_MESSAGE;

/**
 * Created by alvaro on 11/2/17.
 */

public class RPiCommService extends Service {
    private static final String TAG = RPiCommService.class.getSimpleName();
    private Context context = this;

    private class AudioFileFrame {
        public final String type = "audioFileFrame";
        public String name;
        public long frameSize;
        public long totalSize;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(SERVICE_NAME));
        (new Thread(serverSocketRunnable)).start();
        Log.i(TAG, "onCreate");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*************************************************************
     * communication with other services or activities
     *************************************************************/

    public int PORT = 5555;

    // server socket
    Runnable serverSocketRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                // Moves the current Thread into the background
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);

                ServerSocket serverSocket = new ServerSocket(PORT);

                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        Log.i(TAG, "New connection accepted");
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String inputLine;

                        while ((inputLine = in.readLine()) != null) {
                            Log.i(TAG, "Received: " + inputLine);
                        }
                        Log.i(TAG, "Connection closed");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "serverSocketRunnable: IOException " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "serverSocketRunnable: IOException " + e.getMessage());
            }
        }
    };

    private void sendToRPi(final String message) {
        Runnable clientSocketRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    // Open socket
                    InetAddress serverAddr = InetAddress.getByName(IP);
                    Socket clientSocket = new Socket(serverAddr, PORT);

                    // Send message
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(clientSocket.getOutputStream())),
                            true);
                    out.println(message);

                    clientSocket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    Log.e(TAG, "clientSocketRunnable: UnknownHostException: " + e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "clientSocketRunnable: IOException: " + e.getMessage());
                }  catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                }

            }
        };

        (new Thread(clientSocketRunnable)).start();
    }

    private void sendFile(final String filename) {
        Runnable sendFileRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    AudioFileFrame audioFileFrame = new AudioFileFrame();
                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

                    // Open file
                    File file = new File(getExternalCacheDir(), filename);
                    audioFileFrame.name = filename;
                    audioFileFrame.totalSize = file.length();

                    // Send file
                    Gson gson = new Gson();
                    final int BUF_SIZE = 1024 * 16;
                    byte[] fileFrame = new byte[BUF_SIZE];
                    InputStream in = new FileInputStream(file);
                    int frameSize;
                    while ((frameSize = in.read(fileFrame)) > 0) {
                        // Open socket
                        InetAddress serverAddr = InetAddress.getByName(IP);
                        Socket clientSocket = new Socket(serverAddr, PORT);
                        OutputStream out = clientSocket.getOutputStream();

                        audioFileFrame.frameSize = frameSize;
                        byte[] header = gson.toJson(audioFileFrame).getBytes();
                        byte[] buffer = new byte[header.length + frameSize];

                        System.arraycopy(header, 0, buffer, 0, header.length);
                        System.arraycopy(fileFrame, 0, buffer, header.length, frameSize);
                        out.write(buffer, 0, header.length + frameSize);

                        // Close socket
                        out.flush();
                        out.close();
                        clientSocket.close();
                    }

                    // Delete file
                    in.close();
                    file.delete();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    Log.e(TAG, "clientSocketRunnable: UnknownHostException: " + e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "clientSocketRunnable: IOException: " + e.getMessage());
                }  catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                }
            }
        };

        (new Thread(sendFileRunnable)).start();
    }

    /*************************************************************
     * communication with other services or activities
     *************************************************************/
    public static final String SERVICE_NAME = RPiCommService.class.getSimpleName();
    public static final String ACT_SEND = "send";
    public static final String ACT_FILE = "file";
    public static final String ACT_TERMINATE = "terminate";

    // callback for message reception from other service/activity
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String action = intent.getStringExtra(IEX_ACTION);
            Log.i(TAG, "Action: " + action);

            switch (action) {
                case ACT_SEND:
                    String msg = intent.getStringExtra(ServiceComm.IEX_MESSAGE);
                    sendToRPi(msg);
                    break;

                case ACT_FILE:
                    String fileName = intent.getStringExtra(ServiceComm.IEX_MESSAGE);
                    sendFile(fileName);
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
