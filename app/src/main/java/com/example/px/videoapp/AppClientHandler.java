package com.example.px.videoapp;

import java.util.Date;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class AppClientHandler  extends ChannelInboundHandlerAdapter {
	
	public static   int		sCurIndex 	= 400;
	public String  	mCurMac 			= "";
	public String	mCmdData 			= "";
	
	public ChannelHandlerContext	mctx = null;
	
    @Override  
    public void channelActive(ChannelHandlerContext ctx) throws Exception {  
        System.out.println("����ʱ���ǣ�"+new Date());  
        System.out.println("HeartBeatClientHandler channelActive");  
        ctx.fireChannelActive();  
    }  
  
    @Override  
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {  
        System.out.println("ֹͣʱ���ǣ�"+new Date());  
        System.out.println("HeartBeatClientHandler channelInactive");  
    }  
  
  
    @Override  
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {  
        String message = (String) msg;  
        
        System.out.println(message);  
        
        if (0 == mCurMac.length()){
        	sCurIndex++;
        	
        	mCurMac = "20cd399ce" + Integer.toHexString(sCurIndex);
        } 
        
      
        if (message.equals("Heartbeat")) {  
            ctx.write("has read message from server");  
            ctx.flush();  
        } 
        else {
        	//ctx.write("GET /q.x?t=20cd399c9ee82200041127320");  
        	//ctx.write("GET /q.x?t=" + mCurMac + "2200041127320"); 
            //ctx.flush();
        }
        
        ReferenceCountUtil.release(msg);  
    } 
    
    /**
     * �ϱ����� 
     */
    public void uploadData(){
    	if (null != mctx){
        	this.mctx.write("GET /q.x?t=" + mCurMac + "2200041127320"); 
        	this.mctx.flush();
        }
    }
}
