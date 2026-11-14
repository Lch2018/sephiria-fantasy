package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 渴望宝珠（学院，高级，【唯一】）：占用 15 MP，魔法书伤害 +10/12/15/18%。
 *
 * <p>被占用的 15 MP 不能使用，但最大 MP 的显示值不减少——HUD 把那一截单独画在右侧。
 */
public class OrbOfDesireItem extends ArtifactItem {
	private static final double[] DAMAGE_BY_LEVEL = { 10.0D, 12.0D, 15.0D, 18.0D };
	private static final double RESERVED_MP = 15.0D;
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public OrbOfDesireItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.ACADEMY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.orb_of_desire.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DAMAGE_BY_LEVEL.length - 1;
	}

	@Override
	public double magicBookDamageBonus(int level) {
		return valueAt(DAMAGE_BY_LEVEL, level);
	}

	@Override
	public double mpReservedBonus(int level) {
		return RESERVED_MP;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.mp_reserved",
						Component.literal(Numbers.format(RESERVED_MP)).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_damage",
						Component.literal(Numbers.format(this.magicBookDamageBonus(level))).withColor(COLOUR_BONUS)));
	}
}
