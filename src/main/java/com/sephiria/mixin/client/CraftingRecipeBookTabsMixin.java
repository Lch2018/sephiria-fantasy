package com.sephiria.mixin.client;

import com.sephiria.registry.ModItems;
import com.sephiria.registry.ModRecipeBookCategories;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方书多一页「赛菲莉娅」：把本模组的配方书分类插进原版工作台配方书的页签列表里。
 *
 * <p>原版那几页是 <b>写死在静态字段里</b> 的（{@code CraftingRecipeBookComponent.TABS}，
 * 一个 {@code List.of(...)} 的常量），Fabric 也没有配方书页签的注册 API——所以只能在这里
 * 于静态初始化之后把列表换成可变副本、再插一项（{@code @Mutable} 打开那个 final 字段）。
 *
 * <p>页签图标用<b>盾剑</b>（{@code default_sword_and_shield}），按用户 2026-10-15 的要求；
 * 插在<b>装备页后面</b>（第 0 项是搜索页，不能插到它前面）。
 *
 * <p>哪一页显示哪些配方由配方自己的 {@code "category"} 决定（本模组全部写成
 * {@code sephiria_fantasy:items}，见 {@code ModRecipeBookCategories}）；
 * 「默认解锁」由那张 {@code minecraft:tick} 触发的进度负责（进游戏即授予全部合成表）。
 */
@Mixin(CraftingRecipeBookComponent.class)
public abstract class CraftingRecipeBookTabsMixin {
	@Shadow
	@Mutable
	private static List<RecipeBookComponent.TabInfo> TABS;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void sephiria$addRecipeBookTab(CallbackInfo ci) {
		List<RecipeBookComponent.TabInfo> tabs = new ArrayList<>(TABS);
		int at = Math.min(1, tabs.size());
		tabs.add(at, new RecipeBookComponent.TabInfo(ModItems.DEFAULT_SWORD_AND_SHIELD,
				ModRecipeBookCategories.ITEMS));
		TABS = List.copyOf(tabs);
	}
}
