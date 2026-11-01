package com.sephiria.mixin.client;

import com.sephiria.stats.PlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 经验 → 叶子：所有原版经验来源最终都会走 giveExperiencePoints。 */
@Mixin(Player.class)
public abstract class LeafExperienceMixin {
	@ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
	private int sephiria$scaleExperience(int experiencePoints) {
		if (!(((Object) this) instanceof ServerPlayer player) || experiencePoints <= 0) {
			return experiencePoints;
		}

		int actual = (int) Math.round(experiencePoints * PlayerStats.xpDropPercentTotal(player) / 100.0D);
		PlayerStats.addLeaves(player, actual);
		return actual;
	}
}
