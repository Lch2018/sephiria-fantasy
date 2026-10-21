package com.sephiria.wishpool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.registry.ModItems;
import com.sephiria.slate.SlateItem;
import com.sephiria.stats.PlayerStats;
import com.sephiria.talent.Talent;
import com.sephiria.talent.Talents;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 许愿池的规则与玩家状态：<b>能量</b>、<b>锚定</b>（神器池 / 石板池各一组锚定格），以及抽卡与神铸的换算。
 *
 * <p>用户口径：
 * <ul>
 *   <li>抽卡<b>概率与商店一致</b>（{@link ArtifactLoot.Weights#SHOP}）；</li>
 *   <li><b>锚定</b>：默认 1 个锚定格（意志天赋最多加到 5 个），同一件可以占多格。
 *       掷到某一档时，这一档的锚定物共用一个占比
 *       {@code 50% × (1 + 品质增量% × 总次数)}（白 5 / 蓝 10 / 黄 15 / 红与彩 20，最多 100%），
 *       命中后按各物品的次数比例分；没命中才在整档里随机；</li>
 *   <li>抽卡积蓄<b>能量</b>：白 1 / 蓝 2 / 黄 4 / 红 8 / 彩 10（石板最高是「永恒」，按彩那一档给）；</li>
 *   <li><b>神铸</b>：只能换<b>黄与红</b>两档，黄 80 / 红 200 能量；</li>
 *   <li>「许愿池等级越高、能锚定 / 能换的品质越高」这条<b>暂时不做</b>（用户明说）。</li>
 * </ul>
 *
 * <p>与「根之退」不同：许愿池方块<b>不存任何东西</b>——它只是打开界面的入口与四叶草的来源，
 * 能量与锚定都是玩家自己的（数据附件，跟存档走、死亡不清零）。
 */
public final class WishPool {
	/** 抽一次要几片四叶草。 */
	public static final int CLOVERS_PER_DRAW = 1;
	/** 十连抽几次。 */
	public static final int TEN_DRAW = 10;
	/** 锚定件在「掷到它那一档」时的基础占比（50%，用户口径）。 */
	public static final double ANCHOR_SHARE = 0.5D;
	/** 默认锚定格数（意志一 / 三阶段各往上加）。 */
	public static final int DEFAULT_ANCHOR_SLOTS = 1;
	/** 锚定格上限（意志点满：默认 1 + 一阶段 1 + 三阶段 3 = 5）。 */
	public static final int MAX_ANCHOR_SLOTS = 5;

	/**
	 * 存档：四叶草 + 能量 + 两个卡池的锚定件列表 + 「溢出转化」开关。
	 *
	 * <p>锚定存<b>物品 id 字符串</b>（不存数字 id——数字 id 会随注册表变动）。列表的每一项就是
	 * 一个锚定格；同一件物品可以占多格，格数就是它的「锚定次数」。
	 *
	 * <p>{@code artifact_anchor} / {@code slate_anchor} 是老存档的单锚定字段：只读，读到非空且
	 * 新列表为空时当成一项迁移；写新列表时把老字段清空，避免清空锚定后它又冒出来。
	 */
	public record Saved(int clovers, int energy, List<String> artifactAnchors, List<String> slateAnchors,
			boolean overflowClover, String legacyArtifactAnchor, String legacySlateAnchor) {
		public static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("clovers", 0).forGetter(Saved::clovers),
				Codec.INT.optionalFieldOf("energy", 0).forGetter(Saved::energy),
				Codec.STRING.listOf().optionalFieldOf("artifact_anchors", List.of())
						.forGetter(Saved::artifactAnchors),
				Codec.STRING.listOf().optionalFieldOf("slate_anchors", List.of()).forGetter(Saved::slateAnchors),
				// 「溢出的升级宝箱自动变四叶草」开关（许愿池界面里那个）。老存档没有这个字段、
				// 默认关；只加可选字段、不动已有的键名，附件才读得回来（见记忆手册 §三 那条硬规矩）
				Codec.BOOL.optionalFieldOf("overflow_clover", false).forGetter(Saved::overflowClover),
				Codec.STRING.optionalFieldOf("artifact_anchor", "").forGetter(Saved::legacyArtifactAnchor),
				Codec.STRING.optionalFieldOf("slate_anchor", "").forGetter(Saved::legacySlateAnchor)
		).apply(instance, Saved::new));

		static final Saved EMPTY = new Saved(0, 0, List.of(), List.of(), false, "", "");

		/** 某个卡池的锚定件 id 列表（新列表优先；新列表为空时用老单锚定字段迁移）。 */
		public List<String> anchors(boolean slatePool) {
			List<String> list = slatePool ? this.slateAnchors : this.artifactAnchors;

			if (!list.isEmpty()) {
				return list;
			}

			String legacy = slatePool ? this.legacySlateAnchor : this.legacyArtifactAnchor;
			return legacy.isEmpty() ? List.of() : List.of(legacy);
		}

		/** 只换四叶草 / 能量，锚定与开关原样带上。 */
		public Saved with(int clovers, int energy) {
			return new Saved(clovers, energy, this.artifactAnchors, this.slateAnchors, this.overflowClover,
					this.legacyArtifactAnchor, this.legacySlateAnchor);
		}

		/** 换某个卡池的锚定列表（同时清掉该池的老单锚定字段）。 */
		public Saved withAnchors(boolean slatePool, List<String> anchors) {
			return slatePool
					? new Saved(this.clovers, this.energy, this.artifactAnchors, List.copyOf(anchors),
							this.overflowClover, this.legacyArtifactAnchor, "")
					: new Saved(this.clovers, this.energy, List.copyOf(anchors), this.slateAnchors,
							this.overflowClover, "", this.legacySlateAnchor);
		}

		/** 翻转「溢出转化」开关。 */
		public Saved withOverflow(boolean overflow) {
			return new Saved(this.clovers, this.energy, this.artifactAnchors, this.slateAnchors, overflow,
					this.legacyArtifactAnchor, this.legacySlateAnchor);
		}
	}

	private static final AttachmentType<Saved> SAVED = AttachmentRegistry.<Saved>builder()
			.persistent(Saved.CODEC)
			// 抽卡攒的能量与锚定是玩家自己的东西，死了不该清零
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "wish_pool"));

	private WishPool() {
	}

	/** 显式初始化入口（由 {@link Sephiria#onInitialize()} 调用；附件类型写在静态字段里，加载类即注册）。 */
	public static void register() {
	}

	// ------------------------------------------------------------------ 状态

	public static int energy(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);
		return saved == null ? 0 : Math.max(0, saved.energy());
	}

	/** 手上的四叶草（数值，不是物品）。 */
	public static int clovers(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);
		return saved == null ? 0 : Math.max(0, saved.clovers());
	}

	/** 加四叶草（许愿池吸掉升级宝箱、成就结算、溢出转化时调）。 */
	public static void addClovers(ServerPlayer player, int delta) {
		Saved saved = saved(player);
		player.setAttached(SAVED, saved.with(Math.max(0, saved.clovers() + delta), saved.energy()));
	}

	/**
	 * 「溢出的升级宝箱自动转化为四叶草」开关（许愿池界面里那一行，默认关）。
	 *
	 * <p>开着的时候，背包满、本来是"掉在脚下"的升级宝箱会直接变成一片四叶草——
	 * 判断在 {@code PlayerStats.addLeaves} 发宝箱那一步。
	 */
	public static boolean overflowToClover(ServerPlayer player) {
		return saved(player).overflowClover();
	}

	/** 翻转开关，返回翻转后的值。 */
	public static boolean toggleOverflowToClover(ServerPlayer player) {
		Saved saved = saved(player);
		boolean next = !saved.overflowClover();
		player.setAttached(SAVED, saved.withOverflow(next));
		return next;
	}

	/** 扣四叶草；不够就返回 false（什么都不扣）。 */
	public static boolean consumeClovers(ServerPlayer player, int count) {
		if (clovers(player) < count) {
			return false;
		}

		addClovers(player, -count);
		return true;
	}

	// ------------------------------------------------------------------ 锚定

	/** 这个玩家有几个锚定格：默认 1 + 意志天赋的加成（最多 5）。 */
	public static int anchorCapacity(ServerPlayer player) {
		return Math.min(MAX_ANCHOR_SLOTS, DEFAULT_ANCHOR_SLOTS + Talents.anchorSlotBonus(player));
	}

	/**
	 * 某个池子当前的锚定件 id 列表，最多 {@link #anchorCapacity} 项。
	 *
	 * <p>会滤掉已经不在池子里的物品（例如以后删掉的石板），并按格子数裁掉超出的部分——
	 * 意志天赋被重置后，多出来的锚定格暂时不生效，但存档里仍然留着；再点回天赋就又能看到。
	 */
	public static List<String> anchorIds(ServerPlayer player, boolean slatePool) {
		Saved saved = saved(player);
		List<String> all = saved.anchors(slatePool);
		List<String> result = new ArrayList<>(Math.min(anchorCapacity(player), all.size()));
		List<Item> pool = poolOf(slatePool);

		for (String id : all) {
			if (result.size() >= anchorCapacity(player)) {
				break;
			}

			Item item = byId(id);

			if (item != null && pool.contains(item)) {
				result.add(id);
			}
		}

		return List.copyOf(result);
	}

	/** 某个池子当前的锚定件（可以重复：同一个物品占多格就是锚定多次）。 */
	public static List<Item> anchors(ServerPlayer player, boolean slatePool) {
		List<Item> result = new ArrayList<>();

		for (String id : anchorIds(player, slatePool)) {
			Item item = byId(id);

			if (item != null) {
				result.add(item);
			}
		}

		return result;
	}

	/** 某个物品在这个池子里被锚定了几次。 */
	public static int anchorCount(ServerPlayer player, boolean slatePool, Item item) {
		int count = 0;

		for (Item anchored : anchors(player, slatePool)) {
			if (anchored == item) {
				count++;
			}
		}

		return count;
	}

	/**
	 * 增加一次锚定：填到第一个空锚定格。
	 *
	 * <p>同一件物品可以重复锚定（占多格），这正是「锚定相同神器/石板时占比增加」的来源。
	 * 锚定格满了就什么都不做；要换锚定先点卡池页的锚定格取消一个。
	 */
	public static void addAnchor(ServerPlayer player, boolean slatePool, Item item) {
		if (item == null || !poolOf(slatePool).contains(item)) {
			return;
		}

		List<String> ids = new ArrayList<>(anchorIds(player, slatePool));

		if (ids.size() >= anchorCapacity(player)) {
			return;
		}

		ids.add(BuiltInRegistries.ITEM.getKey(item).toString());
		player.setAttached(SAVED, saved(player).withAnchors(slatePool, ids));
	}

	/** 清空某个池子的全部锚定。 */
	public static void clearAnchors(ServerPlayer player, boolean slatePool) {
		player.setAttached(SAVED, saved(player).withAnchors(slatePool, List.of()));
	}

	/** 清掉某个池子的第 {@code index} 个锚定格（0 起）。 */
	public static void clearAnchor(ServerPlayer player, boolean slatePool, int index) {
		List<String> ids = new ArrayList<>(anchorIds(player, slatePool));

		if (index < 0 || index >= ids.size()) {
			return;
		}

		ids.remove(index);
		player.setAttached(SAVED, saved(player).withAnchors(slatePool, ids));
	}

	/** 界面同步用：某个锚定格转成注册表数字 id（0 = 空）。 */
	public static int anchorNetworkId(ServerPlayer player, boolean slatePool, int slot) {
		List<String> ids = anchorIds(player, slatePool);

		if (slot < 0 || slot >= ids.size()) {
			return 0;
		}

		Item item = byId(ids.get(slot));
		return item == null ? 0 : BuiltInRegistries.ITEM.getId(item);
	}

	/**
	 * 某品质的锚定物每多一次锚定的占比增量（%）。
	 *
	 * <p>用户 2026-10-21 的口径：白 5 / 蓝 10 / 黄 15 / 红与彩（粉色）20。
	 */
	public static double anchorStepPercent(ArtifactRarity rarity) {
		return switch (rarity) {
			case COMMON -> 5.0D;
			case ADVANCED -> 10.0D;
			case RARE -> 15.0D;
			case LEGENDARY, BOND, ETERNAL -> 20.0D;
		};
	}

	/**
	 * 这一档的锚定物一共占多少（0..1）：总锚定次数 x 与品质增量 p，
	 * {@code y = 50% × (1 + p% × x)}。
	 *
	 * <p>「总锚定次数」是这一档里所有锚定物的次数之和，不是一个物品单独算——
	 * 命中后再按各物品的次数比例分这块占比（见 {@link #drawOne}）。
	 */
	public static double anchorShare(ArtifactRarity rarity, int totalCount) {
		if (totalCount <= 0) {
			return 0.0D;
		}

		return Math.min(1.0D,
				ANCHOR_SHARE * (1.0D + anchorStepPercent(rarity) / 100.0D * totalCount));
	}

	/** 改能量（神铸扣、抽卡加都走它；不会扣成负数）。 */
	public static void addEnergy(ServerPlayer player, int delta) {
		Saved saved = saved(player);
		player.setAttached(SAVED, saved.with(saved.clovers(), Math.max(0, saved.energy() + delta)));
	}

	private static Saved saved(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);
		return saved == null ? Saved.EMPTY : saved;
	}

	private static Item byId(String id) {
		Identifier key = Identifier.tryParse(id);
		return key == null ? null : BuiltInRegistries.ITEM.getOptional(key).orElse(null);
	}

	// ------------------------------------------------------------------ 换算表

	/**
	 * 抽到某一件给多少愿力：神器与石板**各一套**（用户 2026-10-17 的口径）。
	 *
	 * <p>以前是一张共用表（白 1 / 蓝 2 / 黄 4 / 红 8 / 彩 10），现在神器 2/4/6/8/10、
	 * 石板 3/6/9/12/15。
	 */
	public static int energyFor(Item item) {
		return item instanceof SlateItem ? slateEnergyOf(ArtifactLoot.rarityOf(item))
				: artifactEnergyOf(ArtifactLoot.rarityOf(item));
	}

	/** 神器那套：白 2 / 蓝 4 / 黄 6 / 红 8 / 羁绊 10。 */
	public static int artifactEnergyOf(ArtifactRarity rarity) {
		return switch (rarity) {
			case COMMON -> 2;
			case ADVANCED -> 4;
			case RARE -> 6;
			case LEGENDARY -> 8;
			case BOND, ETERNAL -> 10;
		};
	}

	/** 石板那套：白 3 / 蓝 6 / 黄 9 / 红 12 / 永恒 15。 */
	public static int slateEnergyOf(ArtifactRarity rarity) {
		return switch (rarity) {
			case COMMON -> 3;
			case ADVANCED -> 6;
			case RARE -> 9;
			case LEGENDARY -> 12;
			case BOND, ETERNAL -> 15;
		};
	}

	/** 神铸换一件要多少能量；<b>只有黄与红能换</b>（其它档返回 0 = 换不了）。 */
	public static int forgeCost(ArtifactRarity rarity) {
		return switch (rarity) {
			case RARE -> 80;
			case LEGENDARY -> 200;
			default -> 0;
		};
	}

	// ------------------------------------------------------------------ 抽卡

	/** 某个池子的物品清单。 */
	public static List<Item> poolOf(boolean slatePool) {
		return slatePool ? ArtifactLoot.slatePool() : ArtifactLoot.artifactPool();
	}

	/**
	 * 抽一件：先按商店的权重掷品质，再在那一档里挑一件。
	 *
	 * <p><b>锚定</b>只影响「掷到它那一档」：对每个在这一档里的锚定物品，按它被锚定的次数
	 * {@code x} 算占比 {@code y = 50% × (1 + 20%x)}（5 次 = 100%）。多个不同锚定物同时存在时，
	 * 它们的占比相加（上限 100%），命中的话按各自占比加权挑一个；都没命中才在整档里随机。
	 * 所以只锚定一件物品时，行为就是用户给的公式。
	 *
	 * <p>同一批里允许重复（这是抽卡，也是锚定能刷同一件的意义所在）。
	 */
	public static ItemStack drawOne(RandomSource random, boolean slatePool, List<Item> anchors, double luck) {
		List<Item> pool = poolOf(slatePool);
		// 幸运值：稀有及以上的概率按曲线放大（用户 2026-10-18 的「幸运值」口径）
		ArtifactRarity rarity = ArtifactLoot.rollRarity(random, pool, ArtifactLoot.weightsFor(ArtifactLoot.WeightsKind.GACHA, luck));

		if (rarity == null) {
			return ItemStack.EMPTY;
		}

		// 这一档里被锚定的物品 → 锚定次数（同一件占多格就累加）
		Map<Item, Integer> counts = new LinkedHashMap<>();
		int totalCount = 0;

		for (Item anchor : anchors) {
			// 锚定只能锚池子里的东西：天国这类「不进任何掉落池」的特殊物品即使拿在手里也锚不出来
			if (anchor != null && pool.contains(anchor) && ArtifactLoot.rarityOf(anchor) == rarity) {
				counts.merge(anchor, 1, Integer::sum);
				totalCount++;
			}
		}

		if (totalCount > 0) {
			// 这一档的锚定物共用一个占比：y = 50% × (1 + 品质增量% × 总次数)，
			// 命中后按各物品的次数比例分（红 a2 + b3 → 100%，a 2/5、b 3/5）
			double share = anchorShare(rarity, totalCount);

			if (random.nextDouble() < share) {
				double roll = random.nextDouble() * totalCount;
				Item last = null;

				for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
					last = entry.getKey();

					if (roll < entry.getValue()) {
						return new ItemStack(entry.getKey());
					}

					roll -= entry.getValue();
				}

				// 浮点误差兜底：落到最后一个锚定物上
				return new ItemStack(last);
			}
		}

		List<Item> tier = ArtifactLoot.ofRarity(pool, rarity);

		if (tier.isEmpty()) {
			return ItemStack.EMPTY;
		}

		return new ItemStack(tier.get(random.nextInt(tier.size())));
	}

	/**
	 * 抽 {@code count} 件并把这一批的能量记到玩家头上（服务端调用）。
	 *
	 * <p>返回的就是要弹出去 / 播动画的那一批（空的那几件是异常兜底，正常不会出现）。
	 */
	public static List<ItemStack> draw(ServerPlayer player, boolean slatePool, int count) {
		RandomSource random = player.getRandom();
		List<Item> anchors = anchors(player, slatePool);
		List<ItemStack> drawn = new ArrayList<>(count);
		int gained = 0;

		for (int index = 0; index < count; index++) {
			ItemStack stack = drawOne(random, slatePool, anchors, PlayerStats.luckTotal(player));

			if (stack.isEmpty()) {
				continue;
			}

			gained += energyFor(stack.getItem());
			drawn.add(stack);
		}

		addEnergy(player, gained);
		return drawn;
	}

	// ------------------------------------------------------------------ 四叶草

	/**
	 * 许愿池吸掉一个升级宝箱时调：四叶草 +1。
	 *
	 * <p>四叶草是<b>数值</b>（用户 2026-10-15 的口径），不进背包、不占格子，只在这里记账。
	 */
	public static void giveClover(ServerPlayer player) {
		addClovers(player, 1);
	}
}
