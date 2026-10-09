package com.sephiria.artifact;

import com.sephiria.debuff.Debuffs;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.registry.ModItems;
import com.sephiria.stats.PlayerStats;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
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
 * 红蛇之眼（余烬，高级品质）：【唯一】每 5 秒在附近掉落 1/1/2/2/3/4 个陨石，
 * 每颗造成火焰属性伤害 60/70/70/80/80/80% 的落地伤害，被击中的敌人额外附加「灼伤」减益。
 *
 * <p>取目标与桑德耳环同一套：只挑「有威胁的生物」（{@code SephiriaDamage.strikeable}，玩家不在池里）、
 * 每次取最近的若干个；陨石数比敌人多时绕回来重复砸同一个目标（落点带随机偏移，看着像一轮轰炸）。
 *
 * <p>数值口径：伤害 = 火元素强度面板值 × 倍率，在<b>发出陨石那一刻</b>算好（岩浆从天上落下的
 * 大半秒里换装不改这一发）。伤害走 {@code sephiria:fire_attack}（Kind.FIRE_ATTACK）：
 * 吃通用暴击、减伤与黄金之手，也会触发火焰之触（「攻击打出的火属性伤害」那一类）；
 * 砸中之后另外调用 {@code Debuffs#applyBurn} <b>直接</b>挂灼伤——不要求余烬连击到 2 档，
 * 也不吃火焰之触的 2 秒冷却（与桑德耳环直接上触电同型）。
 *
 * <p>视觉分四段（2026-10 重做）：<b>出现</b>（落点上空一记白光、本体从无到有放大）→
 * <b>下落</b>（本体是一颗岩浆块 Display 实体，边转边砸下来，尾迹越接近地面越密）→
 * <b>撞击</b>（爆炸云 + 火苗 / 岩浆 / 石屑 + 贴地冲击环）→ <b>消失</b>（停在落点原地碎裂、
 * 缩小成烟，最后连同实体一起收掉）。本体用原版方块显示实体：26.3 把它的配置全收进了私有的
 * 同步数据，靠 {@code sephiria.accesswidener} 打开那几个访问器直接写（两边都吃访问拓宽，
 * 渲染仍走原版，不需要客户端代码）。
 */
public class RedSnakeEyeItem extends ArtifactItem {
	/** 各等级每次掉落的陨石数。 */
	private static final int[] COUNT_BY_LEVEL = { 1, 1, 2, 2, 3, 4 };
	/** 各等级陨石的伤害倍率（%）：火元素强度面板值的百分之几。 */
	private static final double[] DAMAGE_BY_LEVEL = { 60.0D, 70.0D, 70.0D, 80.0D, 80.0D, 80.0D };
	/** 冷却（tick）：5 秒。 */
	private static final int INTERVAL_TICKS = 100;
	/** 「附近」的半径（格）：与乌云的索敌范围同口径。 */
	private static final double RADIUS = 8.0D;
	/** 落地伤害的范围（格）：落点为圆心。 */
	private static final double IMPACT_RADIUS = 2.5D;
	/** 落点的随机偏移（格）：同一轮砸同一个目标时几颗石头不会完全重合。 */
	private static final double SCATTER = 1.6D;
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	// —— 演出：出现（上空现形放大）→ 下落 → 撞击（一瞬）→ 消失（原地碎裂缩小）——

	/** 出现动画时长（tick）：在落点上空现形，从 {@link #BODY_SCALE_MIN} 长大到 {@link #BODY_SCALE}。 */
	private static final int APPEAR_TICKS = 6;
	/** 下落轨迹的刻数（tick）：从落点上方 {@link #FALL_HEIGHT} 格处砸下来，约 0.4 秒。 */
	private static final int FALL_TICKS = 8;
	/** 到站后多停的刻数：客户端的位置插值晚一刻才跟上，多等这一下，爆炸才与「石头砸到地上」同一刻。 */
	private static final int FALL_TAIL_TICKS = 1;
	/** 消失动画时长（tick）：停在落点碎裂缩小，收尾时把本体实体移除。 */
	private static final int VANISH_TICKS = 8;
	/** 现形的高度（格）：在落点正上方这么高的地方出现。 */
	private static final double FALL_HEIGHT = 14.0D;
	/** 本体的满尺寸（格）：岩浆块是 1×1×1，稍微放大一点才有体积感。 */
	private static final float BODY_SCALE = 1.15F;
	/** 出现 / 消失动画的首尾尺寸：不给 0——退化的缩放矩阵渲染会出怪东西。 */
	private static final float BODY_SCALE_MIN = 0.05F;
	/** 三段各自的自转总角度（度）：出现时慢慢转，落地碎裂时转得最急。 */
	private static final float APPEAR_SPIN = 90.0F;
	private static final float FALL_SPIN = 360.0F;
	private static final float VANISH_SPIN = 720.0F;
	/** 本体的亮度覆盖（满亮）：岩浆块的光来自自发光贴图，不覆写的话夜里就是一团黑。 */
	private static final int BRIGHTNESS_OVERRIDE = 0xF000F0;
	/** 缩放 / 自转的客户端插值时长（tick）：每刻都在改这两个值，插两刻看起来才是连续的动画。 */
	private static final int TRANSFORM_INTERPOLATION_TICKS = 2;
	/** 位移的客户端插值时长（tick）：与 {@link #FALL_TAIL_TICKS} 配套。 */
	private static final int POS_INTERPOLATION_TICKS = 1;
	/** 撞击的贴地冲击环：粒子数与半径（格）。 */
	private static final int SHOCKWAVE_PARTICLES = 16;
	private static final double SHOCKWAVE_RADIUS = 2.2D;
	/**
	 * 高空那两声（现形 / 下坠）的音量：原版音效只发给「音量 × 16」格内的玩家，
	 * 而这两声响都在落点上方 14 格处，站得远一点的玩家本该听见却会被距离卡掉——音量得压过这段高度差。
	 */
	private static final float SKY_SOUND_VOLUME = 1.2F;
	/** 本体的清理标签：演出表只在内存里，异常退出（崩服）时留在世界里的本体靠它扫掉。 */
	private static final String METEOR_TAG = "sephiria_meteor";
	/** 清扫残留本体的半径（格）：落石都发生在玩家附近，按玩家位置扫就够。 */
	private static final double SWEEP_RADIUS = 64.0D;

	/** 上次掉落陨石的刻（装上后先等一个周期，不立刻砸）。 */
	private static final Map<UUID, Long> LAST_METEOR = new HashMap<>();

	/** 演出中的陨石：出现 / 下落 / 消失三个阶段都由它推进，撞击只是过渡的一瞬。 */
	private static final List<Meteor> METEORS = new ArrayList<>();

	/** 陨石的演出阶段。 */
	private enum Phase {
		/** 上空现形、放大。 */
		APPEAR,
		/** 沿直线砸向落点。 */
		FALL,
		/** 停在落点碎裂缩小，走完就移除本体。 */
		VANISH
	}

	/**
	 * 一颗演出中的陨石。
	 *
	 * @param impact 落点（伤害与全部特效都围着它）
	 * @param damage 在发出陨石那一刻算好的伤害
	 * @param tick   当前阶段已经画过几刻
	 * @param body   岩浆块本体；造不出来时为 null，那就只剩粒子（伤害照旧）
	 */
	private record Meteor(ServerLevel level, UUID ownerId, Vec3 impact, float damage, Phase phase, int tick,
			Display.BlockDisplay body) {
	}

	public RedSnakeEyeItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.EMBER;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.red_snake_eye.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return COUNT_BY_LEVEL.length - 1;
	}

	@Override
	public int meteorCount(int level) {
		return COUNT_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, COUNT_BY_LEVEL.length - 1)];
	}

	@Override
	public double meteorDamagePercent(int level) {
		return valueAt(DAMAGE_BY_LEVEL, level);
	}

	/** 每 5 秒的陨石（服务端刻推进；没带红蛇之眼的玩家直接跳过）。 */
	public static void registerTicker() {
		ServerTickEvents.END_SERVER_TICK.register(RedSnakeEyeItem::tick);
	}

	private static void tick(MinecraftServer server) {
		advanceMeteors(server);

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive()) {
				LAST_METEOR.remove(player.getUUID());
				continue;
			}

			int level = ArtifactEffects.highestLevelOf(player, ModItems.RED_SNAKE_EYE);
			int count = level < 0 ? 0 : meteorCountAt(level);

			if (count <= 0) {
				LAST_METEOR.remove(player.getUUID());
				continue;
			}

			long now = player.level().getGameTime();
			Long last = LAST_METEOR.get(player.getUUID());

			// 第一次见到（刚装上）：先记下时间，等满 5 秒再落第一轮
			if (last == null) {
				LAST_METEOR.put(player.getUUID(), now);
				continue;
			}

			if (now - last < INTERVAL_TICKS) {
				continue;
			}

			// 附近没有有威胁的生物就不落石、也不吃这次冷却（等人打上门来再砸）
			if (summon(player, count)) {
				LAST_METEOR.put(player.getUUID(), now);
			}
		}
	}

	/** 该等级的陨石数（静态取法，供推进器用）。 */
	private static int meteorCountAt(int level) {
		return COUNT_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, COUNT_BY_LEVEL.length - 1)];
	}

	/**
	 * 朝最近的若干个有威胁生物各落一颗陨石；敌人不够就绕回来重复砸。
	 *
	 * @return 是否真的落了石头（附近没有威胁生物时返回 false）
	 */
	private static boolean summon(ServerPlayer player, int count) {
		if (!(player.level() instanceof ServerLevel level)) {
			return false;
		}

		double percent = ArtifactEffects.meteorDamagePercent(player);
		double damage = PlayerStats.elementTotal(player, PlayerStats.Element.FIRE) * percent / 100.0D;

		if (damage <= 0.0D) {
			return false;
		}

		AABB area = player.getBoundingBox().inflate(RADIUS, RADIUS, RADIUS);
		List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area).stream()
				.filter(victim -> SephiriaDamage.strikeable(victim, player))
				.sorted(Comparator.comparingDouble(victim -> victim.distanceToSqr(player)))
				.toList();

		if (victims.isEmpty()) {
			return false;
		}

		for (int index = 0; index < count; index++) {
			LivingEntity target = victims.get(index % victims.size());
			Vec3 impact = new Vec3(
					target.getX() + (level.getRandom().nextDouble() - 0.5D) * SCATTER,
					target.getY(),
					target.getZ() + (level.getRandom().nextDouble() - 0.5D) * SCATTER);
			METEORS.add(new Meteor(level, player.getUUID(), impact, (float) damage, Phase.APPEAR, 0,
					spawnBody(level, skyPoint(impact))));
		}

		return true;
	}

	/** 现形点：落点正上方 {@link #FALL_HEIGHT} 格。 */
	private static Vec3 skyPoint(Vec3 impact) {
		return impact.add(0.0D, FALL_HEIGHT, 0.0D);
	}

	/** 现形与撞击用的闪光：这版的 FLASH 是可着色粒子，颜色得自己给（暖白偏橙才像火）。 */
	private static ColorParticleOption flash() {
		return ColorParticleOption.create(ParticleTypes.FLASH, 1.0F, 0.94F, 0.78F);
	}

	/**
	 * 造一颗陨石本体：岩浆块的方块显示实体，满亮、无重力、带清理标签。
	 *
	 * <p>26.3 的 Display 只留了私有同步数据访问器（连 setBlockState 都是私有的），
	 * 这里靠访问拓宽直接把值写进同步数据——与原版自己装载 Display 的路径一致。
	 */
	private static Display.BlockDisplay spawnBody(ServerLevel level, Vec3 at) {
		Display.BlockDisplay body = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);

		if (body == null) {
			return null;
		}

		SynchedEntityData data = body.getEntityData();
		// 方块显示的原点在方块的一角，平移半格才是「方块中心在实体位置」
		data.set(Display.DATA_TRANSLATION_ID, new Vector3f(-0.5F, -0.5F, -0.5F));
		data.set(Display.DATA_SCALE_ID, new Vector3f(BODY_SCALE_MIN));
		data.set(Display.DATA_BRIGHTNESS_OVERRIDE_ID, BRIGHTNESS_OVERRIDE);
		data.set(Display.DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, TRANSFORM_INTERPOLATION_TICKS);
		data.set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, POS_INTERPOLATION_TICKS);
		data.set(Display.BlockDisplay.DATA_BLOCK_STATE_ID, Blocks.MAGMA_BLOCK.defaultBlockState());
		body.setPos(at.x, at.y, at.z);
		body.setNoGravity(true);
		body.setSilent(true);
		body.addTag(METEOR_TAG);
		level.addFreshEntity(body);
		return body;
	}

	/** 把演出中的陨石往前推一刻：先画当前阶段的特效，阶段走完就交接给下一段。 */
	private static void advanceMeteors(MinecraftServer server) {
		if (METEORS.isEmpty()) {
			return;
		}

		// 先拷一份再清空：这一轮里要把还差几刻的陨石写回去，直接原地增删会踩到迭代器
		List<Meteor> inFlight = new ArrayList<>(METEORS);
		METEORS.clear();

		for (Meteor meteor : inFlight) {
			Meteor next = advance(meteor);

			if (next != null) {
				METEORS.add(next);
			}
		}

		// 刚演完最后一批：顺手清掉「上次崩服 / 关服时留在世界里」的本体
		if (METEORS.isEmpty()) {
			sweepOrphans(server);
		}
	}

	/** 推进一颗陨石一刻；返回 null 表示它已经演完、可以收工了。 */
	private static Meteor advance(Meteor meteor) {
		Phase phase = meteor.phase();
		// 进度按「最后一刻刚好到 1」归一化（除以 duration-1）：出现要长到满尺寸、消失要缩到最小
		double progress = meteor.tick() / (double) (durationOf(phase) - 1);

		switch (phase) {
			case APPEAR -> renderAppear(meteor, progress);
			case FALL -> renderFall(meteor, progress);
			case VANISH -> renderVanish(meteor, progress);
		}

		if (meteor.tick() + 1 < durationOf(phase)) {
			return withPhase(meteor, phase, meteor.tick() + 1);
		}

		return switch (phase) {
			case APPEAR -> {
				startFall(meteor);
				yield withPhase(meteor, Phase.FALL, 0);
			}
			case FALL -> {
				impact(meteor);
				startVanish(meteor);
				yield withPhase(meteor, Phase.VANISH, 0);
			}
			case VANISH -> {
				removeBody(meteor.body());
				yield null;
			}
		};
	}

	/** 某个阶段占几刻。 */
	private static int durationOf(Phase phase) {
		return switch (phase) {
			case APPEAR -> APPEAR_TICKS;
			case FALL -> FALL_TICKS + FALL_TAIL_TICKS;
			case VANISH -> VANISH_TICKS;
		};
	}

	/** 换个阶段 / 推进一刻（其余字段原样带着走）。 */
	private static Meteor withPhase(Meteor meteor, Phase phase, int tick) {
		return new Meteor(meteor.level(), meteor.ownerId(), meteor.impact(), meteor.damage(), phase, tick,
				meteor.body());
	}

	/** 出现：本体在上空从小放大，四周冒着火苗与烟；起手一记白光 + 一声闷响。 */
	private static void renderAppear(Meteor meteor, double progress) {
		ServerLevel level = meteor.level();
		Vec3 at = skyPoint(meteor.impact());
		double grow = BODY_SCALE_MIN + (BODY_SCALE - BODY_SCALE_MIN) * progress;

		setBodyScale(meteor.body(), (float) grow);
		setBodySpin(meteor.body(), (float) (progress * APPEAR_SPIN));

		if (progress == 0.0D) {
			level.sendParticles(flash(), at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 10, 0.7D, 0.7D, 0.7D, 0.02D);
			level.playSound(null, at.x, at.y, at.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS,
					SKY_SOUND_VOLUME, 0.6F);
		}

		level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 3 + (int) (6 * progress),
				0.5D * grow, 0.5D * grow, 0.5D * grow, 0.02D);
		level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.3D, at.z, 2, 0.5D, 0.4D, 0.5D, 0.01D);
	}

	/** 转入下落：起手一声下坠的呼啸。 */
	private static void startFall(Meteor meteor) {
		Vec3 at = skyPoint(meteor.impact());
		meteor.level().playSound(null, at.x, at.y, at.z, SoundEvents.PHANTOM_SWOOP, SoundSource.PLAYERS,
				SKY_SOUND_VOLUME, 0.55F);
	}

	/** 下落：本体沿直线砸向落点，尾迹（火苗 + 石屑 + 烟）越接近地面越密。 */
	private static void renderFall(Meteor meteor, double progress) {
		ServerLevel level = meteor.level();
		// 轨迹在 FALL_TICKS 刻里走完；最后一刻（进度 1）停在落点上，等客户端那 1 刻的插值跟上来
		double travel = Math.min(1.0D, progress);
		Vec3 at = skyPoint(meteor.impact()).lerp(meteor.impact(), travel);

		if (meteor.body() != null) {
			meteor.body().setPos(at.x, at.y, at.z);
		}

		setBodySpin(meteor.body(), (float) (APPEAR_SPIN + travel * FALL_SPIN));

		level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 3 + (int) (6 * travel),
				0.25D, 0.25D, 0.25D, 0.02D);
		level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.6D, at.z, 2 + (int) (3 * travel),
				0.3D, 0.3D, 0.3D, 0.01D);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.defaultBlockState()),
				at.x, at.y, at.z, 1 + (int) (3 * travel), 0.3D, 0.3D, 0.3D, 0.05D);
		level.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 1, 0.2D, 0.2D, 0.2D, 0.0D);
	}

	/** 落地：范围内所有有威胁生物吃一次火属性伤害，并被直接挂上灼伤；同时炸出撞击特效。 */
	private static void impact(Meteor meteor) {
		ServerLevel level = meteor.level();
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(meteor.ownerId());
		AABB area = new AABB(meteor.impact(), meteor.impact()).inflate(IMPACT_RADIUS);
		List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area).stream()
				.filter(victim -> owner == null ? !(victim instanceof net.minecraft.world.entity.player.Player)
						: SephiriaDamage.strikeable(victim, owner))
				.toList();

		for (LivingEntity victim : victims) {
			// 同一次落点多目标、且要能稳定打中：先清无敌帧（真实伤害同款手法）
			victim.setInvulnerableTime(0);
			victim.hurtServer(level, SephiriaDamage.fireAttack(level, owner), meteor.damage());

			// 被陨石击中的敌人附加灼伤：直接调用附加入口，不经过火焰之触的冷却
			if (owner != null) {
				Debuffs.applyBurn(victim, owner);
			}
		}

		Vec3 at = meteor.impact();
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.4D, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.3D, at.z, 26, 0.5D, 0.4D, 0.5D, 0.09D);
		level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.2D, at.z, 12, 0.4D, 0.3D, 0.4D, 0.0D);
		level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.5D, at.z, 18, 0.5D, 0.5D, 0.5D, 0.03D);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.defaultBlockState()),
				at.x, at.y + 0.3D, at.z, 30, 0.6D, 0.3D, 0.6D, 0.14D);
		level.sendParticles(flash(), at.x, at.y + 0.5D, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		shockwave(level, at);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F,
				0.65F + level.getRandom().nextFloat() * 0.2F);
	}

	/** 贴地的冲击环：一圈往外推的尘烟，落地那一下才看得见的「震荡」。 */
	private static void shockwave(ServerLevel level, Vec3 at) {
		for (int index = 0; index < SHOCKWAVE_PARTICLES; index++) {
			double angle = Math.PI * 2.0D * index / SHOCKWAVE_PARTICLES;
			level.sendParticles(ParticleTypes.CLOUD,
					at.x + Math.cos(angle) * SHOCKWAVE_RADIUS, at.y + 0.15D,
					at.z + Math.sin(angle) * SHOCKWAVE_RADIUS, 1, 0.1D, 0.05D, 0.1D, 0.06D);
		}
	}

	/** 转入消失：本体定死在落点（位移插值归零，免得还悬在插值半路上），配一声岩浆嘶响。 */
	private static void startVanish(Meteor meteor) {
		if (meteor.body() != null) {
			meteor.body().getEntityData().set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, 0);
			meteor.body().setPos(meteor.impact().x, meteor.impact().y, meteor.impact().z);
		}

		Vec3 at = meteor.impact();
		meteor.level().playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
				0.8F, 0.7F);
	}

	/** 消失：本体停在落点原地碎裂缩小，石屑与烟越冒越少。 */
	private static void renderVanish(Meteor meteor, double progress) {
		ServerLevel level = meteor.level();
		Vec3 at = meteor.impact();
		double left = 1.0D - progress;

		setBodyScale(meteor.body(), (float) (BODY_SCALE_MIN + (BODY_SCALE - BODY_SCALE_MIN) * left));
		setBodySpin(meteor.body(), (float) (APPEAR_SPIN + FALL_SPIN + progress * VANISH_SPIN));

		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.defaultBlockState()),
				at.x, at.y + 0.4D, at.z, 2 + (int) (5 * left), 0.35D, 0.35D, 0.35D, 0.08D);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.5D, at.z, 1 + (int) (3 * left),
				0.3D, 0.3D, 0.3D, 0.02D);
		level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.3D, at.z, 1 + (int) (3 * left),
				0.3D, 0.2D, 0.3D, 0.02D);
	}

	/** 设置本体的大小。 */
	private static void setBodyScale(Display.BlockDisplay body, float scale) {
		if (body != null) {
			body.getEntityData().set(Display.DATA_SCALE_ID, new Vector3f(scale));
		}
	}

	/** 设置本体的自转角（绕 Y 轴，度）。 */
	private static void setBodySpin(Display.BlockDisplay body, float degrees) {
		if (body != null) {
			body.getEntityData().set(Display.DATA_LEFT_ROTATION_ID,
					new Quaternionf().rotateY((float) Math.toRadians(degrees)));
		}
	}

	/** 收掉本体。 */
	private static void removeBody(Display.BlockDisplay body) {
		if (body != null) {
			body.discard();
		}
	}

	/**
	 * 清扫残留的本体：演出表是内存里的，崩服 / 关服恰好卡在演出中间时，本体（岩浆块）会留在
	 * 世界里。落石只发生在玩家附近，所以每次落完一批就按标签扫一圈玩家周围的方块显示。
	 */
	private static void sweepOrphans(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!(player.level() instanceof ServerLevel level)) {
				continue;
			}

			List<Display.BlockDisplay> orphans = level.getEntitiesOfClass(Display.BlockDisplay.class,
					player.getBoundingBox().inflate(SWEEP_RADIUS), body -> body.entityTags().contains(METEOR_TAG));

			for (Display.BlockDisplay orphan : orphans) {
				orphan.discard();
			}
		}
	}

	/** 玩家退出时清计时表。 */
	public static void forget(ServerPlayer player) {
		LAST_METEOR.remove(player.getUUID());
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.meteor",
						Component.literal(Numbers.format(this.meteorCount(level))).withColor(COLOUR_BONUS),
						Component.literal(Numbers.format(this.meteorDamagePercent(level))).withColor(COLOUR_BONUS)));
	}
}
