package com.ibingbo.proxy.core;

import com.ibingbo.proxy.core.http.HttpProxy;
import com.ibingbo.proxy.core.http.HttpProxyBuilder;
import com.ibingbo.proxy.core.http.handler.HttpProxyHandler;
import com.ibingbo.proxy.core.tcp.TcpProxyBuilder;
import com.ibingbo.proxy.core.tcp.handler.TcpProxyHandler;

/**
 * Created by bing on 17/8/25.
 */
public class App {

    public static void main(String[] args) {
        HttpProxy httpProxy = HttpProxyBuilder.custom()
                .bufferSize(1024)
                .build();
        httpProxy.startProxy(8081, HttpProxyHandler.class);

        TcpProxyBuilder.custom()
                .bufferSize(1024)
                .build()
                .startProxy(8082, TcpProxyHandler.class);
    }
}
