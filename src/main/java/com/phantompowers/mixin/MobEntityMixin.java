package com.phantompowers.mixin;

import com.phantompowers.GhostFlags;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {

	/** Hostile mobs can't target a ghost who is hiding inside a possessed body, or the body itself. */
	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void phantom$ignorePossessed(LivingEntity target, CallbackInfo ci) {
		if (target == null || !((Object) this instanceof Monster)) return;
		if (target.getWorld().isClient) return;
		if (GhostFlags.POSSESSORS.isEmpty() && GhostFlags.HOSTS.isEmpty()) return;
		if (GhostFlags.POSSESSORS.contains(target.getUuid()) || GhostFlags.HOSTS.contains(target.getUuid())) ci.cancel();
	}
}
