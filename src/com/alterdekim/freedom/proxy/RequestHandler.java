package com.alterdekim.freedom.proxy;

import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import javax.imageio.ImageIO;

public class RequestHandler implements Runnable {

    /**
     * Socket connected to client passed by Proxy server
     */
    Socket clientSocket;

    /**
     * Read data client sends to proxy
     */
    BufferedReader proxyToClientBr;

    /**
     * Send data from proxy to client
     */
    BufferedWriter proxyToClientBw;


    /**
     * Thread that is used to transmit data read from client to server when using HTTPS
     * Reference to this is required so it can be closed once completed.
     */
    private Thread httpsClientToServer;


    /**
     * Creates a ReuqestHandler object capable of servicing HTTP(S) GET requests
     * @param clientSocket socket connected to the client
     */
    public RequestHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
        try{
            this.clientSocket.setSoTimeout(10000);
            proxyToClientBr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            proxyToClientBw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }



    /**
     * Reads and examines the requestString and calls the appropriate method based
     * on the request type.
     */
    @Override
    public void run() {

        // Get Request from client
        String post_str = "";
        String requestString;
        try{
            requestString = proxyToClientBr.readLine();
            String line = "";
            int length = 0;
            while( !(line = proxyToClientBr.readLine()).equals("") ) {
                if( line.startsWith("Content-Length: ") ) {
                    length = Integer.parseInt(line.substring(16));
                }
            }
            try {
                for( int i = 0; i < length; i++ ) {
                   post_str += (char)proxyToClientBr.read();
                }
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error reading request from client");
            return;
        }
        // Parse out URL

        System.out.println("Request Received " + requestString);
        // Get the Request type
        String request = requestString.substring(0,requestString.indexOf(' '));

        // remove request type and space
        String urlString = requestString.substring(requestString.indexOf(' ')+1);

        // Remove everything past next space
        urlString = urlString.substring(0, urlString.indexOf(' '));

        // Prepend http:// if necessary to create correct URL
        if(!urlString.substring(0,4).equals("http")){
            String temp = "http://";
            urlString = temp + urlString;
        }

        // Check request type
        if(request.equals("CONNECT")){
        } else {
            System.out.println("HTTP GET/POST for : " + urlString + "\n");
            sendNonCachedToClient(urlString, post_str);
        }
    }

    private String public_key;
    private void sendNonCachedToClient(String url, String post_str){

        try{
            if( Utils.getDomainZone(url+"").equals("freedom") ) {
                String name = Utils.getDomainName(url+"").split("\\.freedom")[0];
                this.public_key = name;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("act", "connect");
                jsonObject.put("destination", name);
                System.out.println("Fetching info about transmitter for: " + name);
                Const.reseedClient.write(new ReseedResponseListener(jsonObject, new IReseedResponse() {
                    @Override
                    public void response(String str) {
                        JSONObject jsonObject1 = new JSONObject(str);
                        if( jsonObject1.get("act").toString().equals("host_not_found") ) {
                            try {
                                BufferedReader br = new BufferedReader(new InputStreamReader(
                                        this.getClass().getResourceAsStream("/nf.html")));
                                String data = "";
                                String line;
                                while ((line = br.readLine()) != null) {
                                    data += line;
                                }

                                proxyToClientBw.write(data);
                                if(proxyToClientBw != null){
                                    proxyToClientBw.close();
                                }
                            } catch ( Exception e ) {
                                e.printStackTrace();
                            }
                        } else if( jsonObject1.get("act").toString().equals("tunnel_not_found") ) {
                            try {
                                BufferedReader br = new BufferedReader(new InputStreamReader(
                                        this.getClass().getResourceAsStream("/tnf.html")));
                                String data = "";
                                String line;
                                while ((line = br.readLine()) != null) {
                                    data += line;
                                }

                                proxyToClientBw.write(data);
                                if(proxyToClientBw != null){
                                    proxyToClientBw.close();
                                }
                            } catch ( Exception e ) {
                                e.printStackTrace();
                            }
                        } else if( jsonObject1.get("act").toString().equals("tunnel_created") ) {
                            new DataTunnel(public_key, jsonObject1.get("ip").toString(), jsonObject1.get("port").toString(), url + "", post_str, new IResponseListener() {
                                @Override
                                public void response(String d, String mime) {
                                    if( d.equals("not_found") ) {
                                        try {
                                            BufferedReader br = new BufferedReader(new InputStreamReader(
                                                    this.getClass().getResourceAsStream("/nf.html")));
                                            String data = "";
                                            String line;
                                            while ((line = br.readLine()) != null) {
                                                data += line;
                                            }
                                            proxyToClientBw.write(data);
                                            if(proxyToClientBw != null){
                                                proxyToClientBw.close();
                                            }
                                        } catch ( Exception e ) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        try {
                                            proxyToClientBw.write(d);
                                            if(proxyToClientBw != null){
                                                proxyToClientBw.close();
                                            }
                                        } catch ( Exception e ) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }).start();
                        }
                    }
                }));
            }
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * Handles HTTPS requests between client and remote server
     * @param urlString desired file to be transmitted over https
     */
    private void handleHTTPSRequest(String urlString){
        // Extract the URL and port of remote
        String url = urlString.substring(7);
        String pieces[] = url.split(":");
        url = pieces[0];
        int port  = Integer.valueOf(pieces[1]);

        try{
            // Only first line of HTTPS request has been read at this point (CONNECT *)
            // Read (and throw away) the rest of the initial data on the stream
            for(int i=0;i<5;i++){
                proxyToClientBr.readLine();
            }

            // Get actual IP associated with this URL through DNS
            InetAddress address = InetAddress.getByName(url);

            // Open a socket to the remote server
            Socket proxyToServerSocket = new Socket(address, port);
            proxyToServerSocket.setSoTimeout(5000);

            // Send Connection established to the client
            String line = "HTTP/1.0 200 Connection established\r\n" +
                    "Proxy-Agent: ProxyServer/1.0\r\n" +
                    "\r\n";
            proxyToClientBw.write(line);
            proxyToClientBw.flush();



            // Client and Remote will both start sending data to proxy at this point
            // Proxy needs to asynchronously read data from each party and send it to the other party


            //Create a Buffered Writer betwen proxy and remote
            BufferedWriter proxyToServerBW = new BufferedWriter(new OutputStreamWriter(proxyToServerSocket.getOutputStream()));

            // Create Buffered Reader from proxy and remote
            BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerSocket.getInputStream()));



            // Create a new thread to listen to client and transmit to server
            ClientToServerHttpsTransmit clientToServerHttps =
                    new ClientToServerHttpsTransmit(clientSocket.getInputStream(), proxyToServerSocket.getOutputStream());

            httpsClientToServer = new Thread(clientToServerHttps);
            httpsClientToServer.start();


            // Listen to remote server and relay to client
            try {
                byte[] buffer = new byte[4096];
                int read;
                do {
                    read = proxyToServerSocket.getInputStream().read(buffer);
                    if (read > 0) {
                        clientSocket.getOutputStream().write(buffer, 0, read);
                        if (proxyToServerSocket.getInputStream().available() < 1) {
                            clientSocket.getOutputStream().flush();
                        }
                    }
                } while (read >= 0);
            }
            catch (SocketTimeoutException e) {

            }
            catch (IOException e) {
                e.printStackTrace();
            }


            // Close Down Resources
            if(proxyToServerSocket != null){
                proxyToServerSocket.close();
            }

            if(proxyToServerBR != null){
                proxyToServerBR.close();
            }

            if(proxyToServerBW != null){
                proxyToServerBW.close();
            }

            if(proxyToClientBw != null){
                proxyToClientBw.close();
            }


        } catch (SocketTimeoutException e) {
            String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
                    "User-Agent: ProxyServer/1.0\n" +
                    "\r\n";
            try{
                proxyToClientBw.write(line);
                proxyToClientBw.flush();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        catch (Exception e){
            System.out.println("Error on HTTPS : " + urlString );
            e.printStackTrace();
        }
    }




    /**
     * Listen to data from client and transmits it to server.
     * This is done on a separate thread as must be done
     * asynchronously to reading data from server and transmitting
     * that data to the client.
     */
    class ClientToServerHttpsTransmit implements Runnable{

        InputStream proxyToClientIS;
        OutputStream proxyToServerOS;

        /**
         * Creates Object to Listen to Client and Transmit that data to the server
         * @param proxyToClientIS Stream that proxy uses to receive data from client
         * @param proxyToServerOS Stream that proxy uses to transmit data to remote server
         */
        public ClientToServerHttpsTransmit(InputStream proxyToClientIS, OutputStream proxyToServerOS) {
            this.proxyToClientIS = proxyToClientIS;
            this.proxyToServerOS = proxyToServerOS;
        }

        @Override
        public void run(){
            try {
                // Read byte by byte from client and send directly to server
                byte[] buffer = new byte[4096];
                int read;
                do {
                    read = proxyToClientIS.read(buffer);
                    if (read > 0) {
                        proxyToServerOS.write(buffer, 0, read);
                        if (proxyToClientIS.available() < 1) {
                            proxyToServerOS.flush();
                        }
                    }
                } while (read >= 0);
            }
            catch (SocketTimeoutException ste) {
                // TODO: handle exception
            }
            catch (IOException e) {
                System.out.println("Proxy to client HTTPS read timed out");
                e.printStackTrace();
            }
        }
    }
}




