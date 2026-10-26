package com.sephiria.compat.torohealth;

import com.sephiria.Sephiria;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * ToroHealth-Continued 的伤害数字兼容层（服务端部分）。
 *
 * <p>只做两件事：
 * <ol>
 *   <li>把本模组伤害的元素类型归到「物理 / 火 / 冰 / 电 / 混沌」，双属性再分出主 / 副两色；</li>
 *   <li>在伤害入口把「实体 id + 主副元素 + 是否暴击 + 税前伤害」推给附近客户端。</li>
 * </ol>
 * 客户端侧由 {@code ToroHealthCompatClient} 与可选 mixin 完成染色 / 2 倍暴击数字；
 * ToroHealth 没装时客户端直接忽略这些包，不影响正常游戏。
 *
 * <p>双属性（酷寒断头台、等离子体）仍会算出主 / 副两色，但显示端统一只用主色
 * （触发者面板值较高的一边）；混沌固定用 #8236B8。只是数字颜色，不改变实际伤害类型。
 */
public final class ToroHealthCompat {
	private static int dualLogs;
	public static final int PHYSICAL = 0;
	public static final int FIRE = 1;
	public static final int ICE = 2;
	public static final int LIGHTNING = 3;
	public static final int CHAOS = 4;

	private static final int PHYSICAL_COLOR = 0xC0C0C0;
	private static final int FIRE_COLOR = 0xEDB12E;
	private static final int ICE_COLOR = 0x6499E8;
	private static final int LIGHTNING_COLOR = 0x7EFAFF;
	private static final int CHAOS_COLOR = 0x741B7C;
	private static final int TRUE_COLOR = 0xFFFFFF;

	/** 元素位：物理 / 火 / 冰 / 电；真伤与混沌用单独的两位。 */
	private static final int BIT_PHYSICAL = 1;
	private static final int BIT_FIRE = 1 << 1;
	private static final int BIT_ICE = 1 << 2;
	private static final int BIT_LIGHTNING = 1 << 3;
	private static final int BIT_TRUE = 1 << 4;
	private static final int BIT_CHAOS = 1 << 5;

	private ToroHealthCompat() {
	}

	/** 伤害数字要用的主 / 副颜色（副色为 -1 表示单色）。 */
	public record Elements(int primary, int secondary) {
		public boolean dual() {
			return this.secondary >= 0;
		}
	}

	/**
	 * 在伤害真正进入 {@code hurtServer} 之前推一份显示信息。
	 *
	 * <p>放在暴击判定之后、生命值同步之前，客户端收到包时会先于 ToroHealth 的生命变化粒子，
	 * 从而能在粒子工厂里把颜色 / 暴击标记挂上去。
	 *
	 * @param preTaxDamage 税前伤害（已含暴击、尚未经过目标护甲 / 减伤），供「税前伤害」设置使用
	 */
	public static void send(ServerPlayer attacker, LivingEntity target, DamageSource source,
			boolean crit, float preTaxDamage) {
		if (!(target.level() instanceof ServerLevel level)) {
			return;
		}

		Elements elements = elementsOf(attacker, source);
		int mask = maskOf(attacker, source);
		ToroHealthDamagePayload payload = new ToroHealthDamagePayload(target.getId(),
				elements.primary(), elements.secondary(), colorForMask(mask), crit,
				Math.max(0, Math.round(preTaxDamage)),
				target.getX(), target.getY() + target.getBbHeight() / 2.0D, target.getZ());

		for (ServerPlayer viewer : level.players()) {
			ServerPlayNetworking.send(viewer, payload);
		}
	}

	/** 把伤害类型归到五色之一；双属性取触发者当前面板值较高的一边作为主色。 */
	public static Elements elementsOf(ServerPlayer player, DamageSource source) {
		SephiriaDamage.Kind kind = SephiriaDamage.kindOf(source);
		Elements elements = switch (kind) {
			case FIRE, FIRE_ATTACK -> single(FIRE);
			case ICE, ICE_ATTACK -> single(ICE);
			case ELECTRIC -> single(LIGHTNING);
			case SUN -> single(source.is(SephiriaDamage.ICE_SUN_SWORD) ? ICE : FIRE);
			case CRYO -> dual(player, ICE, LIGHTNING);
			case PLASMA -> dual(player, FIRE, LIGHTNING);
			// 混沌固定为紫色，不再拆属性
			case CHAOS -> single(CHAOS);
			// WEAPON / ARTIFACT / TRUE 以及其余默认按物理
			default -> single(PHYSICAL);
		};

		// 临时诊断：确认双属性伤害确实走到了 CRYO / PLASMA，以及主色是哪一边。
		if (elements.dual() && dualLogs < 10) {
			dualLogs++;
			Sephiria.LOGGER.info("[SEPHIRIA] dual damage: kind={}, primary=#{}, secondary=#{}",
					kind, String.format("%06X", color(elements.primary())),
					String.format("%06X", color(elements.secondary())));
		}

		return elements;
	}

