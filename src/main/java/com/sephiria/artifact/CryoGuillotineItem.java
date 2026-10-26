package com.sephiria.artifact;

import com.sephiria.Sephiria;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.registry.ModParticleTypes;
import com.sephiria.stats.CombatState;
import com.sephiria.stats.PlayerStats;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 酷寒断头台（冰霜武具 + 魔法科技，羁绊品质）：【唯一】每 6 秒向周围最多 4 名敌人召唤
 * 冰 + 雷电属性的巨型铁剑；对敌人造成触电伤害时，冷却缩短 1 秒。
 *
 * <p><b>双连击</b>神器：同时给冰霜武具与魔法科技各加 1 级（见 {@link #combos()}）。
 *
 * <p>伤害 = (闪电与冰里较高的一方 + 较低的一方 × 30%) × 75/100/175%（0..2 级）。
 * 走 {@link SephiriaDamage#cryoGuillotine}（{@code Kind.CRYO}）：冰 + 电双属性，照常触发
 * 触电 / 冻伤（不触发火焰之触）。
 *
 * <p>视觉效果参考红蛇之眼：一把放大的原版铁剑从目标正上方落下，落地时砸出冰蓝粒子；
 * 本体是 {@code ItemDisplay}，不需要客户端代码。
 */
public class CryoGuillotineItem extends ArtifactItem {
	/** 各等级那一下的伤害倍率（%）。 */
	private static final int[] DAMAGE_PERCENT_BY_LEVEL = { 75, 100, 175 };
	/** 自动索敌半径（格）。 */
	private static final double RADIUS = 10.0D;
	/** 每次最多打几个目标。 */
	private static final int MAX_TARGETS = 4;
	/** 冷却（tick）：6 秒。 */
	private static final int COOLDOWN_TICKS = 120;
	/** 每次触电伤害把冷却缩短多少 tick：1 秒。 */
	private static final int SHOCK_COOLDOWN_RELIEF = 20;
	/** 铁剑从多高处落下（格）。 */
	private static final double FALL_HEIGHT = 8.0D;
	/** 下落阶段（tick）：原 10 tick 提速 30%（10 / 1.3 ≈ 7.7，取 8）。 */
	private static final int FALL_TICKS = 8;
	/** 落地后停留（tick），够看清砸下去那一下。 */
	private static final int LINGER_TICKS = 8;
	/** 铁剑放大倍数：原 5.0 的 40%。 */
	private static final float BLADE_SCALE = 2.0F;
	/** 落地爆炸球：半径 2.5 格，冰蓝色尘粒填满球体（2026-10-26 用户要求粒子数量减半）。 */
	private static final double IMPACT_SPHERE_RADIUS = 2.5D;
	private static final int IMPACT_SPHERE_DUST = 70;
	private static final int IMPACT_SPHERE_SNOW = 12;
	private static final int IMPACT_SPHERE_SPARKS = 10;
	/** 冰蓝 #8FDCFF（与冰冻染色 / 冰霜武具同色）。 */
	private static final int ICE_BLUE = 0x8FDCFF;
	/** 焰橙 #FF8A2B（永恒蚀在左半边时「斩」改成火焰属性）。 */
	private static final int FLAME_ORANGE = 0xFF8A2B;
	/** 满亮，免得地下 / 夜里看不清。 */
	private static final int BRIGHTNESS_OVERRIDE = 0xF000F0;
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	/** 冷却结束时间（gameTime）；没带这件神器时删掉。 */
	private static final Map<UUID, Long> READY_AT = new HashMap<>();
	/** 正在演出的刀刃。 */
	private static final List<Blade> BLADES = new ArrayList<>();

	public CryoGuillotineItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.FROST_ARMAMENT;
	}

	/** 双连击：冰霜武具与魔法科技各 +1 级。 */
	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.FROST_ARMAMENT, ArtifactCombo.MAGIC_TECH);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.cryo_guillotine.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DAMAGE_PERCENT_BY_LEVEL.length - 1;
	}

	@Override
	public int cryoGuillotinePercent(int level) {
		return DAMAGE_PERCENT_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, DAMAGE_PERCENT_BY_LEVEL.length - 1)];
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		Component percent = Component.literal(Numbers.format(this.cryoGuillotinePercent(level)))
				.withColor(COLOUR_BONUS);

		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.cryo_guillotine", percent),
				Component.translatable("artifact.sephiria_fantasy.affix.cryo_guillotine_shock"));
	}

	/** 由 {@code Sephiria#onInitialize()} 调用。 */
	public static void registerTicker() {
		ServerTickEvents.END_SERVER_TICK.register(CryoGuillotineItem::tick);
	}

	private static void tick(MinecraftServer server) {
		advanceBlades(server);

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int level = ArtifactEffects.highestLevelOf(player, com.sephiria.registry.ModItems.CRYO_GUILLOTINE);

			if (level < 0) {
				READY_AT.remove(player.getUUID());
				continue;
			}

			long now = player.level().getGameTime();
			Long ready = READY_AT.get(player.getUUID());

			if (ready == null) {
				// 刚装上：先记下时间，等满一轮冷却再落第一轮
				READY_AT.put(player.getUUID(), now + cooldownTicks(player));
				continue;
			}

			if (now < ready || !CombatState.isInCombat(player)) {
				continue;
			}

			if (summon(player, level)) {
				READY_AT.put(player.getUUID(), now + cooldownTicks(player));
			}
		}
	}

	/** 触电伤害把冷却缩短 1 秒（由 {@code Debuffs#activate} 调用）；已经就绪时不动。 */
	public static void onShockDamage(ServerPlayer player) {
		Long ready = READY_AT.get(player.getUUID());

		if (ready == null) {
			return;
		}

		long now = player.level().getGameTime();

		if (now < ready) {
			READY_AT.put(player.getUUID(), Math.max(now, ready - SHOCK_COOLDOWN_RELIEF));
		}
	}

	/** HUD：剩余冷却（tick）；没带时返回 -1。 */
	public static int cooldownLeft(ServerPlayer player) {
		Long ready = READY_AT.get(player.getUUID());

		if (ready == null) {
			return -1;
		}

		return (int) Math.max(0L, ready - player.level().getGameTime());
	}

	public static int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	/**
	 * 实际冷却：基础 120 刻 ÷ (1 + 冰霜武具连击的充能速度 + 冰封的心脏那几件)。
	 * 「斩」现在算冰霜武具的一员，所以这一项要跟剑鞘 / 锤子 / 长矛同一套。
	 */
	public static int cooldownTicks(ServerPlayer player) {
		double percent = ArtifactCombo.FROST_ARMAMENT.frostChargeSpeedPercent(comboLevel(player))
				+ ArtifactEffects.frostChargeSpeedBonus(player);
		return Math.max(1, (int) Math.round(COOLDOWN_TICKS / (1.0D + percent / 100.0D)));
	}

	/** 「斩」的冰霜武具连击等级（它自己也算一件冰霜武具）。 */
	private static int comboLevel(ServerPlayer player) {
		return ArtifactEffects.comboLevels(player).getOrDefault(ArtifactCombo.FROST_ARMAMENT, 0);
	}

	/** 冰霜武具的「额外触发次数」：连击 10 档 + 冰之翼；每多一次就多落一把「斩」。 */
	private static int extraTriggers(ServerPlayer player) {
		return ArtifactCombo.FROST_ARMAMENT.frostExtraTriggers(comboLevel(player))
				+ ArtifactEffects.frostExtraTriggersBonus(player);
	}

	/** 「冰霜武具的伤害」：连击档位与神器（含永恒蚀）的同一项相加。 */
	private static double frostDamageMultiplier(ServerPlayer player) {
		double percent = ArtifactCombo.FROST_ARMAMENT.frostDamagePercent(comboLevel(player))
				+ ArtifactEffects.frostArmamentDamagePercent(player);
		return 1.0D + percent / 100.0D;
	}

	/** 永恒蚀在背包左半边时，「斩」跟冰霜武具其余几件一样改成火焰属性。 */
	private static boolean flameMode(ServerPlayer player) {
		return EternalEclipseItem.sideOf(player) == EternalEclipseItem.Side.LEFT;
	}

	private static boolean summon(ServerPlayer player, int level) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return false;
		}

		boolean flame = flameMode(player);
		double lightning = PlayerStats.elementTotal(player, PlayerStats.Element.LIGHTNING);
		double other = PlayerStats.elementTotal(player, flame ? PlayerStats.Element.FIRE : PlayerStats.Element.ICE);
		double base = Math.max(lightning, other) + Math.min(lightning, other) * 0.30D;
		float damage = (float) (base * ArtifactEffects.cryoGuillotinePercent(player) / 100.0D
				* frostDamageMultiplier(player));

		if (damage <= 0.0F) {
			return false;
		}

		AABB area = player.getBoundingBox().inflate(RADIUS, RADIUS, RADIUS);
		List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, area).stream()
				.filter(victim -> SephiriaDamage.autoTargetable(victim, player))
				.sorted(Comparator.comparingDouble(victim -> victim.distanceToSqr(player)))
				.limit(MAX_TARGETS)
				.toList();

		if (victims.isEmpty()) {
			return false;
		}

		int blades = 1 + extraTriggers(player);

		for (LivingEntity victim : victims) {
			for (int index = 0; index < blades; index++) {
				// 多发时围成一圈稍微错开，免得几把剑完全重叠看不出额外触发
				double angle = Math.PI * 2.0D * index / blades;
				double offset = blades <= 1 ? 0.0D : 0.55D;
				Vec3 impact = victim.position().add(Math.cos(angle) * offset, 0.0D, Math.sin(angle) * offset);
				Vec3 start = impact.add(0.0D, FALL_HEIGHT, 0.0D);
				BLADES.add(new Blade(serverLevel, player.getUUID(), victim, impact, start, damage, flame, 0,
						spawnBlade(serverLevel, start, flame)));
			}
		}

		return true;
	}

	/** 造一把放大的铁剑本体；颜色靠周身冰蓝粒子补，物品本体仍是原版铁剑（用户口径）。 */
	private static Display.ItemDisplay spawnBlade(ServerLevel level, Vec3 at, boolean flame) {
		Display.ItemDisplay body = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);

		if (body == null) {
			return null;
		}

		SynchedEntityData data = body.getEntityData();
		ItemStack bladeStack = new ItemStack(Items.IRON_SWORD);
		// 原版铁剑的模型换成重绘版；永恒蚀在左半边时用焰橙色版
		bladeStack.set(DataComponents.ITEM_MODEL,
				Identifier.fromNamespaceAndPath(Sephiria.MOD_ID,
						flame ? "cryo_guillotine_blade_flame" : "cryo_guillotine_blade"));
		data.set(Display.ItemDisplay.DATA_ITEM_STACK_ID, bladeStack);
		data.set(Display.ItemDisplay.DATA_ITEM_DISPLAY_ID, (byte) ItemDisplayContext.FIXED.getId());
		data.set(Display.DATA_SCALE_ID, new Vector3f(BLADE_SCALE));
		data.set(Display.DATA_BRIGHTNESS_OVERRIDE_ID, BRIGHTNESS_OVERRIDE);
		// 原版剑图标朝右上；顺时针 135° 后剑尖朝下（负角 = 从正面看的顺时针）
		data.set(Display.DATA_LEFT_ROTATION_ID, new Quaternionf().rotateZ((float) Math.toRadians(-135.0D)));
		data.set(Display.DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, 2);
		data.set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, 2);
		body.setPos(at.x, at.y, at.z);
		body.setNoGravity(true);
		body.setSilent(true);
		body.addTag("sephiria_cryo_guillotine");
		level.addFreshEntity(body);
		return body;
	}

	private static void advanceBlades(MinecraftServer server) {
		if (BLADES.isEmpty()) {
			return;
		}

		List<Blade> inFlight = new ArrayList<>(BLADES);
		BLADES.clear();

		for (Blade blade : inFlight) {
			Blade next = advance(blade);

			if (next != null) {
				BLADES.add(next);
			}
		}
	}

	private static Blade advance(Blade blade) {
		ServerLevel level = blade.level();
		int tick = blade.tick();

		if (tick < FALL_TICKS) {
			double progress = tick / (double) FALL_TICKS;
			Vec3 at = blade.start().lerp(blade.impact(), progress);
			moveBlade(blade.body(), at);
			level.sendParticles(blade.flame() ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE,
					at.x, at.y, at.z, 2, 0.25D, 0.25D, 0.25D, 0.01D);
			level.sendParticles(ModParticleTypes.BLUE_SPARK, at.x, at.y, at.z, 1, 0.2D, 0.2D, 0.2D, 0.01D);
			return new Blade(level, blade.ownerId(), blade.target(), blade.impact(), blade.start(),
					blade.damage(), blade.flame(), tick + 1, blade.body());
		}

		if (tick == FALL_TICKS) {
			moveBlade(blade.body(), blade.impact());
			impact(blade);
		}

		if (tick >= FALL_TICKS + LINGER_TICKS) {
			if (blade.body() != null) {
				blade.body().discard();
			}

			return null;
		}

		return new Blade(level, blade.ownerId(), blade.target(), blade.impact(), blade.start(),
				blade.damage(), blade.flame(), tick + 1, blade.body());
	}

	private static void moveBlade(Display.ItemDisplay body, Vec3 at) {
		if (body != null) {
			body.setPos(at.x, at.y, at.z);
		}
	}

	private static void impact(Blade blade) {
		ServerLevel level = blade.level();
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(blade.ownerId());
		LivingEntity target = blade.target();
		boolean damaged = false;

		if (owner != null && target != null && target.isAlive() && !target.isRemoved()
				&& target.distanceToSqr(blade.impact()) <= 36.0D) {
			SephiriaDamage.clearDamageCooldown(target);
			// 火焰模式跟剑 / 锤 / 矛一样换成 fire_attack（火属性，触发灼伤）；
			// 默认仍是冰 + 电的 cryo_guillotine
			damaged = target.hurtServer(level, blade.flame()
							? SephiriaDamage.fireAttack(level, owner)
							: SephiriaDamage.cryoGuillotine(level, owner),
					blade.damage());
		}

		Vec3 at = blade.impact();
		level.sendParticles(blade.flame() ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE,
				at.x, at.y + 0.5D, at.z, 18, 0.5D, 0.6D, 0.5D, 0.08D);
		level.sendParticles(ModParticleTypes.BLUE_SPARK, at.x, at.y + 0.5D, at.z, 12, 0.45D, 0.5D, 0.45D, 0.06D);

		if (damaged) {
			playImpactSphere(level, at, blade.flame());
		}

		level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.7F);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6F, 1.3F);
	}

	/** 落地时的球形爆炸：冰蓝版撒雪片，火焰版撒火苗；球壳都补电火花。 */
	private static void playImpactSphere(ServerLevel level, Vec3 impact, boolean flame) {
		RandomSource random = level.getRandom();
		Vec3 center = impact.add(0.0D, 0.8D, 0.0D);
		DustParticleOptions dust = new DustParticleOptions(flame ? FLAME_ORANGE : ICE_BLUE, 1.1F);

		for (int index = 0; index < IMPACT_SPHERE_DUST; index++) {
			double radius = IMPACT_SPHERE_RADIUS * Math.cbrt(random.nextDouble());
			double theta = random.nextDouble() * Math.PI * 2.0D;
			double phi = Math.acos(2.0D * random.nextDouble() - 1.0D);
			double sinPhi = Math.sin(phi);
			double x = radius * sinPhi * Math.cos(theta);
			double y = radius * Math.cos(phi);
			double z = radius * sinPhi * Math.sin(theta);
			level.sendParticles(dust, center.x + x, center.y + y, center.z + z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}

		for (int index = 0; index < IMPACT_SPHERE_SNOW; index++) {
			Vec3 point = spherePoint(random, IMPACT_SPHERE_RADIUS * 0.9D);
			level.sendParticles(flame ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE,
					center.x + point.x, center.y + point.y, center.z + point.z,
					1, 0.0D, 0.0D, 0.0D, 0.01D);
		}

		for (int index = 0; index < IMPACT_SPHERE_SPARKS; index++) {
			Vec3 point = spherePoint(random, IMPACT_SPHERE_RADIUS);
			level.sendParticles(ModParticleTypes.BLUE_SPARK, center.x + point.x, center.y + point.y,
					center.z + point.z, 1, 0.0D, 0.0D, 0.0D, 0.02D);
		}
	}

	private static Vec3 spherePoint(RandomSource random, double radius) {
		double theta = random.nextDouble() * Math.PI * 2.0D;
		double phi = Math.acos(2.0D * random.nextDouble() - 1.0D);
		double sinPhi = Math.sin(phi);
		return new Vec3(radius * sinPhi * Math.cos(theta),
				radius * Math.cos(phi),
				radius * sinPhi * Math.sin(theta));
	}

	/** 一把正在下落 / 停在落点的铁剑。 */
	private record Blade(ServerLevel level, UUID ownerId, LivingEntity target, Vec3 impact, Vec3 start,
			float damage, boolean flame, int tick, Display.ItemDisplay body) {
	}

	public static void forget(ServerPlayer player) {
		READY_AT.remove(player.getUUID());
		BLADES.removeIf(blade -> {
			if (!blade.ownerId().equals(player.getUUID())) {
				return false;
			}

			if (blade.body() != null) {
				blade.body().discard();
			}

			return true;
		});
	}
}
