package com.sephiria.potion;

import com.sephiria.registry.ModItems;
import com.sephiria.stats.PlayerStats;
import com.sephiria.util.Numbers;
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
			case LIFESTEAL -> PlayerStats.addPotionLifesteal(player, this.amount);
			case DICE -> {
				ItemStack dice = new ItemStack(ModItems.DICE, (int) this.amount);

				if (!player.getInventory().add(dice)) {
					player.drop(dice, false);
				}
			}
		}
	}

	/** 提示框里的那一行效果说明。 */
	public Component describe() {
		return switch (this.kind) {
			case HEAL_PERCENT -> Component.translatable("potion.sephiria.regen_percent", num(this.amount));
			case HEAL_PER_SECOND -> Component.translatable("potion.sephiria.heal_per_second", num(this.amount),
					num(this.seconds));
			case PHYSICAL -> Component.translatable("potion.sephiria.physical", num(this.amount));
			case DICE -> Component.translatable("potion.sephiria.dice", num(this.amount));
			case LIFESTEAL -> Component.translatable("potion.sephiria.lifesteal", num(this.amount));
		};
	}

	/** 数值统一按本模组的格式来（整数不带小数点）。 */
	private static String num(double value) {
		return Numbers.format(value);
	}
}
