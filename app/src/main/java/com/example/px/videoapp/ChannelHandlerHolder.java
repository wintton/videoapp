package com.example.px.videoapp;

import io.netty.channel.ChannelHandler;

public interface ChannelHandlerHolder {
	ChannelHandler[] handlers(); 
}
