package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.RoxyEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Roxy: hovering with her legs swaying, blowing kisses, throwing her head back to shriek, or sitting on the back of the bike. */
@SuppressWarnings("unused")
public class RoxyModel extends SinglePartEntityModel<RoxyEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("roxy"), "main");
	private final ModelPart root;
	private final ModelPart body, head, rightArm, leftArm, rightLeg, leftLeg;

	public RoxyModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-2.7f, 5f, -1.5f, 5.4f, 3f, 3f)
				.uv(22, 2).cuboid(-3.9f, 0f, -2f, 7.8f, 5.2f, 4f)
				.uv(49, 2).cuboid(-4f, 8f, -2.2f, 8f, 4.2f, 4.4f)
				.uv(77, 2).cuboid(-3.4f, 7.5f, -1.9f, 6.8f, 0.65f, 3.8f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_scarf = p_body.addChild("scarf", ModelPartBuilder.create()
				.uv(102, 2).cuboid(-2.2f, 0f, -2.2f, 4.4f, 1.1f, 4.4f)
				.uv(123, 2).cuboid(-3.9f, 1f, -2.25f, 7.8f, 1.4f, 0.5f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_scarf_tail = p_scarf.addChild("scarf_tail", ModelPartBuilder.create()
				.uv(143, 2).cuboid(-4f, 0f, -0.2f, 4f, 1.6f, 0.5f),
				ModelTransform.of(-3.4f, 1f, -1.8f, 0f, 0.3f, -0.18f));
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(155, 2).cuboid(-3.5f, -8f, -3f, 7f, 8f, 6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_hair = p_head.addChild("hair", ModelPartBuilder.create()
				.uv(184, 2).cuboid(-4f, -9f, -3.6f, 8f, 2f, 7.2f)
				.uv(218, 2).cuboid(-4f, -7f, 2.2f, 8f, 9f, 1.5f)
				.uv(240, 2).cuboid(-4.3f, -7f, -2.6f, 1.6f, 6.5f, 2.3f)
				.uv(2, 19).cuboid(-4.3f, -7f, -0.5f, 1.6f, 8f, 2.3f)
				.uv(13, 19).cuboid(-4.3f, -7f, 1.7f, 1.6f, 9f, 2.3f)
				.uv(24, 19).cuboid(2.8f, -7f, -2.6f, 1.6f, 6.5f, 2.3f)
				.uv(35, 19).cuboid(2.8f, -7f, -0.5f, 1.6f, 8f, 2.3f)
				.uv(46, 19).cuboid(2.8f, -7f, 1.7f, 1.6f, 9f, 2.3f)
				.uv(57, 19).cuboid(-3.8f, -8f, -3.8f, 1.9f, 3f, 1.1f)
				.uv(66, 19).cuboid(-2f, -8f, -3.8f, 1.9f, 2f, 1.1f)
				.uv(75, 19).cuboid(-0.2f, -8f, -3.8f, 1.9f, 2.5f, 1.1f)
				.uv(84, 19).cuboid(1.6f, -8f, -3.8f, 1.9f, 1.8f, 1.1f)
				.uv(93, 19).cuboid(-4.7f, -7.8f, -1f, 1.4f, 2f, 3f)
				.uv(105, 19).cuboid(3.3f, -7.8f, -1f, 1.4f, 2f, 3f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(117, 19).cuboid(-1.7f, -1f, -1.7f, 3f, 5.5f, 3.4f)
				.uv(133, 19).cuboid(-1.5f, 4.5f, -1.4f, 2.6f, 4.5f, 2.8f)
				.uv(147, 19).cuboid(-1.6f, 5f, -1.5f, 2.8f, 0.7f, 3f)
				.uv(162, 19).cuboid(-1.6f, 7.8f, -1.5f, 2.8f, 0.7f, 3f)
				.uv(177, 19).cuboid(-1.6f, 9f, -1.5f, 2.8f, 2.3f, 3f),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(192, 19).cuboid(-1.3f, -1f, -1.7f, 3f, 5.5f, 3.4f)
				.uv(208, 19).cuboid(-1.1f, 4.5f, -1.4f, 2.6f, 4.5f, 2.8f)
				.uv(222, 19).cuboid(-1.2f, 5f, -1.5f, 2.8f, 0.7f, 3f)
				.uv(237, 19).cuboid(-1.2f, 7.8f, -1.5f, 2.8f, 0.7f, 3f)
				.uv(2, 34).cuboid(-1.2f, 9f, -1.5f, 2.8f, 2.3f, 3f),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(17, 34).cuboid(-1.6f, 0f, -1.5f, 3.2f, 4.9f, 3f)
				.uv(33, 34).cuboid(-1.7f, 4.7f, -1.7f, 3.4f, 6.6f, 3.4f)
				.uv(50, 34).cuboid(-1.7f, 10.6f, 0.7f, 3.4f, 1.4f, 1.2f)
				.uv(63, 34).cuboid(-1.8f, 10.2f, -2.7f, 3.6f, 1.5f, 4.5f),
				ModelTransform.of(-1.8f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(83, 34).cuboid(-1.6f, 0f, -1.5f, 3.2f, 4.9f, 3f)
				.uv(99, 34).cuboid(-1.7f, 4.7f, -1.7f, 3.4f, 6.6f, 3.4f)
				.uv(116, 34).cuboid(-1.7f, 10.6f, 0.7f, 3.4f, 1.4f, 1.2f)
				.uv(129, 34).cuboid(-1.8f, 10.2f, -2.7f, 3.6f, 1.5f, 4.5f),
				ModelTransform.of(1.8f, 12f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 256);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(RoxyEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		float bob = e.hasVehicle() ? 0 : MathHelper.sin(age * 0.09f) * 0.7f;
		for (ModelPart p : new ModelPart[]{body, head, rightArm, leftArm}) p.pivotY = (p == rightArm || p == leftArm ? 2f : 0f) + bob;
		rightLeg.pivotY = 12f + bob;
		leftLeg.pivotY = 12f + bob;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		head.roll = 0;
		body.pitch = 0;
		int pose = e.pose();
		if (e.standingOnBike()) {
			// Ride or Die: standing up on the back seat, one fist pumping in the air
			float pump = MathHelper.sin(age * 0.3f);
			rightLeg.pitch = 0.1f;
			leftLeg.pitch = -0.1f;
			rightLeg.yaw = 0.2f;
			leftLeg.yaw = -0.2f;
			rightArm.pitch = -2.7f + pump * 0.25f;
			rightArm.yaw = 0;
			rightArm.roll = 0.2f;
			leftArm.pitch = -0.4f;
			leftArm.yaw = 0;
			leftArm.roll = -0.5f;
			head.pitch -= 0.15f;
		} else if (e.hasVehicle()) {
			// riding pillion: knees up, hands on Rev's waist
			rightLeg.pitch = -1.4f;
			leftLeg.pitch = -1.4f;
			rightLeg.yaw = 0.25f;
			leftLeg.yaw = -0.25f;
			rightArm.pitch = -0.9f;
			leftArm.pitch = -0.9f;
			rightArm.yaw = -0.2f;
			leftArm.yaw = 0.2f;
			rightArm.roll = 0;
			leftArm.roll = 0;
		} else {
			// floating: legs dangle and sway, one knee bent
			float sway = MathHelper.sin(age * 0.07f) * 0.12f;
			rightLeg.pitch = 0.15f + sway;
			leftLeg.pitch = -0.25f - sway;
			rightLeg.yaw = 0;
			leftLeg.yaw = 0;
			rightArm.pitch = MathHelper.sin(age * 0.06f) * 0.08f;
			leftArm.pitch = -MathHelper.sin(age * 0.06f) * 0.08f;
			rightArm.yaw = 0;
			leftArm.yaw = 0;
			rightArm.roll = 0.12f;
			leftArm.roll = -0.12f;
			leftArm.pitch -= 0.0f;
		}
		if (pose > 0 && pose <= 16) {
			// blow a kiss: hand to lips, then fling it out
			float t = 1f - pose / 16f;
			float toLips = MathHelper.clamp(t / 0.45f, 0f, 1f);
			float fling = MathHelper.clamp((t - 0.45f) / 0.3f, 0f, 1f);
			rightArm.pitch = MathHelper.lerp(fling, -2.0f * toLips, -1.5f);
			rightArm.yaw = MathHelper.lerp(fling, 0.55f * toLips, -0.1f);
			rightArm.roll = 0f;
			head.pitch -= 0.15f * fling;
		} else if (pose > 100) {
			// shriek: head thrown forward, arms flung back
			float t = Math.min(1f, (pose - 100) / 6f);
			head.pitch = -0.1f * t + 0.25f * t;
			body.pitch = -0.1f * t;
			rightArm.pitch = 0.6f * t;
			leftArm.pitch = 0.6f * t;
			rightArm.roll = 0.9f * t;
			leftArm.roll = -0.9f * t;
			head.roll = MathHelper.sin(age * 2.5f) * 0.05f * t;
		}
	}
}
