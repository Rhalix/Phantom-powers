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
				.uv(2, 2).cuboid(-4.3f, 0f, -2.7f, 8.6f, 10.3f, 5.4f)
				.uv(33, 2).cuboid(-4.5f, 9f, -2.9f, 9f, 1.3f, 5.8f)
				.uv(66, 2).cuboid(-1.1f, 8.9f, -3.2f, 2.2f, 1.5f, 0.4f)
				.uv(75, 2).cuboid(-4f, 6.3f, -4.45f, 2f, 1.4f, 0.7f)
				.uv(84, 2).cuboid(-2.55f, 5f, -4.45f, 2f, 1.4f, 0.7f)
				.uv(93, 2).cuboid(-1.1f, 3.7f, -4.45f, 2f, 1.4f, 0.7f)
				.uv(102, 2).cuboid(0.35f, 2.4f, -4.45f, 2f, 1.4f, 0.7f)
				.uv(111, 2).cuboid(1.8f, 1.1f, -4.45f, 2f, 1.4f, 0.7f)
				.uv(120, 2).cuboid(1.8f, 1.2f, -4.8f, 0.45f, 0.5f, 0.35f)
				.uv(125, 2).cuboid(-3.9f, 0.4f, -3.05f, 7.8f, 4.7f, 0.8f)
				.uv(146, 2).cuboid(-3.5f, 0.6f, -3.65f, 7f, 0.65f, 0.7f)
				.uv(165, 2).cuboid(-1.1f, 1.6f, -3.95f, 2.2f, 1.8f, 0.4f)
				.uv(174, 2).cuboid(-3.7f, 1.5f, -3.8f, 3.4f, 2.9f, 0.55f)
				.uv(185, 2).cuboid(-2.9f, 2.2f, -4.1f, 0.8f, 0.35f, 0.3f)
				.uv(191, 2).cuboid(-4.35f, 3.5f, -2.1f, 0.7f, 4f, 4.4f)
				.uv(205, 2).cuboid(-0.3f, 1.5f, -3.8f, 3.4f, 2.9f, 0.55f)
				.uv(216, 2).cuboid(2.1f, 2.2f, -4.1f, 0.8f, 0.35f, 0.3f)
				.uv(222, 2).cuboid(3.65f, 3.5f, -2.1f, 0.7f, 4f, 4.4f)
				.uv(236, 2).cuboid(-3.1f, 5f, -3.2f, 6.2f, 0.9f, 0.65f)
				.uv(2, 21).cuboid(-2.8f, 5f, -3.65f, 5.6f, 0.2f, 0.25f)
				.uv(17, 21).cuboid(-3.1f, 6.15f, -3.2f, 6.2f, 0.9f, 0.65f)
				.uv(34, 21).cuboid(-2.8f, 6.15f, -3.65f, 5.6f, 0.2f, 0.25f)
				.uv(49, 21).cuboid(-3.1f, 7.3f, -3.2f, 6.2f, 0.9f, 0.65f)
				.uv(66, 21).cuboid(-2.8f, 7.3f, -3.65f, 5.6f, 0.2f, 0.25f)
				.uv(81, 21).cuboid(-3.9f, 0.5f, 2.5f, 7.8f, 7.8f, 0.9f)
				.uv(102, 21).cuboid(-0.55f, 1.2f, 3.25f, 1.1f, 6.3f, 0.45f)
				.uv(109, 21).cuboid(-0.35f, 1.7f, 3.65f, 0.7f, 0.35f, 0.2f)
				.uv(114, 21).cuboid(-0.35f, 2.9f, 3.65f, 0.7f, 0.35f, 0.2f)
				.uv(119, 21).cuboid(-0.35f, 4.1f, 3.65f, 0.7f, 0.35f, 0.2f)
				.uv(124, 21).cuboid(-0.35f, 5.3f, 3.65f, 0.7f, 0.35f, 0.2f),
				ModelTransform.of(0f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_coat = p_body.addChild("coat", ModelPartBuilder.create(),
				ModelTransform.of(0f, 11f, 0f, 0.08f, 0f, 0f));
		ModelPartData p_tail = p_coat.addChild("tail", ModelPartBuilder.create(),
				ModelTransform.of(0f, 6f, 0f, 0.15f, 0f, 0f));
		ModelPartData p_pack = p_body.addChild("pack", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_cannon = p_body.addChild("cannon", ModelPartBuilder.create(),
				ModelTransform.of(3f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(129, 21).cuboid(-4f, -8f, -3.5f, 8f, 8f, 7f)
				.uv(162, 21).cuboid(-4.2f, -9.5f, -3.7f, 8.4f, 2f, 7.4f)
				.uv(197, 21).cuboid(-4f, -8.7f, 2.5f, 8f, 7.7f, 1.4f)
				.uv(219, 21).cuboid(-0.85f, -0.2f, -3.85f, 1.7f, 1.7f, 0.7f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_flame0 = p_head.addChild("flame0", ModelPartBuilder.create()
				.uv(199, 52).cuboid(-0.65f, -1.7f, -2f, 1.3f, 2.7f, 4f),
				ModelTransform.of(-3.5f, -9.4f, 0f, 0f, 0f, -0.24f));
		ModelPartData p_flame1 = p_head.addChild("flame1", ModelPartBuilder.create()
				.uv(213, 52).cuboid(-0.65f, -2.5f, -2f, 1.3f, 3.5f, 4f),
				ModelTransform.of(-1.9f, -9.4f, 0f, 0.05f, 0f, -0.12f));
		ModelPartData p_flame2 = p_head.addChild("flame2", ModelPartBuilder.create()
				.uv(227, 52).cuboid(-0.65f, -1.7f, -2f, 1.3f, 2.7f, 4f),
				ModelTransform.of(-0.3f, -9.4f, 0f, 0.1f, 0f, 0f));
		ModelPartData p_flame3 = p_head.addChild("flame3", ModelPartBuilder.create()
				.uv(241, 52).cuboid(-0.65f, -2.5f, -2f, 1.3f, 3.5f, 4f),
				ModelTransform.of(1.3f, -9.4f, 0f, 0.15f, 0f, 0.12f));
		ModelPartData p_flame4 = p_head.addChild("flame4", ModelPartBuilder.create()
				.uv(2, 63).cuboid(-0.65f, -1.7f, -2f, 1.3f, 2.7f, 4f),
				ModelTransform.of(2.9f, -9.4f, 0f, 0.2f, 0f, 0.24f));
		ModelPartData p_right_boot = p_body.addChild("right_boot", ModelPartBuilder.create()
				.uv(16, 63).cuboid(-1.6f, 0f, -1.7f, 3.2f, 7.2f, 3.4f)
				.uv(33, 63).cuboid(-1.8f, 6.5f, -1.9f, 3.6f, 0.8f, 3.8f)
				.uv(51, 63).cuboid(-1.8f, 7.2f, -2f, 3.6f, 3.9f, 4f)
				.uv(70, 63).cuboid(-2f, 10f, -3.2f, 4f, 1.4f, 5.2f)
				.uv(92, 63).cuboid(-0.6f, 8f, -2.2f, 1.2f, 2.6f, 0.3f)
				.uv(98, 63).cuboid(-1.5f, 4.9f, -2.2f, 3f, 1.8f, 0.8f)
				.uv(109, 63).cuboid(-1.15f, 5.05f, -2.85f, 2.3f, 1.3f, 0.4f)
				.uv(118, 63).cuboid(-1.55f, 7.6f, -2.45f, 3.1f, 2.9f, 0.8f)
				.uv(129, 63).cuboid(-0.55f, 7.9f, -3.05f, 1.1f, 2f, 0.3f)
				.uv(135, 63).cuboid(-2.15f, 11f, -3.35f, 4.3f, 0.6f, 5.5f)
				.uv(158, 63).cuboid(-1.7f, 10f, -3.65f, 3.4f, 0.65f, 0.6f),
				ModelTransform.of(-2.2f, 10f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_boot = p_body.addChild("left_boot", ModelPartBuilder.create()
				.uv(169, 63).cuboid(-1.6f, 0f, -1.7f, 3.2f, 7.2f, 3.4f)
				.uv(186, 63).cuboid(-1.8f, 6.5f, -1.9f, 3.6f, 0.8f, 3.8f)
				.uv(204, 63).cuboid(-1.8f, 7.2f, -2f, 3.6f, 3.9f, 4f)
				.uv(223, 63).cuboid(-2f, 10f, -3.2f, 4f, 1.4f, 5.2f)
				.uv(245, 63).cuboid(-0.6f, 8f, -2.2f, 1.2f, 2.6f, 0.3f)
				.uv(2, 77).cuboid(-1.5f, 4.9f, -2.2f, 3f, 1.8f, 0.8f)
				.uv(13, 77).cuboid(-1.15f, 5.05f, -2.85f, 2.3f, 1.3f, 0.4f)
				.uv(22, 77).cuboid(-1.55f, 7.6f, -2.45f, 3.1f, 2.9f, 0.8f)
				.uv(33, 77).cuboid(-0.55f, 7.9f, -3.05f, 1.1f, 2f, 0.3f)
				.uv(39, 77).cuboid(-2.15f, 11f, -3.35f, 4.3f, 0.6f, 5.5f)
				.uv(62, 77).cuboid(-1.7f, 10f, -3.65f, 3.4f, 0.65f, 0.6f),
				ModelTransform.of(2.2f, 10f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(227, 21).cuboid(-3.1f, -1.5f, -2f, 3.8f, 5.6f, 4f)
				.uv(2, 39).cuboid(-3f, 3.7f, -1.9f, 3.6f, 3.2f, 3.8f)
				.uv(20, 39).cuboid(-3.3f, 6.3f, -2.2f, 4.2f, 3.3f, 4.4f)
				.uv(41, 39).cuboid(-2.7f, 6.8f, -2.45f, 1.3f, 1.4f, 0.35f)
				.uv(48, 39).cuboid(-3f, 9f, -2.4f, 1f, 1.3f, 1.6f)
				.uv(57, 39).cuboid(-1.8f, 9f, -2.4f, 1f, 1.3f, 1.6f)
				.uv(66, 39).cuboid(-0.6f, 9f, -2.4f, 1f, 1.3f, 1.6f)
				.uv(75, 39).cuboid(-3.65f, -2.8f, -2.55f, 4.9f, 2.3f, 5.1f)
				.uv(98, 39).cuboid(-3.25f, -3.35f, -2.2f, 4.1f, 0.8f, 4.4f)
				.uv(118, 39).cuboid(-3.7f, -1.9f, -2.95f, 5f, 0.7f, 0.65f)
				.uv(133, 39).cuboid(-3.9f, -1.5f, -1.8f, 0.75f, 2.1f, 3.6f)
				.uv(145, 39).cuboid(-2.5f, 3.8f, -2.3f, 2.5f, 1.1f, 0.6f)
				.uv(155, 39).cuboid(-3.05f, 6.6f, -2.75f, 3.7f, 2.2f, 0.75f)
				.uv(167, 39).cuboid(-3.2f, 6.35f, -2.85f, 4f, 0.4f, 0.55f)
				.uv(180, 39).cuboid(-2.55f, 6.9f, -3.2f, 0.35f, 1.2f, 0.2f)
				.uv(185, 39).cuboid(-1.85f, 6.9f, -3.2f, 0.35f, 1.2f, 0.2f)
				.uv(190, 39).cuboid(-1.15f, 6.9f, -3.2f, 0.35f, 1.2f, 0.2f),
				ModelTransform.of(-5f, 3f, 0f, 0f, 0f, 0f));
		ModelPartData p_staff = p_right_arm.addChild("staff", ModelPartBuilder.create(),
				ModelTransform.of(-1f, 10f, -0.5f, -0.25f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(195, 39).cuboid(-0.1f, -1.5f, -2f, 3.8f, 5.6f, 4f)
				.uv(214, 39).cuboid(0f, 3.7f, -1.9f, 3.6f, 3.2f, 3.8f)
				.uv(232, 39).cuboid(-0.3f, 6.3f, -2.2f, 4.2f, 3.3f, 4.4f)
				.uv(2, 52).cuboid(0.3f, 6.8f, -2.45f, 1.3f, 1.4f, 0.35f)
				.uv(9, 52).cuboid(0f, 9f, -2.4f, 1f, 1.3f, 1.6f)
				.uv(18, 52).cuboid(1.2f, 9f, -2.4f, 1f, 1.3f, 1.6f)
				.uv(27, 52).cuboid(2.4f, 9f, -2.4f, 1f, 1.3f, 1.6f)
				.uv(36, 52).cuboid(-0.5f, -2.6f, -2.5f, 4.6f, 2f, 5f)
				.uv(59, 52).cuboid(-0.25f, -3f, -2.2f, 4.1f, 0.5f, 4.4f)
				.uv(79, 52).cuboid(-0.65f, -2.8f, -2.55f, 4.9f, 2.3f, 5.1f)
				.uv(102, 52).cuboid(-0.25f, -3.35f, -2.2f, 4.1f, 0.8f, 4.4f)
				.uv(122, 52).cuboid(-0.7f, -1.9f, -2.95f, 5f, 0.7f, 0.65f)
				.uv(137, 52).cuboid(3.65f, -1.5f, -1.8f, 0.75f, 2.1f, 3.6f)
				.uv(149, 52).cuboid(0.5f, 3.8f, -2.3f, 2.5f, 1.1f, 0.6f)
				.uv(159, 52).cuboid(-0.05f, 6.6f, -2.75f, 3.7f, 2.2f, 0.75f)
				.uv(171, 52).cuboid(-0.2f, 6.35f, -2.85f, 4f, 0.4f, 0.55f)
				.uv(184, 52).cuboid(0.45f, 6.9f, -3.2f, 0.35f, 1.2f, 0.2f)
				.uv(189, 52).cuboid(1.15f, 6.9f, -3.2f, 0.35f, 1.2f, 0.2f)
				.uv(194, 52).cuboid(1.85f, 6.9f, -3.2f, 0.35f, 1.2f, 0.2f),
				ModelTransform.of(5f, 3f, 0f, 0f, 0f, 0f));
		ModelPartData p_lantern = p_left_arm.addChild("lantern", ModelPartBuilder.create(),
				ModelTransform.of(1f, 11f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 256);
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
