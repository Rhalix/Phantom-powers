package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.ShadeEntity;
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

/** The Shade: a smoky shadow with long clawed arms and a curling wisp of a tail. */
@SuppressWarnings("unused")
public class ShadeModel extends SinglePartEntityModel<ShadeEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("shade"), "main");
	private final ModelPart root;
	private final ModelPart body, head, tail, tail2, tail3, tail4, rightArm, rightFore, leftArm, leftFore;

	public ShadeModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.tail = body.getChild("tail");
		this.tail2 = tail.getChild("tail2");
		this.tail3 = tail2.getChild("tail3");
		this.tail4 = tail3.getChild("tail4");
		this.rightArm = root.getChild("right_arm");
		this.rightFore = rightArm.getChild("right_fore");
		this.leftArm = root.getChild("left_arm");
		this.leftFore = leftArm.getChild("left_fore");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-5.8f, 0f, -3f, 11.6f, 4.5f, 6f)
				.uv(41, 2).cuboid(-4.7f, 4.5f, -2.6f, 9.4f, 3.2f, 5.2f)
				.uv(74, 2).cuboid(-3.3f, 7.6f, -2f, 6.6f, 3f, 4f),
				ModelTransform.of(0f, 0f, 0f, 0.25f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(99, 2).cuboid(-4.8f, -8.5f, -4f, 9.6f, 8.5f, 8f)
				.uv(138, 2).cuboid(-3.5f, -11f, -0.6f, 1.6f, 3f, 2f)
				.uv(149, 2).cuboid(-3f, -13f, -0.6f, 1.6f, 2.4f, 2f)
				.uv(160, 2).cuboid(-3.5f, -14.2f, -0.6f, 1.6f, 1.5f, 2f)
				.uv(171, 2).cuboid(1f, -11f, -0.6f, 1.6f, 3f, 2f)
				.uv(182, 2).cuboid(1.5f, -13f, -0.6f, 1.6f, 2.4f, 2f)
				.uv(193, 2).cuboid(1f, -14.2f, -0.6f, 1.6f, 1.5f, 2f),
				ModelTransform.of(0f, -1f, -1f, -0.25f, 0f, 0f));
		ModelPartData p_tail = p_body.addChild("tail", ModelPartBuilder.create()
				.uv(204, 2).cuboid(-2.75f, 0f, -1.5f, 5.5f, 4.8f, 3f)
				.uv(224, 2).cuboid(-2.75f, 0f, -1.65f, 0.25f, 4.8f, 0.25f)
				.uv(228, 2).cuboid(2.5f, 0f, -1.65f, 0.25f, 4.8f, 0.25f)
				.uv(232, 2).cuboid(-2.75f, 4.55f, -1.65f, 5.5f, 0.25f, 0.25f),
				ModelTransform.of(0f, 10.5f, 0.5f, 0.05f, 0f, 0f));
		ModelPartData p_tail2 = p_tail.addChild("tail2", ModelPartBuilder.create()
				.uv(2, 22).cuboid(-0.7f, 0f, -1.5f, 4f, 4.8f, 3f)
				.uv(19, 22).cuboid(-0.7f, 0f, -1.65f, 0.25f, 4.8f, 0.25f)
				.uv(23, 22).cuboid(3.05f, 0f, -1.65f, 0.25f, 4.8f, 0.25f)
				.uv(27, 22).cuboid(-0.7f, 4.55f, -1.65f, 4f, 0.25f, 0.25f),
				ModelTransform.of(0f, 4.5f, 0.5f, 0.05f, 0f, 0f));
		ModelPartData p_tail3 = p_tail2.addChild("tail3", ModelPartBuilder.create()
				.uv(39, 22).cuboid(-1.55f, 0f, -2f, 2.5f, 4.8f, 2f)
				.uv(51, 22).cuboid(-1.55f, 0f, -2.15f, 0.25f, 4.8f, 0.25f)
				.uv(55, 22).cuboid(0.7f, 0f, -2.15f, 0.25f, 4.8f, 0.25f)
				.uv(59, 22).cuboid(-1.55f, 4.55f, -2.15f, 2.5f, 0.25f, 0.25f),
				ModelTransform.of(0f, 4.5f, 0.3f, 0.05f, 0f, 0f));
		ModelPartData p_tail4 = p_tail3.addChild("tail4", ModelPartBuilder.create()
				.uv(68, 22).cuboid(-2f, 0f, -2.5f, 1.4f, 5f, 2f)
				.uv(78, 22).cuboid(-2f, 0f, -2.65f, 0.25f, 5f, 0.25f)
				.uv(82, 22).cuboid(-0.85f, 0f, -2.65f, 0.25f, 5f, 0.25f)
				.uv(86, 22).cuboid(-2f, 4.75f, -2.65f, 1.4f, 0.25f, 0.25f),
				ModelTransform.of(0f, 4.5f, 0.2f, 0.05f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(93, 22).cuboid(-3.4f, -1f, -3.3f, 5.5f, 3f, 6.6f)
				.uv(121, 22).cuboid(-3f, 2f, -2.5f, 4.7f, 5f, 5f),
				ModelTransform.of(-7f, 1f, -1f, -0.3f, 0f, 0.2f));
		ModelPartData p_right_fore = p_right_arm.addChild("right_fore", ModelPartBuilder.create()
				.uv(144, 22).cuboid(-2.3f, 0f, -2.5f, 4.6f, 8.8f, 5f)
				.uv(167, 22).cuboid(-2.5f, 8f, -2.6f, 5f, 2.6f, 5.2f),
				ModelTransform.of(-1f, 7f, 0f, -0.5f, 0f, 0f));
		ModelPartData p_right_claws = p_right_fore.addChild("right_claws", ModelPartBuilder.create()
				.uv(191, 22).cuboid(-3f, -1.5f, -2.5f, 6f, 3.7f, 5f)
				.uv(216, 22).cuboid(-3f, 2f, -2.5f, 1.6f, 3.5f, 2f)
				.uv(227, 22).cuboid(-3f, 4.6f, -3.4f, 1.6f, 1.5f, 2.8f)
				.uv(239, 22).cuboid(-0.9f, 2f, -2.5f, 1.6f, 3.5f, 2f)
				.uv(2, 39).cuboid(-0.9f, 4.6f, -3.4f, 1.6f, 1.5f, 2.8f)
				.uv(14, 39).cuboid(1.2f, 2f, -2.5f, 1.6f, 3.5f, 2f)
				.uv(25, 39).cuboid(1.2f, 4.6f, -3.4f, 1.6f, 1.5f, 2.8f)
				.uv(37, 39).cuboid(-4.1f, -0.3f, -1f, 1.7f, 3f, 2f),
				ModelTransform.of(-0.5f, 11f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(48, 39).cuboid(-1.4f, -1f, -3.3f, 5.5f, 3f, 6.6f)
				.uv(76, 39).cuboid(-1f, 2f, -2.5f, 4.7f, 5f, 5f),
				ModelTransform.of(7f, 1f, -1f, -0.3f, 0f, -0.2f));
		ModelPartData p_left_fore = p_left_arm.addChild("left_fore", ModelPartBuilder.create()
				.uv(99, 39).cuboid(-2.3f, 0f, -2.5f, 4.6f, 8.8f, 5f)
				.uv(122, 39).cuboid(-2.5f, 8f, -2.6f, 5f, 2.6f, 5.2f),
				ModelTransform.of(1f, 7f, 0f, -0.5f, 0f, 0f));
		ModelPartData p_left_claws = p_left_fore.addChild("left_claws", ModelPartBuilder.create()
				.uv(146, 39).cuboid(-3f, -1.5f, -2.5f, 6f, 3.7f, 5f)
				.uv(171, 39).cuboid(-3f, 2f, -2.5f, 1.6f, 3.5f, 2f)
				.uv(182, 39).cuboid(-3f, 4.6f, -3.4f, 1.6f, 1.5f, 2.8f)
				.uv(194, 39).cuboid(-0.9f, 2f, -2.5f, 1.6f, 3.5f, 2f)
				.uv(205, 39).cuboid(-0.9f, 4.6f, -3.4f, 1.6f, 1.5f, 2.8f)
				.uv(217, 39).cuboid(1.2f, 2f, -2.5f, 1.6f, 3.5f, 2f)
				.uv(228, 39).cuboid(1.2f, 4.6f, -3.4f, 1.6f, 1.5f, 2.8f)
				.uv(240, 39).cuboid(-4.1f, -0.3f, -1f, 1.7f, 3f, 2f),
				ModelTransform.of(0.5f, 11f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 256);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(ShadeEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		float bob = MathHelper.sin(age * 0.1f) * 0.8f;
		body.pivotY = bob;
		rightArm.pivotY = 1 + bob;
		leftArm.pivotY = 1 + bob;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = -0.25f + headPitch * MathHelper.RADIANS_PER_DEGREE;
		float wave = MathHelper.sin(age * 0.12f);
		tail.yaw = wave * 0.15f;
		tail2.roll = MathHelper.sin(age * 0.12f - 0.8f) * 0.2f;
		tail3.roll = MathHelper.sin(age * 0.12f - 1.6f) * 0.3f;
		tail4.roll = MathHelper.sin(age * 0.12f - 2.4f) * 0.4f;
		tail2.pitch = 0.3f + limbDistance * 0.3f;
		tail3.pitch = 0.4f + limbDistance * 0.3f;
		// long arms hang and sway; a big two-handed claw swipe when attacking
		float sw = Math.max(e.swipe(), MathHelper.sin(e.handSwingProgress * MathHelper.PI));
		float hang = MathHelper.sin(age * 0.08f) * 0.1f;
		rightArm.pitch = -0.3f + hang - sw * 1.8f;
		leftArm.pitch = -0.3f - hang - sw * 1.6f;
		rightArm.yaw = sw * 0.5f;
		leftArm.yaw = -sw * 0.5f;
		rightFore.pitch = -0.5f + sw * 0.4f;
		leftFore.pitch = -0.5f + sw * 0.4f;
	}
}
