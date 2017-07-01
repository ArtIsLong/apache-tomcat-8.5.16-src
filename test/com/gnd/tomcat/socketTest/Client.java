package com.gnd.tomcat.socketTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by 陈敏 on 2017/6/30.
 */
public class Client {
    public static void main(String[] args){
        try {
            Socket socket = new Socket("127.0.0.1", 8080);
            PrintWriter pw = new PrintWriter(socket.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pw.println("Client Hello");
            pw.flush();
            System.out.println(br.readLine());
            pw.close();
            br.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
