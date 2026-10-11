package com.phantompowers.mixin;

import com.phantompowers.GhostFlags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

	/** Intangible things move straight through blocks. */
	@Inject(method = "move", at = @At("HEAD"))
	private void phantom$phaseMove(MovementType type, Vec3d movement, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (GhostFlags.isPhasing(self)) self.noClip = true;
	}

	/** Intangible things float instead of falling through the floor. */
	@Inject(method = "hasNoGravity", at = @At("HEAD"), cancellable = true)
	private void phantom$float(CallbackInfoReturnable<Boolean> cir) {
		if (GhostFlags.isPhasing((Entity) (Object) this)) cir.setReturnValue(true);
	}

	/** No suffocating inside walls while intangible. */
	@Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
	private void phantom$notInWall(CallbackInfoReturnable<Boolean> cir) {
		if (GhostFlags.isPhasing((Entity) (Object) this)) cir.setReturnValue(false);
	}
}
