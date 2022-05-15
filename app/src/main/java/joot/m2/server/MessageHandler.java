package joot.m2.server;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.jootnet.m2.core.net.Message;
import com.github.jootnet.m2.core.net.messages.SysInfo;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 消息分发器
 * 
 * @author linxing
 *
 */
public class MessageHandler implements ControllerContex {
	
	/**
	 * 当客户端链接建立时
	 * 
	 * @param chn 通道
	 * @throws IOException 
	 */
	public void onOpen(Channel chn) throws IOException {
		sysInfo.time = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
		chn.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(sysInfo.pack())));
	}
	
	/**
	 * 当业务指令到达时
	 * 
	 * @param msg 业务消息
	 * @param chn 通道
	 * @throws IOException 
	 */
	public void onMessage(Message msg, Channel chn) throws IOException {
		curChn.set(chn);
		Controller.dealMessage(msg, this);
	}
	
	/**
	 * 当客户端链接断开时
	 * @param chn 通道
	 */
	public void onClose(Channel chn) {
		var attrKey = AttributeKey.<String>valueOf("una");
		if (chn.hasAttr(attrKey)) {
			var una = chn.attr(attrKey).getAndSet(null);
			if (una != null)
				sessions.remove(una);
		}
	}
	
	/** 当前处理流程中的连接 */
	private static ThreadLocal<Channel> curChn = new ThreadLocal<>();
	/** 系统信息 */
	private static SysInfo sysInfo;
	/** 会话集合 */
	private static Map<String, Session> sessions = new ConcurrentHashMap<>();
	/** 已完成登陆的通道<br>用户名密码校验成功 */
	private static ChannelGroup logins = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	/** 已进入游戏的通道<br>选择了角色，进入了地图 */
	private static ChannelGroup enters = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	/** 地图通道 */
	private static Map<String, ChannelGroup> mapGroups = new ConcurrentHashMap<>();
	/** 行会通道 */
	private static Map<String, ChannelGroup> guildGroups = new ConcurrentHashMap<>();
	/** 队伍通道 */
	private static Map<String, ChannelGroup> teamGroups = new ConcurrentHashMap<>();
	
	static {
		// FIXME 应该是从地图文件夹加载出来
		sysInfo = new SysInfo(0, 0, null, null, null);
		sysInfo.mapCount = 2;
		sysInfo.mapNos = new String[] {"0", "3"};
		sysInfo.mapNames = new String[] {"比奇省", "盟重省"};
		sysInfo.mapMMaps = new int[] {100, 105};
		mapGroups.put("0", new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
		mapGroups.put("3", new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
	}

	@Override
	public void sendLogins(Message msg) throws IOException {
		logins.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())));
	}

	@Override
	public void sendWorld(Message msg) throws IOException {
		enters.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())));
	}

	@Override
	public void sendMap(String mapNo, Message msg, boolean excludeMe) throws IOException {
		var cg = mapGroups.get(mapNo);
		if (cg != null) {
			if (excludeMe) {
				cg.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())), chn -> !chn.equals(curChn.get()));
			} else {
				cg.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())));
			}
		}
	}

	@Override
	public void sendGuild(String guildName, Message msg) throws IOException {
		var cg = guildGroups.get(guildName);
		if (cg != null) cg.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())));
	}

	@Override
	public void sendTeam(String chrName, Message msg) throws IOException {
		var cg = teamGroups.get(chrName);
		if (cg != null) cg.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())));
	}

	@Override
	public void sendArea(Session ses, Message msg) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void resp(Message msg) throws IOException {
		curChn.get().writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())));
	}

	@Override
	public void bindSession(Session ses) {
		var chn = curChn.get();
		chn.attr(AttributeKey.<String>valueOf("una")).set(ses.una);
		ses.channel = chn;
		sessions.put(ses.una, ses);
		logins.add(chn);
	}

	@Override
	public Session getSession(String una) {
		return sessions.get(una);
	}

	@Override
	public Session getSession() {
		return sessions.get(curChn.get().attr(AttributeKey.<String>valueOf("una")).get());
	}

	@Override
	public void enterMap(String mapNo) {
		mapGroups.get(mapNo).add(curChn.get());
	}

}
