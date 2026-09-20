package com.sephiria.network;

import com.sephiria.Sephiria;
import com.sephiria.stats.PlayerStats;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：玩家的自定义属性最新值（进服与每次变更时各推一次）。
 *
 * <p>生命值不在包里——那是原版数据，客户端本来就有。
 *
 * <p>字段较多，超过了 {@code StreamCodec.composite} 的重载数量，所以手写编解码：
 * 顺序读写的 double，简单且不会因为字段增删而错位（增删时两边一起改）。
 */
public record StatsSyncPayload(double mp, double mpRegen, double physical, double fire, double ice,
		double lightning, double defense, double attackSpeed, double meleeRange) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<StatsSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "stats_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, StatsSyncPayload> STREAM_CODEC = StreamCodec.of(
			(RegistryFriendlyByteBuf buffer, StatsSyncPayload payload) -> {
				buffer.writeDouble(payload.mp());
				buffer.writeDouble(payload.mpRegen());
				buffer.writeDouble(payload.physical());
				buffer.writeDouble(payload.fire());
				buffer.writeDouble(payload.ice());
				buffer.writeDouble(payload.lightning());
				buffer.writeDouble(payload.defense());
				buffer.writeDouble(payload.attackSpeed());
				buffer.writeDouble(payload.meleeRange());
			},
			(RegistryFriendlyByteBuf buffer) -> new StatsSyncPayload(
					buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
					buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
					buffer.readDouble()));

	public static StatsSyncPayload of(PlayerStats.Values values) {
		return new StatsSyncPayload(values.mp, values.mpRegen, values.physical, values.fire, values.ice,
				values.lightning, values.defense, values.attackSpeed, values.meleeRange);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
