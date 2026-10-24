package com.sephiria.talent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.slate.SlateItem;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 天赋「生存」三阶段的「石板刻印」存档。
 *
 * <p>长按背包里的石板把进度攒满后，石板从格子里消失，但它的<b>格子坐标、朝向与物品本身</b>
 * 被记在这里；{@link ArtifactBackpack#effectiveLevels()} 在算完背包里还放着的石板之后，
 * 把这一份也按原格子累加一遍——所以周围格子的效果保留。
 *
 * <p><b>刻印是永久的</b>（用户 2026-10-24）：三阶段只决定「能不能刻」，刻下去之后效果一直保留，
 * 阶段熄灭不会把石板掉出来、也不会让效果消失——所以这里没有任何回退逻辑。
 * 存档跟着玩家走（死亡也保留），所以「刻印后重登」不会丢。
 */
public final class SlateImprint {
	/** 一条刻印：原格子下标 + 石板副本（含旋转组件）。 */
	public record Entry(int slot, ItemStack stack) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("slot").forGetter(Entry::slot),
				ItemStack.CODEC.fieldOf("stack").forGetter(Entry::stack)
		).apply(instance, Entry::new));
	}

	/** 存档：刻印列表（旧存档没有这一项时按空表读）。 */
	public record Saved(List<Entry> entries) {
		static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(Saved::entries)
		).apply(instance, Saved::new));
	}

	private static final AttachmentType<Saved> SAVED = AttachmentRegistry.<Saved>builder()
			.persistent(Saved.CODEC)
			// 刻印的石板相当于「存在背包里」，死亡不掉
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "slate_imprints"));

	private SlateImprint() {
	}

	/** 把已刻印石板的效果按原格子累加进 {@code levels}（由 {@link ArtifactBackpack#effectiveLevels()} 调）。 */
	public static void apply(ServerPlayer player, int[] levels) {
		for (Entry entry : entries(player)) {
			if (entry.stack().getItem() instanceof SlateItem slate) {
				slate.apply(levels, entry.slot(), ArtifactBackpack.WIDTH, SlateItem.rotationOf(entry.stack()),
						entry.stack());
			}
		}
	}

	/** 记录一条刻印：石板已经从背包里拿出来了（这里的 stack 是副本）。 */
	public static void imprint(ServerPlayer player, int slot, ItemStack stack) {
		List<Entry> list = new ArrayList<>(entries(player));
		list.add(new Entry(slot, stack.copy()));
		player.setAttached(SAVED, new Saved(List.copyOf(list)));

		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		backpack.invalidateLevels();
		backpack.setChanged();
	}

	public static List<Entry> entries(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);
		return saved == null ? List.of() : saved.entries();
	}

	/** 只读查询：这个格子已经有刻印了吗（同一格允许刻多条，效果叠不叠加由石板自己决定）。 */
	public static boolean isImprinted(ServerPlayer player, int slot) {
		for (Entry entry : entries(player)) {
			if (entry.slot() == slot) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 给客户端显示用：刻印总数（服务端不需要，但把查询集中在同一个类里）。
	 */
	public static int count(ServerPlayer player) {
		return entries(player).size();
	}
}
