package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端 → 服务端：神铸批量购买。
 *
 * <p>只带物品的数字 id 列表；总价、够不够能量、能不能换都由服务端按当前注册表重新算。
 * 用列表而不是复用 {@link WishPoolClickPayload#forge(int)}，是因为批量购买必须一次扣总价、
 * 一次发结果，否则会连续弹好几轮获得动画。
 */
public record WishPoolForgePayload(List<Integer> itemIds) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<WishPoolForgePayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "wish_pool_forge"));

	public static final StreamCodec<FriendlyByteBuf, WishPoolForgePayload> STREAM_CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeVarInt(payload.itemIds().size());

				for (int id : payload.itemIds()) {
					buffer.writeVarInt(id);
				}
			},
			buffer -> {
				int size = buffer.readVarInt();
				List<Integer> ids = new ArrayList<>(size);

				for (int index = 0; index < size; index++) {
					ids.add(buffer.readVarInt());
				}

				return new WishPoolForgePayload(List.copyOf(ids));
			});

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
