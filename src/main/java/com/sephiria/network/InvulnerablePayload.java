package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：某个实体进入了无敌窗口。
 *
 * <p>用途是<b>视觉</b>——无敌期间玩家的模型要描一圈白光，而 HUD 的无敌条也要按剩余刻数
 * 自己倒计时。这个状态只有服务端知道，所以授予无敌时广播一次，客户端按刻数自己递减。
 *
 * <p>1.20.1 沿用旧 channel 写法：固定 channel id + 一对 write/read。
 */
public record InvulnerablePayload(int entityId, int ticks) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "invulnerable");

	public static void write(FriendlyByteBuf buffer, InvulnerablePayload payload) {
		buffer.writeVarInt(payload.entityId());
		buffer.writeVarInt(payload.ticks());
	}

	public static InvulnerablePayload read(FriendlyByteBuf buffer) {
		return new InvulnerablePayload(buffer.readVarInt(), buffer.readVarInt());
	}
}
