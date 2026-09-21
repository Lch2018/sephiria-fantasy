package com.sephiria.artifact;

/**
 * 有品质的东西：神器、石板、药水。
 *
 * <p>抽奖（宝箱、商店）要按品质加权，而品质只有这一个入口——所以三类物品都实现它，
 * 抽奖那边就只用认这一个接口，不用一个个 instanceof。
 */
public interface Quality {
	ArtifactRarity rarity();
}
