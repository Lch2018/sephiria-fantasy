package com.sephiria.artifact;

import com.mojang.serialization.Codec;
import com.sephiria.Sephiria;
import com.sephiria.backpack.ChestMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 宝箱类道具（赛菲莉娅道具）：神器宝箱 / 石板宝箱 / 升级宝箱。
 *
 * <p><b>没开过的箱子可以叠 16 个</b>（用户 2026-11-01 的口径：神器 / 升级 / 石板 / 稀有神器 / 稀有石板
 * 五种宝箱都算）；一旦打开过（抽过奖励 / 用过免费刷新 / 带 {@link #OPENED} 标记）就恢复成一次一个。
 *
 * <p>堆叠靠原版 <b>max_stack_size 数据组件</b>：物品默认 16，打开时把这一份的组件改成 1，
 * 同时加上 {@link #OPENED}。这样合并时组件不同、开过的也叠不起来，shift-click 也不会把
 * 开过的和没开过的并成一堆（原版合并判的是「同物品 + 同组件」）。
 *
 * <p>手持右键打开「宝箱页面」：页面里有 {@value #REWARDS} 格随机奖励，选一件直接获得，
 * 宝箱本身消耗掉、那一栏被选中的东西填上。打开时从手里<b>取出一个</b>交给菜单持有，
 * 剩下的同种箱子仍是没开过的。
 *
 * <p>奖励在<b>第一次打开时</b>抽好并写进物品组件（{@link #ROLLED}），之后关掉再打开还是同一批——
 * 否则玩家可以反复开关界面刷奖池。
 */
public class ChestItem extends Item {
	/** 没开过的宝箱最多叠几个。 */
	public static final int MAX_STACK = 16;
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
	 * 这一份宝箱「已经打开过」（不管选没选）。
	 *
	 * <p>为什么不只靠 {@link #ROLLED}：奖励为空时（池子异常 / 以后加别的箱子）ROLLED 可能不写；
	 * 而且老存档里已经开过的箱子也没有这个标记。打开时标记 + 把这一份的 max_stack_size 改成 1，
	 * 两条一起保证开过的箱子永远和没开过的分开。
	 */
	public static final DataComponentType<Boolean> OPENED = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "chest_opened"),
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

		ItemStack held = serverPlayer.getItemInHand(hand);
		ItemStack opened = held.copyWithCount(1);
		// 先扣掉手里的这一份：菜单持有的是单独的一份，剩下的同种箱子仍是「没开过」、还能叠
		held.shrink(1);
		serverPlayer.getInventory().setChanged();
		// 开过的这一份改成不可堆叠（max_stack_size=1），这次打开就算后面是空奖励也已经标记了
		markOpened(opened);
		serverPlayer.openMenu(ChestMenu.provider(serverPlayer, opened));
		return InteractionResult.CONSUME;
	}

	/**
	 * 标记这一份宝箱已经打开：加 {@link #OPENED}，并把这一份的最大堆叠改成 1。
	 *
	 * <p>原版 {@code ItemStack#getMaxStackSize} 读的是 max_stack_size 数据组件，改这一份的组件
	 * 就只影响这一份——同一个物品的其它箱子仍然是 16。
	 */
	public static ItemStack markOpened(ItemStack chest) {
		chest.set(OPENED, true);
		chest.set(DataComponents.MAX_STACK_SIZE, 1);
		return chest;
	}

	/**
	 * 这一份宝箱是否已经打开过（老存档没有 {@link #OPENED} 时，有奖励表或免费刷新标记也算）。
	 *
	 * <p>只用于展示 / 判断，真正的堆叠上限由 max_stack_size 组件管。
	 */
	public static boolean opened(ItemStack chest) {
		return Boolean.TRUE.equals(chest.get(OPENED))
				|| chest.get(ROLLED) != null
				|| Boolean.TRUE.equals(chest.get(FREE_REROLL_USED));
	}

	/**
	 * 背包里每刻检查一次：老存档里「已经开过、但还没有 max_stack_size 组件」的宝箱
	 * 在这里补上 1，让它和更新后的开箱行为一致（不再能叠）。
	 *
	 * <p>只在服务端主背包 tick 里做；已经标记过的是空操作，开销可以忽略。
	 */
	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
		if (opened(stack) && !Integer.valueOf(1).equals(stack.get(DataComponents.MAX_STACK_SIZE))) {
			stack.set(DataComponents.MAX_STACK_SIZE, 1);
		}
	}

	/** 箱子名字按品质上色：普通箱稀有（黄），稀有箱传说（红）。 */
	@Override
	public Component getName(ItemStack stack) {
		return ItemQualities.nameOf(this);
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
			int choices, double magicBookRewardChance) {
		List<ItemStack> stored = chest.get(ROLLED);

		if (stored != null) {
			return stored;
		}

		// 幸运值：稀有及以上的概率按曲线放大（用户 2026-10-18 的「幸运值」口径）
		ArtifactLoot.Weights weights = ArtifactLoot.weightsFor(ArtifactLoot.WeightsKind.CHEST, luck);
		int count = Math.min(Math.max(1, choices), Math.min(MAX_REWARDS, ArtifactLoot.MAX_ROLL));
		double magicBookMultiplier = 1.0D + magicBookRewardChance / 100.0D;

		// 稀有宝箱：池子先过滤成稀有及以上，再按同一组品质权重抽；普通 / 高级根本不在池里
		List<ItemStack> rolled = kind.rareOnly()
				? List.copyOf(ArtifactLoot.rollFrom(random, count,
						ArtifactLoot.rarePool(kind == Kind.RARE_SLATE), weights, magicBookMultiplier))
				: List.copyOf(ArtifactLoot.rollMixed(random, count, kind.slatePercent(), weights,
						magicBookMultiplier));
		chest.set(ROLLED, rolled);
		return rolled;
	}

	/**
	 * 刷新这一箱：丢掉旧的一批重新抽（天赋「智慧」三阶段的免费刷新 / 骰子刷新都走这里）。
	 *
	 * @return 新的一批奖励
	 */
	public static List<ItemStack> reroll(ItemStack chest, RandomSource random, Kind kind, double luck,
			int choices, double magicBookRewardChance) {
		chest.remove(ROLLED);
		return rewardsOf(chest, random, kind, luck, choices, magicBookRewardChance);
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
