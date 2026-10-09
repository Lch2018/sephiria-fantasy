package com.sephiria.client;

import com.sephiria.network.DebuffSyncPayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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
	private static final Map<Identifier, Map<Integer, Integer>> STACKS = new TreeMap<>();

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
	public static List<Component> labelsFor(int entityId) {
		List<Component> labels = new ArrayList<>();

		for (Map.Entry<Identifier, Map<Integer, Integer>> entry : STACKS.entrySet()) {
			Integer stacks = entry.getValue().get(entityId);

			if (stacks != null) {
				labels.add(Component.translatable("ui.sephiria.debuff." + entry.getKey().getPath(), stacks));
			}
		}

		return labels;
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
