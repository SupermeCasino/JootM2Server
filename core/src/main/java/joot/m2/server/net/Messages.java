package joot.m2.server.net;

import java.nio.charset.StandardCharsets;

import com.github.jootnet.mir2.core.actor.Action;
import com.github.jootnet.mir2.core.actor.Direction;
import com.github.jootnet.mir2.core.actor.HumActionInfo;

import io.netty.buffer.ByteBuf;
import joot.m2.server.net.messages.HumActionChange;

/**
 * 消息工具类
 */
public final class Messages {
	
	/**
	 * 将消息打包到数据缓冲区
	 * 
	 * @param message 消息
	 * @param buffer 缓冲区
	 */
	public static void pack(Message message, ByteBuf buffer) {
		
		switch (message.type()) {
		
		case HUM_ACTION_CHANGE: {
			var humActionChange = (HumActionChange) message;
			// 0.类型
			buffer.writeInt(message.type().id());
			// 1.人物姓名
			byte[] nameBytes = humActionChange.name().getBytes(StandardCharsets.UTF_8);
			buffer.writeByte((byte) nameBytes.length);
			buffer.writeBytes(nameBytes);
			// 2.当前坐标以及动作完成后的坐标
			buffer.writeShort((short) humActionChange.x());
			buffer.writeShort((short) humActionChange.y());
			buffer.writeShort((short) humActionChange.nextX());
			buffer.writeShort((short) humActionChange.nextY());
			// 3.动作
			pack(humActionChange.action(), buffer);
			break;
		}
		
		
		default:break;
		}
		
	}
	
	
	/**
	 * 从缓冲区中解析数据包
	 * 
	 * @param buffer 缓冲区
	 * @return 解析的数据包或null
	 */
	public static Message unpack(ByteBuf buffer) {
		MessageType type = null;
		
		int typeId = buffer.readInt();
		for (var msgType : MessageType.values()) {
			if (msgType.id() == typeId) {
				type = msgType;
				break;
			}
		}
		
		if (type == null) return null;
		
		switch (type) {
		
		case HUM_ACTION_CHANGE: {
			byte nameBytesLen = buffer.readByte();
			byte[] nameBytes = new byte[nameBytesLen];
			buffer.readBytes(nameBytes);
			short x = buffer.readShort();
			short y = buffer.readShort();
			short nx = buffer.readShort();
			short ny = buffer.readShort();
			var humActionInfo = new HumActionInfo();
			unpack(humActionInfo, buffer);
			return new HumActionChange(new String(nameBytes, StandardCharsets.UTF_8), x, y, nx, ny, humActionInfo);
		}
		
		
		default:break;
		}
		
		return null;
	}

    private static void pack(HumActionInfo info, ByteBuf buffer) {
    	buffer.writeByte((byte) info.act.ordinal());
    	buffer.writeByte((byte) info.dir.ordinal());
    	buffer.writeShort(info.frameIdx);
    	buffer.writeShort(info.frameCount);
    	buffer.writeShort(info.duration);
    }
    private static void unpack(HumActionInfo info, ByteBuf buffer) {
    	byte actOrdinal = buffer.readByte();
    	for (var act : Action.values()) {
    		if (act.ordinal() == actOrdinal) {
    			info.act = act;
    			break;
    		}
    	}
    	byte dirOrdinal = buffer.readByte();
    	for (var dir : Direction.values()) {
    		if (dir.ordinal() == dirOrdinal) {
    			info.dir = dir;
    			break;
    		}
    	}
    	info.frameIdx = buffer.readShort();
    	info.frameCount = buffer.readShort();
    	info.duration = buffer.readShort();
    }
}
