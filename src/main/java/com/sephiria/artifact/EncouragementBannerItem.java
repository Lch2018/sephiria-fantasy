package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 鼓励旗帜（风之歌，稀有品质）：<b>发动型神器</b>——原地召唤旗帜，范围内的友军攻击速度提高
 * 10/20/30%，范围 600/700/800（0..2 级），持续 10 秒、冷却 30 秒。
 *
 * <p><b>没有【唯一】</b>：带两面就是两个独立的神器技能（等级各自算），风之歌连击也多算一级。
 * 技能本身在 {@code artifact.skill.EncouragementBannerSkill}。
 */
public class EncouragementBannerItem extends ArtifactItem {
	/** 各等级的攻速加成（百分点）。 */
	private static final double[] ATTACK_SPEED_BY_LEVEL = { 10.0D, 20.0D, 30.0D };
	/** 各等级的范围（本模组基准：100 = 原版铁剑横扫，也就是 1 格）。 */
	private static final double[] RANGE_BY_LEVEL = { 600.0D, 700.0D, 800.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public EncouragementBannerItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.WIND_SONG;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.encouragement_banner.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return ATTACK_SPEED_BY_LEVEL.length - 1;
	}

	/** 该等级的攻速加成（百分点）：旗帜技能用它。 */
	public static double bannerAttackSpeed(int level) {
		return valueAt(ATTACK_SPEED_BY_LEVEL, level);
	}

	/** 该等级的范围（基准值，100 = 1 格）：旗帜技能用它。 */
	public static double bannerRange(int level) {
		return valueAt(RANGE_BY_LEVEL, level);
	}

	@Override
	public com.sephiria.artifact.skill.ArtifactSkill skill() {
		return com.sephiria.artifact.skill.EncouragementBannerSkill.INSTANCE;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.banner_skill",
						Component.literal(Numbers.format(bannerAttackSpeed(level))).withColor(COLOUR_BONUS),
						Component.literal(Numbers.format(bannerRange(level))).withColor(COLOUR_BONUS)));
	}
}
