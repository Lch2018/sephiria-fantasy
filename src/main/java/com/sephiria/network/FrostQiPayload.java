package com.sephiria.network;

import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：玩家<b>用左键挥了一次武器</b>（可能是打中了什么，也可能只是挥空）。
 *
 * <p>冰霜武具的驱邪剑鞘要「挥击即发剑气、不需要打中敌人」，而 26.3 里挥空<b>根本不会告诉服务端</b>
 * （原版的挥手动画是服务端 → 客户端的 {@code ClientboundSwingAnimationPacket}，没有反向包；
 * 打实体走 {@code ServerboundAttackPacket}、挖方块走 {@code ServerboundPlayerActionPacket}），
 * 所以客户端在 {@code Minecraft#startAttack}（每次左键点击一次）里补发这一个包。
 *
 * <p>包里不带内容，服务端收到后自己查是哪个玩家、判充能与主手物品（客户端只说「我挥了一下」）。
 */
public record FrostQiPayload() implements CustomPacketPayload {
	public static final FrostQiPayload INSTANCE = new FrostQiPayload();

	public static final CustomPacketPayload.Type<FrostQiPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "frost_qi"));

	/** 空包：不写任何字节。 */
	public static final StreamCodec<ByteBuf, FrostQiPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
