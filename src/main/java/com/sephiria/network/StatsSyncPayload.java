package com.sephiria.network;

import com.sephiria.Sephiria;
import com.sephiria.stats.PlayerStats;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：玩家的自定义属性最新值（进服与每次变更时各推一次）。
 *
 * <p>1.20.1 沿用旧 channel 写法；字段顺序与 26.1.2 的 0.5.3.8 保持一致（含魔法书 / 护盾新增项）。
 */
public record StatsSyncPayload(double mp, double mpRegen, double physical, double fire, double ice,
		double lightning, double defense, double attackSpeed, double meleeRange, double physicalAmp,
		double physicalBase, double artifactFlat, double potionFlat, double artifactPercent,
		double potionPercent, double attackBase, double attackArtifactFlat, double attackPotionFlat,
		double attackArtifactPercent, double attackPotionPercent, double leaves, double weaponDamage,
		double specialAttack, double lifesteal, double normalAttackDamage, double critChance, double critDamage,
		double ignoreDefense, double moveSpeed, double dodge, double dodgeRate, double negotiation,
		double shopDiscount, double leafGainPercent, double xpDropPercent, double maxMp, double luck,
		double mpAbsorb, double magicBookHaste, double magicBookMpReduction,
		double magicBoltMpCostIncrease, double mpReserved) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "stats_sync");

	public static void write(FriendlyByteBuf buffer, StatsSyncPayload p) {
		buffer.writeDouble(p.mp());
		buffer.writeDouble(p.mpRegen());
		buffer.writeDouble(p.physical());
		buffer.writeDouble(p.fire());
		buffer.writeDouble(p.ice());
		buffer.writeDouble(p.lightning());
		buffer.writeDouble(p.defense());
		buffer.writeDouble(p.attackSpeed());
		buffer.writeDouble(p.meleeRange());
		buffer.writeDouble(p.physicalAmp());
		buffer.writeDouble(p.physicalBase());
		buffer.writeDouble(p.artifactFlat());
		buffer.writeDouble(p.potionFlat());
		buffer.writeDouble(p.artifactPercent());
		buffer.writeDouble(p.potionPercent());
		buffer.writeDouble(p.attackBase());
		buffer.writeDouble(p.attackArtifactFlat());
		buffer.writeDouble(p.attackPotionFlat());
		buffer.writeDouble(p.attackArtifactPercent());
		buffer.writeDouble(p.attackPotionPercent());
		buffer.writeDouble(p.leaves());
		buffer.writeDouble(p.weaponDamage());
		buffer.writeDouble(p.specialAttack());
		buffer.writeDouble(p.lifesteal());
		buffer.writeDouble(p.normalAttackDamage());
		buffer.writeDouble(p.critChance());
		buffer.writeDouble(p.critDamage());
		buffer.writeDouble(p.ignoreDefense());
		buffer.writeDouble(p.moveSpeed());
		buffer.writeDouble(p.dodge());
		buffer.writeDouble(p.dodgeRate());
		buffer.writeDouble(p.negotiation());
		buffer.writeDouble(p.shopDiscount());
		buffer.writeDouble(p.leafGainPercent());
		buffer.writeDouble(p.xpDropPercent());
		buffer.writeDouble(p.maxMp());
		buffer.writeDouble(p.luck());
		buffer.writeDouble(p.mpAbsorb());
		buffer.writeDouble(p.magicBookHaste());
		buffer.writeDouble(p.magicBookMpReduction());
		buffer.writeDouble(p.magicBoltMpCostIncrease());
		buffer.writeDouble(p.mpReserved());
	}

	public static StatsSyncPayload read(FriendlyByteBuf buffer) {
		return new StatsSyncPayload(
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble());
	}

	public static StatsSyncPayload of(PlayerStats.Values values, double mpRegenTotal, double physicalTotal,
			double fireTotal, double iceTotal, double lightningTotal, double attackSpeedTotal, double physicalAmp,
			PlayerStats.PhysicalBreakdown breakdown, PlayerStats.PhysicalBreakdown attackBreakdown,
			double weaponDamageTotal, double specialAttackTotal, double normalAttackDamageTotal,
			double critChanceTotal, double critDamageTotal, double ignoreDefenseTotal, double moveSpeedTotal,
			double dodgeTotal, double dodgeRate, double meleeRangeTotal,
			double negotiationTotal, double shopDiscount, double leafGainTotal, double xpDropTotal, double maxMp,
			double luck, double defenseTotal, double mpAbsorb, double magicBookHaste,
			double magicBookMpReduction, double magicBoltMpCostIncrease, double mpReserved) {
		return new StatsSyncPayload(values.mp, mpRegenTotal, physicalTotal, fireTotal, iceTotal,
				lightningTotal, defenseTotal, attackSpeedTotal, meleeRangeTotal, physicalAmp,
				breakdown.base(), breakdown.artifactFlat(), breakdown.potionFlat(),
				breakdown.artifactPercent(), breakdown.potionPercent(), attackBreakdown.base(),
				attackBreakdown.artifactFlat(), attackBreakdown.potionFlat(),
				attackBreakdown.artifactPercent(), attackBreakdown.potionPercent(), values.leaves,
				weaponDamageTotal, specialAttackTotal, values.lifesteal, normalAttackDamageTotal,
				critChanceTotal, critDamageTotal, ignoreDefenseTotal, moveSpeedTotal, dodgeTotal, dodgeRate,
				negotiationTotal, shopDiscount, leafGainTotal, xpDropTotal, maxMp, luck, mpAbsorb,
				magicBookHaste, magicBookMpReduction, magicBoltMpCostIncrease, mpReserved);
	}
}
