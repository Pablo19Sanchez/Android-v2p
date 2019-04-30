package com.example.v2x_safety.comm;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.v2x_safety.FirstActivity;
import com.example.v2x_safety.entity.MessageV2X;
import com.example.v2x_safety.modeActivity;

import java.io.InputStream;

public class LoginRegistrationAsyncTask extends AsyncTask<String, String, TCPClient> {
    private final MessageV2X message;
    private TCPClient tcpClient;
    private Handler mHandler;
    private static final String TAG = "Login/Registration"; // Test purposes
    private InputStream keyin;
    private Context context;

    /**
     * ShutdownAsyncTask constructor with handler passed as argument. The UI is updated via handler.
     * In doInBackground(...) method, the handler is passed to TCPClient object.
     * @param mHandler Handler object that is retrieved from MainActivity class and passed to TCPClient
     *                 class for sending messages and updating UI.
     */
    public LoginRegistrationAsyncTask(Handler mHandler, MessageV2X message, InputStream keyin){
        this.mHandler = mHandler;
        this.message = message;
        this.keyin = keyin;
    }

    /**
     * Override method from AsyncTask class. There the TCPClient object is created.
     * @param params From MainActivity class empty string is passed.
     * @return TCPClient object for closing it in onPostExecute method.
     */
    @Override
    protected TCPClient doInBackground(String... params) {
        try{
            tcpClient = new TCPClient(mHandler, message, keyin, new TCPClient.ServerAnswer() {
                @Override
                public void callbackMessageReceiver(String message) {
                    publishProgress(message);
                }
            });
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        tcpClient.run();
        return null;
    }

    /**
     * Overriden method from AsyncTask class. Here we're checking if server answered properly.
     */
    @Override
    protected void onProgressUpdate(String... answer) {
        super.onProgressUpdate(answer);
        Log.d(TAG, "Answer Received: " + answer[0]);
        String[] messageRec = answer[0].split("%");
        // We check what was the message sent: BYE or LOGIN/REGISTER
        switch (message.getType()){
            case "HELLO":
                if(messageRec[0].equals("OK")){
                    String sessionID = messageRec[1];
                    String iv = messageRec[2];
                    Message msg = mHandler.obtainMessage(FirstActivity.OK);
                    msg.what = FirstActivity.OK;
                    Bundle bundle = new Bundle();
                    bundle.putString("SESSIONID", sessionID);
                    bundle.putString("IV", iv);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                    tcpClient.stopClient();
                } else if(messageRec[0].equals("NOK")) {
                    mHandler.sendEmptyMessage(FirstActivity.NOK);
                    tcpClient.stopClient();
                } else{
                    mHandler.sendEmptyMessage(FirstActivity.ERROR);
                    tcpClient.stopClient();
                }
                break;
            case "BYE":
                if (messageRec[0].equals("OK")){
                    mHandler.sendEmptyMessage(modeActivity.BYE);
                    tcpClient.stopClient();
                }
                else if (messageRec[0].equals("NOK")){
                    mHandler.sendEmptyMessage(modeActivity.BYE);
                    tcpClient.stopClient();
                }
                break;
        }
    }

    @Override
    protected void onPostExecute(TCPClient result){
        super.onPostExecute(result);
        Log.d(TAG, "In on post execute");
        if(result != null && result.isRunning()){
            result.stopClient();
        }
    }
}
