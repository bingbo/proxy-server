package com.ibingbo.proxy.core.log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

/**
 * Created by bing on 17/8/25.
 */
public class Logger {

    private static OutputStream log = System.out;

    public static void writeLog(int c, boolean browser) throws IOException {
        log.write(c);
    }

    public static void writeLog(byte[] bytes, int offset, int len, boolean browser) throws IOException {
        for (int i = 0; i < len; i++) {
            writeLog((int) bytes[offset + i], browser);
        }
    }

    public static void writeLog(String message, Object... args) throws IOException {
        Date dat = new Date();
        String msg = String.format("%1$tF %1$tT %2$-5s %3$s%n", dat, Thread.currentThread().getId(),
                String.format(message, args));
        log.write(msg.getBytes("utf-8"));
    }
}
