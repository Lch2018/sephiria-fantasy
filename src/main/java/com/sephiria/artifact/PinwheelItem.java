package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 风车（风之歌 + 影子，高级品质）：【唯一】攻击速度 +3/6/9%、闪避 +2/4/6（0..2 级）。
 *
 * <p><b>双连击神器</b>（第一个）：同时给风之歌与影子各加 1 级——见 {@link #combos()}。
 * 设计上「羁绊」品质的神器都是双连击，但双连击不限于羁绊品质。
 */
public class PinwheelItem extends ArtifactItem {
	/** 各等级的攻击速度加成（百分点）。 */
	private static final double[] ATTACK_SPEED_BY_LEVEL = { 3.0D, 6.0D, 9.0D };
	/** 各等级的闪避（点）。 */
	private static final double[] DODGE_BY_LEVEL = { 2.0D, 4.0D, 6.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public PinwheelItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.WIND_SONG;
	}

	/** 双连击：风之歌与影子各 +1 级。 */
	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.WIND_SONG, ArtifactCombo.SHADOW);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.pinwheel.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return ATTACK_SPEED_BY_LEVEL.length - 1;
	}

	@Override
	public double attackSpeedBonus(int level) {
		return valueAt(ATTACK_SPEED_BY_LEVEL, level);
	}

	@Override
	public double dodgeBonus(int level) {
		return valueAt(DODGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.attack_speed",
						Component.literal(Numbers.format(this.attackSpeedBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria.affix.dodge",
						Component.literal(Numbers.format(this.dodgeBonus(level))).withColor(COLOUR_BONUS)));
	}
}
