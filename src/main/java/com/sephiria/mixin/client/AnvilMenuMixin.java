package com.sephiria.mixin.client;

import com.sephiria.artifact.BookItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/** 铁砧上的「书 + 附魔书」合并（1.20.1 NBT 版）。 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	private static final int MAX_COST = 40;

	@Shadow
	@Final
	private DataSlot cost;

	@Shadow
	private int repairItemCountCost;

	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void sephiria$mergeBookEnchantments(CallbackInfo info) {
		ItemCombinerMenuAccessor access = (ItemCombinerMenuAccessor) (Object) this;
		ItemStack book = access.sephiria$inputSlots().getItem(0);

		if (!BookItem.isBook(book)) {
			return;
		}

		ItemStack additionStack = access.sephiria$inputSlots().getItem(1);
		CompoundTag tag = additionStack.getTag();
		Map<Enchantment, Integer> addition = tag == null ? Map.of()
				: EnchantmentHelper.deserializeEnchantments(
						tag.getList("StoredEnchantments", Tag.TAG_COMPOUND));

		if (!BookItem.canMerge(addition)) {
			return;
		}

		Map<Enchantment, Integer> current = EnchantmentHelper.getEnchantments(book);
		BookItem.Merge merge = BookItem.merge(current, addition);

		if (merge.enchantments().equals(current)) {
			return;
		}

		ItemStack result = book.copy();
		EnchantmentHelper.setEnchantments(merge.enchantments(), result);
		this.repairItemCountCost = 0;
		this.cost.set(merge.cost());
		access.sephiria$resultSlots().setItem(0,
				merge.cost() >= MAX_COST && !access.sephiria$player().isCreative()
						? ItemStack.EMPTY
						: result);
		info.cancel();
	}
}
