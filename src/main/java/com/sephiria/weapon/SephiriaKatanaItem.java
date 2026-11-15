package com.sephiria.weapon;

import com.sephiria.Sephiria;
import com.sephiria.ability.Dash;
import com.sephiria.ability.Invulnerability;
import com.sephiria.ability.SkillStorage;
import com.sephiria.client.SkillClientData;
import com.sephiria.compat.ItemData;
import com.sephiria.compat.WeaponAttributes;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import com.sephiria.stats.WeaponStats;
import com.google.common.collect.Multimap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 赛菲莉娅的「刀」：带刀鞘的居合刀（1.20.1 完整版）。
 *
 * <p>两种状态右键切换，两个状态都会打出横扫：出鞘轻快但伤害低；入鞘伤害高、横扫更强，
 * 但每刀耗 5 点 MP。命中攒剑意（0..100），入鞘且满层时左键放「强力斩」：
 * 消耗全部剑意突进并造成范围伤害。
 */
public class SephiriaKatanaItem extends Item implements GeoItem, SephiriaWeapon {
	public static final int SWITCH_TICKS = 13;
	public static final int BLOCK_TICKS = 6;

	private static final float PLAYER_BASE_ATTACK_DAMAGE = 1.0F;
	private static final double PLAYER_BASE_ATTACK_SPEED = 4.0D;
	private static final float VANILLA_SWORD_ATTACK_SPEED = 1.6F;
	private static final float UNSHEATHED_DAMAGE = 2.52F;
	private static final float UNSHEATHED_CHARGE_SCALE = 2.6F;
	private static final double UNSHEATHED_SWEEP_RANGE = 2.0D;
	private static final double UNSHEATHED_SWEEP_RATIO = 0.5D;
	private static final float SHEATHED_DAMAGE = 12.0F;
	private static final float SHEATHED_CHARGE_SCALE = 0.7F;
	private static final double SHEATHED_SWEEP_RANGE = 2.0D;
	private static final double SHEATHED_SWEEP_RATIO = 1.0D;
	private static final double VANILLA_SWEEP_RANGE = 1.0D;
	private static final double VANILLA_SWEEP_HEIGHT = 0.25D;
	private static final float SWEEP_CHARGE_THRESHOLD = 0.9F;

	public static final ResourceLocation SWORD_INTENT =
			new ResourceLocation(Sephiria.MOD_ID, "sword_intent");
	public static final double INTENT_MAX = 100.0D;
	private static final double INTENT_PER_HIT = 12.0D;
	private static final int INTENT_GAIN_TICKS = 6;
	private static final double INTENT_SWITCH_BONUS = 50.0D;
	/**
	 * 强力斩击：向准星突进 {@value #POWERFUL_DISTANCE} 格，落地后以自身为原点劈一圈，
	 * 同时伤害整条突进路径上的敌人。
	 *
	 * <p>位移距离取狂怒的 80%（10.4 × 0.8 = 8.32）；伤害半径 +30%（5.5 → 7.15）。
	 * 倍率 = 120% × (消耗的剑意 ÷ {@value #INTENT_MAX})，满层就是 24——
	 * 也就是物理强度基准（20）的 120%，再乘物理强度换算。
	 */
	private static final double POWERFUL_DISTANCE = 10.4D * 0.8D;
	private static final int POWERFUL_DASH_TICKS = 4;
	private static final int POWERFUL_COOLDOWN = 4;
	private static final double POWERFUL_RANGE = 5.5D * 1.3D;
	private static final double POWERFUL_HEIGHT = 1.375D * 1.3D;
	private static final float POWERFUL_DAMAGE_AT_FULL = 24.0F;
	private static final double SHEATHED_ATTACK_MP_COST = 5.0D;
	private static final float INTENT_FULL_VOLUME = 0.9F;
	private static final float INTENT_FULL_PITCH = 1.5F;

	private static final List<Pending> PENDING = new ArrayList<>();
	private static final Map<UUID, Long> INTENT_GAIN_UNTIL = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> SWITCH_GUARD_UNTIL = new ConcurrentHashMap<>();
	private static final Set<UUID> PENDING_INTENT = ConcurrentHashMap.newKeySet();

	public static final String SHEATHED = "katana_sheathed";
	private static final ResourceLocation DAMAGE_MODIFIER_ID =
			new ResourceLocation(Sephiria.MOD_ID, "katana_attack_damage");
	private static final ResourceLocation SPEED_MODIFIER_ID =
			new ResourceLocation(Sephiria.MOD_ID, "katana_attack_speed");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final WeaponBranch branch;

