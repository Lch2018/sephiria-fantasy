package com.sephiria.talent;

import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.potion.SephiriaPotionItem;
import com.sephiria.stats.PlayerStats;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 天赋「生存」的一阶段：喝下高级及以上品质的药水时，随机一项属性 +1。
 *
 * <p>随机池是「面板上已有的永久点数额度」：物理强度、火/冰/电强度、防御力、HP 偷取、
 * 幸运值、MP 吸收。每项都是永久加在玩家属性上（与药水本体的效果同一套存档字段），
 * 并在动作栏提示具体加了哪一项。
 */
public final class SurvivalTalent {
	/** 随机池：每一项对应一条动作栏文案与一个永久加成。 */
	private enum Stat {
		PHYSICAL("screen.sephiria_fantasy.talent.stat.physical"),
		FIRE("screen.sephiria_fantasy.talent.stat.fire"),
		ICE("screen.sephiria_fantasy.talent.stat.ice"),
		LIGHTNING("screen.sephiria_fantasy.talent.stat.lightning"),
		DEFENSE("screen.sephiria_fantasy.talent.stat.defense"),
		LIFESTEAL("screen.sephiria_fantasy.talent.stat.lifesteal"),
		LUCK("screen.sephiria_fantasy.talent.stat.luck"),
		MP_ABSORB("screen.sephiria_fantasy.talent.stat.mp_absorb");

		private final String key;

		Stat(String key) {
			this.key = key;
		}
	}

	private SurvivalTalent() {
	}

	/** 药水喝完时由 {@link SephiriaPotionItem} 调；不是高级以上或没点亮一阶段就什么都不做。 */
	public static void onPotionDrunk(ServerPlayer player, ItemStack stack) {
		if (Talents.stage(player, Talent.SURVIVAL) < 1) {
			return;
		}

		if (!(stack.getItem() instanceof SephiriaPotionItem potion)
				|| potion.rarity().ordinal() < ArtifactRarity.ADVANCED.ordinal()) {
			return;
		}

		Stat[] pool = Stat.values();
		Stat picked = pool[player.getRandom().nextInt(pool.length)];

		switch (picked) {
			case PHYSICAL -> PlayerStats.addPotionPhysical(player, 1.0D);
			case FIRE -> PlayerStats.addPotionElement(player, PlayerStats.Element.FIRE, 1.0D);
			case ICE -> PlayerStats.addPotionElement(player, PlayerStats.Element.ICE, 1.0D);
			case LIGHTNING -> PlayerStats.addPotionElement(player, PlayerStats.Element.LIGHTNING, 1.0D);
			case DEFENSE -> PlayerStats.addDefense(player, 1.0D);
			case LIFESTEAL -> PlayerStats.addPotionLifesteal(player, 1.0D);
			case LUCK -> PlayerStats.addLuck(player, 1.0D);
			case MP_ABSORB -> PlayerStats.addMpAbsorb(player, 1.0D);
		}

		// 「生存」两个字用生存天赋色；属性名用一份不带 %s 的纯名称键
		// （原来直接拿属性页的「防御力：%s」当参数，会显示成「生存：防御力：%s +1」）
		Component talentName = Component.translatable("screen.sephiria_fantasy.talent.survival")
				.withColor(Talent.SURVIVAL.colour());
		player.sendSystemMessage(Component.translatable("message.sephiria_fantasy.survival_bonus",
				talentName, Component.translatable(picked.key)), true);
	}
}
