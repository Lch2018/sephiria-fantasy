package com.sephiria.network;

import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：剑盾防御中按了左键，请求释放「横扫」。空包，服务端自己查是谁、手里是什么。
 *
 * <p>为什么需要单独发包：26.2 的客户端在<b>使用物品期间会把左键整个吞掉</b>
 * （{@code Minecraft#handleKeybinds} 里 {@code isUsingItem()} 分支只 consumeClick、既不
 * startAttack 也不发攻击包），所以防御时按左键根本走不到 {@code AttackEntityCallback}，
 * 只能由客户端自己抢下这次点击再发这个包。
 */
public record ShieldSweepPayload() implements CustomPacketPayload {
	public static final ShieldSweepPayload INSTANCE = new ShieldSweepPayload();

	public static final CustomPacketPayload.Type<ShieldSweepPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "shield_sweep"));

	public static final StreamCodec<ByteBuf, ShieldSweepPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
