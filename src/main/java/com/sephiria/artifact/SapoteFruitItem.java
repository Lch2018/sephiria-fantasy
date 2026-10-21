package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 沙波特果实（乌云，高级品质）：乌云容量 +4/8/12/16、MP再生 +10/20/30/40（0..3 级，每 10 秒）。
 *
 * <p>没有【唯一】：带几件就叠几份。「乌云容量」加在基础容量 15 与连击档位之上
 * （见 {@code cloud.DarkCloud#maxCapacity}）；「MP再生」并进 MP 再生总值
 * （战斗每 10 秒结算，见 {@code PlayerStats#mpRegenTotal}，不写回存档）。
 */
public class SapoteFruitItem extends ArtifactItem {
	/** 各等级的乌云容量加成（点）。 */
	private static final int[] CAPACITY_BY_LEVEL = { 4, 8, 12, 16 };
	/** 各等级的 MP 再生加成（点/秒）；显示与结算时 ×10 成「每 10 秒」。 */
	private static final double[] MP_REGEN_BY_LEVEL = { 1.0D, 2.0D, 3.0D, 4.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SapoteFruitItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.DARK_CLOUD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.sapote_fruit.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return CAPACITY_BY_LEVEL.length - 1;
	}

	@Override
	public int cloudCapacityBonus(int level) {
		return CAPACITY_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, CAPACITY_BY_LEVEL.length - 1)];
	}

	@Override
	public double mpRegenBonus(int level) {
		return valueAt(MP_REGEN_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.cloud_capacity",
						Component.literal(Numbers.format(this.cloudCapacityBonus(level))).withColor(COLOUR_BONUS)),
				// 属性页的 MP再生 是「每 10 秒」的值，词条也按同一口径显示（内部每秒值 ×10）
				Component.translatable("artifact.sephiria_fantasy.affix.mp_regen",
						Component.literal(Numbers.format(this.mpRegenBonus(level) * 10.0D)).withColor(COLOUR_BONUS)));
	}
}
