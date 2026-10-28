package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：属性页需要的关键数值（1.20.1 旧 channel 版）。
 *
 * <p>只带界面要显示的 19 项，没带的字段在 ClientStats 里保持默认值；这样包体小、也不用
 * 维护 38 个字段的双端镜像。生命值不在包里（原版数据客户端本来就有）。
 */
public record StatsSyncPayload(double mp, double maxMp, double mpRegen, double physical, double fire,
		double ice, double lightning, double defense, double attackSpeed, double meleeRange,
		double physicalAmp, double weaponDamage, double specialAttack, double normalAttackDamage,
		double luck, double moveSpeed, double critChance, double critDamage, double leaves) {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "stats_sync");

	public static void write(FriendlyByteBuf buffer, StatsSyncPayload payload) {
		double[] values = {payload.mp, payload.maxMp, payload.mpRegen, payload.physical, payload.fire,
				payload.ice, payload.lightning, payload.defense, payload.attackSpeed, payload.meleeRange,
				payload.physicalAmp, payload.weaponDamage, payload.specialAttack, payload.normalAttackDamage,
				payload.luck, payload.moveSpeed, payload.critChance, payload.critDamage, payload.leaves};

		for (double value : values) {
			buffer.writeDouble(value);
		}
	}

	public static StatsSyncPayload read(FriendlyByteBuf buffer) {
		return new StatsSyncPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
	}
}
