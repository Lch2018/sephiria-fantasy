package com.sephiria.registry;

import com.sephiria.Sephiria;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeBookCategory;

/**
 * 配方书分类：给「赛菲莉娅幻想」的物品单开一页。
 *
 * <p>原版配方书（背包 / 工作台右上那本书）按<b>分类</b>分页，原版自己有「建筑方块 / 红石 / 装备 /
 * 杂物」等几页（{@code RecipeBookCategories} 里写死的那批）。这里注册一个自己的分类，
 * 本模组所有合成表都声明成它（配方 JSON 里的 {@code "category"} 字段），配方书里就会出现
 * 单独的一页——页签图标与顺序见客户端的 {@code CraftingRecipeBookTabsMixin}。
 *
 * <p>分类本身只是个登记项（原版 {@code RecipeBookCategory} 是个空类），
 * 真正决定「哪一页显示哪些配方」的是配方自己的 category 字段。
 */
public final class ModRecipeBookCategories {
	/** 分类 id（{@code sephiria_fantasy:items}）——配方 JSON 里写的就是它。 */
	public static final Identifier ITEMS_ID =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "items");

	/** 赛菲莉娅那一页。 */
	public static final RecipeBookCategory ITEMS = Registry.register(
			BuiltInRegistries.RECIPE_BOOK_CATEGORY,
			ITEMS_ID,
			new RecipeBookCategory());

	private ModRecipeBookCategories() {
	}

	public static void initialize() {
	}
}
