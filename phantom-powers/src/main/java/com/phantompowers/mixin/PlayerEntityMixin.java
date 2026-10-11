package com.phantompowers.mixin;

import com.phantompowers.GhostFlags;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

	/**
	 * Vanilla resets noClip at the start of every player tick (it's only for spectators).
	 * Turn it back on for intangible ghosts before movement runs, so the game doesn't push them out of walls.
	 */
	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;tick()V"))
	private void phantom$keepNoClip(CallbackInfo ci) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (GhostFlags.isIntangible(self)) self.noClip = true;
	}
}
