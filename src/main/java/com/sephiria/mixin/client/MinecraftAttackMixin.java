package com.sephiria.mixin.client;

import com.sephiria.network.FrostQiPayload;
import com.sephiria.weapon.SephiriaWeapon;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 左键挥击时给服务端补一条「我挥了一下」——冰霜武具的驱邪剑鞘靠它放剑气。
 *
 * <p>为什么要在客户端补这一下：26.3 里<b>挥空不会给服务端发任何包</b>（挥手动画是服务端发下来的
 * {@code ClientboundSwingAnimationPacket}，打实体才发 {@code ServerboundAttackPacket}），
 * 而剑鞘的要求是「点左键就发剑气，不必打中敌人」，挂在伤害事件上根本收不到挥空。
 *
 * <p>注入点是 {@code Minecraft#startAttack}（26.3 里 <b>private boolean startAttack()</b>，
 * 返回类型是 {@code boolean}，所以回调必须用 {@link CallbackInfoReturnable}，
 * 写成 {@code CallbackInfo} 会在注入时炸掉）：它在 <b>每一次左键点击</b>时被调用一次
 * （原版在 {@code handleKeybinds} 里对每次累积的点击调一遍），打实体与挥空都会走到，
 * 而且不像 {@code ClientPreAttackCallback} 那样按住不放时每刻都回调。
 *
 * <p>两个过滤（都是「这不是挥武器」的明显情形）：
 * ① 准星指着方块（挖矿 / 对着墙点）——原版在那里走的是 {@code startDestroyBlock}，不是攻击动作，
 * 也别让剑气一头撞进石头里；
 * ② 主手不是本模组的武器（服务端还会再判一遍，客户端这层只是为了少发包）。
 */
@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
	@Inject(method = "startAttack", at = @At("HEAD"))
	private void sephiria$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
		Minecraft client = (Minecraft) (Object) this;
		LocalPlayer player = client.player;

		if (player == null || client.gui.screen() != null) {
			return;
		}

		if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
			return;
		}

		if (!(player.getMainHandItem().getItem() instanceof SephiriaWeapon)) {
			return;
		}

		ClientPlayNetworking.send(FrostQiPayload.INSTANCE);
	}
}
