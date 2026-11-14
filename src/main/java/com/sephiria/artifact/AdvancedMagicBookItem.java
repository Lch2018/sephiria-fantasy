package com.sephiria.artifact;

import com.sephiria.artifact.skill.ArtifactSkill;
import net.minecraft.network.chat.Component;

/**
 * 技能魔法书的共同实现：给技能、连击、品质、风味键与等级上限，提示框的统一结构是
 * 「获得技能…」抬头 + 效果 / 伤害 / 冷却 / MP 若干行。
 *
 * <p>技能页的 tooltip 复用同一组效果行，但不带神器提示框的「- 」行首标记。
 */
public abstract class AdvancedMagicBookItem extends ArtifactItem implements MagicBook {
	private final ArtifactSkill skill;
	private final ArtifactCombo combo;
	private final String id;
	private final String flavorKey;
	private final int maxLevel;
	private final ArtifactRarity rarity;

	protected AdvancedMagicBookItem(Properties properties, ArtifactSkill skill, ArtifactCombo combo, String id,
			String flavorKey, int maxLevel, ArtifactRarity rarity) {
		super(properties);
		this.skill = skill;
		this.combo = combo;
		this.id = id;
		this.flavorKey = flavorKey;
		this.maxLevel = maxLevel;
		this.rarity = rarity;
	}

	@Override
	public ArtifactCombo combo() {
		return this.combo;
	}

	@Override
	public ArtifactRarity rarity() {
		return this.rarity;
	}

	@Override
	public String flavorKey() {
		return this.flavorKey;
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return this.maxLevel;
	}

	@Override
	public ArtifactSkill skill() {
		return this.skill;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		java.util.List<Component> lines = new java.util.ArrayList<>();
		lines.add(dashed(skillTitle(this.id)));
		lines.addAll(dashedLines(descriptionLines(level)));
		return java.util.List.copyOf(lines);
	}

	@Override
	public java.util.List<Component> skillDescriptionLines(int level) {
		return descriptionLines(level);
	}

	/** 技能效果行：效果、伤害、特殊层数/回旋镖说明、冷却、蓝耗。 */
	protected abstract java.util.List<Component> descriptionLines(int level);
}
