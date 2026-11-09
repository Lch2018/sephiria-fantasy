package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 黑鳞片（精密，传说品质）：【唯一】每持有 1 个石板物品，暴击几率 +1/1.5/2/2.5%。
 *
 * <p>石板数量 = 赛菲莉娅背包里的石板 + 「石板刻印」保留的石板；合成石板算 2 块。
 * 结算见 {@code ArtifactEffects#blackScaleCritChance}，提示框里的当前数量由同步包
 * {@code SlateCountPayload} 推给客户端。
 */
public class BlackScaleItem extends ArtifactItem {
	private static final double[] CRIT_PER_SLATE_BY_LEVEL = { 1.0D, 1.5D, 2.0D, 2.5D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public BlackScaleItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.PRECISION;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.black_scale.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CRIT_PER_SLATE_BY_LEVEL.length - 1;
	}

	@Override
	public double critChancePerSlate(int level) {
		return valueAt(CRIT_PER_SLATE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.black_scale_crit",
						Component.literal(Numbers.format(this.critChancePerSlate(level))).withColor(COLOUR_BONUS)));
	}
}
