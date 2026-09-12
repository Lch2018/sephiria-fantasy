package com.sephiria.weapon;

import net.minecraft.world.item.Item;

/**
 * 近战类 SEPHIRIA 武器（剑与盾 / 大剑 / 匕首 / 刀 / 长棍）。
 *
 * <p>26.2 已经删掉了原版的 {@code SwordItem}，剑类物品就是一个普通 {@code Item}，
 * 攻击力、攻速、耐久全部由 {@code Item.Properties#sword(...)} 写进数据组件，
 * 所以这里继承 {@code Item} 即可，行为与铁剑一致。
 */
public class SephiriaWeaponItem extends Item implements SephiriaWeapon {
	private final WeaponBranch branch;

	public SephiriaWeaponItem(WeaponBranch branch, Item.Properties properties) {
		super(properties);
		this.branch = branch;
	}

	@Override
	public WeaponBranch branch() {
		return this.branch;
	}
}
