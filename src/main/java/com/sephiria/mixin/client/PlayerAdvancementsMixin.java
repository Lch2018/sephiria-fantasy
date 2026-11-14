package com.sephiria.mixin.client;

import com.sephiria.wishpool.WishPool;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 达成成就送一片四叶草与一点天赋点（1.20.1 版）。 */
@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {
	private static final int CLOVERS_PER_ACHIEVEMENT = 1;
	private static final int TALENT_POINTS_PER_ACHIEVEMENT = 1;

	@Shadow
	private ServerPlayer player;

	@Inject(method = "award", at = @At("RETURN"))
	private void sephiria_cloverForAdvancement(Advancement advancement, String criterion,
			CallbackInfoReturnable<Boolean> callback) {
		if (!callback.getReturnValueZ() || !countsAsAchievement(advancement)) {
			return;
		}

		WishPool.addClovers(this.player, CLOVERS_PER_ACHIEVEMENT);
		com.sephiria.talent.Talents.addPoints(this.player, TALENT_POINTS_PER_ACHIEVEMENT);
	}

	private static boolean countsAsAchievement(Advancement holder) {
		DisplayInfo display = holder.getDisplay();
		return display != null && !display.isHidden() && display.shouldShowToast();
	}
}
