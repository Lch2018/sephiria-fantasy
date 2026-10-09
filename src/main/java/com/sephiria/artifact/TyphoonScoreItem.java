package com.sephiria.artifact;

import com.sephiria.damage.SephiriaDamage;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 乐谱《台风》（羁绊品质，双连击：乌云 + 风之歌）：【唯一】乌云的消耗速度增加攻击速度的
 * 25/50/100%；武器攻击时造成 5/10/15 的闪电属性伤害；攻击速度 +4/8/12%（0..2 级）。
 *
 * <p>「乌云的消耗速度」就是乌云的攻速：默认 100%（每秒一次雷击），这一项按<b>攻击速度的百分之多少</b>
 * 往上加——攻速与消耗速度都 100% 时，25% 的档位把它顶到 125%，攻击间隔变成每 0.8 秒一次
 * （算法见 {@code cloud.DarkCloud}）。
 *
 * <p>武器攻击的附加闪电与「被雷击中的树枝」分开：这里只有武器攻击触发、没有冷却；走
 * {@code Kind.ELECTRIC}，所以吃麒麟的角的电属性暴击，也不会再触发电击之触。
 */
public class TyphoonScoreItem extends ArtifactItem {
	/** 各等级的「消耗速度按攻击速度加成」比例（%）。 */
	private static final double[] CLOUD_SPEED_BY_LEVEL = { 25.0D, 50.0D, 100.0D };
	/** 各等级武器攻击附加的闪电属性伤害（点，固定值）。 */
	private static final double[] WEAPON_LIGHTNING_BY_LEVEL = { 5.0D, 10.0D, 15.0D };
	/** 各等级的攻击速度加成（百分点）。 */
	private static final double[] ATTACK_SPEED_BY_LEVEL = { 4.0D, 8.0D, 12.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public TyphoonScoreItem(Properties properties) {
		super(properties);
	}

	/** 双连击：主连击是乌云，另一个是风之歌。 */
	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.DARK_CLOUD;
	}

	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.DARK_CLOUD, ArtifactCombo.WIND_SONG);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.typhoon_score.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CLOUD_SPEED_BY_LEVEL.length - 1;
	}

	@Override
	public double cloudSpeedPercentOfAttackSpeed(int level) {
		return valueAt(CLOUD_SPEED_BY_LEVEL, level);
	}

	@Override
	public double weaponLightningDamage(int level) {
		return valueAt(WEAPON_LIGHTNING_BY_LEVEL, level);
	}

	@Override
	public double attackSpeedBonus(int level) {
		return valueAt(ATTACK_SPEED_BY_LEVEL, level);
	}

	/** 挂在伤害事件上：武器攻击命中时附加闪电属性伤害（无冷却，只有武器触发）。 */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, blockedDamage, damageTaken, blocked) -> {
			if (damageTaken <= 0.0F || !(source.getEntity() instanceof ServerPlayer player) || entity == player) {
				return;
			}

			if (SephiriaDamage.kindOf(source) != SephiriaDamage.Kind.WEAPON) {
				return;
			}

			// 神器的附加闪电不打玩家：普攻 PVP 照旧，只是那一击不带这份电
			if (entity instanceof net.minecraft.world.entity.player.Player) {
				return;
			}

			double flat = ArtifactEffects.weaponLightningDamage(player);

			if (flat <= 0.0D || !(entity.level() instanceof ServerLevel level)) {
				return;
			}

			// 与触电结算同理：额外那一下先清无敌帧，免得被目标上一刻吃到的伤害顶掉
			entity.setInvulnerableTime(0);
			entity.hurtServer(level, SephiriaDamage.electricAttack(level, player), (float) flat);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY(0.5D), entity.getZ(),
					6, 0.3D, 0.4D, 0.3D, 0.05D);
		});
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.cloud_speed",
						Component.literal(Numbers.format(this.cloudSpeedPercentOfAttackSpeed(level)))
								.withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria.affix.weapon_lightning",
						Component.literal(Numbers.format(this.weaponLightningDamage(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria.affix.attack_speed",
						Component.literal(Numbers.format(this.attackSpeedBonus(level))).withColor(COLOUR_BONUS)));
	}
}
