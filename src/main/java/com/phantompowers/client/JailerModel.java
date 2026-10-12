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

/** The Ghost Jailer: a towering prison warden in a greatcoat and peaked cap, chains across his chest and a glowing baton. */
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
				.uv(70, 0).cuboid(-4f, -8f, -4f, 8f, 8f, 8f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_cap = p_head.addChild("cap", ModelPartBuilder.create()
				.uv(48, 17).cuboid(-4.5f, -10.6f, -4.5f, 9f, 2.6f, 9f)
				.uv(0, 45).cuboid(-4.7f, -8.3f, -4.7f, 9.4f, 0.9f, 9.4f)
				.uv(28, 56).cuboid(-4.5f, -8f, -7.2f, 9f, 0.7f, 2.8f)
				.uv(52, 56).cuboid(-1f, -10.3f, -4.8f, 2f, 2f, 0.4f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-5f, 0f, -2.5f, 10f, 12f, 5f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_coat_tail = p_body.addChild("coat_tail", ModelPartBuilder.create()
				.uv(16, 17).cuboid(-5f, 0f, -2.6f, 10f, 7f, 5.2f, new Dilation(0.2f)),
				ModelTransform.of(0f, 11f, 0f, 0.05f, 0f, 0f));
		ModelPartData p_bandolier = p_body.addChild("bandolier", ModelPartBuilder.create()
				.uv(102, 0).cuboid(-0.6f, -7.5f, -0.4f, 1.2f, 15f, 0.8f),
				ModelTransform.of(0f, 6f, -2.75f, 0f, 0f, 0.75f));
		ModelPartData p_keys = p_body.addChild("keys", ModelPartBuilder.create()
				.uv(58, 56).cuboid(-1f, -0.2f, -0.3f, 2f, 2f, 0.6f)
				.uv(64, 56).cuboid(-0.3f, 1.6f, -0.3f, 0.6f, 2f, 0.6f)
				.uv(68, 56).cuboid(0.6f, 1.4f, -0.3f, 0.6f, 1.6f, 0.6f),
				ModelTransform.of(4.2f, 9.5f, -2.6f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(108, 0).cuboid(-3f, -2f, -2f, 4f, 12f, 4f),
				ModelTransform.of(-6f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_pauldron = p_right_arm.addChild("right_pauldron", ModelPartBuilder.create()
				.uv(40, 45).cuboid(-3.6f, -3f, -2.6f, 5f, 3.6f, 5.2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_gauntlet = p_right_arm.addChild("right_gauntlet", ModelPartBuilder.create()
				.uv(84, 17).cuboid(-3.7f, 6.5f, -2.7f, 5.4f, 5.6f, 5.4f)
				.uv(104, 45).cuboid(-3.9f, 10.6f, -2.9f, 5.8f, 1.4f, 2.2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_baton = p_right_arm.addChild("baton", ModelPartBuilder.create()
				.uv(0, 33).cuboid(-0.8f, -0.8f, -9f, 1.6f, 1.6f, 10f)
				.uv(0, 56).cuboid(-1.1f, -1.1f, -9.6f, 2.2f, 2.2f, 1.2f),
				ModelTransform.of(-1f, 9.5f, -1f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(0, 17).cuboid(-1f, -2f, -2f, 4f, 12f, 4f),
				ModelTransform.of(6f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_pauldron = p_left_arm.addChild("left_pauldron", ModelPartBuilder.create()
				.uv(62, 45).cuboid(-1.4f, -3f, -2.6f, 5f, 3.6f, 5.2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_gauntlet = p_left_arm.addChild("left_gauntlet", ModelPartBuilder.create()
				.uv(24, 33).cuboid(-1.7f, 6.5f, -2.7f, 5.4f, 5.6f, 5.4f)
				.uv(10, 56).cuboid(-1.9f, 10.6f, -2.9f, 5.8f, 1.4f, 2.2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_arm_chain = p_left_arm.addChild("arm_chain", ModelPartBuilder.create()
				.uv(88, 45).cuboid(-1f, 2f, -2f, 4f, 2f, 4f, new Dilation(0.45f))
				.uv(84, 45).cuboid(0.5f, 10f, -0.5f, 1f, 9f, 1f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(30, 0).cuboid(-2.25f, 0f, -2.25f, 4.5f, 12f, 4.5f),
				ModelTransform.of(-2.4f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_boot = p_right_leg.addChild("right_boot", ModelPartBuilder.create()
				.uv(48, 33).cuboid(-2.6f, 6.8f, -2.9f, 5.2f, 5.2f, 5.3f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(50, 0).cuboid(-2.25f, 0f, -2.25f, 4.5f, 12f, 4.5f),
				ModelTransform.of(2.4f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_boot = p_left_leg.addChild("left_boot", ModelPartBuilder.create()
				.uv(72, 33).cuboid(-2.6f, 6.8f, -2.9f, 5.2f, 5.2f, 5.3f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 128);
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
