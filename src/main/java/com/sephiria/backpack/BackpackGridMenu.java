package com.sephiria.backpack;

import net.minecraft.world.inventory.Slot;

/**
 * 带赛菲莉娅背包格区的菜单（背包页与神器附魔页）：把「这格是不是背包格区」与
 * 「滚轮翻页」统一成一个入口，服务端处理那些只带菜单格子编号或方向的包时不用为每个
 * 菜单各写一份。
 */
public interface BackpackGridMenu {
	/** 这格是不是赛菲莉娅背包格区（玩家背包区、功能区都不算）。 */
	boolean isBackpackSlot(Slot slot);

	/**
	 * 这一格背后在真实 {@link ArtifactBackpack} 里的下标；不是背包格 / 背后没有格子时返回 -1。
	 *
	 * <p>滚轮翻页后「可视区第几格」与真实下标不同，石板刻印要用真实下标存档；所以由菜单自己
	 * 现算（行号从数据槽读，客户端也适用）。
	 */
	int backpackIndex(Slot slot);

	/**
	 * 滚轮翻页：{@code deltaRows > 0} 往下。返回是否真的动了（到顶/到底时不动）。
	 *
	 * <p>只在服务端调：视窗偏移在服务端，翻页后由服务端把这一屏重新推给客户端。
	 */
	boolean scrollBackpack(int deltaRows);
}
