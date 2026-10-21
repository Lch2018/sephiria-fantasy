package com.sephiria.slate;

import java.util.List;

/**
 * 各石板的影响范围（相对石板所在格的偏移与增量）。
 *
 * <p>数值集中在这里：每块石板一段，改范围与数值只动这一个文件。
 * 坐标是 {@code (dx, dy)}，dx 右为正、dy <b>下</b>为正；朝向 0 就是参考图里画的方向。
 *
 * <p>所有石板都不影响自己所在的格子，只影响周围——参考图里数字都画在旁边的格子里。
 */
public final class Slates {
	/** 未来：正上方三格 + 左侧一格，各 +1（可旋转）。 */
	public static final List<PatternSlateItem.Cell> FUTURE = List.of(
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(-1, -1, 1),
			new PatternSlateItem.Cell(1, -1, 1),
			new PatternSlateItem.Cell(-1, 0, 1));

	/** 誓言：正上方 +2、再上一格 +1，左右各 +1，正下方 +1（可旋转）。 */
	public static final List<PatternSlateItem.Cell> OATH = List.of(
			new PatternSlateItem.Cell(0, -2, 2),
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(-1, 0, 1),
			new PatternSlateItem.Cell(1, 0, 1),
			new PatternSlateItem.Cell(0, 1, 1));

	/** 信念：正上方一格 +5（可旋转）。 */
	public static final List<PatternSlateItem.Cell> BELIEF = List.of(
			new PatternSlateItem.Cell(0, -1, 5));

	/** 入口：正上方一排 +1 / +2 / +1（<b>不可旋转</b>）。 */
	public static final List<PatternSlateItem.Cell> ENTRANCE = List.of(
			new PatternSlateItem.Cell(-1, -1, 1),
			new PatternSlateItem.Cell(0, -1, 2),
			new PatternSlateItem.Cell(1, -1, 1));

	/** 竞争：左上方与正上方各 -1，正下方 +3（可旋转）。 */
	public static final List<PatternSlateItem.Cell> COMPETITION = List.of(
			new PatternSlateItem.Cell(-1, -1, -1),
			new PatternSlateItem.Cell(0, -1, -1),
			new PatternSlateItem.Cell(0, 1, 3));

	/** 集结：正上方与正左方各 +2（可旋转）。 */
	public static final List<PatternSlateItem.Cell> RALLY = List.of(
			new PatternSlateItem.Cell(0, -1, 2),
			new PatternSlateItem.Cell(-1, 0, 2));

	/** 波浪：正上方 -1、正右方 -1、右上方 +3（可旋转）。 */
	public static final List<PatternSlateItem.Cell> WAVE = List.of(
			new PatternSlateItem.Cell(0, -1, -1),
			new PatternSlateItem.Cell(1, 0, -1),
			new PatternSlateItem.Cell(1, -1, 3));

	/** 双星：正上方第二格与正下方第二格各 +2（可旋转）。 */
	public static final List<PatternSlateItem.Cell> DOUBLE_STAR = List.of(
			new PatternSlateItem.Cell(0, -2, 2),
			new PatternSlateItem.Cell(0, 2, 2));

	/** 握手：正上方与正下方各 +1（可旋转）。 */
	public static final List<PatternSlateItem.Cell> HANDSHAKE = List.of(
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(0, 1, 1));


	/** 断绝（稀有，不可旋转）：断绝。 */
	public static final List<PatternSlateItem.Cell> SEVERANCE = List.of(
			new PatternSlateItem.Cell(0, -1, 3),
			new PatternSlateItem.Cell(0, 1, 3),
			new PatternSlateItem.Cell(-1, 0, -1),
			new PatternSlateItem.Cell(1, 0, -1));

	/** 楔子（稀有，可旋转）：楔子。 */
	public static final List<PatternSlateItem.Cell> WEDGE = List.of(
			new PatternSlateItem.Cell(-1, -1, 3));

	/** 防御招式（稀有，可旋转）：防御招式。 */
	public static final List<PatternSlateItem.Cell> DEFENSE = List.of(
			new PatternSlateItem.Cell(-1, -1, 1),
			new PatternSlateItem.Cell(1, 1, 1),
			new PatternSlateItem.Cell(1, -1, 2),
			new PatternSlateItem.Cell(-1, 1, 2),
			new PatternSlateItem.Cell(-1, 0, -1),
			new PatternSlateItem.Cell(1, 0, -1));

