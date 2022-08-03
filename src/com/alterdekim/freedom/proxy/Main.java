package com.alterdekim.freedom.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args){
        new Main();
    }

    public Main() {
        File config = new File("config.json");
        if( config.exists() ) {
            try {
                Scanner scanner = new Scanner(new FileInputStream(config));
                String c = "";
                String line = "";
                try {
                    while ((line = scanner.nextLine()) != null) {
                        c += line;
                    }
                } catch ( Exception e ) {
                    //e.printStackTrace();
                }
                JSONObject cObj = new JSONObject(c);
                Settings.reseed_server_url = cObj.get("reseed").toString();
                Settings.reseed_local_path = cObj.get("exit-node-path").toString();
                Settings.reseed_server_key = cObj.get("reseed-server-key").toString();
                Settings.data_path = cObj.get("data-path").toString();
                Settings.exit_node = cObj.get("exit-node").toString();
                Settings.exit_node_mode = cObj.get("exit-node-mode").toString();
                Settings.exit_node_address = cObj.get("exit-node-address").toString();
                Settings.port = cObj.get("port").toString();
                scanner.close();


                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

                String originalJson = c;
                JsonNode tree = objectMapper .readTree(originalJson);
                String formattedJson = objectMapper.writeValueAsString(tree);

                PrintWriter pw = new PrintWriter(new FileOutputStream(new File("config.json")));
                pw.println(formattedJson);
                pw.flush();
                pw.close();
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        } else {
            try {
                config.createNewFile();
                PrintWriter pw = new PrintWriter(new FileOutputStream(config));
                JSONObject c = new JSONObject();
                c.put("port", Const.port);
                c.put("reseed", Const.reseed_server_url);
                c.put("reseed-server-key", Const.reseed_server_key);
                c.put("data-path", Const.data_path);
                c.put("exit-node", Const.exit_node);
                c.put("exit-node-mode", Const.exit_node_mode);
                c.put("exit-node-path", Const.reseed_local_path);
                c.put("exit-node-address", Const.exit_node_address);
                pw.println(c.toString());
                pw.flush();
                pw.close();
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }
        init();

        System.out.println("Proxy started on port "+Settings.port);
        Proxy myProxy = new Proxy(Integer.parseInt(Settings.port));
        myProxy.listen();
    }

    private void init() {
        new File(Settings.data_path).mkdirs();
        new File(Settings.reseed_local_path).mkdirs();
        if( !new File( Settings.data_path + "/crypt.json" ).exists() ) {
            RSAKeyPair keyPair = RSA.generateRSA();
            String uuid = RSA.generateGUID();
            Settings.uuid = uuid;
            Settings.rsaKeyPair = keyPair;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("private_key", keyPair.getPrivateKey());
            jsonObject.put("public_key", keyPair.getPublicKey());
            jsonObject.put("uuid", uuid);
            try {
                new File(Settings.data_path + "/hostname.txt").createNewFile();
                PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Settings.data_path + "/hostname.txt")));
                pw.println(RSA.SHA256(keyPair.getPublicKey())+".freedom");
                pw.flush();
                pw.close();
            } catch ( Exception e ) {
                e.printStackTrace();
            }
            try {
                new File(Settings.data_path + "/crypt.json").createNewFile();
                PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Settings.data_path + "/crypt.json")));
                pw.println(jsonObject.toString());
                pw.flush();
                pw.close();
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        } else {
            try {
                Scanner scanner = new Scanner(new FileInputStream(new File(Settings.data_path + "/crypt.json")));
                String line = "";
                String data = "";
                try {
                    while ((line = scanner.nextLine()) != null) {
                        data += line;
                    }
                } catch ( Exception e ) {
                  //  e.printStackTrace();
                }
                JSONObject jsonObject = new JSONObject(data);
                RSAKeyPair keyPair = new RSAKeyPair( jsonObject.get("private_key").toString(), jsonObject.get("public_key").toString() );
                String uuid = jsonObject.get("uuid").toString();
                Settings.rsaKeyPair = keyPair;
                Settings.uuid = uuid;
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }
        try {
            Const.exitNode = new ExitNode(Settings.reseed_server_url, 8080 );
            Const.exitNode.start();
            Const.reseedClient = new ReseedClient( Settings.reseed_server_url, 8080);
            Const.reseedClient.start();
        } catch ( Exception e)  {
            e.printStackTrace();
        }
    }
}
