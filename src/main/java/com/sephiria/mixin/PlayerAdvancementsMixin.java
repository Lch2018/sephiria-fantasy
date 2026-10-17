package com.sephiria.mixin;

import com.sephiria.wishpool.WishPool;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 达成成就就送一片四叶草（＝许愿池的抽奖次数 +1，用户 2026-10-17 的口径）。
 *
 * <p>挂 {@code PlayerAdvancements#award(holder, criterion)}：它按「条件」调用，**只有这次调用把
 * 整个成就做完时才返回 true**，所以按返回值判定正好是「每个成就一次」（重复调用返回 false）。
 *
 * <p>「算不算成就」看显示信息：必须有 display、不在隐藏分类里、**而且要弹 toast**
 * （{@code show_toast}）。两个反例正好都被这条排除掉：原版的配方解锁成就是隐藏的；
 * 模组自己那些「配方书展示用」的条目（{@code advancement/weapons/*.json}）是
 * {@code show_toast: false}，而且用 `minecraft:tick` 触发、进游戏就自动发——
 * 不排除的话每个玩家一进服就会白拿八片。
 */
@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {
	/** 达成一个成就送几片四叶草。 */
	private static final int CLOVERS_PER_ACHIEVEMENT = 1;

	@Shadow
	private ServerPlayer player;

	@Inject(method = "award", at = @At("RETURN"))
	private void sephiria_cloverForAdvancement(AdvancementHolder advancement, String criterion,
			CallbackInfoReturnable<Boolean> callback) {
		if (!callback.getReturnValueZ() || !countsAsAchievement(advancement)) {
			return;
		}

		WishPool.addClovers(this.player, CLOVERS_PER_ACHIEVEMENT);
	}

	/** 会弹 toast 的成就才算（见类注释的两个反例）。 */
	private static boolean countsAsAchievement(AdvancementHolder holder) {
		return holder.value().display().filter(display -> !display.hidden() && display.showToast()).isPresent();
	}
}
