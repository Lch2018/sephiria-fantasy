package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 金色枫叶（谈判，普通品质）：【唯一】经验掉落 +10%。
 *
 * <p>只有一条固定数值，没有等级可言（maxLevel 0）——神器附魔币升不了它。
 */
public class GoldenMapleLeafItem extends ArtifactItem {
	/** 经验掉落加成（百分点，100 = 100%）：单一档，整件就是 +10%。 */
	private static final double XP_DROP_BONUS = 10.0D;
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public GoldenMapleLeafItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.NEGOTIATION;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.golden_maple_leaf.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return 0;
	}

	@Override
	public double xpDropPercentBonus(int level) {
		return XP_DROP_BONUS;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.xp_drop",
				Component.literal(Numbers.format(this.xpDropPercentBonus(level))).withColor(COLOUR_BONUS)));
	}
}
