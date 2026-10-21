package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.RevEntity;
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

/** Rev on his black sport bike: spinning wheels, engine rumble, tucked riding pose, leaning into a ram. */
@SuppressWarnings("unused")
public class RevModel extends SinglePartEntityModel<RevEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("rev"), "main");
	private final ModelPart root;
	private final ModelPart bike, frontWheel, rearWheel, body, head, rightArm, leftArm, rightLeg, leftLeg;

	public RevModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.bike = root.getChild("bike");
		this.frontWheel = bike.getChild("front_wheel");
		this.rearWheel = bike.getChild("rear_wheel");
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_bike = r.addChild("bike", ModelPartBuilder.create()
				.uv(143, 21).cuboid(-3.5f, 6f, -4f, 7f, 4.5f, 10f)
				.uv(180, 21).cuboid(-3.4f, 6.1f, 5.2f, 6.8f, 1.6f, 9f)
				.uv(215, 21).cuboid(-3.2f, 10.5f, -1f, 6.4f, 5.5f, 9f)
				.uv(249, 21).cuboid(-3.7f, 12f, -7f, 1f, 1f, 20f)
				.uv(294, 21).cuboid(2.7f, 12f, -7f, 1f, 1f, 20f)
				.uv(339, 21).cuboid(-3.5f, 11.5f, 0.3f, 7f, 0.4f, 6.7f)
				.uv(370, 21).cuboid(-3.5f, 13f, 0.3f, 7f, 0.4f, 6.7f)
				.uv(401, 21).cuboid(-3.5f, 14.5f, 0.3f, 7f, 0.4f, 6.7f)
				.uv(432, 21).cuboid(-3.75f, 7f, -2f, 0.4f, 3.5f, 5f)
				.uv(446, 21).cuboid(3.35f, 7f, -2f, 0.4f, 3.5f, 5f)
				.uv(460, 21).cuboid(-2.6f, 5.4f, -13.3f, 2.3f, 2.6f, 2f)
				.uv(472, 21).cuboid(-2.4f, 5.6f, -13.55f, 1.9f, 2.2f, 0.4f)
				.uv(480, 21).cuboid(1f, 5.4f, -13.3f, 2.3f, 2.6f, 2f)
				.uv(492, 21).cuboid(1.2f, 5.6f, -13.55f, 1.9f, 2.2f, 0.4f)
				.uv(2, 45).cuboid(-4.5f, 14.3f, 3f, 1.5f, 1.5f, 14f)
				.uv(36, 45).cuboid(-4.3f, 14.5f, 16.9f, 1.1f, 1.1f, 0.3f)
				.uv(42, 45).cuboid(-4.5f, 16.4f, 3f, 1.5f, 1.5f, 14f)
				.uv(76, 45).cuboid(-4.3f, 16.6f, 16.9f, 1.1f, 1.1f, 0.3f)
				.uv(82, 45).cuboid(3f, 14.3f, 3f, 1.5f, 1.5f, 14f)
				.uv(116, 45).cuboid(3.2f, 14.5f, 16.9f, 1.1f, 1.1f, 0.3f)
				.uv(122, 45).cuboid(3f, 16.4f, 3f, 1.5f, 1.5f, 14f)
				.uv(156, 45).cuboid(3.2f, 16.6f, 16.9f, 1.1f, 1.1f, 0.3f)
				.uv(162, 45).cuboid(-2f, 10f, 15f, 4f, 3f, 7f)
				.uv(187, 45).cuboid(-1.5f, 9f, 21f, 3f, 2f, 6f)
				.uv(208, 45).cuboid(-2.5f, 10f, 26f, 3f, 2f, 5f)
				.uv(227, 45).cuboid(-1f, 9f, 30f, 2f, 1.3f, 5f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_windscreen = p_bike.addChild("windscreen", ModelPartBuilder.create(),
				ModelTransform.of(0f, 8f, -12f, -0.75f, 0f, 0f));
		ModelPartData p_tail = p_bike.addChild("tail", ModelPartBuilder.create(),
				ModelTransform.of(0f, 7.5f, 10f, -0.25f, 0f, 0f));
		ModelPartData p_muffler = p_bike.addChild("muffler", ModelPartBuilder.create(),
				ModelTransform.of(2.5f, 14f, 4f, -0.25f, 0f, 0f));
		ModelPartData p_fork = p_bike.addChild("fork", ModelPartBuilder.create()
				.uv(244, 45).cuboid(-2.8f, -1f, -1f, 1.1f, 12f, 1.1f)
				.uv(252, 45).cuboid(1.7f, -1f, -1f, 1.1f, 12f, 1.1f),
				ModelTransform.of(0f, 7f, -11f, -0.38f, 0f, 0f));
		ModelPartData p_handlebars = p_bike.addChild("handlebars", ModelPartBuilder.create()
				.uv(260, 45).cuboid(-0.5f, -2f, -0.5f, 1f, 3f, 1f)
				.uv(267, 45).cuboid(-6f, -3f, -0.6f, 12f, 1f, 1.2f)
				.uv(297, 45).cuboid(-6f, -3f, -0.8f, 2f, 1.4f, 1.6f)
				.uv(308, 45).cuboid(4f, -3f, -0.8f, 2f, 1.4f, 1.6f),
				ModelTransform.of(0f, 8f, -10f, 0f, 0f, 0f));
		ModelPartData p_front_wheel = p_bike.addChild("front_wheel", ModelPartBuilder.create()
				.uv(319, 45).cuboid(-2f, -1.5f, -1.5f, 4f, 3f, 3f)
				.uv(336, 45).cuboid(-0.6f, -2.5f, -2.5f, 1f, 5f, 5f),
				ModelTransform.of(0f, 18f, -16.5f, 0f, 0f, 0f));
		ModelPartData p_front_wheel_t0 = p_front_wheel.addChild("front_wheel_t0", ModelPartBuilder.create()
				.uv(351, 45).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(368, 45).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_front_wheel_t1 = p_front_wheel.addChild("front_wheel_t1", ModelPartBuilder.create()
				.uv(381, 45).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(398, 45).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 0.45f, 0f, 0f));
		ModelPartData p_front_wheel_t2 = p_front_wheel.addChild("front_wheel_t2", ModelPartBuilder.create()
				.uv(411, 45).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(428, 45).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 0.9f, 0f, 0f));
		ModelPartData p_front_wheel_t3 = p_front_wheel.addChild("front_wheel_t3", ModelPartBuilder.create()
				.uv(441, 45).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(458, 45).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 1.35f, 0f, 0f));
		ModelPartData p_front_wheel_t4 = p_front_wheel.addChild("front_wheel_t4", ModelPartBuilder.create()
				.uv(471, 45).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(488, 45).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 1.8f, 0f, 0f));
		ModelPartData p_front_wheel_t5 = p_front_wheel.addChild("front_wheel_t5", ModelPartBuilder.create()
				.uv(2, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(19, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 2.24f, 0f, 0f));
		ModelPartData p_front_wheel_t6 = p_front_wheel.addChild("front_wheel_t6", ModelPartBuilder.create()
				.uv(32, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(49, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 2.69f, 0f, 0f));
		ModelPartData p_front_wheel_t7 = p_front_wheel.addChild("front_wheel_t7", ModelPartBuilder.create()
				.uv(62, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(79, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 3.14f, 0f, 0f));
		ModelPartData p_front_wheel_t8 = p_front_wheel.addChild("front_wheel_t8", ModelPartBuilder.create()
				.uv(92, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(109, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 3.59f, 0f, 0f));
		ModelPartData p_front_wheel_t9 = p_front_wheel.addChild("front_wheel_t9", ModelPartBuilder.create()
				.uv(122, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(139, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 4.04f, 0f, 0f));
		ModelPartData p_front_wheel_t10 = p_front_wheel.addChild("front_wheel_t10", ModelPartBuilder.create()
				.uv(152, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(169, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 4.49f, 0f, 0f));
		ModelPartData p_front_wheel_t11 = p_front_wheel.addChild("front_wheel_t11", ModelPartBuilder.create()
				.uv(182, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(199, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 4.94f, 0f, 0f));
		ModelPartData p_front_wheel_t12 = p_front_wheel.addChild("front_wheel_t12", ModelPartBuilder.create()
				.uv(212, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(229, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 5.39f, 0f, 0f));
		ModelPartData p_front_wheel_t13 = p_front_wheel.addChild("front_wheel_t13", ModelPartBuilder.create()
				.uv(242, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(259, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 5.83f, 0f, 0f));
		ModelPartData p_front_wheel_s0 = p_front_wheel.addChild("front_wheel_s0", ModelPartBuilder.create()
				.uv(272, 64).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_front_wheel_s1 = p_front_wheel.addChild("front_wheel_s1", ModelPartBuilder.create()
				.uv(279, 64).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f),
				ModelTransform.of(0f, 0f, 0f, 0.79f, 0f, 0f));
		ModelPartData p_front_wheel_s2 = p_front_wheel.addChild("front_wheel_s2", ModelPartBuilder.create()
				.uv(286, 64).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f),
				ModelTransform.of(0f, 0f, 0f, 1.57f, 0f, 0f));
		ModelPartData p_front_wheel_s3 = p_front_wheel.addChild("front_wheel_s3", ModelPartBuilder.create()
				.uv(293, 64).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f),
				ModelTransform.of(0f, 0f, 0f, 2.36f, 0f, 0f));
		ModelPartData p_rear_wheel = p_bike.addChild("rear_wheel", ModelPartBuilder.create()
				.uv(300, 64).cuboid(-2f, -1.5f, -1.5f, 4f, 3f, 3f)
				.uv(317, 64).cuboid(-0.6f, -2.5f, -2.5f, 1f, 5f, 5f),
				ModelTransform.of(0f, 18f, 13f, 0f, 0f, 0f));
		ModelPartData p_rear_wheel_t0 = p_rear_wheel.addChild("rear_wheel_t0", ModelPartBuilder.create()
				.uv(332, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(349, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_rear_wheel_t1 = p_rear_wheel.addChild("rear_wheel_t1", ModelPartBuilder.create()
				.uv(362, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(379, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 0.45f, 0f, 0f));
		ModelPartData p_rear_wheel_t2 = p_rear_wheel.addChild("rear_wheel_t2", ModelPartBuilder.create()
				.uv(392, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(409, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 0.9f, 0f, 0f));
		ModelPartData p_rear_wheel_t3 = p_rear_wheel.addChild("rear_wheel_t3", ModelPartBuilder.create()
				.uv(422, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(439, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 1.35f, 0f, 0f));
		ModelPartData p_rear_wheel_t4 = p_rear_wheel.addChild("rear_wheel_t4", ModelPartBuilder.create()
				.uv(452, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(469, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 1.8f, 0f, 0f));
		ModelPartData p_rear_wheel_t5 = p_rear_wheel.addChild("rear_wheel_t5", ModelPartBuilder.create()
				.uv(482, 64).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(499, 64).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 2.24f, 0f, 0f));
		ModelPartData p_rear_wheel_t6 = p_rear_wheel.addChild("rear_wheel_t6", ModelPartBuilder.create()
				.uv(2, 77).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(19, 77).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 2.69f, 0f, 0f));
		ModelPartData p_rear_wheel_t7 = p_rear_wheel.addChild("rear_wheel_t7", ModelPartBuilder.create()
				.uv(32, 77).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(49, 77).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 3.14f, 0f, 0f));
		ModelPartData p_rear_wheel_t8 = p_rear_wheel.addChild("rear_wheel_t8", ModelPartBuilder.create()
				.uv(62, 77).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(79, 77).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 3.59f, 0f, 0f));
		ModelPartData p_rear_wheel_t9 = p_rear_wheel.addChild("rear_wheel_t9", ModelPartBuilder.create()
				.uv(92, 77).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(109, 77).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 4.04f, 0f, 0f));
		ModelPartData p_rear_wheel_t10 = p_rear_wheel.addChild("rear_wheel_t10", ModelPartBuilder.create()
				.uv(122, 77).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(139, 77).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 4.49f, 0f, 0f));
		ModelPartData p_rear_wheel_t11 = p_rear_wheel.addChild("rear_wheel_t11", ModelPartBuilder.create()
				.uv(152, 77).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(169, 77).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 4.94f, 0f, 0f));
		ModelPartData p_rear_wheel_t12 = p_rear_wheel.addChild("rear_wheel_t12", ModelPartBuilder.create()
				.uv(182, 77).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(199, 77).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 5.39f, 0f, 0f));
		ModelPartData p_rear_wheel_t13 = p_rear_wheel.addChild("rear_wheel_t13", ModelPartBuilder.create()
				.uv(212, 77).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(229, 77).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f),
				ModelTransform.of(0f, 0f, 0f, 5.83f, 0f, 0f));
		ModelPartData p_rear_wheel_s0 = p_rear_wheel.addChild("rear_wheel_s0", ModelPartBuilder.create()
				.uv(242, 77).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_rear_wheel_s1 = p_rear_wheel.addChild("rear_wheel_s1", ModelPartBuilder.create()
				.uv(249, 77).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f),
				ModelTransform.of(0f, 0f, 0f, 0.79f, 0f, 0f));
		ModelPartData p_rear_wheel_s2 = p_rear_wheel.addChild("rear_wheel_s2", ModelPartBuilder.create()
				.uv(256, 77).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f),
				ModelTransform.of(0f, 0f, 0f, 1.57f, 0f, 0f));
		ModelPartData p_rear_wheel_s3 = p_rear_wheel.addChild("rear_wheel_s3", ModelPartBuilder.create()
				.uv(263, 77).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f),
				ModelTransform.of(0f, 0f, 0f, 2.36f, 0f, 0f));
		ModelPartData p_ffender0 = p_bike.addChild("ffender0", ModelPartBuilder.create()
				.uv(270, 77).cuboid(-2f, -7.5f, -1.6f, 4f, 1f, 3f),
				ModelTransform.of(0f, 18f, -16.5f, -0.7f, 0f, 0f));
		ModelPartData p_ffender1 = p_bike.addChild("ffender1", ModelPartBuilder.create()
				.uv(287, 77).cuboid(-2f, -7.5f, -1.6f, 4f, 1f, 3f),
				ModelTransform.of(0f, 18f, -16.5f, -0.3f, 0f, 0f));
		ModelPartData p_ffender2 = p_bike.addChild("ffender2", ModelPartBuilder.create()
				.uv(304, 77).cuboid(-2f, -7.5f, -1.6f, 4f, 1f, 3f),
				ModelTransform.of(0f, 18f, -16.5f, 0.1f, 0f, 0f));
		ModelPartData p_ffender3 = p_bike.addChild("ffender3", ModelPartBuilder.create()
				.uv(321, 77).cuboid(-2f, -7.5f, -1.6f, 4f, 1f, 3f),
				ModelTransform.of(0f, 18f, -16.5f, 0.5f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-3.9f, -12.5f, -2f, 7.8f, 8f, 4f)
				.uv(29, 2).cuboid(-4.2f, -12.7f, -2.6f, 2.3f, 9.4f, 5.1f)
				.uv(47, 2).cuboid(-4.2f, -3.5f, -2.5f, 2.3f, 5.5f, 5f)
				.uv(65, 2).cuboid(1.9f, -12.7f, -2.6f, 2.3f, 9.4f, 5.1f)
				.uv(83, 2).cuboid(1.9f, -3.5f, -2.5f, 2.3f, 5.5f, 5f)
				.uv(101, 2).cuboid(-4f, -12.3f, 1.7f, 8f, 10f, 0.9f)
				.uv(122, 2).cuboid(-4.1f, -4.5f, -2.45f, 8.2f, 1.2f, 4.9f)
				.uv(152, 2).cuboid(-1f, -4.6f, -2.75f, 2f, 1.5f, 0.5f),
				ModelTransform.of(0f, 9f, 3.5f, 0.75f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(160, 2).cuboid(-4f, -8f, -3.7f, 8f, 8f, 7.4f),
				ModelTransform.of(0f, -12.5f, 0f, -0.85f, 0f, 0f));
		ModelPartData p_hair = p_head.addChild("hair", ModelPartBuilder.create()
				.uv(194, 2).cuboid(-4.25f, -9f, -4f, 8.5f, 2f, 8f)
				.uv(230, 2).cuboid(-4.25f, -7f, 2.5f, 8.5f, 8.2f, 1.5f)
				.uv(253, 2).cuboid(-4.3f, -7f, -2.6f, 1.2f, 7.4f, 6f)
				.uv(271, 2).cuboid(3.1f, -7f, -2.6f, 1.2f, 7.4f, 6f)
				.uv(289, 2).cuboid(-3.4f, -7.7f, -4.15f, 1.6f, 2.3f, 0.7f)
				.uv(297, 2).cuboid(-1.7f, -7.7f, -4.15f, 1.6f, 1.3f, 0.7f)
				.uv(305, 2).cuboid(0.2f, -7.7f, -4.15f, 1.6f, 2f, 0.7f)
				.uv(313, 2).cuboid(2f, -7.7f, -4.15f, 1.6f, 1.3f, 0.7f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(321, 2).cuboid(-2f, -1f, -2f, 3f, 6.2f, 4f)
				.uv(338, 2).cuboid(-1.95f, 5f, -1.9f, 2.9f, 3.2f, 3.8f)
				.uv(355, 2).cuboid(-1.8f, 8.1f, -1.6f, 2.6f, 1.4f, 3.2f)
				.uv(370, 2).cuboid(-1.95f, 9.3f, -1.8f, 2.9f, 2.5f, 3.6f)
				.uv(386, 2).cuboid(-1.8f, 11.6f, -1.6f, 2.6f, 0.7f, 3.2f)
				.uv(401, 2).cuboid(-0.95f, 10f, -1.94f, 0.7f, 0.7f, 0.25f),
				ModelTransform.of(-5f, 1.2f, -3.6f, -0.75f, 0.15f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(479, 2).cuboid(-1f, -1f, -2f, 3f, 6.2f, 4f)
				.uv(2, 21).cuboid(-0.95f, 5f, -1.9f, 2.9f, 3.2f, 3.8f)
				.uv(19, 21).cuboid(-0.8f, 8.1f, -1.6f, 2.6f, 1.4f, 3.2f)
				.uv(34, 21).cuboid(-0.95f, 9.3f, -1.8f, 2.9f, 2.5f, 3.6f)
				.uv(50, 21).cuboid(-0.8f, 11.6f, -1.6f, 2.6f, 0.7f, 3.2f)
				.uv(65, 21).cuboid(0.05f, 10f, -1.94f, 0.7f, 0.7f, 0.25f),
				ModelTransform.of(5f, 1.2f, -3.6f, -0.75f, -0.15f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(406, 2).cuboid(-1.8f, 0f, -1.9f, 3.6f, 5.5f, 3.8f),
				ModelTransform.of(-2.4f, 9f, 4f, -1f, 0.25f, 0f));
		ModelPartData p_right_shin = p_right_leg.addChild("right_shin", ModelPartBuilder.create()
				.uv(424, 2).cuboid(-1.7f, 0f, -1.9f, 3.4f, 5.8f, 3.8f)
				.uv(442, 2).cuboid(-1.9f, 4.5f, -2.7f, 3.8f, 1.6f, 4.9f)
				.uv(463, 2).cuboid(-0.9f, 1f, -2.02f, 1.8f, 0.7f, 0.22f)
				.uv(471, 2).cuboid(-0.9f, 3.4f, -2.02f, 1.8f, 0.7f, 0.22f),
				ModelTransform.of(0f, 5.5f, 0f, 1.9f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(70, 21).cuboid(-1.8f, 0f, -1.9f, 3.6f, 5.5f, 3.8f),
				ModelTransform.of(2.4f, 9f, 4f, -1f, -0.25f, 0f));
		ModelPartData p_left_shin = p_left_leg.addChild("left_shin", ModelPartBuilder.create()
				.uv(88, 21).cuboid(-1.7f, 0f, -1.9f, 3.4f, 5.8f, 3.8f)
				.uv(106, 21).cuboid(-1.9f, 4.5f, -2.7f, 3.8f, 1.6f, 4.9f)
				.uv(127, 21).cuboid(-0.9f, 1f, -2.02f, 1.8f, 0.7f, 0.22f)
				.uv(135, 21).cuboid(-0.9f, 3.4f, -2.02f, 1.8f, 0.7f, 0.22f),
				ModelTransform.of(0f, 5.5f, 0f, 1.9f, 0f, 0f));
		return TexturedModelData.of(data, 512, 512);
	}

	/** Draw just the bike (for the rideable ghost bike): hide Rev, roll the wheels, rumble the engine. */
	public void poseBikeOnly(float wheelRoll, float age, boolean moving) {
		body.visible = false;
		rightArm.visible = false;
		leftArm.visible = false;
		rightLeg.visible = false;
		leftLeg.visible = false;
		frontWheel.pitch = -wheelRoll;
		rearWheel.pitch = -wheelRoll;
		bike.pivotY = MathHelper.sin(age * 2.3f) * (moving ? 0.1f : 0.05f);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(RevEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		body.visible = rightArm.visible = leftArm.visible = rightLeg.visible = leftLeg.visible = true;
		float charge = e.charge();
		// wheels roll with movement; when revving the back wheel spins in place
		float roll = limbAngle * 1.6f + (charge > 0 ? age * 1.4f : 0);
		frontWheel.pitch = -roll;
		rearWheel.pitch = -roll - charge * age * 0.6f;
		// engine rumble
		float rumble = MathHelper.sin(age * 2.3f) * (0.12f + charge * 0.15f);
		bike.pivotY = rumble;
		// tucked low over the bars, ducking lower for a ram
		body.pitch = 0.75f + charge * 0.2f;
		body.pivotY = 9 + rumble;
		head.pitch = -0.85f - charge * 0.2f + headPitch * MathHelper.RADIANS_PER_DEGREE * 0.4f;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.6f;
		float swing = MathHelper.sin(e.handSwingProgress * MathHelper.PI);
		rightArm.pitch = -0.75f - swing * 1.2f;
		rightArm.yaw = 0.15f - swing * 0.4f;
		rightArm.pivotY = 1.2f + rumble;
		leftArm.pitch = -0.75f;
		leftArm.yaw = -0.15f;
		leftArm.pivotY = 1.2f + rumble;
		rightLeg.pivotY = 9 + rumble;
		leftLeg.pivotY = 9 + rumble;
	}
}
