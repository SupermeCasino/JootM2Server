package joot.m2.server;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.jootnet.m2.core.actor.AttackMode;
import com.github.jootnet.m2.core.actor.ChrBasicInfo;
import com.github.jootnet.m2.core.actor.ChrPrivateInfo;
import com.github.jootnet.m2.core.actor.ChrPublicInfo;
import com.github.jootnet.m2.core.actor.Occupation;
import com.github.jootnet.m2.core.net.Message;
import com.github.jootnet.m2.core.net.messages.EnterReq;
import com.github.jootnet.m2.core.net.messages.EnterResp;
import com.github.jootnet.m2.core.net.messages.HumActionChange;
import com.github.jootnet.m2.core.net.messages.LoginReq;
import com.github.jootnet.m2.core.net.messages.LoginResp;
import com.github.jootnet.m2.core.net.messages.NewChrReq;
import com.github.jootnet.m2.core.net.messages.NewChrResp;
import com.github.jootnet.m2.core.net.messages.NewUserReq;
import com.github.jootnet.m2.core.net.messages.NewUserResp;
import com.github.jootnet.m2.core.net.messages.SysInfo;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import redis.clients.jedis.JedisPool;

/**
 * 游戏业务逻辑处理类
 * 
 * @author linxing
 *
 */
