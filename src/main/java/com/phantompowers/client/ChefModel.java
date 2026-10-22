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

/** Chef Ghoulash: a ghost cook with a tall chef's hat, a long braid and a giant ladle, striding about in a pleated skirt and pink clogs. */
@SuppressWarnings("unused")
public class ChefModel extends SinglePartEntityModel<ChefGhoulashEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("chef_ghoulash"), "main");
	private final ModelPart root, body, head, braid, skirt, rightArm, leftArm, ladle, rightLeg, leftLeg;

	public ChefModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.braid = head.getChild("braid");
		this.skirt = body.getChild("skirt");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.ladle = rightArm.getChild("ladle");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-4.2f, 0f, -2.8f, 8.4f, 7f, 5.6f)
				.uv(33, 2).cuboid(-4.7f, 6f, -3f, 9.4f, 4.3f, 6f)
				.uv(67, 2).cuboid(-3.25f, 2f, -3.05f, 6.5f, 4.5f, 0.4f)
				.uv(84, 2).cuboid(-3.1f, -0.2f, -3.15f, 0.75f, 4.5f, 0.45f)
				.uv(90, 2).cuboid(-3.1f, -0.15f, 2.8f, 0.75f, 7.7f, 0.4f)
				.uv(96, 2).cuboid(2.35f, -0.2f, -3.15f, 0.75f, 4.5f, 0.45f)
				.uv(102, 2).cuboid(2.35f, -0.15f, 2.8f, 0.75f, 7.7f, 0.4f)
				.uv(108, 2).cuboid(-3.9f, 6f, -3.32f, 7.8f, 4.3f, 0.45f)
				.uv(128, 2).cuboid(-4.85f, 7f, -3.2f, 9.7f, 0.85f, 6.4f)
				.uv(164, 2).cuboid(-0.6f, 7.1f, 3.3f, 1.2f, 1.2f, 0.7f)
				.uv(171, 2).cuboid(-1.6f, 7f, 3.15f, 1.2f, 1.1f, 0.55f)
				.uv(178, 2).cuboid(-1.2f, 8f, 3.15f, 0.65f, 2f, 0.4f)
				.uv(184, 2).cuboid(0.5f, 7f, 3.15f, 1.2f, 1.1f, 0.55f)
				.uv(191, 2).cuboid(0.9f, 8f, 3.15f, 0.65f, 2f, 0.4f),
				ModelTransform.of(0f, 4f, 0f, 0f, 0f, 0f));
		ModelPartData p_skirt = p_body.addChild("skirt", ModelPartBuilder.create()
				.uv(197, 2).cuboid(-5.1f, -0.7f, -3.2f, 10.2f, 5.8f, 6.4f)
				.uv(234, 2).cuboid(-4f, -0.9f, -3.48f, 8f, 5.2f, 0.45f),
				ModelTransform.of(0f, 10f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(2, 18).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(37, 18).cuboid(-0.65f, -4.4f, -4.45f, 1.3f, 1.25f, 0.65f)
				.uv(44, 18).cuboid(-4.2f, -7.8f, 1.8f, 8.4f, 6.8f, 2.6f)
				.uv(69, 18).cuboid(-4.45f, -6f, 0f, 0.9f, 4.8f, 3f)
				.uv(80, 18).cuboid(-4.4f, -3f, 1.3f, 0.8f, 3f, 2f)
				.uv(89, 18).cuboid(3.55f, -6f, 0f, 0.9f, 4.8f, 3f)
				.uv(100, 18).cuboid(3.6f, -3f, 1.3f, 0.8f, 3f, 2f)
				.uv(109, 18).cuboid(-4.5f, -8.8f, -4.45f, 9f, 1.8f, 8.9f)
				.uv(148, 18).cuboid(-4.1f, -10.3f, -3.9f, 8.2f, 1.8f, 7.8f)
				.uv(183, 18).cuboid(-4.4f, -7.1f, 3.05f, 8.8f, 3.2f, 1.4f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_braid = p_head.addChild("braid", ModelPartBuilder.create(),
				ModelTransform.of(0f, -3f, 4f, 0.25f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(207, 18).cuboid(-3.3f, -2.2f, -1.9f, 3.6f, 3.6f, 3.8f)
				.uv(225, 18).cuboid(-3f, 1f, -1.55f, 3f, 4.7f, 3.1f)
				.uv(2, 37).cuboid(-3.15f, 5.5f, -1.7f, 3.3f, 3.5f, 3.4f)
				.uv(19, 37).cuboid(-3f, 8.5f, -1.8f, 0.85f, 0.9f, 1.5f)
				.uv(27, 37).cuboid(-1.97f, 8.5f, -1.8f, 0.85f, 0.9f, 1.5f)
				.uv(35, 37).cuboid(-0.94f, 8.5f, -1.8f, 0.85f, 0.9f, 1.5f),
				ModelTransform.of(-5f, 6f, 0f, 0f, 0f, 0f));
		ModelPartData p_ladle = p_right_arm.addChild("ladle", ModelPartBuilder.create(),
				ModelTransform.of(-1.5f, 8f, -0.5f, 1.2f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(43, 37).cuboid(-0.3f, -2.2f, -1.9f, 3.6f, 3.6f, 3.8f)
				.uv(61, 37).cuboid(0f, 1f, -1.55f, 3f, 4.7f, 3.1f)
				.uv(77, 37).cuboid(-0.15f, 5.5f, -1.7f, 3.3f, 3.5f, 3.4f)
				.uv(94, 37).cuboid(0f, 8.5f, -1.8f, 0.85f, 0.9f, 1.5f)
				.uv(102, 37).cuboid(1.03f, 8.5f, -1.8f, 0.85f, 0.9f, 1.5f)
				.uv(110, 37).cuboid(2.06f, 8.5f, -1.8f, 0.85f, 0.9f, 1.5f),
				ModelTransform.of(5f, 6f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(118, 37).cuboid(-1.5f, 1.8f, -1.5f, 3f, 2.7f, 3f)
				.uv(133, 37).cuboid(-1.55f, 4.4f, -1.55f, 3.1f, 1.6f, 3.1f)
				.uv(149, 37).cuboid(-1.7f, 6f, -2.25f, 3.4f, 1f, 4.1f)
				.uv(167, 37).cuboid(-1f, 5.9f, -2.3f, 2f, 0.5f, 0.25f),
				ModelTransform.of(-2f, 17f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(175, 37).cuboid(-1.5f, 1.8f, -1.5f, 3f, 2.7f, 3f)
				.uv(190, 37).cuboid(-1.55f, 4.4f, -1.55f, 3.1f, 1.6f, 3.1f)
				.uv(206, 37).cuboid(-1.7f, 6f, -2.25f, 3.4f, 1f, 4.1f)
				.uv(224, 37).cuboid(-1f, 5.9f, -2.3f, 2f, 0.5f, 0.25f),
				ModelTransform.of(2f, 17f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 256);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(ChefGhoulashEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		boolean dazed = e.dazed();
		float stride = MathHelper.cos(limbAngle * 0.6662f) * 0.9f * limbDistance;
		float bob = MathHelper.sin(age * 0.08f) * 0.3f + Math.abs(MathHelper.sin(limbAngle * 0.6662f)) * limbDistance * 0.6f;
		body.pivotY = 4f - bob;
		rightArm.pivotY = 6f - bob;
		leftArm.pivotY = 6f - bob;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		head.roll = 0;
		body.pitch = 0;
		body.roll = 0;
		rightLeg.pitch = stride;
		leftLeg.pitch = -stride;
		rightLeg.roll = 0;
		leftLeg.roll = 0;
		skirt.pitch = 0;
		skirt.roll = stride * 0.06f;
		// her braid swings as she moves
		braid.pitch = 0.25f - headPitch * MathHelper.RADIANS_PER_DEGREE * 0.5f + limbDistance * 0.5f + MathHelper.sin(age * 0.09f) * 0.06f;
		braid.roll = MathHelper.sin(age * 0.07f) * 0.12f + stride * 0.1f;
		// idle: ladle raised a little, other hand on his hip
		rightArm.pitch = -0.35f + MathHelper.sin(age * 0.07f) * 0.08f;
		rightArm.yaw = 0;
		rightArm.roll = 0.1f;
		leftArm.pitch = 0.1f;
		leftArm.yaw = -0.3f;
		leftArm.roll = -0.55f;
		ladle.pitch = 1.2f;
		int pose = e.pose();
		if (dazed) {
			// seeing stars: head lolling, arms limp, wobbling
			head.roll = MathHelper.sin(age * 0.2f) * 0.3f;
			head.pitch = 0.35f;
			body.roll = MathHelper.sin(age * 0.2f) * 0.08f;
			rightArm.pitch = 0.2f;
			rightArm.roll = 0.35f;
			leftArm.pitch = 0.2f;
			leftArm.yaw = 0;
			leftArm.roll = -0.35f;
			ladle.pitch = 0.3f;
			rightLeg.roll = 0.12f;          // wobbly knees
			leftLeg.roll = -0.12f;
		} else if (pose > 0 && pose <= 14) {
			// overarm throw with the free hand
			float t = 1f - pose / 14f;
			float wind = MathHelper.clamp(t / 0.5f, 0f, 1f), fling = MathHelper.clamp((t - 0.5f) / 0.3f, 0f, 1f);
			leftArm.pitch = MathHelper.lerp(fling, -2.8f * wind, -1.2f);
			leftArm.yaw = 0;
			leftArm.roll = -0.15f;
			body.pitch = 0.15f * fling;
		} else if (pose > 100) {
			// big ladle swing: up over his head, then down
			float t = 1f - (pose - 100) / 12f;
			float up = MathHelper.clamp(t / 0.4f, 0f, 1f), down = MathHelper.clamp((t - 0.4f) / 0.25f, 0f, 1f);
			rightArm.pitch = MathHelper.lerp(down, -2.9f * up, -0.4f);
			rightArm.yaw = 0.2f * down;
			body.pitch = 0.25f * down;
			ladle.pitch = 1.2f + 0.3f * down;
		}
		if (e.meat()) {
			// the meat monster hunches and heaves
			body.pitch += 0.12f + MathHelper.sin(age * 0.18f) * 0.04f;
			head.pitch -= 0.1f;
		}
	}
}
