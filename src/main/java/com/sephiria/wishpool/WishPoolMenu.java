package com.sephiria.wishpool;

import com.sephiria.registry.ModMenus;
import com.sephiria.network.WishPoolResultPayload;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 许愿池界面（菜单）：<b>不显示物品栏</b>（与根之退同一套，用户要的是一个新界面）。
 *
 * <p>界面内容全在客户端画（{@code client.wishpool.WishPoolScreen}），这个菜单负责三件事：
 * <ol>
 *   <li>把状态同步过去（{@link ContainerData}：能量、四叶草数量、两个卡池的锚定件数字 id）；</li>
 *   <li>抽卡：扣四叶草 → 按商店概率抽（含锚定）→ 记能量 → 把抽到的东西<b>从许愿池四散弹出</b>
 *       → 把结果发给客户端播动画；</li>
 *   <li>锚定与神铸：改玩家状态、扣能量。</li>
 * </ol>
 *
 * <p>数字 id 只在<b>同步</b>里用（0 = 没锚定，因为 0 号是空气）；存档里存的是物品 id 字符串，
 * 免得以后加物品、数字 id 挪位把老存档的锚定搞乱。
 */
public class WishPoolMenu extends AbstractContainerMenu {
	/** 面板宽度 / 高度：左侧卡池标签 + 右边卡池区（用户 2026-10-15 要求放大，原来挤在一起）。 */
	public static final int IMAGE_WIDTH = 300;
	public static final int IMAGE_HEIGHT = 220;
	/** 左上角那行方块名字（用户 2026-10-15 要求放在最左上角）。 */
	public static final int TITLE_X = 8;
	public static final int TITLE_Y = 8;
	/** 左侧两个卡池标签（竖着排，在标题下面）。 */
	public static final int TAB_X = 8;
	public static final int TAB_Y = 24;
	public static final int TAB_WIDTH = 64;
	public static final int TAB_HEIGHT = 24;
	public static final int TAB_GAP = 4;
	/** 卡池区（标签右边）的左上角与尺寸。 */
	public static final int POOL_X = 82;
	public static final int POOL_Y = 10;
	public static final int POOL_WIDTH = IMAGE_WIDTH - POOL_X - 10;
	public static final int POOL_HEIGHT = IMAGE_HEIGHT - POOL_Y - 12;
	/** 卡池区里竖着排的三件：锚定物品图标（20×20）→「锚定」按钮 →「神铸」按钮。 */
	public static final int ANCHOR_ICON_X = POOL_X + (POOL_WIDTH - 20) / 2;
	public static final int ANCHOR_ICON_Y = POOL_Y + 14;
	public static final int POOL_BUTTON_WIDTH = 84;
	public static final int POOL_BUTTON_HEIGHT = 18;
	public static final int ANCHOR_BUTTON_X = POOL_X + (POOL_WIDTH - POOL_BUTTON_WIDTH) / 2;
	public static final int ANCHOR_BUTTON_Y = ANCHOR_ICON_Y + 26;
	public static final int FORGE_BUTTON_X = ANCHOR_BUTTON_X;
	public static final int FORGE_BUTTON_Y = ANCHOR_BUTTON_Y + POOL_BUTTON_HEIGHT + 6;
	/** 卡池下方：「单抽」「十连」，再下面一行是「四叶草：x   能量：y」（能量跟在四叶草后面）。 */
	public static final int DRAW_BUTTON_WIDTH = 80;
	public static final int DRAW_BUTTON_HEIGHT = 18;
	public static final int DRAW_BUTTON_Y = IMAGE_HEIGHT - 52;
	public static final int DRAW_BUTTON_GAP = 10;
	public static final int SINGLE_BUTTON_X = POOL_X
			+ (POOL_WIDTH - (2 * DRAW_BUTTON_WIDTH + DRAW_BUTTON_GAP)) / 2;
	public static final int TEN_BUTTON_X = SINGLE_BUTTON_X + DRAW_BUTTON_WIDTH + DRAW_BUTTON_GAP;
	public static final int STATS_TEXT_Y = DRAW_BUTTON_Y + DRAW_BUTTON_HEIGHT + 8;

	/** 数据槽：能量、四叶草（数值）、神器池锚定件数字 id、石板池锚定件数字 id。 */
	public static final int DATA_ENERGY = 0;
	public static final int DATA_CLOVERS = 1;
	public static final int DATA_ARTIFACT_ANCHOR = 2;
	public static final int DATA_SLATE_ANCHOR = 3;

	/** 服务端用的玩家与方块坐标（客户端壳里都是 null / 0）。 */
	private final ServerPlayer owner;
	private final BlockPos pos;
	private final ContainerData data;

