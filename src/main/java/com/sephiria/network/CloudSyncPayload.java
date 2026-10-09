package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：乌云容量的当前值 / 上限。
 *
 * <p>容量只存在服务端（{@code cloud/DarkCloud}），而 HUD 要显示「乌云：x/y + 进度条」，
 * 所以每次变化都推一份给拥有者。上限 &gt; 0 就代表乌云处于激活状态（连击 ≥ 2 档），
 * 收云（掉档 / 死亡 / 退出）时推一份 0/0 让 UI 消失。
 */
public record CloudSyncPayload(double capacity, double max) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CloudSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "cloud_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CloudSyncPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, CloudSyncPayload::capacity,
			ByteBufCodecs.DOUBLE, CloudSyncPayload::max,
			CloudSyncPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
