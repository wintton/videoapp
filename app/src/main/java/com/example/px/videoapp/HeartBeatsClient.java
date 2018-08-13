package com.example.px.videoapp;

import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;

public class HeartBeatsClient {
	 protected final HashedWheelTimer timer = new HashedWheelTimer();

	    private Bootstrap boot;


	    public void connect(int port, String host) throws Exception {

	        EventLoopGroup group = new NioEventLoopGroup();

	        boot = new Bootstrap();
	        boot.group(group).channel(NioSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO));

	        final ConnectionWatchdog watchdog = new ConnectionWatchdog(boot, timer, port,host, true) {

	                public ChannelHandler[] handlers() {
	                    return new ChannelHandler[] {
	                            this,
	                            new IdleStateHandler(0, 4, 0, TimeUnit.SECONDS),

	                            new StringDecoder(),
	                            new StringEncoder(),
	                            new HeartBeatClientHandler()
	                    };
	                }
	            };

	            ChannelFuture future;
	            //��������
	            try {
	                synchronized (boot) {
	                    boot.handler(new ChannelInitializer<Channel>() {

	                        //��ʼ��channel
	                        @Override
	                        protected void initChannel(Channel ch) throws Exception {
	                            ch.pipeline().addLast(watchdog.handlers());
	                        }
	                    });

	                    future = boot.connect(host,port);
	                }

	                // ���´�����synchronizedͬ���������ǰ�ȫ��
	                future.sync();
	            } catch (Throwable t) {
	                throw new Exception("connects to  fails", t);
	            }
	    }

	    /**
	     * @param args
	     * @throws Exception
	     */
	    public static void main(String[] args) throws Exception {

	        //int port = 8989;
	        int port = 8686;

	        if (args != null && args.length > 0) {
	            try {
	                port = Integer.valueOf(args[0]);
	            } catch (NumberFormatException e) {
	                // ����Ĭ��ֵ
	            }
	        }

	        // ����ʮ�����ӣ�
	        for (int i = 0; i < 400; i++){
		        //new HeartBeatsClient().connect(port, "127.0.0.1");
		        new HeartBeatsClient().connect(port, "112.126.83.31");
	        	//new HeartBeatsClient().connect(port, "211.149.144.117");
	        	//new HeartBeatsClient().connect(port, "139.219.185.249");
	        }
	    }
}
