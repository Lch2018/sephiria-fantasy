package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 红宝石胸针（乌云 + 余烬，羁绊品质）：【唯一】乌云的闪电会给击中的敌人施加灼伤；
 * 闪电属性伤害 +3/5/7/10、火焰属性伤害 +3/5/7/10（0..3 级）。
 *
 * <p><b>双连击</b>神器：同时给乌云与余烬各加 1 级（见 {@link #combos()}）。
 *
 * <p>「闪电施加灼伤」走 {@code Debuffs.applyBurn}，与余烬的「火焰之触」不是同一入口：
 * 不需要余烬 2 档、也没有那 2 秒冷却，每次乌云雷击命中都会尝试挂一层（层数上限照旧）。
 * 视觉上除了原来的蓝白闪电，还会沿同一条线加一层红色火焰特效（原特效保留）。
 */
public class RubyBroochItem extends ArtifactItem {
	/** 各等级闪电 / 火焰属性伤害加成（点）。 */
	private static final double[] ELEMENT_BY_LEVEL = { 3.0D, 5.0D, 7.0D, 10.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public RubyBroochItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.DARK_CLOUD;
	}

	/** 双连击：乌云与余烬各 +1 级。 */
	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.DARK_CLOUD, ArtifactCombo.EMBER);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.ruby_brooch.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return ELEMENT_BY_LEVEL.length - 1;
	}

	@Override
	public double lightningElementBonus(int level) {
		return valueAt(ELEMENT_BY_LEVEL, level);
	}

	@Override
	public double fireElementBonus(int level) {
		return valueAt(ELEMENT_BY_LEVEL, level);
	}

	/** 固定效果：乌云的闪电命中就尝试施加灼伤。 */
	@Override
	public boolean cloudAppliesBurn(int level) {
		return true;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		Component value = Component.literal(Numbers.format(valueAt(ELEMENT_BY_LEVEL, level)))
				.withColor(COLOUR_BONUS);

		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.cloud_burn"),
				Component.translatable("artifact.sephiria_fantasy.affix.lightning_element", value),
				Component.translatable("artifact.sephiria_fantasy.affix.fire_element", value));
	}
}
