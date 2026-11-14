package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 蜥蜴板甲（守护，稀有）：【唯一】防御力 +14/16/18/21/24；
 * 移动速度 -2..10%、攻击速度 -2..10%、魔法书加速 -2..10。
 *
 * <p>负词条把属性压到默认值以下时按默认值算（见 {@code PlayerStats} 的三处下限）。
 */
public class LizardPlateArmorItem extends ArtifactItem {
	private static final double[] DEFENSE_BY_LEVEL = { 14.0D, 16.0D, 18.0D, 21.0D, 24.0D };
	private static final double[] MOVE_PENALTY_BY_LEVEL = { -2.0D, -4.0D, -6.0D, -8.0D, -10.0D };
	private static final double[] ATTACK_PENALTY_BY_LEVEL = { -2.0D, -4.0D, -6.0D, -8.0D, -10.0D };
	private static final double[] HASTE_PENALTY_BY_LEVEL = { -2.0D, -4.0D, -6.0D, -8.0D, -10.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;
	/** 负词条用红色，和正词条的绿色区分。 */
	private static final int COLOUR_PENALTY = 0xFFFF5555;

	public LizardPlateArmorItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GUARD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.lizard_plate_armor.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DEFENSE_BY_LEVEL.length - 1;
	}

	@Override
	public double defenseBonus(int level) {
		return valueAt(DEFENSE_BY_LEVEL, level);
	}

	@Override
	public double moveSpeedPercentBonus(int level) {
		return valueAt(MOVE_PENALTY_BY_LEVEL, level);
	}

	@Override
	public double attackSpeedBonus(int level) {
		return valueAt(ATTACK_PENALTY_BY_LEVEL, level);
	}

	@Override
	public double magicBookHasteBonus(int level) {
		return valueAt(HASTE_PENALTY_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.defense",
						Component.literal(Numbers.format(this.defenseBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.move_speed_penalty",
						Component.literal(Numbers.format(this.moveSpeedPercentBonus(level))).withColor(COLOUR_PENALTY)),
				Component.translatable("artifact.sephiria_fantasy.affix.attack_speed_penalty",
						Component.literal(Numbers.format(this.attackSpeedBonus(level))).withColor(COLOUR_PENALTY)),
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_haste_penalty",
						Component.literal(Numbers.format(this.magicBookHasteBonus(level))).withColor(COLOUR_PENALTY)));
	}
}
