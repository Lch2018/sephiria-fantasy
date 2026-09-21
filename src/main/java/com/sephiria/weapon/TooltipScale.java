package com.sephiria.weapon;

/**
 * 提示框数值的换算倍率。
 *
 * <p>武器类里的常量是「基准值」（属性为默认值时的面板值），而提示框要显示玩家<b>当前</b>属性下
 * 的实际数字。拼提示框之前由客户端把当前倍率放进来（读属性镜像），拼完立刻清掉；
 * 服务端与其它调用方保持 1.0，也就是原样显示基准值。
 *
 * <p>只影响显示值，括号里的说明仍按基准值算——「（10.25%物理强度）」表达的是面板值 ÷ 20 这个
 * 固有倍率，不随玩家属性变。
 */
public final class TooltipScale {
	private static final double NONE = 1.0D;
	private static double damage = NONE;
	private static double attackSpeed = NONE;
	private static double range = NONE;

	private TooltipScale() {
	}

	public static void set(double damageMultiplier, double attackSpeedMultiplier, double rangeMultiplier) {
		damage = damageMultiplier;
		attackSpeed = attackSpeedMultiplier;
		range = rangeMultiplier;
	}

	public static void reset() {
		damage = NONE;
		attackSpeed = NONE;
		range = NONE;
	}

	static double damage() {
		return damage;
	}

	static double attackSpeed() {
		return attackSpeed;
	}

	static double range() {
		return range;
	}
}
