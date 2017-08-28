package com.ibingbo.proxy.core.http.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Properties;

import com.ibingbo.proxy.core.config.SocketConfig;
import com.ibingbo.proxy.core.log.Logger;

/**
 * Created by bing on 17/8/25.
 */
public class HttpProxyHandler extends Thread implements ProxyHandler {

    private int connectRetries;
    private int connectPause;
    private int timeout;
    private int bufferSize;

    protected Socket inSocket;

    public HttpProxyHandler(Socket inSocket, Properties properties) {
        this.inSocket = inSocket;
        this.init(properties);
        start();
    }

    public void init(Properties properties) {
        this.setBufferSize(Integer.parseInt(properties.getProperty(SocketConfig.KEY_BUFFER_SIZE, "1024")));
        this.setConnectPause(Integer.parseInt(properties.getProperty(SocketConfig.KEY_CONNECT_PAUSE, "50")));
        this.setConnectRetries(Integer.parseInt(properties.getProperty(SocketConfig.KEY_CONNECT_RETRIES, "5")));
        this.setTimeout(Integer.parseInt(properties.getProperty(SocketConfig.KEY_TIMEOUT, "1000")));
    }

    @Override
    public void run() {
        String line;
        String host;
        int port = 80;
        Socket outSocket = null;
        try {
            this.inSocket.setSoTimeout(this.timeout);
            InputStream is = this.inSocket.getInputStream();
            OutputStream os = null;
            try {
                // 获取请求行的内容
                line = "";
                host = "";
                int state = 0;
                boolean space;
                while (true) {
                    int c = is.read();
                    if (c == -1) {
                        break;
                    }
                    Logger.writeLog(c, true);
                    space = Character.isWhitespace((char) c);
                    switch (state) {
                        case 0:
                            if (space) {
                                continue;
                            }
                            state = 1;
                        case 1:
                            if (space) {
                                state = 2;
                                continue;
                            }
                            line = line + (char) c;
                            break;
                        case 2:
                            if (space) {
                                continue; // 跳过多个空白字符
                            }
                            state = 3;
                        case 3:
                            if (space) {
                                state = 4;
                                // 只取出主机名称部分
                                String host0 = host;
                                int n;
                                n = host.indexOf("//");
                                if (n != -1) {
                                    host = host.substring(n + 2);
                                }
                                n = host.indexOf('/');
                                if (n != -1) {
                                    host = host.substring(0, n);
                                }
                                // 分析可能存在的端口号
                                n = host.indexOf(":");
                                if (n != -1) {
                                    port = Integer.parseInt(host.substring(n + 1));
                                    host = host.substring(0, n);
                                }
                                host = processHostName(host0, host, port, inSocket);

                                int retry = connectRetries;
                                while (retry-- != 0) {
                                    try {
                                        outSocket = new Socket(host, port);
                                        break;
                                    } catch (Exception e) {
                                    }
                                    // 等待
                                    Thread.sleep(connectPause);
                                }
                                if (outSocket == null) {
                                    break;
                                }
                                outSocket.setSoTimeout(connectRetries);
                                os = outSocket.getOutputStream();
                                os.write(line.getBytes());
                                os.write(' ');
                                os.write(host0.getBytes());
                                os.write(' ');
                                pipe(is, outSocket.getInputStream(), os, inSocket.getOutputStream());
                                break;
                            }
                            host = host + (char) c;
                            break;
                    }
                }
            } catch (IOException e) {
            } finally {
                try {
                    inSocket.close();
                } catch (Exception e1) {
                }
                try {
                    outSocket.close();
                } catch (Exception e2) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pipe(InputStream is0, InputStream is1,
                     OutputStream os0, OutputStream os1) throws IOException {
        try {
            int ir;
            byte bytes[] = new byte[bufferSize];
            while (true) {
                try {
                    if ((ir = is0.read(bytes)) > 0) {
                        os0.write(bytes, 0, ir);
                        Logger.writeLog(bytes, 0, ir, true);
                    } else if (ir < 0) {
                        break;
                    }
                } catch (InterruptedIOException e) {
                }
                try {
                    if ((ir = is1.read(bytes)) > 0) {
                        os1.write(bytes, 0, ir);
                        Logger.writeLog(bytes, 0, ir, false);
                    } else if (ir < 0) {
                        break;
                    }
                } catch (InterruptedIOException e) {
                }
            }
        } catch (Exception e0) {
            System.out.println("Pipe异常: " + e0);
        }
    }

    public String processHostName(String url, String host, int port, Socket sock) {
        java.text.DateFormat cal = java.text.DateFormat.getDateTimeInstance();
        System.out.println(cal.format(new java.util.Date()) + " - " + url + " "
                + sock.getInetAddress() + "\n");
        return host;
    }

    public int getConnectRetries() {
        return connectRetries;
    }

    public void setConnectRetries(int connecRetries) {
        this.connectRetries = connecRetries;
    }

    public int getConnectPause() {
        return connectPause;
    }

    public void setConnectPause(int connectPause) {
        this.connectPause = connectPause;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public Socket getInSocket() {
        return inSocket;
    }

    public void setInSocket(Socket inSocket) {
        this.inSocket = inSocket;
    }
}
