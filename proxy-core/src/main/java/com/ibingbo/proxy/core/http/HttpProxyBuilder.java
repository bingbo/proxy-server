package com.ibingbo.proxy.core.http;

import java.util.Properties;

import com.ibingbo.proxy.core.config.SocketConfig;

/**
 * Created by bing on 17/8/25.
 */
public class HttpProxyBuilder {
    private static int CONNECT_RETRIES = 5;
    private static int CONNECT_PAUSE = 5;
    private static int TIMEOUT = 50;
    private static int BUFFER_SIZE = 1024;

    private static Properties properties = new Properties();

    static {
        properties.put(SocketConfig.KEY_CONNECT_RETRIES, CONNECT_RETRIES);
        properties.put(SocketConfig.KEY_CONNECT_PAUSE, CONNECT_PAUSE);
        properties.put(SocketConfig.KEY_BUFFER_SIZE, BUFFER_SIZE);
        properties.put(SocketConfig.KEY_TIMEOUT, TIMEOUT);
    }

    public static HttpProxyBuilder custom() {
        return create();
    }
    public static HttpProxyBuilder create() {
        return new HttpProxyBuilder();
    }
    public HttpProxy build() {
        return new HttpProxy(properties);
    }

    public HttpProxyBuilder retry(int value) {
        properties.put(SocketConfig.KEY_CONNECT_RETRIES, value);
        return this;
    }

    public HttpProxyBuilder timeout(int value) {
        properties.put(SocketConfig.KEY_TIMEOUT, value);
        return this;
    }

    public HttpProxyBuilder bufferSize(int value) {
        properties.put(SocketConfig.KEY_BUFFER_SIZE, value);
        return this;
    }
}
