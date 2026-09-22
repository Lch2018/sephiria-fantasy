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
 * 把「面板上算出来的属性」接到原版属性表上——目前只有移动速度。
 *
 * <p>做法与 {@link WeaponStats} 一致：给玩家挂一条临时修饰符，每几 tick 对一次账，
 * 而且以属性表上的当前值为准（复活会换实体，只比自家缓存的话复活后就再也补不回来）。
 * 移动速度按基础值缩放，所以加速药水、疾跑这些原版加成照旧叠加在上面。
 */
public final class StatAttributes {
	private static final Identifier MOVE_SPEED_MODIFIER =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "attr_move_speed");
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
		apply(player, PlayerStats.moveSpeedMultiplier(player));
	}

	private static void tick(MinecraftServer server) {
		if (++ticker % CHECK_INTERVAL != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			apply(player, PlayerStats.moveSpeedMultiplier(player));
		}
	}

	private static void apply(ServerPlayer player, double multiplier) {
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
}
