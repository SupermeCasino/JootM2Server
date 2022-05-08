package joot.m2.server;

import com.github.jootnet.m2.core.net.Message;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public final class M2WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
	private AllInOneController controller;
	
	public M2WebSocketServerHandler(AllInOneController controller) {
		this.controller = controller;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {

		if (frame instanceof BinaryWebSocketFrame) {
			try {
				controller.onMessage(Message.unpack(frame.content().nioBuffer()), ctx.channel());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else if (frame instanceof TextWebSocketFrame) {
			var text = ((TextWebSocketFrame) frame).text();
			if (text.equals("Hello wrold!")) {
				controller.onOpen(ctx.channel());
			}
		}

	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		controller.onClose(ctx.channel());
	}
}