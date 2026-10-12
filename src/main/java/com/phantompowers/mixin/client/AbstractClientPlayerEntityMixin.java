package com.phantompowers.mixin.client;

import com.phantompowers.client.BlasterScope;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom the camera in while aiming the Ghost Blaster down its scope. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
	@Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
	private void phantom$scopeZoom(CallbackInfoReturnable<Float> cir) {
		float m = BlasterScope.fovMultiplier();
		if (m < 1f) cir.setReturnValue(cir.getReturnValue() * m);
	}
}
