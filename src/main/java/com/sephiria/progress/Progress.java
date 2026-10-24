package com.sephiria.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.registry.ModItems;
import com.sephiria.slate.SlateItem;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 赛菲莉娅进度树（原版「进度」页）的计数与达成判定。
 *
 * <p>进度条目本身是 `data/sephiria_fantasy/advancement/progress/` 下的 JSON；需要计数的几条
 * （击杀 / 武器击杀 / 远程击杀 / 收集 / 维度 / 群系）这里自己记，达到目标时用
 * {@link net.minecraft.server.PlayerAdvancements#award} 把 `impossible` 条件标记完成——
 * 这样不用注册自定义 criterion，原版进度页照常显示 / 弹 toast / 送四叶草与天赋点。
 *
 * <p>「收集 5 个不同种类」按<b>物品种类</b>去重（同一件神器 / 石板只算一次），同时扫玩家背包
 * 与赛菲莉娅背包；扫到过就永久记下，后来卖掉 / 丢掉也不会让进度倒退。
 *
 * <p>战斗：匕首 / 刀 / 弩的击杀按「造成致命伤害时手里的武器」（{@code DamageSource#getWeaponItem}，
 * 弩矢这种投射物也会带上发射它的弩）与距离判定；僵尸 / 骷髅用原版实体标签，变种也算。
 * 击杀晚于父节点时，子节点要等父节点完成后的下一次检查才会补发（例如先打了 5 只匕首僵尸、
 * 再打满 10 个任意单位，杀戮1 完成的那次检查会顺手补发匕首1）。
 */
public final class Progress {
	/** 进度目标的文件名（相对 `advancement/`，不含 .json）。 */
	private static final String SLAUGHTER_1 = "progress/combat/slaughter_1";
	private static final String DAGGER_1 = "progress/combat/dagger_1";
	private static final String BLADE_1 = "progress/combat/blade_1";
	private static final String SNIPE_1 = "progress/combat/snipe_1";
	private static final String CROSSBOW_1 = "progress/combat/crossbow_1";
	private static final String ARTIFACT_1 = "progress/collect/artifact_1";
	private static final String SLATE_1 = "progress/collect/slate_1";
	private static final String TOURIST_1 = "progress/explore/tourist_1";
	private static final String TRAVEL_1 = "progress/explore/travel_1";

	/** 击杀目标值。 */
	private static final int KILL_TARGET = 10;
	private static final int DAGGER_ZOMBIE_TARGET = 5;
	private static final int BLADE_ZOMBIE_TARGET = 5;
	private static final int SNIPE_TARGET = 5;
	private static final int CROSSBOW_SKELETON_TARGET = 3;
	private static final int COLLECT_TARGET = 5;
	/** 远程判定：10 格（平方）。 */
	private static final double SNIPE_DISTANCE_SQR = 100.0D;
	/** 收集 / 维度 / 群系扫描间隔：10 刻（0.5 秒）。 */
	private static final int SCAN_INTERVAL = 10;

	private static final String NETHER = "minecraft:the_nether";
	private static final String END = "minecraft:the_end";

	/**
	 * 存档：计数 + 已见过的物品 / 维度 / 群系。用可变对象，扫描时原地累加；
	 * 附件每次改动后重新 {@code setAttached} 一下，保证实体保存时拿到最新值。
	 */
	public static final class Saved {
		public int kills;
		public int daggerZombieKills;
		public int bladeZombieKills;
		public int snipeKills;
		public int crossbowSkeletonKills;
		public final List<String> artifacts = new ArrayList<>();
		public final List<String> slates = new ArrayList<>();
		public final List<String> dimensions = new ArrayList<>();
		public final List<String> biomes = new ArrayList<>();

		public Saved() {
		}

		private Saved(int kills, int daggerZombieKills, int bladeZombieKills, int snipeKills,
				int crossbowSkeletonKills, List<String> artifacts, List<String> slates,
				List<String> dimensions, List<String> biomes) {
			this.kills = kills;
			this.daggerZombieKills = daggerZombieKills;
			this.bladeZombieKills = bladeZombieKills;
			this.snipeKills = snipeKills;
			this.crossbowSkeletonKills = crossbowSkeletonKills;
			this.artifacts.addAll(artifacts);
			this.slates.addAll(slates);
			this.dimensions.addAll(dimensions);
			this.biomes.addAll(biomes);
		}

		static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("kills", 0).forGetter(saved -> saved.kills),
				Codec.INT.optionalFieldOf("dagger_zombie_kills", 0).forGetter(saved -> saved.daggerZombieKills),
				Codec.INT.optionalFieldOf("blade_zombie_kills", 0).forGetter(saved -> saved.bladeZombieKills),
				Codec.INT.optionalFieldOf("snipe_kills", 0).forGetter(saved -> saved.snipeKills),
				Codec.INT.optionalFieldOf("crossbow_skeleton_kills", 0)
						.forGetter(saved -> saved.crossbowSkeletonKills),
				Codec.STRING.listOf().optionalFieldOf("artifacts", List.of()).forGetter(saved -> saved.artifacts),
				Codec.STRING.listOf().optionalFieldOf("slates", List.of()).forGetter(saved -> saved.slates),
				Codec.STRING.listOf().optionalFieldOf("dimensions", List.of()).forGetter(saved -> saved.dimensions),
				Codec.STRING.listOf().optionalFieldOf("biomes", List.of()).forGetter(saved -> saved.biomes)
		).apply(instance, Saved::new));
	}

	private static final AttachmentType<Saved> SAVED = AttachmentRegistry.<Saved>builder()
			.persistent(Saved.CODEC)
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "progress"));

	private static int tickCounter;

	private Progress() {
	}

	/** 在模组初始化时调一次：注册附件与两个服务端事件。 */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register(Progress::onDeath);
		ServerTickEvents.END_SERVER_TICK.register(Progress::tick);
	}

	private static Saved get(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);

		if (saved == null) {
			saved = new Saved();
			player.setAttached(SAVED, saved);
		}

		return saved;
	}

	private static void save(ServerPlayer player, Saved saved) {
		player.setAttached(SAVED, saved);
	}

	/** 击杀结算：任意单位、远程、匕首 / 刀 / 弩的专属击杀。 */
	private static void onDeath(LivingEntity victim, DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer player) || victim == player) {
			return;
		}

		Saved saved = get(player);
		saved.kills++;

		double distanceSqr = player.distanceToSqr(victim);

		if (distanceSqr >= SNIPE_DISTANCE_SQR) {
			saved.snipeKills++;
		}

		// 弩矢这类投射物的 DamageSource 也会带着发射它的武器；没有就退回主手
		ItemStack weapon = source.getWeaponItem();

		if (weapon.isEmpty()) {
			weapon = player.getMainHandItem();
		}

		boolean zombie = victim.getType().builtInRegistryHolder().is(EntityTypeTags.ZOMBIES);
		boolean skeleton = victim.getType().builtInRegistryHolder().is(EntityTypeTags.SKELETONS);

		if (zombie && weapon.is(ModItems.DAGGER)) {
			saved.daggerZombieKills++;
		}

		if (zombie && weapon.is(ModItems.BLADE)) {
			saved.bladeZombieKills++;
		}

		if (skeleton && distanceSqr >= SNIPE_DISTANCE_SQR && weapon.is(ModItems.COLOSSAL_CROSSBOW)) {
			saved.crossbowSkeletonKills++;
		}

		save(player, saved);
		checkAll(player, saved);
	}

	private static void tick(MinecraftServer server) {
		if (++tickCounter % SCAN_INTERVAL != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			tickPlayer(player);
		}
	}

	/** 维度 / 群系 / 收集：扫描一遍当前状态，变化时存档并检查目标。 */
	private static void tickPlayer(ServerPlayer player) {
		Saved saved = get(player);
		boolean changed = false;
		String dimension = player.level().dimension().identifier().toString();

		if (!saved.dimensions.contains(dimension)) {
			saved.dimensions.add(dimension);
			changed = true;
		}

		String biome = player.level().getBiome(player.blockPosition()).getRegisteredName();

		if (!saved.biomes.contains(biome)) {
			saved.biomes.add(biome);
			changed = true;
		}

		changed |= scanInventory(player, saved);

		if (changed) {
			save(player, saved);
		}

		checkAll(player, saved);
	}

	private static boolean scanInventory(ServerPlayer player, Saved saved) {
		boolean changed = false;

		for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
			changed |= scanStack(player.getInventory().getItem(index), saved);
		}

		ArtifactBackpack backpack = ArtifactBackpack.of(player);

		for (int index = 0; index < backpack.getContainerSize(); index++) {
			changed |= scanStack(backpack.getItem(index), saved);
		}

		return changed;
	}

	/** 高级品质及以下（白 / 蓝）的神器或石板；同一件物品只记一次。 */
	private static boolean scanStack(ItemStack stack, Saved saved) {
		if (stack.isEmpty()) {
			return false;
		}

		Item item = stack.getItem();

		if (item instanceof SephiriaArtifact artifact
				&& artifact.rarity().ordinal() <= ArtifactRarity.ADVANCED.ordinal()) {
			return addId(saved.artifacts, item);
		}

		if (item instanceof SlateItem slate
				&& slate.rarity().ordinal() <= ArtifactRarity.ADVANCED.ordinal()) {
			return addId(saved.slates, item);
		}

		return false;
	}

	private static boolean addId(List<String> ids, Item item) {
		String id = BuiltInRegistries.ITEM.getKey(item).toString();

		if (ids.contains(id)) {
			return false;
		}

		ids.add(id);
		return true;
	}

	/** 按当前计数 / 集合检查所有目标；已经完成的条件 {@code award} 会直接返回 false。 */
	private static void checkAll(ServerPlayer player, Saved saved) {
		if (saved.kills >= KILL_TARGET) {
			grant(player, SLAUGHTER_1);
		}

		if (saved.snipeKills >= SNIPE_TARGET) {
			grant(player, SNIPE_1);
		}

		// 子节点要等父节点完成；先打完子节点计数也没关系，父节点完成时这里会补发
		if (isDone(player, SLAUGHTER_1)) {
			if (saved.daggerZombieKills >= DAGGER_ZOMBIE_TARGET) {
				grant(player, DAGGER_1);
			}

			if (saved.bladeZombieKills >= BLADE_ZOMBIE_TARGET) {
				grant(player, BLADE_1);
			}
		}

		if (saved.crossbowSkeletonKills >= CROSSBOW_SKELETON_TARGET && isDone(player, SNIPE_1)) {
			grant(player, CROSSBOW_1);
		}

		if (saved.artifacts.size() >= COLLECT_TARGET) {
			grant(player, ARTIFACT_1);
		}

		if (saved.slates.size() >= COLLECT_TARGET) {
			grant(player, SLATE_1);
		}

		if (saved.dimensions.contains(NETHER) && saved.dimensions.contains(END)) {
			grant(player, TOURIST_1);
		}

		if (saved.biomes.size() >= 2) {
			grant(player, TRAVEL_1);
		}
	}

	private static AdvancementHolder holder(ServerPlayer player, String path) {
		MinecraftServer server = player.level().getServer();
		return server == null ? null
				: server.getAdvancements().get(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, path));
	}

	private static boolean isDone(ServerPlayer player, String path) {
		AdvancementHolder holder = holder(player, path);
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	private static void grant(ServerPlayer player, String path) {
		AdvancementHolder holder = holder(player, path);

		if (holder == null || player.getAdvancements().getOrStartProgress(holder).isDone()) {
			return;
		}

		player.getAdvancements().award(holder, "impossible");
	}
}
