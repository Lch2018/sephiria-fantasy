package com.sephiria.backpack;

import net.minecraft.world.inventory.Slot;

/**
 * 带赛菲利亚背包格区的菜单（背包页与神器附魔页）：把「这格是不是背包格区」统一成
 * 一个判定入口，服务端处理「只带菜单格子编号」的包（比如旋转石板）时不用为每个
 * 菜单各写一份。
 */
public interface BackpackGridMenu {
	/** 这格是不是赛菲利亚背包格区（玩家背包区、功能区都不算）。 */
	boolean isBackpackSlot(Slot slot);
}
