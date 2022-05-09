package joot.m2.server.controller;

import java.io.IOException;

import com.github.jootnet.m2.core.net.Message;
import com.github.jootnet.m2.core.net.MessageType;
import com.github.jootnet.m2.core.net.messages.HumActionChange;

import joot.m2.server.Controller;
import joot.m2.server.ControllerContex;

/**
 * 动作更改
 * 
 * @author linxing
 *
 */
public class HumActionChangeController extends Controller {
	
	static {
		controllers.put(MessageType.HUM_ACTION_CHANGE, new HumActionChangeController());
	}

	@Override
	protected void onMessage(Message msg, ControllerContex ctx) throws IOException {
		if (msg.type() != MessageType.HUM_ACTION_CHANGE) return;
		var humActionChange = (HumActionChange) msg;
		// TODO 判断可移动标记，穿人穿怪等
		var ses = ctx.getSession();
		ses.cBasic.action = humActionChange.action;
		ses.cBasic.x = humActionChange.x;
		ses.cBasic.y = humActionChange.y;
		ses.cBasic.nextX = humActionChange.nextX;
		ses.cBasic.nextY = humActionChange.nextY;
		ctx.sendMap(ses.mapNo, msg, false);
	}

}
