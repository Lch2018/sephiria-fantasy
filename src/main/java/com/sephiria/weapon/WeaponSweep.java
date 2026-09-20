package com.sephiria.weapon;

import com.sephiria.ability.SkillStorage;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

/**
 * 标准剑盾与匕首的普通攻击横扫（按表它们的范围是 ×1，也就是原版横扫的范围）。
 *
 * <p>另外三把（刀 / 大剑 / 棍）在自己的物品类里各有一段参数不同的横扫，这里只服务
 * "原版范围"这一档，所以单列一个共用实现，免得在两处再抄一遍相同的逻辑。
 * 与其它横扫一致：充能 ≥90%、未疾跑、站在地面、命中目标才触发；扫描盒以命中目标为中心，
 * 并按<b>近战攻击范围</b>属性放大。
 */
public final class WeaponSweep {
	/** 原版横扫的基准：以命中目标为中心 inflate(1.0, 0.25, 1.0)，离玩家不超过 3 格。 */
	public static final double RANGE = 1.0D;
	private static final double HEIGHT = 0.25D;
	private static final double MAX_DISTANCE = 3.0D;
	/** 对其它目标的伤害系数。 */
	private static final double RATIO = 0.5D;
	private static final float CHARGE_THRESHOLD = 0.9F;

	private WeaponSweep() {
	}

	public static void register() {
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			var stack = player.getItemInHand(hand);
			// 剑盾与匕首是 ×1 那一档（见类注释）。这里原本判的是 SephiriaWeaponItem，
			// 但已经没有武器继承它了，于是剑盾的普通攻击一直没打出过横扫。
			boolean sweeps = stack.getItem() instanceof SephiriaShieldItem
					|| stack.getItem() instanceof SephiriaDaggerItem;

			if (!sweeps || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
				return InteractionResult.PASS;
			}

			// 盾剑防御中的攻击由它自己处理（释放 330 的横扫），这里让开
			if (SephiriaShieldItem.isDefending(serverPlayer)) {
				return InteractionResult.PASS;
			}

			if (serverPlayer.getAttackStrengthScale(0.5F) <= CHARGE_THRESHOLD
					|| serverPlayer.isSprinting() || !serverPlayer.onGround()) {
				return InteractionResult.PASS;
			}

			sweep(serverPlayer, serverLevel, entity);
			return InteractionResult.PASS;
		});
	}

	private static void sweep(ServerPlayer player, ServerLevel level, Entity target) {
		double mul = PlayerStats.rangeMultiplier(player);
		float mainDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float sweepDamage = 1.0F + (float) (RATIO * mainDamage);
		AABB area = target.getBoundingBox().inflate(RANGE * mul, HEIGHT * mul, RANGE * mul);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (victim == player || victim == target || victim.isAlliedTo(player)) {
				continue;
			}

			if (victim.hurtServer(level, level.damageSources().playerAttack(player), sweepDamage)) {
				victim.knockback(0.4D, player.getX() - victim.getX(), player.getZ() - victim.getZ(),
						level.damageSources().playerAttack(player), sweepDamage);
			}
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);

		double dx = -Math.sin(player.getYRot() * ((float) Math.PI / 180.0F));
		double dz = Math.cos(player.getYRot() * ((float) Math.PI / 180.0F));
		level.sendParticles(ParticleTypes.SWEEP_ATTACK,
				player.getX() + dx, player.getY(0.5D), player.getZ() + dz, 0, dx, 0.0D, dz, 0.0D);
	}
}
