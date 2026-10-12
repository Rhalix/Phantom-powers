package com.phantompowers.mixin.client;

import com.phantompowers.client.ClientCutscene;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cutscene camera: a smooth, slowly orbiting shot of you instead of the default front view. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setPos(double x, double y, double z);

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Inject(method = "update", at = @At("TAIL"))
	private void phantom$cutsceneCamera(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
			float tickDelta, CallbackInfo ci) {
		if (!thirdPerson) return;
		float[] shot = ClientCutscene.cameraShot(focusedEntity, tickDelta);
		if (shot == null) return;
		setPos(shot[0], shot[1], shot[2]);
		setRotation(shot[3], shot[4]);
	}
}
