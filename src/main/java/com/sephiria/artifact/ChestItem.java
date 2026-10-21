package com.sephiria.artifact;

import com.mojang.serialization.Codec;
import com.sephiria.Sephiria;
import com.sephiria.backpack.ChestMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 宝箱类道具（赛菲莉娅道具）：神器宝箱 / 石板宝箱 / 升级宝箱。
 *
 * <p>不可堆叠，手持右键打开「宝箱页面」：页面里有 {@value #REWARDS} 格随机奖励，选一件直接获得，
 * 宝箱本身消耗掉、那一栏被选中的东西填上。
 *
 * <p>三种箱子的区别只有奖池与权重：神器箱只出神器、石板箱只出石板、升级箱两者混着出且好东西更少。
 *
 * <p>奖励在<b>第一次打开时</b>抽好并写进物品组件（{@link #REWARDS}），之后关掉再打开还是同一批——
 * 否则玩家可以反复开关界面刷奖池。
 */
public class ChestItem extends Item {
	/** 宝箱页面的基础奖励格数。 */
	public static final int REWARDS = 5;
	/** 天赋「急智」二阶段之后的奖励格数上限（5 选 1 → 6 选 1）。 */
	public static final int MAX_REWARDS = 6;

	/** 这一箱给几选 1：基础 5，急智二阶段 +1。 */
	public static int choicesFor(ServerPlayer player) {
		return Math.min(MAX_REWARDS, REWARDS + com.sephiria.talent.Talents.chestChoices(player));
	}

	/** 这一箱抽到的奖励（第一次打开时写入）。 */
	public static final DataComponentType<List<ItemStack>> ROLLED = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "chest_rewards"),
			DataComponentType.<List<ItemStack>>builder()
					.persistent(ItemStack.CODEC.listOf())
					.networkSynchronized(ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()))
					.build());

	/**
	 * 这一箱的「免费刷新」用过了没有（天赋「智慧」三阶段每个宝箱免费刷新 1 次）。
	 *
	 * <p>存在宝箱物品上而不是菜单里：关掉界面再打开不能把免费次数刷回来。
	 */
	public static final DataComponentType<Boolean> FREE_REROLL_USED = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "chest_free_reroll_used"),
			DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	/**
	 * 箱子种类：决定奖池，以及「混着出」时石板占几成。
	 *
	 * <p>品质概率不缓存在这里：三种宝箱共用 {@link ArtifactLoot.WeightsKind#CHEST} 那一组比值，
	 * 而它可能被 config 覆盖（整合包），所以领奖时（{@link #rewardsOf}）从概率函数现拿。
	 */
	public enum Kind {
		/** 神器宝箱：只出神器。 */
		ARTIFACT(0.0D),
		/** 石板宝箱：只出石板。 */
		SLATE(100.0D),
		/** 升级宝箱：神器 60% / 石板 40%（用户 2026-10-18 定的口径）。 */
		UPGRADE(40.0D),
		/** 稀有神器宝箱：只出稀有及以上品质的神器（BOSS 掉落，2026-10-21）。 */
		RARE_ARTIFACT(0.0D),
		/** 稀有石板宝箱：只出稀有及以上品质的石板（BOSS 掉落，2026-10-21）。 */
		RARE_SLATE(100.0D);

		/** 混着抽时「这一件抽石板」的概率（%）：0 = 只出神器、100 = 只出石板。 */
		private final double slatePercent;

		Kind(double slatePercent) {
			this.slatePercent = slatePercent;
		}

		public double slatePercent() {
			return this.slatePercent;
		}

		/** 只抽稀有及以上（稀有 / 传说 / 羁绊 / 永恒），普通与高级不出。 */
		public boolean rareOnly() {
			return this == RARE_ARTIFACT || this == RARE_SLATE;
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

	/**
	 * 这一箱的奖励：已经抽过就直接返回存着的那批，没抽过才抽并写回物品。
	 *
	 * <p>写进物品组件而不是每次开界面重抽，是为了「不能关掉再打开刷奖池」。
	 *
	 * @param luck 玩家的幸运值（点数）：稀有及以上的概率按曲线放大，见 {@link ArtifactLoot#luckPercent}
	 * @param choices 这一箱开几选 1（基础 5；急智二阶段 6）
	 */
	public static List<ItemStack> rewardsOf(ItemStack chest, RandomSource random, Kind kind, double luck,
			int choices) {
		List<ItemStack> stored = chest.get(ROLLED);

		if (stored != null) {
			return stored;
		}

		// 幸运值：稀有及以上的概率按曲线放大（用户 2026-10-18 的「幸运值」口径）
		ArtifactLoot.Weights weights = ArtifactLoot.weightsFor(ArtifactLoot.WeightsKind.CHEST, luck);
		int count = Math.min(Math.max(1, choices), Math.min(MAX_REWARDS, ArtifactLoot.MAX_ROLL));

		// 稀有宝箱：池子先过滤成稀有及以上，再按同一组品质权重抽；普通 / 高级根本不在池里
		List<ItemStack> rolled = kind.rareOnly()
				? List.copyOf(ArtifactLoot.rollFrom(random, count,
						ArtifactLoot.rarePool(kind == Kind.RARE_SLATE), weights))
				: List.copyOf(ArtifactLoot.rollMixed(random, count, kind.slatePercent(), weights));
		chest.set(ROLLED, rolled);
		return rolled;
	}

	/**
	 * 刷新这一箱：丢掉旧的一批重新抽（天赋「智慧」三阶段的免费刷新 / 骰子刷新都走这里）。
	 *
	 * @return 新的一批奖励
	 */
	public static List<ItemStack> reroll(ItemStack chest, RandomSource random, Kind kind, double luck,
			int choices) {
		chest.remove(ROLLED);
		return rewardsOf(chest, random, kind, luck, choices);
	}

	/** 这一箱的免费刷新用过了吗。 */
	public static boolean freeRerollUsed(ItemStack chest) {
		return Boolean.TRUE.equals(chest.get(FREE_REROLL_USED));
	}

	/** 标成「免费刷新已用」。 */
	public static void markFreeRerollUsed(ItemStack chest) {
		chest.set(FREE_REROLL_USED, true);
	}
}
