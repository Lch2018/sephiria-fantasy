package com.sephiria.artifact;

import com.sephiria.artifact.skill.ShieldSkill;
import net.minecraft.network.chat.Component;

/** 护盾（守护，普通，魔法书）：【唯一】获得技能「护盾」——10 点护盾 6 秒，冷却 20 秒，蓝耗 20/12。 */
public class ShieldBookItem extends AdvancedMagicBookItem {
	public ShieldBookItem(Properties properties) {
		super(properties, ShieldSkill.INSTANCE, ArtifactCombo.GUARD, "shield",
				"artifact.sephiria_fantasy.shield.flavor", 1, ArtifactRarity.COMMON);
	}

	@Override
	protected java.util.List<Component> descriptionLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.shield_effect",
						Component.literal("10").withColor(0xFF55FF55)),
				Component.translatable("artifact.sephiria_fantasy.affix.shield_duration",
						Component.literal("6").withColor(0xFF55FF55)),
				skillCooldown(20),
				skillMp(ShieldSkill.mpCostAt(level)));
	}
}
