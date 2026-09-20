package com.sephiria.weapon;

import com.sephiria.damage.SephiriaDamage;
import com.sephiria.registry.ModItems;
import com.sephiria.stats.PlayerStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import java.util.HashSet;
import java.util.Set;

/**
 * 弩矢的实体：固定伤害 + 穿透，外加一个"来自 SEPHIRIA"的标记。
 *
 * <p>两点都和原版箭矢不同：
 * <ul>
 *   <li><b>固定伤害</b>：原版 {@code AbstractArrow#onHitEntity} 算的是"当前飞行速度 × 基础伤害"，
 *       所以射远了、打高了伤害都会变；赛菲利亚弩要求恒定 {@value #FIXED_DAMAGE}。</li>
 *   <li><b>穿透</b>：命中生物后不结束飞行，继续打下一个——只在撞到方块时才停。为避免穿透时
 *       每 tick 重复结算同一只生物，这里记下已命中的目标 id。</li>
 * </ul>
 *
 * <p>{@link SephiriaDamage.Source} 是给伤害来源判定用的：命中时凭它认出这发伤害属于模组，
 * 从而让目标（玩家与敌对生物）无视无敌帧。
 */
public class SephiriaBoltArrow extends Arrow implements SephiriaDamage.Source {
	/** 固定伤害：不随飞行速度变化。 */
	public static final float FIXED_DAMAGE = 1.82F;

	/** 已经命中过的目标 id：穿透时同一目标只结算一次。 */
	private final Set<Integer> hitTargets = new HashSet<>();

	public SephiriaBoltArrow(Level level, LivingEntity shooter, ItemStack weapon) {
		super(level, shooter, new ItemStack(ModItems.CROSSBOW_BOLT), weapon);
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		if (!(this.level() instanceof ServerLevel level) || !(result.getEntity() instanceof LivingEntity target)) {
			return;
		}

		if (!this.hitTargets.add(target.getId())) {
			return;
		}

		Entity owner = this.getOwner();
		// 固定伤害指"不随飞行速度变化"，仍按物理强度换算（默认 20 时倍率 1.0，即 1.82）
		float damage = owner instanceof ServerPlayer shooter
				? (float) (FIXED_DAMAGE * PlayerStats.damageMultiplier(shooter))
				: FIXED_DAMAGE;

		target.hurtServer(level, level.damageSources().arrow(this, owner), damage);
	}
}
