package com.sephiria.weapon;

import com.sephiria.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 赛菲利亚弩的专用弹药：弩矢。
 *
 * <p>它和原版箭矢是两种物品——重型弩不消耗、也不要求背包里有原版箭矢，弹匣里装的始终是弩矢。
 * 发射出去后走的是原版箭矢的飞行/伤害/插地逻辑（{@link Arrow} 实体），但**不能被拾取**：
 * 与原版附魔「无限」的弓射出的箭一致，落地的弩矢不会回到任何人手里，弩的弹药只靠自身装填补充。
 */
public class SephiriaBoltItem extends ArrowItem {
	public SephiriaBoltItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, ItemStack weapon) {
		Arrow arrow = new Arrow(level, shooter, new ItemStack(ModItems.CROSSBOW_BOLT), weapon);
		// 与原版「无限」附魔射出的箭一样：不可拾取。
		arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
		return arrow;
	}
}