	public SephiriaKatanaItem(WeaponBranch branch, Item.Properties properties) {
		super(properties);
		this.branch = branch;
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND
				? WeaponAttributes.of(UNSHEATHED_DAMAGE, VANILLA_SWORD_ATTACK_SPEED * UNSHEATHED_CHARGE_SCALE)
				: super.getDefaultAttributeModifiers(slot);
	}

	/** 某个状态下的攻击力/攻速（供 ItemStack 属性 mixin 使用）。 */
	public static Multimap<Attribute, AttributeModifier> stateAttributes(boolean sheathed) {
		float damage = (float) baseAttackDamage(sheathed);
		float speed = (float) baseAttackSpeed(sheathed);
		return WeaponAttributes.of(damage, speed);
	}

	@Override
	public WeaponBranch branch() {
		return this.branch;
	}

	/**
	 * 提示框里的详细说明：两种状态的攻击数据 + 切换 / 剑意 / 强力斩三个技能。
	 * 数值全部取自本类顶部的常量，改数值不用再动这里。
	 */
	@Override
	public java.util.List<Component> detailLines(ItemStack stack) {
		return java.util.List.of(
				skillLine("tooltip.sephiria_fantasy.tip.attack",
						tag("damage", TAG_PHYSICAL), hint("gain_intent")),
				skillLine("tooltip.sephiria_fantasy.skill.toggle",
						tag("immunity", TAG_YELLOW), tag("parry", TAG_YELLOW), hint("katana_toggle")),
				skillLine("tooltip.sephiria_fantasy.skill.slash",
						tag("damage", TAG_PHYSICAL), tag("displacement", TAG_CYAN),
						tag("consume", TAG_BLUE), hint("katana_slash")));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		double max = SkillClientData.max(SWORD_INTENT);
		return max > 0.0D && SkillClientData.current(SWORD_INTENT) >= max;
	}

	public static boolean isSheathed(ItemStack stack) {
		return ItemData.getBoolean(stack, SHEATHED, false);
	}

	/** 当前形态的基础攻速（只用于耐久公式与属性注册，不含任何属性加成）。 */
	public static double baseAttackSpeed(boolean sheathed) {
		return VANILLA_SWORD_ATTACK_SPEED * (sheathed ? SHEATHED_CHARGE_SCALE : UNSHEATHED_CHARGE_SCALE);
	}

	/** 当前形态的基础攻击力（同上）。 */
	public static double baseAttackDamage(boolean sheathed) {
		return sheathed ? SHEATHED_DAMAGE : UNSHEATHED_DAMAGE;
	}

