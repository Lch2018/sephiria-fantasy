package com.sephiria.artifact.skill;

/**
 * 技能提示框的「实际数值」上下文：魔法书的冷却与蓝耗在提示框里要显示结算后的值，
 * 而不是技能表里的基础值。
 *
 * <p>客户端每次画提示框前设置、画完清除；服务端不会走进这里（{@code skillCooldown} /
 * {@code skillMp} 只在客户端渲染提示框时调用），所以这些静态字段不需要同步。
 *
 * <p>冷却的全局加速来自属性同步包（{@code StatsSyncPayload.magicBookHaste}）；
 * 蓝耗减少是「按书所在格子」算的，由提示框按悬停格现算。技能页拿到的是服务端算好的
 * 精确值，用 {@link #setActual} 直接覆盖。
 */
public final class SkillTooltipContext {
	/** 魔法书加速（点）：每 1 点让冷却速度 +1%。 */
	private static double hastePercent;
	/** 当前这本书的额外加速（发光的沙漏）；-1 = 只用全局加速。 */
	private static double bookHastePercent = -1.0D;
	/** 当前这本书吃到的 MP 消耗减少（%）。 */
	private static double mpReductionPercent;
	/** 当前这本书的 MP 消耗增加（%）：双角管。 */
	private static double mpCostIncreasePercent;
	/** 技能页用的精确冷却（tick）；-1 = 用基础值 + 加速现算。 */
	private static int cooldownTicksOverride = -1;
	/** 技能页用的精确蓝耗；-1 = 用基础值 + 减耗现算。 */
	private static double mpCostOverride = -1.0D;

	private SkillTooltipContext() {
	}

	/** 属性同步时更新全局魔法书加速。 */
	public static void setHastePercent(double percent) {
		hastePercent = Math.max(0.0D, percent);
	}

	/** 画某本书的提示框前设置它吃到的 MP 减耗。 */
	public static void setMpReductionPercent(double percent) {
		mpReductionPercent = Math.max(0.0D, percent);
	}

	/** 画某本书的提示框前设置它的额外加速（发光的沙漏）。 */
	public static void setBookHastePercent(double percent) {
		bookHastePercent = Math.max(0.0D, percent);
	}

	/** 画魔弹的提示框前设置双角管的 MP 消耗增加。 */
	public static void setMpCostIncreasePercent(double percent) {
		mpCostIncreasePercent = Math.max(0.0D, percent);
	}

	/** 技能页：服务端算好的精确冷却与蓝耗。 */
	public static void setActual(int cooldownTicks, double mpCost) {
		cooldownTicksOverride = Math.max(0, cooldownTicks);
		mpCostOverride = Math.max(0.0D, mpCost);
	}

	/** 画完一本魔法书的提示框后清掉按书的那部分（全局加速保留）。 */
	public static void clearBook() {
		bookHastePercent = -1.0D;
		mpReductionPercent = 0.0D;
		mpCostIncreasePercent = 0.0D;
		cooldownTicksOverride = -1;
		mpCostOverride = -1.0D;
	}

	/** 实际冷却（秒）：精确值优先，否则基础秒数按加速缩短。 */
	public static double cooldownSeconds(int fallbackSeconds) {
		double ticks = cooldownTicksOverride >= 0
				? cooldownTicksOverride
				: fallbackSeconds * 20.0D;

		if (cooldownTicksOverride < 0) {
			double haste = bookHastePercent >= 0.0D ? bookHastePercent : hastePercent;

			if (haste > 0.0D) {
				ticks = Math.max(1.0D, Math.round(ticks / (1.0D + haste / 100.0D)));
			}
		}

		return ticks / 20.0D;
	}

	/** 实际蓝耗：精确值优先，否则基础值按减耗缩小、再按双角管增加。 */
	public static double mpCost(double fallback) {
		if (mpCostOverride >= 0.0D) {
			return mpCostOverride;
		}

		double cost = mpReductionPercent > 0.0D
				? Math.max(0.0D, fallback * (1.0D - mpReductionPercent / 100.0D))
				: fallback;
		return mpCostIncreasePercent > 0.0D
				? cost * (1.0D + mpCostIncreasePercent / 100.0D)
				: cost;
	}
}
