package com.sephiria.mixin;

import com.sephiria.artifact.ArtifactEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 诱惑的鱼饵：把神器的海之眷顾 / 饵钓叠加到鱼钩已有的原版附魔等级上。 */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
	@Shadow
	@Final
	@Mutable
	private int luck;

	@Shadow
	@Final
	@Mutable
	private int lureSpeed;

	@Inject(method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V",
			at = @At("RETURN"))
	private void sephiria$applyBait(Player player, Level level, int luck, int lureSpeed, CallbackInfo ci) {
		if (player instanceof ServerPlayer serverPlayer) {
			this.luck += ArtifactEffects.fishingLuckBonus(serverPlayer);
			this.lureSpeed += ArtifactEffects.fishingLureBonus(serverPlayer);
		}
	}
}
