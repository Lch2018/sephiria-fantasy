package com.sephiria.backpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import com.sephiria.ability.SkillStorage;
import com.sephiria.artifact.BackpackStorable;
import com.sephiria.artifact.EternalEclipseItem;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 赛菲莉娅背包：只放神器与石板的独立背包，宽度固定 6 格、高度可变（默认 5 格，扩容方式待做）。
 *
 * <p>每个玩家一份，存在 Fabric 的数据附件里（{@code artifact_backpack}），跟着玩家存档走；
 * 内容一变就写回附件，并顺手刷新一次属性同步（神器会影响物理强度，属性面板要跟着变）。
 *
 * <p>这个类只管存储与规则（格子等级、能不能放、不可堆叠）。效果统计在
 * {@link com.sephiria.artifact.ArtifactEffects}，界面与菜单在
 * {@link com.sephiria.backpack.ArtifactBackpackMenu}。
 */
public final class ArtifactBackpack extends SimpleContainer {
	/** 宽度固定 6 格。 */
	public static final int WIDTH = 6;
	/** 默认高度 5 格（界面固定显示的行数也是它，见 {@link BackpackWindow#ROWS}）。 */
	public static final int DEFAULT_HEIGHT = 5;
	/** 高度上限 9 格：超过这个就不给扩了，免得存档里出现离谱尺寸。 */
	public static final int MAX_HEIGHT = 9;
	/** 默认格子数（5 行 × 6 格）。 */
	public static final int DEFAULT_SLOTS = WIDTH * DEFAULT_HEIGHT;
	/** 格子数上限（9 行 × 6 格）。 */
	public static final int MAX_SLOTS = WIDTH * MAX_HEIGHT;

