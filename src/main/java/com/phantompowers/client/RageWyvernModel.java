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
				.uv(0, 0).cuboid(-5f, -5f, -9f, 10f, 10f, 18f)
				.uv(56, 0).cuboid(-4f, 2.75f, -8f, 8f, 3f, 16f, new Dilation(0f, -0.25f, 0f))
				.uv(24, 68).cuboid(-5.5f, -4.5f, -9.4f, 11f, 1f, 7f, new Dilation(-0.1f, 0f, 0f)),
				ModelTransform.of(0f, 8f, 0f, 0f, 0f, 0f));
		ModelPartData p_spines = p_body.addChild("spines", ModelPartBuilder.create()
				.uv(48, 78).cuboid(-0.5f, -2f, -7f, 1f, 2f, 2f)
				.uv(34, 78).cuboid(-0.5f, -2.75f, -3f, 1f, 3f, 2f, new Dilation(0f, -0.25f, 0f))
				.uv(54, 78).cuboid(-0.5f, -2f, 1f, 1f, 2f, 2f)
				.uv(60, 78).cuboid(-0.5f, -1.75f, 5f, 1f, 2f, 2f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(0f, -5f, 0f, 0f, 0f, 0f));
		ModelPartData p_amulet = p_body.addChild("amulet", ModelPartBuilder.create()
				.uv(40, 78).cuboid(-1.5f, 0f, -1f, 3f, 4f, 1f),
				ModelTransform.of(0f, -1f, -9f, 0f, 0f, 0f));
		ModelPartData p_neck = p_body.addChild("neck", ModelPartBuilder.create()
				.uv(32, 28).cuboid(-3f, -3f, -8f, 6f, 6f, 8f)
				.uv(66, 78).cuboid(-0.5f, -4.75f, -6f, 1f, 2f, 2f, new Dilation(0f, -0.25f, 0f))
				.uv(72, 78).cuboid(-0.5f, -4.75f, -2.5f, 1f, 2f, 2f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(0f, -2f, -8f, -0.6f, 0f, 0f));
		ModelPartData p_head = p_neck.addChild("head", ModelPartBuilder.create()
				.uv(60, 28).cuboid(-4f, -4f, -7f, 8f, 7f, 7f)
				.uv(0, 68).cuboid(-3f, -2f, -13f, 6f, 4f, 6f),
				ModelTransform.of(0f, 0f, -7f, 0.55f, 0f, 0f));
		ModelPartData p_jaw = p_head.addChild("jaw", ModelPartBuilder.create()
				.uv(60, 68).cuboid(-2.5f, 0f, -5.5f, 5f, 2f, 6f),
				ModelTransform.of(0f, 2f, -7f, 0.15f, 0f, 0f));
		ModelPartData p_horn_l = p_head.addChild("horn_l", ModelPartBuilder.create()
				.uv(118, 68).cuboid(-1f, -5f, -1f, 2f, 5f, 2f, new Dilation(-0.25f, 0f, -0.25f))
				.uv(78, 78).cuboid(-0.5f, -8f, -0.5f, 1f, 3f, 1f),
				ModelTransform.of(2.5f, -3.5f, -2f, 0.7f, 0.25f, 0f));
		ModelPartData p_horn_r = p_head.addChild("horn_r", ModelPartBuilder.create()
				.uv(0, 78).cuboid(-1f, -5f, -1f, 2f, 5f, 2f, new Dilation(-0.25f, 0f, -0.25f))
				.uv(82, 78).cuboid(-0.5f, -8f, -0.5f, 1f, 3f, 1f),
				ModelTransform.of(-2.5f, -3.5f, -2f, 0.7f, -0.25f, 0f));
		ModelPartData p_left_wing = p_body.addChild("left_wing", ModelPartBuilder.create()
				.uv(86, 78).cuboid(0f, -1f, -1f, 14f, 2f, 2f)
				.uv(0, 43).cuboid(0f, -0.5f, 1f, 14f, 1f, 12f),
				ModelTransform.of(5f, -4f, -4f, 0f, 0f, 0f));
		ModelPartData p_left_wing_tip = p_left_wing.addChild("left_wing_tip", ModelPartBuilder.create()
				.uv(0, 85).cuboid(0f, -1f, -1f, 12f, 2f, 2f, new Dilation(0f, -0.25f, -0.25f))
				.uv(36, 56).cuboid(0f, -0.5f, 0.75f, 12f, 1f, 10f),
				ModelTransform.of(14f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_wing = p_body.addChild("right_wing", ModelPartBuilder.create()
				.uv(28, 85).cuboid(-14f, -1f, -1f, 14f, 2f, 2f)
				.uv(52, 43).cuboid(-14f, -0.5f, 1f, 14f, 1f, 12f),
				ModelTransform.of(-5f, -4f, -4f, 0f, 0f, 0f));
		ModelPartData p_right_wing_tip = p_right_wing.addChild("right_wing_tip", ModelPartBuilder.create()
				.uv(60, 85).cuboid(-12f, -1f, -1f, 12f, 2f, 2f, new Dilation(0f, -0.25f, -0.25f))
				.uv(80, 56).cuboid(-12f, -0.5f, 0.75f, 12f, 1f, 10f),
				ModelTransform.of(-14f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail = p_body.addChild("tail", ModelPartBuilder.create()
				.uv(0, 28).cuboid(-3f, -2f, 0f, 6f, 5f, 10f)
				.uv(88, 85).cuboid(-0.5f, -3.75f, 3f, 1f, 2f, 2f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(0f, -2f, 9f, -0.1f, 0f, 0f));
		ModelPartData p_tail2 = p_tail.addChild("tail2", ModelPartBuilder.create()
				.uv(90, 28).cuboid(-2f, -1.5f, 0f, 4f, 4f, 10f),
				ModelTransform.of(0f, 0.5f, 10f, 0.1f, 0f, 0f));
		ModelPartData p_tail3 = p_tail2.addChild("tail3", ModelPartBuilder.create()
				.uv(104, 43).cuboid(-1f, -1f, 0f, 2f, 3f, 10f)
				.uv(8, 78).cuboid(-3.5f, 0f, 6f, 7f, 1f, 6f),
				ModelTransform.of(0f, 0.5f, 10f, 0.1f, 0f, 0f));
		ModelPartData p_right_leg = p_body.addChild("right_leg", ModelPartBuilder.create()
				.uv(0, 56).cuboid(-2f, 0f, -2.5f, 4f, 7f, 5f),
				ModelTransform.of(-4f, 3f, 4f, 0f, 0f, 0f));
		ModelPartData p_right_foot = p_right_leg.addChild("right_foot", ModelPartBuilder.create()
				.uv(82, 68).cuboid(-2f, 0f, -4f, 4f, 3f, 5f),
				ModelTransform.of(0f, 7f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = p_body.addChild("left_leg", ModelPartBuilder.create()
				.uv(18, 56).cuboid(-2f, 0f, -2.5f, 4f, 7f, 5f),
				ModelTransform.of(4f, 3f, 4f, 0f, 0f, 0f));
		ModelPartData p_left_foot = p_left_leg.addChild("left_foot", ModelPartBuilder.create()
				.uv(100, 68).cuboid(-2f, 0f, -4f, 4f, 3f, 5f),
				ModelTransform.of(0f, 7f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 128);
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
