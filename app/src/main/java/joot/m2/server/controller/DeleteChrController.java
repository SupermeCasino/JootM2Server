package joot.m2.server.controller;

import java.io.IOException;

import com.github.jootnet.m2.core.net.Message;
import com.github.jootnet.m2.core.net.MessageType;
import com.github.jootnet.m2.core.net.messages.DeleteChrReq;
import com.github.jootnet.m2.core.net.messages.DeleteChrResp;

import joot.m2.server.Controller;
import joot.m2.server.ControllerContex;

public class DeleteChrController extends Controller {
	
	static {
		controllers.put(MessageType.DELETE_CHR_REQ, new DeleteChrController());
	}

	@Override
	protected void onMessage(Message msg, ControllerContex ctx) throws IOException {
		if (msg.type() != MessageType.DELETE_CHR_REQ) return;
		var deleteChrReq = (DeleteChrReq) msg;
		var ses = ctx.getSession();
		try (var redis = redisPool.getResource()) {
			var chrInfo = redis.hgetAll("chr:" + deleteChrReq.name);
			if (chrInfo == null || chrInfo.isEmpty()) {
				ctx.resp(new DeleteChrResp(1, null));
				return;
			}
			var chr1 = redis.hget("user:" + ses.una, "chr1");
			var chr2 = redis.hget("user:" + ses.una, "chr2");
			var multi = redis.multi();
			try {
				if (deleteChrReq.name.equals(chr1)) {
					multi.hdel("user:" + ses.una, "chr1");
				} else if (deleteChrReq.name.equals(chr2)) {
					multi.hdel("user:" + ses.una, "chr2");
				}
				multi.del("chr:" + deleteChrReq.name);
				multi.hdel("user:" + ses.una, "lastName");
			} catch (Exception ex) {
				multi.discard();
			}
			multi.exec();
			ctx.resp(new DeleteChrResp(0, null));
		}
	}

}
