package com.sephiria.mixin.client;

import com.sephiria.client.InvulnClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 无敌可视化：让处于无敌窗口的玩家"看起来发光"。
 *
 * <p>原版描边完全由这个方法驱动——{@code EntityRenderer} 提取渲染状态时会问
 * {@code Minecraft#shouldEntityAppearGlowing}，为真就把描边色设成队伍颜色
 * （不在队伍里就是白色）。所以这里只要在无敌期间返回 true，原版的描边管线就会
 * 给玩家模型描一圈白光，不需要自己写渲染。
 */
@Mixin(Minecraft.class)
public class InvulnerableGlowMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void sephiria$glowWhileInvulnerable(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		if (entity instanceof Player && InvulnClientData.isActive(entity)) {
			callback.setReturnValue(true);
		}
	}
}
