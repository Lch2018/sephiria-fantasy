package com.sephiria.mixin.client;

import com.sephiria.artifact.BookItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 「书」的抢夺接进掉落结算（1.20.1 版：getEnchantmentLevel 收 Enchantment 而不是 Holder）。 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
	@Inject(method = "getEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;)I",
			at = @At("RETURN"), cancellable = true)
	private static void sephiria$addBookLooting(Enchantment enchantment, LivingEntity entity,
			CallbackInfoReturnable<Integer> info) {
		if (!(entity instanceof ServerPlayer player) || enchantment != Enchantments.MOB_LOOTING) {
			return;
		}

		int level = BookItem.lootingLevel(player);

		if (level > 0) {
			info.setReturnValue(info.getReturnValueI() + level);
		}
	}
}
