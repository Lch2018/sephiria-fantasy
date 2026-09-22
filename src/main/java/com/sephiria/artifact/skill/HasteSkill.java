package com.sephiria.artifact.skill;

import com.sephiria.Sephiria;
import com.sephiria.artifact.HasteGrimoireItem;
import com.sephiria.stats.TimedAttributes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 急速（魔法书「获得技能」）：攻击速度与移动速度 +15%，持续 24 秒，冷却 38 秒，蓝耗随等级降低。
 *
 * <p>两条加成都是限时属性修饰符（见 {@link TimedAttributes}）。攻速按<b>当前攻速的 15%</b> 算，
 * 所以它和武器本身的攻速、装备上的攻速加成是叠乘关系；移速同理，按基础移速的 15%。
 */
public final class HasteSkill implements ArtifactSkill {
	public static final HasteSkill INSTANCE = new HasteSkill();

	/** 持续 24 秒。 */
	private static final int DURATION_TICKS = 480;
	/** 冷却 38 秒。 */
	private static final int COOLDOWN_TICKS = 760;
	/** 攻速与移速各 +15%。 */
	private static final double PERCENT = 15.0D;

	private static final Identifier ATTACK_SPEED_ID =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "haste_attack_speed");
	private static final Identifier MOVE_SPEED_ID =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "haste_move_speed");

	private HasteSkill() {
	}

	@Override
	public String id() {
		return "haste";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return HasteGrimoireItem.hasteMpCost(level);
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		// 攻速：按当前攻速的 15%（与武器自身攻速、装备加成叠乘）
		double attackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
		TimedAttributes.grant(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID,
				attackSpeed * PERCENT / 100.0D, DURATION_TICKS);

		// 移速：按基础移速的 15%
		double baseMoveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED) == null
				? 0.1D
				: player.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
		TimedAttributes.grant(player, Attributes.MOVEMENT_SPEED, MOVE_SPEED_ID,
				baseMoveSpeed * PERCENT / 100.0D, DURATION_TICKS);

		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.4F);
		serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0D, player.getZ(),
				30, 0.5D, 0.8D, 0.5D, 0.4D);
	}
}
