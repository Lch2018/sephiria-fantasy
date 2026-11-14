package com.sephiria.mixin.client;

import com.sephiria.difficulty.RootRetreat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 根之退「勇气」祝福：40 阶段玩家名字显示为黄色。 */
@Mixin(Player.class)
public abstract class RootRetreatNameMixin {
	@Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
	private void sephiria$courageName(CallbackInfoReturnable<Component> cir) {
		if ((Object) this instanceof ServerPlayer player
				&& RootRetreat.stageOf(player) >= RootRetreat.COURAGE_STAGE) {
			cir.setReturnValue(Component.literal(player.getScoreboardName()).withStyle(ChatFormatting.YELLOW));
		}
	}
}
