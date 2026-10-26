package com.sephiria.client.hud;

import com.sephiria.network.HudSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * HUD 的技能 / 神器状态镜像。
 *
 * <p>服务端每 10 刻推一次 {@link HudSyncPayload}；这里记下收到的客户端刻数，渲染时按差值
 * 把冷却往前推，所以即使两包之间也不会跳变。充能直接是千分比，不需要插值。
 */
public final class HudClientData {
	private static List<HudSyncPayload.Entry> skills = List.of();
	private static List<HudSyncPayload.Entry> artifacts = List.of();
	private static long receivedAt;

	private HudClientData() {
	}

	public static void accept(HudSyncPayload payload) {
		skills = payload.skills();
		artifacts = payload.artifacts();
		receivedAt = gameTime();
	}

	public static List<HudSyncPayload.Entry> skills() {
		return skills;
	}

	public static List<HudSyncPayload.Entry> artifacts() {
		return artifacts;
	}

	/** 当前进度（0 = 刚用 / 空，1 = 就绪 / 充满）。冷却按收到时的剩余刻数往前推。 */
	public static float progress(HudSyncPayload.Entry entry) {
		if (entry.charging()) {
			return Math.clamp(entry.charge() / 1000.0F, 0.0F, 1.0F);
		}

		if (entry.total() <= 0) {
			return 1.0F;
		}

		long elapsed = Math.max(0L, gameTime() - receivedAt);
		long remaining = Math.max(0L, entry.remaining() - elapsed);
		return Math.clamp(1.0F - remaining / (float) entry.total(), 0.0F, 1.0F);
	}

	/** 图标对应的物品；注册表里找不到返回 null。 */
	public static Item itemOf(HudSyncPayload.Entry entry) {
		return BuiltInRegistries.ITEM.getOptional(entry.item()).orElse(null);
	}

	/** 物品图标贴图路径（本模组 / 原版物品都适用）。 */
	public static Identifier iconOf(Item item) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		return Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/item/" + id.getPath() + ".png");
	}

	public static void reset() {
		skills = List.of();
		artifacts = List.of();
		receivedAt = 0L;
	}

	private static long gameTime() {
		Minecraft client = Minecraft.getInstance();
		return client.level == null ? 0L : client.level.getGameTime();
	}
}