	/** 客户端用的构造：空壳数据，内容等原版同步。 */
	public WishPoolMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, null, BlockPos.ZERO, new SimpleContainerData(dataSize()));
	}

	public WishPoolMenu(int containerId, Inventory playerInventory, ServerPlayer owner, BlockPos pos,
			ContainerData data) {
		super(ModMenus.WISH_POOL, containerId);
		this.owner = owner;
		this.pos = pos;
		this.data = data;
		// 一个格子都不加：这个界面不碰物品
		addDataSlots(data);
	}

	public static int dataSize() {
		return 4;
	}

	/** 服务端使用：数据槽接玩家的真实状态。 */
	public static WishPoolMenu forPlayer(int containerId, Inventory playerInventory, ServerPlayer player,
			BlockPos pos) {
		return new WishPoolMenu(containerId, playerInventory, player, pos, liveData(player));
	}

	/** 把菜单打开给玩家（26.3 的菜单必须走 screenOpeningData 这条路，数据用 Unit 占位）。 */
	public static MenuProvider provider(ServerPlayer player, BlockPos pos) {
		return new ExtendedMenuProvider<Unit>() {
			@Override
			public Unit getScreenOpeningData(ServerPlayer ignored) {
				return Unit.INSTANCE;
			}

			@Override
			public Component getDisplayName() {
				return WishPoolBlock.title();
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
				return forPlayer(containerId, inventory, player, pos);
			}
		};
	}

	/** 服务端的数据槽：每次读都现算，改完下一 tick 原版就同步给客户端。 */
	private static ContainerData liveData(ServerPlayer player) {
		return new ContainerData() {
			@Override
			public int get(int index) {
				return switch (index) {
					case DATA_ENERGY -> WishPool.energy(player);
					case DATA_CLOVERS -> WishPool.clovers(player);
					case DATA_ARTIFACT_ANCHOR -> WishPool.anchorNetworkId(player, false);
					case DATA_SLATE_ANCHOR -> WishPool.anchorNetworkId(player, true);
					default -> 0;
				};
			}

			@Override
			public void set(int index, int value) {
				// 只读
			}

			@Override
			public int getCount() {
				return dataSize();
			}
		};
	}

	// ------------------------------------------------------------------ 界面读的值

	public int energy() {
		return this.data.get(DATA_ENERGY);
	}

	public int clovers() {
		return this.data.get(DATA_CLOVERS);
	}

	/** 某个卡池的锚定件数字 id（0 = 没锚定）。 */
	public int anchorId(boolean slatePool) {
		return this.data.get(slatePool ? DATA_SLATE_ANCHOR : DATA_ARTIFACT_ANCHOR);
	}

	// ------------------------------------------------------------------ 服务端动作

	/**
	 * 抽卡：{@code count} 次（单抽 1、十连 10），每次 1 片四叶草。
	 *
	 * <p>四叶草不够就什么都不做（客户端的置灰只是显示）。抽到的东西会从许愿池方块那里
	 * 四散弹出去（用户口径「抽取的物品会从许愿池中四散弹出」），同时把清单发给客户端播动画。
	 */
	public void draw(boolean slatePool, int count) {
		if (this.owner == null || !WishPool.consumeClovers(this.owner, count * WishPool.CLOVERS_PER_DRAW)) {
			return;
		}

		List<ItemStack> drawn = WishPool.draw(this.owner, slatePool, count);
		scatter(drawn);

		if (!drawn.isEmpty()) {
			ServerPlayNetworking.send(this.owner, new WishPoolResultPayload(drawn));
		}

		this.broadcastChanges();
	}

	/** 把抽到的东西从许愿池上方四散弹出去（每件一个随机方向、带点向上的初速）。 */
	private void scatter(List<ItemStack> drawn) {
		if (!(this.owner.level() instanceof ServerLevel level) || drawn.isEmpty()) {
			return;
		}

		Vec3 center = Vec3.atCenterOf(this.pos).add(0.0D, 1.0D, 0.0D);

		for (ItemStack stack : drawn) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
			double speed = 0.18D + level.getRandom().nextDouble() * 0.12D;
			ItemEntity drop = new ItemEntity(level,
					center.x + Math.cos(angle) * 0.3D, center.y, center.z + Math.sin(angle) * 0.3D, stack);
			drop.setDeltaMovement(Math.cos(angle) * speed, 0.35D + level.getRandom().nextDouble() * 0.15D,
					Math.sin(angle) * speed);
			drop.setPickUpDelay(20);
			level.addFreshEntity(drop);
		}

		level.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
				net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.2F);
	}

	/** 锚定 / 取消锚定（{@code anchor} 传 null 就是取消）。 */
	public void setAnchor(boolean slatePool, net.minecraft.world.item.Item anchor) {
		if (this.owner == null) {
			return;
		}

		WishPool.setAnchor(this.owner, slatePool, anchor);
		this.broadcastChanges();
	}

	/**
	 * 神铸：花能量换一件指定物品（只能换黄与红两档，见 {@link WishPool#forgeCost}）。
	 *
	 * <p>换到的东西同样从许愿池弹出去（与抽卡一个观感）。
	 */
	public void forge(net.minecraft.world.item.Item item) {
		if (this.owner == null || item == null) {
			return;
		}

		int cost = WishPool.forgeCost(com.sephiria.artifact.ArtifactLoot.rarityOf(item));

		if (cost <= 0 || WishPool.energy(this.owner) < cost) {
			return;
		}

		WishPool.addEnergy(this.owner, -cost);
		List<ItemStack> forged = List.of(new ItemStack(item));
		scatter(forged);
		ServerPlayNetworking.send(this.owner, new WishPoolResultPayload(forged));
		this.broadcastChanges();
	}

	/** 这个界面不碰物品：Shift 点击一律不搬东西。 */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}
}
