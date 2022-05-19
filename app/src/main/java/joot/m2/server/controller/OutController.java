package joot.m2.server.controller;

import java.io.IOException;

import com.github.jootnet.m2.core.net.Message;
import com.github.jootnet.m2.core.net.MessageType;
import com.github.jootnet.m2.core.net.messages.LoginResp;
import com.github.jootnet.m2.core.net.messages.OutResp;

import joot.m2.server.Controller;
import joot.m2.server.ControllerContex;

/**
 * 退出游戏世界
 * <br>
 * 回到选角界面
 * 
 * @author LinXing
 *
 */
public class OutController extends Controller {
	
	static {
		controllers.put(MessageType.OUT_REQ, new OutController());
	}

	@Override
	protected void onMessage(Message msg, ControllerContex ctx) throws IOException {
		if (msg.type() != MessageType.OUT_REQ) return;
		var ses = ctx.getSession();
		try (var redis = redisPool.getResource()) {
			var multi = redis.multi();
			ctx.saveChrInfo(multi);
			multi.exec();
			
			var chrName = ses.cBasic.name;
			var roleInfoN = new LoginResp.Role();
			roleInfoN.gender = ses.cBasic.gender;
			roleInfoN.level = ses.cBasic.level;
			roleInfoN.mapNo = ses.mapNo;
			roleInfoN.name = chrName;
			roleInfoN.type = ses.cBasic.occupation.ordinal();
			roleInfoN.x = (short) ses.cBasic.x;
			roleInfoN.y = (short) ses.cBasic.y;
			ses.cBasic = null;
			ses.cPrivate = null;
			ses.cPublic = null;
			
			ctx.sendMap(ses.mapNo, new OutResp(0, null, roleInfoN), false);
			ctx.leaveMap();
		}
	}

}
