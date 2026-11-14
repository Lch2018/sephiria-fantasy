package com.sephiria.mixin.client;

import com.sephiria.network.FrostQiPayload;
import com.sephiria.weapon.SephiriaWeapon;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 左键挥击时给服务端补一条「我挥了一下」（冰霜武具驱邪剑鞘用）。 */
@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
	@Inject(method = "startAttack", at = @At("HEAD"))
	private void sephiria$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
		Minecraft client = (Minecraft) (Object) this;
		LocalPlayer player = client.player;

		if (player == null || client.screen != null) {
			return;
		}

		if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
			return;
		}

		if (!(player.getMainHandItem().getItem() instanceof SephiriaWeapon)) {
			return;
		}

		ClientPlayNetworking.send(FrostQiPayload.CHANNEL, new FriendlyByteBuf(Unpooled.buffer()));
	}
}
