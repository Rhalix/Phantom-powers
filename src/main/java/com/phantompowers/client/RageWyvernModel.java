package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.RageWyvernEntity;
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

/** The Rage Wyvern: beating wings, a snaking neck and tail, jaws open to breathe fire, the amulet glowing on its chest. */
@SuppressWarnings("unused")
public class RageWyvernModel extends SinglePartEntityModel<RageWyvernEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("rage_wyvern"), "main");
	private final ModelPart root, body, neck, head, jaw, leftWing, leftTip, rightWing, rightTip, tail, tail2, tail3, rightLeg, leftLeg, rightFoot, leftFoot;

	public RageWyvernModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.body = root.getChild("body");
		this.neck = body.getChild("neck");
		this.head = neck.getChild("head");
		this.jaw = head.getChild("jaw");
		this.leftWing = body.getChild("left_wing");
		this.leftTip = leftWing.getChild("left_wing_tip");
		this.rightWing = body.getChild("right_wing");
		this.rightTip = rightWing.getChild("right_wing_tip");
		this.tail = body.getChild("tail");
		this.tail2 = tail.getChild("tail2");
		this.tail3 = tail2.getChild("tail3");
		this.rightLeg = body.getChild("right_leg");
		this.leftLeg = body.getChild("left_leg");
		this.rightFoot = rightLeg.getChild("right_foot");
		this.leftFoot = leftLeg.getChild("left_foot");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-5f, -5f, -9f, 10f, 10f, 18f)
				.uv(61, 2).cuboid(-4f, 2.7f, -8f, 8f, 2.8f, 15f),
				ModelTransform.of(0f, 8f, 0f, 0f, 0f, 0f));
		ModelPartData p_spines = p_body.addChild("spines", ModelPartBuilder.create()
				.uv(110, 2).cuboid(-0.6f, -2.4f, -7f, 1.2f, 2.8f, 2.3f)
				.uv(120, 2).cuboid(-0.6f, -2.4f, -3.9f, 1.2f, 2.8f, 2.3f)
				.uv(130, 2).cuboid(-0.6f, -2.4f, -0.8f, 1.2f, 2.8f, 2.3f)
				.uv(140, 2).cuboid(-0.6f, -2.4f, 2.3f, 1.2f, 2.8f, 2.3f)
				.uv(150, 2).cuboid(-0.6f, -2.4f, 5.4f, 1.2f, 2.8f, 2.3f),
				ModelTransform.of(0f, -5f, 0f, 0f, 0f, 0f));
		ModelPartData p_amulet = p_body.addChild("amulet", ModelPartBuilder.create()
				.uv(160, 2).cuboid(-1.6f, 0f, -1.2f, 3.2f, 3.2f, 0.8f)
				.uv(171, 2).cuboid(-0.85f, 0.7f, -1.5f, 1.7f, 1.7f, 0.5f),
				ModelTransform.of(0f, -1f, -9f, 0f, 0f, 0f));
		ModelPartData p_neck = p_body.addChild("neck", ModelPartBuilder.create()
				.uv(179, 2).cuboid(-3f, -16f, -7f, 6f, 18f, 6f)
				.uv(206, 2).cuboid(-2.3f, -15f, -7.35f, 4.6f, 17f, 0.5f)
				.uv(220, 2).cuboid(-0.7f, -16f, -0.3f, 1.4f, 2.3f, 2.3f)
				.uv(231, 2).cuboid(-0.7f, -13.5f, -0.3f, 1.4f, 2.3f, 2.3f)
				.uv(242, 2).cuboid(-0.7f, -11f, -0.3f, 1.4f, 2.3f, 2.3f)
				.uv(253, 2).cuboid(-0.7f, -8.5f, -0.3f, 1.4f, 2.3f, 2.3f)
				.uv(264, 2).cuboid(-0.7f, -6f, -0.3f, 1.4f, 2.3f, 2.3f)
				.uv(275, 2).cuboid(-0.7f, -3.5f, -0.3f, 1.4f, 2.3f, 2.3f)
				.uv(286, 2).cuboid(-0.7f, -1f, -0.3f, 1.4f, 2.3f, 2.3f),
				ModelTransform.of(0f, -2f, -8f, -0.6f, 0f, 0f));
		ModelPartData p_head = p_neck.addChild("head", ModelPartBuilder.create()
				.uv(297, 2).cuboid(-4f, -5f, -7f, 8f, 8f, 7f)
				.uv(330, 2).cuboid(-3.5f, -1.5f, -13f, 7f, 3.7f, 6.5f)
				.uv(360, 2).cuboid(-3.6f, 1.4f, -12f, 0.5f, 1f, 0.5f)
				.uv(365, 2).cuboid(-3.6f, 1.4f, -11.1f, 0.5f, 1f, 0.5f)
				.uv(370, 2).cuboid(-3.6f, 1.4f, -10.2f, 0.5f, 1f, 0.5f)
				.uv(375, 2).cuboid(-3.6f, 1.4f, -9.3f, 0.5f, 1f, 0.5f)
				.uv(380, 2).cuboid(-3.6f, 1.4f, -8.4f, 0.5f, 1f, 0.5f)
				.uv(385, 2).cuboid(-3.6f, 1.4f, -7.5f, 0.5f, 1f, 0.5f)
				.uv(390, 2).cuboid(-4.35f, -2f, -1.4f, 0.7f, 2f, 1f)
				.uv(397, 2).cuboid(-4.4f, -1.5f, -1.5f, 0.8f, 0.8f, 1.2f)
				.uv(404, 2).cuboid(3.2f, 1.4f, -12f, 0.5f, 1f, 0.5f)
				.uv(409, 2).cuboid(3.2f, 1.4f, -11.1f, 0.5f, 1f, 0.5f)
				.uv(414, 2).cuboid(3.2f, 1.4f, -10.2f, 0.5f, 1f, 0.5f)
				.uv(419, 2).cuboid(3.2f, 1.4f, -9.3f, 0.5f, 1f, 0.5f)
				.uv(424, 2).cuboid(3.2f, 1.4f, -8.4f, 0.5f, 1f, 0.5f)
				.uv(429, 2).cuboid(3.2f, 1.4f, -7.5f, 0.5f, 1f, 0.5f)
				.uv(434, 2).cuboid(3.65f, -2f, -1.4f, 0.7f, 2f, 1f)
				.uv(441, 2).cuboid(3.6f, -1.5f, -1.5f, 0.8f, 0.8f, 1.2f),
				ModelTransform.of(0f, -14f, -6f, 0.55f, 0f, 0f));
		ModelPartData p_jaw = p_head.addChild("jaw", ModelPartBuilder.create()
				.uv(448, 2).cuboid(-3f, -0.1f, -6f, 6f, 1.8f, 6.4f)
				.uv(476, 2).cuboid(-2.9f, -0.7f, -5.5f, 0.5f, 0.8f, 0.5f)
				.uv(481, 2).cuboid(-2.9f, -0.7f, -4.6f, 0.5f, 0.8f, 0.5f)
				.uv(486, 2).cuboid(-2.9f, -0.7f, -3.7f, 0.5f, 0.8f, 0.5f)
				.uv(491, 2).cuboid(-2.9f, -0.7f, -2.8f, 0.5f, 0.8f, 0.5f)
				.uv(496, 2).cuboid(-2.9f, -0.7f, -1.9f, 0.5f, 0.8f, 0.5f)
				.uv(501, 2).cuboid(-2.9f, -0.7f, -1f, 0.5f, 0.8f, 0.5f)
				.uv(506, 2).cuboid(2.5f, -0.7f, -5.5f, 0.5f, 0.8f, 0.5f)
				.uv(2, 33).cuboid(2.5f, -0.7f, -4.6f, 0.5f, 0.8f, 0.5f)
				.uv(7, 33).cuboid(2.5f, -0.7f, -3.7f, 0.5f, 0.8f, 0.5f)
				.uv(12, 33).cuboid(2.5f, -0.7f, -2.8f, 0.5f, 0.8f, 0.5f)
				.uv(17, 33).cuboid(2.5f, -0.7f, -1.9f, 0.5f, 0.8f, 0.5f)
				.uv(22, 33).cuboid(2.5f, -0.7f, -1f, 0.5f, 0.8f, 0.5f),
				ModelTransform.of(0f, 2f, -7f, 0.15f, 0f, 0f));
		ModelPartData p_horn_l = p_head.addChild("horn_l", ModelPartBuilder.create()
				.uv(27, 33).cuboid(-0.9f, -4f, -0.9f, 1.8f, 4f, 1.8f)
				.uv(38, 33).cuboid(-0.7f, -6.3f, -0.4f, 1.4f, 2.6f, 1.4f)
				.uv(47, 33).cuboid(-0.4f, -7.7f, 0.1f, 0.8f, 1.6f, 0.8f),
				ModelTransform.of(2.5f, -3.5f, -2f, 0.7f, 0.25f, 0f));
		ModelPartData p_horn_r = p_head.addChild("horn_r", ModelPartBuilder.create()
				.uv(54, 33).cuboid(-0.9f, -4f, -0.9f, 1.8f, 4f, 1.8f)
				.uv(65, 33).cuboid(-0.7f, -6.3f, -0.4f, 1.4f, 2.6f, 1.4f)
				.uv(74, 33).cuboid(-0.4f, -7.7f, 0.1f, 0.8f, 1.6f, 0.8f),
				ModelTransform.of(-2.5f, -3.5f, -2f, 0.7f, -0.25f, 0f));
		ModelPartData p_left_wing = p_body.addChild("left_wing", ModelPartBuilder.create()
				.uv(81, 33).cuboid(0f, 0f, -0.1f, 2.275f, 0.65f, 1f)
				.uv(91, 33).cuboid(0f, 0f, 0.5f, 2.275f, 10f, 0.55f)
				.uv(100, 33).cuboid(2.275f, -1.3f, -0.1f, 2.275f, 0.65f, 1f)
				.uv(110, 33).cuboid(2.275f, -1.3f, 0.5f, 2.275f, 8.2f, 0.55f)
				.uv(119, 33).cuboid(4.55f, -2.6f, -0.1f, 2.275f, 0.65f, 1f)
				.uv(129, 33).cuboid(4.55f, -2.6f, 0.5f, 2.275f, 6.4f, 0.55f)
				.uv(138, 33).cuboid(6.825f, -3.9f, -0.1f, 2.275f, 0.65f, 1f)
				.uv(148, 33).cuboid(6.825f, -3.9f, 0.5f, 2.275f, 4.6f, 0.55f),
				ModelTransform.of(5f, -4f, -4f, 0f, 0f, 0f));
		ModelPartData p_left_wing_tip = p_left_wing.addChild("left_wing_tip", ModelPartBuilder.create()
				.uv(157, 33).cuboid(0f, 0f, -0.1f, 1.95f, 0.65f, 1f)
				.uv(166, 33).cuboid(0f, 0f, 0.5f, 1.95f, 10f, 0.55f)
				.uv(174, 33).cuboid(1.95f, -1.3f, -0.1f, 1.95f, 0.65f, 1f)
				.uv(183, 33).cuboid(1.95f, -1.3f, 0.5f, 1.95f, 8.2f, 0.55f)
				.uv(191, 33).cuboid(3.9f, -2.6f, -0.1f, 1.95f, 0.65f, 1f)
				.uv(200, 33).cuboid(3.9f, -2.6f, 0.5f, 1.95f, 6.4f, 0.55f)
				.uv(208, 33).cuboid(5.85f, -3.9f, -0.1f, 1.95f, 0.65f, 1f)
				.uv(217, 33).cuboid(5.85f, -3.9f, 0.5f, 1.95f, 4.6f, 0.55f),
				ModelTransform.of(9.1f, -4.7f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_wing = p_body.addChild("right_wing", ModelPartBuilder.create()
				.uv(225, 33).cuboid(-9.1f, 0f, -0.1f, 2.275f, 0.65f, 1f)
				.uv(235, 33).cuboid(-9.1f, 0f, 0.5f, 2.275f, 10f, 0.55f)
				.uv(244, 33).cuboid(-6.825f, -1.3f, -0.1f, 2.275f, 0.65f, 1f)
				.uv(254, 33).cuboid(-6.825f, -1.3f, 0.5f, 2.275f, 8.2f, 0.55f)
				.uv(263, 33).cuboid(-4.55f, -2.6f, -0.1f, 2.275f, 0.65f, 1f)
				.uv(273, 33).cuboid(-4.55f, -2.6f, 0.5f, 2.275f, 6.4f, 0.55f)
				.uv(282, 33).cuboid(-2.275f, -3.9f, -0.1f, 2.275f, 0.65f, 1f)
				.uv(292, 33).cuboid(-2.275f, -3.9f, 0.5f, 2.275f, 4.6f, 0.55f),
				ModelTransform.of(-5f, -4f, -4f, 0f, 0f, 0f));
		ModelPartData p_right_wing_tip = p_right_wing.addChild("right_wing_tip", ModelPartBuilder.create()
				.uv(301, 33).cuboid(-7.8f, 0f, -0.1f, 1.95f, 0.65f, 1f)
				.uv(310, 33).cuboid(-7.8f, 0f, 0.5f, 1.95f, 10f, 0.55f)
				.uv(318, 33).cuboid(-5.85f, -1.3f, -0.1f, 1.95f, 0.65f, 1f)
				.uv(327, 33).cuboid(-5.85f, -1.3f, 0.5f, 1.95f, 8.2f, 0.55f)
				.uv(335, 33).cuboid(-3.9f, -2.6f, -0.1f, 1.95f, 0.65f, 1f)
				.uv(344, 33).cuboid(-3.9f, -2.6f, 0.5f, 1.95f, 6.4f, 0.55f)
				.uv(352, 33).cuboid(-1.95f, -3.9f, -0.1f, 1.95f, 0.65f, 1f)
				.uv(361, 33).cuboid(-1.95f, -3.9f, 0.5f, 1.95f, 4.6f, 0.55f),
				ModelTransform.of(-9.1f, -4.7f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail = p_body.addChild("tail", ModelPartBuilder.create()
				.uv(369, 33).cuboid(-3f, -2.5f, 0f, 6f, 5f, 10f)
				.uv(404, 33).cuboid(-0.55f, -4.3f, 3f, 1.1f, 2f, 2f),
				ModelTransform.of(0f, -2f, 9f, -0.1f, 0f, 0f));
		ModelPartData p_tail2 = p_tail.addChild("tail2", ModelPartBuilder.create()
				.uv(414, 33).cuboid(-2f, -2f, 0f, 4f, 4f, 10f)
				.uv(445, 33).cuboid(-0.55f, -3.8f, 3f, 1.1f, 2f, 2f),
				ModelTransform.of(0f, 0.5f, 10f, 0.1f, 0f, 0f));
		ModelPartData p_tail3 = p_tail2.addChild("tail3", ModelPartBuilder.create()
				.uv(455, 33).cuboid(-1f, -1.5f, 0f, 2f, 3f, 10f)
				.uv(482, 33).cuboid(-0.55f, -3.3f, 3f, 1.1f, 2f, 2f)
				.uv(2, 51).cuboid(-3f, 0f, 7f, 6f, 0.7f, 5f),
				ModelTransform.of(0f, 0.5f, 10f, 0.1f, 0f, 0f));
		ModelPartData p_right_leg = p_body.addChild("right_leg", ModelPartBuilder.create()
				.uv(27, 51).cuboid(-2f, 0f, -2.5f, 4f, 7f, 5f),
				ModelTransform.of(-4f, 3f, 4f, 0f, 0f, 0f));
		ModelPartData p_right_foot = p_right_leg.addChild("right_foot", ModelPartBuilder.create()
				.uv(48, 51).cuboid(-2.5f, 0f, -4.5f, 5f, 3f, 6f)
				.uv(73, 51).cuboid(-2.2f, 1f, -5f, 1.2f, 2f, 1.5f)
				.uv(82, 51).cuboid(-0.7f, 1f, -5f, 1.2f, 2f, 1.5f)
				.uv(91, 51).cuboid(0.8f, 1f, -5f, 1.2f, 2f, 1.5f),
				ModelTransform.of(0f, 7f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = p_body.addChild("left_leg", ModelPartBuilder.create()
				.uv(100, 51).cuboid(-2f, 0f, -2.5f, 4f, 7f, 5f),
				ModelTransform.of(4f, 3f, 4f, 0f, 0f, 0f));
		ModelPartData p_left_foot = p_left_leg.addChild("left_foot", ModelPartBuilder.create()
				.uv(121, 51).cuboid(-2.5f, 0f, -4.5f, 5f, 3f, 6f)
				.uv(146, 51).cuboid(-2.2f, 1f, -5f, 1.2f, 2f, 1.5f)
				.uv(155, 51).cuboid(-0.7f, 1f, -5f, 1.2f, 2f, 1.5f)
				.uv(164, 51).cuboid(0.8f, 1f, -5f, 1.2f, 2f, 1.5f),
				ModelTransform.of(0f, 7f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_front_leg = p_body.addChild("right_front_leg", ModelPartBuilder.create()
				.uv(173, 51).cuboid(-1.6f, 0f, -1.6f, 3.2f, 8f, 3.2f)
				.uv(189, 51).cuboid(-2f, 7f, -3f, 4f, 2f, 5f),
				ModelTransform.of(-4f, 3f, -6f, 0f, 0f, 0f));
		ModelPartData p_left_front_leg = p_body.addChild("left_front_leg", ModelPartBuilder.create()
				.uv(210, 51).cuboid(-1.6f, 0f, -1.6f, 3.2f, 8f, 3.2f)
				.uv(226, 51).cuboid(-2f, 7f, -3f, 4f, 2f, 5f),
				ModelTransform.of(4f, 3f, -6f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 512, 512);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(RageWyvernEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		int pose = e.pose();
		boolean dazed = e.dazed();
		boolean diving = pose > 300;
		float rate = e.berserk() ? 0.42f : 0.3f;
		float flap = MathHelper.sin(age * rate);
		float bob = -flap * 1.2f;
		body.pivotY = 8f + bob;
		body.pitch = diving ? 0.45f : -0.05f + limbDistance * 0.15f;
		body.roll = 0;
		body.yaw = 0;
		// wings: big beats, the tips lagging behind; swept back in a dive, drooping when dazed
		float beat = diving ? -0.25f : dazed ? 0.55f + MathHelper.sin(age * 0.1f) * 0.05f : flap * 0.75f;
		leftWing.roll = beat;
		rightWing.roll = -beat;
		float tipLag = diving ? 0.1f : dazed ? 0.4f : MathHelper.sin(age * rate - 0.9f) * 0.55f;
		leftTip.roll = tipLag;
		rightTip.roll = -tipLag;
		leftWing.yaw = diving ? -0.7f : 0f;
		rightWing.yaw = diving ? 0.7f : 0f;
		// neck and head follow where it looks
		float hy = headYaw * MathHelper.RADIANS_PER_DEGREE, hp = headPitch * MathHelper.RADIANS_PER_DEGREE;
		neck.yaw = hy * 0.5f;
		neck.pitch = -0.6f + hp * 0.3f + MathHelper.sin(age * 0.08f) * 0.05f;
		head.yaw = hy * 0.5f;
		head.pitch = 0.55f + hp * 0.5f;
		head.roll = 0;
		jaw.pitch = 0.15f + MathHelper.sin(age * 0.07f) * 0.05f;
		// tail snakes
		tail.yaw = MathHelper.sin(age * 0.12f) * 0.2f;
		tail2.yaw = MathHelper.sin(age * 0.12f - 0.8f) * 0.25f;
		tail3.yaw = MathHelper.sin(age * 0.12f - 1.6f) * 0.3f;
		tail.pitch = -0.1f + (diving ? -0.3f : 0f);
		tail2.pitch = 0.1f;
		tail3.pitch = 0.1f;
		// legs tucked up while flying
		rightLeg.pitch = 0.9f;
		leftLeg.pitch = 0.9f;
		rightFoot.pitch = 0.6f;
		leftFoot.pitch = 0.6f;
		if (dazed) {
			neck.pitch = 0.2f;
			head.pitch = 0.5f;
			head.roll = MathHelper.sin(age * 0.2f) * 0.3f;
			jaw.pitch = 0.5f;
			rightLeg.pitch = 0.2f;
			leftLeg.pitch = 0.2f;
			tail.pitch = 0.25f;
		} else if (pose > 0 && pose <= 40) {
			// breathing fire: neck thrust forward, jaws wide
			neck.pitch = -0.25f + hp * 0.4f;
			head.pitch = 0.2f + hp * 0.4f;
			jaw.pitch = 0.9f;
		} else if (pose > 100 && pose <= 112) {
			// tail swipe: the whole body twists
			float t = 1f - (pose - 100) / 12f;
			float s = MathHelper.sin(t * MathHelper.PI);
			body.yaw = s * 0.9f;
			tail.yaw = -s * 0.7f;
			tail2.yaw = -s * 0.7f;
			tail3.yaw = -s * 0.6f;
		} else if (pose > 200 && pose <= 230) {
			// roar: head thrown back, jaws open, wings spread wide
			neck.pitch = -1.1f;
			head.pitch = 0.1f;
			jaw.pitch = 1.0f;
			head.roll = MathHelper.sin(age * 2f) * 0.05f;
			leftWing.roll = -0.2f;
			rightWing.roll = 0.2f;
			leftTip.roll = 0.1f;
			rightTip.roll = -0.1f;
		}
	}
}
