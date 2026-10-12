package com.phantompowers.mixin.client;

import com.phantompowers.client.SupermanFlight;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tilt flying ghosts to lie along their flight path, pivoting around the middle of the body. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerSupermanMixin {
	@Inject(method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V",
			at = @At("TAIL"))
	private void phantom$superman(AbstractClientPlayerEntity player, MatrixStack matrices, float animationProgress, float bodyYaw,
			float tickDelta, float scale, CallbackInfo ci) {
		float lean = SupermanFlight.lean(player, tickDelta);
		if (lean <= 0f) return;
		float pitch = player.getPitch(tickDelta);
		matrices.translate(0f, 0.9f, 0f);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lean * (-90f - pitch)));
		matrices.translate(0f, -0.9f, 0f);
	}
}
