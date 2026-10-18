package com.sephiria.trader;

import com.sephiria.registry.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 赛菲莉娅流浪商人的刷怪器：**机制照原版 {@code WanderingTraderSpawner} 抄一份，但完全独立**
 * ——原版那个照旧刷原版商人，这个刷我们的（用户 2026-10-17 的口径：两个都刷、刷新各自独立）。
 *
 * <p>逐条对齐原版的部分：每 {@value #TICK_DELAY} 刻检查一次；每次检查把出生延迟减 1200，
 * 减到 0 才真正动手（并重置回 {@value #SPAWN_DELAY} 刻 = 4 分钟）；
 * 几率从 {@value #MIN_SPAWN_CHANCE}% 起、每次 +{@value #SPAWN_CHANCE_INCREASE}%、封顶
 * {@value #MAX_SPAWN_CHANCE}%（真刷出来了就重置回最小值）；过了几率还有 1/{@value #SPAWN_ONE_IN_X}；
 * 落点优先挑玩家 {@value #SPAWN_RADIUS} 格内的**村庄集会点**、挑不到就用玩家脚下的位置，
 * 周围最多试 {@value #SPAWN_ATTEMPTS} 次找一处站得下的地方；刷出 1 个商人 +
 * {@value #LLAMAS} 只随从羊驼，然后设 {@value #DESPAWN_DELAY} 刻（40 分钟）的离场倒计时、
 * 把集会点设成它游荡的"家"。受 {@code spawn_wandering_traders} 游戏规则控制（与原版一致）。
 *
 * <p>与原版唯一的数值差异：**出生延迟改成 4800 刻**（原版 24000 = 20 分钟；用户 2026-10-17
 * 要求改短，几率爬升与 1/10 判定不变——所以实际间隔仍不是固定的，平均下来短了很多）。
 *
 * <p>计时器不写存档：原版把延迟与几率存在 {@code WanderingTraderData} 里，
 * 这里记在内存——服务端重启后从「刚进新世界的状态」重来（延迟 4 分钟、几率 25%），
 * 与原版的差别只出现在重启那一刻，A 测阶段接受。
 *
 * <p>不碰原版的刷怪器还有一个好处：升级 Minecraft 时不会因为原版私有方法改了签名而在启动时报错。
 */
public final class SephiriaTraderSpawner {
	/** 每多少刻检查一次（原版默认 tickDelay）。 */
	private static final int TICK_DELAY = 1200;
	/**
	 * 每次尝试后重置的出生延迟：4800 刻 = 4 分钟（原版 24000 = 20 分钟；
	 * 用户 2026-10-17 要求改成这个数，几率爬升与 1/10 判定不变）。
	 */
	private static final int SPAWN_DELAY = 4800;
	/** 出生几率：下限、上限、每次增量（原版三个常量）。 */
	private static final int MIN_SPAWN_CHANCE = 25;
	private static final int MAX_SPAWN_CHANCE = 75;
	private static final int SPAWN_CHANCE_INCREASE = 25;
	/** 过了几率判定之后再 1/{@value}（原版 SPAWN_ONE_IN_X_CHANCE）。 */
	private static final int SPAWN_ONE_IN_X = 10;
	/** 找位置：最多试几次、以锚点为中心的范围（原版 10 次 / 48 格）。 */
	private static final int SPAWN_ATTEMPTS = 10;
	private static final int SPAWN_RADIUS = 48;
	/** 随从羊驼：几只、每只在商人周围多大范围里找位置（原版 2 只 / 4 格）。 */
	private static final int LLAMAS = 2;
	private static final int LLAMA_RADIUS = 4;
	/** 出生后的离场倒计时（原版 48000 刻）与"家"的半径（原版 16）。 */
	private static final int DESPAWN_DELAY = 48000;
	private static final int HOME_RADIUS = 16;

	/** 主世界那一份的状态（见类注释：不写存档）。 */
	private static SephiriaTraderSpawner instance;
	private static ServerLevel instanceLevel;

	private final RandomSource random = RandomSource.create();
	private int tickDelay = TICK_DELAY;
	private int spawnDelay = SPAWN_DELAY;
	private int spawnChance = MIN_SPAWN_CHANCE;

	/**
	 * 挂到服务端每刻上：推进主世界那一份刷怪器。
	 *
	 * <p>调用频率与原版自定义刷怪器一致——{@code ServerChunkCache} 每刻都会调
	 * {@code ServerLevel.tickCustomSpawners}（没有额外的刻数门控），所以挂 END_SERVER_TICK
	 * 与把刷怪器塞进原版那张列表里等价，还不用去动原版的私有字段（那张列表是不可变的）。
	 */
	public static void registerTicker() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ServerLevel overworld = server.overworld();

			if (overworld == null) {
				return;
			}

			// 服务端重启会换一个 ServerLevel 实例：那一份状态跟着换成新的（= 新世界的初始状态）
			if (instance == null || instanceLevel != overworld) {
				instance = new SephiriaTraderSpawner();
				instanceLevel = overworld;
			}

			instance.tick(overworld);
		});
	}

	/** 每一刻推进一格：延迟 → 几率 → 1/10 → 找位置刷出来（顺序与原版 tick 完全一致）。 */
	private void tick(ServerLevel level) {
		if (!level.getGameRules().get(GameRules.SPAWN_WANDERING_TRADERS)) {
			return;
		}

		if (--this.tickDelay > 0) {
			return;
		}

		this.tickDelay = TICK_DELAY;

		if ((this.spawnDelay -= TICK_DELAY) > 0) {
			return;
		}

		this.spawnDelay = SPAWN_DELAY;
		// 这一次用旧几率判定，判定完之后才涨（原版就是这么排的）
		int chance = this.spawnChance;
		this.spawnChance = Mth.clamp(chance + SPAWN_CHANCE_INCREASE, MIN_SPAWN_CHANCE, MAX_SPAWN_CHANCE);

		if (this.random.nextInt(100) > chance) {
			return;
		}

		if (this.spawn(level)) {
			this.spawnChance = MIN_SPAWN_CHANCE;
		}
	}

	/** 真正的出生流程；返回是否「这一轮处理完了」（决定要不要把几率重置）。 */
	private boolean spawn(ServerLevel level) {
		ServerPlayer player = level.getRandomPlayer();

		// 原版：附近没有玩家也算处理完了（延迟已经重置，下次再来）
		if (player == null) {
			return true;
		}

		if (this.random.nextInt(SPAWN_ONE_IN_X) != 0) {
			return false;
		}

		BlockPos anchor = level.getPoiManager()
				.find(type -> type.is(PoiTypes.MEETING), pos -> true, player.blockPosition(), SPAWN_RADIUS,
						PoiManager.Occupancy.ANY)
				.orElse(player.blockPosition());
		BlockPos pos = findSpawnPositionNear(level, anchor, SPAWN_RADIUS);

		if (pos == null || !hasEnoughSpace(level, pos)) {
			return false;
		}

		if (level.getBiome(pos).is(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS)) {
			return false;
		}

		SephiriaWanderingTrader trader = ModEntities.SEPHIRIA_WANDERING_TRADER.spawn(level, pos,
				EntitySpawnReason.EVENT);

		if (trader == null) {
			return false;
		}

		for (int llama = 0; llama < LLAMAS; llama++) {
			tryToSpawnLlamaFor(level, trader);
		}

		trader.setDespawnDelay(DESPAWN_DELAY);
		trader.setWanderTarget(anchor);
		trader.setHomeTo(anchor, HOME_RADIUS);
		return true;
	}

	/** 给商人配一只随从羊驼（在商人周围 {@value #LLAMA_RADIUS} 格里找位置，牵到商人身上）。 */
	private void tryToSpawnLlamaFor(ServerLevel level, SephiriaWanderingTrader trader) {
		BlockPos pos = findSpawnPositionNear(level, trader.blockPosition(), LLAMA_RADIUS);

		if (pos == null) {
			return;
		}

		SephiriaTraderLlama llama = ModEntities.SEPHIRIA_TRADER_LLAMA.spawn(level, pos, EntitySpawnReason.EVENT);

		if (llama != null) {
			llama.setLeashedTo(trader, true);
		}
	}

	/**
	 * 在锚点周围随机试几处，挑第一处「站得下」的位置。
	 *
	 * <p>高度取地表（原版流浪商人用的也是 MOTION_BLOCKING_NO_LEAVES 这张高度图），
	 * 再按 ON_GROUND 的落点规则判一次——我们的实体类型没在原版 {@code SpawnPlacements} 里注册
	 * （那个 register 是私有的），所以这里直接拿规则判，效果与注册过一样。
	 */
	private BlockPos findSpawnPositionNear(ServerLevel level, BlockPos center, int radius) {
		BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();

		for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
			int x = center.getX() + this.random.nextInt(radius * 2 + 1) - radius;
			int z = center.getZ() + this.random.nextInt(radius * 2 + 1) - radius;
			candidate.set(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);

			if (SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, candidate,
					ModEntities.SEPHIRIA_WANDERING_TRADER)) {
				return candidate.immutable();
			}
		}

		return null;
	}

	/** 落点要站得下：原版检查的是 (0..1, 0..2, 0..1) 这个 1×2×1 的碰撞箱全空。 */
	private static boolean hasEnoughSpace(ServerLevel level, BlockPos pos) {
		for (int x = 0; x <= 1; x++) {
			for (int y = 0; y <= 2; y++) {
				for (int z = 0; z <= 1; z++) {
					BlockPos check = pos.offset(x, y, z);

					if (!level.getBlockState(check).getCollisionShape(level, check).isEmpty()) {
						return false;
					}
				}
			}
		}

		return true;
	}
}