public class AllInOneController {
	private static JedisPool redisPool = new JedisPool("localhost", 6379);
	
	
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
		switch(msg.type()) {
		case NEW_USER_REQ: { // 创建用户
			var newUserReq = (NewUserReq) msg;
			try (var redis = redisPool.getResource()) {
				if (redis.sismember("unas", newUserReq.una)) {
					resp(chn, new NewUserResp(1, null, null));
					return;
				}
				// TODO 校验数据
				redis.sadd("unas", newUserReq.una);
				var userInfo = new HashMap<String, String>();
				userInfo.put("psw", newUserReq.psw);
				userInfo.put("name", newUserReq.name);
				userInfo.put("q1", newUserReq.q1);
				userInfo.put("a1", newUserReq.a1);
				userInfo.put("q2", newUserReq.q2);
				userInfo.put("a2", newUserReq.a2);
				userInfo.put("tel", newUserReq.tel);
				userInfo.put("iPhone", newUserReq.iPhone);
				userInfo.put("mail", newUserReq.mail);
				redis.hset("user:" + newUserReq.una, userInfo);
				resp(chn, new NewUserResp(0, null, null));
			}
			break;
		}
		case LOGIN_REQ: { // 登陆
			var loginReq = (LoginReq) msg;
			if (sessions.containsKey(loginReq.una) && sessions.get(loginReq.una).channel != null) {
				resp(chn, new LoginResp(3, null, null, null));
				// TODO 告知链接已被挤掉
				sessions.get(loginReq.una).channel.close();
				return;
			}
			var ses = new Session();
			ses.una = loginReq.una;
			ses.channel = chn;
			sessions.put(loginReq.una, ses);
			try (var redis = redisPool.getResource()) {
				if (!redis.sismember("unas", loginReq.una)) {
					resp(chn, new LoginResp(2, null, null, null));
					return;
				}
				if (!redis.hget("user:" + loginReq.una, "psw").equals(loginReq.psw)) {
					resp(chn, new LoginResp(1, null, null, null));
					return;
				}
				logins.add(chn);
				chn.attr(AttributeKey.<String>valueOf("una")).set(loginReq.una);
				var userInfo = redis.hgetAll("user:" + loginReq.una);
				var chr1 = userInfo.get("chr1");
				var chr2 = userInfo.get("chr2");
				boolean chr1Exist = chr1 != null && !chr1.isBlank();
				boolean chr2Exist = chr2 != null && !chr2.isBlank();
				if (!chr1Exist && !chr2Exist) {
					resp(chn, new LoginResp(0, null, null, null));
					return;
				}
				var roles = new ArrayList<LoginResp.Role>();
				if (chr1Exist) {
					var chrInfo = redis.hgetAll("chr:" + chr1);
					var role = new LoginResp.Role();
					role.gender = Byte.parseByte(chrInfo.get("gender"));
					role.level = Integer.parseInt(chrInfo.get("level"));
					role.mapNo = chrInfo.get("map");
					role.x = Short.parseShort(chrInfo.get("x"));
					role.y = Short.parseShort(chrInfo.get("y"));
					role.name = chr1;
					var occu = Occupation.valueOf(chrInfo.get("occu"));
					if (occu == Occupation.warrior) {
						role.type = 0;
					} else if (occu == Occupation.master) {
						role.type = 1;
					} else if (occu == Occupation.taoist) {
						role.type = 2;
					}
					roles.add(role);
				}
				if (chr2Exist) {
					var chrInfo = redis.hgetAll("chr:" + chr2);
					var role = new LoginResp.Role();
					role.gender = Byte.parseByte(chrInfo.get("gender"));
					role.level = Integer.parseInt(chrInfo.get("level"));
					role.mapNo = chrInfo.get("map");
					role.x = Short.parseShort(chrInfo.get("x"));
					role.y = Short.parseShort(chrInfo.get("y"));
					role.name = chr2;
					var occu = Occupation.valueOf(chrInfo.get("occu"));
					if (occu == Occupation.warrior) {
						role.type = 0;
					} else if (occu == Occupation.master) {
						role.type = 1;
					} else if (occu == Occupation.taoist) {
						role.type = 2;
					}
					roles.add(role);
				}
				resp(chn, new LoginResp(0, null, roles.toArray(new LoginResp.Role[0]), userInfo.get("lastName")));
			}
			break;
		}
		case NEW_CHR_REQ: { // 创建角色
			var newChrReq = (NewChrReq) msg;
			var attrKey = AttributeKey.<String>valueOf("una");
			var una = chn.attr(attrKey).get();
			try (var redis = redisPool.getResource()) {
				if (redis.sismember("chrns", newChrReq.name)) {
					resp(chn, new NewChrResp(2, null, null));
					return;
				}
				var chr1Exist = redis.hget("user:" + una, "chr1") != null;
				var chr2Exist = redis.hget("user:" + una, "chr2") != null;
				if (chr1Exist && chr2Exist) {
					resp(chn, new NewChrResp(1, null, null));
					return;
				}
				// TODO 校验数据
				redis.sadd("chrns", newChrReq.name);
				if (!chr1Exist)
					redis.hset("user:" + una, "chr1", newChrReq.name);
				else
					redis.hset("user:" + una, "chr2", newChrReq.name);
				var chrInfo = new HashMap<String, String>();
				chrInfo.put("map", "3"); // 出生点 盟重省
				chrInfo.put("x", "333");
				chrInfo.put("y", "333");
				chrInfo.put("gender", String.valueOf(newChrReq.gender));
				chrInfo.put("occu", newChrReq.occupation.name());
				chrInfo.put("level", "1");
				var role = new LoginResp.Role();
				role.name = newChrReq.name;
				role.gender = newChrReq.gender;
				role.level = 1;
				role.mapNo = "3";
				role.x = 333;
				role.y = 333;
				if (newChrReq.occupation == Occupation.warrior) {
					role.type = 0;
					chrInfo.put("hp", "19");
					chrInfo.put("maxHp", "19");
					chrInfo.put("mp", "15");
					chrInfo.put("maxMp", "15");
					chrInfo.put("attackPoint", "1");
					chrInfo.put("maxAttackPoint", "1");
				} else if (newChrReq.occupation == Occupation.master) {
					role.type = 1;
					chrInfo.put("hp", "16");
					chrInfo.put("maxHp", "16");
					chrInfo.put("mp", "18");
					chrInfo.put("maxMp", "18");
					chrInfo.put("attackPoint", "0");
					chrInfo.put("maxAttackPoint", "1");
					chrInfo.put("magicAttackPoint", "0");
					chrInfo.put("maxMagicAttackPoint", "1");
				} else {
					role.type = 2;
					chrInfo.put("hp", "17");
					chrInfo.put("maxHp", "17");
					chrInfo.put("mp", "13");
					chrInfo.put("maxMp", "13");
					chrInfo.put("attackPoint", "0");
					chrInfo.put("maxAttackPoint", "1");
					chrInfo.put("taositAttackPoint", "0");
					chrInfo.put("maxTaositAttackPoint", "1");
					chrInfo.put("magicDefensePoint", "0");
					chrInfo.put("maxMagicDefensePoint", "1");
				}
				
				chrInfo.put("levelUpExp", "100");
				chrInfo.put("maxBagWeight", "50");
				chrInfo.put("maxWearWeight", "15");
				chrInfo.put("maxHandWeight", "12");
				redis.hset("chr:" + newChrReq.name, chrInfo);
				resp(chn, new NewChrResp(0, null, role));
			}
			break;
		}
		case ENTER_REQ: { // 进入游戏
			var enterReq = (EnterReq) msg;
			var attrKey = AttributeKey.<String>valueOf("una");
			var una = chn.attr(attrKey).get();
			var ses = sessions.get(una);
			try (var redis = redisPool.getResource()) {
				redis.hset("user:" + una, "lastName", enterReq.chrName);
				
				var chrInfo = redis.hgetAll("chr:" + enterReq.chrName);
				ses.mapNo = chrInfo.get("map");
				var name = enterReq.chrName;
				var gender = Byte.parseByte(chrInfo.get("gender"));
				var occu = Occupation.valueOf(chrInfo.get("occu"));
				var level = Integer.parseInt(chrInfo.get("level"));
				var hp = Integer.parseInt(chrInfo.get("hp"));
				var maxHp = Integer.parseInt(chrInfo.get("maxHp"));
				var mp = Integer.parseInt(chrInfo.get("mp"));
				var maxMp = Integer.parseInt(chrInfo.get("maxMp"));
				var humFileIdx = 0;
				if (chrInfo.containsKey("humFileIdx")) {
					humFileIdx = Integer.parseInt(chrInfo.get("humFileIdx"));
				}
				var humIdx = 1;
				if (chrInfo.containsKey("humIdx")) {
					humIdx = Integer.parseInt(chrInfo.get("humIdx"));
				}
				var humEffectFileIdx = 0;
				if (chrInfo.containsKey("humEffectFileIdx")) {
					humEffectFileIdx = Integer.parseInt(chrInfo.get("humEffectFileIdx"));
				}
				var humEffectIdx = 0;
				if (chrInfo.containsKey("humEffectIdx")) {
					humEffectIdx = Integer.parseInt(chrInfo.get("humEffectIdx"));
				}
				var weaponFileIdx = 0;
				if (chrInfo.containsKey("weaponFileIdx")) {
					weaponFileIdx = Integer.parseInt(chrInfo.get("weaponFileIdx"));
				}
				var weaponIdx = 0;
				if (chrInfo.containsKey("weaponIdx")) {
					weaponIdx = Integer.parseInt(chrInfo.get("weaponIdx"));
				}
				var weaponEffectFileIdx = 0;
				if (chrInfo.containsKey("weaponEffectFileIdx")) {
					weaponEffectFileIdx = Integer.parseInt(chrInfo.get("weaponEffectFileIdx"));
				}
				var weaponEffectIdx = 0;
				if (chrInfo.containsKey("weaponEffectIdx")) {
					weaponEffectIdx = Integer.parseInt(chrInfo.get("weaponEffectIdx"));
				}
				var x = Integer.parseInt(chrInfo.get("x"));
				var y = Integer.parseInt(chrInfo.get("y"));
				var cBasic = new ChrBasicInfo(name, gender, occu, level, hp, maxHp, mp, maxMp
						, humFileIdx, humIdx, humEffectFileIdx, humEffectIdx, weaponFileIdx, weaponIdx, weaponEffectFileIdx, weaponEffectIdx
						, x, y, chrInfo.get("guildName"));
				var exp = 0;
				if (chrInfo.containsKey("exp")) {
					exp = Integer.parseInt(chrInfo.get("exp"));
				}
				var levelUpExp = 0;
				if (chrInfo.containsKey("levelUpExp")) {
					levelUpExp = Integer.parseInt(chrInfo.get("levelUpExp"));
				}
				var bagWeight = 0;
				if (chrInfo.containsKey("bagWeight")) {
					bagWeight = Integer.parseInt(chrInfo.get("bagWeight"));
				}
				var maxBagWeight = 0;
				if (chrInfo.containsKey("maxBagWeight")) {
					maxBagWeight = Integer.parseInt(chrInfo.get("maxBagWeight"));
				}
				var wearWeight = 0;
				if (chrInfo.containsKey("wearWeight")) {
					wearWeight = Integer.parseInt(chrInfo.get("wearWeight"));
				}
				var maxWearWeight = 0;
				if (chrInfo.containsKey("maxWearWeight")) {
					maxWearWeight = Integer.parseInt(chrInfo.get("maxWearWeight"));
				}
				var handWeight = 0;
				if (chrInfo.containsKey("handWeight")) {
					handWeight = Integer.parseInt(chrInfo.get("handWeight"));
				}
				var maxHandWeight = 0;
				if (chrInfo.containsKey("maxHandWeight")) {
					maxHandWeight = Integer.parseInt(chrInfo.get("maxHandWeight"));
				}
				var attckMode = AttackMode.All;
				if (chrInfo.containsKey("attackMode")) {
					attckMode = AttackMode.valueOf(chrInfo.get("attackMode"));
				}
				var cPrivate = new ChrPrivateInfo(exp, levelUpExp, bagWeight, maxBagWeight, wearWeight, maxWearWeight, handWeight, maxHandWeight, attckMode);
				var attackPoint = 0;
				if (chrInfo.containsKey("attackPoint")) {
					attackPoint = Integer.parseInt(chrInfo.get("attackPoint"));
				}
				var maxAttackPoint = 0;
				if (chrInfo.containsKey("maxAttackPoint")) {
					maxAttackPoint = Integer.parseInt(chrInfo.get("maxAttackPoint"));
				}
				var magicAttackPoint = 0;
				if (chrInfo.containsKey("magicAttackPoint")) {
					magicAttackPoint = Integer.parseInt(chrInfo.get("magicAttackPoint"));
				}
				var maxMagicAttackPoint = 0;
				if (chrInfo.containsKey("maxMagicAttackPoint")) {
					maxMagicAttackPoint = Integer.parseInt(chrInfo.get("maxMagicAttackPoint"));
				}
				var taositAttackPoint = 0;
				if (chrInfo.containsKey("taositAttackPoint")) {
					taositAttackPoint = Integer.parseInt(chrInfo.get("taositAttackPoint"));
				}
				var maxTaositAttackPoint = 0;
				if (chrInfo.containsKey("maxTaositAttackPoint")) {
					maxTaositAttackPoint = Integer.parseInt(chrInfo.get("maxTaositAttackPoint"));
				}
				var defensePoint = 0;
				if (chrInfo.containsKey("defensePoint")) {
					defensePoint = Integer.parseInt(chrInfo.get("defensePoint"));
				}
				var maxDefensePoint = 0;
				if (chrInfo.containsKey("maxDefensePoint")) {
					maxDefensePoint = Integer.parseInt(chrInfo.get("maxDefensePoint"));
				}
				var magicDefensePoint = 0;
				if (chrInfo.containsKey("magicDefensePoint")) {
					magicDefensePoint = Integer.parseInt(chrInfo.get("magicDefensePoint"));
				}
				var maxMagicDefensePoint = 0;
				if (chrInfo.containsKey("maxMagicDefensePoint")) {
					maxMagicDefensePoint = Integer.parseInt(chrInfo.get("maxMagicDefensePoint"));
				}
				var cPublic = new ChrPublicInfo(attackPoint, maxAttackPoint, magicAttackPoint, maxMagicAttackPoint
						, taositAttackPoint, maxTaositAttackPoint, defensePoint, maxDefensePoint, magicDefensePoint, maxMagicDefensePoint);
				
				ses.cBasic = cBasic;
				ses.cPrivate = cPrivate;
				ses.cPublic = cPublic;
				
				mapGroups.get(ses.mapNo).add(chn);
				resp(chn, new EnterResp(null, cBasic, cPublic, cPrivate));
				sendMap(ses.mapNo, new EnterResp(null, cBasic, null, null), chn);
			}
			break;
		}
		case HUM_ACTION_CHANGE: { // 角色移动
			var humActionChange = (HumActionChange) msg;
			// TODO 判断可移动标记，穿人穿怪等
			var attrKey = AttributeKey.<String>valueOf("una");
			var una = chn.attr(attrKey).get();
			var ses = sessions.get(una);
			ses.cBasic.action = humActionChange.action;
			ses.cBasic.x = humActionChange.x;
			ses.cBasic.y = humActionChange.y;
			ses.cBasic.nextX = humActionChange.nextX;
			ses.cBasic.nextY = humActionChange.nextY;
			sendMap(ses.mapNo, msg);
			break;
		}
		default:
			break;
		}
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
	
