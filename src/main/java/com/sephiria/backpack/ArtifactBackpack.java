package com.sephiria.backpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import com.sephiria.artifact.BackpackStorable;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 赛菲利亚背包：只放神器与石板的独立背包，宽度固定 6 格、高度可变（默认 5 格，扩容方式待做）。
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
	/** 默认高度 5 格。 */
	public static final int DEFAULT_HEIGHT = 5;
	/** 高度上限（扩容方式还没做，先定个上限，避免存档里出现离谱尺寸）。 */
	public static final int MAX_HEIGHT = 9;

	/** 存档用的数据：物品、格子等级、高度。 */
	public record Saved(List<ItemStack> items, List<Integer> slotLevels, int height) {
		public static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(Saved::items),
				Codec.INT.listOf().fieldOf("slot_levels").forGetter(Saved::slotLevels),
				Codec.INT.fieldOf("height").forGetter(Saved::height)
		).apply(instance, Saved::new));
	}

	private static final AttachmentType<Saved> SAVED = AttachmentRegistry.<Saved>builder()
			.persistent(Saved.CODEC)
			// 神器是收集品：死亡不掉落，背包跟着玩家走。要改成掉落去掉这一行即可。
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "artifact_backpack"));

	/** 每个玩家一份：菜单与效果统计必须看到同一个对象，所以缓存起来。 */
	private static final Map<UUID, ArtifactBackpack> CACHE = new HashMap<>();

	/** 内部数组按最大高度开；{@link #getContainerSize()} 只暴露当前高度内的格子。 */
	private final int[] slotLevels = new int[WIDTH * MAX_HEIGHT];
	private final ServerPlayer owner;
	/**
	 * 正在从存档里装载。
	 *
	 * <p>装载时会调 {@code setItem}，而它内部会回调 {@link #setChanged()}——那一刻这个背包还没进
	 * 缓存，写回附件/刷属性都会再次走到 {@link #of}，等于在构造过程中递归构造自己。所以装载期间
	 * 先关掉副作用。
	 */
	private boolean loading;
	private int height;
	/** 加上石板影响后的格子等级缓存；内容一变就置空重算。 */
	private int[] levelCache;

	private ArtifactBackpack(ServerPlayer owner, Saved saved) {
		super(WIDTH * MAX_HEIGHT);
		this.owner = owner;
		this.loading = true;
		this.height = clampHeight(saved == null ? DEFAULT_HEIGHT : saved.height());

		if (saved != null) {
			List<ItemStack> items = saved.items();

			for (int slot = 0; slot < items.size() && slot < WIDTH * MAX_HEIGHT; slot++) {
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
		return CACHE.computeIfAbsent(player.getUUID(), uuid -> new ArtifactBackpack(player, player.getAttached(SAVED)));
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

	/** 高度（行数）；宽度固定 {@value #WIDTH}。 */
	public int height() {
		return this.height;
	}

	/** 当前可用的格子数 = 宽 × 高。 */
	@Override
	public int getContainerSize() {
		return WIDTH * this.height;
	}

	/** 扩容用（扩容方式待做）。 */
	public void setHeight(int height) {
		this.height = clampHeight(height);
		this.setChanged();
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
					slate.apply(levels, slot, WIDTH, com.sephiria.slate.SlateItem.rotationOf(stack));
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
	 * 格子等级的只读视图，用来当菜单的数据槽（服务端改、客户端读）。
	 *
	 * <p>格子等级是服务端算的东西，客户端只负责把数字画在格子里。
	 */
	public ContainerData levelData() {
		return new ContainerData() {
			@Override
			public int get(int index) {
				return slotLevel(index);
			}

			@Override
			public void set(int index, int value) {
				// 只读：客户端不需要回写
			}

			@Override
			public int getCount() {
				return WIDTH * height;
			}
		};
	}

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

	@Override
	public void setChanged() {
		super.setChanged();
		// 内容变了：格子等级（含石板影响）要重算
		this.levelCache = null;

		if (this.loading || this.owner == null) {
			return;
		}

		this.owner.setAttached(SAVED, snapshot());
		// 神器会影响物理强度，属性面板跟着刷新一次
		PlayerStats.sync(this.owner);
	}

	/** 保存用快照：物品 + 格子等级 + 高度。 */
	private Saved snapshot() {
		List<ItemStack> items = new ArrayList<>(WIDTH * MAX_HEIGHT);
		List<Integer> levels = new ArrayList<>(WIDTH * MAX_HEIGHT);

		for (int slot = 0; slot < WIDTH * MAX_HEIGHT; slot++) {
			items.add(getItem(slot));
			levels.add(this.slotLevels[slot]);
		}

		return new Saved(items, levels, this.height);
	}

	private static int clampHeight(int height) {
		return Math.max(1, Math.min(MAX_HEIGHT, height));
	}
}
