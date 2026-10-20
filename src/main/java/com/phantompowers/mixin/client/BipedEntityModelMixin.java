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
		if (entity instanceof com.phantompowers.entity.GhostCloneEntity clone) {
			phantom$clonePose(clone.wailing(), age);
			return;
		}
		if (!(entity instanceof PlayerEntity)) return;
		if (entity.getVehicle() instanceof GliderEntity) phantom$gliderPose(age);
		if (entity.getVehicle() instanceof com.phantompowers.entity.GhostBikeEntity) phantom$bikePose(age);
		if (com.phantompowers.client.GuitarFeature.guitarOf(entity) != null) phantom$guitarPose(entity, age);
		float hero = com.phantompowers.client.ClientRings.transformProgress(entity.getId(), age - (int) age);
		if (hero >= 0f) {
			float release = com.phantompowers.client.ClientRings.heroRelease(entity.getId(), age - (int) age);
			phantom$heroPose(hero, release, age);
		}
		float wail = com.phantompowers.client.ClientRings.wailProgress(entity.getId(), age - (int) age);
		if (wail >= 0f) phantom$wailPose(wail, age);
		float lean = com.phantompowers.client.SupermanFlight.lean(entity, age - (int) age);
		if (lean > 0f) phantom$supermanPose(lean, age);
		float aim = com.phantompowers.client.WristGadgetFeature.aim(entity, age - (int) age);
		if (aim > 0f) phantom$aimPose(aim);
		float portal = com.phantompowers.client.ClientCutscene.portalTime(entity, age - (int) age);
		if (portal >= 0f) phantom$portalPose(portal, age);
		float dark = com.phantompowers.client.ClientCutscene.darkTime(entity, age - (int) age);
		if (dark >= 0f) phantom$darkPose(dark, age);
		float origin = com.phantompowers.client.ClientCutscene.originTime(entity, age - (int) age);
		if (origin >= 0f) phantom$originPose(origin, age);
		if (com.phantompowers.client.ClientCutscene.spinning(entity)) phantom$spinPose(age);
		float heroT = com.phantompowers.client.ClientCutscene.heroTime(entity, age - (int) age);
		if (heroT >= 0f && heroT < com.phantompowers.Cutscenes.HERO_CHANGE - 10) phantom$auraPose(heroT, age);
		float suitT = com.phantompowers.client.ClientCutscene.suitTime(entity, age - (int) age);
		if (suitT >= 0f) phantom$suitUpPose(suitT, age);
	}

	/**
	 * The hero's aura farm: feet set wide, fists clenched at the sides and head down while the energy builds...
	 * then the chin comes up, the fists draw back to the hips, ready (the rings and the sky punch take it from there).
	 */
	@org.spongepowered.asm.mixin.Unique
	private void phantom$auraPose(float t, float age) {
		float set = phantom$ease(t / 10f);
		float rise = phantom$ease((t - 28f) / 18f);
		float breathe = MathHelper.sin(age * 0.15f) * 0.03f;
		float tremble = MathHelper.sin(age * 2.5f) * 0.015f * rise;
		head.pitch = 0.5f * set * (1 - rise) - 0.12f * rise + breathe;
		head.yaw = 0f;
		hat.copyTransform(head);
		body.pitch = 0.08f * set * (1 - rise);
		rightLeg.roll = 0.2f * set;
		leftLeg.roll = -0.2f * set;
		rightLeg.pitch = leftLeg.pitch = 0f;
		// clenched fists held a little out from the sides, drawing back to the hips
		rightArm.pitch = MathHelper.lerp(rise, 0.05f, 0.35f) + tremble;
		rightArm.yaw = 0f;
		rightArm.roll = MathHelper.lerp(rise, 0.32f, 0.55f) + breathe;
		leftArm.pitch = MathHelper.lerp(rise, 0.05f, 0.35f) - tremble;
		leftArm.yaw = 0f;
		leftArm.roll = MathHelper.lerp(rise, -0.32f, -0.55f) - breathe;
	}

	/** Suiting up: arms out wide while the plates fly round you, then the fists clench and come down as it locks on. */
	@org.spongepowered.asm.mixin.Unique
	private void phantom$suitUpPose(float t, float age) {
		float plates = com.phantompowers.Cutscenes.SUIT_PLATES, done = com.phantompowers.Cutscenes.SUIT_DONE;
		float out = phantom$ease((t - 4f) / 10f) * (1 - phantom$ease((t - done + 6f) / 8f));
		float lock = phantom$ease((t - done + 6f) / 8f);
		head.pitch = -0.1f * out + 0.15f * lock;
		head.yaw = MathHelper.sin(t * 0.05f) * 0.2f * out;   // glancing at the plates as they come
		hat.copyTransform(head);
		rightArm.pitch = -0.1f * out - 0.5f * lock;
		rightArm.roll = 1.25f * out + 0.15f * lock;
		leftArm.pitch = -0.1f * out - 0.5f * lock;
		leftArm.roll = -1.25f * out - 0.15f * lock;
		rightArm.yaw = leftArm.yaw = 0f;
		rightLeg.roll = 0.1f * (out + lock);
		leftLeg.roll = -0.1f * (out + lock);
	}

	@org.spongepowered.asm.mixin.Unique
	private static float phantom$ease(float v) {
		v = MathHelper.clamp(v, 0f, 1f);
		return v * v * (3 - 2 * v);
	}

	/**
	 * The evil transformation: arms folded and head bowed while the aura builds... then the head comes up slowly,
	 * the arms open wide, palms up, as the power pours in, and at the change everything is flung outward.
	 */
	@org.spongepowered.asm.mixin.Unique
	private void phantom$darkPose(float t, float age) {
		float change = com.phantompowers.Cutscenes.DARK_CHANGE;
		float open = phantom$ease((t - 30f) / 35f);
		float fling = phantom$ease((t - (change - 3)) / 4f);
		float bow = 1f - phantom$ease((t - 20f) / 30f);
		float breathe = MathHelper.sin(age * 0.12f) * 0.03f;
		head.pitch = 0.55f * bow - 0.25f * fling + breathe;
		head.yaw = 0f;
		hat.copyTransform(head);
		body.pitch = -0.1f * fling;
		// folded arms -> open, palms up -> flung out wide
		rightArm.pitch = MathHelper.lerp(open, -1.15f, -0.35f) + breathe;
		rightArm.yaw = MathHelper.lerp(open, 0.85f, 0f);
		rightArm.roll = MathHelper.lerp(open, -0.1f, 0.85f) + 0.5f * fling;
		leftArm.pitch = MathHelper.lerp(open, -1.15f, -0.35f) + breathe;
		leftArm.yaw = MathHelper.lerp(open, -0.85f, 0f);
		leftArm.roll = MathHelper.lerp(open, 0.1f, -0.85f) - 0.5f * fling;
		rightLeg.roll = 0.12f * open;
		leftLeg.roll = -0.12f * open;
		rightLeg.pitch = leftLeg.pitch = 0f;
	}

	/** The portal surge: twisting and shaking as the energy rips through you, then thrown out, dazed. */
	@org.spongepowered.asm.mixin.Unique
	private void phantom$originPose(float t, float age) {
		float blast = com.phantompowers.Cutscenes.ORIGIN_BLAST;
		if (t < 18f) {
			head.pitch = -0.2f;
			head.yaw = MathHelper.sin(t * 0.3f) * 0.4f;   // looking around, confused
		} else if (t < blast) {
			float k = phantom$ease((t - 18f) / 6f);
			float shake = MathHelper.sin(age * 3.1f) * 0.12f * k;
			body.pitch = -0.25f * k;
			head.pitch = -0.7f * k + shake;
			head.yaw = shake;
			rightArm.pitch = -0.4f * k + shake;
			rightArm.roll = 1.3f * k;
			leftArm.pitch = -0.4f * k - shake;
			leftArm.roll = -1.3f * k;
			rightLeg.roll = 0.2f * k;
			leftLeg.roll = -0.2f * k;
		} else {
			float k = 1f - phantom$ease((t - blast - 10f) / 25f);
			body.pitch = 0.25f * k;
			head.pitch = 0.3f * k;
			head.roll = MathHelper.sin(age * 0.2f) * 0.15f * k;
			rightArm.pitch = -0.6f * k;
			leftArm.pitch = 0.3f * k;
		}
		hat.copyTransform(head);
	}

	/** Flying round in circles: leaning hard into the turn, arms swept back. */
	@org.spongepowered.asm.mixin.Unique
	private void phantom$spinPose(float age) {
		body.roll = 0f;
		head.pitch = -0.4f;
		hat.copyTransform(head);
		rightArm.pitch = 0.5f;
		rightArm.roll = 0.6f;
		leftArm.pitch = 0.5f;
		leftArm.roll = -0.6f;
		rightLeg.pitch = 0.2f + MathHelper.sin(age * 0.6f) * 0.08f;
		leftLeg.pitch = 0.25f - MathHelper.sin(age * 0.6f) * 0.08f;
	}

	/** Aiming the wrist launcher: right arm held straight out where you look, left hand steadying the wrist. */
	@org.spongepowered.asm.mixin.Unique
	private void phantom$aimPose(float a) {
		rightArm.pitch = MathHelper.lerp(a, rightArm.pitch, head.pitch - MathHelper.HALF_PI);
		rightArm.yaw = MathHelper.lerp(a, rightArm.yaw, head.yaw - 0.08f);
		rightArm.roll = MathHelper.lerp(a, rightArm.roll, 0f);
		leftArm.pitch = MathHelper.lerp(a, leftArm.pitch, head.pitch - 1.3f);
		leftArm.yaw = MathHelper.lerp(a, leftArm.yaw, head.yaw + 0.6f);
		leftArm.roll = MathHelper.lerp(a, leftArm.roll, 0f);
	}

	/**
	 * The prototype portal: lean in curiously and poke the switch... then get blasted, arms flung up over your face,
	 * thrown back, and finally swaying dizzily.
	 */
	@org.spongepowered.asm.mixin.Unique
	private void phantom$portalPose(float t, float age) {
		float blast = com.phantompowers.DarkPowers.PORTAL_BLAST;
		if (t < blast) {
			float lean = MathHelper.clamp(t / 12f, 0f, 1f);
			float poke = MathHelper.clamp((t - 14f) / 6f, 0f, 1f) * (1f - MathHelper.clamp((t - 30f) / 6f, 0f, 1f));
			body.pitch = 0.18f * lean;
			head.pitch = 0.25f * lean;
			head.yaw = MathHelper.sin(t * 0.15f) * 0.15f * (1 - poke);
			rightArm.pitch = -0.4f * lean - 1.1f * poke;
			rightArm.yaw = -0.15f * poke;
			leftArm.pitch = -0.2f * lean;
			// scratching your head while it hums
			float scratch = MathHelper.clamp((t - 34f) / 4f, 0f, 1f);
			leftArm.pitch = MathHelper.lerp(scratch, leftArm.pitch, -2.6f + MathHelper.sin(age * 1.6f) * 0.08f);
			leftArm.roll = MathHelper.lerp(scratch, leftArm.roll, -0.5f);
		} else {
			float k = t - blast;
			float in = MathHelper.clamp(k / 3f, 0f, 1f);
			in = in * in * (3 - 2 * in);
			float hit = MathHelper.clamp(1f - (k - 14f) / 14f, 0f, 1f) * in;
			float shake = MathHelper.sin(age * 2.9f) * 0.08f * hit;
			body.pitch = -0.35f * hit;
			head.pitch = -0.6f * hit + shake;
			head.yaw = shake;
			rightArm.pitch = MathHelper.lerp(hit, rightArm.pitch, -2.4f + shake);
			rightArm.roll = MathHelper.lerp(hit, rightArm.roll, -0.7f);
			leftArm.pitch = MathHelper.lerp(hit, leftArm.pitch, -2.4f - shake);
			leftArm.roll = MathHelper.lerp(hit, leftArm.roll, 0.7f);
			rightLeg.pitch = 0.35f * hit;
			leftLeg.pitch = -0.2f * hit;
			// dizzy wobble afterwards
			float dizzy = 1f - hit;
			body.roll = MathHelper.sin(age * 0.2f) * 0.08f * dizzy;
			head.roll = MathHelper.sin(age * 0.2f + 0.6f) * 0.15f * dizzy;
		}
		hat.copyTransform(head);
	}

	/** A ghost clone: floats with its legs hanging; when it wails, head thrust forward and arms flung back. */
	@org.spongepowered.asm.mixin.Unique
	private void phantom$clonePose(float wail, float age) {
		float sway = MathHelper.sin(age * 0.08f) * 0.1f;
		rightLeg.pitch = 0.15f + sway;
		leftLeg.pitch = -0.1f - sway;
		rightLeg.yaw = leftLeg.yaw = 0;
		rightArm.pitch = MathHelper.lerp(wail, sway * 0.5f, 0.5f);
		leftArm.pitch = MathHelper.lerp(wail, -sway * 0.5f, 0.5f);
		rightArm.roll = MathHelper.lerp(wail, 0.1f, 1.1f);
		leftArm.roll = MathHelper.lerp(wail, -0.1f, -1.1f);
		head.pitch = MathHelper.lerp(wail, head.pitch, 0.15f);
		hat.copyTransform(head);
	}

	/** Riding Rev's bike: tucked forward over the tank, hands on the bars, knees bent to the pegs. */
	@org.spongepowered.asm.mixin.Unique
	private void phantom$bikePose(float age) {
		float lean = 0.55f;
		body.pitch = lean;
		// the hips swing back as the torso leans forward
		float hipY = 12f * MathHelper.cos(lean), hipZ = 12f * MathHelper.sin(lean);
		rightLeg.pivotY = hipY;
		leftLeg.pivotY = hipY;
		rightLeg.pivotZ = hipZ;
		leftLeg.pivotZ = hipZ;
		rightLeg.pitch = -1.15f;
		leftLeg.pitch = -1.15f;
		rightLeg.yaw = 0.18f;
		leftLeg.yaw = -0.18f;
		rightLeg.roll = 0;
		leftLeg.roll = 0;
		float shoulderY = 2f * MathHelper.cos(lean), shoulderZ = 2f * MathHelper.sin(lean);
		rightArm.pivotY = shoulderY;
		leftArm.pivotY = shoulderY;
		rightArm.pivotZ = shoulderZ;
		leftArm.pivotZ = shoulderZ;
		float bump = MathHelper.sin(age * 1.3f) * 0.02f;
		rightArm.pitch = -1.25f + bump;
		leftArm.pitch = -1.25f + bump;
		rightArm.yaw = -0.1f;
		leftArm.yaw = 0.1f;
		rightArm.roll = 0;
		leftArm.roll = 0;
		head.pitch -= 0.35f;
		hat.copyTransform(head);
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
	private void phantom$heroPose(float t, float release, float age) {
		// Remember the normal pose so we can blend back into it smoothly afterwards
		float[] normal = {body.pitch, head.pitch, head.yaw, rightLeg.roll, leftLeg.roll, rightLeg.pitch, leftLeg.pitch, rightLeg.yaw, leftLeg.yaw,
				rightArm.pitch, rightArm.yaw, rightArm.roll, leftArm.pitch, leftArm.yaw, leftArm.roll};
		float rise = MathHelper.clamp(t / 0.55f, 0f, 1f);
		float punchRaw = MathHelper.clamp((t - 0.55f) / 0.3f, 0f, 1f);   // a smooth punch, not a snap
		float punch = punchRaw * punchRaw * (3 - 2 * punchRaw);
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
		// After the rings finish, the fist comes down and the stance relaxes gently
		if (release > 0f) {
			float w = release * release * (3 - 2 * release);
			body.pitch = MathHelper.lerp(w, body.pitch, normal[0]);
			head.pitch = MathHelper.lerp(w, head.pitch, normal[1]);
			head.yaw = MathHelper.lerp(w, head.yaw, normal[2]);
			hat.copyTransform(head);
			rightLeg.roll = MathHelper.lerp(w, rightLeg.roll, normal[3]);
			leftLeg.roll = MathHelper.lerp(w, leftLeg.roll, normal[4]);
			rightLeg.pitch = MathHelper.lerp(w, rightLeg.pitch, normal[5]);
			leftLeg.pitch = MathHelper.lerp(w, leftLeg.pitch, normal[6]);
			rightLeg.yaw = MathHelper.lerp(w, rightLeg.yaw, normal[7]);
			leftLeg.yaw = MathHelper.lerp(w, leftLeg.yaw, normal[8]);
			rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, normal[9]);
			rightArm.yaw = MathHelper.lerp(w, rightArm.yaw, normal[10]);
			rightArm.roll = MathHelper.lerp(w, rightArm.roll, normal[11]);
			leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, normal[12]);
			leftArm.yaw = MathHelper.lerp(w, leftArm.yaw, normal[13]);
			leftArm.roll = MathHelper.lerp(w, leftArm.roll, normal[14]);
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
