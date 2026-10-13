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
				.uv(0, 0).cuboid(-3.5f, 0f, -2f, 7f, 12f, 4f)
				.uv(22, 0).cuboid(-3.5f, 0f, -2f, 7f, 12f, 4f, new Dilation(0.3f))
				.uv(80, 16).cuboid(-4f, 8f, -2.5f, 8f, 4f, 5f, new Dilation(0.2f, 0f, 0.2f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(44, 0).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(48, 16).cuboid(-4f, -7.6f, -4f, 8f, 2f, 8f, new Dilation(0.35f, 0f, 0.35f))
				.uv(12, 16).cuboid(-4.5f, -8.6f, -4.5f, 9f, 2f, 9f)
				.uv(106, 16).cuboid(-4.5f, -7f, -1f, 1f, 4f, 5f)
				.uv(0, 31).cuboid(3.5f, -7f, -1f, 1f, 4f, 5f)
				.uv(12, 31).cuboid(-4f, -9.6f, -2f, 8f, 1f, 6f)
				.uv(40, 31).cuboid(-4.4f, -8f, 3.6f, 9f, 5f, 1f)
				.uv(60, 31).cuboid(-4.2f, -8.2f, -4.6f, 4f, 2f, 1f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(108, 0).cuboid(-2f, -2f, -1.5f, 3f, 12f, 3f),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0.06f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(0, 16).cuboid(-1f, -2f, -1.5f, 3f, 12f, 3f),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, -0.06f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(76, 0).cuboid(-2f, 0f, -2f, 4f, 12f, 4f, new Dilation(-0.25f, 0f, -0.25f)),
				ModelTransform.of(-1.8f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(92, 0).cuboid(-2f, 0f, -2f, 4f, 12f, 4f, new Dilation(-0.25f, 0f, -0.25f)),
				ModelTransform.of(1.8f, 12f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 128);
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
