package com.phantompowers.mixin;

import com.phantompowers.world.TownRepair;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldMixin {
	/** Anything that smashes a block (the Ghostly Wail, ghosts, mobs...) in Amity Park: it grows back. */
	@Inject(method = "breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;I)Z", at = @At("HEAD"))
	private void phantom$rememberTown(BlockPos pos, boolean drop, Entity breaker, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
		World self = (World) (Object) this;
		if (!self.isClient) TownRepair.record(self, pos);
	}

	/** ...and town blocks that grow back don't drop anything (no farming the town). */
	@ModifyVariable(method = "breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;I)Z", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private boolean phantom$noTownDrops(boolean drop, BlockPos pos) {
		World self = (World) (Object) this;
		return drop && (self.isClient || !TownRepair.isTownBlock(self, pos));
	}
}
