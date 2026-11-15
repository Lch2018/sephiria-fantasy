package com.sephiria.mixin.client;

import com.sephiria.artifact.BookItem;
import com.sephiria.weapon.SephiriaWeapon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

	/**
	 * 武器不能再附魔原版附魔书（用户 2026-11-15）：任意一格是赛菲莉娅武器、另一格是原版附魔书时，
	 * 结果直接清空、费用归零——两格摆放本身还允许（不会吞物品），只是合不出东西。
	 *
	 * <p>放在 HEAD 且不区分创造模式：这是玩法限制，不是经验成本问题。
	 * 本模组自己的「书」（{@link BookItem}）不是原版附魔书，走下面那条合并逻辑，不受影响。
	 */
	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void sephiria$blockVanillaEnchantedBook(CallbackInfo info) {
		ItemCombinerMenuAccessor access = (ItemCombinerMenuAccessor) (Object) this;
		ItemStack left = access.sephiria$inputSlots().getItem(0);
		ItemStack right = access.sephiria$inputSlots().getItem(1);

		boolean weapon = left.getItem() instanceof SephiriaWeapon || right.getItem() instanceof SephiriaWeapon;
		boolean book = left.is(Items.ENCHANTED_BOOK) || right.is(Items.ENCHANTED_BOOK);

		if (!weapon || !book) {
			return;
		}

		// 和原版「无结果」的状态保持一致：不消耗第二格、费用清零
		this.repairItemCountCost = 0;
		this.cost.set(0);
		access.sephiria$resultSlots().setItem(0, ItemStack.EMPTY);
		info.cancel();
	}

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
