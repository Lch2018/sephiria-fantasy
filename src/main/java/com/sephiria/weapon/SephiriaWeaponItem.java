package com.sephiria.weapon;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * 近战类 SEPHIRIA 武器（剑与盾 / 大剑 / 匕首 / 刀 / 长棍）。
 *
 * <p>26.2 已经删掉了原版的 {@code SwordItem}，剑类物品就是一个普通 {@code Item}，
 * 攻击力、攻速、耐久全部由 {@code Item.Properties#sword(...)} 写进数据组件，
 * 所以这里继承 {@code Item} 即可，行为与铁剑一致。
 *
 * <p>带 {@code drawAnimation} 的武器（目前是刀）右击会进入"使用中"状态：
 * 物品定义里的 {@code using_item} 与 {@code use_duration} 就是靠它来切换
 * 收鞘 / 拔刀中 / 出鞘三个模型，从而做出拔刀动作。
 *
 * <p>带 {@code blocks} 的武器（目前是标准剑盾）右击进入原版盾牌式的格挡：
 * 物品注册时挂了 {@code blocks_attacks} 组件（参数直接取自原版盾），
 * 使用动画为 {@code BLOCK}，所以抬手、减伤、斧头破盾等行为全部走原版逻辑。
 */
public class SephiriaWeaponItem extends Item implements SephiriaWeapon {
	/** 拔刀动作的总时长（tick），模型在 {@code use_duration} 达到 6 时切成完全出鞘。 */
	public static final int DRAW_TICKS = 12;

	private final WeaponBranch branch;
	private final boolean drawAnimation;
	private final boolean blocks;

	public SephiriaWeaponItem(WeaponBranch branch, boolean drawAnimation, boolean blocks, Item.Properties properties) {
		super(properties);
		this.branch = branch;
		this.drawAnimation = drawAnimation;
		this.blocks = blocks;
	}

	@Override
	public WeaponBranch branch() {
		return this.branch;
	}

	@Override
	public java.util.List<Component> detailLines(ItemStack stack) {
		// 详细描述由各自的武器类报告（这个通用实现没有具体数值可说）
		return java.util.List.of();
	}


	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!this.blocks && !this.drawAnimation) {
			return super.use(level, player, hand);
		}

		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		if (this.blocks) {
			return 72000;
		}
		return this.drawAnimation ? DRAW_TICKS : 0;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		if (this.blocks) {
			return ItemUseAnimation.BLOCK;
		}
		return this.drawAnimation ? ItemUseAnimation.SPEAR : ItemUseAnimation.NONE;
	}
}
