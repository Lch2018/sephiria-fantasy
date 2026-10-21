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
 * <p>三阶段熄灭时 {@link #dropAll} 把它们全部丢在玩家脚下、清空存档，效果随之消失。
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
		static final Saved EMPTY = new Saved(List.of());

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
				slate.apply(levels, entry.slot(), ArtifactBackpack.WIDTH, SlateItem.rotationOf(entry.stack()));
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

	/** 三阶段熄灭：所有刻印石板从玩家身上掉出，效果一起消失。 */
	public static void dropAll(ServerPlayer player) {
		List<Entry> list = entries(player);

		if (list.isEmpty()) {
			return;
		}

		for (Entry entry : list) {
			player.drop(entry.stack().copy(), false, net.minecraft.util.Prediction.SERVER_ONLY);
		}

		player.setAttached(SAVED, Saved.EMPTY);
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		backpack.invalidateLevels();
		backpack.setChanged();
	}

	/** 阶段变化：三阶段熄灭时清空（点亮时没有额外动作）。 */
	public static void onStageChanged(ServerPlayer player, int oldStage, int newStage) {
		if (oldStage >= 3 && newStage < 3) {
			dropAll(player);
		}
	}

	/** 进服对账：三阶段没点亮却还有残留刻印（异常退出 / 老存档）时掉出来。 */
	public static void dropIfInactive(ServerPlayer player) {
		if (Talents.stage(player, Talent.SURVIVAL) < 3) {
			dropAll(player);
		}
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
