package com.sephiria.mixin;

import com.sephiria.stats.PlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 经验 → 叶子：每获得 1 点实际经验就给 1 点赛菲利亚货币「叶子」。
 *
 * <p>挂在 {@code giveExperiencePoints} 上而不是自己累计经验值：原版所有经验来源（打怪、挖矿、
 * 烧炼、经验瓶、指令）最后都会走到这里，一处就够。
 *
 * <p>「经验掉落」属性在这里缩放：入参 100、掉落 110% → 实际入账 110 经验、叶子 110 ×
 * 叶子获得量%；掉落 120%、获得量 110% → 100 经验变 132 叶子（叶子按实际入账经验结算）。
 * 扣经验（附魔等）不缩放也不结算，避免把扣的东西还回来。
 */
@Mixin(Player.class)
public abstract class LeafExperienceMixin {

	/** 只改方法自己的参数、只对入账（正值）生效，改完再落进原版逻辑。 */
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
