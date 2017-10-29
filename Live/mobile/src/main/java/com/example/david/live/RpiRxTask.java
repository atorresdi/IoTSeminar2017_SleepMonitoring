package com.example.david.live;

import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by alvaro on 10/22/17.
 */

public class RpiRxTask implements Runnable {

    private static final int PORT = 5555;
    private volatile boolean shouldRun = true;

    private Handler handler;

    public void terminate() {
        shouldRun = false;
    }

    public RpiRxTask(Handler h) {
        handler = h;
    }

    @Override
    public void run() {
        // Moves the current Thread into the background
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);

        // Initialize server socket
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);

            while (shouldRun) {
                try {
                    Socket socket = serverSocket.accept();
                    Log.i("CommProtocol", "New connection accepted");
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        Log.i("CommProtocol", "Received: " + inputLine);

                        Message message = new Message();
                        message.obj = inputLine;
                        handler.sendMessage(message);
                    }

                    Log.i("CommProtocol", "Connection closed");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("CommProtocol", "IOException " + e.getMessage());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CommProtocol", "IOException " + e.getMessage());
        }
    }
}