	/** 干燥（普通，不可旋转）：干燥。 */
	public static final List<PatternSlateItem.Cell> DROUGHT = List.of(
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(0, 1, 1));

	/** 剥削（普通，可旋转）：剥削。 */
	public static final List<PatternSlateItem.Cell> EXPLOITATION = List.of(
			new PatternSlateItem.Cell(-1, 0, -1),
			new PatternSlateItem.Cell(1, 0, 1));

	/** 团结（普通，可旋转）：团结。 */
	public static final List<PatternSlateItem.Cell> UNITY = List.of(
			new PatternSlateItem.Cell(0, 1, 1),
			new PatternSlateItem.Cell(1, 0, 1),
			new PatternSlateItem.Cell(-1, 0, -1),
			new PatternSlateItem.Cell(0, -1, -1));

	/** 和平（传说，可旋转）：和平。 */
	public static final List<PatternSlateItem.Cell> PEACE = List.of(
			new PatternSlateItem.Cell(-1, 0, 3),
			new PatternSlateItem.Cell(1, 0, 3));

	/** 白日梦（传说，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> DAYDREAM = List.of(
			new PatternSlateItem.Cell(1, -1, 2),
			new PatternSlateItem.Cell(-1, -1, 2),
			new PatternSlateItem.Cell(-1, 1, 2),
			new PatternSlateItem.Cell(1, 1, 2));

	/** 反抗（稀有，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> REBELLION = List.of(
			new PatternSlateItem.Cell(1, 1, 1),
			new PatternSlateItem.Cell(2, 2, 1),
			new PatternSlateItem.Cell(3, 3, 1),
			new PatternSlateItem.Cell(4, 4, 1),
			new PatternSlateItem.Cell(5, 5, 1),
			new PatternSlateItem.Cell(6, 6, 1),
			new PatternSlateItem.Cell(7, 7, 1),
			new PatternSlateItem.Cell(8, 8, 1),
			new PatternSlateItem.Cell(-1, -1, 1),
			new PatternSlateItem.Cell(-2, -2, 1),
			new PatternSlateItem.Cell(-3, -3, 1),
			new PatternSlateItem.Cell(-4, -4, 1),
			new PatternSlateItem.Cell(-5, -5, 1),
			new PatternSlateItem.Cell(-6, -6, 1),
			new PatternSlateItem.Cell(-7, -7, 1),
			new PatternSlateItem.Cell(-8, -8, 1));

	/** 接合（稀有，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> JUNCTION = List.of(
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(0, -2, 1),
			new PatternSlateItem.Cell(0, -3, 1),
			new PatternSlateItem.Cell(1, 0, 1),
			new PatternSlateItem.Cell(2, 0, 1),
			new PatternSlateItem.Cell(3, 0, 1));

	/** 报恩（永恒，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> GRATITUDE = List.of(
			new PatternSlateItem.Cell(-1, 0, 3),
			new PatternSlateItem.Cell(-2, 0, 3),
			new PatternSlateItem.Cell(-3, 0, 3),
			new PatternSlateItem.Cell(-4, 0, 3),
			new PatternSlateItem.Cell(1, 0, 3),
			new PatternSlateItem.Cell(2, 0, 3),
			new PatternSlateItem.Cell(3, 0, 3),
			new PatternSlateItem.Cell(4, 0, 3),
			new PatternSlateItem.Cell(-5, 0, 3),
			new PatternSlateItem.Cell(-6, 0, 3),
			new PatternSlateItem.Cell(-7, 0, 3),
			new PatternSlateItem.Cell(-8, 0, 3),
			new PatternSlateItem.Cell(5, 0, 3),
			new PatternSlateItem.Cell(6, 0, 3),
			new PatternSlateItem.Cell(7, 0, 3),
			new PatternSlateItem.Cell(8, 0, 3));

	/** 到来（普通，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> ARRIVAL = List.of(
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(0, -2, 1),
			new PatternSlateItem.Cell(0, 1, -1),
			new PatternSlateItem.Cell(0, 2, -1));

	/** 近似（普通，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> APPROXIMATION = List.of(
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(1, 0, 1));

	/** 恶作剧（普通，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> MISCHIEF = List.of(
			new PatternSlateItem.Cell(-1, -1, 1),
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(1, -1, 1),
			new PatternSlateItem.Cell(-1, 0, -1),
			new PatternSlateItem.Cell(1, 0, -1));

	/** 滋养（普通，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> NOURISHMENT = List.of(
			new PatternSlateItem.Cell(-1, -1, 1),
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(1, -1, 1),
			new PatternSlateItem.Cell(0, 1, -1),
			new PatternSlateItem.Cell(0, 2, -1));

	/** 智慧（普通，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> WISDOM = List.of(
			new PatternSlateItem.Cell(-1, -1, 1));

	/** 骑士道（普通，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> CHIVALRY = List.of(
			new PatternSlateItem.Cell(-1, -2, 1));

	/** 命运（普通，不可旋转）：。 */
	public static final List<PatternSlateItem.Cell> FATE = List.of(
			new PatternSlateItem.Cell(0, 1, 1));

