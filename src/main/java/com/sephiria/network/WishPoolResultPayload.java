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
 * 服务端 → 客户端：这一抽（或这一次神铸）抽到了什么，用来播抽卡动画。
 *
 * <p>用户口径：「点击单抽或十连，会进入抽卡动画，展示你抽取的物品，动画结束会返回当前卡池页面」。
 * 物品本身已经在服务端从许愿池弹到世界里了，这个包只是让客户端知道该展示哪几张图。
 *
 * <p>{@code energyGained} 是服务端实际到账的愿力（含锚定加成，向下取整）。客户端不能自己按
 * {@code WishPool.energyFor} 重算——那样会漏掉锚定加成，抽卡结果页的数字会偏小。
 */
public record WishPoolResultPayload(List<ItemStack> items, int energyGained) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<WishPoolResultPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "wish_pool_result"));

	public static final StreamCodec<RegistryFriendlyByteBuf, WishPoolResultPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), WishPoolResultPayload::items,
			ByteBufCodecs.VAR_INT, WishPoolResultPayload::energyGained,
			WishPoolResultPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
