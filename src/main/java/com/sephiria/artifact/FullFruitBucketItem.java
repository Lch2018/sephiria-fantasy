package com.sephiria.artifact;

import com.sephiria.artifact.skill.ArtifactSkill;
import com.sephiria.artifact.skill.FullFruitBucketSkill;
import net.minecraft.network.chat.Component;

/** 饱满果实桶（湖泊，稀有，【唯一】）：发动型神器，恢复 50/100% MP，冷却 30 秒。 */
public class FullFruitBucketItem extends ArtifactItem {
	public FullFruitBucketItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.LAKE;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.full_fruit_bucket.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return 1;
	}

	@Override
	public ArtifactSkill skill() {
		return FullFruitBucketSkill.INSTANCE;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.full_fruit_bucket",
						Component.literal(level <= 0 ? "50" : "100").withColor(0xFF55FF55)));
	}
}
