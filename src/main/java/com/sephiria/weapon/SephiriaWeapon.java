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
	default Component tooltipLine() {
		return Component.translatable("tooltip.sephiria.branch",
				Component.translatable(this.branch().translationKey()),
				this.branch().procMultiplierText());
	}
}
