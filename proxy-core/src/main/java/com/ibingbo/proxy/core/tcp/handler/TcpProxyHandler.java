package com.ibingbo.proxy.core.tcp.handler;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import com.ibingbo.proxy.core.config.SocketConfig;
import com.ibingbo.proxy.core.log.Logger;

/**
 * Created by bing on 17/8/25.
 */
public class TcpProxyHandler extends Thread implements ProxyHandler {

    private int connectRetries;
    private int connectPause;
    private int timeout;
    private int bufferSize;
    private boolean openSock4 = true;
    private boolean openSock5 = true;
    private String user;
    private String password;
    private boolean login = false;

    protected Socket inSocket;

    public TcpProxyHandler(Socket inSocket, Properties properties) {
        this.inSocket = inSocket;
        this.init(properties);
        start();
    }

    public void init(Properties properties) {
        this.setBufferSize(Integer.parseInt(properties.getProperty(SocketConfig.KEY_BUFFER_SIZE, "1024")));
        this.setConnectPause(Integer.parseInt(properties.getProperty(SocketConfig.KEY_CONNECT_PAUSE, "50")));
        this.setConnectRetries(Integer.parseInt(properties.getProperty(SocketConfig.KEY_CONNECT_RETRIES, "5")));
        this.setTimeout(Integer.parseInt(properties.getProperty(SocketConfig.KEY_TIMEOUT, "1000")));
        this.setOpenSock4(
                Boolean.parseBoolean(properties.getProperty(SocketConfig.KEY_OPEN_SOCK4, Boolean.TRUE.toString())));
        this.setOpenSock5(
                Boolean.parseBoolean(properties.getProperty(SocketConfig.KEY_OPEN_SOCK5, Boolean.TRUE.toString())));
        this.setUser(properties.getProperty(SocketConfig.KEY_USER, null));
        this.setPassword(properties.getProperty(SocketConfig.KEY_PASSWORD, null));
        this.setLogin(Boolean.parseBoolean(properties.getProperty(SocketConfig.KEY_LOGIN, Boolean.FALSE.toString())));
    }

