package com.sephiria.wishpool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.registry.ModItems;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 许愿池的规则与玩家状态：<b>能量</b>、<b>锚定</b>（神器池 / 石板池各一个），以及抽卡与神铸的换算。
 *
 * <p>用户口径（2026-10-15）：
 * <ul>
 *   <li>抽卡<b>概率与商店一致</b>（{@link ArtifactLoot.Weights#SHOP}）；</li>
 *   <li><b>锚定</b>一件神器 / 石板之后，掷到<b>它所属品质</b>时有一半概率直接给锚定件
 *       （「抽到一堆品质为黄的物品时，其中有 50% 左右为锚定的神器或石板」）；</li>
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
	/** 锚定件在「掷到它那一档」时被抽中的概率（50%，用户口径）。 */
	public static final double ANCHOR_SHARE = 0.5D;

	/**
	 * 存档：四叶草 + 能量 + 两个锚定件（锚定存<b>物品 id 字符串</b>，不存数字 id——数字 id 会随注册表变动）。
	 *
	 * <p><b>四叶草是数值不是物品</b>（用户 2026-10-15 的口径）：与能量一样存在这里，不占背包格子。
	 */
	public record Saved(int clovers, int energy, String artifactAnchor, String slateAnchor) {
		public static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("clovers", 0).forGetter(Saved::clovers),
				Codec.INT.optionalFieldOf("energy", 0).forGetter(Saved::energy),
				Codec.STRING.optionalFieldOf("artifact_anchor", "").forGetter(Saved::artifactAnchor),
				Codec.STRING.optionalFieldOf("slate_anchor", "").forGetter(Saved::slateAnchor)
		).apply(instance, Saved::new));
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

	/** 加四叶草（许愿池吸掉升级宝箱时调）。 */
	public static void addClovers(ServerPlayer player, int delta) {
		Saved saved = saved(player);
		player.setAttached(SAVED, new Saved(Math.max(0, saved.clovers() + delta), saved.energy(),
				saved.artifactAnchor(), saved.slateAnchor()));
	}

	/** 扣四叶草；不够就返回 false（什么都不扣）。 */
	public static boolean consumeClovers(ServerPlayer player, int count) {
		if (clovers(player) < count) {
			return false;
		}

		addClovers(player, -count);
		return true;
	}

	/** 某个池子的锚定件（没锚定返回 null）。 */
	public static Item anchorOf(ServerPlayer player, boolean slatePool) {
		Saved saved = player.getAttached(SAVED);

		if (saved == null) {
			return null;
		}

		String id = slatePool ? saved.slateAnchor() : saved.artifactAnchor();
		return id.isEmpty() ? null : byId(id);
	}

	/** 设置 / 清除某个池子的锚定件（传 null 就是取消锚定）。 */
	public static void setAnchor(ServerPlayer player, boolean slatePool, Item anchor) {
		Saved saved = saved(player);
		String id = anchor == null ? "" : BuiltInRegistries.ITEM.getKey(anchor).toString();

		player.setAttached(SAVED, new Saved(saved.clovers(), saved.energy(),
				slatePool ? saved.artifactAnchor() : id,
				slatePool ? id : saved.slateAnchor()));
	}

	/** 改能量（神铸扣、抽卡加都走它；不会扣成负数）。 */
	public static void addEnergy(ServerPlayer player, int delta) {
		Saved saved = saved(player);
		player.setAttached(SAVED, new Saved(saved.clovers(), Math.max(0, saved.energy() + delta),
				saved.artifactAnchor(), saved.slateAnchor()));
	}

	/** 界面同步用：锚定件转成注册表数字 id（0 = 没锚定，因为 0 号是空气）。 */
	public static int anchorNetworkId(ServerPlayer player, boolean slatePool) {
		Item anchor = anchorOf(player, slatePool);
		return anchor == null ? 0 : BuiltInRegistries.ITEM.getId(anchor);
	}

	private static Saved saved(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);
		return saved == null ? new Saved(0, 0, "", "") : saved;
	}

	private static Item byId(String id) {
		Identifier key = Identifier.tryParse(id);
		return key == null ? null : BuiltInRegistries.ITEM.getOptional(key).orElse(null);
	}

	// ------------------------------------------------------------------ 换算表

	/** 抽到某一档给多少能量：白 1 / 蓝 2 / 黄 4 / 红 8 / 彩 10（石板最高的「永恒」按彩那一档）。 */
	public static int energyOf(ArtifactRarity rarity) {
		return switch (rarity) {
			case COMMON -> 1;
			case ADVANCED -> 2;
			case RARE -> 4;
			case LEGENDARY -> 8;
			case BOND, ETERNAL -> 10;
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
	 * 抽一件：先按商店的权重掷品质，再在那一档里随机挑一件；<b>锚定</b>只影响「掷到它那一档」的那一半。
	 *
	 * <p>同一批里允许重复（这是抽卡，也是锚定能刷同一件的意义所在）。
	 */
	public static ItemStack drawOne(RandomSource random, boolean slatePool, Item anchor) {
		List<Item> pool = poolOf(slatePool);
		ArtifactRarity rarity = ArtifactLoot.rollRarity(random, pool, ArtifactLoot.Weights.SHOP);

		if (rarity == null) {
			return ItemStack.EMPTY;
		}

		if (anchor != null && ArtifactLoot.rarityOf(anchor) == rarity && random.nextDouble() < ANCHOR_SHARE) {
			return new ItemStack(anchor);
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
		Item anchor = anchorOf(player, slatePool);
		List<ItemStack> drawn = new ArrayList<>(count);
		int gained = 0;

		for (int index = 0; index < count; index++) {
			ItemStack stack = drawOne(random, slatePool, anchor);

			if (stack.isEmpty()) {
				continue;
			}

			gained += energyOf(ArtifactLoot.rarityOf(stack.getItem()));
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
