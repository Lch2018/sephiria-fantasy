package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** 客户端 → 服务端：神铸批量购买（物品数字 id 列表）。 */
public record WishPoolForgePayload(List<Integer> itemIds) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "wish_pool_forge");

	public static void write(FriendlyByteBuf buffer, WishPoolForgePayload payload) {
		buffer.writeVarInt(payload.itemIds().size());

		for (int id : payload.itemIds()) {
			buffer.writeVarInt(id);
		}
	}

	public static WishPoolForgePayload read(FriendlyByteBuf buffer) {
		int size = buffer.readVarInt();
		List<Integer> ids = new ArrayList<>(size);

		for (int index = 0; index < size; index++) {
			ids.add(buffer.readVarInt());
		}

		return new WishPoolForgePayload(List.copyOf(ids));
	}
}
