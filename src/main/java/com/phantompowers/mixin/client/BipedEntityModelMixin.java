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
		float hero = com.phantompowers.client.ClientRings.transformProgress(entity.getId(), age - (int) age);
		if (hero >= 0f) phantom$heroPose(hero, age);
		float wail = com.phantompowers.client.ClientRings.wailProgress(entity.getId(), age - (int) age);
		if (wail >= 0f) phantom$wailPose(wail, age);
		float lean = com.phantompowers.client.SupermanFlight.lean(entity, age - (int) age);
		if (lean > 0f) phantom$supermanPose(lean, age);
	}

	/** Superman flight: head up looking ahead, right fist punched forward, left arm along the side, legs together. */
	@org.spongepowered.asm.mixin.Unique
	private void phantom$supermanPose(float l, float age) {
		float flutter = MathHelper.sin(age * 0.5f) * 0.03f;
		head.pitch = MathHelper.lerp(l, head.pitch, -1.15f);
		hat.copyTransform(head);
		rightArm.pitch = MathHelper.lerp(l, rightArm.pitch, -3.0f + flutter);
		rightArm.yaw = MathHelper.lerp(l, rightArm.yaw, 0f);
		rightArm.roll = MathHelper.lerp(l, rightArm.roll, 0.08f);
		leftArm.pitch = MathHelper.lerp(l, leftArm.pitch, 0.15f - flutter);
		leftArm.yaw = MathHelper.lerp(l, leftArm.yaw, 0f);
		leftArm.roll = MathHelper.lerp(l, leftArm.roll, -0.06f);
		rightLeg.pitch = MathHelper.lerp(l, rightLeg.pitch, 0.05f + flutter);
		leftLeg.pitch = MathHelper.lerp(l, leftLeg.pitch, 0.12f - flutter);
		rightLeg.roll = MathHelper.lerp(l, rightLeg.roll, 0.02f);
		leftLeg.roll = MathHelper.lerp(l, leftLeg.roll, -0.02f);
	}

	/** The wail finale: head thrown back screaming, arms flung wide and back, the whole body shaking. */
	@org.spongepowered.asm.mixin.Unique
	private void phantom$wailPose(float t, float age) {
		float in = MathHelper.clamp(t / 0.15f, 0f, 1f);
		float shake = MathHelper.sin(age * 2.7f) * 0.04f * in;
		body.pitch = -0.18f * in;
		head.pitch = -0.55f * in + shake;
		head.yaw = shake;
		hat.copyTransform(head);
		rightArm.pitch = 0.5f * in + shake;
		rightArm.yaw = 0f;
		rightArm.roll = 1.25f * in;
		leftArm.pitch = 0.5f * in - shake;
		leftArm.yaw = 0f;
		leftArm.roll = -1.25f * in;
		rightLeg.roll = 0.18f * in;
		leftLeg.roll = -0.18f * in;
		rightLeg.pitch = 0.1f * in;
		leftLeg.pitch = 0.1f * in;
	}

	/**
	 * Going ghost: feet planted wide, chest out. Arms sweep up as the rings pass,
	 * then one fist punches to the sky while the other plants on the hip.
	 */
	@org.spongepowered.asm.mixin.Unique
	private void phantom$heroPose(float t, float age) {
		float rise = MathHelper.clamp(t / 0.6f, 0f, 1f);          // 0..1 over the first 60%
		float punch = MathHelper.clamp((t - 0.6f) / 0.15f, 0f, 1f); // snaps up near the end
		float ease = rise * rise * (3 - 2 * rise);
		body.pitch = -0.08f * ease;
		head.pitch = -0.25f * ease - 0.2f * punch;
		head.yaw = 0f;
		hat.copyTransform(head);
		// Wide heroic stance
		rightLeg.roll = 0.22f * ease;
		leftLeg.roll = -0.22f * ease;
		rightLeg.pitch = 0f;
		leftLeg.pitch = 0f;
		rightLeg.yaw = 0.15f * ease;
		leftLeg.yaw = -0.15f * ease;
		// Arms: crossed in front, then thrown out wide...
		float cross = 1f - ease;
		rightArm.pitch = -1.2f * cross - 0.2f * ease;
		rightArm.yaw = -0.6f * cross;
		rightArm.roll = 0.9f * ease;
		leftArm.pitch = -1.2f * cross - 0.2f * ease;
		leftArm.yaw = 0.6f * cross;
		leftArm.roll = -0.9f * ease;
		// ...then the right fist punches up to the sky, left hand on the hip
		if (punch > 0f) {
			rightArm.pitch = MathHelper.lerp(punch, rightArm.pitch, -2.9f);
			rightArm.roll = MathHelper.lerp(punch, rightArm.roll, -0.25f);
			rightArm.yaw = 0f;
			leftArm.pitch = MathHelper.lerp(punch, leftArm.pitch, 0.25f);
			leftArm.roll = MathHelper.lerp(punch, leftArm.roll, -0.75f);
			leftArm.yaw = MathHelper.lerp(punch, leftArm.yaw, 0.4f);
		}
	}

	@org.spongepowered.asm.mixin.Unique
	private void phantom$guitarPose(LivingEntity entity, float age) {
		if (entity.handSwinging) phantom$played.put(entity, entity.age);
		Integer last = phantom$played.get(entity);
		float play = last == null ? 0f : (entity.age - last < 50 ? 1f : Math.max(0f, 1f - (entity.age - last - 50) / 10f));
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
