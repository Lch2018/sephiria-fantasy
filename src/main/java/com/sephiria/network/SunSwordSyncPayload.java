package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：太阳剑的现有数量 / 上限。
 *
 * <p>数量只存在服务端（{@code sun/SunSword}），而 HUD 要显示「太阳剑：x/y + 进度条」，
 * 所以每次变化都推一份给拥有者。上限 &gt; 0 就代表太阳剑处于激活状态（连击 ≥ 2 档），
 * 收剑（掉档 / 退出）时推一份 0/0 让 UI 消失。
 */
public record SunSwordSyncPayload(double current, double max) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SunSwordSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "sun_sword_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SunSwordSyncPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.DOUBLE, SunSwordSyncPayload::current,
					ByteBufCodecs.DOUBLE, SunSwordSyncPayload::max,
					SunSwordSyncPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
