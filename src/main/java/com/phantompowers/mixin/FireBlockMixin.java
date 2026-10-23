package com.phantompowers.mixin;

import com.phantompowers.world.TownRepair;
import net.minecraft.block.FireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
	/** Burnt-down bits of Amity Park grow back. */
	@Inject(method = "trySpreadingFire", at = @At("HEAD"))
	private void phantom$rememberTown(World world, BlockPos pos, int spreadFactor, Random random, int currentAge, CallbackInfo ci) {
		if (!world.isClient && !world.getBlockState(pos).isAir()) TownRepair.record(world, pos);
	}
}
