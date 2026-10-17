package com.sephiria.backpack;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 赛菲莉娅背包的「可视窗口」：界面里固定只显示 {@value #ROWS} 行（{@value #SLOTS} 格），
 * 背包更多时靠滚轮翻页（见 {@code ArtifactBackpackMenu#scrollBackpack}）。
 *
 * <p>背包是按**格数**长的（「赛菲莉娅额外背包栏」一次 +1 格），所以最后一行可能只有几格；
 * 窗口按格线性映射（不是按行对齐），末尾那几格由 {@link #exists} 标成「不存在的空格子」。
 *
 * <p><b>为什么需要这一层</b>：菜单里的格子对象（{@code Slot}）在创建时就绑死了容器下标、
 * 之后改不了，而背包的行数会被「赛菲莉娅额外背包栏」撑大。窗口让格子永远只对应「可视区第几格」，
 * 真正指向背包里的哪一格由 {@link #setRow} 现算——背包长到 9 行，界面尺寸也不会跟着涨。
 *
 * <p>两边各拿一个：服务端是玩家真实的 {@link ArtifactBackpack}（偏移随滚轮动），
 * 客户端是原版同步用的空壳容器（偏移恒为 0，内容由服务端整体推过来）。因为两边都按
 * 「可视区下标」建格子，客户端第 i 格永远对应服务端第 i 格。
 */
public final class BackpackWindow implements Container {
	/** 界面里固定显示的行数（= 背包的默认高度）。 */
	public static final int ROWS = ArtifactBackpack.DEFAULT_HEIGHT;
	/** 可视区的格子数 = 宽 × 行。 */
	public static final int SLOTS = ArtifactBackpack.WIDTH * ROWS;

	private final Container source;
	/** 可视区第一格在源容器里的下标（按行对齐，所以是 {@link ArtifactBackpack#WIDTH} 的倍数）。 */
	private int offset;

	public BackpackWindow(Container source) {
		this.source = source;
	}

	/** 可视区第 index 格对应的源容器下标；越界时返回 -1，读出来就是空。 */
	public int sourceIndex(int index) {
		int real = this.offset + index;
		return real >= 0 && real < this.source.getContainerSize() ? real : -1;
	}

	/** 滚到第几行（0 = 从第一行开始；一行 = {@link ArtifactBackpack#WIDTH} 格）。 */
	public void setRow(int row) {
		this.offset = Math.max(0, Math.min(row, maxRow())) * ArtifactBackpack.WIDTH;
	}

	/**
	 * 最多能往下滚几行：还有格子没显示出来时**向上取整**——格子数不是 6 的整数倍时
	 * （一次只 +1 格），最后一行只有几格，那几格也得能滚到。
	 */
	public int maxRow() {
		int hidden = this.source.getContainerSize() - SLOTS;
		return hidden <= 0 ? 0 : (hidden + ArtifactBackpack.WIDTH - 1) / ArtifactBackpack.WIDTH;
	}

	/** 现在滚到第几行（0 = 从第一行开始）。 */
	public int row() {
		return this.offset / ArtifactBackpack.WIDTH;
	}

	// ------------------------------------------------------------------ Container

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public boolean isEmpty() {
		for (int index = 0; index < SLOTS; index++) {
			if (!getItem(index).isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getItem(int index) {
		int real = sourceIndex(index);
		return real < 0 ? ItemStack.EMPTY : this.source.getItem(real);
	}

	@Override
	public ItemStack removeItem(int index, int amount) {
		int real = sourceIndex(index);
		return real < 0 ? ItemStack.EMPTY : this.source.removeItem(real, amount);
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		int real = sourceIndex(index);
		return real < 0 ? ItemStack.EMPTY : this.source.removeItemNoUpdate(real);
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		int real = sourceIndex(index);

		if (real >= 0) {
			this.source.setItem(real, stack);
		}
	}

	/** 神器与石板一格一件：沿用背包自己的规则。 */
	@Override
	public int getMaxStackSize() {
		return this.source.getMaxStackSize();
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return this.source.canPlaceItem(0, stack);
	}

	@Override
	public void setChanged() {
		this.source.setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	/** 只清掉这一屏能看见的格子（整个背包的清理不该由界面触发）。 */
	@Override
	public void clearContent() {
		for (int index = 0; index < SLOTS; index++) {
			setItem(index, ItemStack.EMPTY);
		}
	}
}
