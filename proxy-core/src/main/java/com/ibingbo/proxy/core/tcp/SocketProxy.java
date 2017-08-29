package com.ibingbo.proxy.core.tcp;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import com.ibingbo.proxy.core.log.Logger;

/**
 * Created by bing on 17/8/28.
 */
public class SocketProxy extends Thread {

    private Class<?> handlerClass;
    private int port;

    public void startProxy(final int port, final Class proxyHandlerClass) {
        this.port = port;
        this.handlerClass = proxyHandlerClass;
        start();

    }

    @Override
    public void run() {
        ServerSocket serverSocket;
        Socket clientSocket;
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                try {
                    Logger.writeLog("proxy server started on %s", port);
                    Class[] types = new Class[] {Socket.class};
                    Constructor constructor = handlerClass.getDeclaredConstructor(types);
                    clientSocket = serverSocket.accept();
                    constructor.newInstance(new Object[] {clientSocket});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
