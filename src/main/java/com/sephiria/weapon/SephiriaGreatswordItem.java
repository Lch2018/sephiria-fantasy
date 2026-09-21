package com.sephiria.weapon;

import com.sephiria.Sephiria;
import com.sephiria.ability.Dash;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 钢铁巨剑：慢速重击 + 右键蓄力的「旋风」。
 *
 * <p><b>左键</b>：和刀一样会打出横扫，但范围按入鞘状态下的刀缩到 {@value #SWEEP_RANGE_SCALE} 倍
 * （{@value #VANILLA_SWEEP_RANGE} × 2 × {@value #SWEEP_RANGE_SCALE}），攻速则是入鞘刀的
 * {@value #ATTACK_SPEED_SCALE} 倍——伤害沿用物品自身的攻击力，不改。
 *
 * <p><b>右键·旋风</b>：按住蓄力，满 {@value #CHARGE_TICKS} tick（1.5 秒）才算满蓄；
 * 满蓄后松开才会释放：先沿准星方向（含垂直分量）突进 {@value #DASH_DISTANCE} 格、
 * 耗时 {@value #DASH_TICKS} tick（0.2 秒），落地后立刻以<b>自身为原点</b>发动一次环形斩击。
 * 伤害 = 入鞘刀的 {@value #DAMAGE_SCALE} 倍；半径取玩家的<b>实体交互距离</b>
 * （原版是 3 格，也就是"手能打到多远，这个技能就能打到多远"）。
 * 这个技能没有无敌、也不涉及专注或存储，只有伤害。
 */
public class SephiriaGreatswordItem extends Item implements SephiriaWeapon {
	/** 入鞘状态下的刀的基准值——换算全部以它为参照。 */
	private static final float KATANA_SHEATHED_DAMAGE = 12.0F;
	private static final float KATANA_SHEATHED_SPEED = 1.6F * 0.7F;
	private static final double KATANA_SHEATHED_SWEEP_RANGE = 2.0D;

	/** 左键横扫：范围是入鞘刀的 80%，攻速是它的 140%。 */
	private static final double SWEEP_RANGE_SCALE = 1.5D;
	private static final double ATTACK_SPEED_SCALE = 1.4D;
	/** 攻速目标值（供物品注册使用）。 */
	public static final float ATTACK_SPEED = 1.6F;

	/** 伤害目标值（供物品注册使用）。 */
	public static final float ATTACK_DAMAGE = 6.0F;

	private static final double KATANA_SHEATHED_SWEEP_HEIGHT = 0.5D;
	/** 横扫对其它目标的伤害系数；右键技能的伤害倍数。 */
	private static final double SWEEP_RATIO = 0.5D;
	private static final double DAMAGE_SCALE = 1.75D;
	/** 旋风伤害 = 入鞘刀伤害 × 这个倍数。 */
	public static final float WHIRLWIND_DAMAGE = (float) (KATANA_SHEATHED_DAMAGE * DAMAGE_SCALE);

	/** 蓄力满所需 tick（1.5 秒）；蓄力上限就是它，按住更久不会更强。 */
	public static final int CHARGE_TICKS = 30;
	/** 释放时的位移与耗时。 */
	private static final double DASH_DISTANCE = 5.0D;
	private static final int DASH_TICKS = 4;
	/** 环形斩击的垂直半径（水平半径取玩家的实体交互距离）。 */
	private static final double RING_RADIUS = 4.0D;
	private static final double RING_HEIGHT = 1.5D;
	/** 蓄力期间每隔这么多 tick 响一声蓄力音。 */
	private static final int CHARGE_SOUND_INTERVAL = 6;
	/** 横扫触发的充能门槛，与原版一致。 */
	private static final float SWEEP_CHARGE_THRESHOLD = 0.9F;

	/** 待结算的环形斩击。 */
	private static final List<Pending> PENDING = new ArrayList<>();

	private final WeaponBranch branch;

	public SephiriaGreatswordItem(WeaponBranch branch, Item.Properties properties) {
		super(properties);
		this.branch = branch;
	}

	@Override
	public WeaponBranch branch() {
		return this.branch;
	}

	@Override
	public java.util.List<Component> detailLines(ItemStack stack) {
		return java.util.List.of(
				Line.attack(ATTACK_DAMAGE, ATTACK_SPEED, KATANA_SHEATHED_SWEEP_RANGE * SWEEP_RANGE_SCALE).build(),
				Line.titled("tooltip.sephiria.skill.whirlwind")
						.damage(WHIRLWIND_DAMAGE)
						.stat("tooltip.sephiria.part.charge", CHARGE_TICKS / 20.0D, COLOR_COOLDOWN)
						.range(RING_RADIUS).distance(DASH_DISTANCE).build(),
				desc("tooltip.sephiria.skill.whirlwind", "tooltip.sephiria.desc.whirlwind"));
	}


	/** 注册横扫判定与环形斩击的推进器（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(SephiriaGreatswordItem::tick);

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			ItemStack stack = player.getItemInHand(hand);

			if (!(stack.getItem() instanceof SephiriaGreatswordItem)) {
				return InteractionResult.PASS;
			}

			if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
				return InteractionResult.PASS;
			}

			if (serverPlayer.getAttackStrengthScale(0.5F) <= SWEEP_CHARGE_THRESHOLD
					|| serverPlayer.isSprinting() || !serverPlayer.onGround()) {
				return InteractionResult.PASS;
			}

			sweep(serverPlayer, serverLevel, stack, entity);
			return InteractionResult.PASS;
		});
	}

	/** 左键横扫：以命中目标为中心扫一圈，参数按巨剑的比例。 */
	private static void sweep(ServerPlayer player, ServerLevel level, ItemStack stack, Entity target) {
		double rangeMul = PlayerStats.rangeMultiplier(player);
		double range = KATANA_SHEATHED_SWEEP_RANGE * SWEEP_RANGE_SCALE * rangeMul;
		double height = KATANA_SHEATHED_SWEEP_HEIGHT * SWEEP_RANGE_SCALE * rangeMul;
		float mainDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float sweepDamage = 1.0F + (float) (SWEEP_RATIO * mainDamage);
		AABB area = target.getBoundingBox().inflate(range, height, range);

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

	// ------------------------------------------------------------------ 右键：旋风

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
		return ItemUseAnimation.SPEAR;
	}

	/**
	 * 蓄力期间的音效（只在服务端放，所有玩家都能听到；客户端也会调这个方法，不拦会重复播放）：
	 * 每 {@value #CHARGE_SOUND_INTERVAL} tick 一声蓄力音、音高随蓄力升高，满蓄那一刻换成一声确认音。
	 */
	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
		if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
			return;
		}

		int charge = chargeTicks(stack, entity, remaining);

		if (charge == CHARGE_TICKS) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 1.4F);
			return;
		}

		if (charge <= 0 || charge > CHARGE_TICKS || charge % CHARGE_SOUND_INTERVAL != 0) {
			return;
		}

		float pitch = 0.8F + 0.5F * (charge / (float) CHARGE_TICKS);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.CROSSBOW_LOADING_MIDDLE, SoundSource.PLAYERS, 0.8F, pitch);
	}

	/** 松开右键：只有蓄满才释放旋风；返回是否真的放出去了（原版用它决定要不要播使用成功的表现）。 */
	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
			return false;
		}

		if (chargeTicks(stack, entity, timeLeft) < CHARGE_TICKS) {
			return false;
		}

		Vec3 direction = player.getLookAngle();
		Dash.startVertical(player, direction, DASH_DISTANCE, DASH_TICKS, Dash.DEFAULT_DECAY);
		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.7F);
		PENDING.add(Pending.ring(player, DASH_TICKS, (float) (WHIRLWIND_DAMAGE * PlayerStats.skillDamageMultiplier(player)), RING_HEIGHT));
		return true;
	}

	/**
	 * 已经蓄了多少 tick。
	 *
	 * <p>客户端的 HUD 也用它画进度条：用物品的使用时长（{@code getUseDuration}）减去剩余刻数，
	 * 两端都是客户端已知的量，所以进度条不需要任何同步包。
	 */
	public static int chargeTicks(ItemStack stack, LivingEntity entity, int timeLeft) {
		return Math.max(0, stack.getUseDuration(entity) - timeLeft);
	}

	private static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}

		long now = server.overworld().getGameTime();
		// 用快照遍历：resolve() 里可能往 PENDING 追加新的一段（比如回击的第二段），
		// 直接在原列表上迭代会抛 ConcurrentModificationException。
		for (Pending pending : new ArrayList<>(PENDING)) {
			if (pending.dueTick > now) {
				continue;
			}

			PENDING.remove(pending);
			pending.resolve();
		}
	}

	/**
	 * 一次待结算的环形斩击。
	 *
	 * <p>判定区域在<b>结算那一刻</b>才按自身位置生成，而不是释放瞬间——斩击要跟着位移走，
	 * 打的是落点而不是起跳点（区域跟着玩家，位移完成后自然以落点为圆心）。
	 */
	private static final class Pending {
		private final ServerPlayer player;
		private final long dueTick;
		private final float damage;
		private final double height;

		private Pending(ServerPlayer player, long dueTick, float damage, double height) {
			this.player = player;
			this.dueTick = dueTick;
			this.damage = damage;
			this.height = height;
		}

		static Pending ring(ServerPlayer player, int delay, float damage, double height) {
			return new Pending(player, player.level().getGameTime() + delay, damage, height);
		}

		void resolve() {
			if (player.isRemoved() || !(player.level() instanceof ServerLevel level)) {
				return;
			}

			// 水平半径是固定值（4 格），再乘近战攻击范围属性
			double reach = RING_RADIUS * PlayerStats.rangeMultiplier(player);
			AABB area = player.getBoundingBox().inflate(reach, height, reach);

			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
				if (victim == player || victim.isAlliedTo(player)) {
					continue;
				}

				victim.hurtServer(level, level.damageSources().playerAttack(player), damage);
			}

			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.9F);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK,
					player.getX(), player.getY(0.5D), player.getZ(), 8, 1.2D, 0.2D, 1.2D, 0.0D);
		}
	}
}
