package com.sephiria.artifact;

import com.sephiria.artifact.skill.MagicBoltSkill;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 三本「魔弹」魔法书的共同实现：普通品质、【唯一】、最高 1 级，
 * 数值（固定伤害 / 元素倍率 / 蓝耗）全部从 {@link MagicBoltSkill} 取，提示框与结算不会对不上。
 */
public abstract class MagicBoltBookItem extends ArtifactItem implements MagicBook {
	private static final int COLOUR_BONUS = 0xFF55FF55;

	private final MagicBoltSkill skill;
	private final ArtifactCombo combo;
	private final String flavorKey;
	private final String id;

	protected MagicBoltBookItem(Properties properties, MagicBoltSkill skill, ArtifactCombo combo,
			String flavorKey, String id) {
		super(properties);
		this.skill = skill;
		this.combo = combo;
		this.flavorKey = flavorKey;
		this.id = id;
	}

	@Override
	public ArtifactCombo combo() {
		return this.combo;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
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
		return this.skill.maxLevel();
	}

	@Override
	public com.sephiria.artifact.skill.ArtifactSkill skill() {
		return this.skill;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		java.util.List<Component> lines = new java.util.ArrayList<>();
		lines.add(dashed(skillTitle(this.skill.id())));
		lines.addAll(dashedLines(descriptionLines(level)));
		return java.util.List.copyOf(lines);
	}

	@Override
	public java.util.List<Component> skillDescriptionLines(int level) {
		return descriptionLines(level);
	}

	/** 不带行首标记的技能说明：技能页的 tooltip 直接用它。 */
	private java.util.List<Component> descriptionLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix." + this.id + "_effect"),
				Component.translatable("artifact.sephiria_fantasy.affix." + this.id + "_damage",
						Component.literal(Numbers.format(this.skill.flatDamage(level))).withColor(COLOUR_BONUS),
						Component.literal(Numbers.format(this.skill.percentDamage(level))).withColor(COLOUR_BONUS)),
				skillCooldown(Math.max(1, this.skill.cooldownTicks() / 20)),
				skillMp(this.skill.mpCost(level)));
	}
}
