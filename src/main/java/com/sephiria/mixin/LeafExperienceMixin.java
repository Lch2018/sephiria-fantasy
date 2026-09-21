package com.sephiria.mixin;

import com.sephiria.stats.PlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 经验 → 树叶：每获得 1 点原版经验就给 1 点赛菲利亚货币「树叶」。
 *
 * <p>挂在 {@code giveExperiencePoints} 上而不是自己累计经验值：原版所有经验来源（打怪、挖矿、
 * 烧炼、经验瓶、指令）最后都会走到这里，一处就够。
 */
@Mixin(Player.class)
public abstract class LeafExperienceMixin {

	@Inject(method = "giveExperiencePoints", at = @At("HEAD"))
	private void sephiria$grantLeaves(int experiencePoints, CallbackInfo info) {
		if ((Object) this instanceof ServerPlayer player && experiencePoints > 0) {
			PlayerStats.addLeaves(player, experiencePoints);
		}
	}
}
