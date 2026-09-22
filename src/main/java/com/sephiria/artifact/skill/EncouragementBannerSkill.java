package com.sephiria.artifact.skill;

import com.sephiria.Sephiria;
import com.sephiria.artifact.EncouragementBannerItem;
import com.sephiria.stats.TimedAttributes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 鼓励旗帜（发动型神器）：原地召唤一面旗，范围内的友军攻击速度提高，持续 10 秒。
 *
 * <p>旗子本身是<b>一件丢在原地的旗帜物品</b>：不受重力、捡不起来、也不会被烧掉炸掉，就那样悬在
 * 原地当旗子用（盔甲架那套「无碰撞」在 26.2 是私有 API，而且物品本来就没有碰撞体积）；
 * 时间一到连同特效一起 discard。
 *
 * <p>范围用一圈粒子画出来（只在边缘，不铺满整片），攻速加成每 10 tick 给范围内友军刷一次，
 * 每次只给 20 tick 的时长——所以走出范围后大约一秒加成自然消失，不需要额外记录谁进来过。
 */
public final class EncouragementBannerSkill implements ArtifactSkill {
	public static final EncouragementBannerSkill INSTANCE = new EncouragementBannerSkill();

	/** 持续 10 秒。 */
	private static final int DURATION_TICKS = 200;
	/** 冷却 30 秒。 */
	private static final int COOLDOWN_TICKS = 600;
	/** 范围基准：100 = 原版铁剑横扫（1 格），与武器数据表同一套写法。 */
	private static final double RANGE_BASIS = 100.0D;
	/** 攻速加成的刷新间隔与单次时长（tick）：比刷新间隔略长，走出范围后自然会掉。 */
	private static final int REFRESH_INTERVAL = 10;
	private static final int BUFF_TICKS = 20;
	/** 边缘粒子画多少个点。 */
	private static final int RING_POINTS = 24;
	private static final Identifier BUFF_ID = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "banner_attack_speed");

	/** 场上的旗子。 */
	private static final List<Banner> BANNERS = new ArrayList<>();

	/** 一面旗子：谁放的、旗子实体、范围与攻速加成、到期时刻。 */
	/** 旗子悬停的高度（格）：升到头顶上方，避免挡住视线；竖直范围另外加宽，所以不影响增益。 */
	private static final double HOVER_HEIGHT = 3.0D;
	/** 竖直范围在水平半径之外再扩多少格（上下各一份）。 */
	private static final double VERTICAL_EXTRA = 2.0D;

	private record Banner(ServerPlayer owner, ItemEntity visual, double radius, double attackSpeed, long until) {
	}

	private EncouragementBannerSkill() {
	}

	@Override
	public String id() {
		return "encouragement_banner";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		// 发动型神器不耗蓝（耗蓝的是魔法书那类「获得技能」）
		return 0.0D;
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		if (!(player.level() instanceof ServerLevel level0)) {
			return;
		}

		// 旗子：一件丢在原地的旗帜物品，无重力、捡不起来、免疫伤害
		ItemEntity banner = new ItemEntity(level0, player.getX(), player.getY() + HOVER_HEIGHT, player.getZ(),
				new ItemStack(Items.BANNER.pick(DyeColor.LIME)));
		banner.setNoGravity(true);
		banner.setPermanentlyInvulnerable(true);
		banner.setNeverPickUp();
		banner.setDeltaMovement(0.0D, 0.0D, 0.0D);
		level0.addFreshEntity(banner);
		BANNERS.add(new Banner(player, banner, EncouragementBannerItem.bannerRange(level) / RANGE_BASIS,
				EncouragementBannerItem.bannerAttackSpeed(level), level0.getGameTime() + DURATION_TICKS));

		level0.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
	}

	/** 注册推进器（由 {@link ArtifactSkills#initialize()} 间接调用）。 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(EncouragementBannerSkill::tick);
	}

	private static void tick(MinecraftServer server) {
		if (BANNERS.isEmpty()) {
			return;
		}

		long now = server.overworld().getGameTime();
		Iterator<Banner> iterator = BANNERS.iterator();

		while (iterator.hasNext()) {
			Banner banner = iterator.next();

			if (banner.until() <= now || !banner.visual().isAlive()) {
				// 时间到（或旗子被拆了）：特效与加成一起收掉
				if (banner.visual().isAlive()) {
					banner.visual().discard();
				}

				iterator.remove();
				continue;
			}

			if (!(banner.visual().level() instanceof ServerLevel level)) {
				continue;
			}

			// 物品实体自己会往上飘/被推走，所以每 tick 把它按回原位：原地悬停
			banner.visual().setDeltaMovement(0.0D, 0.0D, 0.0D);
			banner.visual().setPos(banner.visual().xo, banner.visual().yo, banner.visual().zo);
			drawRing(level, banner);
			grant(level, banner);
		}
	}

	/** 边缘粒子：只在圆周上点几个点，间隔刷新。 */
	private static void drawRing(ServerLevel level, Banner banner) {
		if (level.getGameTime() % 5L != 0L) {
			return;
		}

		double cx = banner.visual().getX();
		double cy = banner.visual().getY();
		double cz = banner.visual().getZ();

		for (int point = 0; point < RING_POINTS; point++) {
			double angle = point * (Math.PI * 2.0D) / RING_POINTS;
			double x = cx + Math.cos(angle) * banner.radius();
			double z = cz + Math.sin(angle) * banner.radius();
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, cy + 0.3D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}

	/** 给范围内的友军刷新攻速加成：玩家，以及和施法者同阵营的生物（宠物等）。 */
	private static void grant(ServerLevel level, Banner banner) {
		if (level.getGameTime() % REFRESH_INTERVAL != 0L) {
			return;
		}

		// 竖直方向额外上下各扩 2 格：旗子悬在 3 格高，站地上也照样吃得到
		AABB area = banner.visual().getBoundingBox()
				.inflate(banner.radius(), banner.radius() + VERTICAL_EXTRA, banner.radius());

		// 注意别用 getEntities(owner, ...)：那个重载的第一个参数是「要排除的实体」，会把施法者自己排除掉
		for (net.minecraft.world.entity.LivingEntity ally : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
				area, candidate -> isAlly(candidate, banner.owner()))) {
			// 攻速加成按「当前攻速的百分比」算，所以先读一次它现在的攻速值
			double current = ally.getAttributeValue(Attributes.ATTACK_SPEED);
			TimedAttributes.grant(ally, Attributes.ATTACK_SPEED, BUFF_ID,
					current * banner.attackSpeed() / 100.0D, BUFF_TICKS);
		}
	}

	/** 友军：玩家（含施法者自己），或者与施法者同阵营的生物。 */
	private static boolean isAlly(Entity candidate, ServerPlayer owner) {
		return candidate instanceof Player || candidate.isAlliedTo(owner);
	}
}
