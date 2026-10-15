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
 */
public record WishPoolResultPayload(List<ItemStack> items) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<WishPoolResultPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "wish_pool_result"));

	public static final StreamCodec<RegistryFriendlyByteBuf, WishPoolResultPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), WishPoolResultPayload::items,
			WishPoolResultPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
