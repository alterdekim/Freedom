package com.alterdekim.freedom.proxy;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class ReseedClient extends Thread {
    private String address;
    private int port;

    public ReseedClient( String address, int port ) {
        this.address = address;
        this.port = port;
    }

    public void write(ReseedResponseListener listener) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(address, port), 30000);
            socket.setSoTimeout(30000);
            PrintWriter pw = new PrintWriter(socket.getOutputStream());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("act", "login");
            jsonObject.put("password", RSA.RSAEncode(Settings.uuid, Settings.reseed_server_key));
            jsonObject.put("public_key", Settings.rsaKeyPair.getPublicKey());
            pw.println(jsonObject.toString());
            pw.flush();
            pw.println(listener.getJsonObject().toString());
            pw.flush();
            Scanner sc = new Scanner(socket.getInputStream());
            String line = "";
            try {
                while ((line = sc.nextLine()) != null) {
                    try {
                        listener.getListener().response(line);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch ( Exception e ) {
                e.printStackTrace();
            }
            socket.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(address, port), 30000);
            socket.setSoTimeout(30000);
            PrintWriter pw = new PrintWriter(socket.getOutputStream());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("act", "login");
            jsonObject.put("password", RSA.RSAEncode(Settings.uuid, Settings.reseed_server_key));
            jsonObject.put("public_key", Settings.rsaKeyPair.getPublicKey());
            pw.println(jsonObject.toString());
            pw.flush();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
