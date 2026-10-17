package com.sephiria.artifact;

/**
 * 能放进赛菲莉娅背包的物品：神器，以及将来用于升级的神器附魔与石板。
 *
 * <p>神器和石板都不可堆叠（{@code stacksTo(1)}），放在原版背包里不会生效——
 * 效果只在赛菲莉娅背包里统计（见 {@link ArtifactEffects}）。
 */
public interface BackpackStorable {
}
