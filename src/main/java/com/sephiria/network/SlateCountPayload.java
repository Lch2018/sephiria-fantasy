package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：当前持有的石板物品数量（黑鳞片的提示框用）。
 *
 * <p>口径由 {@code ArtifactEffects#slateCount} 统一：赛菲莉娅背包里的石板 + 石板刻印保留的，
 * 合成石板算 2 块。数量随属性同步一起推。
 */
public record SlateCountPayload(int count) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SlateCountPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "slate_count"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SlateCountPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SlateCountPayload::count,
			SlateCountPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
