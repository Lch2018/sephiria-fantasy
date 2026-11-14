package com.sephiria.artifact;

import net.minecraft.network.chat.Component;

/**
 * 音叉（学院 + 坚固，羁绊，【唯一】）：施放魔法书攒「声波」层数（最多 5 层，0.8 秒冷却），
 * 武器攻击时一次消耗所有层数，对命中点造成混沌伤害（视为魔法书伤害）。
 */
public class TuningForkItem extends ArtifactItem {
	public TuningForkItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.ACADEMY;
	}

	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.ACADEMY, ArtifactCombo.STURDY);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.tuning_fork.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return 0;
	}

	@Override
	public boolean tuningForkEnabled(int level) {
		return true;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.tuning_fork_stack"),
				Component.translatable("artifact.sephiria_fantasy.affix.tuning_fork_consume"),
				Component.translatable("artifact.sephiria_fantasy.affix.tuning_fork_damage"));
	}
}
