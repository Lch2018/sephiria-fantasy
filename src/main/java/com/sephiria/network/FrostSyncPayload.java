package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：冰霜武具四件 + 「斩」的状态（HUD 那一行「冰霜武具：剑 锤 矛 弓x/7 斩」读它）。
 *
 * <p>充能 / 弹药 / 斩的冷却只存在服务端（{@code frost.FrostArmament} 与 {@code CryoGuillotineItem}），
 * 而 HUD 要显示「哪件带着、哪件可用了、弓还剩几发」，所以状态一变就推一份给拥有者。
 * 每种「字」各两个字段：<b>有没有带</b>与<b>是不是可用了</b>；弓另外带上「剩几发 / 上限」。
 *
 * <p>1.20.1 沿用旧 channel 写法：固定 channel id + 一对 write/read，字段顺序与 1.21.1 相同。
 */
public record FrostSyncPayload(boolean hasScabbard, boolean scabbardReady,
		boolean hasHammer, boolean hammerReady,
		boolean hasSpear, boolean spearReady,
		boolean hasBow, int bowAmmo, int bowMax,
		boolean hasGuillotine, boolean guillotineReady) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "frost_sync");

	/** 一件都没带（收 UI）。 */
	public static FrostSyncPayload none() {
		return new FrostSyncPayload(false, false, false, false, false, false, false, 0, 0, false, false);
	}

	public static void write(FriendlyByteBuf buffer, FrostSyncPayload payload) {
		buffer.writeBoolean(payload.hasScabbard());
		buffer.writeBoolean(payload.scabbardReady());
		buffer.writeBoolean(payload.hasHammer());
		buffer.writeBoolean(payload.hammerReady());
		buffer.writeBoolean(payload.hasSpear());
		buffer.writeBoolean(payload.spearReady());
		buffer.writeBoolean(payload.hasBow());
		buffer.writeVarInt(payload.bowAmmo());
		buffer.writeVarInt(payload.bowMax());
		buffer.writeBoolean(payload.hasGuillotine());
		buffer.writeBoolean(payload.guillotineReady());
	}

	public static FrostSyncPayload read(FriendlyByteBuf buffer) {
		return new FrostSyncPayload(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
				buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
				buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean());
	}
}
