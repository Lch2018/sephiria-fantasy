package com.sephiria.stats;

import com.sephiria.Sephiria;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 把「面板上算出来的属性」接到原版属性表上——移动速度与最大生命值。
 *
 * <p>做法与 {@link WeaponStats} 一致：给玩家挂一条临时修饰符，每几 tick 对一次账，
 * 而且以属性表上的当前值为准（复活会换实体，只比自家缓存的话复活后就再也补不回来）。
 * 移动速度按基础值缩放，所以加速药水、疾跑这些原版加成照旧叠加在上面；
 * 最大生命值：神器 / 天赋给固定点数（ADD_VALUE），药水给最终百分比（ADD_MULTIPLIED_TOTAL）；
 * 上限提高后当前血量不跟着涨，那部分血要自己回满——和原版加血量上限的观感一致。
 */
public final class StatAttributes {
	private static final Identifier MOVE_SPEED_MODIFIER =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "attr_move_speed");
	private static final Identifier MAX_HP_MODIFIER =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "attr_max_hp");
	/** 药水「最终 HP +5%」用的百分比修饰符（ADD_MULTIPLIED_TOTAL，叠在固定值之后）。 */
	private static final Identifier MAX_HP_PERCENT_MODIFIER =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "attr_max_hp_percent");
	/** 检查间隔（tick）：属性变化后最多等这么久就会重算。 */
	private static final int CHECK_INTERVAL = 4;
	/** 原版「幸运」buff 每次续多久（刻）：每 4 刻对账一次，时长比它大就不会闪断。 */
	private static final int LUCK_BUFF_TICKS = 60;

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
		applyLuck(player);
	}

	private static void tick(MinecraftServer server) {
		if (++ticker % CHECK_INTERVAL != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			applyMoveSpeed(player);
			applyMaxHp(player);
			applyLuck(player);
		}
	}

	/**
	 * 幸运值换原版「幸运」buff：每满 {@link PlayerStats#LUCK_PER_VANILLA_LEVEL} 点一级
	 * （6..11 → 幸运 I、12..17 → 幸运 II……），掉到 6 点以下就把 buff 摘掉。
	 *
	 * <p>时长 {@value #LUCK_BUFF_TICKS} 刻、每 {@value #CHECK_INTERVAL} 刻续一次（时间还够就不重发，
	 * 免得每秒发好几遍效果包）；粒子关掉（幸运本来也没有粒子），图标留着——它是实打实挂在身上的 buff。
	 */
	private static void applyLuck(ServerPlayer player) {
		int level = (int) Math.floor(PlayerStats.luckTotal(player) / PlayerStats.LUCK_PER_VANILLA_LEVEL);
		MobEffectInstance current = player.getEffect(MobEffects.LUCK);

		if (level <= 0) {
			if (current != null) {
				player.removeEffect(MobEffects.LUCK);
			}

			return;
		}

		if (current != null && current.getAmplifier() == level - 1
				&& current.getDuration() > LUCK_BUFF_TICKS / 2) {
			return;
		}

		player.addEffect(new MobEffectInstance(MobEffects.LUCK, LUCK_BUFF_TICKS, level - 1, false, false, true));
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

	/**
	 * 最大生命值：固定点数用 ADD_VALUE，药水的「最终 HP +5%」用 ADD_MULTIPLIED_TOTAL。
	 *
	 * <p>百分比放在固定值之后结算，所以多瓶叠加时每瓶都是按「加上其它加成后的最终上限」乘，
	 * 与描述里的「最终 HP」一致。0 时把对应修饰符摘掉。
	 */
	private static void applyMaxHp(ServerPlayer player) {
		AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);

		if (instance == null) {
			return;
		}

		double flat = PlayerStats.maxHpBonus(player);
		AttributeModifier currentFlat = instance.getModifier(MAX_HP_MODIFIER);

		if (flat == 0.0D) {
			if (currentFlat != null) {
				// 摘掉修饰符会顺手把当前血量夹回新上限，不会留下超上限的血
				instance.removeModifier(MAX_HP_MODIFIER);
			}
		} else if (currentFlat == null || currentFlat.amount() != flat) {
			instance.addOrUpdateTransientModifier(
					new AttributeModifier(MAX_HP_MODIFIER, flat, AttributeModifier.Operation.ADD_VALUE));
		}

		double percent = PlayerStats.maxHpPercentTotal(player) / 100.0D;
		AttributeModifier currentPercent = instance.getModifier(MAX_HP_PERCENT_MODIFIER);

		if (percent == 0.0D) {
			if (currentPercent != null) {
				instance.removeModifier(MAX_HP_PERCENT_MODIFIER);
			}
		} else if (currentPercent == null || currentPercent.amount() != percent) {
			instance.addOrUpdateTransientModifier(
					new AttributeModifier(MAX_HP_PERCENT_MODIFIER, percent, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}
}
