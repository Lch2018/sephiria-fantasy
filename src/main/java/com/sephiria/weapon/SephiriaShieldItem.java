package com.sephiria.weapon;

import com.sephiria.Sephiria;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标准剑盾：主手是剑、副手渲染成盾（见物品定义的 display_context 分流）。
 *
 * <p><b>右键·防御</b>：按住右键进入防御，松开结束。防御期间受到<b>最终伤害减免 50%</b>
 * （与长棍回击后那段用的是同一套减伤机制，见 {@link SephiriaDamage#damageMultiplier}）。
 * 这里没有沿用原版盾牌的 {@code BlocksAttacks} 组件——那是 90° 定向的满额减伤，
 * 与"防御状态下 50%"不是一回事。
 *
 * <p><b>防御的蓝耗</b>（2026-10-21）：防御期间每挨一下扣 {@value #DEFEND_MP_COST} 点蓝，
 * 刚举盾的 {@value #PERFECT_GUARD_TICKS} tick 内挨打算「完美防御」、只扣 {@value #PERFECT_DEFEND_MP_COST} 点，
 * 并让下一次横扫免费；蓝不够时这一下防不住（不减伤）。横扫本身 {@value #SWEEP_MP_COST} 点。
 * 这些数字刻意<b>不写进武器描述</b>（用户口径）。
 *
 * <p><b>防御中攻击·横扫</b>：按住右键时点左键，不再打普通攻击，而是以自身为原点横扫一圈：
 * 伤害 = 物理强度 × {@value #SWEEP_DAMAGE_RATIO}（默认物理强度下就是 10），
 * 冷却 {@value #SWEEP_COOLDOWN_TICKS} tick（1 秒），半径同匕首的招架（330，即水平 3.3 格）。
 * 松手后的普通攻击仍是原来那套原版范围的横扫（见 {@link WeaponSweep}）。
 */
public class SephiriaShieldItem extends Item implements SephiriaWeapon {
	/** 防御时的最终伤害减免。 */
	private static final float DEFEND_REDUCTION = 0.5F;

	/** 攻速目标值（供物品注册使用）。 */
	public static final float ATTACK_SPEED = 3.2F;
	/** 伤害目标值（供物品注册使用）。 */
	public static final float ATTACK_DAMAGE = 3.0F;

	/** 防御中攻击的横扫参数。 */
	private static final double SWEEP_DAMAGE_RATIO = 0.5D;
	private static final int SWEEP_COOLDOWN_TICKS = 20;
	private static final double SWEEP_RANGE = 3.3D;
	private static final double SWEEP_HEIGHT = 0.825D;
	/** 攻击充能下限：与普通攻击同一套判据（原版横扫之刃也是 0.9）。 */
	private static final float SWEEP_CHARGE_THRESHOLD = 0.9F;

	/**
	 * 防御挨打时的蓝耗：普通 10、完美防御 5（半价）。
	 *
	 * <p>「完美防御」= 刚举盾的 {@value #PERFECT_GUARD_TICKS} tick（0.3 秒）内挨到攻击——
	 * 判定窗口和长棍的完美防御一样长，只是这里按「举盾时刻」算。完美防御还会让<b>下一次横扫免费</b>。
	 */
	private static final double DEFEND_MP_COST = 10.0D;
	private static final double PERFECT_DEFEND_MP_COST = 5.0D;
	private static final int PERFECT_GUARD_TICKS = 6;

	/** 防御中横扫的蓝耗（完美防御后的一次免费，见 {@link #FREE_SWEEP}）。 */
	private static final double SWEEP_MP_COST = 15.0D;

	/** 完美防御窗口的到期刻。 */
	private static final Map<UUID, Long> PERFECT_GUARD_UNTIL = new ConcurrentHashMap<>();
	/** 完美防御奖励的「下一次横扫免费」标记；只存一个，连着完美防御不会叠加。 */
	private static final Set<UUID> FREE_SWEEP = ConcurrentHashMap.newKeySet();

	private final WeaponBranch branch;

	public SephiriaShieldItem(WeaponBranch branch, Item.Properties properties) {
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
				Line.attack(ATTACK_DAMAGE, ATTACK_SPEED, WeaponSweep.RANGE).build(),
				Line.titled("tooltip.sephiria_fantasy.skill.defend")
						.stat("tooltip.sephiria_fantasy.part.reduction", DEFEND_REDUCTION * 100.0F, COLOR_RANGE).build(),
				desc("tooltip.sephiria_fantasy.skill.defend", "tooltip.sephiria_fantasy.desc.defend"),
				Line.titled("tooltip.sephiria_fantasy.skill.shield_sweep")
						.skillDamage(PlayerStats.DEFAULT_STRENGTH * SWEEP_DAMAGE_RATIO)
						.cooldownTicks(SWEEP_COOLDOWN_TICKS).range(SWEEP_RANGE).build(),
				desc("tooltip.sephiria_fantasy.skill.shield_sweep", "tooltip.sephiria_fantasy.desc.shield_sweep"));
	}

	/** 某玩家此刻是否在用它防御（右键按住期间，原版的使用状态就是判据）。 */
	public static boolean isDefending(LivingEntity entity) {
		return entity.isUsingItem() && entity.getUseItem().getItem() instanceof SephiriaShieldItem;
	}

	/** 注册"防御中攻击释放横扫"的判定（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		// 完美防御窗口按绝对游戏刻记，到期清掉（与长棍 / 匕首同一套写法）
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long now = server.overworld().getGameTime();
			PERFECT_GUARD_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);
		});

		// 这一条是兜底：正常玩法下防御中的左键会被客户端吞掉，走不到攻击回调（见 tryDefendSweep）。
		// 留着是为了覆盖非常规来源的攻击包——冷却会挡住重复触发，不会打出两次。
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			ItemStack stack = player.getItemInHand(hand);

			if (!(stack.getItem() instanceof SephiriaShieldItem)
					|| !(player instanceof ServerPlayer serverPlayer)
					|| !isDefending(serverPlayer)) {
				return InteractionResult.PASS;
			}

			tryDefendSweep(serverPlayer);
			return InteractionResult.PASS;
		});
	}

	/**
	 * 伤害入口在结算减伤之前调这里：扣这一下防御的蓝，并返回本下该用的防御倍率。
	 *
	 * <p>蓝不够时防御失败、返回 {@code 1.0}（这一下全额吃伤害）——不然蓝就永远花不完，
	 * 防御也就没有代价了。真实伤害 / 元素伤害那几路本来就不走减伤，不会调到这里。
	 *
	 * <p>完美防御（刚举盾的 0.3 秒内挨打）按半价扣蓝，并把下一次横扫标成免费。
	 */
	public static float onIncomingDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayer player) || !isDefending(player)) {
			return 1.0F;
		}

		// 0 伤害的「命中」（雪球那类）不该扣蓝
		if (amount <= 0.0F) {
			return DEFEND_REDUCTION;
		}

		// 创造模式 / 旁观者不扣蓝，防御照旧生效（与各种减伤对创造模式的口径一致）；
		// 冲刺 / 招架那类无敌窗口里的伤害本来就会被整个取消，也不该为它付蓝
		if (player.isCreative() || player.isSpectator()
				|| com.sephiria.ability.Invulnerability.isActive(player)) {
			return DEFEND_REDUCTION;
		}

		long now = player.level().getGameTime();
		boolean perfect = isPerfectGuard(player, now);

		if (!PlayerStats.spendMp(player, perfect ? PERFECT_DEFEND_MP_COST : DEFEND_MP_COST)) {
			return 1.0F;
		}

		if (perfect) {
			// 一次完美防御只发一次奖励：把窗口清掉，接着挨的那几下按普通防御算
			PERFECT_GUARD_UNTIL.remove(player.getUUID());
			FREE_SWEEP.add(player.getUUID());
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.45F);
		}

		return DEFEND_REDUCTION;
	}

	/** 是否处在「刚举盾」的完美防御窗口里。 */
	private static boolean isPerfectGuard(ServerPlayer player, long now) {
		Long until = PERFECT_GUARD_UNTIL.get(player.getUUID());
		return until != null && now < until;
	}

	/**
	 * 防御中按左键 → 横扫。由客户端按键包（{@link com.sephiria.network.ShieldSweepPayload}）驱动。
	 *
	 * <p>冷却是 1 秒；攻击充能没满或不在防御状态就直接不作数（客户端可能连点，服务端每次都要重新判定）。
	 */
	public static void tryDefendSweep(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level) || !isDefending(player)) {
			return;
		}

		ItemStack stack = player.getUsedItemHand() == InteractionHand.MAIN_HAND
				? player.getMainHandItem()
				: player.getOffhandItem();

		if (!(stack.getItem() instanceof SephiriaShieldItem)
				|| player.getCooldowns().isOnCooldown(stack)
				|| player.getAttackStrengthScale(0.5F) <= SWEEP_CHARGE_THRESHOLD) {
			return;
		}

		// 完美防御奖励的那一次免费；否则 15 点，不够就放不出来（冷却与充能都还在）
		boolean free = FREE_SWEEP.remove(player.getUUID());

		if (!free && !PlayerStats.spendMp(player, SWEEP_MP_COST)) {
			return;
		}

		sweep(player, level, stack);
		// 和普通攻击一样把充能清零：不然充能这一条判定等于没写
		player.resetAttackStrengthTicker();
	}

	private static void sweep(ServerPlayer player, ServerLevel level, ItemStack stack) {
		double mul = PlayerStats.rangeMultiplier(player);
		float damage = (float) (PlayerStats.skillDamageMultiplier(player) * 20.0D * SWEEP_DAMAGE_RATIO);
		AABB area = player.getBoundingBox().inflate(SWEEP_RANGE * mul, SWEEP_HEIGHT * mul, SWEEP_RANGE * mul);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
			// 突进的范围大：只打有威胁的生物，和平生物不误伤（见 SephiriaDamage.strikeable）
			if (!SephiriaDamage.strikeable(victim, player)) {
				continue;
			}

			// 告诉伤害入口「这是技能伤害」：无视防御伤害要按技能的倍率放大（见 SephiriaDamage.applyTrueDamage）
			SephiriaDamage.markSkill(player, PlayerStats.skillDamageMultiplier(player));
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

		// 举盾那一刻开一个 0.3 秒的完美防御窗口：这段时间内挨打按半价扣蓝 + 下一次横扫免费
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			PERFECT_GUARD_UNTIL.put(serverPlayer.getUUID(),
					level.getGameTime() + PERFECT_GUARD_TICKS);
		}

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
