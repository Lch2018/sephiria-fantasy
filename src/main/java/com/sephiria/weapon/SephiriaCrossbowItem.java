package com.sephiria.weapon;

import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;

/**
 * 远程类 SEPHIRIA 武器（目前只有弩分支的重型弩）。
 *
 * <p>装填、蓄力、发射这些行为全部由原版 {@link CrossbowItem} 提供，
 * 这里只补上分支信息。
 */
public class SephiriaCrossbowItem extends CrossbowItem implements SephiriaWeapon {
	private final WeaponBranch branch;

	public SephiriaCrossbowItem(WeaponBranch branch, Item.Properties properties) {
		super(properties);
		this.branch = branch;
	}

	@Override
	public WeaponBranch branch() {
		return this.branch;
	}
}
