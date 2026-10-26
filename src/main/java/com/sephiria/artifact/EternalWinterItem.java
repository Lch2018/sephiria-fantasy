package com.sephiria.artifact;

import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 永恒的冬日（冰川 + 坚固，羁绊品质）：【唯一】普通攻击命中时，有 11/16.5/24.8% 概率
 * 追加一段「物理强度 × 20/35/50% + 冰元素强度 × 40/70/120%」的伤害（视为普通攻击伤害）。
 *
 * <p><b>双连击</b>神器：同时给冰川与坚固各加 1 级（见 {@link #combos()}）。
 *
 * <p>额外那一下走 {@link SephiriaDamage#eternalWinter}（{@code Kind.WEAPON}）：吃普通攻击的
 * 暴击、黄金之手与「造成武器伤害时」那一串口径，所以它是真正的「普通攻击伤害」。
 * 技能打出的伤害排除在外（技能与普攻共用 {@code playerAttack} 来源，靠
 * {@link SephiriaDamage#isSkillAttack} 分辨）；横扫仍算普通攻击。
 */
public class EternalWinterItem extends ArtifactItem {
	/** 各等级的触发概率（%）。 */
	private static final double[] CHANCE_BY_LEVEL = { 11.0D, 16.5D, 24.8D };
	/** 各等级的物理部分倍率（%）：物理强度 × 它。 */
	private static final double[] PHYSICAL_BY_LEVEL = { 20.0D, 35.0D, 50.0D };
	/** 各等级的冰部分倍率（%）：冰元素强度 × 它。 */
	private static final double[] ICE_BY_LEVEL = { 40.0D, 70.0D, 120.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public EternalWinterItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GLACIER;
	}

	/** 双连击：冰川与坚固各 +1 级。 */
	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.GLACIER, ArtifactCombo.STURDY);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.eternal_winter.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CHANCE_BY_LEVEL.length - 1;
	}

	@Override
	public double eternalWinterChance(int level) {
		return valueAt(CHANCE_BY_LEVEL, level);
	}

	@Override
	public double eternalWinterPhysicalPercent(int level) {
		return valueAt(PHYSICAL_BY_LEVEL, level);
	}

	@Override
	public double eternalWinterIcePercent(int level) {
		return valueAt(ICE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		Component chance = Component.literal(Numbers.format(this.eternalWinterChance(level)))
				.withColor(COLOUR_BONUS);
		Component physical = Component.literal(Numbers.format(this.eternalWinterPhysicalPercent(level)))
				.withColor(COLOUR_BONUS);
		Component ice = Component.literal(Numbers.format(this.eternalWinterIcePercent(level)))
				.withColor(COLOUR_BONUS);

		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.eternal_winter", chance, physical, ice));
	}

	/** 挂在伤害事件上：普通攻击命中后按概率补一段冰霜伤害。 */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, blockedDamage, damageTaken, blocked) -> {
			if (damageTaken <= 0.0F || !(source.getEntity() instanceof ServerPlayer player) || entity == player) {
				return;
			}

			if (SephiriaDamage.kindOf(source) != SephiriaDamage.Kind.WEAPON
					|| source.is(SephiriaDamage.ETERNAL_WINTER)
					|| SephiriaDamage.isSkillAttack(player)) {
				return;
			}

			// 额外那一下不打玩家：普攻 PVP 照旧，只是那一击不带这份冰
			if (entity instanceof net.minecraft.world.entity.player.Player) {
				return;
			}

			double chance = ArtifactEffects.eternalWinterChance(player);

			if (chance <= 0.0D || !(entity.level() instanceof ServerLevel level)
					|| level.getRandom().nextDouble() * 100.0D >= chance) {
				return;
			}

			double physical = PlayerStats.physicalTotal(player)
					* ArtifactEffects.eternalWinterPhysicalPercent(player) / 100.0D;
			double ice = PlayerStats.elementTotal(player, PlayerStats.Element.ICE)
					* ArtifactEffects.eternalWinterIcePercent(player) / 100.0D;
			float extra = (float) (physical + ice);

			if (extra <= 0.0F) {
				return;
			}

			// 额外那一下先清无敌帧，免得被目标上一刻吃到的伤害顶掉
			SephiriaDamage.clearDamageCooldown(entity);
			entity.hurtServer(level, SephiriaDamage.eternalWinter(level, player), extra);
			level.sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY(0.5D), entity.getZ(),
					10, 0.35D, 0.5D, 0.35D, 0.04D);
			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
					SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 0.5F,
					1.1F + level.getRandom().nextFloat() * 0.2F);
		});
	}
}
