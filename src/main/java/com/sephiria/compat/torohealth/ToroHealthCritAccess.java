package com.sephiria.compat.torohealth;

/**
 * 给 ToroHealth 的 {@code HealthChangeParticle} 加运行时显示信息。
 *
 * <p>混入类实现它，粒子工厂拿到粒子后写入；粒子渲染文字时读取：
 * <ul>
 *   <li>{@code custom}：这一颗是本模组伤害的数字，需要走自定义渲染；</li>
 *   <li>{@code crit}：暴击，数字放大 2 倍；</li>
 *   <li>{@code secondaryColor}：双属性的下半色（-1 = 单色）；</li>
 *   <li>{@code displayValue}：税前伤害的显示值（-1 = 用 ToroHealth 的生命变化值）。</li>
 * </ul>
 */
public interface ToroHealthCritAccess {
	void sephiria$setCustom(boolean custom);

	boolean sephiria$isCustom();

	void sephiria$setCrit(boolean crit);

	boolean sephiria$isCrit();

	void sephiria$setSecondaryColor(int color);

	int sephiria$secondaryColor();

	void sephiria$setDisplayValue(int value);

	int sephiria$displayValue();
}
