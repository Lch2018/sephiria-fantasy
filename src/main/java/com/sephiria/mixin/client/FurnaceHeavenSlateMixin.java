package com.sephiria.mixin.client;

import com.sephiria.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
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

/** 天国石板：烧炼远古残骸每一份烧成时有 0.01% 概率弹出（1.20.1 版）。 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceHeavenSlateMixin {
	@Unique
	private static final double HEAVEN_CHANCE = 0.0001D;

	@Inject(method = "serverTick",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;burn(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/crafting/Recipe;Lnet/minecraft/core/NonNullList;I)Z",
					shift = At.Shift.BEFORE))
	private static void sephiria$heavenSlate(Level level, BlockPos pos, BlockState state,
			AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
		if (!furnace.getItem(0).is(Items.ANCIENT_DEBRIS)) {
			return;
		}

		if (level.getRandom().nextDouble() >= HEAVEN_CHANCE) {
			return;
		}

		Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
				new ItemStack(ModItems.SLATE_OF_HEAVEN));
		level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
	}
}
