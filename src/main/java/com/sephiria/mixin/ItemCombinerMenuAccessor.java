package com.sephiria.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 铁砧 / 合成器共用的那两个容器与开菜单的玩家：它们声明在 {@code ItemCombinerMenu} 上，
 * 而 {@code AnvilMenu} 的 mixin 里 @Shadow 不到**继承来**的字段（Mixin 只在自己的类里找），
 * 所以在这里开一个访问器，由 {@code AnvilMenuMixin} 转型过来读。
 */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {
	@Accessor("inputSlots")
	Container sephiria$inputSlots();

	@Accessor("resultSlots")
	ResultContainer sephiria$resultSlots();

	@Accessor("player")
	Player sephiria$player();
}