	/**
	 * 这一发伤害由哪些元素组成。颜色表按元素集合查（见 {@link #colorForMask}）：
	 * 物理 / 火 / 冰 / 电四种可以任意组合，真伤与混沌单独算。
	 *
	 * <p>口径：普通攻击 / 神器伤害是物理；纯减益（灼伤 / 冰冻 / 触电持续伤害）是单元素；
	 * 带固定值的元素攻击（火 / 冰 / 电 attack）按「物理 + 元素」算；双属性神器按两组元素算；
	 * 永恒冬日按「物理 + 冰」。
	 */
	public static int maskOf(ServerPlayer player, DamageSource source) {
		if (source.is(SephiriaDamage.TRUE_DAMAGE)) {
			return BIT_TRUE;
		}

		if (source.is(SephiriaDamage.CHAOS)) {
			return BIT_CHAOS;
		}

		if (source.is(SephiriaDamage.ETERNAL_WINTER)) {
			return BIT_PHYSICAL | BIT_ICE;
		}

		if (source.is(SephiriaDamage.CRYO_GUILLOTINE)) {
			return BIT_ICE | BIT_LIGHTNING;
		}

		if (source.is(SephiriaDamage.PLASMA)) {
			return BIT_FIRE | BIT_LIGHTNING;
		}

		if (source.is(SephiriaDamage.SUN_SWORD)) {
			return BIT_FIRE;
		}

		if (source.is(SephiriaDamage.ICE_SUN_SWORD)) {
			return BIT_ICE;
		}

		if (source.is(SephiriaDamage.ELECTRIC_SHOCK)) {
			return BIT_LIGHTNING;
		}

		if (source.is(SephiriaDamage.ELECTRIC_ATTACK)) {
			return BIT_LIGHTNING | BIT_PHYSICAL;
		}

		if (source.is(SephiriaDamage.BURN)) {
			return BIT_FIRE;
		}

		if (source.is(SephiriaDamage.FIRE_ATTACK)) {
			return BIT_FIRE | BIT_PHYSICAL;
		}

		if (source.is(SephiriaDamage.ICE_ATTACK)) {
			return BIT_ICE | BIT_PHYSICAL;
		}

		if (source.is(SephiriaDamage.FREEZE) || source.is(SephiriaDamage.BLUE_BURN)) {
			return BIT_ICE;
		}

		return switch (SephiriaDamage.kindOf(source)) {
			case FIRE -> BIT_FIRE;
			case ICE -> BIT_ICE;
			case ELECTRIC -> BIT_LIGHTNING;
			case CHAOS -> BIT_CHAOS;
			case TRUE -> BIT_TRUE;
			case SUN -> BIT_FIRE;
			default -> BIT_PHYSICAL;
		};
	}

	/** 元素集合 → 单一显示色；颜色由用户 2026-10-26 给定的表。 */
	public static int colorForMask(int mask) {
		return switch (mask) {
			case BIT_PHYSICAL -> PHYSICAL_COLOR;
			case BIT_FIRE -> FIRE_COLOR;
			case BIT_ICE -> ICE_COLOR;
			case BIT_LIGHTNING -> LIGHTNING_COLOR;
			case BIT_FIRE | BIT_LIGHTNING -> 0xEE8AF8;
			case BIT_ICE | BIT_PHYSICAL -> 0x16417C;
			case BIT_ICE | BIT_LIGHTNING -> 0x3282F6;
			case BIT_FIRE | BIT_PHYSICAL -> 0xF08784;
			case BIT_LIGHTNING | BIT_PHYSICAL -> 0x5A879E;
			case BIT_FIRE | BIT_ICE -> 0xEF88BE;
			case BIT_ICE | BIT_FIRE | BIT_LIGHTNING -> 0x7F82BB;
			case BIT_FIRE | BIT_LIGHTNING | BIT_PHYSICAL -> 0xFFFD55;
			case BIT_ICE | BIT_LIGHTNING | BIT_PHYSICAL -> 0x3282F6;
			case BIT_FIRE | BIT_ICE | BIT_PHYSICAL -> 0xEA3FF7;
			case BIT_CHAOS -> CHAOS_COLOR;
			case BIT_TRUE -> TRUE_COLOR;
			// 四元素 / 未定义组合：用混沌紫兜底，至少不会退回旧色
			default -> CHAOS_COLOR;
		};
	}

	private static Elements single(int element) {
		return new Elements(element, -1);
	}

	/**
	 * 双属性：面板值较高的一边作为主色（数字上半边），较低的一边作为副色（下半边）。
	 * 并列时按传入顺序（冰在电前、火在电前）取主色。
	 */
	private static Elements dual(ServerPlayer player, int first, int second) {
		double firstValue = elementValue(player, first);
		double secondValue = elementValue(player, second);

		return firstValue >= secondValue
				? new Elements(first, second)
				: new Elements(second, first);
	}

	private static double elementValue(ServerPlayer player, int element) {
		return switch (element) {
			case FIRE -> PlayerStats.elementTotal(player, PlayerStats.Element.FIRE);
			case ICE -> PlayerStats.elementTotal(player, PlayerStats.Element.ICE);
			case LIGHTNING -> PlayerStats.elementTotal(player, PlayerStats.Element.LIGHTNING);
			default -> PlayerStats.elementTotal(player, PlayerStats.Element.PHYSICAL);
		};
	}

	/** 单元素调色板（组合色见 {@link #colorForMask}）：物理 #C0C0C0 / 火 #EDB12E / 冰 #6499E8 / 电 #7EFAFF / 混沌 #741B7C。 */
	public static int color(int element) {
		return switch (element) {
			case FIRE -> FIRE_COLOR;
			case ICE -> ICE_COLOR;
			case LIGHTNING -> LIGHTNING_COLOR;
			case CHAOS -> CHAOS_COLOR;
			default -> PHYSICAL_COLOR;
		};
	}

	/** 副元素的颜色；-1（单属性）返回 -1。 */
	public static int secondaryColor(int secondaryElement) {
		return secondaryElement < 0 ? -1 : color(secondaryElement);
	}

}
