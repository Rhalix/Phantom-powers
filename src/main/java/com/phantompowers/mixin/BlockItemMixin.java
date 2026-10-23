package com.phantompowers.mixin;

import com.phantompowers.world.TownRepair;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	/** Blocks players put down in Amity Park are theirs (they don't grow back when broken). */
	@Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;Lnet/minecraft/block/BlockState;)Z", at = @At("RETURN"))
	private void phantom$placedInTown(ItemPlacementContext ctx, BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && !ctx.getWorld().isClient) TownRepair.onPlaced(ctx.getWorld(), ctx.getBlockPos());
	}
}
