package com.sephiria.weapon;

import net.minecraft.network.chat.Component;

/**
 * 标记这个物品是一把 SEPHIRIA 武器，并给出它所属的分支。
 *
 * <p>近战武器（{@link SephiriaWeaponItem}）和远程武器（{@link SephiriaCrossbowItem}）走的是
 * 不同的原版父类，锻造系统接入后需要按分支而不是按父类来处理，所以这里统一成一个接口。
 */
public interface SephiriaWeapon {
	WeaponBranch branch();

	/**
	 * 提示框里显示的那一行：分支 + 命中倍率。
	 *
	 * <p>注意 26.2 已把 {@code Item#appendHoverText} 标记为过时（提示框改为由数据组件实现
	 * {@code TooltipProvider} 提供），所以这里只负责拼文本，实际添加由客户端的
	 * {@code ItemTooltipCallback} 完成。
	 */
	/**
	 * 详细描述：由武器自己报告技能数值。
	 *
	 * <p>放在这里而不是在客户端写死文本，是为了让提示框和实际数值必然一致——数值都取自
	 * 各个武器类顶部的常量，改一处两边都变。
	 */
	default java.util.List<Component> detailLines(net.minecraft.world.item.ItemStack stack) {
		return java.util.List.of();
	}

	/** 数值格式化：整数不带小数点，其余保留两位。 */
	default String format(double value) {
		if (Math.abs(value - Math.rint(value)) < 0.005D) {
			return String.valueOf((long) Math.rint(value));
		}

		return String.format("%.2f", value);
	}

	/** 时长格式化：tick → 秒。 */
	default String seconds(int ticks) {
		return format(ticks / 20.0D);
	}

	default Component tooltipLine() {
		return Component.translatable("tooltip.sephiria.branch",
				Component.translatable(this.branch().translationKey()),
				this.branch().procMultiplierText());
	}
}
