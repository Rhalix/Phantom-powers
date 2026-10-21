package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.WishkeeperEntity;
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

/** The Wishkeeper: a genie ghost with flowing black hair swept over one eye, a jeweled headband, a sky-blue top, lilac choker and belt, blue bangles and a long curling sky-blue smoke tail. */
public class WishkeeperModel extends SinglePartEntityModel<WishkeeperEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("wishkeeper"), "main");
	private final ModelPart root;
	private final ModelPart head, longHair, hairTip, rightLock, leftLock, body, tail1, tail2, tail3, tail4, tail5, tail6, rightArm, leftArm;

	public WishkeeperModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.head = root.getChild("head");
		ModelPart hair = head.getChild("hair");
		this.longHair = hair.getChild("long_hair");
		this.hairTip = longHair.getChild("hair_tip");
		this.rightLock = hair.getChild("right_lock");
		this.leftLock = hair.getChild("left_lock");
		this.body = root.getChild("body");
		this.tail1 = body.getChild("tail1");
		this.tail2 = tail1.getChild("tail2");
		this.tail3 = tail2.getChild("tail3");
		this.tail4 = tail3.getChild("tail4");
		this.tail5 = tail4.getChild("tail5");
		this.tail6 = tail5.getChild("tail6");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-3.5f, -8f, -3f, 7f, 8f, 6f)
				.uv(31, 2).cuboid(0.1f, -5.8f, -3.08f, 2.65f, 2.25f, 0.16f)
				.uv(40, 2).cuboid(0.7f, -5.5f, -3.19f, 1.15f, 1.5f, 0.15f)
				.uv(46, 2).cuboid(0.85f, -5.45f, -3.29f, 0.35f, 0.45f, 0.12f)
				.uv(50, 2).cuboid(-0.05f, -6.1f, -3.2f, 3f, 0.45f, 0.2f)
				.uv(60, 2).cuboid(0.1f, -6.9f, -3.18f, 2.6f, 0.5f, 0.18f)
				.uv(69, 2).cuboid(0.2f, -3.7f, -3.3f, 0.7f, 0.75f, 0.5f)
				.uv(75, 2).cuboid(-1.8f, -2.8f, -3.13f, 3.8f, 1.4f, 0.2f)
				.uv(86, 2).cuboid(-1.55f, -2.75f, -3.27f, 3.25f, 0.65f, 0.16f)
				.uv(96, 2).cuboid(-0.8f, -1.55f, -3.25f, 2.4f, 0.35f, 0.2f),
				ModelTransform.of(0f, -2f, 0f, 0f, 0f, 0f));
		ModelPartData p_hair = p_head.addChild("hair", ModelPartBuilder.create()
				.uv(105, 2).cuboid(-4.5f, -9f, -4f, 9f, 3f, 8f)
				.uv(142, 2).cuboid(-3.5f, -10f, -3f, 7f, 1f, 6f)
				.uv(171, 2).cuboid(-4.5f, -7f, -4.2f, 4f, 4f, 1.4f)
				.uv(185, 2).cuboid(-4.5f, -3f, -4.1f, 2.5f, 4f, 1.3f)
				.uv(196, 2).cuboid(-4.7f, -7f, -3f, 1.5f, 8f, 7f)
				.uv(216, 2).cuboid(3.2f, -7f, -2f, 1.5f, 8f, 6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_long_hair = p_hair.addChild("long_hair", ModelPartBuilder.create()
				.uv(234, 2).cuboid(-5.8f, 0f, 0f, 2f, 21f, 2.7f)
				.uv(2, 28).cuboid(-3.9f, 0f, 0.3f, 2f, 23f, 2.7f)
				.uv(15, 28).cuboid(-2f, 0f, 0.1f, 2f, 22f, 2.7f)
				.uv(28, 28).cuboid(0f, 0f, 0.4f, 2f, 24f, 2.7f)
				.uv(41, 28).cuboid(2f, 0f, 0.1f, 2f, 22f, 2.7f)
				.uv(54, 28).cuboid(4f, 0f, 0f, 2f, 20f, 2.7f),
				ModelTransform.of(0f, -7f, 2.4f, 0.05f, 0f, 0f));
		ModelPartData p_hair_tip = p_long_hair.addChild("hair_tip", ModelPartBuilder.create()
				.uv(67, 28).cuboid(-6f, 0f, 0f, 2f, 8f, 2.5f)
				.uv(79, 28).cuboid(-4f, 2f, 0.4f, 2f, 9f, 2.5f)
				.uv(91, 28).cuboid(-2f, 1f, 0.1f, 2f, 10f, 2.5f)
				.uv(103, 28).cuboid(0f, 3f, 0.4f, 2f, 9f, 2.5f)
				.uv(115, 28).cuboid(2f, 1f, 0.2f, 2f, 9f, 2.5f)
				.uv(127, 28).cuboid(4f, 0f, 0f, 2f, 7f, 2.5f),
				ModelTransform.of(0f, 20f, 0.3f, 0.08f, 0f, 0f));
		ModelPartData p_right_lock = p_hair.addChild("right_lock", ModelPartBuilder.create()
				.uv(139, 28).cuboid(-1.4f, 0f, -0.5f, 3f, 12f, 2f)
				.uv(152, 28).cuboid(-2.2f, 11f, -0.5f, 3f, 7f, 2f)
				.uv(165, 28).cuboid(-3f, 17f, -0.5f, 2f, 6f, 2f)
				.uv(176, 28).cuboid(-3.6f, 22f, -0.5f, 1.4f, 4f, 2f),
				ModelTransform.of(-4.6f, -1.5f, -2.4f, -0.06f, 0f, 0.1f));
		ModelPartData p_left_lock = p_hair.addChild("left_lock", ModelPartBuilder.create()
				.uv(186, 28).cuboid(-1.4f, 0f, -0.5f, 3f, 12f, 2f)
				.uv(199, 28).cuboid(-0.6f, 11f, -0.5f, 3f, 7f, 2f)
				.uv(212, 28).cuboid(0.2f, 17f, -0.5f, 2f, 6f, 2f)
				.uv(223, 28).cuboid(0.8f, 22f, -0.5f, 1.4f, 4f, 2f),
				ModelTransform.of(4.6f, -1.5f, -2.4f, -0.06f, 0f, -0.1f));
		ModelPartData p_headband = p_head.addChild("headband", ModelPartBuilder.create()
				.uv(2, 57).cuboid(-4.65f, -8.2f, -4.18f, 9.3f, 1f, 8.4f)
				.uv(41, 57).cuboid(-3.3f, -8f, -4.5f, 1.3f, 1.6f, 0.5f)
				.uv(48, 57).cuboid(-3.05f, -7.8f, -4.62f, 0.8f, 1f, 0.2f)
				.uv(53, 57).cuboid(-0.65f, -8f, -4.5f, 1.3f, 1.6f, 0.5f)
				.uv(60, 57).cuboid(-0.4f, -7.8f, -4.62f, 0.8f, 1f, 0.2f)
				.uv(65, 57).cuboid(2f, -8f, -4.5f, 1.3f, 1.6f, 0.5f)
				.uv(72, 57).cuboid(2.25f, -7.8f, -4.62f, 0.8f, 1f, 0.2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(77, 57).cuboid(-1.4f, -0.7f, -1.4f, 2.8f, 2f, 2.8f)
				.uv(92, 57).cuboid(-4f, 1f, -2f, 8f, 3f, 4f)
				.uv(119, 57).cuboid(-3.5f, 4f, -1.8f, 7f, 3f, 3.6f)
				.uv(144, 57).cuboid(-2.2f, 7f, -1.4f, 4.4f, 3f, 2.8f)
				.uv(162, 57).cuboid(-2.8f, 10f, -1.7f, 5.6f, 2f, 3.4f)
				.uv(183, 57).cuboid(-3.6f, 11.8f, -2.2f, 7.2f, 1.2f, 4.4f)
				.uv(210, 57).cuboid(-4f, 13f, -2.5f, 8f, 1.2f, 5f)
				.uv(239, 57).cuboid(-1.4f, 12.5f, -2.9f, 2.8f, 2.8f, 0.65f)
				.uv(249, 57).cuboid(-0.95f, 12.95f, -3.05f, 1.9f, 1.9f, 0.25f)
				.uv(2, 69).cuboid(-0.6f, 13.15f, -3.21f, 1.2f, 1.25f, 0.18f),
				ModelTransform.of(0f, -2f, 0f, 0f, 0f, 0f));
		ModelPartData p_choker = p_body.addChild("choker", ModelPartBuilder.create()
				.uv(8, 69).cuboid(-1.7f, -0.3f, -1.7f, 3.4f, 1f, 3.4f)
				.uv(25, 69).cuboid(-1.6f, 0.6f, -2f, 1f, 1.2f, 0.5f)
				.uv(31, 69).cuboid(-0.5f, 0.9f, -2f, 1f, 1.2f, 0.5f)
				.uv(37, 69).cuboid(0.6f, 0.6f, -2f, 1f, 1.2f, 0.5f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_tube_top = p_body.addChild("tube_top", ModelPartBuilder.create()
				.uv(43, 69).cuboid(-4f, 3f, -2.15f, 8f, 1.3f, 4.3f)
				.uv(71, 69).cuboid(-3.75f, 4.1f, -2.5f, 7.5f, 1.3f, 5f)
				.uv(99, 69).cuboid(-3.35f, 5.3f, -2.4f, 6.7f, 1.3f, 4.8f)
				.uv(125, 69).cuboid(-2.9f, 6.4f, -1.8f, 5.8f, 1.3f, 3.6f)
				.uv(147, 69).cuboid(-4.1f, 2.6f, -2.3f, 8.2f, 0.7f, 4.6f)
				.uv(176, 69).cuboid(-3.8f, 3.3f, -2.51f, 7.6f, 0.55f, 0.25f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_tabs = p_body.addChild("tabs", ModelPartBuilder.create()
				.uv(195, 69).cuboid(-3.5f, 14.1f, -2.9f, 1.2f, 1.6f, 0.5f)
				.uv(202, 69).cuboid(-3.25f, 14.35f, -3.05f, 0.7f, 0.85f, 0.2f)
				.uv(207, 69).cuboid(-0.6f, 15f, -2.9f, 1.2f, 1.6f, 0.5f)
				.uv(214, 69).cuboid(-0.35f, 15.25f, -3.05f, 0.7f, 0.85f, 0.2f)
				.uv(219, 69).cuboid(2.3f, 14.1f, -2.9f, 1.2f, 1.6f, 0.5f)
				.uv(226, 69).cuboid(2.55f, 14.35f, -3.05f, 0.7f, 0.85f, 0.2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail1 = p_body.addChild("tail1", ModelPartBuilder.create()
				.uv(2, 78).cuboid(-4f, 0f, -2.5f, 8f, 7f, 5f),
				ModelTransform.of(0f, 13f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail2 = p_tail1.addChild("tail2", ModelPartBuilder.create()
				.uv(31, 78).cuboid(-3.3f, 0f, -2.2f, 6.6f, 6f, 4.4f),
				ModelTransform.of(0f, 6.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail3 = p_tail2.addChild("tail3", ModelPartBuilder.create()
				.uv(56, 78).cuboid(-2.4f, 0f, -1.8f, 5.2f, 5f, 3.6f),
				ModelTransform.of(0f, 5.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail4 = p_tail3.addChild("tail4", ModelPartBuilder.create()
				.uv(77, 78).cuboid(-1.3f, 0f, -1.4f, 3.8f, 4.5f, 2.8f),
				ModelTransform.of(0f, 4.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail5 = p_tail4.addChild("tail5", ModelPartBuilder.create()
				.uv(94, 78).cuboid(-0.2f, 0f, -1f, 2.6f, 4f, 2f),
				ModelTransform.of(0f, 4f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail6 = p_tail5.addChild("tail6", ModelPartBuilder.create()
				.uv(107, 78).cuboid(0.6f, 0f, -0.7f, 1.6f, 3.5f, 1.4f)
				.uv(116, 78).cuboid(0.6f, 3f, -1.6f, 1.4f, 1.5f, 2.5f)
				.uv(127, 78).cuboid(0.9f, 4f, -2.6f, 1f, 1.8f, 1.4f),
				ModelTransform.of(0f, 3.6f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(135, 78).cuboid(-2f, -1f, -1.5f, 3f, 6f, 3f)
				.uv(150, 78).cuboid(-1.85f, 5f, -1.35f, 2.7f, 5f, 2.7f)
				.uv(164, 78).cuboid(-2f, 10f, -1.45f, 3f, 2.5f, 2.9f)
				.uv(179, 78).cuboid(-2f, 12f, -1.45f, 0.85f, 1f, 2f)
				.uv(188, 78).cuboid(-1f, 12f, -1.45f, 0.85f, 1.4f, 2f)
				.uv(197, 78).cuboid(0f, 12f, -1.45f, 0.85f, 1f, 2f)
				.uv(206, 78).cuboid(-2.65f, 10f, -1f, 1f, 1.7f, 1.8f),
				ModelTransform.of(-5f, -0.5f, 0f, 0f, 0f, 0.2f));
		ModelPartData p_right_bands = p_right_arm.addChild("right_bands", ModelPartBuilder.create()
				.uv(215, 78).cuboid(-2.1f, 2.1f, -1.6f, 3.2f, 1.3f, 3.2f)
				.uv(231, 78).cuboid(-2.3f, 8f, -1.8f, 3.6f, 0.75f, 3.6f)
				.uv(2, 92).cuboid(-2.3f, 9.5f, -1.8f, 3.6f, 0.75f, 3.6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(20, 92).cuboid(-1f, -1f, -1.5f, 3f, 6f, 3f)
				.uv(35, 92).cuboid(-0.85f, 5f, -1.35f, 2.7f, 5f, 2.7f)
				.uv(49, 92).cuboid(-1f, 10f, -1.45f, 3f, 2.5f, 2.9f)
				.uv(64, 92).cuboid(-1f, 12f, -1.45f, 0.85f, 1f, 2f)
				.uv(73, 92).cuboid(0f, 12f, -1.45f, 0.85f, 1.4f, 2f)
				.uv(82, 92).cuboid(1f, 12f, -1.45f, 0.85f, 1f, 2f)
				.uv(91, 92).cuboid(1.7f, 10f, -1f, 1f, 1.7f, 1.8f),
				ModelTransform.of(5f, -0.5f, 0f, 0f, 0f, -0.2f));
		ModelPartData p_left_bands = p_left_arm.addChild("left_bands", ModelPartBuilder.create()
				.uv(100, 92).cuboid(-1.1f, 2.1f, -1.6f, 3.2f, 1.3f, 3.2f)
				.uv(116, 92).cuboid(-1.3f, 8f, -1.8f, 3.6f, 0.75f, 3.6f)
				.uv(134, 92).cuboid(-1.3f, 9.5f, -1.8f, 3.6f, 0.75f, 3.6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 256);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(WishkeeperEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		float bob = MathHelper.sin(age * 0.07f) * 1.2f;
		head.pivotY = -2f + bob;
		body.pivotY = -2f + bob;
		rightArm.pivotY = -0.5f + bob;
		leftArm.pivotY = -0.5f + bob;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		// Long hair flows behind her and sways
		longHair.pitch = 0.06f + Math.max(0f, -head.pitch) + limbDistance * 0.45f + MathHelper.sin(age * 0.09f) * 0.05f;
		hairTip.pitch = 0.08f + MathHelper.sin(age * 0.09f - 0.8f) * 0.1f;
		longHair.roll = MathHelper.sin(age * 0.06f) * 0.04f;
		rightLock.pitch = -head.pitch * 0.5f + MathHelper.sin(age * 0.08f) * 0.04f;
		leftLock.pitch = -head.pitch * 0.5f + MathHelper.sin(age * 0.08f + 1f) * 0.04f;
		// The tail curls and swirls like smoke
		float speed = e.isStorm() ? 0.22f : 0.12f;
		tail1.pitch = 0.05f + MathHelper.sin(age * speed) * 0.06f + limbDistance * 0.3f;
		tail2.pitch = 0.12f + MathHelper.sin(age * speed - 0.7f) * 0.1f;
		tail3.pitch = 0.22f + MathHelper.sin(age * speed - 1.4f) * 0.15f;
		tail4.pitch = 0.35f + MathHelper.sin(age * speed - 2.1f) * 0.2f;
		tail5.pitch = 0.5f + MathHelper.sin(age * speed - 2.8f) * 0.25f;
		tail6.pitch = 0.8f + MathHelper.sin(age * speed - 3.5f) * 0.3f;
		tail3.roll = MathHelper.sin(age * speed * 0.7f) * 0.1f;
		tail4.roll = 0.1f + MathHelper.sin(age * speed * 0.7f - 0.7f) * 0.15f;
		tail5.roll = 0.2f + MathHelper.sin(age * speed * 0.7f - 1.4f) * 0.2f;
		tail6.roll = 0.3f + MathHelper.sin(age * speed * 0.7f - 2.1f) * 0.25f;
		// Arms hang loose at her sides; she raises them to cast a wish
		float cast = e.casting();
		rightArm.pitch = -cast * 1.9f + MathHelper.sin(age * 0.07f) * 0.04f;
		rightArm.yaw = cast * -0.3f;
		rightArm.roll = 0.12f + cast * 0.3f + MathHelper.sin(age * 0.6f) * 0.08f * cast;
		leftArm.pitch = -cast * 1.9f + MathHelper.sin(age * 0.07f + 1f) * 0.04f;
		leftArm.yaw = cast * 0.3f;
		leftArm.roll = -0.12f - cast * 0.3f - MathHelper.sin(age * 0.6f) * 0.08f * cast;
	}
}
