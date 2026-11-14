package com.sephiria.artifact;

import com.sephiria.artifact.skill.MagicBoltSkill;

/** 闪电魔弹（魔法科技，普通品质，魔法书）：【唯一】获得技能「闪电魔弹」。 */
public class LightningBoltItem extends MagicBoltBookItem {
	public LightningBoltItem(Properties properties) {
		super(properties, MagicBoltSkill.LIGHTNING, ArtifactCombo.MAGIC_TECH,
				"artifact.sephiria_fantasy.lightning_bolt.flavor", "lightning_bolt");
	}
}
