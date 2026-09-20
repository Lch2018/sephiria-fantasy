package com.sephiria.weapon;

import com.sephiria.Sephiria;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 标准剑盾：主手是剑、副手渲染成盾（见物品定义的 display_context 分流）。
 *
 * <p><b>右键·防御</b>：按住右键进入防御，松开结束。防御期间受到<b>最终伤害减免 50%</b>
 * （与长棍回击后那段用的是同一套减伤机制，见 {@link SephiriaDamage#damageMultiplier}）。
 * 这里没有沿用原版盾牌的 {@code BlocksAttacks} 组件——那是 90° 定向的满额减伤，
 * 与"防御状态下 50%"不是一回事。
 *
 * <p><b>防御中攻击·横扫</b>：按住右键时点左键，不再打普通攻击，而是以自身为原点横扫一圈：
 * 伤害 = 物理强度 × {@value #SWEEP_DAMAGE_RATIO}（默认物理强度下就是 10），
 * 冷却 {@value #SWEEP_COOLDOWN_TICKS} tick（1 秒），半径同匕首的招架（330，即水平 3.3 格）。
 * 松手后的普通攻击仍是原来那套原版范围的横扫（见 {@link WeaponSweep}）。
 */
public class SephiriaShieldItem extends Item implements SephiriaWeapon {
	/** 防御时的最终伤害减免。 */
	private static final float DEFEND_REDUCTION = 0.5F;

	/** 防御中攻击的横扫参数。 */
	private static final double SWEEP_DAMAGE_RATIO = 0.5D;
	private static final int SWEEP_COOLDOWN_TICKS = 20;
	private static final double SWEEP_RANGE = 3.3D;
	private static final double SWEEP_HEIGHT = 0.825D;

	private final WeaponBranch branch;

	public SephiriaShieldItem(WeaponBranch branch, Item.Properties properties) {
		super(properties);
		this.branch = branch;
	}

	@Override
	public WeaponBranch branch() {
		return this.branch;
	}

	/** 某玩家此刻是否在用它防御（右键按住期间，原版的使用状态就是判据）。 */
	public static boolean isDefending(LivingEntity entity) {
		return entity.isUsingItem() && entity.getUseItem().getItem() instanceof SephiriaShieldItem;
	}

	/** 注册"防御中攻击释放横扫"的判定（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			ItemStack stack = player.getItemInHand(hand);

			if (!(stack.getItem() instanceof SephiriaShieldItem)
					|| !(player instanceof ServerPlayer serverPlayer)
					|| !(level instanceof ServerLevel serverLevel)
					|| !isDefending(serverPlayer)) {
				return InteractionResult.PASS;
			}

			// 冷却中或该次攻击还没充能完，就当普通攻击处理
			if (serverPlayer.getCooldowns().isOnCooldown(stack)
					|| serverPlayer.getAttackStrengthScale(0.5F) <= 0.9F) {
				return InteractionResult.PASS;
			}

			sweep(serverPlayer, serverLevel, stack);
			return InteractionResult.PASS;
		});
	}

	private static void sweep(ServerPlayer player, ServerLevel level, ItemStack stack) {
		double mul = PlayerStats.rangeMultiplier(player);
		float damage = (float) (PlayerStats.damageMultiplier(player) * 20.0D * SWEEP_DAMAGE_RATIO);
		AABB area = player.getBoundingBox().inflate(SWEEP_RANGE * mul, SWEEP_HEIGHT * mul, SWEEP_RANGE * mul);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (victim == player || victim.isAlliedTo(player)) {
				continue;
			}

			victim.hurtServer(level, level.damageSources().playerAttack(player), damage);
		}

		player.getCooldowns().addCooldown(stack, SWEEP_COOLDOWN_TICKS);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.9F);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK,
				player.getX(), player.getY(0.5D), player.getZ(), 6, 1.0D, 0.2D, 1.0D, 0.0D);
	}

	/** 右键：开始防御（原版 BLOCK 姿势），松开由原版自动结束。 */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BLOCK;
	}

	/** 防御期间的伤害倍率（供减伤判定读取）。 */
	public static float damageMultiplier(LivingEntity entity) {
		return isDefending(entity) ? DEFEND_REDUCTION : 1.0F;
	}
}
