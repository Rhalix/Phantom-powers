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
 * arms, ham-hock legs, bones poking out, his ghostly face snarling out of the top and his chef's hat still on.
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
				.uv(0, 0).cuboid(-10f, 0f, -7f, 20f, 16f, 14f)
				.uv(28, 79).cuboid(-8f, 5f, -8.5f, 16f, 9f, 2f)
				.uv(68, 0).cuboid(-8.5f, -7f, -5.5f, 17f, 8f, 11f),
				ModelTransform.of(0f, -2f, 0f, 0f, 0f, 0f));
		ModelPartData p_bones = p_body.addChild("bones", ModelPartBuilder.create()
				.uv(98, 79).cuboid(6f, -10f, -1f, 2f, 5f, 2f)
				.uv(40, 94).cuboid(5.5f, -11.5f, -1.5f, 3f, 2f, 3f)
				.uv(68, 94).cuboid(-11.5f, 6f, 2f, 3f, 2f, 2f)
				.uv(106, 79).cuboid(-13f, 5.5f, 1.5f, 2f, 3f, 3f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(0, 48).cuboid(-4f, -7f, -5.5f, 8f, 8f, 8f)
				.uv(64, 30).cuboid(-6f, -9f, -3f, 12f, 9f, 8f)
				.uv(102, 94).cuboid(-3.5f, -2.2f, -6.2f, 7f, 1f, 1f)
				.uv(64, 79).cuboid(-4.5f, -11f, -3.5f, 9f, 2f, 8f)
				.uv(0, 64).cuboid(-5f, -17f, -4f, 10f, 6f, 9f),
				ModelTransform.of(0f, -7f, -1f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(38, 64).cuboid(-5f, -3f, -3f, 6f, 9f, 6f),
				ModelTransform.of(-11f, -5f, 0f, 0f, 0f, 0.1f));
		ModelPartData p_right_fore = p_right_arm.addChild("right_fore", ModelPartBuilder.create()
				.uv(62, 64).cuboid(-3.5f, 0f, -3.5f, 7f, 8f, 7f)
				.uv(32, 48).cuboid(-4.5f, 7f, -4.5f, 9f, 7f, 9f),
				ModelTransform.of(-2f, 6f, 0f, 0f, 0f, 0f));
		ModelPartData p_ladle = p_right_fore.addChild("ladle", ModelPartBuilder.create()
				.uv(104, 30).cuboid(-0.5f, -2f, -0.5f, 1f, 16f, 1f)
				.uv(0, 94).cuboid(-2.5f, 13.5f, -2.5f, 5f, 1f, 5f)
				.uv(78, 94).cuboid(-2.5f, 11.5f, -2.7f, 5f, 2f, 1f, new Dilation(0f, 0f, -0.2f))
				.uv(90, 94).cuboid(-2.5f, 11.5f, 1.7f, 5f, 2f, 1f, new Dilation(0f, 0f, -0.2f))
				.uv(20, 94).cuboid(-2.7f, 11.5f, -2f, 1f, 2f, 4f, new Dilation(-0.2f, 0f, -0.1f))
				.uv(30, 94).cuboid(1.7f, 11.5f, -2f, 1f, 2f, 4f, new Dilation(-0.2f, 0f, -0.1f))
				.uv(52, 94).cuboid(-2f, 11.9f, -2f, 4f, 1f, 4f, new Dilation(-0.1f, -0.3f, -0.1f)),
				ModelTransform.of(-0.5f, 11f, -4f, 1.3f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(90, 64).cuboid(-1f, -3f, -3f, 6f, 9f, 6f),
				ModelTransform.of(11f, -5f, 0f, 0f, 0f, -0.1f));
		ModelPartData p_left_fore = p_left_arm.addChild("left_fore", ModelPartBuilder.create()
				.uv(0, 79).cuboid(-3.5f, 0f, -3.5f, 7f, 8f, 7f)
				.uv(68, 48).cuboid(-4.5f, 7f, -4.5f, 9f, 7f, 9f),
				ModelTransform.of(2f, 6f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(0, 30).cuboid(-4f, 0f, -4f, 8f, 10f, 8f),
				ModelTransform.of(-5f, 14f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(32, 30).cuboid(-4f, 0f, -4f, 8f, 10f, 8f),
				ModelTransform.of(5f, 14f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 128);
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
