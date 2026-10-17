package com.sephiria.mixin;

import com.sephiria.registry.ModRecipeBookCategories;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让本模组的合成表归到「赛菲莉娅」那一页配方书里。
 *
 * <p>原版合成表的 {@code "category"} 字段其实是个<b>四值枚举</b>（{@code CraftingBookCategory}：
 * 建筑方块 / 红石 / 装备 / 杂物），配方书分类由 {@code CraftingRecipe#recipeBookCategory()}
 * 这个<b>默认方法</b>从枚举映射出来——所以配方 JSON 里<b>写不出</b>自定义分类，
 * 只能在代码里改这一处。
 *
 * <p>怎么认出「本模组的合成表」：配方对象自己不知道自己的 id，但它的产物知道——所以看
 * {@code recipe.display()} 的结果里有没有<b>本模组命名空间的物品</b>（合成表的产物一定是我们自己的）。
 *
 * <p>分类本身在 {@code registry.ModRecipeBookCategories} 注册；客户端那一页页签
 * （盾剑图标）在 {@code client.CraftingRecipeBookTabsMixin} 里插进原版的页签列表。
 */
@Mixin(CraftingRecipe.class)
public interface CraftingRecipeCategoryMixin {
	// cancellable = true 是必需的：CallbackInfoReturnable#setReturnValue 在非 cancellable 的注入点上
	// 会抛 UnsupportedOperationException（一遇到本模组的合成表就炸）
	@Inject(method = "recipeBookCategory", at = @At("HEAD"), cancellable = true)
	private void sephiria$useSephiriaCategory(CallbackInfoReturnable<RecipeBookCategory> callback) {
		if (isSephiriaRecipe((CraftingRecipe) this)) {
			callback.setReturnValue(ModRecipeBookCategories.ITEMS);
		}
	}

	/** 这个合成表的产物是不是本模组的物品（顺着产物链找一遍）。 */
	private static boolean isSephiriaRecipe(CraftingRecipe recipe) {
		for (RecipeDisplay display : recipe.display()) {
			if (producesModItem(display.result())) {
				return true;
			}
		}

		return false;
	}

	private static boolean producesModItem(SlotDisplay display) {
		if (display instanceof SlotDisplay.ItemSlotDisplay item) {
			return isModItem(item.item().value());
		}

		if (display instanceof SlotDisplay.ItemStackSlotDisplay stack) {
			return isModItem(stack.stack().item().value());
		}

		if (display instanceof SlotDisplay.Composite composite) {
			for (SlotDisplay part : composite.contents()) {
				if (producesModItem(part)) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean isModItem(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(com.sephiria.Sephiria.MOD_ID);
	}
}
