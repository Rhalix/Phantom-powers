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
				.uv(60, 0).cuboid(-4f, -8f, -4f, 8f, 8f, 8f),
				ModelTransform.of(0f, -2f, 0f, 0f, 0f, 0f));
		ModelPartData p_hair = p_head.addChild("hair", ModelPartBuilder.create()
				.uv(46, 24).cuboid(-4.8f, -9.4f, -4.8f, 9.6f, 3.4f, 9.6f)
				.uv(66, 40).cuboid(-3.8f, -10.4f, -3.8f, 7.6f, 1.2f, 7.6f)
				.uv(104, 62).cuboid(-1.2f, -8f, -5f, 6f, 4f, 1.2f)
				.uv(0, 70).cuboid(2.6f, -4.2f, -5f, 2.2f, 3.2f, 1.2f)
				.uv(92, 0).cuboid(-5.4f, -7.8f, -4f, 1.4f, 7f, 8.4f)
				.uv(0, 24).cuboid(4f, -7.8f, -4f, 1.4f, 7f, 8.4f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_long_hair = p_hair.addChild("long_hair", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-6f, 0f, 0f, 12f, 20f, 3.2f),
				ModelTransform.of(0f, -7f, 2.4f, 0.05f, 0f, 0f));
		ModelPartData p_hair_tip = p_long_hair.addChild("hair_tip", ModelPartBuilder.create()
				.uv(24, 52).cuboid(-6f, 0f, -0.2f, 12f, 6f, 3f)
				.uv(54, 52).cuboid(-6f, 6f, -0.1f, 2.5f, 6f, 2.6f)
				.uv(40, 62).cuboid(-3f, 6f, -0.1f, 2.5f, 4f, 2.6f)
				.uv(98, 40).cuboid(0f, 6f, -0.1f, 2.5f, 7f, 2.6f)
				.uv(116, 52).cuboid(3.2f, 6f, -0.1f, 2.8f, 5f, 2.6f),
				ModelTransform.of(0f, 20f, 0.3f, 0.08f, 0f, 0f));
		ModelPartData p_right_lock = p_hair.addChild("right_lock", ModelPartBuilder.create()
				.uv(32, 0).cuboid(-1.6f, 0f, -1.2f, 3f, 15f, 3.4f)
				.uv(52, 62).cuboid(-1.2f, 15f, -1f, 2f, 4f, 3f)
				.uv(10, 70).cuboid(-0.6f, 19f, -0.8f, 1.2f, 3f, 2.4f),
				ModelTransform.of(-4.6f, -1.5f, -2.4f, -0.06f, 0f, 0.1f));
		ModelPartData p_left_lock = p_hair.addChild("left_lock", ModelPartBuilder.create()
				.uv(46, 0).cuboid(-1.4f, 0f, -1.2f, 3f, 15f, 3.4f)
				.uv(62, 62).cuboid(-0.8f, 15f, -1f, 2f, 4f, 3f)
				.uv(20, 70).cuboid(-0.6f, 19f, -0.8f, 1.2f, 3f, 2.4f),
				ModelTransform.of(4.6f, -1.5f, -2.4f, -0.06f, 0f, -0.1f));
		ModelPartData p_headband = p_head.addChild("headband", ModelPartBuilder.create()
				.uv(0, 40).cuboid(-4.9f, -8.6f, -4.9f, 9.8f, 1.3f, 9.8f)
				.uv(48, 76).cuboid(-3.2f, -8.8f, -5.2f, 1.3f, 1.3f, 0.5f)
				.uv(54, 76).cuboid(-0.65f, -8.8f, -5.2f, 1.3f, 1.3f, 0.5f)
				.uv(60, 76).cuboid(1.9f, -8.8f, -5.2f, 1.3f, 1.3f, 0.5f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(38, 70).cuboid(-1.5f, -0.5f, -1.5f, 3f, 1.5f, 3f)
				.uv(0, 52).cuboid(-4f, 1f, -2f, 8f, 6f, 4f)
				.uv(72, 62).cuboid(-3f, 7f, -1.5f, 6f, 4f, 3f)
				.uv(66, 52).cuboid(-4.4f, 11f, -2.6f, 8.8f, 2.6f, 5.2f)
				.uv(66, 76).cuboid(-1f, 11.5f, -2.9f, 2f, 1.6f, 0.4f),
				ModelTransform.of(0f, -2f, 0f, 0f, 0f, 0f));
		ModelPartData p_choker = p_body.addChild("choker", ModelPartBuilder.create()
				.uv(50, 70).cuboid(-2f, 0f, -2f, 4f, 1f, 4f)
				.uv(72, 76).cuboid(-1.6f, 1f, -2.2f, 1f, 1.2f, 0.4f)
				.uv(76, 76).cuboid(-0.5f, 1f, -2.2f, 1f, 1.6f, 0.4f)
				.uv(80, 76).cuboid(0.6f, 1f, -2.2f, 1f, 1.2f, 0.4f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_tube_top = p_body.addChild("tube_top", ModelPartBuilder.create()
				.uv(0, 62).cuboid(-4f, 2.2f, -2f, 8f, 4f, 4f, new Dilation(0.25f))
				.uv(100, 70).cuboid(-3.6f, 3f, -2.9f, 7.2f, 2.6f, 1f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_tabs = p_body.addChild("tabs", ModelPartBuilder.create()
				.uv(84, 76).cuboid(-3.6f, 13.6f, -2.7f, 1.4f, 1.8f, 0.4f)
				.uv(118, 70).cuboid(-0.7f, 13.6f, -2.7f, 1.4f, 2.2f, 0.4f)
				.uv(90, 76).cuboid(2.2f, 13.6f, -2.7f, 1.4f, 1.8f, 0.4f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail1 = p_body.addChild("tail1", ModelPartBuilder.create()
				.uv(86, 24).cuboid(-4.4f, 0f, -2.7f, 8.8f, 7f, 5.4f),
				ModelTransform.of(0f, 13f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail2 = p_tail1.addChild("tail2", ModelPartBuilder.create()
				.uv(40, 40).cuboid(-3.8f, 0f, -2.3f, 7.6f, 6f, 4.6f),
				ModelTransform.of(0f, 6.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail3 = p_tail2.addChild("tail3", ModelPartBuilder.create()
				.uv(96, 52).cuboid(-3f, 0f, -1.9f, 6f, 5f, 3.8f),
				ModelTransform.of(0f, 5.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail4 = p_tail3.addChild("tail4", ModelPartBuilder.create()
				.uv(24, 62).cuboid(-2.3f, 0f, -1.5f, 4.6f, 4.5f, 3f),
				ModelTransform.of(0f, 4.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail5 = p_tail4.addChild("tail5", ModelPartBuilder.create()
				.uv(90, 62).cuboid(-1.6f, 0f, -1.1f, 3.2f, 4f, 2.2f),
				ModelTransform.of(0f, 4f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail6 = p_tail5.addChild("tail6", ModelPartBuilder.create()
				.uv(30, 70).cuboid(-1f, 0f, -0.8f, 2f, 3.5f, 1.6f)
				.uv(66, 70).cuboid(-1.2f, 3f, -2.2f, 1.6f, 1.2f, 2.4f),
				ModelTransform.of(0f, 3.6f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(22, 24).cuboid(-2f, -1f, -1.5f, 3f, 13f, 3f),
				ModelTransform.of(-5f, -0.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_bands = p_right_arm.addChild("right_bands", ModelPartBuilder.create()
				.uv(76, 70).cuboid(-2f, 1f, -1.5f, 3f, 1.5f, 3f, new Dilation(0.25f))
				.uv(0, 76).cuboid(-2f, 7.4f, -1.5f, 3f, 0.9f, 3f, new Dilation(0.4f))
				.uv(12, 76).cuboid(-2f, 8.7f, -1.5f, 3f, 0.9f, 3f, new Dilation(0.4f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(34, 24).cuboid(-1f, -1f, -1.5f, 3f, 13f, 3f),
				ModelTransform.of(5f, -0.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_bands = p_left_arm.addChild("left_bands", ModelPartBuilder.create()
				.uv(88, 70).cuboid(-1f, 1f, -1.5f, 3f, 1.5f, 3f, new Dilation(0.25f))
				.uv(24, 76).cuboid(-1f, 7.4f, -1.5f, 3f, 0.9f, 3f, new Dilation(0.4f))
				.uv(36, 76).cuboid(-1f, 8.7f, -1.5f, 3f, 0.9f, 3f, new Dilation(0.4f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 128);
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
