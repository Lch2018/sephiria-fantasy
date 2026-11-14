package com.sephiria.mixin.client;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 打开 {@code Particle#getLightColor(float)}（protected）：ToroHealth 粒子渲染要复用它。 */
@Mixin(Particle.class)
public interface ParticleAccessor {
	@Invoker("getLightColor")
	int sephiria$getLightColor(float partialTick);

	@org.spongepowered.asm.mixin.gen.Accessor("xo")
	double sephiria$xo();

	@org.spongepowered.asm.mixin.gen.Accessor("yo")
	double sephiria$yo();

	@org.spongepowered.asm.mixin.gen.Accessor("zo")
	double sephiria$zo();

	@org.spongepowered.asm.mixin.gen.Accessor("x")
	double sephiria$x();

	@org.spongepowered.asm.mixin.gen.Accessor("y")
	double sephiria$y();

	@org.spongepowered.asm.mixin.gen.Accessor("z")
	double sephiria$z();

	@org.spongepowered.asm.mixin.gen.Accessor("rCol")
	float sephiria$red();

	@org.spongepowered.asm.mixin.gen.Accessor("gCol")
	float sephiria$green();

	@org.spongepowered.asm.mixin.gen.Accessor("bCol")
	float sephiria$blue();

	@org.spongepowered.asm.mixin.gen.Accessor("alpha")
	float sephiria$alpha();

}
