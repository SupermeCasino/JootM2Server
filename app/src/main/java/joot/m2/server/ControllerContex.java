package joot.m2.server;

import java.io.IOException;

import com.github.jootnet.m2.core.net.Message;

/**
 * 消息处理器上下文
 * 
 * @author linxing
 *
 */
public interface ControllerContex {
	
	/**
	 * 发送消息到所有已登陆客户端
	 * @param msg 消息
	 * @throws IOException 
	 */
	void sendLogins(Message msg) throws IOException;
	
	/**
	 * 发送消息到所有已进入游戏的客户端
	 * @param msg 消息
	 * @throws IOException 
	 */
	void sendWorld(Message msg) throws IOException;
	
	/**
	 * 发送消息到特定地图所有玩家
	 * @param mapNo 地图编号
	 * @param msg 消息
	 * @param excludeMe 是否排除自己
	 * @throws IOException 
	 */
	void sendMap(String mapNo, Message msg, boolean excludeMe) throws IOException;
	/**
	 * 发送消息到特定行会所有玩家
	 * @param guildName 行会名称
	 * @param msg 消息
	 * @throws IOException 
	 */
	void sendGuild(String guildName, Message msg) throws IOException;
	/**
	 * 发送消息到队伍所有玩家
	 * @param chrName 角色昵称
	 * @param msg 消息
	 * @throws IOException 
	 */
	void sendTeam(String chrName, Message msg) throws IOException;
	/**
	 * 发送消息到角色身处坐标的周围玩家
	 * @param ses 会话
	 * @param msg 消息
	 * @throws IOException 
	 */
	void sendArea(Session ses, Message msg) throws IOException;
	/**
	 * 将消息回应到当前连接
	 * 
	 * @param msg 消息
	 * @throws IOException
	 */
	void resp(Message msg) throws IOException;
	/**
	 * 获取当前连接会话
	 * 
	 * @return 当前会话
	 */
	Session getSession();
	/**
	 * 获取其他登陆用户的会话
	 * 
	 * @param una 用户名
	 * @return 会话对象或null
	 */
	Session getSession(String una);
	/**
	 * 为当前连接绑定会话
	 * 
	 * @param ses 会话
	 */
	void bindSession(Session ses);
	/**
	 * 修改当前会话角色身处地图
	 * 
	 * @param mapNo 地图编号
	 */
	void enterMap(String mapNo);
}
