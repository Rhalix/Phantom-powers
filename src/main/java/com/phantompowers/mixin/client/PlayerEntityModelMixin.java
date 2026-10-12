package com.phantompowers.mixin.client;

import com.phantompowers.client.GhostTail;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Flying ghosts swap their legs for a ghost tail. */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin {
	@Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void phantom$hideLegs(LivingEntity entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch, CallbackInfo ci) {
		if (!GhostTail.isTailed(entity)) return;
		PlayerEntityModel<?> self = (PlayerEntityModel<?>) (Object) this;
		self.rightLeg.visible = false;
		self.leftLeg.visible = false;
		self.rightPants.visible = false;
		self.leftPants.visible = false;
	}
}
