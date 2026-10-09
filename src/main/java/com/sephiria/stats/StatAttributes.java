package com.sephiria.stats;

import com.sephiria.Sephiria;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 把「面板上算出来的属性」接到原版属性表上——移动速度与最大生命值。
 *
 * <p>做法与 {@link WeaponStats} 一致：给玩家挂一条临时修饰符，每几 tick 对一次账，
 * 而且以属性表上的当前值为准（复活会换实体，只比自家缓存的话复活后就再也补不回来）。
 * 移动速度按基础值缩放，所以加速药水、疾跑这些原版加成照旧叠加在上面；
 * 最大生命值直接加固定点数（索利斯·弗拉克托那类「最大HP +N」），上限提高后当前血量不跟着涨，
 * 那部分血要自己回满——和原版加血量上限的观感一致。
 */
public final class StatAttributes {
	private static final Identifier MOVE_SPEED_MODIFIER =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "attr_move_speed");
	private static final Identifier MAX_HP_MODIFIER =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "attr_max_hp");
	/** 检查间隔（tick）：属性变化后最多等这么久就会重算。 */
	private static final int CHECK_INTERVAL = 4;

	private static int ticker;

	private StatAttributes() {
	}

	/** 注册推进器（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(StatAttributes::tick);
	}

	/** 立刻按当前属性刷新一次（进服、属性变化时调用）。 */
	public static void refresh(ServerPlayer player) {
		applyMoveSpeed(player);
		applyMaxHp(player);
	}

	private static void tick(MinecraftServer server) {
		if (++ticker % CHECK_INTERVAL != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			applyMoveSpeed(player);
			applyMaxHp(player);
		}
	}

	private static void applyMoveSpeed(ServerPlayer player) {
		double multiplier = PlayerStats.moveSpeedMultiplier(player);
		AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);

		if (instance == null) {
			return;
		}

		// 增量 = 基础移速 × (倍率 − 1)：倍率为 1（默认 100%）时恰好不加不减
		double amount = instance.getBaseValue() * (multiplier - 1.0D);
		AttributeModifier current = instance.getModifier(MOVE_SPEED_MODIFIER);

		if (amount == 0.0D) {
			if (current != null) {
				instance.removeModifier(MOVE_SPEED_MODIFIER);
			}

			return;
		}

		if (current == null || current.amount() != amount) {
			instance.addOrUpdateTransientModifier(
					new AttributeModifier(MOVE_SPEED_MODIFIER, amount, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	/** 最大生命值：神器给的固定点数直接加（0 就把修饰符摘掉）。 */
	private static void applyMaxHp(ServerPlayer player) {
		double amount = PlayerStats.maxHpBonus(player);
		AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);

		if (instance == null) {
			return;
		}

		AttributeModifier current = instance.getModifier(MAX_HP_MODIFIER);

		if (amount == 0.0D) {
			if (current != null) {
				// 摘掉修饰符会顺手把当前血量夹回新上限，不会留下超上限的血
				instance.removeModifier(MAX_HP_MODIFIER);
			}

			return;
		}

		if (current == null || current.amount() != amount) {
			instance.addOrUpdateTransientModifier(
					new AttributeModifier(MAX_HP_MODIFIER, amount, AttributeModifier.Operation.ADD_VALUE));
		}
	}
}
