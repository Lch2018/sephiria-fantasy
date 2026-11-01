package com.sephiria.card;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.network.CardSelectionPayload;
import com.sephiria.util.PlayerItems;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 「选卡」的规则与玩家状态。
 *
 * <p>用户口径：
 * <ul>
 *   <li>一次给三张卡，只能选一张；三张<b>同一品质</b>，品质白 / 蓝 / 黄 / 红各 1/4；</li>
 *   <li>只出神器，不出石板，也不出羁绊 / 永恒；</li>
 *   <li>每张卡各有一个刷新按钮，每次选卡每张卡能刷一次；刷新后仍是神器，
 *       有 5% 概率品质升一档（红不再升）；</li>
 *   <li>每 2 个游戏日（48000 刻）获得 1 次选卡机会，首次创建为 1 次；</li>
 *   <li>不选就关掉不扣次数，下次打开重新发三张；选一张就扣次数并把那张神器给玩家。</li>
 * </ul>
 *
 * <p>「选卡机会」与「上次发机会的时间」存在玩家数据附件里（跟存档走、死亡不清零）；
 * 当前这三张卡是<b>临时</b>的，只存在服务端内存里，客户端看到的永远是服务端发过去的那一份
 * ——刷新 / 选择都只是请求，实际掷点全在服务端，客户端没法伪造卡面。
 *
 * <p>自动打开：Fabric 26.3 没有现成的「睡醒」事件，这里用一个服务端 tick 的
 * <b>睡眠状态翻转检测</b>：上一刻在睡、这一刻不在睡、且天已经亮了（正常睡满一夜）才弹窗；
 * 半夜被怪打断不会弹。
 */
public final class CardSelection {
	/** 一次给几张卡。 */
	public static final int CARDS = 3;
	/** 首次拿到这个系统的玩家有几张选卡机会（用户口径：初始 1 次）。 */
	public static final int INITIAL_CHANCES = 1;
	/**
	 * 最多攒几次机会。
	 *
	 * <p>用户要求「按经过的完整 2 日周期给，但要有个合理上限」：这里是 10 次，
	 * 相当于最多攒 20 个游戏日；再多也不会继续累加。
	 */
	public static final int MAX_CHANCES = 10;
	/** 每 2 个游戏日加一次机会：2 × 24000 刻。 */
	public static final long AWARD_PERIOD_TICKS = 2L * 24000L;
	/**
	 * 一次结算最多补几次。
	 *
	 * <p>玩家离线很久（比如几十天）时，一次 tick 里最多补 5 次，超出的部分直接作废——
	 * 既不让人挂机离线刷满，也避免一上线就弹出几十次。
	 */
	public static final int MAX_AWARDS_PER_CHECK = 5;
	/** 刷新时品质升一档的概率（用户口径 5%）。 */
	public static final double UPGRADE_RARITY_CHANCE = 0.05D;

	/** 初始品质只取这四档（不含羁绊 / 永恒），每档概率相同 = 1/4。 */
	private static final ArtifactRarity[] BASE_RARITIES = {
			ArtifactRarity.COMMON, ArtifactRarity.ADVANCED, ArtifactRarity.RARE, ArtifactRarity.LEGENDARY
	};