    @Override
    public void run() {
        // 获取来源的地址用于日志打印使用
        String addr = inSocket.getRemoteSocketAddress().toString();
        // 声明流
        InputStream a_in = null, b_in = null;
        OutputStream a_out = null, b_out = null;
        Socket proxy_socket = null;
        ByteArrayOutputStream cache = null;
        try {
            a_in = inSocket.getInputStream();
            a_out = inSocket.getOutputStream();

            // 获取协议头。取代理的类型，只有 4，5。
            byte[] tmp = new byte[1];
            int n = a_in.read(tmp);
            if (n == 1) {
                byte protocol = tmp[0];
                if ((openSock4 && 0x04 == protocol)) {// 如果开启代理4，并以socks4协议请求
                    proxy_socket = sock4_check(a_in, a_out);
                } else if ((openSock5 && 0x05 == protocol)) {// 如果开启代理5，并以socks5协议请求
                    proxy_socket = sock5_check(a_in, a_out);
                } else {// 非socks 4 ,5 协议的请求
                    Logger.writeLog("not socks proxy : %s  openSock4[] openSock5[]", tmp[0], openSock4, openSock5);
                }
                if (null != proxy_socket) {
                    CountDownLatch latch = new CountDownLatch(1);
                    b_in = proxy_socket.getInputStream();
                    b_out = proxy_socket.getOutputStream();
                    // 交换流数据
                    if (80 == proxy_socket.getPort()) {
                        cache = new ByteArrayOutputStream();
                    }
                    transfer(latch, a_in, b_out, cache);
                    transfer(latch, b_in, a_out, cache);
                    try {
                        latch.await();
                    } catch (Exception e) {
                    }
                }
            } else {
                Logger.writeLog("socks error : %s", Arrays.toString(tmp));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeIo(a_in);
            closeIo(b_in);
            closeIo(b_out);
            closeIo(a_out);
            closeIo(inSocket);
            closeIo(proxy_socket);

        }
    }

    private Socket sock5_check(InputStream in, OutputStream out) throws IOException {
        byte[] tmp = new byte[2];
        in.read(tmp);
        boolean isLogin = false;
        byte method = tmp[1];
        if (0x02 == tmp[0]) {
            method = 0x00;
            in.read();
        }
        if (login) {
            method = 0x02;
        }
        tmp = new byte[] {0x05, method};
        out.write(tmp);
        out.flush();
        // Socket result = null;
        Object resultTmp = null;
        if (0x02 == method) {// 处理登录.
            int b = in.read();
            String user = null;
            String pwd = null;
            if (0x01 == b) {
                b = in.read();
                tmp = new byte[b];
                in.read(tmp);
                user = new String(tmp);
                b = in.read();
                tmp = new byte[b];
                in.read(tmp);
                pwd = new String(tmp);
                if (null != user && user.trim().equals(this.user) && null != pwd && pwd.trim()
                        .equals(this.password)) {// 权限过滤
                    isLogin = true;
                    tmp = new byte[] {0x05, 0x00};// 登录成功
                    out.write(tmp);
                    out.flush();
                    Logger.writeLog("%s login success !", user);
                } else {
                    Logger.writeLog("%s login faild !", user);
                }
            }
        }
        byte cmd = 0;
        if (!login || isLogin) {// 验证是否需要登录
            tmp = new byte[4];
            in.read(tmp);
            Logger.writeLog("proxy header >>  %s", Arrays.toString(tmp));
            cmd = tmp[1];
            String host = getHost(tmp[3], in);
            tmp = new byte[2];
            in.read(tmp);
            int port = ByteBuffer.wrap(tmp).asShortBuffer().get() & 0xFFFF;
            Logger.writeLog("connect %s:%s", host, port);
            ByteBuffer rsv = ByteBuffer.allocate(10);
            rsv.put((byte) 0x05);
            try {
                if (0x01 == cmd) {
                    resultTmp = new Socket(host, port);
                    rsv.put((byte) 0x00);
                } else if (0x02 == cmd) {
                    resultTmp = new ServerSocket(port);
                    rsv.put((byte) 0x00);
                } else {
                    rsv.put((byte) 0x05);
                    resultTmp = null;
                }
            } catch (Exception e) {
                rsv.put((byte) 0x05);
                resultTmp = null;
            }
            rsv.put((byte) 0x00);
            rsv.put((byte) 0x01);
            rsv.put(inSocket.getLocalAddress().getAddress());
            Short localPort = (short) ((inSocket.getLocalPort()) & 0xFFFF);
            rsv.putShort(localPort);
            tmp = rsv.array();
        } else {
            tmp = new byte[] {0x05, 0x01};// 登录失败
            Logger.writeLog("socks server need login,but no login info .");
        }
        out.write(tmp);
        out.flush();
        if (null != resultTmp && 0x02 == cmd) {
            ServerSocket ss = (ServerSocket) resultTmp;
            try {
                resultTmp = ss.accept();
            } catch (Exception e) {
            } finally {
                closeIo(ss);
            }
        }
        return (Socket) resultTmp;
    }

    /**
     * sock4代理的头处理
     *
     * @param in
     * @param out
     *
     * @return
     *
     * @throws IOException
     */
    private Socket sock4_check(InputStream in, OutputStream out) throws IOException {
        Socket proxy_socket = null;
        byte[] tmp = new byte[3];
        in.read(tmp);
        // 请求协议|VN1|CD1|DSTPORT2|DSTIP4|NULL1|
        int port = ByteBuffer.wrap(tmp, 1, 2).asShortBuffer().get() & 0xFFFF;
        String host = getHost((byte) 0x01, in);
        in.read();
        byte[] rsv = new byte[8];// 返回一个8位的响应协议
        // |VN1|CD1|DSTPORT2|DSTIP 4|
        try {
            proxy_socket = new Socket(host, port);
            Logger.writeLog("connect [%s] %s:%s", tmp[1], host, port);
            rsv[1] = 90;// 代理成功
        } catch (Exception e) {
            Logger.writeLog("connect exception  %s:%s", host, port);
            rsv[1] = 91;// 代理失败.
        }
        out.write(rsv);
        out.flush();
        return proxy_socket;
    }

    /**
     * 获取目标的服务器地址
     *
     * @param type
     * @param in
     *
     * @return
     *
     * @throws IOException
     * @createTime 2014年12月14日 下午8:32:15
     */
    private String getHost(byte type, InputStream in) throws IOException {
        String host = null;
        byte[] tmp = null;
        switch (type) {
            case 0x01:// IPV4协议
                tmp = new byte[4];
                in.read(tmp);
                host = InetAddress.getByAddress(tmp).getHostAddress();
                break;
            case 0x03:// 使用域名
                int l = in.read();
                tmp = new byte[l];
                in.read(tmp);
                host = new String(tmp);
                break;
            case 0x04:// 使用IPV6
                tmp = new byte[16];
                in.read(tmp);
                host = InetAddress.getByAddress(tmp).getHostAddress();
                break;
            default:
                break;
        }
        return host;
    }

    /**
     * IO操作中共同的关闭方法
     *
     * @param socket
     *
     * @createTime 2014年12月14日 下午7:50:56
     */
    protected static final void closeIo(Socket closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * IO操作中共同的关闭方法
     *
     * @param socket
     *
     * @createTime 2014年12月14日 下午7:50:56
     */
    protected static final void closeIo(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * 数据交换.主要用于tcp协议的交换
     *
     * @param lock 锁
     * @param in   输入流
     * @param out  输出流
     *
     * @createTime 2014年12月13日 下午11:06:47
     */
    protected static final void transfer(final CountDownLatch latch, final InputStream in, final OutputStream out,
                                         final OutputStream cache) {
        new Thread() {
            public void run() {
                byte[] bytes = new byte[1024];
                int n = 0;
                try {
                    while ((n = in.read(bytes)) > 0) {
                        out.write(bytes, 0, n);
                        out.flush();
                        if (null != cache) {
                            synchronized(cache) {
                                cache.write(bytes, 0, n);
                            }
                        }
                    }
                } catch (Exception e) {
                }
                if (null != latch) {
                    latch.countDown();
                }
            }

            ;
        }.start();
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

    public boolean isOpenSock4() {
        return openSock4;
    }

    public void setOpenSock4(boolean openSock4) {
        this.openSock4 = openSock4;
    }

    public boolean isOpenSock5() {
        return openSock5;
    }

    public void setOpenSock5(boolean openSock5) {
        this.openSock5 = openSock5;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isLogin() {
        return login;
    }

    public void setLogin(boolean login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