	private static class MapInfo {
		public String no;
		public String name;
		public int mmap;
		public int width;
		public int height;
	}
	/** 服务端地图集合 */
	private static MapInfo[] mapInfos;
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
	/** 区域通道 */
	private static Map<String, ChannelGroup[][]> areaGroups = new ConcurrentHashMap<>();
	
	static {
		// FIXME 应该是从地图文件夹加载出来
		mapInfos = new MapInfo[2];
		mapInfos[0] = new MapInfo();
		mapInfos[0].no = "0";
		mapInfos[0].name = "比奇省";
		mapInfos[0].mmap = 100;
		mapInfos[0].width = 700;
		mapInfos[0].height = 700;
		mapInfos[1] = new MapInfo();
		mapInfos[1].no = "3";
		mapInfos[1].name = "盟重省";
		mapInfos[1].mmap = 105;
		mapInfos[1].width = 1000;
		mapInfos[1].height = 800;
		
		sysInfo = new SysInfo(0, 0, null, null, null);
		sysInfo.mapCount = mapInfos.length;
		sysInfo.mapNos = new String[mapInfos.length];
		sysInfo.mapNames = new String[mapInfos.length];
		sysInfo.mapMMaps = new int[mapInfos.length];
		for (var i = 0; i < mapInfos.length; ++i) {
			sysInfo.mapNos[i] = mapInfos[i].no;
			sysInfo.mapNames[i] = mapInfos[i].name;
			sysInfo.mapMMaps[i] = mapInfos[i].mmap;
			
			mapGroups.put(mapInfos[i].no, new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
			
			var cgs = new DefaultChannelGroup[mapInfos[i].width + 1][mapInfos[i].height + 1];
			areaGroups.put(mapInfos[i].no, cgs);
			// 地图区域以50x50划分，大改约等于5个屏幕大小，也可以分得更细
			for (var w = 0; w < mapInfos[i].width; w += 50) {
				for (var h = 0; h < mapInfos[i].height; h += 50) {
					
					var cg = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
					for (var _w = w; _w < Math.min(w + 50, mapInfos[i].width); ++_w) {
						for (var _h = h; _h < Math.min(h + 50, mapInfos[i].height); ++_h) {
							cgs[_w + 1][_h + 1] = cg;
						}
					}
				}
			}
		}
		
	}
	
	/**
	 * 发送消息到所有已登陆客户端
	 * @param msg 消息
	 * @throws IOException 
	 */
	private void sendLogins(Message msg) throws IOException {
		logins.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())));
	}
	/**
	 * 发送消息到所有已进入游戏的客户端
	 * @param msg 消息
	 * @throws IOException 
	 */
	private void sendWorld(Message msg) throws IOException {
		enters.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())));
	}
	/**
	 * 发送消息到特定地图所有玩家
	 * @param mapNo 地图编号
	 * @param msg 消息
	 * @param excludeChns 需要排除的通道
	 * @throws IOException 
	 */
	private void sendMap(String mapNo, Message msg, Channel... excludeChns) throws IOException {
		var cg = mapGroups.get(mapNo);
		if (cg != null) {
			if (excludeChns.length > 0) {
				cg.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())), chn -> {
					for (var item : excludeChns) {
						if (!item.equals(chn)) return true;
					}
					return false;
				});
			} else {
				cg.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())));
			}
		}
	}
	/**
	 * 发送消息到特定行会所有玩家
	 * @param guildName 行会名称
	 * @param msg 消息
	 * @throws IOException 
	 */
	private void sendGuild(String guildName, Message msg) throws IOException {
		var cg = guildGroups.get(guildName);
		if (cg != null) cg.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())));
	}
	/**
	 * 发送消息到队伍所有玩家
	 * @param chrName 角色昵称
	 * @param msg 消息
	 * @throws IOException 
	 */
	private void sendTeam(String chrName, Message msg) throws IOException {
		var cg = teamGroups.get(chrName);
		if (cg != null) cg.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())));
	}
	/**
	 * 发送消息到角色身处坐标的周围玩家
	 * @param ses 会话
	 * @param msg 消息
	 * @throws IOException 
	 */
	private void sendArea(Session ses, Message msg) throws IOException {
		var cgs = areaGroups.get(ses.mapNo);
		if (cgs != null) {
			var cg = cgs[ses.cBasic.x][ses.cBasic.y];
			if (cg != null) cg.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())));
		}
	}
	private void resp(Channel chn, Message msg) throws IOException {
		chn.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg.pack())));
	}
}
