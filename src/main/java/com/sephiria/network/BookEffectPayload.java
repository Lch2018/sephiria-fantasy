package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：书「效率」给当前玩家的挖掘速度倍率。
 *
 * <p>挖掘速度要在客户端预测方块破坏进度，所以不能只在服务端算；倍率由服务端按
 * 背包里的书与赛菲莉娅攻速算好后推过来，客户端 {@code Player.getDestroySpeed} 的
 * mixin 直接乘它。横扫之刃的冷却倍率走 ATTACK_SPEED 属性修饰符，原版会自己同步，
 * 不需要放在这个包里。
 */
public record BookEffectPayload(float miningSpeedFactor) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<BookEffectPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "book_effect"));

	public static final StreamCodec<RegistryFriendlyByteBuf, BookEffectPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.FLOAT, BookEffectPayload::miningSpeedFactor,
					BookEffectPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
