package com.sephiria.artifact;

import com.sephiria.debuff.Debuffs;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.registry.ModItems;
import com.sephiria.stats.PlayerStats;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
 * 0.4 秒里换装不改这一发）。伤害走 {@code sephiria:fire_attack}（Kind.FIRE_ATTACK）：
 * 吃通用暴击、减伤与黄金之手，也会触发火焰之触（「攻击打出的火属性伤害」那一类）；
 * 砸中之后另外调用 {@code Debuffs#applyBurn} <b>直接</b>挂灼伤——不要求余烬连击到 2 档，
 * 也不吃火焰之触的 2 秒冷却（与桑德耳环直接上触电同型）。
 *
 * <p>视觉：陨石从落点上方 14 格处砸下来（每刻画一条火/烟尾迹），落地炸开一圈火苗、岩浆粒与烟，
 * 音效是原版爆炸声压低音高（比 TNT 轻）。
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
	/** 下落时长（tick）：0.4 秒，从落点上方 {@link #FALL_HEIGHT} 格处砸下来。 */
	private static final int FALL_TICKS = 8;
	private static final double FALL_HEIGHT = 14.0D;
	/** 落点的随机偏移（格）：同一轮砸同一个目标时几颗石头不会完全重合。 */
	private static final double SCATTER = 1.6D;
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	/** 上次掉落陨石的刻（装上后先等一个周期，不立刻砸）。 */
	private static final Map<UUID, Long> LAST_METEOR = new HashMap<>();

	/** 正在下落的陨石：落地那一下才结算。 */
	private static final List<Falling> FALLING = new ArrayList<>();

	/** 一颗下落中的陨石：落点、剩余刻数、已经算好的伤害与触发者。 */
	private record Falling(ServerLevel level, UUID ownerId, Vec3 impact, int ticksLeft, float damage) {
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
		advanceMeteors();

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
			FALLING.add(new Falling(level, player.getUUID(), impact, FALL_TICKS, (float) damage));
		}

		return true;
	}

	/** 把下落中的陨石往前推一刻：画尾迹，落地的结算伤害与灼伤。 */
	private static void advanceMeteors() {
		if (FALLING.isEmpty()) {
			return;
		}

		// 先拷一份再清空：这一轮里要把还差几刻的陨石写回去，直接原地增删会踩到迭代器
		List<Falling> inFlight = new ArrayList<>(FALLING);
		FALLING.clear();

		for (Falling meteor : inFlight) {
			int left = meteor.ticksLeft() - 1;

			if (left > 0) {
				// 尾迹：从上往下走，越接近地面越密（残影里带烟，主体是火苗）
				double progress = (FALL_TICKS - left) / (double) FALL_TICKS;
				Vec3 position = meteor.impact().add(0.0D, FALL_HEIGHT, 0.0D).lerp(meteor.impact(), progress);
				meteor.level().sendParticles(ParticleTypes.FLAME, position.x, position.y, position.z,
						3, 0.15D, 0.15D, 0.15D, 0.02D);
				meteor.level().sendParticles(ParticleTypes.SMOKE, position.x, position.y + 0.6D, position.z,
						2, 0.2D, 0.2D, 0.2D, 0.01D);
				FALLING.add(new Falling(meteor.level(), meteor.ownerId(), meteor.impact(), left, meteor.damage()));
				continue;
			}

			impact(meteor);
		}
	}

	/** 落地：范围内所有有威胁生物吃一次火属性伤害，并被直接挂上灼伤。 */
	private static void impact(Falling meteor) {
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
		level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.3D, at.z, 26, 0.5D, 0.4D, 0.5D, 0.09D);
		level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.2D, at.z, 12, 0.4D, 0.3D, 0.4D, 0.0D);
		level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.5D, at.z, 18, 0.5D, 0.5D, 0.5D, 0.03D);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F,
				0.7F + level.getRandom().nextFloat() * 0.2F);
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
