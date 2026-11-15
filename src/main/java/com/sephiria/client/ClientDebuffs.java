package com.sephiria.client;

import com.sephiria.network.DebuffSyncPayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 客户端的减益层数镜像：敌人脚下的层数标签（「触电：x」）读它。
 *
 * <p>服务端在层数变化时推 {@link DebuffSyncPayload}（0 = 减益消失），这里按
 * 「减益 id → 实体 id → 层数」存下来。实体死亡/卸载后服务端不会再发包，所以每客户端刻
 * {@link #prune} 一次，把已经不存在的实体条目清掉——标签也就跟着消失。
 */
public final class ClientDebuffs {
	/**
	 * 减益 id → （实体 id → 层数）；层数为 0 的条目直接删掉，不留空壳。
	 *
	 * <p>外层用 TreeMap：一只怪同时挂着两个减益时，脚下那两行标签的顺序按减益 id 固定下来
	 * （burns 在下、electric_shocks 在上），不然每次进游戏两行都可能换位。
	 */
	private static final Map<ResourceLocation, Map<Integer, Integer>> STACKS = new TreeMap<>();

	/**
	 * 减益标签的字色：跟伤害数字那套元素色保持一致（火 / 冰 / 电），
	 * 等离子体是火 + 电的组合色，见 {@code ToroHealthCompat#colorForMask}。
	 *
	 * <p>用户 2026-11-15 的口径：脚下的减益字不该一律是红色，要按「基础元素」上色。
	 * 这里只认减益 id，不看触发者的面板值——标签是给所有人看的，颜色要稳定。
	 */
	private static final int FIRE_COLOR = 0xEDB12E;
	private static final int ICE_COLOR = 0x6499E8;
	private static final int LIGHTNING_COLOR = 0x7EFAFF;
	/** 火 + 电（等离子体）：与伤害数字的双属性组合色一致。 */
	private static final int PLASMA_COLOR = 0xEE8AF8;

	/** 一条减益标签：文字与字色分开存，绘制端不再自己猜颜色。 */
	public record DebuffLabel(Component text, int color) {
	}

	private ClientDebuffs() {
	}

	public static void accept(DebuffSyncPayload payload) {
		Map<Integer, Integer> perEntity = STACKS.get(payload.debuff());

		if (payload.stacks() <= 0) {
			if (perEntity == null) {
				return;
			}

			perEntity.remove(payload.entityId());

			if (perEntity.isEmpty()) {
				STACKS.remove(payload.debuff());
			}

			return;
		}

		if (perEntity == null) {
			perEntity = new HashMap<>();
			STACKS.put(payload.debuff(), perEntity);
		}

		perEntity.put(payload.entityId(), payload.stacks());
	}

	/**
	 * 这个实体身上要显示的减益标签（「触电：2」这样的一行），没有就返回空表。
	 *
	 * <p>标签文字按减益 id 找 {@code ui.sephiria.debuff.<id>} 这个语言键。
	 */
	public static List<DebuffLabel> labelsFor(int entityId) {
		List<DebuffLabel> labels = new ArrayList<>();

		for (Map.Entry<ResourceLocation, Map<Integer, Integer>> entry : STACKS.entrySet()) {
			Integer stacks = entry.getValue().get(entityId);

			if (stacks != null) {
				String path = entry.getKey().getPath();
				labels.add(new DebuffLabel(
						Component.translatable("ui.sephiria_fantasy.debuff." + path, stacks),
						colorOf(path)));
			}
		}

		return labels;
	}

	/** 减益 id → 字色；未知减益退回原来的红色，至少不会看不见。 */
	private static int colorOf(String debuffPath) {
		return switch (debuffPath) {
			case "electric_shocks" -> LIGHTNING_COLOR;
			case "burns" -> FIRE_COLOR;
			case "plasma" -> PLASMA_COLOR;
			case "frostbites", "frozen" -> ICE_COLOR;
			default -> 0xFFFF7777;
		};
	}

	/**
	 * 这个实体身上有没有某个减益（层数 > 0）。
	 *
	 * <p>冰蓝色那层染色（{@code FrozenTintMixin}）按它判断——被冰冻眩晕的敌人要染成冰蓝，
	 * 与「受伤时闪红」同一个思路，只是颜色从减益状态来。
	 */
	public static boolean has(ResourceLocation debuff, int entityId) {
		Map<Integer, Integer> perEntity = STACKS.get(debuff);

		return perEntity != null && perEntity.containsKey(entityId);
	}

	/** 每客户端刻清一次：实体已经不在客户端世界里（死亡/卸载/换维度）就删掉它的条目。 */
	public static void prune(ClientLevel level) {
		if (level == null) {
			return;
		}

		STACKS.values().forEach(perEntity ->
				perEntity.keySet().removeIf(entityId -> level.getEntity(entityId) == null));
		STACKS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
	}

	/** 换世界 / 重连时清空（减益状态不跨世界）。 */
	public static void reset() {
		STACKS.clear();
	}
}
