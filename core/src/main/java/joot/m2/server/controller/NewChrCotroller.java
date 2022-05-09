package joot.m2.server.controller;

import java.io.IOException;
import java.util.HashMap;

import com.github.jootnet.m2.core.actor.Occupation;
import com.github.jootnet.m2.core.net.Message;
import com.github.jootnet.m2.core.net.MessageType;
import com.github.jootnet.m2.core.net.messages.LoginResp;
import com.github.jootnet.m2.core.net.messages.NewChrReq;
import com.github.jootnet.m2.core.net.messages.NewChrResp;

import joot.m2.server.Controller;
import joot.m2.server.ControllerContex;

/**
 * 创建角色
 * 
 * @author linxing
 *
 */
public class NewChrCotroller extends Controller {
	
	static {
		controllers.put(MessageType.NEW_CHR_REQ, new NewChrCotroller());
	}

	@Override
	protected void onMessage(Message msg, ControllerContex ctx) throws IOException {
		if (msg.type() != MessageType.NEW_CHR_REQ) return;
		var newChrReq = (NewChrReq) msg;
		var ses = ctx.getSession();
		try (var redis = redisPool.getResource()) {
			if (redis.sismember("chrns", newChrReq.name)) {
				ctx.resp(new NewChrResp(2, null, null));
				return;
			}
			var chr1Exist = redis.hget("user:" + ses.una, "chr1") != null;
			var chr2Exist = redis.hget("user:" + ses.una, "chr2") != null;
			if (chr1Exist && chr2Exist) {
				ctx.resp(new NewChrResp(1, null, null));
				return;
			}
			// TODO 校验数据
			redis.sadd("chrns", newChrReq.name);
			if (!chr1Exist)
				redis.hset("user:" + ses.una, "chr1", newChrReq.name);
			else
				redis.hset("user:" + ses.una, "chr2", newChrReq.name);
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
			ctx.resp(new NewChrResp(0, null, role));
		}
	}

}
