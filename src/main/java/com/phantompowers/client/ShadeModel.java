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
				.uv(0, 0).cuboid(-5f, 0f, -3f, 10f, 11f, 6f)
				.uv(0, 17).cuboid(-6f, -1f, -3.5f, 12f, 4f, 7f),
				ModelTransform.of(0f, 0f, 0f, 0.25f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(32, 0).cuboid(-4f, -9f, -4f, 8f, 9f, 8f)
				.uv(36, 28).cuboid(-3f, -12f, -1f, 2f, 4f, 2f)
				.uv(20, 28).cuboid(1f, -13f, 0f, 2f, 5f, 2f)
				.uv(52, 28).cuboid(-1f, -11f, 1f, 2f, 3f, 2f),
				ModelTransform.of(0f, -1f, -1f, -0.25f, 0f, 0f));
		ModelPartData p_tail = p_body.addChild("tail", ModelPartBuilder.create()
				.uv(38, 17).cuboid(-4f, 0f, -2.5f, 8f, 5f, 5f),
				ModelTransform.of(0f, 10.5f, 0.5f, 0.2f, 0f, 0f));
		ModelPartData p_tail2 = p_tail.addChild("tail2", ModelPartBuilder.create()
				.uv(64, 17).cuboid(-3f, 0f, -2f, 6f, 5f, 4f),
				ModelTransform.of(0f, 4.5f, 0.5f, 0.3f, 0f, 0f));
		ModelPartData p_tail3 = p_tail2.addChild("tail3", ModelPartBuilder.create()
				.uv(84, 17).cuboid(-2f, 0f, -1.5f, 4f, 5f, 3f),
				ModelTransform.of(0f, 4.5f, 0.3f, 0.4f, 0f, 0f));
		ModelPartData p_tail4 = p_tail3.addChild("tail4", ModelPartBuilder.create()
				.uv(28, 28).cuboid(-1f, 0f, -1f, 2f, 5f, 2f),
				ModelTransform.of(0f, 4.5f, 0.2f, 0.5f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(64, 0).cuboid(-3f, -2f, -2f, 4f, 9f, 4f),
				ModelTransform.of(-7f, 1f, -1f, -0.3f, 0f, 0.2f));
		ModelPartData p_right_fore = p_right_arm.addChild("right_fore", ModelPartBuilder.create()
				.uv(96, 0).cuboid(-2f, 0f, -1.5f, 3f, 9f, 3f)
				.uv(98, 17).cuboid(-3f, 8f, -2.5f, 5f, 3f, 5f),
				ModelTransform.of(-1f, 7f, 0f, -0.5f, 0f, 0f));
		ModelPartData p_right_claws = p_right_fore.addChild("right_claws", ModelPartBuilder.create()
				.uv(60, 28).cuboid(-2.5f, 0f, -2.5f, 1f, 4f, 1f)
				.uv(44, 28).cuboid(-0.5f, 0f, -2.5f, 1f, 5f, 1f)
				.uv(64, 28).cuboid(1.5f, 0f, -2.5f, 1f, 4f, 1f),
				ModelTransform.of(-0.5f, 11f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(80, 0).cuboid(-1f, -2f, -2f, 4f, 9f, 4f),
				ModelTransform.of(7f, 1f, -1f, -0.3f, 0f, -0.2f));
		ModelPartData p_left_fore = p_left_arm.addChild("left_fore", ModelPartBuilder.create()
				.uv(108, 0).cuboid(-1f, 0f, -1.5f, 3f, 9f, 3f)
				.uv(0, 28).cuboid(-2f, 8f, -2.5f, 5f, 3f, 5f),
				ModelTransform.of(1f, 7f, 0f, -0.5f, 0f, 0f));
		ModelPartData p_left_claws = p_left_fore.addChild("left_claws", ModelPartBuilder.create()
				.uv(68, 28).cuboid(-2.5f, 0f, -2.5f, 1f, 4f, 1f)
				.uv(48, 28).cuboid(-0.5f, 0f, -2.5f, 1f, 5f, 1f)
				.uv(72, 28).cuboid(1.5f, 0f, -2.5f, 1f, 4f, 1f),
				ModelTransform.of(0.5f, 11f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 128);
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
