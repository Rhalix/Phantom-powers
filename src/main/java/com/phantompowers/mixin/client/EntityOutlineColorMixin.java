package com.phantompowers.mixin.client;

import com.phantompowers.client.SuitVision;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The suit's ghost outline is infrared orange-red instead of white. */
@Mixin(Entity.class)
public abstract class EntityOutlineColorMixin {
	@Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
	private void phantom$infraredColor(CallbackInfoReturnable<Integer> cir) {
		Entity self = (Entity) (Object) this;
		if (self.getWorld() != null && self.getWorld().isClient && SuitVision.sees(self)) cir.setReturnValue(SuitVision.INFRARED);
	}
}
