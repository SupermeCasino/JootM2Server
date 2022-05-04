package joot.m2.server;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.github.jootnet.m2.core.actor.AttackMode;
import com.github.jootnet.m2.core.actor.ChrBasicInfo;
import com.github.jootnet.m2.core.actor.ChrPrivateInfo;
import com.github.jootnet.m2.core.actor.ChrPublicInfo;
import com.github.jootnet.m2.core.actor.Occupation;
import com.github.jootnet.m2.core.net.Messages;
import com.github.jootnet.m2.core.net.messages.EnterReq;
import com.github.jootnet.m2.core.net.messages.EnterResp;
import com.github.jootnet.m2.core.net.messages.LoginReq;
import com.github.jootnet.m2.core.net.messages.LoginResp;
import com.github.jootnet.m2.core.net.messages.SysInfo;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
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
        						roles[0].mapNo = "3";
        						roles[0].x = 300;
        						roles[0].y = 300;
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
        			var cbi = new ChrBasicInfo(enterReq.chrName, (byte) 0, Occupation.warrior, 1, 19, 19, 10, 15, 0, 1, 0, 0, 0, 0, 0, 0, 300, 300);
    				ctx.channel().writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(Messages.pack(
    						new EnterResp(null
    								, cbi
    								, new ChrPublicInfo(1, 1, 0, 0, 0, 0, 0, 0, 0, 0)
    								, new ChrPrivateInfo(50, 100, 0, 50, 5, 15, 0, 12, AttackMode.All))))));
    				allChannelGroup.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(Messages.pack(
    						new EnterResp(null
    								, cbi, null, null)))), chn -> !chn.equals(ctx.channel()));
        			break;
        		}
				default:
					break;
        		}
        	} catch (Exception ex) {
        		ex.printStackTrace();
        	}
        } else if (frame instanceof TextWebSocketFrame) {
        	var text = ((TextWebSocketFrame) frame).text();
        	if (text.equals("Hello wrold!")) {
            	allChannelGroup.add(ctx.channel());
	        	ctx.channel().writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(Messages.pack(new SysInfo(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
	        			, 2, new String[] {"0", "3"}, new String[] {"比奇省", "盟重省"}, new int[] {100, 105})))));
        	}
        }
        
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception { }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception { }
}