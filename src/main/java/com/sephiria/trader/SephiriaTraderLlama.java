package com.sephiria.trader;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * 赛菲莉娅商队羊驼：披红毯的商队羊驼（原版商队羊驼披的是蓝毯）。
 *
 * <p>「红袍」不用自带贴图：原版 26.3 的羊驼毯子是**装备层**——红毯物品的装备组件指向
 * {@code minecraft:red_carpet} 那套原版装备素材（和原版给羊驼披地毯是同一个机制），
 * 所以出生时直接把红毯写进背上的装备槽就是红毯外观，仓库里不需要放改过的原版贴图。
 *
 * <p>写装备槽用的是 {@code setItemSlot}（不校验「这件能不能装」），因此不受
 * 「谁在什么时候给它穿上」的限制。
 */
public class SephiriaTraderLlama extends TraderLlama {
	public SephiriaTraderLlama(EntityType<? extends TraderLlama> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		this.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.CARPET.pick(DyeColor.RED)));
		return data;
	}
}
