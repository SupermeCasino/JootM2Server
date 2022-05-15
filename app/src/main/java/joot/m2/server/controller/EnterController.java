package joot.m2.server.controller;

import java.io.IOException;

import com.github.jootnet.m2.core.actor.AttackMode;
import com.github.jootnet.m2.core.actor.ChrBasicInfo;
import com.github.jootnet.m2.core.actor.ChrPrivateInfo;
import com.github.jootnet.m2.core.actor.ChrPublicInfo;
import com.github.jootnet.m2.core.actor.Occupation;
import com.github.jootnet.m2.core.net.Message;
import com.github.jootnet.m2.core.net.MessageType;
import com.github.jootnet.m2.core.net.messages.EnterReq;
import com.github.jootnet.m2.core.net.messages.EnterResp;

import joot.m2.server.Controller;
import joot.m2.server.ControllerContex;

/**
 * 进入游戏
 * 
 * @author linxing
 *
 */
public class EnterController extends Controller {
	
	static {
		controllers.put(MessageType.ENTER_REQ, new EnterController());
	}

	@Override
	protected void onMessage(Message msg, ControllerContex ctx) throws IOException {
		if (msg.type() != MessageType.ENTER_REQ) return;
		var enterReq = (EnterReq) msg;
		var ses = ctx.getSession();
		try (var redis = redisPool.getResource()) {
			redis.hset("user:" + ses.una, "lastName", enterReq.chrName);
			
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
			
			ctx.enterMap(ses.mapNo);
			ctx.resp(new EnterResp(null, cBasic, cPublic, cPrivate));
			ctx.sendMap(ses.mapNo, new EnterResp(null, cBasic, null, null), true);
		}
	}

}