	/** 凝视（普通，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> GAZE = List.of(
			new PatternSlateItem.Cell(-1, -1, 1),
			new PatternSlateItem.Cell(1, 1, -1));

	/** 善意（普通，不可旋转）：。 */
	public static final List<PatternSlateItem.Cell> GOODWILL = List.of(
			new PatternSlateItem.Cell(-1, 0, 1),
			new PatternSlateItem.Cell(1, 0, 1));

	/** 希望（普通，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> HOPE = List.of(
			new PatternSlateItem.Cell(1, 0, 1));

	/** 欢呼（普通，不可旋转）：。 */
	public static final List<PatternSlateItem.Cell> CHEER = List.of(
			new PatternSlateItem.Cell(0, -1, 1));

	/** 出口（高级，不可旋转）：。 */
	public static final List<PatternSlateItem.Cell> EXIT = List.of(
			new PatternSlateItem.Cell(-1, 1, 1),
			new PatternSlateItem.Cell(0, 1, 2),
			new PatternSlateItem.Cell(1, 1, 1));

	/** 进步（高级，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> PROGRESS = List.of(
			new PatternSlateItem.Cell(-1, -1, 2),
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(-1, 0, 1));

	/** 三头（高级，不可旋转）：。 */
	public static final List<PatternSlateItem.Cell> THREE_HEADS = List.of(
			new PatternSlateItem.Cell(-1, 0, 1),
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(1, 0, 1));

	/** 准备（高级，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> PREPARATION = List.of(
			new PatternSlateItem.Cell(-1, -1, 1),
			new PatternSlateItem.Cell(1, 1, 2));

	/** 分配（高级，不可旋转）：。 */
	public static final List<PatternSlateItem.Cell> DISTRIBUTION = List.of(
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(-1, 0, 1),
			new PatternSlateItem.Cell(0, 1, 1),
			new PatternSlateItem.Cell(1, 0, 1));

	/** 堆叠（高级，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> STACK = List.of(
			new PatternSlateItem.Cell(-1, -1, 1),
			new PatternSlateItem.Cell(-1, -2, 1),
			new PatternSlateItem.Cell(0, -2, 1),
			new PatternSlateItem.Cell(0, -1, 1));

	/** 荣誉（高级，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> HONOR = List.of(
			new PatternSlateItem.Cell(-1, -2, 1),
			new PatternSlateItem.Cell(0, -1, 2));

	/** 过去（高级，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> PAST = List.of(
			new PatternSlateItem.Cell(1, 0, 1),
			new PatternSlateItem.Cell(1, -1, 1),
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(-1, -1, 1));

	/** 前进（高级，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> ADVANCE = List.of(
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(0, -2, 1),
			new PatternSlateItem.Cell(0, -3, 1));

	/** 献礼（高级，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> OFFERING = List.of(
			new PatternSlateItem.Cell(-1, -1, 1),
			new PatternSlateItem.Cell(-1, 1, 1),
			new PatternSlateItem.Cell(1, 1, 1),
			new PatternSlateItem.Cell(1, -1, 1));

	/** 渴望（高级，不可旋转）：。 */
	public static final List<PatternSlateItem.Cell> DESIRE = List.of(
			new PatternSlateItem.Cell(0, -1, 2));

