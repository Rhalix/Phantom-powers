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
				.uv(100, 0).cuboid(-4f, 0f, -2.5f, 8f, 7f, 5f)
				.uv(32, 28).cuboid(-3.5f, 3f, -3.4f, 7f, 7f, 1f)
				.uv(64, 16).cuboid(-5f, 6f, -3f, 10f, 4f, 6f, new Dilation(0.2f))
				.uv(0, 37).cuboid(-4.5f, 1.25f, -3f, 9f, 3f, 1f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(0f, 4f, 0f, 0f, 0f, 0f));
		ModelPartData p_skirt = p_body.addChild("skirt", ModelPartBuilder.create()
				.uv(0, 28).cuboid(-5f, 0f, -3f, 10f, 3f, 6f, new Dilation(0.25f)),
				ModelTransform.of(0f, 10f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(82, 37).cuboid(-0.5f, -4.4f, -4.6f, 1f, 1f, 1f)
				.uv(48, 37).cuboid(-3f, -1.25f, -3.2f, 6f, 2f, 1f, new Dilation(0f, -0.25f, 0f))
				.uv(28, 16).cuboid(-4.5f, -9.5f, -4.5f, 9f, 2f, 9f)
				.uv(32, 0).cuboid(-5f, -15.5f, -5f, 10f, 6f, 10f)
				.uv(86, 37).cuboid(-4.6f, -5f, -2f, 1f, 1f, 1f)
				.uv(90, 37).cuboid(3.6f, -5f, -2f, 1f, 1f, 1f)
				.uv(0, 16).cuboid(-4.4f, -8f, -3.5f, 1f, 6f, 6f)
				.uv(14, 16).cuboid(3.4f, -8f, -3.5f, 1f, 6f, 6f)
				.uv(20, 37).cuboid(1.5f, -1.5f, -4.6f, 1f, 3f, 1f)
				.uv(72, 28).cuboid(-4.6f, -6f, 1f, 1f, 2f, 4f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_braid = p_head.addChild("braid", ModelPartBuilder.create()
				.uv(48, 28).cuboid(-1.5f, 0f, -0.5f, 3f, 4f, 3f)
				.uv(60, 28).cuboid(-1.5f, 4f, -0.5f, 3f, 4f, 3f, new Dilation(-0.25f, 0f, -0.25f))
				.uv(24, 37).cuboid(-1.5f, 7.5f, -0.5f, 3f, 1f, 3f)
				.uv(82, 28).cuboid(-1f, 8.5f, 0f, 2f, 3f, 2f),
				ModelTransform.of(0f, -3f, 4f, 0.25f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(72, 0).cuboid(-3f, -2f, -1.5f, 3f, 11f, 3f),
				ModelTransform.of(-5f, 6f, 0f, 0f, 0f, 0f));
		ModelPartData p_ladle = p_right_arm.addChild("ladle", ModelPartBuilder.create()
				.uv(84, 0).cuboid(-0.5f, -2f, -0.5f, 1f, 13f, 1f)
				.uv(90, 28).cuboid(-2f, 10.5f, -2f, 4f, 1f, 4f)
				.uv(62, 37).cuboid(-2f, 8.5f, -2.2f, 4f, 2f, 1f, new Dilation(0f, 0f, -0.2f))
				.uv(72, 37).cuboid(-2f, 8.5f, 1.2f, 4f, 2f, 1f, new Dilation(0f, 0f, -0.2f))
				.uv(106, 28).cuboid(-2.2f, 8.5f, -1.5f, 1f, 2f, 3f, new Dilation(-0.2f, 0f, -0.1f))
				.uv(114, 28).cuboid(1.2f, 8.5f, -1.5f, 1f, 2f, 3f, new Dilation(-0.2f, 0f, -0.1f))
				.uv(36, 37).cuboid(-1.5f, 8.9f, -1.5f, 3f, 1f, 3f, new Dilation(-0.1f, -0.3f, -0.1f)),
				ModelTransform.of(-1.5f, 8f, -0.5f, 1.2f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(88, 0).cuboid(0f, -2f, -1.5f, 3f, 11f, 3f),
				ModelTransform.of(5f, 6f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(96, 16).cuboid(-1.5f, 0f, -1.5f, 3f, 7f, 3f),
				ModelTransform.of(-2f, 17f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(108, 16).cuboid(-1.5f, 0f, -1.5f, 3f, 7f, 3f),
				ModelTransform.of(2f, 17f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 64);
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
