package com.sephiria.mixin;

import com.sephiria.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 天国（特殊石板）的唯一获取途径：烧炼远古残骸时，<b>每一份</b>烧成的瞬间有 0.01% 的概率
 * 从熔炉里弹出一块，并伴随一声雷霆。
 *
 * <p>挂在 {@code serverTick} 里调用 {@code burn} 之前：那一刻输入槽还是远古残骸、结果已经
 * 组装好并通过了「输出槽放得下」的检查，正好是「这一份快要烧成」的时点。熔炉 / 高炉都走同一个
 * 基类，所以两者都会触发（烟熏炉没有远古残骸配方，自然不会命中）。
 *
 * <p>天国不进任何掉落池，创造页那一件只用于测试；正常游戏只能靠这里碰运气。
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceHeavenSlateMixin {
	/** 用户 2026-10-21 定的固定概率：0.01%（不是每炉，而是每一份烧成的物品）。 */
	@Unique
	private static final double HEAVEN_CHANCE = 0.0001D;

	@Inject(method = "serverTick",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;burn(Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
					shift = At.Shift.BEFORE))
	private static void sephiria$heavenSlate(ServerLevel level, BlockPos pos, BlockState state,
			AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
		if (!furnace.getItem(0).is(Items.ANCIENT_DEBRIS)) {
			return;
		}

		if (level.getRandom().nextDouble() >= HEAVEN_CHANCE) {
			return;
		}

		Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
				new ItemStack(ModItems.SLATE_OF_HEAVEN));
		// 雷霆从炉子那里响起；null = 附近所有玩家都听得到
		level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
	}
}
