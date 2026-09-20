package com.sephiria.stats;

import com.sephiria.Sephiria;
import com.sephiria.weapon.SephiriaWeapon;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 把属性系统接到武器的普通攻击上（伤害与攻速；技能另算，攻速不影响技能）。
 *
 * <p><b>基准从物品自己读</b>，不在代码里再抄一份数值：物品的 {@code ATTRIBUTE_MODIFIERS} 里，
 * ATTACK_DAMAGE 的加成等于"面板伤害 − 1"、ATTACK_SPEED 的加成等于"基础攻速 − 4"（1 是玩家
 * 基础攻击力、4 是玩家基础攻速），所以从当前手持物品就能反推出这把武器的基础值——刀这种
 * 两态武器切状态会换组件，读到的自然是当前状态的基准。
 *
 * <p>然后给玩家挂两个<b>临时修饰符</b>（按玩家、不写回物品，所以多人各算各的）：
 * <pre>
 *   伤害增量 = 面板伤害 × (物理强度 / 20 − 1)
 *   攻速增量 = 基础攻速 × (攻击速度 / 100 − 1)
 * </pre>
 * 属性为默认值时增量恰好为 0，也就是表里的数值原样生效。
 *
 * <p>不需要每 tick 重算：只在"手持物变化"或"数值变化"时才写一次，其余时刻直接跳过。
 */
public final class WeaponStats {
	private static final Identifier DAMAGE_MODIFIER = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "attr_weapon_damage");
	private static final Identifier SPEED_MODIFIER = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "attr_weapon_speed");
	/** 检查间隔（tick）：换武器后最多等这么久就会重算。 */
	private static final int CHECK_INTERVAL = 4;

	/** 上次算出来的增量，用来判断是否需要写属性（避免每轮都重建修饰符）。 */
	private static final Map<UUID, double[]> APPLIED = new HashMap<>();

	private static int ticker;

	private WeaponStats() {
	}

	/** 注册推进器（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(WeaponStats::tick);
	}

	/** 立刻按当前属性和手持物刷新一次（进服、属性变化时调用）。 */
	public static void refresh(ServerPlayer player) {
		apply(player, of(player));
	}

	private static void tick(MinecraftServer server) {
		if (++ticker % CHECK_INTERVAL != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			apply(player, of(player));
		}
	}

	private static double[] of(ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();

		if (!(stack.getItem() instanceof SephiriaWeapon)) {
			return new double[]{0.0D, 0.0D};
		}

		double damage = 0.0D;
		double speed = 0.0D;

		for (ItemAttributeModifiers.Entry entry : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
			Holder<Attribute> attribute = entry.attribute();

			if (attribute.is(Attributes.ATTACK_DAMAGE)) {
				damage = 1.0D + entry.modifier().amount();
			}
			else if (attribute.is(Attributes.ATTACK_SPEED)) {
				speed = Attributes.DEFAULT_ATTACK_SPEED + entry.modifier().amount();
			}
		}

		PlayerStats.Values values = PlayerStats.of(player);

		return new double[]{
				damage * (values.physical / PlayerStats.DEFAULT_STRENGTH - 1.0D),
				speed * (values.attackSpeed / PlayerStats.DEFAULT_ATTACK_SPEED - 1.0D)};
	}

	private static void apply(ServerPlayer player, double[] deltas) {
		double[] previous = APPLIED.get(player.getUUID());

		if (previous != null && previous[0] == deltas[0] && previous[1] == deltas[1]) {
			return;
		}

		APPLIED.put(player.getUUID(), deltas);
		player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(DAMAGE_MODIFIER);
		player.getAttribute(Attributes.ATTACK_SPEED).removeModifier(SPEED_MODIFIER);

		if (deltas[0] != 0.0D) {
			player.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(
					new AttributeModifier(DAMAGE_MODIFIER, deltas[0], AttributeModifier.Operation.ADD_VALUE));
		}

		if (deltas[1] != 0.0D) {
			player.getAttribute(Attributes.ATTACK_SPEED).addTransientModifier(
					new AttributeModifier(SPEED_MODIFIER, deltas[1], AttributeModifier.Operation.ADD_VALUE));
		}
	}
}
