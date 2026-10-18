package com.sephiria.mixin;

import com.sephiria.artifact.BookItem;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「书」的抢夺接进掉落结算：原版所有按抢夺算的东西（生物掉落物数量、装备掉落率……）
 * 都是问 {@code EnchantmentHelper.getEnchantmentLevel(附魔, 实体)}「这个人身上几级抢夺」，
 * 所以在这一个点上把它加上背包里那本书的等级就够——与武器上的抢夺自然相加。
 *
 * <p>只认抢夺这一种附魔、只认服务端玩家：其它附魔的查询与客户端的一律原样返回。
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
	@Inject(method = "getEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;)I",
			at = @At("RETURN"), cancellable = true)
	private static void sephiria$addBookLooting(Holder<Enchantment> enchantment, LivingEntity entity,
			CallbackInfoReturnable<Integer> info) {
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}

		if (enchantment.unwrapKey().filter(Enchantments.LOOTING::equals).isEmpty()) {
			return;
		}

		int level = BookItem.lootingLevel(player);

		if (level > 0) {
			info.setReturnValue(info.getReturnValueI() + level);
		}
	}
}
