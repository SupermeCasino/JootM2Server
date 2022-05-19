package joot.m2.server;

import com.github.jootnet.m2.core.net.Message;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public final class M2WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
	private MessageHandler messageHandler;
	private TextWebSocketFrame PONG = new TextWebSocketFrame("PONG");
	
	public M2WebSocketServerHandler(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {

		if (frame instanceof BinaryWebSocketFrame) {
			try {
				messageHandler.onMessage(Message.unpack(frame.content().nioBuffer()), ctx.channel());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else if (frame instanceof TextWebSocketFrame) {
			var text = ((TextWebSocketFrame) frame).text();
			if (text.equals("Hello wrold!")) {
				messageHandler.onOpen(ctx.channel());
			} else if (text.equals("PING")) {
				ctx.channel().writeAndFlush(PONG);
			}
		}

	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		messageHandler.onClose(ctx.channel());
	}
}