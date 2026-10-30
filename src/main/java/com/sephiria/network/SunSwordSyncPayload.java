package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：太阳剑的现有数量 / 上限。
 *
 * <p>数量只存在服务端（{@code sun/SunSword}），而 HUD 要显示「太阳剑：x/y + 进度条」，
 * 所以每次变化都推一份给拥有者。上限 &gt; 0 就代表太阳剑处于激活状态（连击 ≥ 2 档），
 * 收剑（掉档 / 退出）时推一份 0/0 让 UI 消失。
 *
 * <p>1.20.1 沿用旧 channel 写法：固定 channel id + 一对 write/read。
 */
public record SunSwordSyncPayload(double current, double max) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "sun_sword_sync");

	public static void write(FriendlyByteBuf buffer, SunSwordSyncPayload payload) {
		buffer.writeDouble(payload.current());
		buffer.writeDouble(payload.max());
	}

	public static SunSwordSyncPayload read(FriendlyByteBuf buffer) {
		return new SunSwordSyncPayload(buffer.readDouble(), buffer.readDouble());
	}
}
