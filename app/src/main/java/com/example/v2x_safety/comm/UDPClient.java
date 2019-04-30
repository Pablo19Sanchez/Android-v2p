package com.example.v2x_safety.comm;

import android.util.Log;

import com.example.v2x_safety.entity.MessageV2X;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPClient {

    //private final String ip = "192.168.1.200";
    private final String ip = "130.229.138.84";
    private final int port = 8888;
    private InetAddress serverAddr;
    private DatagramSocket udpSocket;
    private MessageSent listener;


    public UDPClient() {
        this.listener = null;
        try {
            serverAddr = InetAddress.getByName(ip);
            udpSocket = new DatagramSocket(port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void sendData(MessageV2X message){
        byte[] buf = message.getWarningMessage();
        try{
            DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, port);
            udpSocket.send(packet);
            listener.callbackMessageReceiver();
            Log.d("UDP", "Datagram Sent " + Integer.toString(packet.getLength()));
        } catch (IOException ex){
            Log.e("UDP:", "IO Error", ex);
        }
    }

    public void setListener(MessageSent listener){
        this.listener = listener;
    }

    /*
     * Callback Interface for sending received messages to 'onPublishProgress' method in AsyncTask.
     */
    public interface MessageSent {
        public void callbackMessageReceiver();
    }

}
