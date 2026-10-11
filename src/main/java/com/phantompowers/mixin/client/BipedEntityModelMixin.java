package com.phantompowers.mixin.client;

import com.phantompowers.entity.GliderEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Riding the Hover Glider: stand on it in a surfer stance, knees bent, arms out for balance. */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin {
	@Shadow @Final public ModelPart head;
	@Shadow @Final public ModelPart body;
	@Shadow @Final public ModelPart rightArm;
	@Shadow @Final public ModelPart leftArm;
	@Shadow @Final public ModelPart rightLeg;
	@Shadow @Final public ModelPart leftLeg;

	@Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void phantom$gliderPose(LivingEntity entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch, CallbackInfo ci) {
		if (!(entity instanceof PlayerEntity) || !(entity.getVehicle() instanceof GliderEntity)) return;
		float sway = MathHelper.sin(age * 0.08f);
		float sway2 = MathHelper.cos(age * 0.11f);

		// Body twisted sideways like a surfer, leaning slightly into the wind
		float lean = 0.12f;
		float twist = -0.40f;
		body.pitch = lean;
		body.yaw = twist;
		float sin = MathHelper.sin(twist), cos = MathHelper.cos(twist);
		float hipZ = MathHelper.sin(lean) * 12f;

		// Shoulders and hips follow the twist so nothing comes apart
		rightArm.pivotX = -cos * 5f;
		rightArm.pivotZ = sin * 5f;
		leftArm.pivotX = cos * 5f;
		leftArm.pivotZ = -sin * 5f;
		rightArm.pivotY = 2f;
		leftArm.pivotY = 2f;
		rightLeg.pivotZ = sin * 1.9f + hipZ;
		leftLeg.pivotZ = -sin * 1.9f + hipZ;
		rightLeg.pivotY = 11.9f;
		leftLeg.pivotY = 11.9f;

		// Front foot forward, back foot back, knees bent
		rightLeg.pitch = -0.50f;
		rightLeg.yaw = twist + 0.15f;
		rightLeg.roll = 0.12f;
		leftLeg.pitch = 0.42f;
		leftLeg.yaw = twist - 0.15f;
		leftLeg.roll = -0.12f;

		// Arms out wide for balance, swaying slowly
		rightArm.pitch = -0.25f + sway * 0.08f;
		rightArm.yaw = twist;
		rightArm.roll = 1.15f + sway2 * 0.10f;
		leftArm.pitch = 0.30f - sway * 0.08f;
		leftArm.yaw = twist;
		leftArm.roll = -1.05f - sway2 * 0.10f;
	}
}
