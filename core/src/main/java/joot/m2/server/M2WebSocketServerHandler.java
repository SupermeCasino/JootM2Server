package joot.m2.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import joot.m2.server.net.Messages;
import joot.m2.server.net.messages.LoginReq;
import joot.m2.server.net.messages.LoginResp;

public final class M2WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
	// 所有人物 FIXME 改为地图/区域等
	private static ChannelGroup allChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
    	
        if (frame instanceof BinaryWebSocketFrame) {
        	try {
        		var msg = Messages.unpack(frame.content());
        		switch (msg.type()) {
        		
        		case HUM_ACTION_CHANGE: {
        			// TODO 校验地图/人物/npc/怪物重合
        			// 广播人物动作更改
        			allChannelGroup.writeAndFlush(new BinaryWebSocketFrame(Messages.pack(msg)));
        			break;
        		}
        		case LOGIN_REQ: {
        			var loginReq = (LoginReq) msg;
        			var unas = new String[] {
        					"ll01131458",
        					"legendarycici",
        					"linxing"
        			};
        			var psws = new String[] {
        					"123456",
        					"123456",
        					"123456"
        			};
        			for (var i = 0; i < unas.length; ++i) {
        				if (loginReq.una().equals(unas[i])) {
        					if (loginReq.psw().equals(psws[i])) {
        						var roles = new LoginResp.Role[1];
        						roles[0] = new LoginResp.Role();
        						roles[0].name = i == 0 ? "AlexKit" : i == 1 ? "二条" : "林星";
        						ctx.channel().writeAndFlush(new BinaryWebSocketFrame(Messages.pack(new LoginResp(0, null, roles))));
        						return;
        					}
    						ctx.channel().writeAndFlush(new BinaryWebSocketFrame(Messages.pack(new LoginResp(1, null, null))));
    						return;
        				}
        			}
					ctx.channel().writeAndFlush(new BinaryWebSocketFrame(Messages.pack(new LoginResp(2, null, null))));
        			break;
        		}
				default:
					break;
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
    	//allChannelGroup.remove(ctx.channel());
    }
}