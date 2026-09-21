package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 金色斗篷（风之歌，高级品质）：【唯一】冲刺次数 +1/1/2、攻击速度 +3/6/9%（0 / 1 / 2 级）。 */
public class GoldenCloakItem extends ArtifactItem {
	private static final int[] CHARGES_BY_LEVEL = { 1, 1, 2 };
	private static final double[] ATTACK_SPEED_BY_LEVEL = { 3.0D, 6.0D, 9.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public GoldenCloakItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.WIND_SONG;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.golden_cloak.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CHARGES_BY_LEVEL.length - 1;
	}

	@Override
	public int dashChargesBonus(int level) {
		// 词条是「+N 次」这种整数，取值前先夹到 0..上限（负数等级的神器不参与统计，这里只是保险）
		return CHARGES_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, CHARGES_BY_LEVEL.length - 1)];
	}

	@Override
	public double attackSpeedBonus(int level) {
		return valueAt(ATTACK_SPEED_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		// 数值本身不带「+」——加号在语言文件里，和别的词条保持一致
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.dash_charges",
						Component.literal(Numbers.format(this.dashChargesBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria.affix.attack_speed",
						Component.literal(Numbers.format(this.attackSpeedBonus(level))).withColor(COLOUR_BONUS)));
	}
}
