package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 战士的证明（坚固，高级品质）：【唯一】物理伤害 +1/2/3/4/6/8、攻击速度 +5/5/5/10/10/10%（0..5 级）。
 *
 * <p>攻击速度是属性系统里的「攻击速度」百分点（100 = 100%），所以 +5% 就是 +5 点。
 */
public class WarriorsProofItem extends ArtifactItem {
	/** 各等级的物理强度加成。 */
	private static final double[] PHYSICAL_BY_LEVEL = { 1.0D, 2.0D, 3.0D, 4.0D, 6.0D, 8.0D };
	/** 各等级的攻击速度加成（百分点）。 */
	private static final double[] ATTACK_SPEED_BY_LEVEL = { 5.0D, 5.0D, 5.0D, 10.0D, 10.0D, 10.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public WarriorsProofItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.STURDY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.warriors_proof.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return PHYSICAL_BY_LEVEL.length - 1;
	}

	@Override
	public double physicalBonus(int level) {
		return valueAt(PHYSICAL_BY_LEVEL, level);
	}

	@Override
	public double attackSpeedBonus(int level) {
		return valueAt(ATTACK_SPEED_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.physical",
						Component.literal(Numbers.format(this.physicalBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.attack_speed",
						Component.literal(Numbers.format(this.attackSpeedBonus(level))).withColor(COLOUR_BONUS)));
	}
}
