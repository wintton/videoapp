package com.example.px.videoapp;

/** 
 * Created by Administrator on 2016/9/22. 
 */  
/** 
 * 
 * 閲嶈繛妫�娴嬬嫍锛屽綋鍙戠幇褰撳墠鐨勯摼璺笉绋冲畾鍏抽棴涔嬪悗锛岃繘琛�12娆￠噸杩� 
 */  
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;


@Sharable 
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask, ChannelHandlerHolder{

	  
    private final Bootstrap bootstrap;  
    private final Timer timer;  
    private final int port;  
  
    private final String host;  
  
    private volatile boolean reconnect = true;  
    private int attempts;  
  
  
    public ConnectionWatchdog(Bootstrap bootstrap, Timer timer, int port,String host, boolean reconnect) {  
        this.bootstrap = bootstrap;  
        this.timer = timer;  
        this.port = port;  
        this.host = host;  
        this.reconnect = reconnect;  
    }  
  
    /** 
     * channel閾捐矾姣忔active鐨勬椂鍊欙紝灏嗗叾杩炴帴鐨勬鏁伴噸鏂扳槥 0 
     */  
    @Override  
    public void channelActive(ChannelHandlerContext ctx) throws Exception {  
        System.out.println("连接已经激活 Connect Activie");  
        attempts = 0;  
        ctx.fireChannelActive();  
    }  
  
    @Override  
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {  
        System.out.println("channelInactive");  
        if(reconnect){  
            System.out.println("连接断开，自动重连中");  
            if (attempts < 12) {  
                attempts++;  
            }           //閲嶈繛鐨勯棿闅旀椂闂翠細瓒婃潵瓒婇暱  
            int timeout = 2 << attempts;  
            timer.newTimeout(this, timeout, TimeUnit.MILLISECONDS);  
        }  
        ctx.fireChannelInactive();  
    }  
  
    public void run(Timeout timeout) throws Exception {  
        ChannelFuture future;  
        //bootstrap宸茬粡鍒濆鍖栧ソ浜嗭紝鍙渶瑕佸皢handler濉叆灏卞彲浠ヤ簡  
        synchronized (bootstrap) {  
            bootstrap.handler(new ChannelInitializer<Channel>(){  
                @Override  
                protected void initChannel(Channel ch) throws Exception {  
                    ch.pipeline().addLast(handlers());  
                }  
            });  
            future = bootstrap.connect(host,port);  
        }  
         //future瀵硅薄  
         future.addListener(new ChannelFutureListener() {  
             public void operationComplete(ChannelFuture f) throws Exception {  
                 boolean succeed = f.isSuccess();  
                 //如果重连失败，则调用ChannelInactive方法，再次出发重连事件，一直尝试12次，如果失败则不再重连 
                 if (!succeed) {  
                     System.out.println("重连失败");  
                     f.channel().pipeline().fireChannelInactive();  
                 }else{  
                     System.out.println("重连成功");  
                 }  
             }  
         });  
    }  
}
