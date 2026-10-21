package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：把某个背包格里的石板刻印掉（长按进度满）。
 *
 * <p>只带菜单格子编号；「是不是背包格、真实下标、三阶段是否点亮、格子里是不是石板」全部由
 * 服务端用 {@link com.sephiria.backpack.BackpackGridMenu} 与天赋状态再判一次。
 */
public record SlateImprintPayload(int slot) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SlateImprintPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "slate_imprint"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SlateImprintPayload> STREAM_CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, SlateImprintPayload::slot, SlateImprintPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
