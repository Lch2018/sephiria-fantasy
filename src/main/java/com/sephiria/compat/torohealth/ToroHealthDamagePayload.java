package com.sephiria.compat.torohealth;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：一次赛菲莉娅伤害的显示信息，供 ToroHealth 的浮动伤害数字使用。
 *
 * <p>ToroHealth 的伤害数字是客户端从生命值变化自己生成的，拿不到伤害来源；所以由我们在
 * 伤害入口（{@code SephiriaDamage#applyCrit}）先推这一小包，客户端按目标实体匹配到
 * ToroHealth 刚生成的粒子后染色 / 放大 / 改数字。没有 ToroHealth 的客户端收到包会直接忽略。
 *
 * @param entityId        被打的目标实体 id
 * @param element         主元素：0 物理 / 1 火 / 2 冰 / 3 电 / 4 混沌
 * @param secondaryElement 双属性的副元素（0..3，-1 = 单属性）
 * @param displayColor    这一发最终显示色（服务端按元素集合查表得到）
 * @param crit            这一下是否暴击（暴击数字放大 2 倍）
 * @param preTaxDamage    税前伤害（四舍五入后的整数）：显示设置勾选「税前伤害」时用它
 * @param x               目标位置（实体不在客户端时的兜底匹配）
 */
public record ToroHealthDamagePayload(int entityId, int element, int secondaryElement, int displayColor,
		boolean crit, int preTaxDamage, double x, double y, double z) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ToroHealthDamagePayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "torohealth_damage"));

	public static final StreamCodec<FriendlyByteBuf, ToroHealthDamagePayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, ToroHealthDamagePayload payload) -> {
				buffer.writeVarInt(payload.entityId());
				buffer.writeVarInt(payload.element() + 1);
				buffer.writeVarInt(payload.secondaryElement() + 1);
				buffer.writeVarInt(payload.displayColor() & 0xFFFFFF);
				buffer.writeBoolean(payload.crit());
				buffer.writeVarInt(payload.preTaxDamage());
				buffer.writeDouble(payload.x());
				buffer.writeDouble(payload.y());
				buffer.writeDouble(payload.z());
			},
			(FriendlyByteBuf buffer) -> new ToroHealthDamagePayload(
					buffer.readVarInt(), buffer.readVarInt() - 1, buffer.readVarInt() - 1,
					buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(),
					buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