	/** 鼓动（高级，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> AGITATION = List.of(
			new PatternSlateItem.Cell(0, -2, 2));

	/** 收获（高级，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> HARVEST = List.of(
			new PatternSlateItem.Cell(0, -1, 2),
			new PatternSlateItem.Cell(0, 1, 2));

	/** 转移（高级，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> TRANSFER = List.of(
			new PatternSlateItem.Cell(0, -8, -1),
			new PatternSlateItem.Cell(0, -7, -1),
			new PatternSlateItem.Cell(0, -6, -1),
			new PatternSlateItem.Cell(0, -5, -1),
			new PatternSlateItem.Cell(0, -4, -1),
			new PatternSlateItem.Cell(0, -3, -1),
			new PatternSlateItem.Cell(0, -2, -1),
			new PatternSlateItem.Cell(0, -1, -1),
			new PatternSlateItem.Cell(0, 1, -1),
			new PatternSlateItem.Cell(0, 2, -1),
			new PatternSlateItem.Cell(0, 3, -1),
			new PatternSlateItem.Cell(0, 4, -1),
			new PatternSlateItem.Cell(0, 5, -1),
			new PatternSlateItem.Cell(0, 6, -1),
			new PatternSlateItem.Cell(0, 7, -1),
			new PatternSlateItem.Cell(0, 8, -1),
			new PatternSlateItem.Cell(-1, 0, 1),
			new PatternSlateItem.Cell(-2, 0, 1),
			new PatternSlateItem.Cell(-3, 0, 1),
			new PatternSlateItem.Cell(-4, 0, 1),
			new PatternSlateItem.Cell(-5, 0, 1),
			new PatternSlateItem.Cell(-6, 0, 1),
			new PatternSlateItem.Cell(-7, 0, 1),
			new PatternSlateItem.Cell(-8, 0, 1),
			new PatternSlateItem.Cell(1, 0, 1),
			new PatternSlateItem.Cell(2, 0, 1),
			new PatternSlateItem.Cell(4, 0, 1),
			new PatternSlateItem.Cell(3, 0, 1),
			new PatternSlateItem.Cell(5, 0, 1),
			new PatternSlateItem.Cell(6, 0, 1),
			new PatternSlateItem.Cell(7, 0, 1),
			new PatternSlateItem.Cell(8, 0, 1));

	/** 正义（高级，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> JUSTICE = List.of(
			new PatternSlateItem.Cell(0, -4, 1),
			new PatternSlateItem.Cell(0, -3, 1),
			new PatternSlateItem.Cell(0, -2, 1),
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(0, -5, 1),
			new PatternSlateItem.Cell(0, -6, 1),
			new PatternSlateItem.Cell(0, -7, 1),
			new PatternSlateItem.Cell(0, -8, 1),
			new PatternSlateItem.Cell(0, 1, 1),
			new PatternSlateItem.Cell(0, 2, 1),
			new PatternSlateItem.Cell(0, 3, 1),
			new PatternSlateItem.Cell(0, 8, 1),
			new PatternSlateItem.Cell(0, 7, 1),
			new PatternSlateItem.Cell(0, 5, 1),
			new PatternSlateItem.Cell(0, 6, 1),
			new PatternSlateItem.Cell(0, 4, 1));

	/** 聚集（高级，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> GATHERING = List.of(
			new PatternSlateItem.Cell(0, 1, -1),
			new PatternSlateItem.Cell(-1, 1, -1),
			new PatternSlateItem.Cell(-2, 1, -1),
			new PatternSlateItem.Cell(-3, 1, -1),
			new PatternSlateItem.Cell(-4, 1, -1),
			new PatternSlateItem.Cell(-5, 1, -1),
			new PatternSlateItem.Cell(-6, 1, -1),
			new PatternSlateItem.Cell(-7, 1, -1),
			new PatternSlateItem.Cell(-8, 1, -1),
			new PatternSlateItem.Cell(1, 1, -1),
			new PatternSlateItem.Cell(2, 1, -1),
			new PatternSlateItem.Cell(3, 1, -1),
			new PatternSlateItem.Cell(4, 1, -1),
			new PatternSlateItem.Cell(5, 1, -1),
			new PatternSlateItem.Cell(6, 1, -1),
			new PatternSlateItem.Cell(7, 1, -1),
			new PatternSlateItem.Cell(8, 1, -1),
			new PatternSlateItem.Cell(0, -1, 3));

	/** 旗帜（稀有，不可旋转）：。 */
	public static final List<PatternSlateItem.Cell> BANNER = List.of(
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(1, 0, 1),
			new PatternSlateItem.Cell(2, 0, 2),
			new PatternSlateItem.Cell(3, 0, 3),
			new PatternSlateItem.Cell(0, 1, -1));

