package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 无色立方体（风之歌，稀有品质）：无视防御伤害 +1/1/2/2/3、移动速度 +5/6/7/9/12%、
 * 攻击速度 +5/6/7/9/12%（0..4 级）。
 *
 * <p><b>没有【唯一】</b>：带几个就叠几份，风之歌连击也会跟着多算几级。
 */
public class ColorlessCubeItem extends ArtifactItem {
	/** 各等级的无视防御伤害（点）。 */
	private static final double[] IGNORE_DEFENSE_BY_LEVEL = { 1.0D, 1.0D, 2.0D, 2.0D, 3.0D };
	/** 各等级的移动速度加成（百分点）。 */
	private static final double[] MOVE_SPEED_BY_LEVEL = { 5.0D, 6.0D, 7.0D, 9.0D, 12.0D };
	/** 各等级的攻击速度加成（百分点）。 */
	private static final double[] ATTACK_SPEED_BY_LEVEL = { 5.0D, 6.0D, 7.0D, 9.0D, 12.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ColorlessCubeItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.WIND_SONG;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.colorless_cube.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return IGNORE_DEFENSE_BY_LEVEL.length - 1;
	}

	@Override
	public double ignoreDefenseBonus(int level) {
		return valueAt(IGNORE_DEFENSE_BY_LEVEL, level);
	}

	@Override
	public double moveSpeedPercentBonus(int level) {
		return valueAt(MOVE_SPEED_BY_LEVEL, level);
	}

	@Override
	public double attackSpeedBonus(int level) {
		return valueAt(ATTACK_SPEED_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.ignore_defense",
						Component.literal(Numbers.format(this.ignoreDefenseBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.move_speed",
						Component.literal(Numbers.format(this.moveSpeedPercentBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.attack_speed",
						Component.literal(Numbers.format(this.attackSpeedBonus(level))).withColor(COLOUR_BONUS)));
	}
}
