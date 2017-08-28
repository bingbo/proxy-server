package com.ibingbo;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by bing on 17/8/25.
 */
public class App {
    public static void main(String[] args) {
        // http代理设置
        System.setProperty("http.proxyHost", "10.99.192.200");
        System.setProperty("http.proxyPort", "8084");

        // https代理设置
        //        System.setProperty("https.proxyHost", "10.99.192.200");
        //        System.setProperty("https.proxyPort", "8081");
        //
        //        // ftp代理设置
        //        System.setProperty("ftp.proxyHost", "");
        //        System.setProperty("ftp.proxyPort", "");
        //
        // tcp代理设置
        System.setProperty("socksProxyHost", "10.99.192.200");
        System.setProperty("socksProxyPort", "8084");

        try {
            String[] urls = new String[] {
                    "http://cp01-hotel-04.epc.baidu.com:8063"
            };
            for (String uri : urls) {
                URL url = new URL(uri);
                URLConnection connection = url.openConnection();
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                byte[] bytes = new byte[1024];
                while (inputStream.read(bytes) >= 0) {
                    System.out.println(new String(bytes));

                }
            }

            sendTcp();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendTcp() {
        System.out.println("客户端启动...");
        System.out.println("当接收到服务器端字符为 \"OK\" 的时候, 客户端将终止\n");
        while (true) {
            Socket socket = null;
            try {
                //创建一个流套接字并将其连接到指定主机上的指定端口号
                socket = new Socket("10.99.192.200", 8083);

                //读取服务器端数据
                DataInputStream input = new DataInputStream(socket.getInputStream());
                //向服务器端发送数据
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                System.out.print("请输入: \t");
                String str = new BufferedReader(new InputStreamReader(System.in)).readLine();
                out.writeUTF(str);

                String ret = input.readUTF();
                System.out.println("服务器端返回过来的是: " + ret);
                // 如接收到 "OK" 则断开连接
                if ("OK".equals(ret)) {
                    System.out.println("客户端将关闭连接");
                    Thread.sleep(500);
                    break;
                }

                out.close();
                input.close();
            } catch (Exception e) {
                System.out.println("客户端异常:" + e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        socket = null;
                        System.out.println("客户端 finally 异常:" + e.getMessage());
                    }
                }
            }
        }
    }
}
