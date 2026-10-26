package com.sephiria.mixin;

import com.sephiria.damage.SephiriaDamage;
import com.sephiria.difficulty.RootRetreat;
import com.sephiria.talent.Talents;
import com.sephiria.weapon.SephiriaShieldItem;
import com.sephiria.weapon.SephiriaStaffItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 服务端伤害入口上的三件事：暴击、最终减伤、无视防御伤害。
 *
 * <p>原版没有"受到的伤害乘算"这种属性，而 Fabric 的 {@code ALLOW_DAMAGE} 只能整体取消、
 * 不能改数值，所以这里直接在 {@code hurtServer} 上改 amount。倍率由
 * {@link SephiriaStaffItem#damageMultiplier} 提供——非减免期间恒为 1.0，所以减伤对其它情况没有影响。
 *
 * <p>三件事必须分开在两个注入点做：
 * <ul>
 *   <li>暴击与减伤在 <b>HEAD</b>，都是"改这一下的数值"；</li>
 *   <li>无视防御伤害在 <b>TAIL</b>：它是额外打的一次真实伤害，如果在 HEAD 打，就会先把目标的
 *       无敌帧顶起来，紧接着原来那一下反而被无敌帧吞掉。</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public class DamageReductionMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float sephiria$adjustDamage(float amount, ServerLevel level, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		SephiriaDamage.Kind kind = SephiriaDamage.kindOf(source);
		float result;

		// 真实伤害绕开减伤（这是它存在的意义），但和别的伤害一样吃暴击与黄金之手（2026-10 起）
		if (kind == SephiriaDamage.Kind.TRUE) {
			result = SephiriaDamage.applyGoldenHands(source, SephiriaDamage.applyCrit(self, source, amount));
		} else if (kind == SephiriaDamage.Kind.ELECTRIC || kind == SephiriaDamage.Kind.FIRE
				|| kind == SephiriaDamage.Kind.ICE || kind == SephiriaDamage.Kind.PLASMA) {
			// 电属性伤害、火/冰/等离子体减益伤害同样不吃减伤与黄金之手，但照掷暴击：
			// 电属性与等离子体是「通用 + 电属性攻击的暴击几率（麒麟的角）」，火/冰减益只用通用那一项
			result = SephiriaDamage.applyCrit(self, source, amount);
		} else {
			float critted = SephiriaDamage.applyCrit(self, source, amount);
			// 长棍的「回击后减免」是纯倍率；盾剑的防御要在这里先扣蓝（扣不起就防不住），
			// 所以不能走 SephiriaDamage.damageMultiplier 那条不带伤害来源的纯查询
			float multiplier = SephiriaStaffItem.damageMultiplier(self)
					* SephiriaShieldItem.onIncomingDamage(self, source, amount);
			// 黄金之手是攻击方的增益，接在减伤之后统一放大
			result = SephiriaDamage.applyGoldenHands(source,
					multiplier >= 1.0F ? critted : critted * multiplier);
		}

		// 根之退（难度选项，见 difficulty.RootRetreat）——两件事都作用在伤害的最后一步：
		// ① 凶猛的爪子：敌人打到自己身上的伤害放大（只作用于被打的人自己的设置，
		//    玩家打的与环境伤害都不算，所以绝大多数伤害这里是 ×1）；
		// ② 顽强之躯 / 防护铠甲：自己打敌人的伤害被削（前者是「最底层的减免」、真实伤害也吃，
		//    后者是「敌人的防御力」、真实伤害不吃）
		float adjusted = result * RootRetreat.incomingDamageMultiplier(self, source)
				* RootRetreat.outgoingDamageMultiplier(self, source, kind == SephiriaDamage.Kind.TRUE);

		// 天赋「愤怒」二阶段：对 BOSS 造成的伤害 +10%（所有由玩家打出的伤害都吃，含减益与真实伤害）；
		// 天赋「急智」三阶段：每种激活的连击类型再放大 7%
		ServerPlayer talentAttacker = Talents.attackerOf(source);

		if (talentAttacker != null) {
			adjusted *= Talents.bossDamageMultiplier(talentAttacker, self);
			adjusted *= Talents.comboDamageMultiplier(talentAttacker);
		}

		return adjusted;
	}

	/**
	 * 玩家「防御力」的减伤：{@code x / (x + 80)}（防御力 80 → 50%），接在护甲结算之后。
	 *
	 * <p>只作用于**吃护甲**的伤害：{@code getDamageAfterArmorAbsorb} 开头对 {@code BYPASSES_ARMOR}
	 * 的伤害会原样返回，那个 RETURN 也在注入范围内，所以显式排掉它——坠落、火焰、真实伤害这类
	 * 绕过护甲的伤害不吃防御力，和以前「防御力并进护甲值」时的语义一致。
	 * 吃攻击方的防御穿透（愤怒三阶段）。
	 */
	@Inject(method = "getDamageAfterArmorAbsorb", at = @At("RETURN"), cancellable = true)
	private void sephiria$defenseReduction(DamageSource source, float amount, CallbackInfoReturnable<Float> callback) {
		if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
			return;
		}

		float multiplier = defenseMultiplier((LivingEntity) (Object) this, source);

		if (multiplier < 1.0F) {
			callback.setReturnValue(callback.getReturnValueF() * multiplier);
		}
	}

	/**
	 * 玩家「防御力」的减伤倍率：{@code 1 - x/(x+80)}（防御力 80 → 50%）。
	 *
	 * <p>吃攻击方的防御穿透：愤怒三阶段的「无视目标 20% 防御力」＝把 x 乘 0.8 再折算。
	 * 只对玩家目标生效——敌人的防御力走根之退那条（{@code RootRetreat.outgoingDamageMultiplier}），
	 * 用的是同一个 {@link com.sephiria.stats.PlayerStats#defenseReduction(double)} 公式。
	 */
	private static float defenseMultiplier(LivingEntity victim, DamageSource source) {
		if (!(victim instanceof ServerPlayer player)) {
			return 1.0F;
		}

		double defense = com.sephiria.stats.PlayerStats.defenseTotal(player);
		ServerPlayer attacker = Talents.attackerOf(source);

		if (attacker != null) {
			defense *= 1.0D - Talents.defensePenetrationPercent(attacker) / 100.0D;
		}

		return (float) (1.0D - com.sephiria.stats.PlayerStats.defenseReduction(defense));
	}

	/**
	 * 天赋「愤怒」三阶段的防御穿透：无视目标 20% 的**护甲值**。
	 *
	 * <p>原版在 {@code getDamageAfterArmorAbsorb} 里把 {@code getArmorValue()} 的结果直接交给
	 * 护甲减伤公式，所以这里只改<b>这一次结算</b>读到的护甲值——装备本身、护甲耐久都不变。
	 * 护甲被削到 0 以下没有意义，夹在 0。玩家自己的「防御力」不再并进这里（它有自己的
	 * x/(x+80) 减伤，穿透在 {@link #defenseMultiplier} 里扣）。
	 */
	@Redirect(method = "getDamageAfterArmorAbsorb",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getArmorValue()I"))
	private int sephiria$talentArmorPenetration(LivingEntity instance, DamageSource source) {
		int armor = instance.getArmorValue();

		ServerPlayer attacker = Talents.attackerOf(source);

		if (attacker == null) {
			return armor;
		}

		double penetration = Talents.defensePenetrationPercent(attacker);

		if (penetration <= 0.0D) {
			return armor;
		}

		return Math.max(0, (int) Math.round(armor * (1.0D - penetration / 100.0D)));
	}

	@Inject(method = "hurtServer", at = @At("TAIL"))
	private void sephiria$applyTrueDamage(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callback) {
		// 只有这一下真的打中了才补真伤：被无敌帧挡掉、被取消的都不算
		if (callback.getReturnValueZ()) {
			SephiriaDamage.applyTrueDamage((LivingEntity) (Object) this, source, amount);
		}
	}
}
