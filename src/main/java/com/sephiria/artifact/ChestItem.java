package com.sephiria.artifact;

import com.sephiria.backpack.ChestMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 宝箱类道具（赛菲利亚道具）：神器宝箱 / 石板宝箱 / 升级宝箱。
 *
 * <p>不可堆叠，手持右键打开「宝箱页面」：页面里有 {@value #REWARDS} 格随机奖励，选一件直接获得，
 * 宝箱本身消耗掉、那一栏被选中的东西填上。
 *
 * <p>三种箱子的区别只有奖池与权重：神器箱只出神器、石板箱只出石板、升级箱两者混着出且好东西更少。
 */
public class ChestItem extends Item {
	/** 宝箱页面的奖励格数。 */
	public static final int REWARDS = 5;

	/** 箱子种类：决定奖池与品质权重。 */
	public enum Kind {
		ARTIFACT(false, ArtifactLoot.Weights.DEFAULT),
		SLATE(true, ArtifactLoot.Weights.DEFAULT),
		UPGRADE(true, ArtifactLoot.Weights.UPGRADE);

		private final boolean withSlates;
		private final ArtifactLoot.Weights weights;

		Kind(boolean withSlates, ArtifactLoot.Weights weights) {
			this.withSlates = withSlates;
			this.weights = weights;
		}

		public boolean withSlates() {
			return this.withSlates;
		}

		public ArtifactLoot.Weights weights() {
			return this.weights;
		}
	}

	private final Kind kind;

	public ChestItem(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public Kind kind() {
		return this.kind;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.CONSUME;
		}

		serverPlayer.openMenu(ChestMenu.provider(serverPlayer, hand));
		return InteractionResult.CONSUME;
	}

	/** 箱子名字按品质上色（道具都是普通）。 */
	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.WHITE);
	}
}
