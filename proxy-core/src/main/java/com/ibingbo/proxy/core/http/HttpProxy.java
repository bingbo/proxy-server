package com.ibingbo.proxy.core.http;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import com.ibingbo.proxy.core.log.Logger;

/**
 * Created by bing on 17/8/25.
 */
public class HttpProxy {

    private Properties properties;

    public HttpProxy(Properties properties) {
        this.properties = properties;
    }

    public void startProxy(final int port, final Class proxyHandlerClass) {
        new Thread(){
            @Override
            public void run() {
                ServerSocket serverSocket;
                Socket clientSocket;
                try {
                    serverSocket = new ServerSocket(port);
                    while (true) {
                        try {
                            Logger.writeLog("proxy server started on %s", port);
                            Class[] types = new Class[] {Socket.class, Properties.class};
                            Constructor constructor = proxyHandlerClass.getDeclaredConstructor(types);
                            clientSocket = serverSocket.accept();
                            constructor.newInstance(new Object[] {clientSocket, properties});
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
