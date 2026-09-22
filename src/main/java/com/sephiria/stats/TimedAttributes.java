package com.sephiria.stats;

import com.sephiria.Sephiria;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 限时属性修饰符：挂上去、到点自动摘掉。
 *
 * <p>急速的加速、鼓励旗帜给友军的攻速都属于「限时增益」。原版的做法是自定义 MobEffect，
 * 但那要连图标、颜色、HUD 一起做；这里的效果只改两条属性、也不该和药水效果抢位置，所以用
 * 临时修饰符自己计时更直接。
 *
 * <p>同一个来源重复给（旗帜每几 tick 刷一次）会刷新时间而不是叠加，所以站在旗子里不会越叠越高。
 */
public final class TimedAttributes {
	/** 正挂着的限时修饰符（数量很少，直接线性扫）。 */
	private static final List<Entry> ACTIVE = new ArrayList<>();

	/** 一条限时修饰符：挂在谁身上、改哪条属性、什么数值、什么时候到期。 */
	private record Entry(LivingEntity target, Holder<Attribute> attribute, Identifier id, double amount, long until) {
	}

	private TimedAttributes() {
	}

	/** 注册推进器（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TimedAttributes::tick);
	}

	/**
	 * 给目标挂一条限时修饰符。
	 *
	 * @param ticks 持续多少 tick（从这一刻算起）
	 */
	public static void grant(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount,
			int ticks) {
		// 同一个来源重复给：刷新时间与数值，不叠上去
		ACTIVE.removeIf(entry -> entry.target() == entity && entry.id().equals(id));
		ACTIVE.add(new Entry(entity, attribute, id, amount, entity.level().getGameTime() + ticks));
		sync(entity);
		AttributeInstance instance = entity.getAttribute(attribute);

		if (instance != null) {
			instance.addOrUpdateTransientModifier(
					new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	/**
	 * 目标身上某条属性正挂着的限时加成总和。
	 *
	 * <p>面板要显示「当前实际值」，而限时加成是直接挂在原版属性上的修饰符、不在面板数值里，
	 * 所以面板把这些加回去（机械结算那边不加，免得重复计算）。
	 */
	public static double bonusOf(LivingEntity entity, Holder<Attribute> attribute) {
		double total = 0.0D;

		for (Entry entry : ACTIVE) {
			if (entry.target() == entity && entry.attribute() == attribute) {
				total += entry.amount();
			}
		}

		return total;
	}

	/** 挂着的攻速加成换算成面板的「攻击速度」百分点（基准 4 次/秒）。 */
	public static double attackSpeedPercent(ServerPlayer player) {
		return bonusOf(player, Attributes.ATTACK_SPEED) / Attributes.DEFAULT_ATTACK_SPEED * 100.0D;
	}

	/** 挂着的移速加成换算成面板的「移动速度」百分点（按基础移速算）。 */
	public static double moveSpeedPercent(ServerPlayer player) {
		AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
		double base = instance == null ? 0.1D : instance.getBaseValue();

		return bonusOf(player, Attributes.MOVEMENT_SPEED) / base * 100.0D;
	}

	/** 加成表变了就推一次面板（限时加成开始/结束时数值会变）。 */
	private static void sync(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			PlayerStats.sync(player);
		}
	}

	/** 立刻摘掉某一条（技能提前结束、玩家换装时用）。 */
	public static void revoke(LivingEntity entity, Holder<Attribute> attribute, Identifier id) {
		ACTIVE.removeIf(entry -> entry.target() == entity && entry.id().equals(id));
		remove(entity, attribute, id);
		sync(entity);
	}

	private static void tick(MinecraftServer server) {
		if (ACTIVE.isEmpty()) {
			return;
		}

		long now = server.overworld().getGameTime();
		Iterator<Entry> iterator = ACTIVE.iterator();

		while (iterator.hasNext()) {
			Entry entry = iterator.next();

			if (entry.until() > now) {
				continue;
			}

			iterator.remove();
			remove(entry.target(), entry.attribute(), entry.id());
			sync(entry.target());
		}
	}

	private static void remove(LivingEntity entity, Holder<Attribute> attribute, Identifier id) {
		AttributeInstance instance = entity.getAttribute(attribute);

		if (instance != null) {
			instance.removeModifier(id);
		}
	}
}
