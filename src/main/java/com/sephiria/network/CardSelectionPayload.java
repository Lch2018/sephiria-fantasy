package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 服务端 → 客户端：这一次选卡的三张卡面、刷新状态、剩余机会。 */
public record CardSelectionPayload(List<ItemStack> cards, int refreshUsedMask, int chances,
		float chargeProgress, boolean open) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "card_selection");

	public static void write(FriendlyByteBuf buffer, CardSelectionPayload payload) {
		buffer.writeVarInt(payload.cards().size());

		for (ItemStack stack : payload.cards()) {
			buffer.writeItem(stack);
		}

		buffer.writeVarInt(payload.refreshUsedMask());
		buffer.writeVarInt(payload.chances());
		buffer.writeFloat(payload.chargeProgress());
		buffer.writeBoolean(payload.open());
	}

	public static CardSelectionPayload read(FriendlyByteBuf buffer) {
		int count = buffer.readVarInt();
		List<ItemStack> cards = new ArrayList<>(count);

		for (int index = 0; index < count; index++) {
			cards.add(buffer.readItem());
		}

		return new CardSelectionPayload(List.copyOf(cards), buffer.readVarInt(), buffer.readVarInt(),
				buffer.readFloat(), buffer.readBoolean());
	}

	/** 第 {@code index} 张卡是否已经刷过。 */
	public boolean refreshUsed(int index) {
		return (this.refreshUsedMask & (1 << index)) != 0;
	}
}
