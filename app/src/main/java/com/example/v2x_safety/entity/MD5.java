package com.example.v2x_safety.entity;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {
    private String username, password;
    private String secret = "V2X";

    public MD5(String username, String password){
        this.username = username;
        this.password = password;
    }

    public String generateHash() {
        String md5 = username + password + secret;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes("UTF-8"));
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        } catch(UnsupportedEncodingException ex){
        }
        return null;
    }
}
