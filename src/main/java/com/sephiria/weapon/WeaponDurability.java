package com.sephiria.weapon;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 赛菲莉娅武器的耐久规则（用户 2026-11-15 定）。
 *
 * <p>六把武器统一为原版铁剑的 {@value #MAX_DURABILITY} 点耐久，不再不可破坏；
 * 耐久条由原版根据 damage 自动绘制，不再显示剑意 / 弹匣。
 *
 * <p><b>消耗</b>：普通攻击每次命中扣 {@value #NORMAL_ATTACK_COST}（1.20.1 没有 WEAPON
 * 组件，由 {@link #register()} 的命中回调统一扣）；技能「射击」扣 {@value #SHOT_COST}，
 * 其余技能每次发动扣 {@value #SKILL_COST}：防御（每次挡下伤害）、横扫、旋风、招架、狂怒、
 * 强力斩击、回击 1/2/3。
 */
public final class WeaponDurability {
	/** 与原版铁剑相同的耐久上限。 */
	public static final int MAX_DURABILITY = 250;
	/** 普通攻击每次命中。 */
	public static final int NORMAL_ATTACK_COST = 1;
	/** 弩射击。 */
	public static final int SHOT_COST = 1;
	/** 其余技能。 */
	public static final int SKILL_COST = 2;

	private WeaponDurability() {
	}

	/** 注册普通攻击的耐久消耗（所有赛菲莉娅武器共用一条，避免每个武器各写一遍）。 */
	public static void register() {
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (!(player instanceof ServerPlayer serverPlayer) || !(entity instanceof LivingEntity)) {
				return InteractionResult.PASS;
			}

			ItemStack stack = player.getItemInHand(hand);

			if (!(stack.getItem() instanceof SephiriaWeapon)) {
				return InteractionResult.PASS;
			}

			// 防御中的盾剑左键是「横扫」技能，由技能入口收 2 点；普通攻击不再重复收
			if (stack.getItem() instanceof SephiriaShieldItem && SephiriaShieldItem.isDefending(player)) {
				return InteractionResult.PASS;
			}

			// 刀在自己的命中回调里区分普通攻击与强力斩击，由它处理
			if (stack.getItem() instanceof SephiriaKatanaItem) {
				return InteractionResult.PASS;
			}

			hurt(stack, serverPlayer, hand, NORMAL_ATTACK_COST);
			return InteractionResult.PASS;
		});
	}

	public static void hurt(ItemStack stack, ServerPlayer player, InteractionHand hand, int amount) {
		hurt(stack, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND, amount);
	}

	/** 扣耐久；技能消耗都是整数，耐久上限见 {@link #MAX_DURABILITY}。 */
	public static void hurt(ItemStack stack, ServerPlayer player, EquipmentSlot slot, int amount) {
		if (amount <= 0 || !(stack.getItem() instanceof SephiriaWeapon)) {
			return;
		}

		if (!stack.isDamageableItem() || player.isCreative() || player.isSpectator()) {
			return;
		}

		stack.hurtAndBreak(amount, player, entity -> entity.broadcastBreakEvent(slot));
	}
}
