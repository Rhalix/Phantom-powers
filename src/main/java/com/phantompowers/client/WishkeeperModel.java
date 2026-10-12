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

/** The Wishkeeper: a genie ghost with long black hair, a gold circlet, a sheer blue veil, a blue Arabian outfit with gold trim, gold bangles, an oil lamp and a swirling smoke tail. */
public class WishkeeperModel extends SinglePartEntityModel<WishkeeperEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("wishkeeper"), "main");
	private final ModelPart root;
	private final ModelPart head, longHair, hairTip, body, shawl, tail1, tail2, tail3, tail4, rightArm, leftArm, lamp;

	public WishkeeperModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.head = root.getChild("head");
		this.longHair = head.getChild("hair").getChild("long_hair");
		this.hairTip = longHair.getChild("hair_tip");
		this.body = root.getChild("body");
		this.shawl = body.getChild("shawl");
		this.tail1 = body.getChild("tail1");
		this.tail2 = tail1.getChild("tail2");
		this.tail3 = tail2.getChild("tail3");
		this.tail4 = tail3.getChild("tail4");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.lamp = leftArm.getChild("lamp");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(22, 0).cuboid(-4f, -8f, -4f, 8f, 8f, 8f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_hair = p_head.addChild("hair", ModelPartBuilder.create()
				.uv(44, 18).cuboid(-4.5f, -8.6f, -4.5f, 9f, 3f, 9f)
				.uv(6, 50).cuboid(-4.6f, -7.4f, -4.7f, 3.4f, 1.6f, 1f)
				.uv(16, 50).cuboid(1.2f, -7.4f, -4.7f, 3.4f, 1.6f, 1f)
				.uv(54, 0).cuboid(-5.1f, -6.5f, -3.5f, 1f, 9f, 6f)
				.uv(68, 0).cuboid(4.1f, -6.5f, -3.5f, 1f, 9f, 6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_long_hair = p_hair.addChild("long_hair", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-4.5f, 0f, 0f, 9f, 16f, 1.4f),
				ModelTransform.of(0f, -6f, 4.2f, 0.08f, 0f, 0f));
		ModelPartData p_hair_tip = p_long_hair.addChild("hair_tip", ModelPartBuilder.create()
				.uv(32, 42).cuboid(-3.5f, 0f, -0.2f, 7f, 5f, 1.2f),
				ModelTransform.of(0f, 16f, 0.2f, 0.1f, 0f, 0f));
		ModelPartData p_circlet = p_head.addChild("circlet", ModelPartBuilder.create()
				.uv(80, 18).cuboid(-4.7f, -7.6f, -4.7f, 9.4f, 1f, 9.4f)
				.uv(26, 50).cuboid(-0.9f, -8.9f, -5f, 1.8f, 1.8f, 0.5f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_veil = p_head.addChild("veil", ModelPartBuilder.create()
				.uv(58, 42).cuboid(-4.3f, -3.1f, -4.6f, 8.6f, 3.7f, 0.4f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_earrings = p_head.addChild("earrings", ModelPartBuilder.create()
				.uv(102, 42).cuboid(-4.7f, -3.2f, -0.6f, 0.6f, 2f, 1.2f)
				.uv(108, 42).cuboid(4.1f, -3.2f, -0.6f, 0.6f, 2f, 1.2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(0, 18).cuboid(-4f, 0f, -2f, 8f, 10f, 4f)
				.uv(0, 32).cuboid(-4f, 0f, -2f, 8f, 6f, 4f, new Dilation(0.3f))
				.uv(70, 32).cuboid(-4.5f, 8f, -2.5f, 9f, 2.2f, 5f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_shawl = p_body.addChild("shawl", ModelPartBuilder.create()
				.uv(24, 18).cuboid(-4.5f, 0f, 0f, 9f, 13f, 0.6f),
				ModelTransform.of(0f, 0f, 2.4f, 0.1f, 0f, 0f));
		ModelPartData p_tail1 = p_body.addChild("tail1", ModelPartBuilder.create()
				.uv(24, 32).cuboid(-4f, 0f, -2.5f, 8f, 5f, 5f),
				ModelTransform.of(0f, 10f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail2 = p_tail1.addChild("tail2", ModelPartBuilder.create()
				.uv(50, 32).cuboid(-3f, 0f, -2f, 6f, 5f, 4f),
				ModelTransform.of(0f, 4.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail3 = p_tail2.addChild("tail3", ModelPartBuilder.create()
				.uv(98, 32).cuboid(-2f, 0f, -1.5f, 4f, 5f, 3f),
				ModelTransform.of(0f, 4.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail4 = p_tail3.addChild("tail4", ModelPartBuilder.create()
				.uv(50, 42).cuboid(-1f, 0f, -1f, 2f, 4f, 2f),
				ModelTransform.of(0f, 4.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(82, 0).cuboid(-2f, -2f, -1.5f, 3f, 12f, 3f),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_sleeve = p_right_arm.addChild("right_sleeve", ModelPartBuilder.create()
				.uv(112, 32).cuboid(-2f, -2f, -1.5f, 3f, 5f, 3f, new Dilation(0.45f))
				.uv(78, 42).cuboid(-2f, 6f, -1.5f, 3f, 2f, 3f, new Dilation(0.25f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(94, 0).cuboid(-1f, -2f, -1.5f, 3f, 12f, 3f),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_sleeve = p_left_arm.addChild("left_sleeve", ModelPartBuilder.create()
				.uv(0, 42).cuboid(-1f, -2f, -1.5f, 3f, 5f, 3f, new Dilation(0.45f))
				.uv(90, 42).cuboid(-1f, 6f, -1.5f, 3f, 2f, 3f, new Dilation(0.25f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_lamp = p_left_arm.addChild("lamp", ModelPartBuilder.create()
				.uv(12, 42).cuboid(-1.6f, 0f, -2.8f, 3.2f, 2f, 5.6f)
				.uv(114, 42).cuboid(-0.6f, 0.3f, -5.2f, 1.2f, 1f, 2.6f)
				.uv(0, 50).cuboid(-0.6f, -1.2f, 2.6f, 1.2f, 2.4f, 1f)
				.uv(32, 50).cuboid(-1f, -1f, -1f, 2f, 1f, 2f)
				.uv(40, 50).cuboid(-0.5f, -0.7f, -5.8f, 1f, 1.3f, 1f),
				ModelTransform.of(0.5f, 11f, -1f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 128);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(WishkeeperEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		float bob = MathHelper.sin(age * 0.07f) * 1.2f;
		head.pivotY = bob;
		body.pivotY = bob;
		rightArm.pivotY = 2f + bob;
		leftArm.pivotY = 2f + bob;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		// Long hair and shawl flow behind her
		longHair.pitch = 0.08f + Math.max(0f, -head.pitch) + limbDistance * 0.4f + MathHelper.sin(age * 0.09f) * 0.05f;
		hairTip.pitch = 0.1f + MathHelper.sin(age * 0.09f - 0.8f) * 0.1f;
		longHair.roll = MathHelper.sin(age * 0.06f) * 0.04f;
		shawl.pitch = 0.1f + limbDistance * 0.6f + MathHelper.sin(age * 0.1f) * 0.05f;
		// Swirling smoke tail
		float speed = e.isStorm() ? 0.22f : 0.12f;
		tail1.pitch = 0.2f + MathHelper.sin(age * speed) * 0.1f;
		tail2.pitch = 0.3f + MathHelper.sin(age * speed - 0.8f) * 0.15f;
		tail3.pitch = 0.4f + MathHelper.sin(age * speed - 1.6f) * 0.22f;
		tail4.pitch = 0.6f + MathHelper.sin(age * speed - 2.4f) * 0.3f;
		tail1.roll = MathHelper.sin(age * speed * 0.7f) * 0.12f;
		tail2.roll = MathHelper.sin(age * speed * 0.7f - 0.7f) * 0.18f;
		tail3.roll = MathHelper.sin(age * speed * 0.7f - 1.4f) * 0.24f;
		// Lamp held out in the left hand; the right hand weaves spells
		float cast = e.casting();
		leftArm.pitch = -0.9f + MathHelper.sin(age * 0.1f) * 0.05f;
		leftArm.yaw = -0.2f;
		leftArm.roll = -0.15f;
		lamp.pitch = 0.9f;
		rightArm.pitch = -0.3f - cast * 1.8f + MathHelper.sin(age * 0.3f) * 0.1f * cast;
		rightArm.yaw = cast * -0.3f;
		rightArm.roll = 0.3f + MathHelper.sin(age * 0.6f) * 0.1f * cast;
	}
}
