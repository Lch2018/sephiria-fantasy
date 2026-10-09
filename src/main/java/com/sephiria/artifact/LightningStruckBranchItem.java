package com.sephiria.artifact;

import com.sephiria.damage.SephiriaDamage;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 被雷击中的树枝（魔法科技，普通品质）：【唯一】使用武器攻击或魔法书造成伤害时，
 * 对目标附加 10/20/30 闪电属性伤害，冷却 2 秒（0..2 级）。
 *
 * <p>附加的那一下走 {@code sephiria:electric_shock}（{@code Kind.ELECTRIC}）：固定值、不吃减伤与
 * 黄金之手，但吃「电属性攻击的暴击几率」（麒麟的角）；它也不会再触发电击之触，防链式。
 * 「武器攻击或魔法书造成伤害」的实现口径与电击之触同一套守门（{@code Kind.WEAPON / ARTIFACT}）。
 */
public class LightningStruckBranchItem extends ArtifactItem {
	/** 各等级附加的闪电属性伤害（点，固定值）。 */
	private static final double[] DAMAGE_BY_LEVEL = { 10.0D, 20.0D, 30.0D };
	/** 冷却（tick）：2 秒，按攻击者记。 */
	private static final int COOLDOWN_TICKS = 40;
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;
	/** 冷却表：攻击者 → 上次触发的时间刻。 */
	private static final Map<UUID, Long> LAST_HIT = new HashMap<>();

	public LightningStruckBranchItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.MAGIC_TECH;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.lightning_struck_branch.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DAMAGE_BY_LEVEL.length - 1;
	}

	@Override
	public double onHitLightningDamage(int level) {
		return valueAt(DAMAGE_BY_LEVEL, level);
	}

	/** 挂在伤害事件上：武器 / 魔法书打出的伤害触发附加闪电（冷却 2 秒）。 */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, blockedDamage, damageTaken, blocked) -> {
			if (damageTaken <= 0.0F || !(source.getEntity() instanceof ServerPlayer player) || entity == player) {
				return;
			}

			SephiriaDamage.Kind kind = SephiriaDamage.kindOf(source);

			if (kind != SephiriaDamage.Kind.WEAPON && kind != SephiriaDamage.Kind.ARTIFACT) {
				return;
			}

			// 神器的附加闪电不打玩家：普攻 PVP 照旧，只是那一击不带这份电
			if (entity instanceof net.minecraft.world.entity.player.Player) {
				return;
			}

			double flat = ArtifactEffects.onHitLightningDamage(player);

			if (flat <= 0.0D || !(entity.level() instanceof ServerLevel level)) {
				return;
			}

			long now = level.getGameTime();
			Long last = LAST_HIT.get(player.getUUID());

			// 与电击之触同一处坑：别拿 Long.MIN_VALUE 当「从没打过」的初值——now - MIN_VALUE
			// 溢出成负数，第一次判定就当「还在冷却」，附加闪电从此永远不触发；用 null 判。
			if (last != null && now - last < COOLDOWN_TICKS) {
				return;
			}

			LAST_HIT.put(player.getUUID(), now);
			// 与触电结算同理：额外那一下先清无敌帧，免得被目标上一刻吃到的伤害顶掉
			entity.setInvulnerableTime(0);
			entity.hurtServer(level, SephiriaDamage.electricAttack(level, player), (float) flat);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY(0.5D), entity.getZ(),
					8, 0.3D, 0.4D, 0.3D, 0.05D);
		});
	}

	/** 玩家退出时清冷却表（与 Debuffs.forget 同一批调用）。 */
	public static void forget(ServerPlayer player) {
		LAST_HIT.remove(player.getUUID());
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria.affix.on_hit_lightning",
				Component.literal(Numbers.format(this.onHitLightningDamage(level))).withColor(COLOUR_BONUS)));
	}
}
