package com.gnd.tomcat.socketTest;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by 陈敏 on 2017/6/30.
 */
public class Server {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(8080);
            Socket socket = serverSocket.accept();
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            System.out.println(br.readLine());
            bw.write("Hello, World!");
            bw.flush();
            bw.close();
            br.close();
            socket.close();
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
