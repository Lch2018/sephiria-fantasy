package com.sephiria.weapon;

/**
 * SEPHIRIA 原作的六大武器分支。
 *
 * <p>{@code procMultiplier} 是原作里该类型的命中倍率，也就是触发类效果（proc）的伤害系数；
 * 各分支基础武器的数值差异就按这个倍率换算。分支之间不互通，所以后续的锻造系统
 * （基础武器 → 一级变种 → 二级变种）只会沿着同一分支展开。
 */
public enum WeaponBranch {
	SWORD_AND_SHIELD("sword_and_shield", 1.0F, "100%"),
	GREATSWORD("greatsword", 1.5F, "150%"),
	DAGGER("dagger", 0.6875F, "68.75%"),
	CROSSBOW("crossbow", 1.2F, "120%"),
	KATANA("katana", 0.825F, "82.5%"),
	STAFF("staff", 0.9F, "90%");

	private final String id;
	private final float procMultiplier;
	private final String procMultiplierText;

	WeaponBranch(String id, float procMultiplier, String procMultiplierText) {
		this.id = id;
		this.procMultiplier = procMultiplier;
		this.procMultiplierText = procMultiplierText;
	}

	/** 分支标识，同时用于物品名与语言文件的键名。 */
	public String id() {
		return this.id;
	}

	public float procMultiplier() {
		return this.procMultiplier;
	}

	/** 倍率的展示文本，例如 {@code 68.75%}。 */
	public String procMultiplierText() {
		return this.procMultiplierText;
	}

	/** 语言文件里的分支名字键，如 {@code sephiria.branch.greatsword}。 */
	public String translationKey() {
		return "sephiria_fantasy.branch." + this.id;
	}

	/** 弩是唯一的远程分支，其余都是近战。 */
	public boolean isRanged() {
		return this == CROSSBOW;
	}
}
