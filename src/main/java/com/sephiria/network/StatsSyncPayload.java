package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 服务端 → 客户端：属性页完整数值（1.20.1 旧 channel 版）。 */
public record StatsSyncPayload(double mp, double mpRegen, double physical, double fire, double ice, double lightning, double defense, double attackSpeed, double meleeRange, double physicalAmp, double physicalBase, double artifactFlat, double potionFlat, double artifactPercent, double potionPercent, double attackBase, double attackArtifactFlat, double attackPotionFlat, double attackArtifactPercent, double attackPotionPercent, double leaves, double weaponDamage, double specialAttack, double lifesteal, double normalAttackDamage, double critChance, double critDamage, double ignoreDefense, double moveSpeed, double dodge, double dodgeRate, double negotiation, double shopDiscount, double leafGainPercent, double xpDropPercent, double maxMp, double luck, double mpAbsorb) {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "stats_sync");

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
	}

	public static StatsSyncPayload read(FriendlyByteBuf buffer) {
		return new StatsSyncPayload(
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble());
	}
}
