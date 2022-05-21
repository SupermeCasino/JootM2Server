package joot.m2.server.controller;

import java.io.IOException;

import com.github.jootnet.m2.core.net.Message;
import com.github.jootnet.m2.core.net.MessageType;
import com.github.jootnet.m2.core.net.messages.ModifyPswReq;
import com.github.jootnet.m2.core.net.messages.ModifyPswResp;

import joot.m2.server.Controller;
import joot.m2.server.ControllerContex;

/**
 * 修改密码
 * 
 * @author linxing
 *
 */
public class ModifyPswController extends Controller {
	
	static {
		controllers.put(MessageType.MODIFY_PSW_REQ, new ModifyPswController());
	}

	@Override
	protected void onMessage(Message msg, ControllerContex ctx) throws IOException {
		if (msg.type() != MessageType.MODIFY_PSW_REQ) return;
		var modifyPswReq = (ModifyPswReq) msg;
		try (var redis = redisPool.getResource()) {
			if (!redis.sismember("unas", modifyPswReq.una)) {
				ctx.resp(new ModifyPswResp(2, null));
				return;
			}
			if (!redis.hget("user:" + modifyPswReq.una, "psw").equals(modifyPswReq.oldPsw)) {
				ctx.resp(new ModifyPswResp(1, null));
				return;
			}
			// TODO 校验
			redis.hset("user:" + modifyPswReq.una, "psw", modifyPswReq.newPsw);
			ctx.resp(new ModifyPswResp(0, null));
		}
	}

}
