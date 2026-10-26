package com.sephiria.cloud;

import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.debuff.Debuffs;
import com.sephiria.network.CloudSyncPayload;
import com.sephiria.registry.ModParticleTypes;
import com.sephiria.stats.CombatState;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 「乌云」连击：玩家头顶飘一朵灰黑色的烟雾云，战斗中每隔一段时间对附近<b>有威胁的生物</b>打一记雷击。
 *
 * <p>容量（堆栈）是这套系统的资源：每次攻击消耗 1 点（10 档「2 点射」连打两下、消耗 2 点），
 * 战斗中每 5 秒回 5%、脱战每 0.1 秒回 5%（都按容量上限的百分比算，见 {@link CombatState}）。
 * 容量不够就不打，等回复。战斗状态的口径是「挨打或主动打出伤害都算」——乌云只看这个开关，
 * 自己开火也会刷新它（云在打，就是在战斗；等云打完了、8 秒没人挨打没人出手，容量才回得快）。
 *
 * <p>攻击间隔由「消耗速度」决定：基础 100% = 每秒一次（20 刻），乐谱《台风》按<b>攻击速度的
 * 百分之多少</b>往上加（攻速 100% + 台风 25% → 消耗速度 125% → 每 0.8 秒一次），
 * 桅杆模型再叠一个固定点数（+20/40/60%）。
 *
 * <p>神器对雷击本身的改动：雷石按百分比放大每记伤害、云种箭头有概率把一记强化成双倍
 * （落点炸得更开）、雷文泥板有概率让这一下不扣容量（每一下单独掷）。
 *
 * <p>视觉是<b>篝火那种大团烟</b>在头顶不断鼓出翻腾（CAMPFIRE_COSY_SMOKE 的大团类圆烟形，
 * 间插 LARGE_SMOKE 压暗成灰黑），云团大小随连击档位放大（2 档 1 倍 → 10 档 3 倍）；
 * 打雷时从云心向目标画一条蓝白色火花线，音效用原版雷声的一半音量。
 */
public final class DarkCloud {
	/** 基础容量（2 档点亮时就有）。 */
	public static final int BASE_CAPACITY = 15;
	/** 基础攻击间隔（tick）：消耗速度 100% = 每 1 秒一次。 */
	private static final int BASE_INTERVAL_TICKS = 20;
	/** 单次雷击的伤害：电元素强度面板值 × 这个比例（%）。 */
	private static final double DAMAGE_PERCENT = 20.0D;
	/** 攻击范围（格）：找最近的有威胁生物。 */
	private static final double RANGE = 8.0D;
	/** 战斗中回容量的间隔与比例：每 5 秒回上限的 5%。 */
	private static final int COMBAT_REGEN_INTERVAL = 100;
	private static final double COMBAT_REGEN_PERCENT = 5.0D;
	/** 脱战回容量的间隔与比例：每 0.1 秒回上限的 5%。 */
	private static final int OUT_OF_COMBAT_REGEN_INTERVAL = 2;
	private static final double OUT_OF_COMBAT_REGEN_PERCENT = 5.0D;
	/** 乌云悬停高度（格）：比贴图时代再高两格（头顶上方不挡视线），与绕圈半径。 */
	private static final double HOVER_HEIGHT = 4.6D;
	private static final double ORBIT_RADIUS = 0.9D;
	/** 烟雾云的补烟间隔（tick）：篝火那种大团烟 spawn 频率要低，靠烟自己慢慢长大叠成云。 */
	private static final int SMOKE_INTERVAL_TICKS = 5;
	/** 雷击音效的音量：比原版雷声小 50%。 */
	private static final float STRIKE_VOLUME = 0.5F;
	/** 云体大小的最大倍率：10 档时是基础体型的 3 倍（2 档 = 1 倍，中间档线性）。 */
	private static final double BODY_SCALE_MAX = 3.0D;
	/** 体型从 1 倍长到最大倍率跨过的连击等级数（2 档 → 10 档）。 */
	private static final int BODY_SCALE_LEVELS = 8;
	/** 暴风雪云的地面影响半径（格）。 */
	private static final int BLIZZARD_RADIUS = 4;
	/** 每轮地面效果：1 次找露天水源结冰，3 次找实心方块铺雪 / 叠高。 */
	private static final int BLIZZARD_WATER_ATTEMPTS = 1;
	private static final int BLIZZARD_SNOW_ATTEMPTS = 3;
	/** 暴风雪云地面效果的间隔（tick）：5 秒。 */
	private static final int BLIZZARD_GROUND_INTERVAL = 100;
	/** 雪层最高叠到几层（与原版雪层上限一致）。 */
	private static final int BLIZZARD_SNOW_MAX_LAYERS = 8;
	/** 红石 / 容器检测半径（格）：附近有这些东西时，暴风雪云不改造地面。 */
	private static final double BLIZZARD_PROTECTION_RADIUS = 20.0D;

