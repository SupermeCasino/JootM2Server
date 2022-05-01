package joot.m2.server;

import com.github.jootnet.m2.core.actor.ChrBasicInfo;
import com.github.jootnet.m2.core.actor.Occupation;
import com.github.jootnet.m2.core.net.Messages;
import com.github.jootnet.m2.core.net.messages.EnterReq;
import com.github.jootnet.m2.core.net.messages.EnterResp;
import com.github.jootnet.m2.core.net.messages.LoginReq;
import com.github.jootnet.m2.core.net.messages.LoginResp;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

public final class M2WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
	// 所有人物 FIXME 改为地图/区域等
	private static ChannelGroup allChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
    	
        if (frame instanceof BinaryWebSocketFrame) {
        	try {
        		var msg = Messages.unpack(frame.content().nioBuffer());
        		switch (msg.type()) {
        		
        		case HUM_ACTION_CHANGE: {
        			// TODO 校验地图/人物/npc/怪物重合
        			// 广播人物动作更改
        			allChannelGroup.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(Messages.pack(msg))));
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
        				if (loginReq.una.equals(unas[i])) {
        					if (loginReq.psw.equals(psws[i])) {
        						var roles = new LoginResp.Role[1];
        						roles[0] = new LoginResp.Role();
        						roles[0].name = i == 0 ? "AlexKit" : i == 1 ? "二条" : "林星";
        						roles[0].level = 1;
        						ctx.channel().attr(AttributeKey.valueOf("una")).set(loginReq.una);
        						ctx.channel().writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(Messages.pack(new LoginResp(0, null, roles, roles[0].name)))));
        						return;
        					}
    						ctx.channel().writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(Messages.pack(new LoginResp(1, null, null, null)))));
    						return;
        				}
        			}
					ctx.channel().writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(Messages.pack(new LoginResp(2, null, null, null)))));
        			break;
        		}
        		case ENTER_REQ: {
        			var enterReq = (EnterReq) msg;
        			var una = (String) ctx.channel().attr(AttributeKey.valueOf("una")).get();
        			if (una == null) {
        				ctx.channel().close();
        				break;
        			}
        			if (una.equals("linxing")) {
        				if (!enterReq.chrName.equals("林星")) {
            				ctx.channel().close();
            				break;
        				}
        			} else if (una.equals("ll01131458")) {
        				if (!enterReq.chrName.equals("AlexKit")) {
            				ctx.channel().close();
            				break;
        				}
        			} else if (una.equals("legendarycici")) {
        				if (!enterReq.chrName.equals("二条")) {
            				ctx.channel().close();
            				break;
        				}
        			} else {
        				ctx.channel().close();
        				break;
        			}
    				allChannelGroup.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(Messages.pack(
    						new EnterResp(null, new ChrBasicInfo(enterReq.chrName, Occupation.warrior, 1, 19, 15, "3", 300, 300))
    						))));
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