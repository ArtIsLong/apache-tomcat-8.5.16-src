package com.gnd.tomcat.socketTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by 陈敏 on 2017/6/30.
 */
public class Client {
    public static void main(String[] args){
        try {
            Socket socket = new Socket("127.0.0.1", 8080);
//            PrintWriter pw = new PrintWriter(socket.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            char[] data = new char[102400];
            br.read(data);
            String str = new String(data);
            System.out.println(str);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