	/** 会被视为「红石 / 容器」的方块（按钮 / 压力板 / 铁轨 / 潜影盒 / 铜箱另有标签覆盖）。 */
	private static final Set<Block> PROTECTED_BLOCKS = Set.of(
			Blocks.REDSTONE_WIRE, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH,
			Blocks.REPEATER, Blocks.COMPARATOR, Blocks.LEVER,
			Blocks.REDSTONE_BLOCK, Blocks.REDSTONE_LAMP, Blocks.DAYLIGHT_DETECTOR,
			Blocks.TARGET, Blocks.TRIPWIRE, Blocks.TRIPWIRE_HOOK,
			Blocks.OBSERVER, Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.PISTON_HEAD, Blocks.MOVING_PISTON,
			Blocks.DISPENSER, Blocks.DROPPER, Blocks.HOPPER, Blocks.CRAFTER,
			Blocks.SCULK_SENSOR, Blocks.CALIBRATED_SCULK_SENSOR, Blocks.NOTE_BLOCK,
			Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
			Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.BARREL,
			Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.BREWING_STAND,
			Blocks.LECTERN, Blocks.DECORATED_POT, Blocks.CHISELED_BOOKSHELF);

	/** 会被视为「红石 / 容器」的掉落物 / 展示物（铁轨 / 木按钮 / 木压力板 / 潜影盒 / 箱船另有标签）。 */
	private static final Set<Item> PROTECTED_ITEMS = Set.of(
			Items.REDSTONE, Items.REDSTONE_TORCH, Items.REDSTONE_BLOCK,
			Items.REPEATER, Items.COMPARATOR, Items.LEVER,
			Items.REDSTONE_LAMP, Items.DAYLIGHT_DETECTOR, Items.TARGET,
			Items.TRIPWIRE_HOOK, Items.OBSERVER, Items.PISTON, Items.STICKY_PISTON,
			Items.DISPENSER, Items.DROPPER, Items.HOPPER, Items.CRAFTER,
			Items.SCULK_SENSOR, Items.CALIBRATED_SCULK_SENSOR, Items.NOTE_BLOCK,
			Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE,
			Items.CHEST, Items.TRAPPED_CHEST, Items.ENDER_CHEST, Items.BARREL,
			Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER, Items.BREWING_STAND,
			Items.LECTERN, Items.DECORATED_POT, Items.CHISELED_BOOKSHELF,
			Items.CHEST_MINECART, Items.FURNACE_MINECART, Items.HOPPER_MINECART,
			Items.STONE_BUTTON, Items.STONE_PRESSURE_PLATE,
			Items.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.HEAVY_WEIGHTED_PRESSURE_PLATE,
			Items.POLISHED_BLACKSTONE_BUTTON, Items.POLISHED_BLACKSTONE_PRESSURE_PLATE);

	/** 一名玩家的乌云状态（视觉改成了纯粒子，不再持有实体）。 */
	private static final class Cloud {
		private final UUID playerId;
		private double capacity = BASE_CAPACITY;
		private int attackCooldown;
		private int regenTimer;
		/** 上一次推给客户端的容量 / 上限（NaN = 还没推过，首刻必定同步一次给 HUD）。 */
		private double syncedCapacity = Double.NaN;
		private double syncedMax = Double.NaN;

		Cloud(UUID playerId) {
			this.playerId = playerId;
		}
	}

	private static final Map<UUID, Cloud> CLOUDS = new HashMap<>();

	private DarkCloud() {
	}

	/** 注册推进器（由 {@link com.sephiria.Sephiria#onInitialize()} 调用）。 */
	public static void registerTicker() {
		ServerTickEvents.END_SERVER_TICK.register(DarkCloud::tick);
	}

