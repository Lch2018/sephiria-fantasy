package com.sephiria.compat.torohealth;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ToroHealth 伤害数字的客户端兼容层：收服务端的「主副元素 + 暴击 + 税前伤害」包，
 * 等 ToroHealth 为同一个目标生成生命变化粒子时把信息交出去。
 *
 * <p>ToroHealth 的粒子工厂只拿得到粒子坐标，没有实体 id；所以这里缓存待处理记录，
 * 在生命值同步时由 {@code LivingEntitySyncMixin} 提供实体 UUID，按 UUID 精确匹配；
 * 没有同步信息时才退回水平距离匹配。包在生命值同步之前发出，正常情况下会在同一 tick 内匹配成功。
 * <b>用 UUID 而不是 {@code getId()}</b>：同步数据在实体构造期间就会被设置，那时实体 id 还没分配，
 * {@code getId()} 会直接抛异常。
 *
 * <p>ToroHealth 没装时 {@link #accept} 直接丢弃，不会积压；即使装了但粒子被配置关掉，
 * 记录也会在 {@link #EXPIRE_MILLIS} 后清掉。
 *
 * <p>为了让渲染端知道副色和暴击，粒子文本前会加一段标签：
 * {@code MARKER + (crit ? "1" : "0") + 六位副色（无则 "------"） + MARKER + 数字}。
 * 真正的渲染器会把标签剥掉，自己按上下半 / 2 倍大小绘制。
 */
public final class ToroHealthCompatClient {
	/** 标签边界：零宽空格，正常渲染前会被剥离。 */
	public static final String MARKER = "\u200B";

	private static final long EXPIRE_MILLIS = 1000L;
	/** 生命值同步 → 粒子工厂是同一线程同步调用；这个时间只是防止没有粒子时留下过期的实体 UUID。 */
	private static final long ENTITY_ID_EXPIRE_MILLIS = 100L;
	private static final int MAX_PENDING = 128;
	/** HUD 上的血量变化文字只保留 1 秒，这里稍长一点，覆盖 ToroHealth 的 20 tick 显示窗口。 */
	private static final long HUD_EXPIRE_MILLIS = 2000L;
	private static final List<Pending> PENDING = new ArrayList<>();
	/** 每个目标最近一次本模组伤害，供 ToroHealth 左上角 HUD 的血量变化文字染色。 */
	private static final Map<Integer, TimedDamage> LAST_DAMAGE = new HashMap<>();
	private static long nextSequence;
	private static UUID particleEntityUuid;
	private static long particleEntityAt;

	private ToroHealthCompatClient() {
	}

	public static void accept(ToroHealthDamagePayload payload) {
		if (!FabricLoader.getInstance().isModLoaded("torohealth")) {
			return;
		}

		prune();
		long now = System.currentTimeMillis();
		PENDING.add(new Pending(payload.entityId(), payload.element(), payload.secondaryElement(),
				payload.displayColor(), payload.crit(), payload.preTaxDamage(),
				payload.x(), payload.y(), payload.z(), now, nextSequence++));

		while (PENDING.size() > MAX_PENDING) {
			PENDING.remove(0);
		}

		// HUD 的血量变化文字没有粒子可挂钩，单独记一份「这个目标最近一次本模组伤害」。
		LAST_DAMAGE.put(payload.entityId(),
				new TimedDamage(payload.displayColor(), now));
		pruneHudDamage(now);
	}

	/**
	 * 给 ToroHealth 刚创建的伤害粒子找一条显示信息。
	 *
	 * <p>优先级：先按实体 UUID 精确匹配（生命值同步时由 {@code LivingEntitySyncMixin} 记下），
	 * 这样同一次攻击产生的附加伤害 / 减益跳数不会把主伤害的信息抢走；没有同步信息时才退回
	 * 原来的坐标匹配。同一实体有多条待处理记录时取<b>最新</b>的一条——ToroHealth 的粒子是跟着
	 * 一次生命值同步生成的，最新那条才是这次同步对应的伤害。
	 *
	 * @return 匹配到的元素 / 暴击 / 税前伤害；没有就返回 null（保留 ToroHealth 原样式）
	 */
	public static DamageInfo consume(ClientLevel level, double x, double y, double z, UUID entityUuid) {
		prune();
		Pending best = null;

		if (entityUuid != null) {
			// 有确切 UUID 就只认这个实体；找不到说明这一下不是本模组伤害（或包还没到），
			// 宁可保留 ToroHealth 原色，也不要拿旁边另一只怪的记录来染色。
			best = newestForEntity(level, entityUuid);

			if (best == null) {
				return null;
			}
		} else {
			best = nearestByPosition(level, x, y, z);
		}

		if (best == null) {
			return null;
		}

		PENDING.remove(best);
		return new DamageInfo(best.element(), best.secondaryElement(), best.displayColor(),
				best.crit(), best.preTaxDamage());
	}

	/** 同一实体的待处理记录里取最新的一条。 */
	private static Pending newestForEntity(ClientLevel level, UUID entityUuid) {
		Pending best = null;

		for (Pending pending : PENDING) {
			Entity entity = level.getEntity(pending.entityId());

			if (entity != null && entityUuid.equals(entity.getUUID())
					&& (best == null || pending.sequence() > best.sequence())) {
				best = pending;
			}
		}

		return best;
	}

	/** 坐标匹配：目标可能比粒子坐标高（粒子在实体半身高），只比水平距离最稳。 */
	private static Pending nearestByPosition(ClientLevel level, double x, double y, double z) {
		Pending best = null;
		double bestDistance = Double.MAX_VALUE;

		for (Pending pending : PENDING) {
			Entity entity = level.getEntity(pending.entityId());
			double distance;

			if (entity != null) {
				double dx = entity.getX() - x;
				double dz = entity.getZ() - z;
				distance = dx * dx + dz * dz;
			} else {
				double dx = pending.x() - x;
				double dy = pending.y() - y;
				double dz = pending.z() - z;
				distance = dx * dx + dy * dy + dz * dz;
			}

			// 距离相同时取更新的那条，避免同一个目标的多段伤害总是被最旧的一条染色
			if (distance <= 9.0D && (distance < bestDistance
					|| (distance == bestDistance && (best == null || pending.sequence() > best.sequence())))) {
				best = pending;
				bestDistance = distance;
			}
		}

		return best;
	}

	/** HUD 用：这个目标最近一次本模组伤害；超过窗口或没有记录返回 null。 */
	public static HudDamage lastDamage(int entityId) {
		pruneHudDamage(System.currentTimeMillis());
		TimedDamage timed = LAST_DAMAGE.get(entityId);
		return timed == null ? null : new HudDamage(timed.displayColor());
	}

	/** 丢掉过期的 HUD 记录，避免实体 id 复用时串色 / 无限增长。 */
	private static void pruneHudDamage(long now) {
		long cutoff = now - HUD_EXPIRE_MILLIS;
		LAST_DAMAGE.entrySet().removeIf(entry -> entry.getValue().at() < cutoff);

		if (LAST_DAMAGE.size() > MAX_PENDING) {
			LAST_DAMAGE.clear();
		}
	}

	/** {@code LivingEntity.onSyncedDataUpdated} 开始时记下实体 UUID，供紧接着同步创建的粒子使用。 */
	public static void setParticleEntity(UUID entityUuid) {
		particleEntityUuid = entityUuid;
		particleEntityAt = System.currentTimeMillis();
	}

	/** 取出并清掉粒子的实体 UUID；超过 {@link #ENTITY_ID_EXPIRE_MILLIS} 的旧值当作没有。 */
	public static UUID takeParticleEntity() {
		UUID entityUuid = System.currentTimeMillis() - particleEntityAt <= ENTITY_ID_EXPIRE_MILLIS
				? particleEntityUuid
				: null;
		particleEntityUuid = null;
		return entityUuid;
	}

	public static int color(int element) {
		return ToroHealthCompat.color(element);
	}

	public static int secondaryColor(int secondaryElement) {
		return ToroHealthCompat.secondaryColor(secondaryElement);
	}


	/** 生成带主副色 / 暴击信息的文本标签；渲染端用 {@link #parseTag} 解开。 */
	public static String tag(int secondaryColor, boolean crit) {
		String secondary = secondaryColor < 0 ? "------" : String.format("%06X", secondaryColor & 0xFFFFFF);
		return MARKER + (crit ? '1' : '0') + secondary + MARKER;
	}

	/** 解析文本标签；不是我们的标签返回 null。 */
	public static ParsedTag parseTag(String text) {
		if (!text.startsWith(MARKER) || text.length() < 9 || !text.startsWith(MARKER, 8)) {
			return null;
		}

		boolean crit = text.charAt(1) == '1';
		String hex = text.substring(2, 8);
		int secondary;

		try {
			secondary = hex.equals("------") ? -1 : Integer.parseInt(hex, 16);
		} catch (NumberFormatException exception) {
			return null;
		}

		return new ParsedTag(crit, secondary, text.substring(9));
	}

	public static void reset() {
		PENDING.clear();
		LAST_DAMAGE.clear();
	}

	private static void prune() {
		long cutoff = System.currentTimeMillis() - EXPIRE_MILLIS;
		Iterator<Pending> iterator = PENDING.iterator();

		while (iterator.hasNext()) {
			if (iterator.next().receivedAt() < cutoff) {
				iterator.remove();
			}
		}
	}

	/** 一次待匹配的伤害显示信息。 */
	public record DamageInfo(int element, int secondaryElement, int displayColor, boolean crit, int preTaxDamage) {
	}

	/** 标签解析结果：暴击、副色、去掉标签后的数字文本。 */
	public record ParsedTag(boolean crit, int secondaryColor, String text) {
	}

	/** HUD 血量变化文字要用的颜色（服务端已按元素集合查好）。 */
	public record HudDamage(int displayColor) {
	}

	private record TimedDamage(int displayColor, long at) {
	}

	private record Pending(int entityId, int element, int secondaryElement, int displayColor,
			boolean crit, int preTaxDamage, double x, double y, double z, long receivedAt, long sequence) {
	}
}
