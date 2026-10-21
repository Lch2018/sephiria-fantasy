package com.sephiria.talent;

/**
 * 天赋页的种类：每一种对应界面上的一条天赋页。
 *
 * <p>颜色是用户指定的天赋色：加点数字、点亮的阶段图标都用它。每条天赋页的加成数值
 * 统一放在 {@link Talents} 里，这里只留界面需要的东西（id / 颜色 / 属性文案键）。
 */
public enum Talent {
	/** 愤怒：每点 +1% 暴击几率，阶段给暴击伤害、BOSS 伤害与防御穿透。 */
	RAGE("rage", 0xBE2C33),
	/** 迅捷：每点 +1% 攻击速度，阶段给闪避、冲刺与武器伤害。 */
	SWIFT("swift", 0xACD9C3),
	/** 生存：每点 +0.6 最大 HP，阶段给药水随机属性、背包格与石板刻印。 */
	SURVIVAL("survival", 0xD49ED8),
	/** 耐心：每点 +1 防御力，阶段给石板基础、低血回复与十日一次复活。 */
	PATIENCE("patience", 0xD4D7DD),
	/** 智慧：每点 +1 战斗 MP 再生，阶段给 MP 吸收 / 最大 MP、神器合并与宝箱重掷。 */
	WISDOM("wisdom", 0xA6DFDE),
	/** 意志：每点 +1 幸运值，阶段给许愿池锚定格、最大 HP / MP。 */
	WILL("will", 0xADCF88),
	/** 急智：每点 +0.3 最大 HP / +0.5 闪避，阶段给背包格、宝箱选择与连击增伤。 */
	QUICK_WIT("quick_wit", 0xF59F42);

	/** 图标与语言键用的 id。 */
	private final String id;
	/** 天赋色（不带 alpha）。 */
	private final int colour;

	Talent(String id, int colour) {
		this.id = id;
		this.colour = colour;
	}

	public String id() {
		return this.id;
	}

	/** 天赋色（0xRRGGBB）。 */
	public int colour() {
		return this.colour;
	}

	/** 界面按这个下标发加点包，服务端按同一顺序解析。 */
	public int index() {
		return ordinal();
	}

	/** 加点数量对应的阶段（0 = 一个都没点亮，1..3 = 已点亮的阶段数）。 */
	public int stage(int points) {
		int stage = 0;

		for (int threshold : Talents.STAGE_POINTS) {
			if (points >= threshold) {
				stage++;
			}
		}

		return stage;
	}
}
