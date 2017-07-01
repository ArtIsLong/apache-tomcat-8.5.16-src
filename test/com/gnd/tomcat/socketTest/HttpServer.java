package com.gnd.tomcat.socketTest;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * Created by 陈敏 on 2017/7/1.
 */
public class HttpServer {

    private final static String CRTL = "\r\n";

    public static void main(String[] args) throws Exception {
        // 创建ServerSocketChannel，监听8080端口
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(8080));
        // 设置为非阻塞模式
        serverSocketChannel.configureBlocking(false);
        // 为serverSocketChannel注册选择器
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        // 创建处理器
        while (true) {
            // 等待请求，每次等待阻塞3s，超过3s后线程继续向下运行，如果传入0或者不传则一直阻塞
            if (selector.select(3000) == 0) {
                continue;
            }
            // 获取待处理的SelectionKey
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();
                // 启动新线程处理SelectionKey
                new Thread(new HttpHandler(key)).run();
                // 处理完后，从待处理的SelectionKey迭代器中移除当前所使用的key
                keyIter.remove();
            }
        }
    }

    private static class HttpHandler implements Runnable {
        private int bufferSize = 1024;
        private String localCharset = "UTF-8";
        private SelectionKey key;

        public HttpHandler(SelectionKey key) {
            this.key = key;
        }

        public void handleAccept() throws Exception {
            SocketChannel clientChannel = ((ServerSocketChannel)key.channel()).accept();
            clientChannel.configureBlocking(false);
            clientChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(bufferSize));
        }

        public void handleRead() throws Exception {
            // 获取channel
            SocketChannel socketChannel = (SocketChannel)key.channel();
            // 获取buffer并重置
            ByteBuffer buffer = (ByteBuffer)key.attachment();
            buffer.clear();
            // 没有读到内容则关闭
            if (socketChannel.read(buffer) == -1) {
                socketChannel.close();
            } else {
                // 接收请求数据
                buffer.flip();
                String receivedString = Charset.forName(localCharset).newDecoder().decode(buffer).toString();
                // 控制台打印请求报文头
                String[] requestMessage = receivedString.split("\r\n");
                for (String s : requestMessage) {
                    System.out.println(s);
                    // 遇到空行说明报文头已经打印完
                    if (s.isEmpty()) {
                        break;
                    }
                }

                // 控制台打印首行信息
                String[] firstLine = requestMessage[0].split(" ");
                System.out.println();
                System.out.println("Method:\t" + firstLine[0]);
                System.out.println("url:\t" + firstLine[1]);
                System.out.println("HTTP Version:\t" + firstLine[2]);
                System.out.println();

                // 返回客户端
                StringBuilder sendString = new StringBuilder();
                sendString.append("HTTP/1.1 200 OK" + CRTL);
                sendString.append("Content-Type:text/html;charset=" + localCharset + CRTL);
                sendString.append(CRTL);
                sendString.append("<html><head><title>显示报文</title></head><body>");
                for (String s : requestMessage) {
                    sendString.append(s + "<br>");
                }
                sendString.append("</body></html>");
                buffer = ByteBuffer.wrap(sendString.toString().getBytes(localCharset));
                socketChannel.write(buffer);
                socketChannel.close();
            }
        }

        @Override
        public void run() {
            try {
                // 接收到连接请求时
                if (key.isAcceptable()) {
                    handleAccept();
                }
                // 读数据
                if (key.isReadable()) {
                    handleRead();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
