package com.example.v2x_safety.comm;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.example.v2x_safety.entity.MessageV2X;

import org.apache.http.conn.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;
import java.security.KeyStore;

import javax.net.ssl.SSLSocket;


public class TCPClient {
    private static final String TAG = "TCPClient";
    private final Handler mHandler;
    private String incomingMessage;
    private SSLSocket socket;
    private BufferedWriter out = null;
    private BufferedReader in = null;
    private char keystorepass[] = "v2x12345".toCharArray();
    private InputStream keyin;
    private MessageV2X message;
    private Context context;

    String ip = "192.168.1.200";
    //String ip = "130.229.138.84";
    int port = 9999;

    private ServerAnswer listener = null;
    private boolean mRun = false;

    /**
     * TCPClient class constructor, which is created in AsyncTasks we click Registration or Login buttons.
     * @param mHandler Handler passed as an argument for updating the UI with sent messages
     * @param message
     * @param listener Callback interface object
     */
    public TCPClient(Handler mHandler, MessageV2X message, InputStream keyin, ServerAnswer listener) {
        this.listener = listener;
        this.message = message ;
        this.mHandler = mHandler;
        this.keyin = keyin;
    }

    /**
     * Public method for sending the message via OutputStream object.
     * @param message Message passed as an argument and sent via OutputStream object.
     */
    public void sendMessage(String message) throws IOException {
        out.write(message);
        out.flush();
        Log.d(TAG, "Message Sent: " + message);
    }

    /**
     * Public method for stopping the TCPClient object (and finalizing it after that) from AsyncTask
     */
    public void stopClient(){
        mRun = false;
    }

    public boolean isRunning(){
        return mRun;
    }

    public void run() {
        mRun = true;
        try {
            KeyStore ks = KeyStore.getInstance("BKS");
            ks.load(keyin, keystorepass);
            SSLSocketFactory socketFactory = new SSLSocketFactory(ks);
            socketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            socket = (SSLSocket) socketFactory.createSocket(new Socket(ip, port), ip, port, false);
            socket.startHandshake();
            String command = message.getInitialMessage();
            try {
                // Create BufferedWriter object for sending messages to server.
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                //Create BufferedReader object for receiving messages from server.
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //Sending message with command specified by AsyncTask
                this.sendMessage(command);
                //Listen for the incoming messages while mRun = true
                while (mRun) {
                    incomingMessage = in.readLine();
                    if (incomingMessage != null && listener != null) {
                        /*
                         * Incoming message is passed to MessageCallback object.
                         * Next it is retrieved by AsyncTask and passed to onPublishProgress method.
                         */
                        listener.callbackMessageReceiver(incomingMessage);
                    }
                    incomingMessage = null;
                }
            } catch (Exception e) {
                Log.d(TAG, "Error", e);
            } finally {
                out.flush();
                out.close();
                in.close();
                socket.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "Error", e);
        }
    }

    /*
     * Callback Interface for sending received messages to 'onPublishProgress' method in AsyncTask.
     */
    public interface ServerAnswer {
        /**
         * Method overriden in AsyncTask 'doInBackground' method while creating the TCPClient object.
         * @param message Received message from server app.
         */
        public void callbackMessageReceiver(String message);
    }
}