	/** 玩家退出时收掉状态（乌云本身只是粒子，没有东西要清）。 */
	public static void forget(ServerPlayer player) {
		CLOUDS.remove(player.getUUID());
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Cloud cloud = CLOUDS.get(player.getUUID());
			int comboLevel = ArtifactEffects.comboLevels(player)
					.getOrDefault(ArtifactCombo.DARK_CLOUD, 0);
			boolean active = player.isAlive() && ArtifactCombo.DARK_CLOUD.cloudActive(comboLevel);

			if (!active) {
				// 没点亮（或人没了）：把乌云收掉，下次点亮重新攒满容量；
				// 顺手推一份 0/0 让 HUD 的乌云 UI 跟着消失
				if (CLOUDS.remove(player.getUUID()) != null) {
					ServerPlayNetworking.send(player, new CloudSyncPayload(0.0D, 0.0D));
				}

				continue;
			}

			if (cloud == null) {
				cloud = new Cloud(player.getUUID());
				CLOUDS.put(player.getUUID(), cloud);
			}

			if (!(player.level() instanceof ServerLevel level)) {
				continue;
			}

			Vec3 pos = cloudPos(player, level.getGameTime());
			boolean blizzard = ArtifactEffects.blizzardCloud(player);
			emitSmoke(level, pos, level.getGameTime(), bodyScale(comboLevel), blizzard);

			// 暴风雪云像原版降雪一样改造地面：水结冰、地面铺雪层（只在露天的目标上做）。
			// 附近 20 格内有红石元件 / 容器 / 红石物品时整段跳过（用户 2026-10-25 定）。
			if (blizzard && level.getGameTime() % BLIZZARD_GROUND_INTERVAL == 0L
					&& !isBlizzardBlocked(level, player)) {
				applyBlizzardGround(level, player);
			}

			regen(player, cloud, comboLevel);
			tryStrike(level, player, cloud, comboLevel, pos);
			syncCapacity(player, cloud, comboLevel);
		}
	}

	/**
	 * 容量 / 上限有变化才推给客户端（HUD 的乌云 UI 读它）。
	 *
	 * <p>变化频率不高：出手时 -1、回复是每 2 刻（脱战）或每 5 秒（战斗）一次、回满自然停；
	 * 上限变化（换神器 / 连击档位）也会被这里带出去。Cloud 里存的上一份初值是 NaN，
	 * 所以刚激活那一 tick 必定同步一次。
	 */
	private static void syncCapacity(ServerPlayer player, Cloud cloud, int comboLevel) {
		double max = maxCapacity(player, comboLevel);

		if (cloud.syncedCapacity == cloud.capacity && cloud.syncedMax == max) {
			return;
		}

		cloud.syncedCapacity = cloud.capacity;
		cloud.syncedMax = max;
		ServerPlayNetworking.send(player, new CloudSyncPayload(cloud.capacity, max));
	}

	/** 容量上限：基础 15 + 连击各档的加成 + 神器的「乌云容量」加成（沙波特果实 / 避雷针）。 */
	public static double maxCapacity(ServerPlayer player, int comboLevel) {
		return BASE_CAPACITY + ArtifactCombo.DARK_CLOUD.cloudCapacity(comboLevel)
				+ ArtifactEffects.cloudCapacityBonus(player);
	}

	/** 乌云此刻的位置：玩家头顶上方两格多，绕圈 + 上下轻飘。 */
	private static Vec3 cloudPos(ServerPlayer player, long now) {
		double angle = (now % 160L) / 160.0D * Math.PI * 2.0D;
		double x = player.getX() + Math.cos(angle) * ORBIT_RADIUS;
		double z = player.getZ() + Math.sin(angle) * ORBIT_RADIUS;
		double y = player.getY() + HOVER_HEIGHT + Math.sin(now / 14.0D) * 0.12D;
		return new Vec3(x, y, z);
	}

	/**
	 * 灰黑色的乌云：主体是<b>篝火那种大团烟（CAMPFIRE_COSY_SMOKE）</b>——类圆、粒子大、
	 * 慢悠悠往上飘，靠低频率不断补烟让大团一个个鼓出来再散掉；间插一点大团黑烟
	 * （LARGE_SMOKE，小一号的暗斑）把整体压向灰黑——原版篝火烟粒本身是浅色的，
	 * 单用它到不了「灰黑」，两条一起就是「篝火的形状 + 灰黑色」。
	 *
	 * <p>云团大小随连击档位放大（见 {@link #bodyScale}）：铺开的范围按倍率放大，
	 * 烟粒数量按面积（倍率的平方）跟着涨——云团变大但不会变稀。
	 */
	private static void emitSmoke(ServerLevel level, Vec3 pos, long now, double scale, boolean blizzard) {
		if (now % SMOKE_INTERVAL_TICKS != 0L) {
			return;
		}

		int count = (int) Math.round(scale * scale);
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z,
				count, 0.35D * scale, 0.1D * scale, 0.35D * scale, 0.0D);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z,
				count, 0.5D * scale, 0.15D * scale, 0.5D * scale, 0.0D);

		if (blizzard) {
			// 暴风雪：云下一直飘雪。雪是显式粒子，与群系 / 高度 / 天气无关，
			// 任何情况下都是雪，不会像原版降雨那样在低温群系外变成雨。
			// 数量比烟多一些，低档位也能看出「在下雪」。
			level.sendParticles(ModParticleTypes.SNOWFALL, pos.x, pos.y - 0.4D, pos.z,
					Math.max(4, count * 3), 1.1D * scale, 0.15D, 1.1D * scale, 0.01D);
		}
	}

	/**
	 * 玩家周围 {@value #BLIZZARD_PROTECTION_RADIUS} 格内有没有红石元件 / 容器 / 红石物品。
	 *
	 * <p>有的话暴风雪云这一轮不改造地面（雪粒子照常下），避免冻住红石农场的水、往箱子上积雪
	 * 或把红石线路埋掉。方块扫一遍 20 格半径，外加掉落的红石物品、物品展示框、运输矿车 / 箱船。
	 */
	private static boolean isBlizzardBlocked(ServerLevel level, ServerPlayer player) {
		AABB area = player.getBoundingBox().inflate(BLIZZARD_PROTECTION_RADIUS);

		if (level.getBlockStates(area).anyMatch(DarkCloud::isProtectedBlock)) {
			return true;
		}

		for (Entity entity : level.getEntitiesOfClass(Entity.class, area)) {
			if (entity instanceof Container) {
				return true;
			}

			if (entity instanceof ItemEntity item && isProtectedItem(item.getItem())) {
				return true;
			}

			if (entity instanceof ItemFrame frame && isProtectedItem(frame.getItem())) {
				return true;
			}
		}

		return false;
	}

	/** 红石元件 / 容器方块。 */
	private static boolean isProtectedBlock(BlockState state) {
		if (state.is(BlockTags.RAILS) || state.is(BlockTags.BUTTONS) || state.is(BlockTags.PRESSURE_PLATES)
				|| state.is(BlockTags.SHULKER_BOXES) || state.is(BlockTags.COPPER_CHESTS)
				|| state.getBlock() instanceof CopperBulbBlock) {
			return true;
		}

		return PROTECTED_BLOCKS.contains(state.getBlock());
	}

	/** 红石相关物品 / 容器物品（掉落物或展示框里的那件）。 */
	private static boolean isProtectedItem(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		if (stack.is(ItemTags.RAILS) || stack.is(ItemTags.WOODEN_BUTTONS)
				|| stack.is(ItemTags.WOODEN_PRESSURE_PLATES) || stack.is(ItemTags.SHULKER_BOXES)
				|| stack.is(ItemTags.CHEST_BOATS)) {
			return true;
		}

		return PROTECTED_ITEMS.contains(stack.getItem());
	}

	/**
	 * 暴风雪云的地面改造（每 5 秒一次）：
	 * <ul>
	 *   <li>随机挑 <b>1</b> 列：如果这一列的表面是露天水源，冻成冰；</li>
	 *   <li>随机挑 <b>3</b> 列：如果表面是露天、上方是空气的实心方块，铺一层雪；
	 *       已有雪层则叠高（最多 8 层）；</li>
	 *   <li>植物 / 其它非实心方块：这一列跳过。</li>
	 * </ul>
	 * 只改<b>能看见天空</b>的目标，室内 / 洞穴 / 屋檐下不会凭空积雪或结冰。
	 */
	private static void applyBlizzardGround(ServerLevel level, ServerPlayer player) {
		for (int attempt = 0; attempt < BLIZZARD_WATER_ATTEMPTS; attempt++) {
			freezeBlizzardWater(level, player);
		}

		for (int attempt = 0; attempt < BLIZZARD_SNOW_ATTEMPTS; attempt++) {
			snowBlizzardGround(level, player);
		}
	}

	/** 随机一列：露天水源 → 冰。 */
	private static void freezeBlizzardWater(ServerLevel level, ServerPlayer player) {
		BlockPos pos = randomSurface(level, player);

		if (pos == null) {
			return;
		}

		BlockState state = level.getBlockState(pos);

		if (state.is(Blocks.WATER) && state.getFluidState().isSource() && level.canSeeSky(pos.above())) {
			level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
		}
	}

	/** 随机一列：露天实心方块上方铺雪；已有雪层则叠高。 */
	private static void snowBlizzardGround(ServerLevel level, ServerPlayer player) {
		BlockPos pos = randomSurface(level, player);

		if (pos == null) {
			return;
		}

		BlockState state = level.getBlockState(pos);

		if (state.is(Blocks.SNOW)) {
			int layers = state.getValue(SnowLayerBlock.LAYERS);

			if (layers < BLIZZARD_SNOW_MAX_LAYERS && level.canSeeSky(pos)) {
				level.setBlockAndUpdate(pos, state.setValue(SnowLayerBlock.LAYERS, layers + 1));
			}

			return;
		}

		if (state.isSolid()) {
			BlockPos above = pos.above();

			if (level.getBlockState(above).isAir() && level.canSeeSky(above)) {
				level.setBlockAndUpdate(above, Blocks.SNOW.defaultBlockState());
			}
		}
	}

	/**
	 * 在玩家周围半径 4 格里随机挑一列，从脚上一格往下找到第一块非空气方块；
	 * 玩家自己那一列（dx = dz = 0）跳过，避免把玩家冻在水里或顶起来。
	 */
	private static BlockPos randomSurface(ServerLevel level, ServerPlayer player) {
		BlockPos origin = player.blockPosition();
		var random = level.getRandom();

		for (int tries = 0; tries < 8; tries++) {
			int dx = random.nextInt(BLIZZARD_RADIUS * 2 + 1) - BLIZZARD_RADIUS;
			int dz = random.nextInt(BLIZZARD_RADIUS * 2 + 1) - BLIZZARD_RADIUS;

			if (dx == 0 && dz == 0) {
				continue;
			}

			int x = origin.getX() + dx;
			int z = origin.getZ() + dz;

			for (int y = origin.getY() + 1; y >= origin.getY() - 4; y--) {
				BlockPos pos = new BlockPos(x, y, z);

				if (!level.getBlockState(pos).isAir()) {
					return pos;
				}
			}
		}

		return null;
	}

	/** 云体大小倍率：连击档越高云团越大（2 档 1 倍、10 档 {@link #BODY_SCALE_MAX} 倍，中间线性）。 */
	private static double bodyScale(int comboLevel) {
		int steps = Math.clamp(comboLevel - 2, 0, BODY_SCALE_LEVELS);
		return 1.0D + steps * (BODY_SCALE_MAX - 1.0D) / BODY_SCALE_LEVELS;
	}

	/** 回复容量：战斗中每 5 秒 5%（可被避雷针放大）、脱战每 0.1 秒 5%（都按上限算，回满为止）。 */
	private static void regen(ServerPlayer player, Cloud cloud, int comboLevel) {
		double max = maxCapacity(player, comboLevel);
		boolean inCombat = CombatState.isInCombat(player);
		int interval = inCombat ? COMBAT_REGEN_INTERVAL : OUT_OF_COMBAT_REGEN_INTERVAL;
		double percent = inCombat
				? COMBAT_REGEN_PERCENT * (1.0D + ArtifactEffects.cloudCombatRegenPercent(player) / 100.0D)
				: OUT_OF_COMBAT_REGEN_PERCENT;

		cloud.capacity = Math.min(max, cloud.capacity);
		cloud.regenTimer++;

		if (cloud.regenTimer >= interval) {
			cloud.regenTimer = 0;
			cloud.capacity = Math.min(max, cloud.capacity + max * percent / 100.0D);
		}
	}

	/** 攻击：只要处于战斗状态就打（挨打或主动打出伤害都算战斗），容量够、附近有威胁生物时出手。 */
	private static void tryStrike(ServerLevel level, ServerPlayer player, Cloud cloud, int comboLevel, Vec3 from) {
		if (cloud.attackCooldown > 0) {
			cloud.attackCooldown--;
			return;
		}

		if (!CombatState.isInCombat(player)) {
			return;
		}

		boolean doubleShot = ArtifactCombo.DARK_CLOUD.cloudDoubleShot(comboLevel);
		int shots = doubleShot ? 2 : 1;

		if (cloud.capacity < shots) {
			return;
		}

		List<LivingEntity> targets = threateningNear(level, player);

		if (targets.isEmpty()) {
			return;
		}

		// 消耗速度：基础 100%，再按攻击速度的百分之多少往上加（乐谱《台风》），
		// 最后加上固定点数（桅杆模型）——两者相加后一起算攻击间隔
		double speed = 100.0D + PlayerStats.attackSpeedTotal(player)
				* ArtifactEffects.cloudSpeedPercent(player) / 100.0D
				+ ArtifactEffects.cloudSpeedBonus(player);
		cloud.attackCooldown = (int) Math.max(1.0D, Math.round(BASE_INTERVAL_TICKS * 100.0D / speed));

		for (int shot = 0; shot < shots; shot++) {
			// 每一下都挑当前最近的目标：第一下打死的话第二下自然换人
			LivingEntity target = threateningNear(level, player).stream().findFirst().orElse(null);

			if (target == null) {
				break;
			}

			// 雷文泥板：这一下有可能不扣容量（每一下单独掷；「打不打得起」在上面按容量判过了）
			if (!(level.getRandom().nextDouble() * 100.0D < ArtifactEffects.cloudFreeShotChance(player))) {
				cloud.capacity -= 1.0D;
			}

			strike(level, player, cloud, target, from);
		}
	}

	/** 附近有威胁的生物：只打玩家 / 敌对 / 正在打玩家的生物，和平生物不误伤（SephiriaDamage.strikeable）。 */
	private static List<LivingEntity> threateningNear(ServerLevel level, ServerPlayer player) {
		return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE),
				candidate -> SephiriaDamage.autoTargetable(candidate, player)).stream()
				.sorted(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(player)))
				.toList();
	}

	/**
	 * 一记攻击：普通乌云是「电元素强度 × {@link #DAMAGE_PERCENT}%」的电属性伤害；
	 * 暴风雪云改成「闪电与冰里较高的一方 × {@link #DAMAGE_PERCENT}% × 110% +
	 * 较低的一方 × {@link #DAMAGE_PERCENT}% × 15%」的两段双属性伤害
	 * ——即两段都先按普通乌云的 20% 折算，再分别乘 110% / 15%。
	 * 云种箭头的强化、雷石的额外伤害、容量 / 消耗速度等其余规则完全不变。
	 *
	 * <p>红宝石胸针：两段里只要有电属性那一段，命中后就 {@link Debuffs#applyBurn}；视觉上在原蓝白
	 * 闪电之外加一层红色火焰。暴风雪云：云下持续飘雪，冰那一段命中时再加一层雪粒子。
	 */
	private static void strike(ServerLevel level, ServerPlayer player, Cloud cloud, LivingEntity target, Vec3 from) {
		boolean blizzard = ArtifactEffects.blizzardCloud(player);
		double multiplier = 1.0D + ArtifactEffects.cloudDamagePercent(player) / 100.0D;
		double normalPercent = DAMAGE_PERCENT / 100.0D;

		// 云种箭头：这一记有概率被强化——伤害翻倍（落点也会炸得更开，一眼能看出是双倍）
		boolean enhanced = level.getRandom().nextDouble() * 100.0D
				< ArtifactEffects.cloudEnhancedChance(player);

		if (enhanced) {
			multiplier *= 2.0D;
		}

		boolean hasElectric;
		boolean hasIce;
		boolean hit = false;

		if (blizzard) {
			double lightning = PlayerStats.elementTotal(player, PlayerStats.Element.LIGHTNING);
			double ice = PlayerStats.elementTotal(player, PlayerStats.Element.ICE);
			boolean lightningHigher = lightning >= ice;
			double higher = Math.max(lightning, ice);
			double lower = Math.min(lightning, ice);

			hasElectric = lightning > 0.0D;
			hasIce = ice > 0.0D;

			// 两段都先按普通乌云的 20% 折算，再分别 ×110% / ×15%。
			// 较高属性那一行走它自己的元素类型；较低属性走另一边的元素类型。
			// 两段各自清无敌帧，所以都会落地；各自也会触发对应元素的减益守门。
			hit |= hit(level, player, target,
					higher * normalPercent * 1.10D * multiplier, lightningHigher);
			hit |= hit(level, player, target,
					lower * normalPercent * 0.15D * multiplier, !lightningHigher);
		}
		else {
			hasElectric = true;
			hasIce = false;

			double damage = PlayerStats.elementTotal(player, PlayerStats.Element.LIGHTNING)
					* normalPercent * multiplier;

			if (damage > 0.0D) {
				hit = hit(level, player, target, damage, true);
			}
		}

		if (!hit) {
			return;
		}

		Vec3 to = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D);
		int steps = (int) Math.ceil(from.distanceTo(to) * 2.0D);

		// 原来的蓝白闪电：只有存在电属性那一段时才画
		if (hasElectric) {
			for (int step = 0; step <= steps; step++) {
				Vec3 point = from.lerp(to, step / (double) Math.max(1, steps));
				level.sendParticles(ModParticleTypes.BLUE_SPARK, point.x, point.y, point.z,
						1, 0.05D, 0.05D, 0.05D, 0.0D);
			}

			if (enhanced) {
				level.sendParticles(ModParticleTypes.BLUE_SPARK, to.x, to.y, to.z, 24, 0.6D, 0.6D, 0.6D, 0.12D);
			}
			else {
				level.sendParticles(ModParticleTypes.BLUE_SPARK, to.x, to.y, to.z, 12, 0.3D, 0.4D, 0.3D, 0.08D);
			}
		}

		// 红宝石胸针：原闪电特效不变，沿同一条线再加一层红色火焰
		if (hasElectric && ArtifactEffects.cloudAppliesBurn(player)) {
			for (int step = 0; step <= steps; step += 2) {
				Vec3 point = from.lerp(to, step / (double) Math.max(1, steps));
				level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z,
						1, 0.06D, 0.06D, 0.06D, 0.0D);
			}

			level.sendParticles(ParticleTypes.FLAME, to.x, to.y, to.z, 8, 0.35D, 0.35D, 0.35D, 0.03D);
		}

		// 暴风雪云：冰那一段命中时再加一层雪
		if (hasIce) {
			for (int step = 0; step <= steps; step += 2) {
				Vec3 point = from.lerp(to, step / (double) Math.max(1, steps));
				level.sendParticles(ModParticleTypes.SNOWFALL, point.x, point.y, point.z,
						1, 0.06D, 0.06D, 0.06D, 0.0D);
			}

			level.sendParticles(ModParticleTypes.SNOWFALL, to.x, to.y, to.z, 10, 0.4D, 0.4D, 0.4D, 0.02D);
		}

		level.playSound(null, from.x, from.y, from.z,
				SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, STRIKE_VOLUME, 1.4F);
	}

	/** 打一段伤害；{@code electric} 决定伤害类型与是否触发红宝石胸针的灼伤。 */
	private static boolean hit(ServerLevel level, ServerPlayer player, LivingEntity target,
			double damage, boolean electric) {
		if (damage <= 0.0D || !target.isAlive()) {
			return false;
		}

		DamageSource source = electric
				? SephiriaDamage.electricAttack(level, player)
				: SephiriaDamage.iceAttack(level, player);
		// 每段都要落地：清掉无敌帧（与触电结算同一套手法）
		SephiriaDamage.clearDamageCooldown(target);
		target.hurtServer(level, source, (float) damage);

		if (electric && ArtifactEffects.cloudAppliesBurn(player) && target.isAlive()) {
			Debuffs.applyBurn(target, player);
		}

		return true;
	}
}
