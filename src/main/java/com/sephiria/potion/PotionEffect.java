package com.sephiria.potion;

import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.registry.ModItems;
import com.sephiria.stats.PlayerStats;
import com.sephiria.util.Numbers;
import com.sephiria.util.PlayerItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 一种药水的效果：喝下去做什么、提示框怎么写。
 *
 * <p>数值全部集中在 {@link Potions}，这里只负责「按效果种类办事」。
 *
 * <p>药水效果<b>不是原版 buff</b>：不占效果栏、牛奶解不掉。带持续时间的（苹果汁）由
 * {@link PotionTimers} 自己计时；没写持续时间的（物理强度、HP 偷取）就是永久加在属性上。
 */
public record PotionEffect(Kind kind, double amount, int seconds) {
	/** 效果种类。 */
	public enum Kind {
		/** 立即回复最大生命值的百分之 amount。 */
		HEAL_PERCENT,
		/** 每秒回复 amount 点生命，持续 seconds 秒。 */
		HEAL_PER_SECOND,
		/** 永久 +amount 物理强度（算「药水」那一份来源）。 */
		PHYSICAL,
		/** 永久 +amount 火元素强度（算「药水」那一份来源）。 */
		FIRE,
		/** 永久 +amount 冰元素强度（算「药水」那一份来源）。 */
		ICE,
		/** 永久 +amount 电元素强度（算「药水」那一份来源）。 */
		LIGHTNING,
		/** 随机挑一种连击，永久 +amount 级（连击面板与连击效果都会跟着涨）。 */
		COMBO,
		/** 立即获得 amount 个骰子。 */
		DICE,
		/** 永久 +amount HP 偷取。 */
		LIFESTEAL
	}

	/** 服务端结算：喝下去的效果都走这里。 */
	public void apply(ServerPlayer player) {
		switch (this.kind) {
			case HEAL_PERCENT -> player.heal((float) (player.getMaxHealth() * this.amount / 100.0D));
			case HEAL_PER_SECOND -> PotionTimers.healPerSecond(player, (float) this.amount, this.seconds * 20);
			case PHYSICAL -> PlayerStats.addPotionPhysical(player, this.amount);
			case FIRE -> PlayerStats.addPotionElement(player, PlayerStats.Element.FIRE, this.amount);
			case ICE -> PlayerStats.addPotionElement(player, PlayerStats.Element.ICE, this.amount);
			case LIGHTNING -> PlayerStats.addPotionElement(player, PlayerStats.Element.LIGHTNING, this.amount);
			case COMBO -> grantComboBonus(player);
			case LIFESTEAL -> PlayerStats.addPotionLifesteal(player, this.amount);
			// 交付走 PlayerItems.give：背包满时掉在脚下（别用 Inventory#add 的返回值判断，见它顶部）
			case DICE -> PlayerItems.give(player, new ItemStack(ModItems.DICE, (int) this.amount));
		}
	}

	/** 「索莱拉的偶然」：随机一种连击 +1 级，并在动作栏告诉玩家是哪一种。 */
	private void grantComboBonus(ServerPlayer player) {
		ArtifactCombo[] combos = ArtifactCombo.values();
		ArtifactCombo picked = combos[player.getRandom().nextInt(combos.length)];
		PlayerStats.addComboBonus(player, picked, (int) this.amount);
		player.sendSystemMessage(Component.translatable("message.sephiria_fantasy.combo_bonus",
				Component.translatable(picked.translationKey()), (int) this.amount), true);
	}

	/** 提示框里的那一行效果说明。 */
	public Component describe() {
		return switch (this.kind) {
			case HEAL_PERCENT -> Component.translatable("potion.sephiria_fantasy.regen_percent", num(this.amount));
			case HEAL_PER_SECOND -> Component.translatable("potion.sephiria_fantasy.heal_per_second", num(this.amount),
					num(this.seconds));
			case PHYSICAL -> Component.translatable("potion.sephiria_fantasy.physical", num(this.amount));
			case FIRE -> Component.translatable("potion.sephiria_fantasy.fire", num(this.amount));
			case ICE -> Component.translatable("potion.sephiria_fantasy.ice", num(this.amount));
			case LIGHTNING -> Component.translatable("potion.sephiria_fantasy.lightning", num(this.amount));
			case COMBO -> Component.translatable("potion.sephiria_fantasy.combo", num(this.amount));
			case DICE -> Component.translatable("potion.sephiria_fantasy.dice", num(this.amount));
			case LIFESTEAL -> Component.translatable("potion.sephiria_fantasy.lifesteal", num(this.amount));
		};
	}

	/** 数值统一按本模组的格式来（整数不带小数点）。 */
	private static String num(double value) {
		return Numbers.format(value);
	}
}
