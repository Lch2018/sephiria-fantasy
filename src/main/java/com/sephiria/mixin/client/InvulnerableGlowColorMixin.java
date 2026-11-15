package com.sephiria.mixin.client;

import com.sephiria.client.InvulnClientData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 无敌描边颜色：原版描边色来自 {@code Entity#getTeamColor()}（不在队伍时由渲染器回落成白色）。
 *
 * <p>根之退 40 阶段把无敌描边改成金色、50 阶段改成红色；颜色随无敌包从服务端同步过来，
 * 这里只在无敌窗口内覆盖队伍色，不影响其它发光来源。
 */
@Mixin(Entity.class)
public class InvulnerableGlowColorMixin {
	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void sephiria$glowColor(CallbackInfoReturnable<Integer> callback) {
		Entity self = (Entity) (Object) this;

		if (self instanceof Player && InvulnClientData.isActive(self)) {
			callback.setReturnValue(InvulnClientData.color(self));
		}
	}
}
