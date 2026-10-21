package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.GhostJailerEntity;
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

/** The Ghost Jailer: a towering prison warden in a greatcoat and peaked cap: black three-piece suit with a vest, white tie, pocket square, cufflinks and polished shoes, plus a glowing baton. */
public class JailerModel extends SinglePartEntityModel<GhostJailerEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("ghost_jailer"), "main");
	private final ModelPart root;
	private final ModelPart head, body, coatTail, rightArm, leftArm, rightLeg, leftLeg, baton, armChain;

	public JailerModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.coatTail = body.getChild("coat_tail");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
		this.baton = rightArm.getChild("baton");
		this.armChain = leftArm.getChild("arm_chain");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-4f, -8f, -3.8f, 8f, 8f, 7.6f)
				.uv(37, 2).cuboid(-3.65f, -1f, -3.9f, 7.3f, 1.25f, 7.4f)
				.uv(70, 2).cuboid(-0.65f, -4.4f, -4.15f, 1.3f, 1f, 0.6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_hat = p_head.addChild("hat", ModelPartBuilder.create()
				.uv(77, 2).cuboid(-7f, -9f, -5.5f, 14f, 1f, 11f)
				.uv(130, 2).cuboid(-4.3f, -13.2f, -3.8f, 8.6f, 4.2f, 7.6f)
				.uv(166, 2).cuboid(-3.8f, -13.6f, -3.3f, 7.6f, 0.5f, 6.6f)
				.uv(198, 2).cuboid(-4.36f, -10f, -3.86f, 8.72f, 1f, 7.72f)
				.uv(2, 21).cuboid(-7f, -9.05f, -5.55f, 14f, 0.18f, 0.18f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(34, 21).cuboid(-5f, 0f, -2.6f, 10f, 11f, 5.2f)
				.uv(68, 21).cuboid(-2.4f, -0.3f, -2.15f, 4.8f, 1.3f, 4.3f)
				.uv(90, 21).cuboid(-5.05f, 10f, -2.65f, 10.1f, 1.2f, 5.3f)
				.uv(124, 21).cuboid(-1.4f, 9.9f, -2.95f, 2.8f, 1.6f, 0.5f)
				.uv(134, 21).cuboid(-0.85f, 10.22f, -3.11f, 1.7f, 0.95f, 0.2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_coat_tail = p_body.addChild("coat_tail", ModelPartBuilder.create()
				.uv(141, 21).cuboid(-5f, 0f, -2.6f, 4.85f, 3f, 5.2f)
				.uv(165, 21).cuboid(0.15f, 0f, -2.6f, 4.85f, 3f, 5.2f),
				ModelTransform.of(0f, 11f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(189, 21).cuboid(-2.65f, -1.3f, -2.2f, 4.3f, 2.3f, 4.4f)
				.uv(210, 21).cuboid(-2.5f, -0.7f, -2f, 4f, 7.7f, 4f)
				.uv(229, 21).cuboid(-2.65f, 6.5f, -2.15f, 4.3f, 1.6f, 4.3f)
				.uv(2, 41).cuboid(-2.75f, 7.8f, -2.25f, 4.5f, 0.65f, 4.5f)
				.uv(23, 41).cuboid(-2.65f, 8.45f, -2.1f, 4.3f, 2.7f, 4.2f)
				.uv(43, 41).cuboid(-2.5f, 10.7f, -2.35f, 0.9f, 1.3f, 1.6f)
				.uv(51, 41).cuboid(-2.5f, 11.5f, -2.05f, 0.9f, 0.8f, 1.8f)
				.uv(60, 41).cuboid(-2.42f, 10.85f, -2.45f, 0.7f, 0.25f, 0.14f)
				.uv(65, 41).cuboid(-1.47f, 10.7f, -2.35f, 0.9f, 1.3f, 1.6f)
				.uv(73, 41).cuboid(-1.47f, 11.5f, -2.05f, 0.9f, 0.8f, 1.8f)
				.uv(82, 41).cuboid(-1.39f, 10.85f, -2.45f, 0.7f, 0.25f, 0.14f)
				.uv(87, 41).cuboid(-0.44f, 10.7f, -2.35f, 0.9f, 1.3f, 1.6f)
				.uv(95, 41).cuboid(-0.44f, 11.5f, -2.05f, 0.9f, 0.8f, 1.8f)
				.uv(104, 41).cuboid(-0.36f, 10.85f, -2.45f, 0.7f, 0.25f, 0.14f)
				.uv(109, 41).cuboid(0.59f, 10.7f, -2.35f, 0.9f, 1.3f, 1.6f)
				.uv(117, 41).cuboid(0.59f, 11.5f, -2.05f, 0.9f, 0.8f, 1.8f)
				.uv(126, 41).cuboid(0.67f, 10.85f, -2.45f, 0.7f, 0.25f, 0.14f)
				.uv(131, 41).cuboid(-3.3f, 9f, -1.6f, 1.2f, 2f, 2.7f)
				.uv(142, 41).cuboid(-3.3f, 9f, -1.7f, 1.2f, 0.3f, 0.2f),
				ModelTransform.of(-6f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_baton = p_right_arm.addChild("baton", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(2, 57).cuboid(-1.65f, -1.3f, -2.2f, 4.3f, 2.3f, 4.4f)
				.uv(23, 57).cuboid(-1.5f, -0.7f, -2f, 4f, 7.7f, 4f)
				.uv(42, 57).cuboid(-1.65f, 6.5f, -2.15f, 4.3f, 1.6f, 4.3f)
				.uv(63, 57).cuboid(-1.75f, 7.8f, -2.25f, 4.5f, 0.65f, 4.5f)
				.uv(84, 57).cuboid(-1.65f, 8.45f, -2.1f, 4.3f, 2.7f, 4.2f)
				.uv(104, 57).cuboid(-1.5f, 10.7f, -2.35f, 0.9f, 1.3f, 1.6f)
				.uv(112, 57).cuboid(-1.5f, 11.5f, -2.05f, 0.9f, 0.8f, 1.8f)
				.uv(121, 57).cuboid(-1.42f, 10.85f, -2.45f, 0.7f, 0.25f, 0.14f)
				.uv(126, 57).cuboid(-0.47f, 10.7f, -2.35f, 0.9f, 1.3f, 1.6f)
				.uv(134, 57).cuboid(-0.47f, 11.5f, -2.05f, 0.9f, 0.8f, 1.8f)
				.uv(143, 57).cuboid(-0.39f, 10.85f, -2.45f, 0.7f, 0.25f, 0.14f)
				.uv(148, 57).cuboid(0.56f, 10.7f, -2.35f, 0.9f, 1.3f, 1.6f)
				.uv(156, 57).cuboid(0.56f, 11.5f, -2.05f, 0.9f, 0.8f, 1.8f)
				.uv(165, 57).cuboid(0.64f, 10.85f, -2.45f, 0.7f, 0.25f, 0.14f)
				.uv(170, 57).cuboid(1.59f, 10.7f, -2.35f, 0.9f, 1.3f, 1.6f)
				.uv(178, 57).cuboid(1.59f, 11.5f, -2.05f, 0.9f, 0.8f, 1.8f)
				.uv(187, 57).cuboid(1.67f, 10.85f, -2.45f, 0.7f, 0.25f, 0.14f)
				.uv(192, 57).cuboid(2.4f, 9f, -1.6f, 1.2f, 2f, 2.7f)
				.uv(203, 57).cuboid(2.4f, 9f, -1.7f, 1.2f, 0.3f, 0.2f),
				ModelTransform.of(6f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_arm_chain = p_left_arm.addChild("arm_chain", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(148, 41).cuboid(-2.35f, 0f, -2.15f, 4.7f, 8f, 4.3f)
				.uv(169, 41).cuboid(-2f, 7.5f, -1.9f, 4f, 3.5f, 3.8f)
				.uv(188, 41).cuboid(-2.3f, 10f, -3f, 4.6f, 2f, 5.3f)
				.uv(211, 41).cuboid(-2.4f, 11.6f, -3.1f, 4.8f, 0.5f, 5.5f)
				.uv(235, 41).cuboid(0.8f, 10.1f, -3.13f, 0.6f, 1.4f, 0.2f),
				ModelTransform.of(-2.5f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(209, 57).cuboid(-2.35f, 0f, -2.15f, 4.7f, 8f, 4.3f)
				.uv(230, 57).cuboid(-2f, 7.5f, -1.9f, 4f, 3.5f, 3.8f)
				.uv(2, 73).cuboid(-2.3f, 10f, -3f, 4.6f, 2f, 5.3f)
				.uv(25, 73).cuboid(-2.4f, 11.6f, -3.1f, 4.8f, 0.5f, 5.5f)
				.uv(49, 73).cuboid(0.8f, 10.1f, -3.13f, 0.6f, 1.4f, 0.2f),
				ModelTransform.of(2.5f, 12f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 256);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(GhostJailerEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		float bob = MathHelper.sin(age * 0.07f) * 0.6f;
		head.pivotY = bob;
		body.pivotY = bob;
		rightArm.pivotY = 2f + bob;
		leftArm.pivotY = 2f + bob;
		rightLeg.pivotY = 12f + bob;
		leftLeg.pivotY = 12f + bob;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		// Heavy marching stride
		float walk = MathHelper.cos(limbAngle * 0.6662f) * 0.8f * limbDistance;
		rightLeg.pitch = walk;
		leftLeg.pitch = -walk;
		coatTail.pitch = 0.06f + Math.abs(walk) * 0.35f;
		// Baton raised, ready to strike; swings hard when attacking
		float swing = MathHelper.sin(e.handSwingProgress * MathHelper.PI);
		float cast = e.casting();
		float point = e.pointing();
		rightArm.pitch = -0.55f - walk * 0.3f - swing * 1.6f - cast * 1.4f;
		rightArm.yaw = 0.1f;
		rightArm.roll = 0.08f + cast * 0.25f;
		baton.pitch = 0.6f + swing * 0.6f;
		// Left fist: thrust out to point an accusing finger at you, or raised crackling with ecto energy
		float shake = MathHelper.sin(age * 2.2f) * 0.05f * cast;
		leftArm.pitch = MathHelper.lerp(Math.max(point, cast), walk * 0.5f + MathHelper.sin(age * 0.05f) * 0.05f, -1.5f + shake);
		leftArm.yaw = point * 0.15f * (1 - cast);
		leftArm.roll = -0.12f - cast * 0.25f;
		head.pitch += cast * -0.2f;
		armChain.roll = MathHelper.sin(age * 0.1f) * 0.05f;
	}
}
