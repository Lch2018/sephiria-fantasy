package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：冰霜武具四件 + 「斩」的状态（HUD 那一行「冰霜武具：剑 锤 矛 弓x/7 斩」读它）。
 *
 * <p>充能 / 弹药 / 斩的冷却只存在服务端（{@code frost.FrostArmament} 与 {@code CryoGuillotineItem}），
 * 而 HUD 要显示「哪件带着、哪件可用了、弓还剩几发」，所以状态一变就推一份给拥有者
 * （与乌云 / 太阳剑的「变化才推」同一套）。每种「字」各两个字段：
 * <b>有没有带</b>与<b>是不是可用了</b>。弓另外带上「剩几发 / 上限」，HUD 显示成 {@code 弓3/7}；
 * 斩只看冷却是不是已经转好。
 */
public record FrostSyncPayload(boolean hasScabbard, boolean scabbardReady,
		boolean hasHammer, boolean hammerReady,
		boolean hasSpear, boolean spearReady,
		boolean hasBow, int bowAmmo, int bowMax,
		boolean hasGuillotine, boolean guillotineReady) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<FrostSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "frost_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, FrostSyncPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.BOOL, FrostSyncPayload::hasScabbard,
					ByteBufCodecs.BOOL, FrostSyncPayload::scabbardReady,
					ByteBufCodecs.BOOL, FrostSyncPayload::hasHammer,
					ByteBufCodecs.BOOL, FrostSyncPayload::hammerReady,
					ByteBufCodecs.BOOL, FrostSyncPayload::hasSpear,
					ByteBufCodecs.BOOL, FrostSyncPayload::spearReady,
					ByteBufCodecs.BOOL, FrostSyncPayload::hasBow,
					ByteBufCodecs.VAR_INT, FrostSyncPayload::bowAmmo,
					ByteBufCodecs.VAR_INT, FrostSyncPayload::bowMax,
					ByteBufCodecs.BOOL, FrostSyncPayload::hasGuillotine,
					ByteBufCodecs.BOOL, FrostSyncPayload::guillotineReady,
					FrostSyncPayload::new);

	/** 一件都没带（收 UI）。 */
	public static FrostSyncPayload none() {
		return new FrostSyncPayload(false, false, false, false, false, false, false, 0, 0, false, false);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
