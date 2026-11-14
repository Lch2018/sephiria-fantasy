package com.sephiria.artifact;

import com.sephiria.artifact.skill.MagicBoltSkill;

/** 寒冰魔弹（冰川，普通品质，魔法书）：【唯一】获得技能「寒冰魔弹」。 */
public class IceBoltItem extends MagicBoltBookItem {
	public IceBoltItem(Properties properties) {
		super(properties, MagicBoltSkill.ICE, ArtifactCombo.GLACIER,
				"artifact.sephiria_fantasy.ice_bolt.flavor", "ice_bolt");
	}
}
