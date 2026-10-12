package com.phantompowers.mixin.client;

import com.phantompowers.client.SuitVision;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

	/**
	 * Invisible ghosts are completely hidden from other players: no body, armor, held items or name.
	 * A Ghost Hunter Suit still picks them up as an infrared outline.
	 */
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private <E extends Entity> void phantom$hideGhost(E entity, Frustum frustum, double x, double y, double z,
			CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof PlayerEntity p && !p.isMainPlayer() && SuitVision.isInvisibleGhost(p)) {
			if (SuitVision.sees(p)) {
				cir.setReturnValue(true);
				return;
			}
			cir.setReturnValue(false);
		}
		// The suit sees ghosts through walls, even out of view of the normal culling
		if (SuitVision.sees(entity)) cir.setReturnValue(true);
	}
}
