package com.sephiria.mixin.client;

import com.sephiria.damage.SephiriaDamage;
import com.sephiria.difficulty.RootRetreat;
import com.sephiria.talent.Talents;
import com.sephiria.weapon.SephiriaShieldItem;
import com.sephiria.weapon.SephiriaStaffItem;
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
 * 服务端伤害入口上的三件事：暴击、最终减伤、无视防御伤害（1.20.1 版）。
 *
 * <p>1.20.1 的伤害入口是 {@code LivingEntity#hurt(DamageSource, float)}（不是 26.x 的
 * {@code hurtServer}），所以描述符相应调整；逻辑与 26.1.2 的 {@code DamageReductionMixin}
 * 一致。护盾吸收挂在 {@code getDamageAfterArmorAbsorb} 入口，保证只有真正结算的伤害才扣盾。
 */
@Mixin(LivingEntity.class)
public class DamageReductionMixin {
	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float sephiria$adjustDamage(float amount, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		SephiriaDamage.Kind kind = SephiriaDamage.kindOf(source);
		float result;

		if (kind == SephiriaDamage.Kind.TRUE) {
			result = SephiriaDamage.applyGoldenHands(source, SephiriaDamage.applyCrit(self, source, amount));
		} else if (kind == SephiriaDamage.Kind.ELECTRIC || kind == SephiriaDamage.Kind.FIRE
				|| kind == SephiriaDamage.Kind.ICE || kind == SephiriaDamage.Kind.BLUE_BURN
				|| kind == SephiriaDamage.Kind.PLASMA) {
			result = SephiriaDamage.applyCrit(self, source, amount);
		} else {
			float critted = SephiriaDamage.applyCrit(self, source, amount);
			float multiplier = SephiriaStaffItem.damageMultiplier(self)
					* SephiriaShieldItem.onIncomingDamage(self, source, amount);
			result = SephiriaDamage.applyGoldenHands(source, multiplier >= 1.0F ? critted : critted * multiplier);
		}

		float adjusted = result * RootRetreat.incomingDamageMultiplier(self, source)
				* RootRetreat.outgoingDamageMultiplier(self, source, kind == SephiriaDamage.Kind.TRUE);

		ServerPlayer talentAttacker = Talents.attackerOf(source);

		if (talentAttacker != null) {
			adjusted *= Talents.bossDamageMultiplier(talentAttacker, self);
			adjusted *= Talents.comboDamageMultiplier(talentAttacker);
			adjusted *= com.sephiria.stats.PlayerStats.allDamageMultiplier(talentAttacker);
		}

		return adjusted;
	}

	/** 护盾吸收：只有真正结算的伤害才扣盾（被闪避 / 无敌帧挡掉的不扣）。 */
	@ModifyVariable(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), argsOnly = true)
	private float sephiria$absorbShield(float amount, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (self instanceof ServerPlayer player) {
			return (float) com.sephiria.stats.ShieldSystem.absorb(player, amount);
		}

		return amount;
	}

	/** 玩家「防御力」的减伤：x / (x + 80)，接在护甲结算之后。 */
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

	private static float defenseMultiplier(LivingEntity victim, DamageSource source) {
		if (!(victim instanceof ServerPlayer player)) {
			return 1.0F;
		}

		double defense = com.sephiria.stats.PlayerStats.defenseTotal(player);
		ServerPlayer attacker = Talents.attackerOf(source);

		if (attacker != null) {
			defense *= remainingDefenseMultiplier(attacker);
		}

		return (float) (1.0D - com.sephiria.stats.PlayerStats.defenseReduction(defense));
	}

	private static double remainingDefenseMultiplier(ServerPlayer attacker) {
		double penetration = Talents.defensePenetrationPercent(attacker) / 100.0D;
		double ignore = com.sephiria.artifact.ArtifactEffects.defenseIgnorePercent(attacker) / 100.0D;
		return (1.0D - penetration) * (1.0D - ignore);
	}

	/** 攻击方的防御穿透 / 无视防御：只削这一次结算读到的护甲值。 */
	@Redirect(method = "getDamageAfterArmorAbsorb",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getArmorValue()I"))
	private int sephiria$armorPenetration(LivingEntity instance, DamageSource source) {
		int armor = instance.getArmorValue();
		ServerPlayer attacker = Talents.attackerOf(source);

		if (attacker == null) {
			return armor;
		}

		double remaining = remainingDefenseMultiplier(attacker);

		if (remaining >= 1.0D) {
			return armor;
		}

		return Math.max(0, (int) Math.round(armor * remaining));
	}

	@Inject(method = "hurt", at = @At("TAIL"))
	private void sephiria$applyTrueDamage(DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callback) {
		if (callback.getReturnValueZ()) {
			SephiriaDamage.applyTrueDamage((LivingEntity) (Object) this, source, amount);
		}
	}
}
