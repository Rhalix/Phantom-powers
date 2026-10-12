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

/** The Wishkeeper: a genie ghost with a golden turban, long white beard, open vest, gold bracers, an oil lamp in hand and a swirling smoke tail. */
public class WishkeeperModel extends SinglePartEntityModel<WishkeeperEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("wishkeeper"), "main");
	private final ModelPart root;
	private final ModelPart head, beard, plume, body, tail1, tail2, tail3, tail4, rightArm, leftArm, lamp;

	public WishkeeperModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.head = root.getChild("head");
		this.beard = head.getChild("beard");
		this.plume = head.getChild("turban").getChild("plume");
		this.body = root.getChild("body");
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
				.uv(0, 0).cuboid(-4f, -8f, -4f, 8f, 8f, 8f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_turban = p_head.addChild("turban", ModelPartBuilder.create()
				.uv(0, 16).cuboid(-4.7f, -11f, -4.7f, 9.4f, 3.6f, 9.4f)
				.uv(40, 16).cuboid(-3.6f, -13f, -3.6f, 7.2f, 2f, 7.2f)
				.uv(76, 39).cuboid(-1.1f, -10.8f, -5.2f, 2.2f, 2.2f, 0.7f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_plume = p_turban.addChild("plume", ModelPartBuilder.create()
				.uv(20, 39).cuboid(-0.6f, -6f, -0.4f, 1.2f, 6f, 0.8f),
				ModelTransform.of(0f, -11f, -4.4f, -0.35f, 0f, 0.15f));
		ModelPartData p_beard = p_head.addChild("beard", ModelPartBuilder.create()
				.uv(52, 30).cuboid(-3f, 0f, -0.6f, 6f, 6f, 1.4f)
				.uv(34, 39).cuboid(-1.2f, 6f, -0.5f, 2.4f, 3f, 1.2f),
				ModelTransform.of(0f, -1.5f, -4.1f, 0.1f, 0f, 0f));
		ModelPartData p_earrings = p_head.addChild("earrings", ModelPartBuilder.create()
				.uv(100, 39).cuboid(-4.6f, -3f, -0.5f, 0.6f, 1.6f, 1f)
				.uv(104, 39).cuboid(4f, -3f, -0.5f, 0.6f, 1.6f, 1f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(64, 0).cuboid(-4.5f, 0f, -2.5f, 9f, 10f, 5f)
				.uv(92, 0).cuboid(-4.5f, 0f, -2.5f, 9f, 10f, 5f, new Dilation(0.3f))
				.uv(0, 30).cuboid(-5f, 8f, -3f, 10f, 2.2f, 6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail1 = p_body.addChild("tail1", ModelPartBuilder.create()
				.uv(72, 16).cuboid(-4f, 0f, -2.5f, 8f, 5f, 5f),
				ModelTransform.of(0f, 10f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail2 = p_tail1.addChild("tail2", ModelPartBuilder.create()
				.uv(32, 30).cuboid(-3f, 0f, -2f, 6f, 5f, 4f),
				ModelTransform.of(0f, 4.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail3 = p_tail2.addChild("tail3", ModelPartBuilder.create()
				.uv(68, 30).cuboid(-2f, 0f, -1.5f, 4f, 5f, 3f),
				ModelTransform.of(0f, 4.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail4 = p_tail3.addChild("tail4", ModelPartBuilder.create()
				.uv(26, 39).cuboid(-1f, 0f, -1f, 2f, 4f, 2f),
				ModelTransform.of(0f, 4.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(32, 0).cuboid(-3f, -2f, -2f, 4f, 12f, 4f),
				ModelTransform.of(-6f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_bracer = p_right_arm.addChild("right_bracer", ModelPartBuilder.create()
				.uv(82, 30).cuboid(-3f, 5f, -2f, 4f, 4f, 4f, new Dilation(0.3f))
				.uv(44, 39).cuboid(-3f, -1f, -2f, 4f, 1f, 4f, new Dilation(0.25f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(48, 0).cuboid(-1f, -2f, -2f, 4f, 12f, 4f),
				ModelTransform.of(6f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_bracer = p_left_arm.addChild("left_bracer", ModelPartBuilder.create()
				.uv(98, 30).cuboid(-1f, 5f, -2f, 4f, 4f, 4f, new Dilation(0.3f))
				.uv(60, 39).cuboid(-1f, -1f, -2f, 4f, 1f, 4f, new Dilation(0.25f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_lamp = p_left_arm.addChild("lamp", ModelPartBuilder.create()
				.uv(0, 39).cuboid(-1.6f, 0f, -2.8f, 3.2f, 2f, 5.6f)
				.uv(84, 39).cuboid(-0.6f, 0.3f, -5.2f, 1.2f, 1f, 2.6f)
				.uv(94, 39).cuboid(-0.6f, -1.2f, 2.6f, 1.2f, 2.4f, 1f)
				.uv(108, 39).cuboid(-1f, -1f, -1f, 2f, 1f, 2f)
				.uv(116, 39).cuboid(-0.5f, -0.7f, -5.8f, 1f, 1.3f, 1f),
				ModelTransform.of(1f, 11f, -1f, 0f, 0f, 0f));
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
		beard.pitch = 0.1f + MathHelper.sin(age * 0.09f) * 0.05f;
		plume.roll = 0.15f + MathHelper.sin(age * 0.13f) * 0.12f;
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
		rightArm.roll = 0.35f + MathHelper.sin(age * 0.6f) * 0.1f * cast;
	}
}
