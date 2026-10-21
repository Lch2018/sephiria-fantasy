package com.sephiria.mixin;

import com.sephiria.difficulty.RootRetreat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 根之退「勇气」祝福：40 阶段时玩家的 id 显示为黄色（聊天 / 玩家列表 / 头顶名字都走这里）。
 */
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
