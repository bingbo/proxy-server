package com.ibingbo.proxy.core.channel;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Created by bing on 17/8/28.
 */
public class SocketChannel extends Thread{
    private DataInputStream in; // 读数据
    private DataOutputStream out; // 写数据
    public SocketChannel(DataInputStream _in, DataOutputStream _out){
        in=_in;
        out=_out;
        start();
    }
    public void run(){
        // 线程运行函数 , 循环读取返回数据 , 并发送给相关客户端
        int readbytes=0;
        byte buf[]=new byte[10000];
        while(true){ // 循环
            try{
                if(readbytes==-1)
                    break; // 无数据则退出循环
                readbytes=in.read(buf,0,10000);
                if(readbytes>0){
                    out.write(buf,0,readbytes);
                    out.flush();
                }
            }catch(Exception e){break;} // 异常则退出循环
        }
    }
}
