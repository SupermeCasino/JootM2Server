package joot.m2.server.controller;

import java.io.IOException;

import com.github.jootnet.m2.core.net.Message;
import com.github.jootnet.m2.core.net.MessageType;
import com.github.jootnet.m2.core.net.messages.LoginResp;
import com.github.jootnet.m2.core.net.messages.LogoutResp;
import com.github.jootnet.m2.core.net.messages.OutResp;

import joot.m2.server.Controller;
import joot.m2.server.ControllerContex;

/**
 * 大退
 * 
 * @author LinXing
 *
 */
public class LogoutController extends Controller {
	
	static {
		controllers.put(MessageType.LOGOUT_REQ, new LogoutController());
	}

	@Override
	protected void onMessage(Message msg, ControllerContex ctx) throws IOException {
		if (msg.type() != MessageType.LOGOUT_REQ) return;
		var ses = ctx.getSession();
		try (var redis = redisPool.getResource()) {
			var multi = redis.multi();
			ctx.saveChrInfo(multi);
			multi.exec();
			
			if (ses.cBasic != null) {
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
				
				ctx.resp(new LogoutResp(0, null));
				ctx.sendMap(ses.mapNo, new OutResp(0, null, roleInfoN), true);
				ctx.leaveMap();
			}
			ctx.unbindSession();
		}
	}

}
