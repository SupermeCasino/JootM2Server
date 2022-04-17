package joot.m2.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import joot.m2.server.net.Messages;

public final class M2WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
	// 所有人物 FIXME 改为地图/区域等
	private static ChannelGroup allChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
    	
        if (frame instanceof BinaryWebSocketFrame) {
        	try {
        		var msg = Messages.unpack(frame.content());
        		switch (msg.type() ) {
        		
        		case HUM_ACTION_CHANGE: {
        			// TODO 校验地图/人物/npc/怪物重合
        			var buf = Unpooled.buffer();
        			// 广播人物动作更改
        			Messages.pack(msg, buf);
        			var writeMsg = new BinaryWebSocketFrame(buf);
        			allChannelGroup.writeAndFlush(writeMsg);
        			break;
        		}
        		
        		}
        	} catch (Exception ex) {
        		ex.printStackTrace();
        	}
        }
        
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	allChannelGroup.add(ctx.channel());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	allChannelGroup.remove(ctx.channel());
    }
}