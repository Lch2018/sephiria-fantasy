package com.sephiria.compat;

import com.sephiria.slate.PatternSlateItem.Cell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 1.20.1 没有数据组件，物品状态只能写 NBT。这里给 26.3 里用组件的字段提供一组等价读写，
 * 键名沿用旧组件 id，方便之后把 1.20.1 的存档往新版本迁移时按名字对上。
 *
 * <p>只处理「读不到就用默认值」这一种语义；旧组件的网络同步在 1.20.1 由原版 NBT 同步
 * （服务端改完物品栈后随容器/背包包一起发给客户端）承担，不需要额外通道。
 */
public final class ItemData {
	private ItemData() {
	}

	public static int getInt(ItemStack stack, String key, int fallback) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
	}

	public static void setInt(ItemStack stack, String key, int value) {
		stack.getOrCreateTag().putInt(key, value);
	}

	public static boolean getBoolean(ItemStack stack, String key, boolean fallback) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.contains(key, Tag.TAG_BYTE) ? tag.getBoolean(key) : fallback;
	}

	public static void setBoolean(ItemStack stack, String key, boolean value) {
		stack.getOrCreateTag().putBoolean(key, value);
	}

	public static boolean has(ItemStack stack, String key) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.contains(key);
	}

	public static void remove(ItemStack stack, String key) {
		CompoundTag tag = stack.getTag();
		if (tag != null) {
			tag.remove(key);
		}
	}

	/** UUID 存成两个 long，避免字符串解析失败时把「有主人」误判成「无主人」。 */
	public static UUID getUuid(ItemStack stack, String key) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.hasUUID(key)) {
			return null;
		}
		return tag.getUUID(key);
	}

	public static void setUuid(ItemStack stack, String key, UUID value) {
		stack.getOrCreateTag().putUUID(key, value);
	}

	public static List<Cell> getCells(ItemStack stack, String key) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(key, Tag.TAG_LIST)) {
			return List.of();
		}

		ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
		List<Cell> cells = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			CompoundTag cell = list.getCompound(i);
			cells.add(new Cell(cell.getInt("dx"), cell.getInt("dy"), cell.getInt("delta")));
		}
		return List.copyOf(cells);
	}

	public static void setCells(ItemStack stack, String key, List<Cell> cells) {
		ListTag list = new ListTag();
		for (Cell cell : cells) {
			CompoundTag entry = new CompoundTag();
			entry.putInt("dx", cell.dx());
			entry.putInt("dy", cell.dy());
			entry.putInt("delta", cell.delta());
			list.add(entry);
		}
		stack.getOrCreateTag().put(key, list);
	}

	public static List<ItemStack> getItems(ItemStack stack, String key) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(key, Tag.TAG_LIST)) {
			return List.of();
		}

		ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
		List<ItemStack> items = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			items.add(ItemStack.of(list.getCompound(i)));
		}
		return List.copyOf(items);
	}

	public static void setItems(ItemStack stack, String key, List<ItemStack> items) {
		ListTag list = new ListTag();
		for (ItemStack item : items) {
			list.add(item.save(new CompoundTag()));
		}
		stack.getOrCreateTag().put(key, list);
	}
}
