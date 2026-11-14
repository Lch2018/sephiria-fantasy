package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 鼠魔法师的吊坠（学院，高级品质）：【唯一】火焰 / 寒冰 / 闪电魔弹追踪准星附近最近的敌人；
 * MP 再生 +2/5（每 10 秒）。
 */
public class MouseMagicianPendantItem extends ArtifactItem {
	/** 内部每秒值；显示与结算 ×10 成每 10 秒。 */
	private static final double[] MP_REGEN_BY_LEVEL = { 0.2D, 0.5D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public MouseMagicianPendantItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.mouse_magician_pendant.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return MP_REGEN_BY_LEVEL.length - 1;
	}

	@Override
	public boolean magicBoltTracking(int level) {
		return true;
	}

	@Override
	public double mpRegenBonus(int level) {
		return valueAt(MP_REGEN_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.magic_bolt_tracking"),
				Component.translatable("artifact.sephiria_fantasy.affix.mp_regen",
						Component.literal(Numbers.format(this.mpRegenBonus(level) * 10.0D)).withColor(COLOUR_BONUS)));
	}
}
