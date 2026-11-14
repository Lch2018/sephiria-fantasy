package com.sephiria.mixin.client;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 铁砧 / 合成器共用的容器与玩家（AnvilMenuMixin 读继承字段用）。 */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {
	@Accessor("inputSlots")
	Container sephiria$inputSlots();

	@Accessor("resultSlots")
	ResultContainer sephiria$resultSlots();

	@Accessor("player")
	Player sephiria$player();
}
