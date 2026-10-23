package com.sephiria.difficulty;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * 「根之退」里的一项难度选项：等级越高越难，同时提供越多「阶段」（= 分数）。
 *
 * <p>等级 0 = 未激活（默认），每一项互不影响，玩家可以在界面上单独加 / 减。
 * 加新选项照抄 {@link #FIERCE_CLAWS} 那一行即可：两张表都是「每级一格」的数组，
 * 长度 - 1 就是这一项的最高等级；名字 / 效果行的语言键按 id 拼
 * （{@code screen.sephiria_fantasy.root_retreat.option.<id>} 与 {@code ...effect.<id>}）。
 *
 * <p><b>阶段</b>（= 分数）是「你把自己绑得多紧」的度量：界面最上方显示所有选项当前提供的
 * 阶段之和。用户的口径是「不同选项或等级提供的层数也不一样」，所以阶段单独一张表
 * （例如防护铠甲 1/2/4/5/6 就不是按等级线性给的）。
 *
 * <p><b>数值一律用 double</b>：顽强之躯最后一级是 87.5%，整数表存不下。
 */
public enum DifficultyOption {
	/**
	 * 凶猛的爪子：敌人对你的伤害增加 10/20/30/40/50/60/70/80%（1..8 级）。
	 *
	 * <p>效果落在 {@code RootRetreat#incomingDamageMultiplier}：只放大<b>生物打到自己身上</b>
	 * 的那一下（玩家打的、环境伤害都不算），而且只看自己的等级。
	 */
	FIERCE_CLAWS("fierce_claws",
			new double[] { 0.0D, 10.0D, 20.0D, 30.0D, 40.0D, 50.0D, 60.0D, 70.0D, 80.0D },
			new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 },
			null),

	/**
	 * 顽强之躯：敌人最终受到的伤害减少 20/40/50/65/75/80/85/87.5/90%（1..9 级）。
	 *
	 * <p>这是<b>最底层</b>的减免（用户口径）——它作用在自己打出去的伤害的最后一步，
	 * 所以连「无视防御伤害」（真实伤害）也照减。
	 */
	TENACIOUS_BODY("tenacious_body",
			new double[] { 0.0D, 20.0D, 40.0D, 50.0D, 65.0D, 75.0D, 80.0D, 85.0D, 87.5D, 90.0D },
			new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 10 },
			"note.tenacious_body"),

	/**
	 * 防护铠甲：敌人的防御力 +30/50/70/100/150（1..5 级，用户 2026-10-23 调整）。
	 *
	 * <p>防御力按 {@code x / (x + 80)} 折算成减伤（与玩家防御力同一条公式，x = 80 时减 50%），
	 * 作用在自己打出去的伤害上；<b>它减不了真实伤害</b>（用户口径）——真实伤害绕开的正是
	 * 这一类「防御」。
	 */
	PROTECTIVE_ARMOR("protective_armor",
			new double[] { 0.0D, 30.0D, 50.0D, 70.0D, 100.0D, 150.0D },
			new int[] { 0, 1, 2, 4, 5, 6 },
			"note.protective_armor"),

	/**
	 * 衰退：所有生命恢复效果降低 5/10/20/30/40/50%（1..6 级，阶段 1..6）。
	 *
	 * <p>结算在 {@link RootRetreat#healingMultiplier}，挂在 {@code LivingEntity#heal} 上——
	 * 自然回复、再生药水、瞬间治疗、金苹果等所有走 heal 的恢复都会被削。
	 */
	DECAY("decay",
			new double[] { 0.0D, 5.0D, 10.0D, 20.0D, 30.0D, 40.0D, 50.0D },
			new int[] { 0, 1, 2, 3, 4, 5, 6 },
			null),

	/**
	 * 紧张：战斗状态下生命值无法恢复（1 级，阶段 4）。
	 *
	 * <p>用户口径是「层数 4」——这一项只有一级，但一次给 4 层阶段。
	 */
	TENSION("tension",
			new double[] { 0.0D, 100.0D },
			new int[] { 0, 4 },
			"note.tension"),

	/**
	 * 心之重担：死亡时背包物品消失 20/40/60/80/100%（1..5 级，阶段 2/4/6/8/10）。
	 *
	 * <p>结算在 {@link RootRetreat#applyDeathBurden}，由 {@code ServerPlayer#die} 的 mixin 在
	 * 死亡掉落之前调用；赛菲莉娅背包（附件）也按同一个概率消失。
	 */
	HEART_BURDEN("heart_burden",
			new double[] { 0.0D, 20.0D, 40.0D, 60.0D, 80.0D, 100.0D },
			new int[] { 0, 2, 4, 6, 8, 10 },
			"note.heart_burden");

	/**
	 * 效果行里数值的颜色：<b>红色</b>。
	 *
	 * <p>用户 2026-10-14 定的口径：难度选项对玩家来说都是负面效果，所以这些数值一律红色
	 * （原来是跟神器词条一样的绿色）；以后凡是「对玩家不利」的数值都用这个色。
	 */
	public static final int COLOUR_VALUE = 0xFFFF5555;
	/** 「解除了 x 层束缚」里那截字（含「层」）的颜色：也是红色（用户指定）。 */
	public static final int COLOUR_BOUND = 0xFFFF5555;
	/** 机制说明行（第三行）的颜色：灰色。 */
	private static final int COLOUR_NOTE = 0xFF9A9AA8;

	private final String id;
	/** 每一级的数值（凶猛的爪子 = 敌人伤害加成 %、防护铠甲 = 防御力点数……）。 */
	private final double[] effectByLevel;
	/** 每一级提供的阶段（层）。 */
	private final int[] stagesByLevel;
	/** 可选的第三行「机制说明」的语言键后缀（没有就是 null）。 */
	private final String noteKey;

	DifficultyOption(String id, double[] effectByLevel, int[] stagesByLevel, String noteKey) {
		if (effectByLevel.length != stagesByLevel.length || effectByLevel.length < 2) {
			throw new IllegalArgumentException(id + ": effect/stage tables must have the same length >= 2");
		}

		this.id = id;
		this.effectByLevel = effectByLevel;
		this.stagesByLevel = stagesByLevel;
		this.noteKey = noteKey;
	}

	public String id() {
		return this.id;
	}

	/** 最高等级（等级 0 是「未激活」，所以最高等级 = 表长 - 1）。 */
	public int maxLevel() {
		return this.effectByLevel.length - 1;
	}

	/** 这一级的数值（凶猛的爪子 = 敌人伤害加成 %）。 */
	public double effectAt(int level) {
		return this.effectByLevel[Mth.clamp(level, 0, this.maxLevel())];
	}

	/** 这一级提供的阶段（层）。 */
	public int stagesAt(int level) {
		return this.stagesByLevel[Mth.clamp(level, 0, this.maxLevel())];
	}

	/**
	 * 名字（界面里的那一行）。
	 *
	 * <p>不能叫 {@code name()}：{@link Enum#name()} 是 final 的，覆盖不了。
	 */
	public Component label() {
		return Component.translatable(nameKey());
	}

	/** 名字的语言键。 */
	public String nameKey() {
		return "screen.sephiria_fantasy.root_retreat.option." + this.id;
	}

	/**
	 * 鼠标悬停时的说明行：
	 * <ol>
	 *   <li>这一级的效果（数值<b>红色</b>——难度选项对玩家都是负面效果）；</li>
	 *   <li>「解除了 3 层束缚」——其中「3层」也是红色（用户指定）；</li>
	 *   <li>有机制需要点明的选项再补一行灰色说明（例如「包含真实伤害减免」）。</li>
	 * </ol>
	 */
	public List<Component> tooltip(int level) {
		int clamped = Mth.clamp(level, 0, this.maxLevel());
		Component layers = Component.translatable("screen.sephiria_fantasy.root_retreat.layers",
				Numbers.format(this.stagesAt(clamped))).withColor(COLOUR_BOUND);
		List<Component> lines = new ArrayList<>(3);
		lines.add(Component.translatable("screen.sephiria_fantasy.root_retreat.effect." + this.id,
				Component.literal(Numbers.format(this.effectAt(clamped))).withColor(COLOUR_VALUE)));
		lines.add(Component.translatable("screen.sephiria_fantasy.root_retreat.bound", layers));

		if (this.noteKey != null) {
			lines.add(Component.translatable("screen.sephiria_fantasy.root_retreat." + this.noteKey)
					.withColor(COLOUR_NOTE));
		}

		return lines;
	}

	/** 这一级的「阶段层数」文字（「3层」），右侧「你的方案」那一列用。 */
	public Component stageLine(int level) {
		return Component.translatable("screen.sephiria_fantasy.root_retreat.layers",
				Numbers.format(this.stagesAt(level)));
	}
}
