package joot.m2.server;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
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
import redis.clients.jedis.Transaction;

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
			var una = chn.attr(attrKey).get();
			if (una != null) {
				try (var redis = Controller.redisPool.getResource()) {
					var multi = redis.multi();
					saveChrInfo(multi);
					multi.exec();
				}
				sessions.remove(una);
			}
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
	public void unbindSession() {
		var chn = curChn.get();
		var attr = chn.attr(AttributeKey.<String>valueOf("una"));
		var ses = sessions.remove(attr.getAndSet(null));
		if (ses != null)
			ses.channel = null;
		logins.remove(chn);
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
		getSession().mapNo = mapNo;
		mapGroups.get(mapNo).add(curChn.get());
	}

	@Override
	public void leaveMap() {
		var ses = getSession();
		mapGroups.get(ses.mapNo).remove(curChn.get());
		ses.mapNo = null;
	}

	@Override
	public void saveChrInfo(Transaction multi) {
		var ses = getSession();
		if (ses.cBasic == null) return;
		var chrInfo = new HashMap<String, String>();
		chrInfo.put("gender", String.valueOf(ses.cBasic.gender));
		chrInfo.put("occu", ses.cBasic.occupation.name());
		chrInfo.put("level", String.valueOf(ses.cBasic.level));
		chrInfo.put("hp", String.valueOf(ses.cBasic.hp));
		chrInfo.put("maxHp", String.valueOf(ses.cBasic.maxHp));
		chrInfo.put("mp", String.valueOf(ses.cBasic.mp));
		chrInfo.put("maxMp", String.valueOf(ses.cBasic.maxMp));
		chrInfo.put("humFileIdx", String.valueOf(ses.cBasic.humFileIdx));
		chrInfo.put("humIdx", String.valueOf(ses.cBasic.humIdx));
		chrInfo.put("humEffectFileIdx", String.valueOf(ses.cBasic.humEffectFileIdx));
		chrInfo.put("humEffectIdx", String.valueOf(ses.cBasic.humEffectIdx));
		chrInfo.put("weaponFileIdx", String.valueOf(ses.cBasic.weaponFileIdx));
		chrInfo.put("weaponIdx", String.valueOf(ses.cBasic.weaponIdx));
		chrInfo.put("weaponEffectFileIdx", String.valueOf(ses.cBasic.weaponEffectFileIdx));
		chrInfo.put("weaponEffectIdx", String.valueOf(ses.cBasic.weaponEffectIdx));
		chrInfo.put("x", String.valueOf(ses.cBasic.x));
		chrInfo.put("y", String.valueOf(ses.cBasic.y));
		if (ses.cBasic.guildName != null)
			chrInfo.put("guildName", ses.cBasic.guildName);
		chrInfo.put("exp", String.valueOf(ses.cPrivate.exp));
		chrInfo.put("levelUpExp", String.valueOf(ses.cPrivate.levelUpExp));
		chrInfo.put("bagWeight", String.valueOf(ses.cPrivate.bagWeight));
		chrInfo.put("maxBagWeight", String.valueOf(ses.cPrivate.maxBagWeight));
		chrInfo.put("wearWeight", String.valueOf(ses.cPrivate.wearWeight));
		chrInfo.put("maxWearWeight", String.valueOf(ses.cPrivate.maxWearWeight));
		chrInfo.put("handWeight", String.valueOf(ses.cPrivate.handWeight));
		chrInfo.put("maxHandWeight", String.valueOf(ses.cPrivate.maxHandWeight));
		chrInfo.put("attackMode", ses.cPrivate.attackMode.name());
		chrInfo.put("attackPoint", String.valueOf(ses.cPublic.attackPoint));
		chrInfo.put("maxAttackPoint", String.valueOf(ses.cPublic.maxAttackPoint));
		chrInfo.put("magicAttackPoint", String.valueOf(ses.cPublic.magicAttackPoint));
		chrInfo.put("maxMagicAttackPoint", String.valueOf(ses.cPublic.maxMagicAttackPoint));
		chrInfo.put("taositAttackPoint", String.valueOf(ses.cPublic.taositAttackPoint));
		chrInfo.put("maxTaositAttackPoint", String.valueOf(ses.cPublic.maxTaositAttackPoint));
		chrInfo.put("defensePoint", String.valueOf(ses.cPublic.defensePoint));
		chrInfo.put("maxDefensePoint", String.valueOf(ses.cPublic.maxDefensePoint));
		chrInfo.put("magicDefensePoint", String.valueOf(ses.cPublic.magicDefensePoint));
		chrInfo.put("maxMagicDefensePoint", String.valueOf(ses.cPublic.maxMagicDefensePoint));
		chrInfo.put("map", ses.mapNo);
		multi.hset("chr:" + ses.cBasic.name, chrInfo);
	}

}
