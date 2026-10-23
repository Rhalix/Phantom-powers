package com.phantompowers.mixin;

import com.phantompowers.world.TownRepair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
	@Shadow @Final private World world;

	@Shadow public abstract List<BlockPos> getAffectedBlocks();

	/** Amity Park grows back after explosions too. */
	@Inject(method = "affectWorld", at = @At("HEAD"))
	private void phantom$rememberTown(boolean particles, CallbackInfo ci) {
		if (world.isClient) return;
		for (BlockPos pos : getAffectedBlocks()) TownRepair.record(world, pos);
	}
}
