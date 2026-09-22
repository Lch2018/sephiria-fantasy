package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 红色露水（精密，普通品质）：【唯一】暴击时对周围敌人造成「最高属性值 10/14/30%」的物理伤害（0..2 级）。
 *
 * <p>触发以被暴击的目标为原点、范围 300（3 格），吃「物理伤害增幅」——见
 * {@code SephiriaDamage#critSplash}。它是神器伤害，所以吃不到「武器攻击的暴击几率」，
 * 神器伤害的加成属性也还没做。
 */
public class RedDewItem extends ArtifactItem {
	/** 各等级的溅射比例（%）：最高属性值的百分之几。 */
	private static final double[] SPLASH_BY_LEVEL = { 10.0D, 14.0D, 30.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public RedDewItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.PRECISION;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.red_dew.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return SPLASH_BY_LEVEL.length - 1;
	}

	@Override
	public double critSplashPercent(int level) {
		return valueAt(SPLASH_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria.affix.crit_splash",
				Component.literal(Numbers.format(this.critSplashPercent(level))).withColor(COLOUR_BONUS)));
	}
}
