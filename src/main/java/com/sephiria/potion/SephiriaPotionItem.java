package com.sephiria.potion;

import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.artifact.Quality;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 赛菲利亚药水：用法和原版口服药水一样——长按右键喝，喝完留下一个玻璃瓶。
 *
 * <p>效果不走原版 buff 系统（见 {@link PotionEffect}）：喝下去由服务端直接结算，
 * 所以牛奶解不掉、也不占效果栏。客户端那边只负责播喝药的动作与音效。
 */
public class SephiriaPotionItem extends Item implements Quality {
	/** 喝一口要按住多久（tick）：和原版药水一样 1.6 秒。 */
	private static final int DRINK_TICKS = 32;

	private final PotionEffect effect;
	private final ArtifactRarity rarity;

	public SephiriaPotionItem(Properties properties, PotionEffect effect, ArtifactRarity rarity) {
		super(properties);
		this.effect = effect;
		this.rarity = rarity;
	}

	public PotionEffect effect() {
		return this.effect;
	}

	@Override
	public ArtifactRarity rarity() {
		return this.rarity;
	}

	/** 名字按品质上色（和神器、石板一致）。 */
	@Override
	public Component getName(ItemStack stack) {
		return this.rarity.style(Component.translatable(this.getDescriptionId()).getString());
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);

		if (!level.isClientSide()) {
			// 原版喝药的音效；粒子就不补了，反正只有一口
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
		}

		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.DRINK;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return DRINK_TICKS;
	}

	/** 喝完：服务端结算效果，手里换成空瓶子。 */
	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			this.effect.apply(player);
		}

		stack.shrink(1);
		return stack.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : stack;
	}
}
