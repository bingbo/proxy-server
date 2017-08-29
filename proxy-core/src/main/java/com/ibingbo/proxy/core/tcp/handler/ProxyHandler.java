package com.ibingbo.proxy.core.tcp.handler;

/**
 * Created by bing on 17/8/25.
 */
public interface ProxyHandler {

    default int byte2int(byte b) {
        int mask = 0xff;
        int temp = 0;
        int res = 0;
        res <<= 8;
        temp = b & mask;
        res |= temp;
        return res;
    }
}
