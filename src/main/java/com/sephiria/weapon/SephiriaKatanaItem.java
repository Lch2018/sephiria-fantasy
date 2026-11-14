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
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
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
	private static final double POWERFUL_DISTANCE = 1.0D;
	private static final int POWERFUL_DASH_TICKS = 4;
	private static final int POWERFUL_COOLDOWN = 4;
	private static final double POWERFUL_RANGE = 5.5D;
	private static final double POWERFUL_HEIGHT = 1.375D;
	private static final float POWERFUL_DAMAGE_AT_FULL = 24.0F;
	private static final double SHEATHED_ATTACK_MP_COST = 5.0D;
	private static final int INTENT_BAR_COLOR = 0xFF6FD8FF;
	private static final int INTENT_BAR_FULL_COLOR = 0xFFFFD24A;
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
		float damage = sheathed ? SHEATHED_DAMAGE : UNSHEATHED_DAMAGE;
		float speed = VANILLA_SWORD_ATTACK_SPEED * (sheathed ? SHEATHED_CHARGE_SCALE : UNSHEATHED_CHARGE_SCALE);
		return WeaponAttributes.of(damage, speed);
	}

	@Override
	public WeaponBranch branch() {
		return this.branch;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return intentRatio() > 0.0F;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0F * intentRatio());
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return intentRatio() >= 1.0F ? INTENT_BAR_FULL_COLOR : INTENT_BAR_COLOR;
	}

	private static float intentRatio() {
		double max = SkillClientData.max(SWORD_INTENT);
		return max <= 0.0D ? 0.0F : (float) (SkillClientData.current(SWORD_INTENT) / max);
	}

	/**
	 * 提示框里的详细说明：两种状态的攻击数据 + 切换 / 剑意 / 强力斩三个技能。
	 * 数值全部取自本类顶部的常量，改数值不用再动这里。
	 */
	@Override
	public java.util.List<Component> detailLines(ItemStack stack) {
		double sheathedSpeed = VANILLA_SWORD_ATTACK_SPEED * SHEATHED_CHARGE_SCALE;
		double drawnSpeed = VANILLA_SWORD_ATTACK_SPEED * UNSHEATHED_CHARGE_SCALE;
		double sheathedRange = VANILLA_SWEEP_RANGE * SHEATHED_SWEEP_RANGE;
		double drawnRange = VANILLA_SWEEP_RANGE * UNSHEATHED_SWEEP_RANGE;

		return java.util.List.of(
				// 出鞘是默认状态，所以放在前面，和数据表一致
				Line.attack("tooltip.sephiria_fantasy.katana.drawn", UNSHEATHED_DAMAGE, drawnSpeed, drawnRange).build(),
				Line.attack("tooltip.sephiria_fantasy.katana.sheathed", SHEATHED_DAMAGE, sheathedSpeed, sheathedRange).build(),
				Line.titled("tooltip.sephiria_fantasy.skill.toggle")
						.cooldownTicks(SWITCH_TICKS)
						.stat("tooltip.sephiria_fantasy.part.invuln", BLOCK_TICKS / 20.0D, COLOR_RANGE).build(),
				desc("tooltip.sephiria_fantasy.skill.toggle", "tooltip.sephiria_fantasy.desc.toggle"),
				Line.titled("tooltip.sephiria_fantasy.skill.intent")
						.stat("tooltip.sephiria_fantasy.part.per_hit", INTENT_PER_HIT, COLOR_DAMAGE)
						.stat("tooltip.sephiria_fantasy.part.max", INTENT_MAX, COLOR_RANGE)
						.stat("tooltip.sephiria_fantasy.part.block_bonus", INTENT_SWITCH_BONUS, COLOR_DAMAGE).build(),
				desc("tooltip.sephiria_fantasy.skill.intent", "tooltip.sephiria_fantasy.desc.intent"),
				Line.titled("tooltip.sephiria_fantasy.skill.slash")
						.skillDamage(POWERFUL_DAMAGE_AT_FULL).cooldownTicks(POWERFUL_COOLDOWN)
						.range(POWERFUL_RANGE).distance(POWERFUL_DISTANCE).build(),
				desc("tooltip.sephiria_fantasy.skill.slash", "tooltip.sephiria_fantasy.desc.slash"));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		double max = SkillClientData.max(SWORD_INTENT);
		return max > 0.0D && SkillClientData.current(SWORD_INTENT) >= max;
	}

	public static boolean isSheathed(ItemStack stack) {
		return ItemData.getBoolean(stack, SHEATHED, false);
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
			this.triggerAnim(serverPlayer, GeoItem.getOrAssignId(stack, (ServerLevel) level),
					"main", sheathed ? "to_sheathed" : "to_unsheathed");
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

			double intent = SkillStorage.current(serverPlayer, SWORD_INTENT);

			if (isSheathed(stack) && intent >= INTENT_MAX) {
				SkillStorage.consume(serverPlayer, SWORD_INTENT, intent);
				powerfulSlash(serverPlayer, serverLevel, stack, intent);
				return InteractionResult.FAIL;
			}

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

	private static void powerfulSlash(ServerPlayer player, ServerLevel level, ItemStack stack, double intent) {
		float damage = (float) (POWERFUL_DAMAGE_AT_FULL * (intent / INTENT_MAX)
				* PlayerStats.skillDamageMultiplier(player));

		Dash.startVertical(player, player.getLookAngle(), POWERFUL_DISTANCE, POWERFUL_DASH_TICKS, Dash.DEFAULT_DECAY);
		player.getCooldowns().addCooldown(stack.getItem(), POWERFUL_COOLDOWN);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4F, 0.6F);
		PENDING.add(Pending.burst(player, POWERFUL_DASH_TICKS, damage, POWERFUL_RANGE, POWERFUL_HEIGHT));
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

	private static final class Pending {
		private final ServerPlayer player;
		private final long dueTick;
		private final float damage;
		private final double range;
		private final double height;

		private Pending(ServerPlayer player, long dueTick, float damage, double range, double height) {
			this.player = player;
			this.dueTick = dueTick;
			this.damage = damage;
			this.range = range;
			this.height = height;
		}

		static Pending burst(ServerPlayer player, int delay, float damage, double range, double height) {
			return new Pending(player, player.level().getGameTime() + delay, damage, range, height);
		}

		void resolve() {
			if (player.isRemoved() || !(player.level() instanceof ServerLevel level)) {
				return;
			}

			double mul = PlayerStats.rangeMultiplier(player) * PlayerStats.skillRangeMultiplier(player);
			AABB area = player.getBoundingBox().inflate(range * mul, height * mul, range * mul);
			DamageSource source = level.damageSources().playerAttack(player);

			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
				if (!SephiriaDamage.strikeable(victim, player)) {
					continue;
				}

				SephiriaDamage.markSkill(player, PlayerStats.skillDamageMultiplier(player));
				victim.hurt(source, damage);
			}

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
				.triggerableAnim("attack_sheathed", RawAnimation.begin()
						.thenPlay("animation.idle_sheathed2").thenLoop("animation.idle_sheathed")));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	private RenderProvider renderProvider;

	@Override
	public void createRenderer(java.util.function.Consumer<Object> consumer) {
		consumer.accept(this.getRenderProvider().get());
	}

	@Override
	public java.util.function.Supplier<Object> getRenderProvider() {
		return () -> {
			if (this.renderProvider == null) {
				this.renderProvider = new RenderProvider() {
					@Override
					public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
						return new com.sephiria.client.render.KatanaRenderer();
					}
				};
			}

			return this.renderProvider;
		};
	}
}
