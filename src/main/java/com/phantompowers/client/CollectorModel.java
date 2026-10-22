package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.CollectorEntity;
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

/** The Collector: a lanky ghost explorer, net raised, lantern swinging, coat tails trailing off into mist. */
@SuppressWarnings("unused")
public class CollectorModel extends SinglePartEntityModel<CollectorEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("collector"), "main");
	private final ModelPart root, body, head, coat, tail, rightArm, leftArm, staff, lantern;

	public CollectorModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.coat = body.getChild("coat");
		this.tail = coat.getChild("tail");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.staff = rightArm.getChild("staff");
		this.lantern = leftArm.getChild("lantern");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(4, 0).cuboid(-4f, 0f, -2.5f, 8f, 11f, 5f),
				ModelTransform.of(0f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_coat = p_body.addChild("coat", ModelPartBuilder.create()
				.uv(86, 0).cuboid(-4.5f, 0f, -3f, 9f, 6f, 6f),
				ModelTransform.of(0f, 11f, 0f, 0.08f, 0f, 0f));
		ModelPartData p_tail = p_coat.addChild("tail", ModelPartBuilder.create()
				.uv(92, 25).cuboid(-3.5f, 0f, -2.5f, 7f, 5f, 5f),
				ModelTransform.of(0f, 6f, 0f, 0.15f, 0f, 0f));
		ModelPartData p_pack = p_body.addChild("pack", ModelPartBuilder.create()
				.uv(0, 25).cuboid(-3.5f, 1f, 2.5f, 7f, 8f, 3f)
				.uv(112, 36).cuboid(-3f, -2f, 3f, 2f, 3f, 2f)
				.uv(72, 36).cuboid(0.8f, -2.75f, 3.2f, 2f, 4f, 2f, new Dilation(0f, -0.25f, 0f))
				.uv(80, 36).cuboid(-1.5f, 8.75f, 2.5f, 3f, 3f, 3f, new Dilation(-0.3f, -0.25f, -0.3f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_cannon = p_body.addChild("cannon", ModelPartBuilder.create()
				.uv(32, 36).cuboid(-0.5f, -3f, -3f, 3f, 3f, 6f),
				ModelTransform.of(3f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(58, 0).cuboid(-3.5f, -8f, -3.5f, 7f, 8f, 7f)
				.uv(0, 36).cuboid(-4f, -6.4f, -4f, 8f, 2f, 8f, new Dilation(-0.2f, -0.4f, -0.2f))
				.uv(20, 25).cuboid(-5f, -8.6f, -5f, 10f, 1f, 10f)
				.uv(60, 25).cuboid(-4f, -11.6f, -4f, 8f, 3f, 8f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(30, 0).cuboid(-2.5f, -2f, -2f, 3f, 12f, 4f),
				ModelTransform.of(-5f, 3f, 0f, 0f, 0f, 0f));
		ModelPartData p_staff = p_right_arm.addChild("staff", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-0.5f, -16f, -0.5f, 1f, 24f, 1f)
				.uv(0, 46).cuboid(-3.5f, -23f, -0.5f, 7f, 1f, 1f)
				.uv(16, 46).cuboid(-3.5f, -17f, -0.5f, 7f, 1f, 1f)
				.uv(92, 36).cuboid(-3.5f, -22f, -0.5f, 1f, 5f, 1f)
				.uv(96, 36).cuboid(2.5f, -22f, -0.5f, 1f, 5f, 1f)
				.uv(100, 36).cuboid(-2.5f, -22f, -0.5f, 5f, 5f, 1f, new Dilation(0f, 0f, -0.25f)),
				ModelTransform.of(-1f, 10f, -0.5f, -0.25f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(44, 0).cuboid(-0.5f, -2f, -2f, 3f, 12f, 4f)
				.uv(50, 36).cuboid(2.3f, 4f, -2f, 1f, 4f, 4f, new Dilation(-0.2f, 0f, 0f)),
				ModelTransform.of(5f, 3f, 0f, 0f, 0f, 0f));
		ModelPartData p_lantern = p_left_arm.addChild("lantern", ModelPartBuilder.create()
				.uv(60, 36).cuboid(-1.5f, 0f, -1.5f, 3f, 4f, 3f)
				.uv(32, 46).cuboid(-0.5f, -1f, -0.5f, 1f, 1f, 1f),
				ModelTransform.of(1f, 11f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 64);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(CollectorEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		float bob = MathHelper.sin(age * 0.08f) * 0.7f;
		body.pivotY = 2f + bob;
		rightArm.pivotY = 3f + bob;
		leftArm.pivotY = 3f + bob;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		head.roll = 0;
		body.pitch = 0.05f + limbDistance * 0.2f;
		coat.pitch = 0.08f + limbDistance * 0.4f + MathHelper.sin(age * 0.1f) * 0.04f;
		tail.pitch = 0.15f + limbDistance * 0.4f + MathHelper.sin(age * 0.12f + 1f) * 0.08f;
		tail.roll = MathHelper.sin(age * 0.11f) * 0.12f;
		// net held up and ready, lantern swinging
		rightArm.pitch = -0.4f + MathHelper.sin(age * 0.06f) * 0.06f;
		rightArm.yaw = 0;
		rightArm.roll = 0.08f;
		staff.pitch = -0.25f;
		leftArm.pitch = -0.2f;
		leftArm.yaw = 0;
		leftArm.roll = -0.1f;
		lantern.pitch = 0.2f - leftArm.pitch + MathHelper.sin(age * 0.15f) * 0.15f;
		lantern.roll = MathHelper.cos(age * 0.12f) * 0.1f;
		int pose = e.pose();
		if (e.dazed()) {
			head.pitch = 0.4f;
			head.roll = MathHelper.sin(age * 0.2f) * 0.3f;
			rightArm.pitch = 0.3f;
			leftArm.pitch = 0.3f;
			staff.pitch = 0.6f;
		} else if (pose > 0 && pose <= 14) {
			// lobs a jar with the lantern hand
			float t = 1f - pose / 14f;
			float wind = MathHelper.clamp(t / 0.5f, 0f, 1f), fling = MathHelper.clamp((t - 0.5f) / 0.3f, 0f, 1f);
			leftArm.pitch = MathHelper.lerp(fling, -2.6f * wind, -1.1f);
			body.pitch += 0.15f * fling;
		} else if (pose > 100) {
			// a big two-handed sweep of the net
			float t = 1f - (pose - 100) / 12f;
			float up = MathHelper.clamp(t / 0.4f, 0f, 1f), down = MathHelper.clamp((t - 0.4f) / 0.3f, 0f, 1f);
			rightArm.pitch = MathHelper.lerp(down, -2.8f * up, -0.6f);
			rightArm.yaw = 0.6f * down;
			staff.pitch = MathHelper.lerp(down, -0.25f, 0.6f);
			body.pitch += 0.2f * down;
		}
	}
}