	/** 同时性（稀有，不可旋转）：。 */
	public static final List<PatternSlateItem.Cell> SIMULTANEITY = List.of(
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(0, -2, 1),
			new PatternSlateItem.Cell(0, -3, 1),
			new PatternSlateItem.Cell(0, -4, 1),
			new PatternSlateItem.Cell(0, -5, 1),
			new PatternSlateItem.Cell(0, -6, 1),
			new PatternSlateItem.Cell(0, -7, 1),
			new PatternSlateItem.Cell(0, -8, 1),
			new PatternSlateItem.Cell(0, 1, 1),
			new PatternSlateItem.Cell(0, 2, 1),
			new PatternSlateItem.Cell(0, 3, 1),
			new PatternSlateItem.Cell(0, 4, 1),
			new PatternSlateItem.Cell(0, 5, 1),
			new PatternSlateItem.Cell(0, 6, 1),
			new PatternSlateItem.Cell(0, 7, 1),
			new PatternSlateItem.Cell(0, 8, 1));

	/** 破釜沉舟（稀有，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> BURN_THE_BOATS = List.of(
			new PatternSlateItem.Cell(0, -1, 5),
			new PatternSlateItem.Cell(1, 0, -1),
			new PatternSlateItem.Cell(0, 1, -1),
			new PatternSlateItem.Cell(-1, 0, -1));

	/** 基础（稀有，不可旋转）：。 */
	public static final List<PatternSlateItem.Cell> FOUNDATION = List.of(
			new PatternSlateItem.Cell(-1, 0, 1),
			new PatternSlateItem.Cell(-3, 0, 1),
			new PatternSlateItem.Cell(-2, 0, 1),
			new PatternSlateItem.Cell(-4, 0, 1),
			new PatternSlateItem.Cell(-5, 0, 1),
			new PatternSlateItem.Cell(-6, 0, 1),
			new PatternSlateItem.Cell(-7, 0, 1),
			new PatternSlateItem.Cell(-8, 0, 1),
			new PatternSlateItem.Cell(1, 0, 1),
			new PatternSlateItem.Cell(2, 0, 1),
			new PatternSlateItem.Cell(4, 0, 1),
			new PatternSlateItem.Cell(3, 0, 1),
			new PatternSlateItem.Cell(5, 0, 1),
			new PatternSlateItem.Cell(7, 0, 1),
			new PatternSlateItem.Cell(6, 0, 1),
			new PatternSlateItem.Cell(8, 0, 1));