	private static int switchInvulnTicks(ServerPlayer player) {
		double percent = com.sephiria.artifact.ArtifactEffects.katanaSwitchInvulnPercent(player);
		return Math.max(1, (int) Math.round(BLOCK_TICKS * (1.0D + percent / 100.0D)));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.getCooldowns().isOnCooldown(stack.getItem())) {
			return InteractionResultHolder.fail(stack);
		}

		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			boolean sheathed = !isSheathed(stack);
			ItemData.setBoolean(stack, SHEATHED, sheathed);
			WeaponStats.refresh(serverPlayer);
			serverPlayer.getCooldowns().addCooldown(stack.getItem(), SWITCH_TICKS);
			int blockTicks = switchInvulnTicks(serverPlayer);
			Invulnerability.grant(serverPlayer, blockTicks);
			SWITCH_GUARD_UNTIL.put(serverPlayer.getUUID(),
					serverPlayer.level().getGameTime() + blockTicks);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0F, sheathed ? 0.8F : 1.2F);
			// 动画不在这里播：切换动画由每个客户端按自己看到的视角触发（第一人称动作版 /
			// 第三人称静止版，见 syncClientHand）。服务端只保证这把刀有一个动画实例 id。
			GeoItem.getOrAssignId(stack, (ServerLevel) level);
		}

		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (entity instanceof ServerPlayer && level instanceof ServerLevel serverLevel) {
			GeoItem.getOrAssignId(stack, serverLevel);
		}
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.NONE;
	}

	public static void register() {
		SkillStorage.registerManual(SWORD_INTENT, INTENT_MAX, 0.0D);
		ServerTickEvents.END_SERVER_TICK.register(SephiriaKatanaItem::tick);

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && isSwitchGuarding(player)) {
				SWITCH_GUARD_UNTIL.remove(player.getUUID());
				PENDING_INTENT.add(player.getUUID());
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.5F);
				return false;
			}

			return true;
		});
	}

	private static boolean isSwitchGuarding(ServerPlayer player) {
		Long until = SWITCH_GUARD_UNTIL.get(player.getUUID());
		return until != null && player.level().getGameTime() < until;
	}

	private static void playFullSoundIfFilled(ServerPlayer player, double before) {
		if (before < INTENT_MAX && SkillStorage.current(player, SWORD_INTENT) >= INTENT_MAX) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, INTENT_FULL_VOLUME, INTENT_FULL_PITCH);
		}
	}

	public static void registerEvents() {
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			ItemStack stack = player.getItemInHand(hand);

			if (!(stack.getItem() instanceof SephiriaKatanaItem)) {
				return InteractionResult.PASS;
			}

			if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
				return InteractionResult.PASS;
			}

			if (isSheathed(stack)) {
				((SephiriaKatanaItem) stack.getItem()).triggerAnim(serverPlayer,
						GeoItem.getOrAssignId(stack, serverLevel), "main", "attack_sheathed");
			}

			// 入鞘且满层：正常情况下左键点击已被客户端按键包拦下（可以空放），
			// 只有没装本模组客户端的攻击包会走到这里；两条路都靠层数与冷却互斥，不会重复放。
			double intent = SkillStorage.current(serverPlayer, SWORD_INTENT);

			if (isSheathed(stack) && intent >= INTENT_MAX) {
				SkillStorage.consume(serverPlayer, SWORD_INTENT, intent);
				powerfulSlash(serverPlayer, serverLevel, stack, intent);
				return InteractionResult.FAIL;
			}

			// 普通命中按原版扣 1 点耐久（强力斩击那一刀在上面已按 3x 扣过并返回）
			WeaponDurability.hurt(stack, serverPlayer, hand, WeaponDurability.NORMAL_ATTACK_COST);

			if (isSheathed(stack) && !PlayerStats.spendMp(serverPlayer, SHEATHED_ATTACK_MP_COST)) {
				bareHandedAttack(serverPlayer, serverLevel, entity);
				return InteractionResult.FAIL;
			}

			long now = serverLevel.getGameTime();
			Long gainUntil = INTENT_GAIN_UNTIL.get(serverPlayer.getUUID());

			if (gainUntil == null || now >= gainUntil) {
				INTENT_GAIN_UNTIL.put(serverPlayer.getUUID(), now + INTENT_GAIN_TICKS);
				SkillStorage.regenerate(serverPlayer, SWORD_INTENT, INTENT_PER_HIT);
				playFullSoundIfFilled(serverPlayer, intent);
			}

			if (canSweep(serverPlayer)) {
				sweep(serverPlayer, serverLevel, stack, entity);
			}

			return InteractionResult.PASS;
		});
	}

	private static void bareHandedAttack(ServerPlayer player, ServerLevel level, Entity target) {
		player.resetAttackStrengthTicker();

		if (!(target instanceof LivingEntity victim)) {
			return;
		}

		float damage = (float) (PLAYER_BASE_ATTACK_DAMAGE * PlayerStats.normalAttackMultiplier(player));
		DamageSource source = level.damageSources().playerAttack(player);

		if (victim.hurt(source, damage)) {
			SephiriaDamage.knockback(victim, player, 0.4D, source);
		}
	}

	private static boolean canSweep(ServerPlayer player) {
		return player.getAttackStrengthScale(0.5F) > SWEEP_CHARGE_THRESHOLD
				&& !player.isSprinting()
				&& player.onGround();
	}

	private static void sweep(ServerPlayer player, ServerLevel level, ItemStack stack, Entity target) {
		boolean sheathed = isSheathed(stack);
		double ratio = sheathed ? SHEATHED_SWEEP_RATIO : UNSHEATHED_SWEEP_RATIO;
		double rangeScale = sheathed ? SHEATHED_SWEEP_RANGE : UNSHEATHED_SWEEP_RANGE;
		float mainDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float sweepDamage = 1.0F + (float) (ratio * mainDamage);
		double rangeMul = PlayerStats.rangeMultiplier(player);
		AABB area = target.getBoundingBox().inflate(
				VANILLA_SWEEP_RANGE * rangeScale * rangeMul,
				VANILLA_SWEEP_HEIGHT * rangeScale * rangeMul,
				VANILLA_SWEEP_RANGE * rangeScale * rangeMul);
		DamageSource source = level.damageSources().playerAttack(player);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (victim == player || victim == target || victim.isAlliedTo(player)) {
				continue;
			}

			if (victim instanceof ArmorStand stand && stand.isMarker()) {
				continue;
			}

			if (victim.hurt(source, sweepDamage)) {
				SephiriaDamage.knockback(victim, player, 0.4D, source);
			}
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);

		double dx = -Mth.sin(player.getYRot() * ((float) Math.PI / 180.0F));
		double dz = Mth.cos(player.getYRot() * ((float) Math.PI / 180.0F));
		level.sendParticles(ParticleTypes.SWEEP_ATTACK,
				player.getX() + dx, player.getY(0.5D), player.getZ() + dz,
				0, dx, 0.0D, dz, 0.0D);
	}

	/**
	 * 客户端满层左键的请求入口：与狂怒一样可以空放，服务端重新验一遍手里是不是入鞘的满层刀。
	 *
	 * <p>层数和冷却都在服务端判定，客户端镜像只负责决定要不要抢这次点击；
	 * 抢下后不会再发原版攻击包，所以不会和命中路径重复触发。
	 */
	public static void tryPowerfulSlash(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		ItemStack stack = player.getMainHandItem();

		if (!(stack.getItem() instanceof SephiriaKatanaItem) || !isSheathed(stack)
				|| player.getCooldowns().isOnCooldown(stack.getItem())) {
			return;
		}

		double intent = SkillStorage.current(player, SWORD_INTENT);

		if (intent < INTENT_MAX) {
			return;
		}

		SkillStorage.consume(player, SWORD_INTENT, intent);
		powerfulSlash(player, level, stack, intent);
	}

	private static void powerfulSlash(ServerPlayer player, ServerLevel level, ItemStack stack, double intent) {
		float damage = (float) (POWERFUL_DAMAGE_AT_FULL * (intent / INTENT_MAX)
				* PlayerStats.skillDamageMultiplier(player));

		// 强力斩击 3x 耐久（入鞘状态的基础值，见 WeaponDurability）
		WeaponDurability.hurt(stack, player, InteractionHand.MAIN_HAND,
				WeaponDurability.SKILL_COST);

		// 突进走 3D 版本：准星朝上/朝下时也要能斜着窜出去
		Vec3 direction = player.getLookAngle();
		Dash.startVertical(player, direction, POWERFUL_DISTANCE, POWERFUL_DASH_TICKS, Dash.DEFAULT_DECAY);
		player.getCooldowns().addCooldown(stack.getItem(), POWERFUL_COOLDOWN);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4F, 0.6F);
		PENDING.add(Pending.burst(player, direction, POWERFUL_DISTANCE, POWERFUL_DASH_TICKS,
				damage, POWERFUL_RANGE, POWERFUL_HEIGHT));
	}

	private static void tick(MinecraftServer server) {
		if (!PENDING_INTENT.isEmpty()) {
			for (UUID id : new ArrayList<>(PENDING_INTENT)) {
				PENDING_INTENT.remove(id);
				ServerPlayer player = server.getPlayerList().getPlayer(id);

				if (player != null) {
					double before = SkillStorage.current(player, SWORD_INTENT);
					SkillStorage.regenerate(player, SWORD_INTENT, INTENT_SWITCH_BONUS);
					playFullSoundIfFilled(player, before);
				}
			}
		}

		long now = server.overworld().getGameTime();
		SWITCH_GUARD_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);
		INTENT_GAIN_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);

		if (PENDING.isEmpty()) {
			return;
		}

		for (Pending pending : new ArrayList<>(PENDING)) {
			if (pending.dueTick > now) {
				continue;
			}

			PENDING.remove(pending);
			pending.resolve();
		}
	}

	/** 一次待结算的斩击。落点范围在结算时按自身位置拼，路径范围用释放时定格的线段。 */
	private static final class Pending {
		private final ServerPlayer player;
		private final long dueTick;
		private final float damage;
		private final double range;
		private final double height;
		private final Vec3 start;
		private final Vec3 end;

		private Pending(ServerPlayer player, long dueTick, float damage, double range, double height,
				Vec3 start, Vec3 end) {
			this.player = player;
			this.dueTick = dueTick;
			this.damage = damage;
			this.range = range;
			this.height = height;
			this.start = start;
			this.end = end;
		}

		static Pending burst(ServerPlayer player, Vec3 direction, double distance, int delay,
				float damage, double range, double height) {
			Vec3 start = player.position();
			return new Pending(player, player.level().getGameTime() + delay, damage, range, height,
					start, start.add(direction.scale(distance)));
		}

		void resolve() {
			if (player.isRemoved() || !(player.level() instanceof ServerLevel level)) {
				return;
			}

			double mul = PlayerStats.rangeMultiplier(player) * PlayerStats.skillRangeMultiplier(player);
			// 落点一圈 + 整条突进路径：路径用释放时定格的线段，位移被挡住也不影响判定
			AABB landing = player.getBoundingBox().inflate(range * mul, height * mul, range * mul);
			AABB path = new AABB(this.start, this.end)
					.inflate(range * mul, height * mul, range * mul);
			Set<LivingEntity> victims = new HashSet<>(level.getEntitiesOfClass(LivingEntity.class, landing));
			victims.addAll(level.getEntitiesOfClass(LivingEntity.class, path));
			DamageSource source = level.damageSources().playerAttack(player);

			for (LivingEntity victim : victims) {
				if (!SephiriaDamage.strikeable(victim, player)) {
					continue;
				}

				SephiriaDamage.markSkill(player, PlayerStats.skillDamageMultiplier(player));
				victim.hurt(source, damage);
			}

			// TNT 爆炸观感：按实际伤害盒铺粒子，只放粒子、不放声音
			WeaponExplosionFx.box(level, path);
			WeaponExplosionFx.box(level, landing);

			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4F, 0.8F);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK,
					player.getX(), player.getY(0.5D), player.getZ(), 12, 1.6D, 0.3D, 1.6D, 0.0D);
		}
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "main", 0,
				state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.idle_unsheathed")))
				.triggerableAnim("to_sheathed", RawAnimation.begin()
						.thenPlay("animation.switch_to_sheathed").thenLoop("animation.idle_sheathed"))
				.triggerableAnim("to_unsheathed", RawAnimation.begin()
						.thenPlay("animation.switch_to_unsheathed").thenLoop("animation.idle_unsheathed"))
				// 第三人称用的「静止版」过渡：播 *_third（同一姿态、没有动作）再进待机
				.triggerableAnim("to_sheathed_third", RawAnimation.begin()
						.thenPlay("animation.switch_to_sheathed_third").thenLoop("animation.idle_sheathed"))
				.triggerableAnim("to_unsheathed_third", RawAnimation.begin()
						.thenPlay("animation.switch_to_unsheathed_third").thenLoop("animation.idle_unsheathed"))
				// 首次看到这把刀时只把待机对上（进服时手里拿着一把入鞘的刀）
				.triggerableAnim("sync_sheathed", RawAnimation.begin().thenLoop("animation.idle_sheathed"))
				.triggerableAnim("sync_unsheathed", RawAnimation.begin().thenLoop("animation.idle_unsheathed"))
				.triggerableAnim("attack_sheathed", RawAnimation.begin()
						.thenPlay("animation.idle_sheathed2").thenLoop("animation.idle_sheathed")));
	}

	// ------------------------------------------------------------------ 客户端动画同步

	/** 客户端：每把刀「上一次看到的入鞘状态」（键是 GeckoLib 实例 id）。 */
	private static final Map<Long, Boolean> CLIENT_SHEATHED = new ConcurrentHashMap<>();

	/**
	 * 客户端：每个「玩家的某只手」上一次对过账的物品栈，**只用来比引用**。
	 *
	 * <p>键 = 实体 id × 2 +（副手 1 / 主手 0）。服务端同步物品栏 / 装备时会换成一个新的
	 * {@code ItemStack} 对象，所以「引用没变」就等于「这把刀没被动过」——每 tick 只花一次
	 * {@code get} 比较，真正要读 NBT、查实例 id 的对账只在物品栈换对象那一下做。
	 * 只在客户端 tick 线程访问，用普通 {@code HashMap} 即可。
	 */
	private static final Map<Integer, ItemStack> CLIENT_HAND_STACK = new java.util.HashMap<>();

	/** 客户端：自己的刀在第一人称触发了「带动作的过渡」后，允许在这个游戏刻之前被掐掉。 */
	private static final Map<Long, Long> CLIENT_ANIMATED_UNTIL = new java.util.HashMap<>();

	/**
	 * 客户端 tick 调用（见 {@code SephiriaClient}）：这只手的物品栈换了对象才去对账。
	 *
	 * @param firstPerson 这一侧看到的视角：自己的刀看相机，别人手里的刀永远是第三人称
	 */
	public static void syncClientHand(Player player, InteractionHand hand, boolean firstPerson) {
		ItemStack stack = player.getItemInHand(hand);
		int key = player.getId() * 2 + (hand == InteractionHand.OFF_HAND ? 1 : 0);

		if (CLIENT_HAND_STACK.get(key) == stack) {
			return;
		}

		CLIENT_HAND_STACK.put(key, stack);
		syncClientAnimation(player, stack, firstPerson);
	}

	/**
	 * 第三人称下调用：把还在播的「带动作过渡」掐成待机。
	 *
	 * <p>第一人称右键后立刻按 F5，过渡动画还会在第三人称继续播——动画状态按实例 id 记、
	 * 两个视角共用一条，切视角本身不会重新触发。这里补一刀：如果上一条是带动作的过渡、
	 * 且还在 0.65 秒（{@link #SWITCH_TICKS}）内，就直接切到对应的静止待机。
	 */
	public static void cancelAnimatedSwitch(Player player, ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof SephiriaKatanaItem item)) {
			return;
		}

		long id = GeoItem.getId(stack);
		Long until = CLIENT_ANIMATED_UNTIL.remove(id);

		if (until != null && player.level().getGameTime() <= until) {
			triggerClientAnimation(item, id, isSheathed(stack) ? "sync_sheathed" : "sync_unsheathed");
		}
	}

	/**
	 * 客户端：看这把刀的入鞘状态变没变，变了就按 {@code firstPerson} 触发对应动画。
	 *
	 * <p>服务端只改 NBT，动画由每个客户端自己触发——「自己第一人称看到的」播带动作的
	 * {@code to_*}，「第三人称（自己的和别人的）」播静止版 {@code to_*_third}。
	 * 第一次看到这把刀时只把待机对上（{@code sync_*}，不播过渡）。
	 */
	private static void syncClientAnimation(Player player, ItemStack stack, boolean firstPerson) {
		if (stack.isEmpty() || !(stack.getItem() instanceof SephiriaKatanaItem item)) {
			return;
		}

		long id = GeoItem.getId(stack);
		boolean sheathed = isSheathed(stack);
		Boolean last = CLIENT_SHEATHED.put(id, sheathed);

		if (last == null) {
			CLIENT_ANIMATED_UNTIL.remove(id);
			triggerClientAnimation(item, id, sheathed ? "sync_sheathed" : "sync_unsheathed");
			return;
		}

		if (last == sheathed) {
			return;
		}

		String animation = sheathed
				? (firstPerson ? "to_sheathed" : "to_sheathed_third")
				: (firstPerson ? "to_unsheathed" : "to_unsheathed_third");

		if (firstPerson) {
			// 带动作的那版：记下窗口，切到第三人称时好在它播完前掐掉
			CLIENT_ANIMATED_UNTIL.put(id, player.level().getGameTime() + SWITCH_TICKS);
		} else {
			CLIENT_ANIMATED_UNTIL.remove(id);
		}

		triggerClientAnimation(item, id, animation);
	}

	private static void triggerClientAnimation(SephiriaKatanaItem item, long id, String animation) {
		item.getAnimatableInstanceCache().getManagerForId(id).tryTriggerAnimation("main", animation);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	private RenderProvider renderProvider;
	private java.util.function.Supplier<Object> renderProviderSupplier;

	@Override
	public void createRenderer(java.util.function.Consumer<Object> consumer) {
		consumer.accept(this.getRenderProvider().get());
	}

	@Override
	public java.util.function.Supplier<Object> getRenderProvider() {
		if (this.renderProviderSupplier == null) {
			// 手持渲染每帧都会走 KatanaItemRendererMixin → getRenderProvider()，
			// 所以 lambda 与渲染器都只建一次；动画实例缓存本来就挂在物品上。
			this.renderProviderSupplier = () -> {
				if (this.renderProvider == null) {
					this.renderProvider = new RenderProvider() {
						private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;

						@Override
						public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
							if (this.renderer == null) {
								this.renderer = new com.sephiria.client.render.KatanaRenderer();
							}

							return this.renderer;
						}
					};
				}

				return this.renderProvider;
			};
		}

		return this.renderProviderSupplier;
	}
}