	/**
	 * 存档用的数据：物品、格子等级、格子数（以及只为读回老存档留的高度）。
	 *
	 * <p>格子数以前是按「高度（行数）」存的。2026-10-17 改成按**格数**记（「赛菲莉娅额外背包栏」
	 * 一次 +1 格，最后一行可能只有几格），所以新增了 {@code slots}：
	 * <ul>
	 * <li>老存档没有 {@code slots}（读到 0）→ 用 {@code height × 6} 折算回来，**不会丢数据**；</li>
	 * <li>新存档只写 {@code slots}，{@code height} 写 0（它是可选字段，0 就是"没这条"）。</li>
	 * </ul>
	 * 这一步必须做成兼容读：附件解码一失败就会被 Fabric 静默丢掉（见记忆手册 §三），
	 * 直接换字段名等于把所有人的背包清空。
	 */
	public record Saved(List<ItemStack> items, List<Integer> slotLevels, int slots, int height) {
		public static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(Saved::items),
				Codec.INT.listOf().fieldOf("slot_levels").forGetter(Saved::slotLevels),
				Codec.INT.optionalFieldOf("slots", 0).forGetter(Saved::slots),
				Codec.INT.optionalFieldOf("height", 0).forGetter(Saved::height)
		).apply(instance, Saved::new));

		/** 写存档用：只带新的格数字段。 */
		public static Saved of(List<ItemStack> items, List<Integer> slotLevels, int slots) {
			return new Saved(items, slotLevels, slots, 0);
		}

		/** 实际格子数：新的 {@code slots} 优先，老存档按「高度 × 宽」折算。 */
		public int resolvedSlots() {
			if (this.slots > 0) {
				return this.slots;
			}

			return this.height > 0 ? this.height * WIDTH : DEFAULT_SLOTS;
		}
	}

	private static final AttachmentType<Saved> SAVED = AttachmentRegistry.<Saved>builder()
			.persistent(Saved.CODEC)
			// 神器是收集品：死亡不掉落，背包跟着玩家走。要改成掉落去掉这一行即可。
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "artifact_backpack"));

	/** 每个玩家一份：菜单与效果统计必须看到同一个对象，所以缓存起来。 */
	private static final Map<UUID, ArtifactBackpack> CACHE = new HashMap<>();

	/** 内部数组按上限开；{@link #getContainerSize()} 只暴露当前格子数以内的部分。 */
	private final int[] slotLevels = new int[MAX_SLOTS];
	/**
	 * 当前绑定的玩家实体。
	 *
	 * <p>不能是 final：死亡复活（以及别的换实体流程）会让服务端换一个全新的 {@code ServerPlayer}
	 * 实例，旧实例被移除、它的附件再也不会被存档——所以要能换绑，见 {@link #of}。
	 */
	private ServerPlayer owner;
	/**
	 * 正在从存档里装载。
	 *
	 * <p>装载时会调 {@code setItem}，而它内部会回调 {@link #setChanged()}——那一刻这个背包还没进
	 * 缓存，写回附件/刷属性都会再次走到 {@link #of}，等于在构造过程中递归构造自己。所以装载期间
	 * 先关掉副作用。
	 */
	private boolean loading;
	/** 当前格子数（默认 {@value #DEFAULT_SLOTS}，每次扩容 +1 格）。 */
	private int slots;
	/** 加上石板影响后的格子等级缓存；内容一变就置空重算。 */
	private int[] levelCache;

	private ArtifactBackpack(ServerPlayer owner, Saved saved) {
		super(MAX_SLOTS);
		this.owner = owner;
		this.loading = true;
		this.slots = clampSlots(saved == null ? DEFAULT_SLOTS : saved.resolvedSlots());

		if (saved != null) {
			List<ItemStack> items = saved.items();

			for (int slot = 0; slot < items.size() && slot < MAX_SLOTS; slot++) {
				ItemStack stack = items.get(slot);

				if (stack != null && !stack.isEmpty()) {
					super.setItem(slot, stack);
				}
			}

			List<Integer> levels = saved.slotLevels();

			for (int slot = 0; slot < levels.size() && slot < this.slotLevels.length; slot++) {
				this.slotLevels[slot] = Math.max(0, levels.get(slot));
			}
		}

		this.loading = false;
	}

	/** 取某个玩家的背包（首次访问时从附件里读出来）。 */
	public static ArtifactBackpack of(ServerPlayer player) {
		ArtifactBackpack cached = CACHE.get(player.getUUID());

		if (cached != null) {
			// 复活会换一个 ServerPlayer 实例：缓存还绑在旧实例上，而旧实例的附件永远不再被存档。
			// 不换绑的话，从死亡那一刻起的所有改动（放进去/拿出来的神器、格子等级）都只写在
			// 死实体上，重登就回滚到死亡时的样子——2026-10-16 实测到的「买完商店的东西、大退
			// 重进买卖被撤销」就是这条。换绑后立刻把手里这份内容写回新实例：内存里这份才是最新的
			// （新实例上那份是复活时复制的旧快照，比它旧）。
			// 注意顺序：先把 owner 换掉再 setChanged——它内部会绕回 of()，否则会无限换绑。
			if (cached.owner != player && !player.isRemoved()) {
				cached.owner = player;
				cached.setChanged();
			}

			return cached;
		}

		ArtifactBackpack backpack = new ArtifactBackpack(player, player.getAttached(SAVED));
		// 装载完对账一次永恒蚀的图标颜色：存档里没写过这个组件（老存档、或指令塞进来的副本）
		// 的那一份也能立刻拿到正确的那张图。这里只改物品组件、不调 setChanged，不会递归。
		EternalEclipseItem.stamp(backpack);
		CACHE.put(player.getUUID(), backpack);
		return backpack;
	}

	/**
	 * 显式初始化入口（由 {@link com.sephiria.Sephiria#onInitialize()} 调用）。
	 *
	 * <p>附件类型写在静态字段里，加载这个类就会注册；这个方法只是让「初始化」这件事有个明确的
	 * 调用点，免得将来有人把静态字段挪走之后忘了注册。
	 */
	public static void register() {
	}

	/** 玩家退出时清缓存（附件里已经是最新状态）。 */
	public static void forget(ServerPlayer player) {
		CACHE.remove(player.getUUID());
	}

	// ------------------------------------------------------------------ 尺寸与格子等级

	/** 当前格子数（默认 {@value #DEFAULT_SLOTS}，上限 {@value #MAX_SLOTS}）。 */
	public int slots() {
		return this.slots;
	}

	/** 当前格子数决定容器大小（宽固定 {@value #WIDTH}，所以最后一行可能只有几格）。 */
	@Override
	public int getContainerSize() {
		// 天赋给的是动态格数：急智一阶段 +2、生存二阶段 +3 / 三阶段 -12（不写进存档，
		// 重置/退点就收回）；放进去的东西仍在存档的 items 列表里（再点回天赋就又能看到）。
		// 下限夹 1：生存三阶段在很小的背包装备上也不能变成负格子。
		int talent = com.sephiria.talent.Talents.backpackSlots(this.owner);
		return Math.max(1, Math.min(MAX_SLOTS, this.slots + talent));
	}

	/**
	 * 扩容：+1 格（「赛菲莉娅额外背包栏」右键用一次走这里）。返回是否真的加了。
	 *
	 * <p>到上限（{@value #MAX_SLOTS} 格）就不再变化、返回 false，调用方据此决定要不要消耗道具。
	 * 内容一变 {@code setChanged} 会把格子数一起写进附件，所以是永久扩容。
	 */
	public boolean addSlot() {
		if (this.slots >= MAX_SLOTS) {
			return false;
		}

		this.slots++;
		this.setChanged();
		return true;
	}

	/** 某个格子的格子等级：基础等级 + 石板的加成（可以为负）。 */
	public int slotLevel(int slot) {
		int[] levels = effectiveLevels();
		return slot >= 0 && slot < levels.length ? levels[slot] : 0;
	}

	/** 基础格子等级（升级系统将来直接改它；石板另算）。 */
	public int baseSlotLevel(int slot) {
		return slot >= 0 && slot < this.slotLevels.length ? this.slotLevels[slot] : 0;
	}

	/**
	 * 加上石板影响后的全部格子等级。
	 *
	 * <p>石板之间会互相影响：这里按格子顺序把所有石板的效果累加一遍，所以加加减减都生效，
	 * 结果可以是负数（神器放在负等级格子上就失效）。
	 */
	public int[] effectiveLevels() {
		if (this.levelCache == null) {
			int[] levels = java.util.Arrays.copyOf(this.slotLevels, this.slotLevels.length);
			int size = getContainerSize();

				for (int slot = 0; slot < size; slot++) {
					ItemStack stack = getItem(slot);

					if (stack.getItem() instanceof com.sephiria.slate.SlateItem slate) {
						slate.apply(levels, slot, WIDTH, com.sephiria.slate.SlateItem.rotationOf(stack), stack);
					}
				}

				// 已刻印（从格子里消耗掉、但效果保留）的石板：仍然按它原来的格子生效
				if (this.owner != null) {
					com.sephiria.talent.SlateImprint.apply(this.owner, levels);

					// 天赋「耐心」一阶段：背包第一行（前 6 格）格子等级 +1（2026-10-24 取代石板基础）
					if (com.sephiria.talent.Talents.stage(this.owner, com.sephiria.talent.Talent.PATIENCE) >= 1) {
						for (int slot = 0; slot < WIDTH; slot++) {
							levels[slot] += 1;
						}
					}
				}

				this.levelCache = levels;
		}

		return this.levelCache;
	}

	/** 设置基础格子等级（石板之类的升级手段接进来时用）。 */
	public void setSlotLevel(int slot, int level) {
		if (slot < 0 || slot >= this.slotLevels.length) {
			return;
		}

		this.slotLevels[slot] = level;
		this.setChanged();
	}

	/**
	 * 让格子等级缓存失效：天赋改格数（急智 / 生存）或刻印存档变化时调。
	 *
	 * <p>不写存档、不调 {@code setChanged}——这两件事的调用方各自负责。
	 */
	public void invalidateLevels() {
		this.levelCache = null;
	}

	// 格子等级的数据槽以前在这里（levelData）；界面改成「固定一屏的可视窗口」之后，
	// 数据槽必须按可视区下标给（还要带上滚到第几行），所以挪到两个菜单自己算——
	// 见 ArtifactBackpackMenu.liveData / EnchantMenu.liveData。

	// ------------------------------------------------------------------ 容器规则

	/** 只有神器与石板能放进来。 */
	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return stack.getItem() instanceof BackpackStorable;
	}

	/** 神器和石板都不可堆叠。 */
	@Override
	public int getMaxStackSize() {
		return 1;
	}

	/**
	 * 放进格子（或换下来）时先给<b>被换掉的那一份</b>清掉「在哪半边」组件。
	 *
	 * <p>永恒蚀的图标颜色跟着它待在哪半边走（见 {@link EternalEclipseItem#SIDE}），
	 * 而搬走之后那一份已经不在这个容器里、{@link #setChanged()} 再也看不到它，
	 * 所以必须在交接的这一瞬间清掉，否则它会带着背包里的颜色跑到别处去。
	 */
	@Override
	public void setItem(int slot, ItemStack stack) {
		EternalEclipseItem.clear(this.getItem(slot));
		super.setItem(slot, stack);
	}

	/** 取走（同样是离开背包的那一瞬间，理由见 {@link #setItem}）。 */
	@Override
	public ItemStack removeItem(int slot, int amount) {
		EternalEclipseItem.clear(this.getItem(slot));
		return super.removeItem(slot, amount);
	}

	@Override
	public void setChanged() {
		super.setChanged();
		// 内容变了：格子等级（含石板影响）要重算
		this.levelCache = null;

		if (this.loading || this.owner == null) {
			return;
		}

		// 永恒蚀的图标颜色按「在第几列」对账（与属性一样是现查状态，不是记上次的值）
		EternalEclipseItem.stamp(this);
		this.owner.setAttached(SAVED, snapshot());
		// 神器会影响物理强度与冲刺上限，属性面板和 HUD 各刷新一次
		PlayerStats.sync(this.owner);
		SkillStorage.syncAll(this.owner);
	}

	/** 保存用快照：物品 + 格子等级 + 格子数（数组按上限开，多出来的位置是空的那份）。 */
	private Saved snapshot() {
		List<ItemStack> items = new ArrayList<>(MAX_SLOTS);
		List<Integer> levels = new ArrayList<>(MAX_SLOTS);

		for (int slot = 0; slot < MAX_SLOTS; slot++) {
			items.add(getItem(slot));
			levels.add(this.slotLevels[slot]);
		}

		return Saved.of(items, levels, this.slots);
	}

	private static int clampSlots(int slots) {
		return Math.max(1, Math.min(MAX_SLOTS, slots));
	}
}
