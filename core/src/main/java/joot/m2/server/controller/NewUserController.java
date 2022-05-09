package joot.m2.server.controller;

import java.io.IOException;
import java.util.HashMap;

import com.github.jootnet.m2.core.net.Message;
import com.github.jootnet.m2.core.net.MessageType;
import com.github.jootnet.m2.core.net.messages.NewUserReq;
import com.github.jootnet.m2.core.net.messages.NewUserResp;

import joot.m2.server.Controller;
import joot.m2.server.ControllerContex;

/**
 * 创建用户
 * 
 * @author linxing
 *
 */
public class NewUserController extends Controller {
	
	static {
		controllers.put(MessageType.NEW_USER_REQ, new NewUserController());
	}

	@Override
	protected void onMessage(Message msg, ControllerContex ctx) throws IOException {
		if (msg.type() != MessageType.NEW_USER_REQ) return;
		var newUserReq = (NewUserReq) msg;
		try (var redis = redisPool.getResource()) {
			if (redis.sismember("unas", newUserReq.una)) {
				ctx.resp(new NewUserResp(1, null, null));
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
			ctx.resp(new NewUserResp(0, null, null));
		}

	}

}
