package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：某个实体进入了无敌窗口。
 *
 * <p>用途是<b>视觉</b>——无敌期间玩家的模型要描一圈白光。原版描边由
 * {@code Minecraft#shouldEntityAppearGlowing} 决定（发光效果/幽灵箭），而这个状态只有
 * 服务端知道，所以授予无敌时广播一次，客户端按刻数自己倒计时。
 */
public record InvulnerablePayload(int entityId, int ticks) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<InvulnerablePayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "invulnerable"));

	public static final StreamCodec<RegistryFriendlyByteBuf, InvulnerablePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, InvulnerablePayload::entityId,
			ByteBufCodecs.VAR_INT, InvulnerablePayload::ticks,
			InvulnerablePayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
