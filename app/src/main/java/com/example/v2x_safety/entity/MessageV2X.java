package com.example.v2x_safety.entity;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import at.favre.lib.crypto.HKDF;

public class MessageV2X {

    private final String type, sessionID, keyUser, iv, mode, level, latitude, longitude;
    private final String sep = "%";
    private final String sep2 = "SEP";
    byte[] encKey, authKey, ivByte;

    public MessageV2X(String type, String secondInfo){
        this.type = type;
        switch (type){
            case "HELLO":
                this.sessionID = " ";
                this.keyUser = secondInfo;
                break;
            case "BYE":
                this.sessionID = secondInfo;
                this.keyUser = " ";
                break;
                default:
                    this.sessionID = " ";
                    this.keyUser = " ";
        }
        this.iv = " ";
        this.mode = " ";
        this.level = " ";
        this.latitude = " ";
        this.longitude = " ";
    }

    public MessageV2X(String type, String sessionID, String mode, int level, double latitude, double longitude, Context context, String keyUser, String iv){
        this.type = type;
        this.sessionID = sessionID;
        this.mode = mode;
        this.level = Integer.toString(level);
        this.latitude = Double.toString(latitude);
        this.longitude = Double.toString(longitude);
        this.keyUser = keyUser;
        this.iv = iv;
        deriveKeys(keyUser);
        ivByte = Base64.decode(iv, Base64.DEFAULT);
    }

    public String getInitialMessage(){
        String message = null;
        switch (type){
            case "HELLO":
                message = type + sep + keyUser + "\n";
                break;
            case "BYE":
                message = type + sep + sessionID + "\n";
        }
        return message;
    }

    public byte[] getWarningMessage(){
        String toEncrypt = mode + sep2 + level + sep2 + latitude + sep2 + longitude;
        byte[] encryptedMessage = encryptMessage(toEncrypt);
        byte[] macMessage = getMAC(Base64.encodeToString(encryptedMessage, Base64.NO_PADDING));
        String finalMessage = type + sep2 + sessionID + sep2 + Base64.encodeToString(encryptedMessage, Base64.NO_PADDING) + sep2 + Base64.encodeToString(macMessage, Base64.NO_PADDING) + "END";
        Log.d("Message", "Datagram to Send: " + finalMessage);
        return Base64.decode(finalMessage, Base64.DEFAULT);
    }

    public String getType(){
        return type;
    }

    private byte[] encryptMessage(String info){
        byte[] cipherText = null;
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encKey, "AES"), new IvParameterSpec(ivByte));
            cipherText = cipher.doFinal(info.getBytes(Charset.forName("UTF-8")));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return cipherText;
    }

    private byte[] getMAC(String message){
        String info = type + sep2 + sessionID + sep2 + message;
        SecretKey macKey = new SecretKeySpec(authKey, "HmacSHA256");
        Mac hmac = null;
        Log.d("MACKey", "Used: " + Base64.encodeToString(authKey, Base64.NO_WRAP));
        Log.d("IV", "Used: " + iv);
        try {
            hmac = Mac.getInstance("HmacSHA256");
            hmac.init(macKey);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        hmac.update(ivByte);
        byte [] mac = hmac.doFinal(info.getBytes(Charset.forName("UTF-8")));
        return mac;
    }

    private void deriveKeys(String key){
        byte[] keyBytes = Base64.decode(key, Base64.NO_PADDING);
        encKey = HKDF.fromHmacSha256().expand(keyBytes, "encKey".getBytes(Charset.forName("UTF-8")), 16);
        authKey = HKDF.fromHmacSha256().expand(keyBytes, "authKey".getBytes(Charset.forName("UTF-8")), 32); //HMAC-SHA256 key is 32 byte
    }

}
