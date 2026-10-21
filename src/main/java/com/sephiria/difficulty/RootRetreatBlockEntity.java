package com.sephiria.difficulty;

import com.sephiria.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * 根之退方块上存着的<b>方案</b>（草稿）：每项难度选项一个等级。
 *
 * <p>用户的口径：放两个根之退 a 和 b，可以给 a 设方案 1、给 b 设方案 2——界面上改的是<b>方块这份</b>，
 * 点「应用」才把它拷到玩家身上（{@link RootRetreat} 那份 = 真正生效的）；不点应用就还是玩家原来的方案。
 * 所以这份数据必须<b>跟着方块存</b>（方块实体进存档），而不是只存在界面里。
 *
 * <p>等级在这里就夹好范围（存档被改过、或者以后表变短了也不会越界）。
 */
public class RootRetreatBlockEntity extends BlockEntity {
	/** 放置时间达到一个游戏日（用户口径：24000 刻）就可以交互。 */
	public static final long ACTIVATION_TICKS = 24000L;
	/** 出生点附近多少格内可以直接交互。 */
	public static final double SPAWN_RANGE = 100.0D;
	/** 存档键。 */
	private static final String KEY_LEVELS = "levels";
	private static final String KEY_PLACED_AT = "placed_at";

	/** 这一份方案的等级表（按 {@link DifficultyOption#ordinal()} 排列）。 */
	private int[] levels = new int[DifficultyOption.values().length];
	/** 放置时的游戏刻；老存档 / 指令放置没有这个值时按 0（世界时间早就过了 24000，视为早已放好）。 */
	private long placedAt;

	public RootRetreatBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ROOT_RETREAT, pos, state);
	}

	/** 某一项在这份方案里的等级。 */
	public int levelOf(DifficultyOption option) {
		int index = option.ordinal();
		return index < this.levels.length ? Mth.clamp(this.levels[index], 0, option.maxLevel()) : 0;
	}

	/** 改某一项（超范围夹住），返回改完的实际等级。 */
	public int setLevel(DifficultyOption option, int level) {
		int clamped = Mth.clamp(level, 0, option.maxLevel());

		if (option.ordinal() < this.levels.length) {
			this.levels[option.ordinal()] = clamped;
			this.setChanged();
		}

		return clamped;
	}

	/** 清空：所有项回到 0 级（界面上的「清空」）。 */
	public void clear() {
		java.util.Arrays.fill(this.levels, 0);
		this.setChanged();
	}

	/** 这份方案的总阶段。 */
	public int stage() {
		int total = 0;

		for (DifficultyOption option : DifficultyOption.values()) {
			total += option.stagesAt(this.levelOf(option));
		}

		return total;
	}

	/** 拷一份等级表（「应用」时写进玩家那份）。 */
	public int[] levels() {
		return this.levels.clone();
	}

	/** 记下放置时的游戏刻（方块被放下时由 {@link RootRetreatBlock#setPlacedBy} 调用）。 */
	public void setPlacedAt(long gameTime) {
		this.placedAt = gameTime;
		this.setChanged();
	}

	/** 这个根之退现在能不能交互：放够一个游戏日，或者在出生点 100 格内（任意一条）。 */
	public boolean isActive(ServerLevel level) {
		if (level.getGameTime() - this.placedAt >= ACTIVATION_TICKS) {
			return true;
		}

		BlockPos spawn = level.getRespawnData().pos();
		return Vec3.atCenterOf(this.getBlockPos()).distanceToSqr(Vec3.atCenterOf(spawn))
				<= SPAWN_RANGE * SPAWN_RANGE;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putIntArray(KEY_LEVELS, this.levels);
		output.putLong(KEY_PLACED_AT, this.placedAt);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.levels = clampAll(input.getIntArray(KEY_LEVELS).orElse(new int[0]));
		// 老存档没有这个键 → 0（世界时间早就超过 24000，等同「早已放好」）
		this.placedAt = input.getLongOr(KEY_PLACED_AT, 0L);
	}

	/** 把读回来的数组规整成「枚举长度、逐项夹住」的样子（存档里的数组可能长短不一）。 */
	private static int[] clampAll(int[] raw) {
		DifficultyOption[] options = DifficultyOption.values();
		int[] levels = new int[options.length];

		for (int index = 0; index < levels.length && index < raw.length; index++) {
			levels[index] = Mth.clamp(raw[index], 0, options[index].maxLevel());
		}

		return levels;
	}
}
