package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.ChefGhoulashEntity;
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

/**
 * Chef Ghoulash's second stage, the Meat Monster: a hulking mound of roast meat dripping with gravy, with sausage-link
 * arms, ham-hock legs, bones poking out, her ghostly face snarling out of the top and her chef's hat still on.
 */
@SuppressWarnings("unused")
public class ChefMeatModel extends SinglePartEntityModel<ChefGhoulashEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("meat_monster"), "main");
	private final ModelPart root, body, head, rightArm, rightFore, leftArm, leftFore, ladle, rightLeg, leftLeg;

	public ChefMeatModel(ModelPart root) {
		super(RenderLayer::getEntityCutoutNoCull);
		this.root = root;
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = root.getChild("right_arm");
		this.rightFore = rightArm.getChild("right_fore");
		this.ladle = rightFore.getChild("ladle");
		this.leftArm = root.getChild("left_arm");
		this.leftFore = leftArm.getChild("left_fore");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-10f, -1f, -6.7f, 20f, 16f, 13.4f)
				.uv(72, 2).cuboid(-8.5f, -6f, -5.7f, 17f, 6f, 11.4f)
				.uv(132, 2).cuboid(-10.5f, 1f, -7f, 21f, 2.3f, 14f)
				.uv(2, 35).cuboid(-11.5f, 5.2f, -7.5f, 23f, 2.3f, 15f)
				.uv(81, 35).cuboid(-11f, 10f, -7.25f, 22f, 2.3f, 14.5f)
				.uv(157, 35).cuboid(-9.5f, 14f, -6.5f, 19f, 2.3f, 13f)
				.uv(224, 35).cuboid(-0.42896107f, 6.004447f, -7.7f, 3.1561825f, 1.3679277f, 1.3693249f)
				.uv(237, 35).cuboid(3.3538332f, 9.148616f, -7.7f, 2.3204591f, 1.7768701f, 1.029441f)
				.uv(2, 56).cuboid(-5.25005f, 13.339437f, -7.7f, 3.993114f, 1.1579233f, 1.4021127f)
				.uv(16, 56).cuboid(0.047859166f, 5.342484f, -7.7f, 2.5672364f, 1.9774543f, 1.1197819f)
				.uv(27, 56).cuboid(1.2879223f, 9.265848f, -7.7f, 2.2659564f, 2.098189f, 1.4876893f)
				.uv(38, 56).cuboid(5.5408225f, 8.5865755f, -7.7f, 2.0885212f, 1.1052717f, 0.89378077f)
				.uv(47, 56).cuboid(5.115034f, 4.240048f, -7.7f, 2.7322912f, 2.2676551f, 1.0200547f)
				.uv(58, 56).cuboid(-0.76526725f, 6.1044335f, -7.7f, 2.1299884f, 1.8599101f, 1.3908476f)
				.uv(69, 56).cuboid(-6.653716f, 3.1401856f, -7.7f, 2.8257403f, 1.1480024f, 1.1476225f)
				.uv(80, 56).cuboid(3.269743f, 9.210473f, -7.7f, 3.0669675f, 2.2116635f, 0.9047817f)
				.uv(91, 56).cuboid(-0.49146783f, 5.2384477f, -7.7f, 3.202609f, 1.2467904f, 1.3428589f)
				.uv(104, 56).cuboid(-7.5509124f, 2.3290925f, -7.7f, 3.6148994f, 2.3320022f, 1.1032621f)
				.uv(117, 56).cuboid(-2.7887876f, 3.4367464f, -7.7f, 2.5494068f, 1.9024286f, 0.9249223f),
				ModelTransform.of(0f, -2f, 0f, 0f, 0f, 0f));
		ModelPartData p_bones = p_body.addChild("bones", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(127, 56).cuboid(-6f, -8f, -5f, 12f, 11f, 9f)
				.uv(172, 56).cuboid(-5f, -10f, -4f, 10f, 2.5f, 8f),
				ModelTransform.of(0f, -7f, -1f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(211, 56).cuboid(-5f, -3f, -3f, 6f, 4.5f, 6f)
				.uv(2, 79).cuboid(-4.7f, 1f, -2.8f, 5.4f, 5.5f, 5.6f)
				.uv(27, 79).cuboid(-5.3f, -2f, -3.4f, 6.6f, 1.3f, 6.8f)
				.uv(57, 79).cuboid(-5.3f, 0f, -3.4f, 6.6f, 1.3f, 6.8f)
				.uv(87, 79).cuboid(-5.3f, 2f, -3.4f, 6.6f, 1.3f, 6.8f),
				ModelTransform.of(-11f, -5f, 0f, 0f, 0f, 0.1f));
		ModelPartData p_right_fore = p_right_arm.addChild("right_fore", ModelPartBuilder.create()
				.uv(117, 79).cuboid(-3.5f, 0f, -3.5f, 7f, 7f, 7f)
				.uv(148, 79).cuboid(-4f, 6f, -4f, 8f, 5f, 8f)
				.uv(183, 79).cuboid(-3.5f, 7f, -4.5f, 2f, 3f, 1f)
				.uv(192, 79).cuboid(-1.2f, 7f, -4.5f, 2f, 3f, 1f)
				.uv(201, 79).cuboid(1.1f, 7f, -4.5f, 2f, 3f, 1f),
				ModelTransform.of(-2f, 6f, 0f, 0f, 0f, 0f));
		ModelPartData p_ladle = p_right_fore.addChild("ladle", ModelPartBuilder.create(),
				ModelTransform.of(-0.5f, 11f, -4f, 1.3f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(210, 79).cuboid(-1f, -3f, -3f, 6f, 4.5f, 6f)
				.uv(2, 96).cuboid(-0.7f, 1f, -2.8f, 5.4f, 5.5f, 5.6f)
				.uv(27, 96).cuboid(-1.3f, -2f, -3.4f, 6.6f, 1.3f, 6.8f)
				.uv(57, 96).cuboid(-1.3f, 0f, -3.4f, 6.6f, 1.3f, 6.8f)
				.uv(87, 96).cuboid(-1.3f, 2f, -3.4f, 6.6f, 1.3f, 6.8f),
				ModelTransform.of(11f, -5f, 0f, 0f, 0f, -0.1f));
		ModelPartData p_left_fore = p_left_arm.addChild("left_fore", ModelPartBuilder.create()
				.uv(117, 96).cuboid(-3.5f, 0f, -3.5f, 7f, 7f, 7f)
				.uv(148, 96).cuboid(-4f, 6f, -4f, 8f, 5f, 8f)
				.uv(183, 96).cuboid(-3.5f, 7f, -4.5f, 2f, 3f, 1f)
				.uv(192, 96).cuboid(-1.2f, 7f, -4.5f, 2f, 3f, 1f)
				.uv(201, 96).cuboid(1.1f, 7f, -4.5f, 2f, 3f, 1f),
				ModelTransform.of(2f, 6f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(210, 96).cuboid(-3.5f, 0f, -3.2f, 7f, 7f, 6.4f)
				.uv(2, 113).cuboid(-4f, 7f, -5f, 8f, 3f, 9f)
				.uv(39, 113).cuboid(-4.3f, 7.8f, -5.3f, 8.6f, 1.5f, 9.6f),
				ModelTransform.of(-5f, 14f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(79, 113).cuboid(-3.5f, 0f, -3.2f, 7f, 7f, 6.4f)
				.uv(109, 113).cuboid(-4f, 7f, -5f, 8f, 3f, 9f)
				.uv(146, 113).cuboid(-4.3f, 7.8f, -5.3f, 8.6f, 1.5f, 9.6f),
				ModelTransform.of(5f, 14f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 256);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(ChefGhoulashEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		// a heavy, lumbering stomp; the whole mound heaves as it breathes
		float stride = MathHelper.cos(limbAngle * 0.45f) * 0.7f * limbDistance;
		float heave = MathHelper.sin(age * 0.12f);
		body.pivotY = -2f + heave * 0.4f + Math.abs(MathHelper.sin(limbAngle * 0.45f)) * limbDistance * 1.2f;
		body.pitch = 0.08f + heave * 0.02f;
		body.roll = stride * 0.08f;
		body.yaw = 0;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.6f;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE * 0.5f - 0.05f;
		head.roll = 0;
		rightLeg.pitch = stride;
		leftLeg.pitch = -stride;
		rightArm.pivotY = -5f + heave * 0.4f;
		leftArm.pivotY = -5f + heave * 0.4f;
		rightArm.pitch = -stride * 0.6f - 0.15f;
		leftArm.pitch = stride * 0.6f - 0.15f;
		rightArm.yaw = 0;
		leftArm.yaw = 0;
		rightArm.roll = 0.18f + heave * 0.03f;
		leftArm.roll = -0.18f - heave * 0.03f;
		rightFore.pitch = -0.35f;
		leftFore.pitch = -0.35f;
		ladle.pitch = 1.3f;
		int pose = e.pose();
		if (e.dazed()) {
			// slumped over, arms dragging, swaying
			body.pitch = 0.35f;
			head.pitch = 0.4f;
			head.roll = MathHelper.sin(age * 0.2f) * 0.3f;
			body.roll = MathHelper.sin(age * 0.2f) * 0.06f;
			rightArm.pitch = 0.3f;
			leftArm.pitch = 0.3f;
			rightFore.pitch = 0;
			leftFore.pitch = 0;
		} else if (pose > 0 && pose <= 14) {
			// scoops up food and hurls it with the left fist
			float t = 1f - pose / 14f;
			float wind = MathHelper.clamp(t / 0.5f, 0f, 1f), fling = MathHelper.clamp((t - 0.5f) / 0.3f, 0f, 1f);
			leftArm.pitch = MathHelper.lerp(fling, -2.6f * wind, -1.0f);
			leftFore.pitch = MathHelper.lerp(fling, -0.8f * wind, 0f);
			body.yaw = MathHelper.lerp(fling, 0.25f * wind, -0.2f);
		} else if (pose > 100) {
			// both fists up over its head, then SLAM down
			float t = 1f - (pose - 100) / 12f;
			float up = MathHelper.clamp(t / 0.45f, 0f, 1f), down = MathHelper.clamp((t - 0.45f) / 0.2f, 0f, 1f);
			float a = MathHelper.lerp(down, -2.9f * up, -0.9f);
			rightArm.pitch = a;
			leftArm.pitch = a;
			rightArm.roll = 0.05f;
			leftArm.roll = -0.05f;
			rightFore.pitch = -0.3f * up * (1 - down);
			leftFore.pitch = -0.3f * up * (1 - down);
			body.pitch = 0.08f - 0.15f * up * (1 - down) + 0.3f * down;
		}
	}
}
