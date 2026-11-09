package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 装填用木箱（坚固，稀有品质）：【唯一】最大弹匣数量 +0/1/1/2。
 *
 * <p>弩默认只有 1 个 10 发弹匣；多出来的弹匣打空后会自动换下一个，不需要按 R。
 * 具体规则在 {@code SephiriaCrossbowItem}。
 */
public class SturdyReloadCrateItem extends ArtifactItem {
	private static final int[] MAGAZINES_BY_LEVEL = { 0, 1, 1, 2 };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SturdyReloadCrateItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.STURDY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.sturdy_reload_crate.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return MAGAZINES_BY_LEVEL.length - 1;
	}

	@Override
	public int maxMagazinesBonus(int level) {
		return MAGAZINES_BY_LEVEL[Math.max(0, Math.min(MAGAZINES_BY_LEVEL.length - 1, level))];
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.max_magazines",
						Component.literal(Numbers.format(this.maxMagazinesBonus(level))).withColor(COLOUR_BONUS)));
	}
}
