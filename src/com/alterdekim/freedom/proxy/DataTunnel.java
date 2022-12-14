package com.alterdekim.freedom.proxy;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Base64;
import java.util.Scanner;

public class DataTunnel extends Thread {

    private String ip;
    private String port;
    private String aes;
    private String public_key;
    private String url;
    private PrintWriter pw;
    private IResponseListener listener;
    private String post_str;

    public Socket socket;
    private String hash;
    public DataTunnel( String public_key, String ip, String port, String url, String post_str,String hash,  IResponseListener listener ) {
        this.ip = ip;
        this.port = port;
        this.url = url;
        this.hash = hash;
        this.listener = listener;
        this.public_key = public_key;
        this.post_str = post_str;
    }

    private void write( String line ) {
        pw.println(line);
        pw.flush();
    }

    @Override
    public void run() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(this.ip, Integer.parseInt(this.port)));
            this.pw = new PrintWriter(socket.getOutputStream());

            JSONObject jsonObject5 = new JSONObject();
            jsonObject5.put("uuid", hash);
            pw.println(jsonObject5.toString());
            pw.flush();

            String line = "";
            Scanner scanner = new Scanner(socket.getInputStream());
            try {
                while( ( line = scanner.nextLine() ) != null ) {
                    try {
                        JSONObject jsonObject = new JSONObject(line);
                        if (jsonObject.get("act").toString().equals("connected")) {
                            JSONObject jsonObject1 = new JSONObject();
                            jsonObject1.put("act", "cipher");
                            aes = ECC.generateAES();
                            jsonObject1.put("data", Base58.encode(new RC4(public_key.getBytes()).encrypt(aes.getBytes())));
                            write(jsonObject1.toString());
                        } else if( jsonObject.get("act").toString().equals("exit") ) {
                            socket.close();
                        } else if( jsonObject.get("act").toString().equals("url") ) {
                            JSONObject jsonObject1 = new JSONObject();
                            jsonObject1.put("act", "url");
                            jsonObject1.put("data", ECC.AESEncode(url, aes));
                            if( !post_str.equals("") ) {
                                jsonObject1.put("query", ECC.AESEncode(post_str, aes));
                            }
                            write(jsonObject1.toString());
                        } else if( jsonObject.get("act").toString().equals("result") ) {
                            String mime = ECC.AESDecode(jsonObject.get("mime").toString(), aes);
                            String data = ECC.AESDecode(jsonObject.get("data").toString(), aes);
                            listener.response(data, mime);

                            JSONObject jsonObject1 = new JSONObject();
                            jsonObject1.put("act", "exit");
                            write(jsonObject1.toString());
                        } else if( jsonObject.get("act").toString().equals("not_found") ) {
                            listener.response("not_found", "");

                            JSONObject jsonObject1 = new JSONObject();
                            jsonObject1.put("act", "exit");
                            write(jsonObject1.toString());
                        }
                    } catch ( Exception e ) {
                        System.out.println("Error " + line);
                        e.printStackTrace();
                    }
                }
            } catch ( Exception exception ) {
                exception.printStackTrace();
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
