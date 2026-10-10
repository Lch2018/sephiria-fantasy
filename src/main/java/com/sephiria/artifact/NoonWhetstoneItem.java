package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 正午磨刀石（太阳剑，传说品质）：【唯一】造成武器伤害时太阳剑额外触发 0/1/1/1/2 次、
 * 攻击速度 +3/6/9/12/15%、无视防御伤害 +1/1/2/2/2（0..5 级）。
 *
 * <p>额外触发在 {@code sun.SunSword#register} 的伤害回调里连着投；攻速与无视防御伤害
 * 分别走 {@code attackSpeedBonus} / {@code ignoreDefenseBonus} 两个通用钩子。
 */
public class NoonWhetstoneItem extends ArtifactItem {
	/** 各等级的额外触发次数（次）。 */
	private static final int[] EXTRA_BY_LEVEL = { 0, 1, 1, 1, 2 };
	/** 各等级的攻击速度（%）。 */
	private static final double[] ATTACK_SPEED_BY_LEVEL = { 3D, 6D, 9D, 12D, 15D };
	/** 各等级的无视防御伤害（点）。 */
	private static final double[] IGNORE_DEFENSE_BY_LEVEL = { 1D, 1D, 2D, 2D, 2D };

	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public NoonWhetstoneItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SUN_SWORD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.noon_whetstone.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return EXTRA_BY_LEVEL.length - 1;
	}

	@Override
	public int sunSwordExtraTriggers(int level) {
		return EXTRA_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, EXTRA_BY_LEVEL.length - 1)];
	}

	@Override
	public double attackSpeedBonus(int level) {
		return valueAt(ATTACK_SPEED_BY_LEVEL, level);
	}

	@Override
	public double ignoreDefenseBonus(int level) {
		return valueAt(IGNORE_DEFENSE_BY_LEVEL, level);
	}


	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.sun_sword_extra",
						Component.literal(Numbers.format(this.sunSwordExtraTriggers(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.attack_speed",
						Component.literal(Numbers.format(this.attackSpeedBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.ignore_defense",
						Component.literal(Numbers.format(this.ignoreDefenseBonus(level))).withColor(COLOUR_BONUS)));
	}
}
