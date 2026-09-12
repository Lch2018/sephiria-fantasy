package com.sephiria.weapon;

import net.minecraft.world.item.Item;

/**
 * 一把基础武器：所属分支 + 已注册的物品。
 *
 * <p>锻造系统接入后，这里会再挂上升级变种（一级 / 二级）的列表。
 */
public record BaseWeapon(WeaponBranch branch, Item item) {
}
