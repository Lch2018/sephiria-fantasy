package com.sephiria.artifact;

import com.sephiria.artifact.skill.MagicBoltSkill;

/** 火焰魔弹（余烬，普通品质，魔法书）：【唯一】获得技能「火焰魔弹」。 */
public class FireBoltItem extends MagicBoltBookItem {
	public FireBoltItem(Properties properties) {
		super(properties, MagicBoltSkill.FIRE, ArtifactCombo.EMBER,
				"artifact.sephiria_fantasy.fire_bolt.flavor", "fire_bolt");
	}
}