	/** 权能（稀有，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> AUTHORITY = List.of(
			new PatternSlateItem.Cell(0, -1, 3));

	/** 压缩（传说，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> COMPRESSION = List.of(
			new PatternSlateItem.Cell(0, -3, 1),
			new PatternSlateItem.Cell(0, -2, 2),
			new PatternSlateItem.Cell(0, -1, 3));

	/** 奇迹（传说，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> MIRACLE = List.of(
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(0, -2, 1),
			new PatternSlateItem.Cell(0, -3, 1),
			new PatternSlateItem.Cell(0, -4, 1),
			new PatternSlateItem.Cell(0, -5, 1),
			new PatternSlateItem.Cell(0, -6, 1),
			new PatternSlateItem.Cell(0, -7, 1),
			new PatternSlateItem.Cell(0, -8, 1),
			new PatternSlateItem.Cell(-1, 0, 1),
			new PatternSlateItem.Cell(-2, 0, 1),
			new PatternSlateItem.Cell(-3, 0, 1),
			new PatternSlateItem.Cell(-4, 0, 1),
			new PatternSlateItem.Cell(-5, 0, 1),
			new PatternSlateItem.Cell(-6, 0, 1),
			new PatternSlateItem.Cell(-7, 0, 1),
			new PatternSlateItem.Cell(-8, 0, 1),
			new PatternSlateItem.Cell(1, 0, 1),
			new PatternSlateItem.Cell(2, 0, 1),
			new PatternSlateItem.Cell(3, 0, 1),
			new PatternSlateItem.Cell(4, 0, 1),
			new PatternSlateItem.Cell(5, 0, 1),
			new PatternSlateItem.Cell(6, 0, 1),
			new PatternSlateItem.Cell(7, 0, 1),
			new PatternSlateItem.Cell(8, 0, 1),
			new PatternSlateItem.Cell(0, 1, 1),
			new PatternSlateItem.Cell(0, 3, 1),
			new PatternSlateItem.Cell(0, 2, 1),
			new PatternSlateItem.Cell(0, 4, 1),
			new PatternSlateItem.Cell(0, 6, 1),
			new PatternSlateItem.Cell(0, 7, 1),
			new PatternSlateItem.Cell(0, 5, 1),
			new PatternSlateItem.Cell(0, 8, 1));

	/** 荆棘（传说，不可旋转）：。 */
	public static final List<PatternSlateItem.Cell> THORNS = List.of(
			new PatternSlateItem.Cell(0, -1, 2),
			new PatternSlateItem.Cell(1, -1, 1),
			new PatternSlateItem.Cell(1, 0, 1),
			new PatternSlateItem.Cell(1, 1, 1),
			new PatternSlateItem.Cell(0, 1, 2),
			new PatternSlateItem.Cell(-1, 1, 1),
			new PatternSlateItem.Cell(-1, 0, 1),
			new PatternSlateItem.Cell(-1, -1, 1));

	/** 光辉（传说，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> RADIANCE = List.of(
			new PatternSlateItem.Cell(-1, 0, 1),
			new PatternSlateItem.Cell(-2, 0, 1),
			new PatternSlateItem.Cell(-3, 0, 1),
			new PatternSlateItem.Cell(-4, 0, 1),
			new PatternSlateItem.Cell(-6, 0, 1),
			new PatternSlateItem.Cell(-5, 0, 1),
			new PatternSlateItem.Cell(-7, 0, 1),
			new PatternSlateItem.Cell(-8, 0, 1),
			new PatternSlateItem.Cell(1, 0, 1),
			new PatternSlateItem.Cell(2, 0, 1),
			new PatternSlateItem.Cell(3, 0, 1),
			new PatternSlateItem.Cell(4, 0, 1),
			new PatternSlateItem.Cell(5, 0, 1),
			new PatternSlateItem.Cell(6, 0, 1),
			new PatternSlateItem.Cell(7, 0, 1),
			new PatternSlateItem.Cell(8, 0, 1),
			new PatternSlateItem.Cell(0, -1, 2),
			new PatternSlateItem.Cell(0, 1, 2));

	/** 勇气（传说，可旋转）：。 */
	public static final List<PatternSlateItem.Cell> COURAGE = List.of(
			new PatternSlateItem.Cell(-1, -1, 1),
			new PatternSlateItem.Cell(-2, -2, 1),
			new PatternSlateItem.Cell(-3, -3, 1),
			new PatternSlateItem.Cell(-4, -4, 1),
			new PatternSlateItem.Cell(-5, -5, 1),
			new PatternSlateItem.Cell(-6, -6, 1),
			new PatternSlateItem.Cell(-7, -7, 1),
			new PatternSlateItem.Cell(-8, -8, 1),
			new PatternSlateItem.Cell(1, 1, 1),
			new PatternSlateItem.Cell(2, 2, 1),
			new PatternSlateItem.Cell(3, 3, 1),
			new PatternSlateItem.Cell(4, 4, 1),
			new PatternSlateItem.Cell(5, 5, 1),
			new PatternSlateItem.Cell(6, 6, 1),
			new PatternSlateItem.Cell(7, 7, 1),
			new PatternSlateItem.Cell(8, 8, 1),
			new PatternSlateItem.Cell(1, -1, 2),
			new PatternSlateItem.Cell(-1, 1, 2));
	private Slates() {
	}
}
