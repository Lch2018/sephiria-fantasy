package com.sephiria.weapon;

import com.sephiria.stats.PlayerStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * 标记这个物品是一把 SEPHIRIA 武器，并给出它所属的分支。
 *
 * <p>近战武器与远程武器走的是不同的原版父类，锻造系统接入后需要按分支而不是按父类来处理，
 * 所以这里统一成一个接口。
 *
 * <p>提示框里的详细描述由各武器自己报告（{@link #detailLines}）：数值一律取自武器类顶部的
 * 常量，所以改常量的同时描述就跟着变，不会出现"写着 12、实际打 8"。
 *
 * <p>注意 26.2 已把 {@code Item#appendHoverText} 标记为过时（提示框改为由数据组件实现
 * {@code TooltipProvider} 提供），所以这里只负责拼文本，实际添加由客户端的
 * {@code ItemTooltipCallback} 完成。
 */
public interface SephiriaWeapon {
	/** 描述行里数值的颜色：伤害橙黄、攻速黄、范围与位移白、冷却青；标签沿用提示框默认的灰。 */
	int COLOR_DAMAGE = 0xFFFFA64D;
	int COLOR_ATTACK_SPEED = 0xFFFFF04D;
	int COLOR_RANGE = 0xFFFFFFFF;
	int COLOR_COOLDOWN = 0xFF4DD8FF;

	/**
	 * 范围基准：原版铁剑的横扫（以命中目标为中心水平膨胀 1.0 格）= 100。
	 *
	 * <p>所有范围都按这个基准显示——3.3 格就是 330——与 {@code 武器数据表.txt} 同一套写法。
	 */
	double RANGE_BASIS = 100.0D;

	WeaponBranch branch();

	/**
	 * 详细描述：由武器自己报告技能数值与效果。
	 *
	 * <p>每一行都用 {@link Line} 拼，格式统一；没有描述时返回空列表。
	 */
	default java.util.List<Component> detailLines(ItemStack stack) {
		return java.util.List.of();
	}

	/** 技能效果描述行：{@code 技能名描述：效果}。 */
	default Component desc(String skillKey, String descKey) {
		return Component.translatable("tooltip.sephiria.tip.desc",
				Component.translatable(skillKey), Component.translatable(descKey));
	}

	/** 数值格式化：整数不带小数点，其余保留两位。 */
	default String format(double value) {
		return Line.number(value);
	}

	/** 时长格式化：tick → 秒。 */
	default String seconds(int ticks) {
		return format(ticks / 20.0D);
	}

	/**
	 * 描述行的拼装器：统一「标签：数值（说明）」的格式与配色。
	 *
	 * <p>格式示例（斜杠分隔各项、括号里是换算说明，<b>只有每项的主数值上色</b>）：
	 * <pre>普通攻击：伤害：2.05（10.25%物理强度）/攻速：6.4（6.4*攻击速度）/范围：100（100*近战攻击范围）</pre>
	 *
	 * <p>范围一律用「原版铁剑的横扫 = 100」这个基准表示（3.3 格就是 330），与
	 * {@code 武器数据表.txt} 一致。
	 */
	final class Line {
		private final MutableComponent line;
		private boolean first = true;

		private Line(String labelKey, Object... labelArgs) {
			this.line = Component.translatable(labelKey, labelArgs);
		}

		/**
		 * 以「名称：」开头的一行。
		 *
		 * <p>冒号也走语言键（中英文标点不同），所以名称本身不带冒号——
		 * 同一个名称键还能给描述行（{@code 招架描述：…}）复用。
		 */
		static Line titled(String nameKey, Object... nameArgs) {
			return new Line("tooltip.sephiria.tip.label", Component.translatable(nameKey, nameArgs));
		}

		/** 普通攻击行：伤害 / 攻速 / 范围。 */
		static Line attack(double damage, double attackSpeed, double range) {
			return titled("tooltip.sephiria.tip.attack").damage(damage).attackSpeed(attackSpeed).range(range);
		}

		/** 远程武器的普通攻击行：没有横扫，所以不写范围，改写「无」。 */
		static Line attackRanged(double damage, double attackSpeed) {
			return titled("tooltip.sephiria.tip.attack").damage(damage).attackSpeed(attackSpeed).rangeNone();
		}

		/** 同上，但名称带上状态（刀的入鞘 / 出鞘）。 */
		static Line attack(String stateKey, double damage, double attackSpeed, double range) {
			return titled("tooltip.sephiria.tip.attack_state", Component.translatable(stateKey))
					.damage(damage).attackSpeed(attackSpeed).range(range);
		}

		/**
		 * 伤害：主数值橙黄、按当前物理强度换算后显示；括号里给的是<b>基准值</b>
		 * 相对物理强度基准（20）的倍率，不随属性变化。
		 */
		Line damage(double value) {
			return damage(value, TooltipScale.damage());
		}

		/** 技能那一行的伤害：比普攻多乘一项「特殊攻击伤害」（见武器数据表第十三节）。 */
		Line skillDamage(double value) {
			return damage(value, TooltipScale.skillDamage());
		}

		private Line damage(double value, double scale) {
			return part(Component.translatable("tooltip.sephiria.part.damage",
					number(value * scale, COLOR_DAMAGE),
					number(value / PlayerStats.DEFAULT_STRENGTH * 100.0D) + "%"));
		}

		/** 攻速：主数值黄色、按当前攻击速度换算；括号里给出「基准值 ×攻击速度」这个换算关系。 */
		Line attackSpeed(double value) {
			return part(Component.translatable("tooltip.sephiria.part.attack_speed",
					number(value * TooltipScale.attackSpeed(), COLOR_ATTACK_SPEED), number(value)));
		}

		/**
		 * 范围：传<b>格数</b>（水平半径），显示时换算成基准值（1 格 = {@value #RANGE_BASIS}）
		 * 并按当前近战攻击范围放大；括号里给出「基准值 ×近战攻击范围」这个换算关系。
		 */
		Line range(double blocks) {
			double basis = blocks * RANGE_BASIS * TooltipScale.range();
			double baseBasis = blocks * RANGE_BASIS;

			return part(Component.translatable("tooltip.sephiria.part.range",
					number(basis, COLOR_RANGE), number(baseBasis)));
		}

		/** 该项没有范围时用（远程武器不触发横扫）。 */
		Line rangeNone() {
			return part(Component.translatable("tooltip.sephiria.part.range_none"));
		}

		/** 冷却：青色。 */
		Line cooldown(double seconds) {
			return part(Component.translatable("tooltip.sephiria.part.cooldown", number(seconds, COLOR_COOLDOWN)));
		}

		/** 冷却：按 tick 传（内部换算成秒），省得各处再写一遍除法。 */
		Line cooldownTicks(int ticks) {
			return cooldown(ticks / 20.0D);
		}

		/** 冷却与上一项共用（回击 2/3 是同一个技能的后续段）。 */
		Line cooldownSame() {
			return part(Component.translatable("tooltip.sephiria.part.cooldown_same"));
		}

		/** 位移距离：白色。 */
		Line distance(double blocks) {
			return part(Component.translatable("tooltip.sephiria.part.distance", number(blocks, COLOR_RANGE)));
		}

		/**
		 * 自定义一项：语言键里带一个数值占位符，数值按 {@code color} 上色。
		 *
		 * <p>单位之类的文字直接写在语言键里（如 {@code 减伤：%s%%}），这样各项不必共用一套单位表。
		 */
		Line stat(String key, double value, int color) {
			return part(Component.translatable(key, number(value, color)));
		}

		Component build() {
			return this.line;
		}

		private Line part(Component part) {
			if (!this.first) {
				this.line.append("/");
			}

			this.first = false;
			this.line.append(part);
			return this;
		}

		private static MutableComponent number(double value, int color) {
			return Component.literal(number(value)).withColor(color);
		}

		/** 数值格式化：整数不带小数点，小数最多两位并去掉末尾的 0（0.80 → 0.8、10.25 保留）。 */
		static String number(double value) {
			return com.sephiria.util.Numbers.format(value);
		}
	}
}
