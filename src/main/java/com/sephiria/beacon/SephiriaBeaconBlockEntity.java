package com.sephiria.beacon;

import com.sephiria.registry.ModBlockEntities;
import com.sephiria.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


/**
 * 赛菲莉娅信标：单格容器 + 一条与原版信标同款的光柱。
 *
 * <p>光柱不是自己画的：方块实体实现 {@link BeaconBeamOwner}，客户端直接复用原版
 * {@code BeaconRenderer}。所以这里只需要给出「颜色」这一件事——颜色取容器里那件物品的品质色，
 * 容器空了就返回空列表，光柱自然消失。
 *
 * <p>物品变化后必须让客户端重新拿到方块实体数据，否则光柱颜色会停在旧值：
 * {@link #setChanged()} 里额外发一次方块更新（原版 ChunkHolder 会顺带广播
 * {@link #getUpdatePacket()}）。
 */
public class SephiriaBeaconBlockEntity extends BlockEntity implements Container {
	private static final String KEY_ITEM = "item";

	private ItemStack item = ItemStack.EMPTY;

	public SephiriaBeaconBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SEPHIRIA_BEACON, pos, state);
	}

	/** 容器里那件物品（只读副本由调用方自己复制；这里返回真实栈，原版容器习惯如此）。 */
	public ItemStack heldItem() {
		return this.item;
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return this.item.isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return slot == 0 ? this.item : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		if (slot != 0 || this.item.isEmpty() || amount <= 0) {
			return ItemStack.EMPTY;
		}

		ItemStack removed = this.item.split(Math.min(amount, this.item.getCount()));

		if (this.item.isEmpty()) {
			this.item = ItemStack.EMPTY;
		}

		this.setChanged();
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		if (slot != 0) {
			return ItemStack.EMPTY;
		}

		ItemStack removed = this.item;
		this.item = ItemStack.EMPTY;
		return removed;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot != 0) {
			return;
		}

		// 一格容器：附魔币最多 3 个（信标奖励），其他物品只留 1 件
		this.item = stack.isEmpty() ? ItemStack.EMPTY
				: stack.copyWithCount(Math.min(stack.getCount(), getMaxStackSize(stack)));
		this.setChanged();
	}

	@Override
	public int getMaxStackSize() {
		return 3;
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return stack.is(ModItems.ENCHANT_COIN) ? 3 : 1;
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		this.item = ItemStack.EMPTY;
		this.setChanged();
	}

	@Override
	public void setChanged() {
		super.setChanged();

		if (this.level != null && !this.level.isClientSide()) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag output, HolderLookup.Provider registries) {
		super.saveAdditional(output, registries);
		// ItemStack.CODEC 不允许空气（0 个物品）；空容器就不写这个字段，读的时候缺省为空
		if (!this.item.isEmpty()) {
			output.put(KEY_ITEM, this.item.saveOptional(registries));
		}
	}

	@Override
	protected void loadAdditional(CompoundTag input, HolderLookup.Provider registries) {
		super.loadAdditional(input, registries);
		this.item = input.contains(KEY_ITEM)
				? ItemStack.parseOptional(registries, input.getCompound(KEY_ITEM))
				: ItemStack.EMPTY;

		if (!this.item.isEmpty()) {
			this.item = this.item.copyWithCount(Math.min(this.item.getCount(), getMaxStackSize(this.item)));
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveCustomOnly(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
