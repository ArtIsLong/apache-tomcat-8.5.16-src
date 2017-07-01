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
 * Created by 陈敏 on 2017/6/30.
 */
public class NIOServer {
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
        Handler handler = new Handler(1024);
        while (true) {
            // 等待请求，每次等待阻塞3s，超过3s后线程继续向下运行，如果传入0或者不传参数将一直阻塞
            if (selector.select(3000) == 0) {
                System.out.println("等待请求超时。。。。。。");
                continue;
            }
            System.out.println("处理请求。。。。。。");
            // 获取待处理额SelectionKey
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();
                try {
                    // 接收到连接请求时
                    if (key.isAcceptable()) {
                        handler.handleAccept(key);
                    }
                    // 读数据
                    if (key.isReadable()) {
                        handler.handleRead(key);
                    }
                } catch (Exception e) {
                    keyIter.remove();
                    continue;
                }
                // 处理完后，从待处理的SelectionKey迭代器中移除当前所使用的key
                keyIter.remove();
            }
        }
    }

    private static class Handler {
        private int bufferSize = 1024;
        private String localCharset = "UTF-8";
        public Handler(){}
        public Handler(int bufferSize) {
            this(bufferSize, null);
        }
        public Handler(String localCharset) {
            this(-1, localCharset);
        }

        public Handler(int bufferSize, String localCharset) {
            if (bufferSize > 0) {
                this.bufferSize = bufferSize;
            }
            if (localCharset != null) {
                this.localCharset = localCharset;
            }
        }

        public void handleAccept(SelectionKey key) throws Exception {
            SocketChannel socketChannel = ((ServerSocketChannel)key.channel()).accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(bufferSize));
        }

        public void handleRead(SelectionKey key) throws Exception {
            // 获取Channel
            SocketChannel channel = (SocketChannel) key.channel();
            // 获取Buffer并重置
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            buffer.clear();
            // 没有读到内容则关闭
            if (channel.read(buffer) == -1) {
                channel.close();
            } else {
                // 将Buffer转换为读状态
                buffer.flip();
                // 将buffer中接收到的值按localCharset格式编码后保存到receivedString
                String receivedString = Charset.forName(localCharset).newDecoder().decode(buffer).toString();

                // 返回数据给客户端
                String sendString = "received data:" + receivedString;
                buffer = ByteBuffer.wrap(sendString.getBytes(localCharset));
                channel.write(buffer);
                channel.close();
            }
        }
    }
}
