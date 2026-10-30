package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：乌云容量的当前值 / 上限。
 *
 * <p>容量只存在服务端（{@code cloud/DarkCloud}），而 HUD 要显示「乌云：x/y + 进度条」，
 * 所以每次变化都推一份给拥有者。上限 &gt; 0 就代表乌云处于激活状态（连击 ≥ 2 档），
 * 收云（掉档 / 死亡 / 退出）时推一份 0/0 让 UI 消失。
 *
 * <p>1.20.1 沿用旧 channel 写法：固定 channel id + 一对 write/read。
 */
public record CloudSyncPayload(double capacity, double max) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "cloud_sync");

	public static void write(FriendlyByteBuf buffer, CloudSyncPayload payload) {
		buffer.writeDouble(payload.capacity());
		buffer.writeDouble(payload.max());
	}

	public static CloudSyncPayload read(FriendlyByteBuf buffer) {
		return new CloudSyncPayload(buffer.readDouble(), buffer.readDouble());
	}
}
