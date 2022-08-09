package com.alterdekim.freedom.proxy;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Scanner;

public class ExitNodeTunnel extends Thread {

    private String ip;

    private String port;

    private PrintWriter pw;

    private String uuid_hash;

    private byte[] key;

    public Socket socket;

    public ExitNodeTunnel( String ip, String port, String uuid_hash ) {
        this.ip = ip;
        this.port = port;
        this.uuid_hash = uuid_hash;
    }

    private void write( String str ) {
        pw.println(str);
        pw.flush();
    }

    @Override
    public void run() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, Integer.parseInt(port)));
            pw = new PrintWriter(socket.getOutputStream());

            JSONObject jsonObject5 = new JSONObject();
            jsonObject5.put("uuid", uuid_hash);
            pw.println(jsonObject5.toString());
            pw.flush();

            try {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                            JSONObject jsonObject = new JSONObject(line);
                            if (jsonObject.get("act").toString().equals("connected")) {
                                System.out.println("New exit node connection...");
                            } else if (jsonObject.get("act").toString().equals("exit")) {
                                socket.close();
                            } else if (jsonObject.get("act").toString().equals("cipher")) {
                                String data = jsonObject.get("data").toString();
                                byte[] encrypted = Base58.decode(data);
                                this.key = new RC4(ECC.SHA256(Settings.rsaKeyPair.getPublicKey()).getBytes()).decrypt(encrypted);
                                JSONObject jsonObject1 = new JSONObject();
                                jsonObject1.put("act", "url");
                                write(jsonObject1.toString());
                            } else if (jsonObject.get("act").toString().equals("url")) {
                                String url = ECC.AESDecode(jsonObject.get("data").toString(), new String(this.key));

                                if (Settings.exit_node_mode.equals("0")) {

                                    if (url.split("\\.freedom\\/").length > 1) {
                                        String subaddress = url.split("\\.freedom\\/")[1];
                                        File file = new File(Settings.reseed_local_path + "/" + subaddress);
                                        if (file.exists()) {
                                            Path path = file.toPath();
                                            String mimeType = Files.probeContentType(path);
                                            String str = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                                            JSONObject jsonObject1 = new JSONObject();
                                            jsonObject1.put("act", "result");
                                            jsonObject1.put("mime", ECC.AESEncode(mimeType, new String(key)));
                                            jsonObject1.put("data", ECC.AESEncode(str, new String(key)));

                                            write(jsonObject1.toString());
                                        } else {
                                            JSONObject jsonObject1 = new JSONObject();
                                            jsonObject1.put("act", "not_found");
                                            write(jsonObject1.toString());
                                        }
                                    } else {
                                        File file = new File(Settings.reseed_local_path + "/index.html");
                                        if (file.exists()) {
                                            Path path = file.toPath();
                                            String mimeType = Files.probeContentType(path);
                                            String str = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

                                            JSONObject jsonObject1 = new JSONObject();
                                            jsonObject1.put("act", "result");
                                            jsonObject1.put("mime", ECC.AESEncode(mimeType, new String(key)));
                                            jsonObject1.put("data", ECC.AESEncode(str, new String(key)));

                                            write(jsonObject1.toString());
                                        } else {
                                            JSONObject jsonObject1 = new JSONObject();
                                            jsonObject1.put("act", "not_found");
                                            write(jsonObject1.toString());
                                        }
                                    }
                                } else if (Settings.exit_node_mode.equals("1")) {
                                    if (jsonObject.has("query")) {
                                        String post_str = ECC.AESDecode(jsonObject.get("query").toString(), new String(this.key));

                                        String subaddress = "";
                                        if (url.split("\\.freedom\\/").length > 1) {
                                            subaddress = url.split("\\.freedom\\/")[1];
                                        }
                                        String working_url = Settings.exit_node_address + "/" + subaddress;

                                        System.out.println(working_url + " ->1");

                                        URL obj = new URL(working_url);
                                        URLConnection connection = null;
                                        if (working_url.startsWith("https://")) {
                                            connection = (HttpsURLConnection) obj.openConnection();
                                            ((HttpsURLConnection) connection).setRequestMethod("POST");
                                        } else {
                                            connection = (HttpURLConnection) obj.openConnection();
                                            ((HttpURLConnection) connection).setRequestMethod("POST");
                                        }
                                        connection.setDoOutput(true);
                                        connection.setRequestProperty("Accept", "*/*");
                                        OutputStream stream = connection.getOutputStream();
                                        stream.write(post_str.getBytes());

                                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                        String inputLine;
                                        StringBuffer response = new StringBuffer();

                                        while ((inputLine = in.readLine()) != null) {
                                            response.append(inputLine);
                                        }
                                        in.close();

                                        JSONObject jsonObject1 = new JSONObject();
                                        jsonObject1.put("act", "result");
                                        jsonObject1.put("mime", ECC.AESEncode("text/html", new String(key)));
                                        jsonObject1.put("data", ECC.AESEncode(response.toString(), new String(key)));

                                        write(jsonObject1.toString());

                                        if (working_url.startsWith("https://")) {
                                            ((HttpsURLConnection) connection).disconnect();
                                        } else {
                                            ((HttpURLConnection) connection).disconnect();
                                        }
                                    } else {
                                        String subaddress = "";
                                        if (url.split("\\.freedom\\/").length > 1) {
                                            subaddress = url.split("\\.freedom\\/")[1];
                                        }
                                        String working_url = Settings.exit_node_address + "/" + subaddress;

                                        System.out.println(working_url + " ->2");

                                        URL obj = new URL(working_url);
                                        URLConnection connection = null;
                                        if (working_url.startsWith("https://")) {
                                            connection = (HttpsURLConnection) obj.openConnection();
                                            ((HttpsURLConnection) connection).setRequestMethod("GET");
                                        } else {
                                            connection = (HttpURLConnection) obj.openConnection();
                                            ((HttpURLConnection) connection).setRequestMethod("GET");
                                        }

                                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                        String inputLine;
                                        StringBuffer response = new StringBuffer();

                                        while ((inputLine = in.readLine()) != null) {
                                            response.append(inputLine);
                                        }
                                        in.close();

                                        JSONObject jsonObject1 = new JSONObject();
                                        jsonObject1.put("act", "result");
                                        jsonObject1.put("mime", ECC.AESEncode("text/html", new String(key)));
                                        jsonObject1.put("data", ECC.AESEncode(response.toString(), new String(key)));

                                        write(jsonObject1.toString());

                                        if (working_url.startsWith("https://")) {
                                            ((HttpsURLConnection) connection).disconnect();
                                        } else {
                                            ((HttpURLConnection) connection).disconnect();
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch ( Exception e ) {
                    //e.printStackTrace();
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
    }
}
