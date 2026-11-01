package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 服务端 → 客户端：这一次选卡的三张卡面、每张卡刷没刷过、还剩几次机会。
 *
 * <p>卡面由服务端掷好再发（客户端没有卡池数据，也不能自己造卡）；
 * {@code open = false} 表示这次会话已经结束（选完了），客户端收到就关界面。
 *
 * <p>刷新状态用位掩码传：第 {@code i} 位 = 第 {@code i} 张卡是否已经用过刷新。
 */
public record CardSelectionPayload(List<ItemStack> cards, int refreshUsedMask, int chances, float chargeProgress, boolean open)
		implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CardSelectionPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "card_selection"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CardSelectionPayload> STREAM_CODEC =
			StreamCodec.composite(
					ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), CardSelectionPayload::cards,
					ByteBufCodecs.VAR_INT, CardSelectionPayload::refreshUsedMask,
					ByteBufCodecs.VAR_INT, CardSelectionPayload::chances,
					ByteBufCodecs.FLOAT, CardSelectionPayload::chargeProgress,
					ByteBufCodecs.BOOL, CardSelectionPayload::open,
					CardSelectionPayload::new);

	/** 第 {@code index} 张卡是否已经刷过。 */
	public boolean refreshUsed(int index) {
		return (this.refreshUsedMask & (1 << index)) != 0;
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
