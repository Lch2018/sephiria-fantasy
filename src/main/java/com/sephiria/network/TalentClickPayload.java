package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：天赋界面上的按钮。
 *
 * <p>只带最小信息：哪一条天赋页（0 = 愤怒、1 = 迅捷）、加减多少、是不是重置。
 * 「还剩多少点」「这一页加到几点」「有没有到上限」全部由服务端用
 * {@code talent/Talents} 里那份存档判定——客户端发什么都不会凭空多出点数。
 */
public record TalentClickPayload(int page, int delta, boolean reset) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TalentClickPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "talent_click"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TalentClickPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TalentClickPayload::page,
			ByteBufCodecs.VAR_INT, TalentClickPayload::delta,
			ByteBufCodecs.BOOL, TalentClickPayload::reset,
			TalentClickPayload::new);

	/** 加减按钮：page = 天赋页下标，delta = ±1 / ±5。 */
	public static TalentClickPayload adjust(int page, int delta) {
		return new TalentClickPayload(page, delta, false);
	}

	/** 「重置」按钮：两条天赋页一起清零。 */
	public static TalentClickPayload resetAll() {
		return new TalentClickPayload(0, 0, true);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
