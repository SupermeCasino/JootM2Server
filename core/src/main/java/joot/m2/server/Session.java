package joot.m2.server;

import com.github.jootnet.m2.core.actor.ChrBasicInfo;
import com.github.jootnet.m2.core.actor.ChrPrivateInfo;
import com.github.jootnet.m2.core.actor.ChrPublicInfo;

import io.netty.channel.Channel;

/**
 * 游戏会话
 * 
 * @author linxing
 *
 */
public class Session {
	
	/** 登陆用户名 */
	public String una;
	/** 消息发送通道 */
	public Channel channel;
	/** 身处地图 */
	public String mapNo;
	/** 角色属性 */
	public ChrBasicInfo cBasic;
	public ChrPublicInfo cPublic;
	public ChrPrivateInfo cPrivate;
}
