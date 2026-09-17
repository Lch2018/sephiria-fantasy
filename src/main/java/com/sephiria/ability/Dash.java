package com.sephiria.ability;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 通用突进：所有"位移/冲刺"类能力都从这里启动。
 *
 * <p>只接管水平速度，垂直分量保留实体自身的速度，所以<b>重力照常生效</b>——
 * 空中突进会边冲边落，地面突进会被摩擦和碰撞正常影响。
 *
 * <p>速度每 tick 乘一次保留率自然衰减，于是每 tick 的位移构成等比数列，
 * 总位移就是这个数列的和：
 * <pre>
 *     distance = v0 · (1 - k^n) / (1 - k)        k = 衰减率，n = 耗时
 * </pre>
 * 调用方一般给「距离 + 耗时 + 衰减率」，初速度用 {@link #initialSpeed} 反推；
 * 需要精确控制初速度时也可以直接给速度。
 */
public final class Dash {
	/** 默认速度保留率：每 tick 保留 70%。 */
	public static final double DEFAULT_DECAY = 0.7D;

	private static final Map<UUID, Active> ACTIVE = new HashMap<>();

	private Dash() {
	}

	/** 注册推进器（由 {@link com.sephiria.Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Dash::tick);
	}

	/**
	 * 启动一次突进，初速度由距离/耗时/衰减率反推。垂直方向交给重力，突进只管水平面。
	 *
	 * @param entity        被突进的实体
	 * @param direction     方向（只取水平分量，垂直方向交给重力）
	 * @param distance      位移距离（格）
	 * @param durationTicks 位移耗时（tick）
	 * @param decay         速度自然衰减率：每 tick 保留的比例（0~1）
	 */
	public static void start(Entity entity, Vec3 direction, double distance, int durationTicks, double decay) {
		startWithSpeed(entity, direction, initialSpeed(distance, durationTicks, decay), durationTicks, decay);
	}

	/**
	 * 启动一次<b>含垂直分量</b>的突进：整条方向向量都参与，位移沿着给定方向走
	 * （比如照准星朝向突进，抬头就会往上冲、低头往下冲）。代价是突进期间垂直速度被接管，
	 * 重力要等突进结束才会重新把实体往下拉。
	 */
	public static void startVertical(Entity entity, Vec3 direction, double distance, int durationTicks, double decay) {
		Vec3 full = direction.normalize();

		if (full.lengthSqr() < 1.0E-8D || distance <= 0.0D || durationTicks <= 0) {
			return;
		}

		ACTIVE.put(entity.getUUID(),
				new Active(entity, full, initialSpeed(distance, durationTicks, decay), decay, durationTicks, true));
	}

	/** 启动一次突进，初速度直接给定（水平）。 */
	public static void startWithSpeed(Entity entity, Vec3 direction, double speed, int durationTicks, double decay) {
		Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);

		if (horizontal.lengthSqr() < 1.0E-8D || speed <= 0.0D || durationTicks <= 0) {
			return;
		}

		ACTIVE.put(entity.getUUID(),
				new Active(entity, horizontal.normalize(), speed, decay, durationTicks, false));
	}

	/** 由位移距离、耗时、衰减率反推初速度。 */
	public static double initialSpeed(double distance, int durationTicks, double decay) {
		if (distance <= 0.0D || durationTicks <= 0) {
			return 0.0D;
		}
		if (Math.abs(1.0D - decay) < 1.0E-6D) {
			// 不衰减时退化成匀速
			return distance / durationTicks;
		}

		double sum = (1.0D - Math.pow(decay, durationTicks)) / (1.0D - decay);
		return distance / sum;
	}

	public static boolean isDashing(Entity entity) {
		return ACTIVE.containsKey(entity.getUUID());
	}

	/** 提前结束某个实体的突进。 */
	public static void stop(Entity entity) {
		ACTIVE.remove(entity.getUUID());
	}

	private static void tick(MinecraftServer server) {
		if (ACTIVE.isEmpty()) {
			return;
		}

		Iterator<Map.Entry<UUID, Active>> iterator = ACTIVE.entrySet().iterator();

		while (iterator.hasNext()) {
			Active dash = iterator.next().getValue();
			Entity entity = dash.entity;

			if (entity.isRemoved() || !entity.isAlive()) {
				iterator.remove();
				continue;
			}

			Vec3 velocity = entity.getDeltaMovement();
			// 含垂直分量的突进连 Y 一起接管，否则保留实体自身的垂直速度（重力照常作用）
			double vertical = dash.vertical ? dash.direction.y * dash.speed : velocity.y;
			entity.setDeltaMovement(dash.direction.x * dash.speed, vertical, dash.direction.z * dash.speed);
			// 玩家的位移是客户端物理执行的，必须把这份速度同步过去才会真的动
			// ——原版的击退、三叉戟激流用的也是这个标记。
			entity.hurtMarked = true;

			dash.speed *= dash.decay;

			if (--dash.remaining <= 0) {
				iterator.remove();
			}
		}
	}

	/** 一次进行中的突进。 */
	private static final class Active {
		final Entity entity;
		final Vec3 direction;
		final double decay;
		final boolean vertical;
		double speed;
		int remaining;

		Active(Entity entity, Vec3 direction, double speed, double decay, int remaining, boolean vertical) {
			this.entity = entity;
			this.direction = direction;
			this.speed = speed;
			this.decay = decay;
			this.remaining = remaining;
			this.vertical = vertical;
		}
	}
}
