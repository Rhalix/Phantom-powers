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

/**
 * Riding the Hover Glider: stand on it in a surfer stance, knees bent, arms out for balance.
 * Holding the Thunder Guitar: hold it like a real guitar (right hand strumming, left hand on the neck)
 * and rock out when you play it.
 */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin {
	@Shadow @Final public ModelPart head;
	@Shadow @Final public ModelPart hat;
	@org.spongepowered.asm.mixin.Unique
	private static final java.util.Map<LivingEntity, Integer> phantom$played = new java.util.WeakHashMap<>();
	@Shadow @Final public ModelPart body;
	@Shadow @Final public ModelPart rightArm;
	@Shadow @Final public ModelPart leftArm;
	@Shadow @Final public ModelPart rightLeg;
	@Shadow @Final public ModelPart leftLeg;

	@Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void phantom$poses(LivingEntity entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch, CallbackInfo ci) {
		if (!(entity instanceof PlayerEntity)) return;
		if (entity.getVehicle() instanceof GliderEntity) phantom$gliderPose(age);
		if (com.phantompowers.client.GuitarFeature.guitarOf(entity) != null) phantom$guitarPose(entity, age);
	}

	@org.spongepowered.asm.mixin.Unique
	private void phantom$guitarPose(LivingEntity entity, float age) {
		if (entity.handSwinging) phantom$played.put(entity, entity.age);
		Integer last = phantom$played.get(entity);
		float play = last == null ? 0f : Math.max(0f, 1f - (entity.age - last) / 26f);
		// Strumming hand: across the body to the strings
		rightArm.pitch = -0.64f + play * 0.34f * MathHelper.sin(age * 1.8f);
		rightArm.yaw = 0f;
		rightArm.roll = -0.5f + play * 0.06f * MathHelper.cos(age * 1.8f);
		// Fretting hand: out to the left, holding the neck, sliding along it while playing
		leftArm.pitch = -0.85f + play * 0.05f * MathHelper.sin(age * 0.5f);
		leftArm.yaw = 0f;
		leftArm.roll = -0.91f + play * 0.07f * MathHelper.sin(age * 0.35f);
		// Headbang to the beat
		if (play > 0f) {
			head.pitch += play * 0.22f * Math.abs(MathHelper.sin(age * 0.45f));
			hat.copyTransform(head);
		}
	}

	@org.spongepowered.asm.mixin.Unique
	private void phantom$gliderPose(float age) {
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
