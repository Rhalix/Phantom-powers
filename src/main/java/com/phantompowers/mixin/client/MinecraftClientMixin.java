package com.phantompowers.mixin.client;

import com.phantompowers.client.SuitVision;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ghost Hunter Suit: ghosts glow with an outline, visible through walls. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
	private void phantom$infrared(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (SuitVision.sees(entity)) cir.setReturnValue(true);
	}
}
