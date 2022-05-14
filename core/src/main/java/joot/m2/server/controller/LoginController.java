package joot.m2.server.controller;

import java.io.IOException;
import java.util.ArrayList;

import com.github.jootnet.m2.core.actor.Occupation;
import com.github.jootnet.m2.core.net.Message;
import com.github.jootnet.m2.core.net.MessageType;
import com.github.jootnet.m2.core.net.messages.LoginReq;
import com.github.jootnet.m2.core.net.messages.LoginResp;

import joot.m2.server.Controller;
import joot.m2.server.ControllerContex;
import joot.m2.server.Session;

/**
 * 登陆
 * 
 * @author linxing
 *
 */
public class LoginController extends Controller {
	
	static {
		controllers.put(MessageType.LOGIN_REQ, new LoginController());
	}

	@Override
	protected void onMessage(Message msg, ControllerContex ctx) throws IOException {
		if (msg.type() != MessageType.LOGIN_REQ) return;
		var loginReq = (LoginReq) msg;
		try (var redis = redisPool.getResource()) {
			if (!redis.sismember("unas", loginReq.una)) {
				ctx.resp(new LoginResp(2, null, null, null));
				return;
			}
			if (!redis.hget("user:" + loginReq.una, "psw").equals(loginReq.psw)) {
				ctx.resp(new LoginResp(1, null, null, null));
				return;
			}
			var ses = ctx.getSession(loginReq.una);
			if (ses != null) {
				// TODO 告知链接已被挤掉
				ses.close();
				ctx.resp(new LoginResp(3, null, null, null));
				return;
			}
			
			ses = new Session();
			ses.una = loginReq.una;
			ctx.bindSession(ses);
			
			var userInfo = redis.hgetAll("user:" + loginReq.una);
			var chr1 = userInfo.get("chr1");
			var chr2 = userInfo.get("chr2");
			boolean chr1Exist = chr1 != null && !chr1.isBlank();
			boolean chr2Exist = chr2 != null && !chr2.isBlank();
			if (!chr1Exist && !chr2Exist) {
				ctx.resp(new LoginResp(0, null, null, null));
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
			ctx.resp(new LoginResp(0, null, roles.toArray(new LoginResp.Role[0]), userInfo.get("lastName")));
		}

	}

}
