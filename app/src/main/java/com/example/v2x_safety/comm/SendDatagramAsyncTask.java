package com.example.v2x_safety.comm;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.example.v2x_safety.entity.MessageV2X;
import com.example.v2x_safety.modeActivity;

public class SendDatagramAsyncTask extends AsyncTask<String, String, String> {

    private UDPClient udpClient;
    private MessageV2X message;
    private Handler mHandler;

    public SendDatagramAsyncTask(MessageV2X message, Handler mHandler, UDPClient udpClient){
        this.message = message;
        this.mHandler = mHandler;
        this.udpClient = udpClient;
    }

    @Override
    protected String doInBackground(String... strings) {
        udpClient.setListener(new UDPClient.MessageSent() {
            @Override
            public void callbackMessageReceiver() {
                publishProgress("DONE");
            }
        });
        udpClient.sendData(message);
        Log.d("UDP", "Message Sent: " + message);
        return null;
    }

    /**
     * Overriden method from AsyncTask class. Here we're checking if server answered properly.
     */
    @Override
    protected void onProgressUpdate(String... answer) {
        super.onProgressUpdate(answer);
        mHandler.sendEmptyMessage(modeActivity.OK);
    }
}


