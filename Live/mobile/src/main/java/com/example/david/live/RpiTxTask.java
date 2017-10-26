package com.example.david.live;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by alvaro on 10/23/17.
 */

class RpiTxTask extends AsyncTask<String, Void, Void> {

    @Override
    protected Void doInBackground(String... args) {
        try {
            if (args.length != 3)
                throw new Exception("Invalid arguments number");

            final String SERVER_IP = args[0];
            final int PORT = Integer.parseInt(args[1]);
            final String MESSAGE = args[2];

            // Open socket
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            Socket clientSocket = new Socket(serverAddr, PORT);

            // Send message
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(clientSocket.getOutputStream())),
                    true);
            out.println(MESSAGE);

            clientSocket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e("RpiTxTask", "UnknownHostException: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("RpiTxTask", "IOException: " + e.getMessage());
        }  catch (Exception e) {
            e.printStackTrace();
            Log.e("RpiTxTask", e.getMessage());
        }

        return null;
    }
}
