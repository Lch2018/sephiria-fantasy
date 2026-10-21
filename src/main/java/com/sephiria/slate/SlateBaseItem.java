package com.sephiria.slate;

import com.sephiria.artifact.ArtifactRarity;

/**
 * 石板基础：天赋「耐心」第一阶段发给玩家的石板。
 *
 * <p><b>效果与石板「基础」（slate_of_foundation）完全一致</b>（用户 2026-10-21 口径）：
 * 同一行左右各 8 格、每格 +1，不可旋转。直接复用 {@link Slates#FOUNDATION} 的图案，
 * 所以以后调「基础」的数值时这里会跟着变，不会出现两份不同的表。
 *
 * <p>特殊之处只在携带规则：放进背包后不能移出 / 丢弃，也不能被「石板刻印」消耗；
 * 第一阶段熄灭时由 {@code PatienceTalent} 直接删掉。
 */
public class SlateBaseItem extends PatternSlateItem {
	public SlateBaseItem(Properties properties) {
		super(properties, ArtifactRarity.COMMON, "artifact.sephiria_fantasy.slate_base.flavor",
				Slates.FOUNDATION, false);
	}
}
