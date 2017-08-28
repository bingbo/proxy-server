package com.ibingbo.proxy.core.tcp;

import java.util.Properties;

import com.ibingbo.proxy.core.config.SocketConfig;

/**
 * Created by bing on 17/8/25.
 */
public class TcpProxyBuilder {
    private static int CONNECT_RETRIES = 5;
    private static int CONNECT_PAUSE = 5;
    private static int TIMEOUT = 50;
    private static int BUFFER_SIZE = 1024;

    private static boolean OPEN_SOCK4 = true;
    private static boolean OPEN_SOCK5 = true;
    private static String USER = "";
    private static String PASSWORD = "";
    private static boolean LOGIN = false;

    private static Properties properties = new Properties();

    static {
        properties.put(SocketConfig.KEY_CONNECT_RETRIES, CONNECT_RETRIES);
        properties.put(SocketConfig.KEY_CONNECT_PAUSE, CONNECT_PAUSE);
        properties.put(SocketConfig.KEY_BUFFER_SIZE, BUFFER_SIZE);
        properties.put(SocketConfig.KEY_TIMEOUT, TIMEOUT);
        properties.put(SocketConfig.KEY_LOGIN, LOGIN);
        properties.put(SocketConfig.KEY_OPEN_SOCK4, OPEN_SOCK4);
        properties.put(SocketConfig.KEY_OPEN_SOCK5, OPEN_SOCK5);
        properties.put(SocketConfig.KEY_USER, USER);
        properties.put(SocketConfig.KEY_PASSWORD, PASSWORD);
    }

    public static TcpProxyBuilder custom() {
        return create();
    }
    public static TcpProxyBuilder create() {
        return new TcpProxyBuilder();
    }
    public TcpProxy build() {
        return new TcpProxy(properties);
    }

    public TcpProxyBuilder retry(int value) {
        properties.put(SocketConfig.KEY_CONNECT_RETRIES, value);
        return this;
    }

    public TcpProxyBuilder timeout(int value) {
        properties.put(SocketConfig.KEY_TIMEOUT, value);
        return this;
    }

    public TcpProxyBuilder bufferSize(int value) {
        properties.put(SocketConfig.KEY_BUFFER_SIZE, value);
        return this;
    }
}
