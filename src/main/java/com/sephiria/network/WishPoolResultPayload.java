package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 服务端 → 客户端：这一抽实际抽到的物品（供翻牌动画显示）。 */
public record WishPoolResultPayload(List<ItemStack> items) {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "wish_pool_result");

	public static void write(FriendlyByteBuf buffer, WishPoolResultPayload payload) {
		buffer.writeVarInt(payload.items().size());

		for (ItemStack stack : payload.items()) {
			buffer.writeItem(stack);
		}
	}

	public static WishPoolResultPayload read(FriendlyByteBuf buffer) {
		int count = buffer.readVarInt();
		List<ItemStack> items = new ArrayList<>(count);

		for (int index = 0; index < count; index++) {
			items.add(buffer.readItem());
		}

		return new WishPoolResultPayload(items);
	}
}
