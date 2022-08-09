package com.alterdekim.freedom.proxy;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class ExitNode extends Thread {
    private String address;
    private int port;

    private PrintWriter pw;

    private ExitNodeTunnel en;

    public ExitNode( String address, int port ) {
        this.address = address;
        this.port = port;
    }

    public void write(String str) {
        pw.println(str);
        pw.flush();
    }

    @Override
    public void run() {
        init();
    }

    private void init() {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(address, port), 30000);
            socket.setSoTimeout(30000);
            pw = new PrintWriter(socket.getOutputStream());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("act", "login");
            jsonObject.put("password", ECC.ECCEncode(Settings.uuid, Settings.reseed_server_key));
            jsonObject.put("public_key", Settings.rsaKeyPair.getPublicKey());
            pw.println(jsonObject.toString());
            pw.flush();
            Scanner scanner = new Scanner(socket.getInputStream());
            String line = "";
            try {
                while ((line = scanner.nextLine()) != null) {
                    try {
                        JSONObject jsonObject1 = new JSONObject(line);
                        if (jsonObject1.get("act").toString().equals("endgate_created")) {
                            new ExitNodeTunnel(jsonObject1.get("ip").toString(), jsonObject1.get("port").toString(), jsonObject1.get("uuid_hash").toString()).start();
                        }
                    } catch ( Exception e ) {
                        e.printStackTrace();
                    }
                }
            } catch ( Exception e ) {

            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}