package com.sephiria.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.20.1 的 MouseHandler.xpos / ypos 是私有的，光标还原要直接写这两个字段。 */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
	@Accessor("xpos")
	void sephiria$setXpos(double value);

	@Accessor("ypos")
	void sephiria$setYpos(double value);
}
