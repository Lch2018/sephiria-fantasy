package com.sephiria.weapon;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * 赛菲莉娅武器的耐久规则（用户 2026-11-15 定）。
 *
 * <p>六把武器统一为原版铁剑的 {@value #MAX_DURABILITY} 点耐久，不再不可破坏；
 * 耐久条由原版根据 damage 自动绘制，不再显示剑意 / 弹匣。
 *
 * <p><b>消耗</b>：普通攻击走原版 {@code WEAPON.itemDamagePerAttack = 1}，每次命中扣 1；
 * 技能「射击」扣 {@value #SHOT_COST}，其余技能每次发动扣 {@value #SKILL_COST}：
 * 防御（每次挡下伤害）、横扫、旋风、招架、狂怒、强力斩击、回击 1/2/3。
 *
 * <p>这里只负责技能扣除与旧存档迁移；普通攻击由原版处理，避免重复扣耐久。
 */
public final class WeaponDurability {
	/** 与原版铁剑相同的耐久上限。 */
	public static final int MAX_DURABILITY = 250;
	/** 普通攻击：原版 WEAPON 组件每次命中扣 1（本类不再额外扣）。 */
	public static final int NORMAL_ATTACK_COST = 1;
	/** 弩射击。 */
	public static final int SHOT_COST = 1;
	/** 其余技能。 */
	public static final int SKILL_COST = 2;

	private WeaponDurability() {
	}

	public static void hurt(ItemStack stack, ServerPlayer player, InteractionHand hand, int amount) {
		hurt(stack, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND, amount);
	}

	/** 扣耐久：技能消耗都是整数；旧存档里带 UNBREAKABLE 的武器在第一次消耗时迁移。 */
	public static void hurt(ItemStack stack, ServerPlayer player, EquipmentSlot slot, int amount) {
		if (amount <= 0 || !(stack.getItem() instanceof SephiriaWeapon)) {
			return;
		}

		// 旧存档里带 UNBREAKABLE 的武器：第一次产生消耗时迁成可损坏
		if (stack.get(DataComponents.UNBREAKABLE) != null) {
			stack.remove(DataComponents.UNBREAKABLE);
		}

		if (!stack.isDamageableItem() || player.isCreative() || player.isSpectator()) {
			return;
		}

		stack.hurtAndBreak(amount, player, slot);
	}
}