	/**
	 * 存档：还剩几次选卡 + 上次发机会时的游戏刻。
	 *
	 * <p>两个字段都带默认值，老存档（或以后加字段）读不出来也不会整份附件报废。
	 */
	public record Saved(int chances, long lastAward) {
		public static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("chances", INITIAL_CHANCES).forGetter(Saved::chances),
				Codec.LONG.optionalFieldOf("last_award", 0L).forGetter(Saved::lastAward)
		).apply(instance, Saved::new));
	}

	private static final AttachmentType<Saved> SAVED = AttachmentRegistry.<Saved>builder()
			.persistent(Saved.CODEC)
			// 选卡机会是玩家自己的东西，死了不该清零
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "card_selection"));

	/** 当前未选的三张卡与刷新状态：持久化，关界面/重进游戏都不重掷。 */
	private record SessionSaved(List<ItemStack> cards, List<Boolean> refreshed) {
		private static final Codec<SessionSaved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ItemStack.CODEC.listOf().optionalFieldOf("cards", List.of()).forGetter(SessionSaved::cards),
				Codec.BOOL.listOf().optionalFieldOf("refreshed", List.of()).forGetter(SessionSaved::refreshed)
		).apply(instance, SessionSaved::new));
	}

	private static final AttachmentType<SessionSaved> SESSION = AttachmentRegistry.<SessionSaved>builder()
			.persistent(SessionSaved.CODEC)
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "card_session"));

	/** 当前打开的选卡（只活在一次界面会话里，关掉/选完就丢）。键 = 玩家 UUID。 */
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	/** 上一刻是否在睡觉：用来检测「睡着 → 醒来」这个翻转。 */
	private static final Set<UUID> SLEEPING = new HashSet<>();

	/** 一次界面会话里的三张卡与各自的刷新状态。 */
	private static final class Session {
		private final List<ItemStack> cards;
		private final boolean[] refreshed = new boolean[CARDS];

		private Session(List<ItemStack> cards) {
			this.cards = cards;
		}
	}

	private CardSelection() {
	}

	/** 显式初始化入口（由 {@link Sephiria#onInitialize()} 调用；附件类型写在静态字段里，加载类即注册）。 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(CardSelection::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			SESSIONS.remove(id);
			SLEEPING.remove(id);
		});
	}

	// ------------------------------------------------------------------ 次数与计时

	public static int chances(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);
		return saved == null ? INITIAL_CHANCES : Mth.clamp(saved.chances(), 0, MAX_CHANCES);
	}

	/** 扣一次选卡机会；不够返回 false（什么都不扣）。 */
	private static boolean consumeChance(ServerPlayer player) {
		Saved saved = saved(player);

		if (saved.chances() <= 0) {
			return false;
		}

		player.setAttached(SAVED, new Saved(saved.chances() - 1, saved.lastAward()));
		return true;
	}

	/**
	 * 取存档；第一次访问时按「初始 1 次、从现在开始计时」写一份。
	 *
	 * <p>不能简单返回空默认值：{@code lastAward} 必须落成一个真实刻数，否则下次算
	 * 「经过了多久」会拿 0 去减，直接把整条游戏时间当成周期发出去。
	 */
	private static Saved saved(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);

		if (saved == null) {
			saved = new Saved(INITIAL_CHANCES, player.level().getGameTime());
			player.setAttached(SAVED, saved);
		}

		return saved;
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			awardIfDue(player);
			checkWaking(player);
		}
	}

	/** 每 2 个游戏日补一次机会；一次最多补 {@link #MAX_AWARDS_PER_CHECK} 次。 */
	private static void awardIfDue(ServerPlayer player) {
		long now = player.level().getGameTime();
		Saved saved = player.getAttached(SAVED);

		if (saved == null) {
			// 首次创建：从此刻开始计时，给初始的 1 次
			player.setAttached(SAVED, new Saved(INITIAL_CHANCES, now));
			return;
		}

		if (saved.lastAward() <= 0L) {
			// 缺省 / 坏档里没有上次发放刻：从现在开始算，别把整条游戏时间当成周期一次发满
			player.setAttached(SAVED, new Saved(saved.chances(), now));
			return;
		}

		long elapsed = now - saved.lastAward();

		if (elapsed < 0L) {
			// 存档被回滚 / 时间被调小：别倒扣次数，只把计时基准拉回现在
			player.setAttached(SAVED, new Saved(saved.chances(), now));
			return;
		}

		if (elapsed < AWARD_PERIOD_TICKS) {
			return;
		}

		long periods = elapsed / AWARD_PERIOD_TICKS;
		int award = (int) Math.min(periods, MAX_AWARDS_PER_CHECK);
		int chances = Math.min(MAX_CHANCES, saved.chances() + award);
		// 计时基准一次推到「完整的周期之后」（超出上限的周期算作放弃），
		// 否则离线很久的玩家会在接下来几十 tick 里连续补满
		long lastAward = saved.lastAward() + periods * AWARD_PERIOD_TICKS;

		player.setAttached(SAVED, new Saved(chances, lastAward));
	}

	/** 睡醒检测：睡着 → 醒来、天亮了、还有机会，才自动打开。 */
	private static void checkWaking(ServerPlayer player) {
		UUID id = player.getUUID();

		if (player.isSleeping()) {
			SLEEPING.add(id);
			return;
		}

		if (!SLEEPING.remove(id)) {
			return;
		}

		// 只有正常睡到天亮才弹；半夜被打断 / 半途起床不打扰玩家
		if (!player.level().isBrightOutside() || chances(player) <= 0) {
			return;
		}

		open(player);
	}

	// ------------------------------------------------------------------ 界面动作

	/** 打开（或重新打开）选卡：每次重新掷三张，不扣机会。 */
	public static void open(ServerPlayer player) {
		Session session = session(player);

		if (session == null) {
			session = roll(player.getRandom());
			saveSession(player, session);
		}

		send(player, session, true);
	}

	/**
	 * 刷新某一张卡。服务端重新掷一件<b>同品质</b>的神器，5% 概率升一档；
	 * 每张卡在一次会话里只能刷一次。
	 */
	public static void refresh(ServerPlayer player, int index) {
		Session session = session(player);

		if (session == null) {
			// 没有会话（比如服务端重启后残留的界面）：关掉客户端那一页，别发三张空卡
			send(player, null, false);
			return;
		}

		if (index < 0 || index >= CARDS) {
			send(player, session, true);
			return;
		}

		if (session.refreshed[index]) {
			// 客户端说还能刷但服务端已经用过了：以服务端为准，把当前状态重发一遍
			send(player, session, true);
			return;
		}

		ItemStack old = session.cards.get(index);
		ArtifactRarity rarity = old.isEmpty() ? ArtifactRarity.COMMON
				: ArtifactLoot.rarityOf(old.getItem());

		if (player.getRandom().nextDouble() < UPGRADE_RARITY_CHANCE) {
			rarity = upgraded(rarity);
		}

		List<Item> pool = new ArrayList<>(ArtifactLoot.ofRarity(ArtifactLoot.artifactPool(), rarity));

		if (pool.isEmpty()) {
			// 这一档暂时没有神器（理论上不会）：退回整个神器池，至少别给空卡
			pool = new ArrayList<>(ArtifactLoot.artifactPool());
		}

		Item picked = pick(pool, old.isEmpty() ? null : old.getItem(), player.getRandom());

		if (picked != null) {
			session.cards.set(index, new ItemStack(picked));
		}

		session.refreshed[index] = true;
		saveSession(player, session);
		send(player, session, true);
	}

	/**
	 * 选择某一张卡：给玩家那件神器、扣一次机会、关界面。
	 *
	 * <p>所有条件都在服务端重新判一遍（有没有这次会话、下标合不合法、还有没有机会），
	 * 客户端发来的只是「点了哪一张」。
	 */
	public static void select(ServerPlayer player, int index) {
		Session session = session(player);

		if (session == null) {
			send(player, null, false);
			return;
		}

		if (index < 0 || index >= CARDS) {
			send(player, session, true);
			return;
		}

		ItemStack chosen = session.cards.get(index);

		if (chosen.isEmpty() || chances(player) <= 0 || !consumeChance(player)) {
			// 没机会 / 卡面是空的：不交付、不关界面，把权威状态发回去让客户端对齐
			send(player, session, true);
			return;
		}

		clearSession(player);
		// 走 PlayerItems.give：背包满时装不下的部分掉在脚下，不会被吞
		PlayerItems.give(player, chosen.copy());
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
		send(player, null, false);
	}

	private static Session session(ServerPlayer player) {
		SessionSaved saved = player.getAttached(SESSION);

		if (saved == null || saved.cards().size() != CARDS) {
			return null;
		}

		Session session = new Session(new ArrayList<>(saved.cards()));

		for (int index = 0; index < CARDS; index++) {
			session.refreshed[index] = index < saved.refreshed().size() && saved.refreshed().get(index);
		}

		return session;
	}

	private static void saveSession(ServerPlayer player, Session session) {
		List<Boolean> refreshed = new ArrayList<>(CARDS);

		for (boolean flag : session.refreshed) {
			refreshed.add(flag);
		}

		player.setAttached(SESSION, new SessionSaved(List.copyOf(session.cards), List.copyOf(refreshed)));
	}

	private static void clearSession(ServerPlayer player) {
		player.setAttached(SESSION, new SessionSaved(List.of(), List.of()));
	}

	// ------------------------------------------------------------------ 掷卡

	/** 掷一次界面：先定品质（四档等概率），再在同一档里抽三件不重复的神器。 */
	private static Session roll(RandomSource random) {
		ArtifactRarity rarity = BASE_RARITIES[random.nextInt(BASE_RARITIES.length)];
		List<Item> pool = new ArrayList<>(ArtifactLoot.ofRarity(ArtifactLoot.artifactPool(), rarity));

		if (pool.isEmpty()) {
			pool = new ArrayList<>(ArtifactLoot.artifactPool());
		}

		List<ItemStack> cards = new ArrayList<>(CARDS);
		List<Item> remaining = new ArrayList<>(pool);

		while (cards.size() < CARDS && !pool.isEmpty()) {
			if (remaining.isEmpty()) {
				remaining = new ArrayList<>(pool);
			}

			Item item = remaining.remove(random.nextInt(remaining.size()));
			cards.add(new ItemStack(item));
		}

		// 池子不足三件时用空卡补位（当前神器池每档都远多于三件，只是兜底）
		while (cards.size() < CARDS) {
			cards.add(ItemStack.EMPTY);
		}

		return new Session(cards);
	}

	/** 从池子里挑一件，尽量避开当前这张（只有一件时只能不变）。 */
	private static Item pick(List<Item> pool, Item avoid, RandomSource random) {
		if (pool.isEmpty()) {
			return null;
		}

		Item picked = pool.get(random.nextInt(pool.size()));

		if (avoid != null && pool.size() > 1) {
			for (int attempt = 0; attempt < 8 && picked == avoid; attempt++) {
				picked = pool.get(random.nextInt(pool.size()));
			}
		}

		return picked;
	}

	/** 品质升一档；红色（传说）不再升，也不进入羁绊 / 永恒。 */
	private static ArtifactRarity upgraded(ArtifactRarity rarity) {
		return switch (rarity) {
			case COMMON -> ArtifactRarity.ADVANCED;
			case ADVANCED -> ArtifactRarity.RARE;
			case RARE -> ArtifactRarity.LEGENDARY;
			default -> rarity;
		};
	}

	// ------------------------------------------------------------------ 同步

	private static void send(ServerPlayer player, Session session, boolean open) {
		if (session == null) {
			ServerPlayNetworking.send(player, new CardSelectionPayload(List.of(), 0, chances(player), open));
			return;
		}

		List<ItemStack> cards = new ArrayList<>(CARDS);

		for (ItemStack stack : session.cards) {
			cards.add(stack.copy());
		}

		int mask = 0;

		for (int index = 0; index < CARDS; index++) {
			if (session.refreshed[index]) {
				mask |= 1 << index;
			}
		}

		ServerPlayNetworking.send(player, new CardSelectionPayload(cards, mask, chances(player), open));
	}
}
