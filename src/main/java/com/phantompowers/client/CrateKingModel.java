package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.CrateKingEntity;
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

/** The Crate King: a burly ghost king in cardboard armor with a cardboard crown and a bubble-wrap cape. Unboxed, he holds Pandora's Box. */
public class CrateKingModel extends SinglePartEntityModel<CrateKingEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("crate_king"), "main");
	private final ModelPart root;
	private final ModelPart head, body, cape, box, lid, rightArm, leftArm, rightLeg, leftLeg;

	public CrateKingModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.cape = body.getChild("cape");
		this.box = body.getChild("box");
		this.lid = box.getChild("lid");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(20, 18).cuboid(-4f, -8f, -4f, 8f, 8f, 8f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_crown = p_head.addChild("crown", ModelPartBuilder.create()
				.uv(52, 18).cuboid(-4.6f, -10.2f, -4.6f, 9.2f, 2.4f, 9.2f)
				.uv(0, 57).cuboid(-4.6f, -12.2f, -4.6f, 1.6f, 2f, 1.6f)
				.uv(8, 57).cuboid(3f, -12.2f, -4.6f, 1.6f, 2f, 1.6f)
				.uv(16, 57).cuboid(-4.6f, -12.2f, 3f, 1.6f, 2f, 1.6f)
				.uv(24, 57).cuboid(3f, -12.2f, 3f, 1.6f, 2f, 1.6f)
				.uv(114, 47).cuboid(-0.8f, -13.2f, -4.7f, 1.6f, 3f, 1.6f)
				.uv(32, 57).cuboid(-0.8f, -12.2f, 3.1f, 1.6f, 2f, 1.6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-5f, 0f, -3f, 10f, 12f, 6f)
				.uv(96, 47).cuboid(-4f, 5f, -4f, 8f, 6f, 1f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_cape = p_body.addChild("cape", ModelPartBuilder.create()
				.uv(32, 0).cuboid(-5.5f, 0f, 0f, 11f, 17f, 1f),
				ModelTransform.of(0f, 0f, 3.1f, 0.12f, 0f, 0f));
		ModelPartData p_sash = p_body.addChild("sash", ModelPartBuilder.create()
				.uv(56, 0).cuboid(-0.9f, -8f, -0.3f, 1.8f, 16f, 0.6f),
				ModelTransform.of(0f, 6f, -3.2f, 0f, 0f, -0.7f));
		ModelPartData p_box = p_body.addChild("box", ModelPartBuilder.create()
				.uv(92, 18).cuboid(-3f, -3f, -3f, 6f, 6f, 6f),
				ModelTransform.of(0f, 7f, -7f, 0f, 0f, 0f));
		ModelPartData p_lid = p_box.addChild("lid", ModelPartBuilder.create()
				.uv(68, 47).cuboid(-3.4f, -1.3f, -6.4f, 6.8f, 1.3f, 6.8f),
				ModelTransform.of(0f, -3f, 3f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(62, 0).cuboid(-3.5f, -2f, -2.25f, 4.5f, 12f, 4.5f),
				ModelTransform.of(-6.5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_plate = p_right_arm.addChild("right_plate", ModelPartBuilder.create()
				.uv(48, 35).cuboid(-4f, -3f, -2.8f, 5.5f, 4f, 5.6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_glove = p_right_arm.addChild("right_glove", ModelPartBuilder.create()
				.uv(72, 35).cuboid(-3.5f, 7f, -2.25f, 4.5f, 5f, 4.5f, new Dilation(0.3f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(82, 0).cuboid(-1f, -2f, -2.25f, 4.5f, 12f, 4.5f),
				ModelTransform.of(6.5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_plate = p_left_arm.addChild("left_plate", ModelPartBuilder.create()
				.uv(92, 35).cuboid(-1.5f, -3f, -2.8f, 5.5f, 4f, 5.6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_glove = p_left_arm.addChild("left_glove", ModelPartBuilder.create()
				.uv(0, 47).cuboid(-1f, 7f, -2.25f, 4.5f, 5f, 4.5f, new Dilation(0.3f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(102, 0).cuboid(-2.4f, 0f, -2.4f, 4.8f, 12f, 4.8f),
				ModelTransform.of(-2.6f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_greave = p_right_leg.addChild("right_greave", ModelPartBuilder.create()
				.uv(0, 35).cuboid(-2.7f, 2f, -2.8f, 5.4f, 6f, 5.6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_boot = p_right_leg.addChild("right_boot", ModelPartBuilder.create()
				.uv(20, 47).cuboid(-2.7f, 8.5f, -3.2f, 5.4f, 3.5f, 6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(0, 18).cuboid(-2.4f, 0f, -2.4f, 4.8f, 12f, 4.8f),
				ModelTransform.of(2.6f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_greave = p_left_leg.addChild("left_greave", ModelPartBuilder.create()
				.uv(24, 35).cuboid(-2.7f, 2f, -2.8f, 5.4f, 6f, 5.6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_boot = p_left_leg.addChild("left_boot", ModelPartBuilder.create()
				.uv(44, 47).cuboid(-2.7f, 8.5f, -3.2f, 5.4f, 3.5f, 6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 128);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(CrateKingEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		float bob = MathHelper.sin(age * 0.08f) * 0.7f;
		head.pivotY = bob;
		body.pivotY = bob;
		rightArm.pivotY = 2f + bob;
		leftArm.pivotY = 2f + bob;
		rightLeg.pivotY = 12f + bob;
		leftLeg.pivotY = 12f + bob;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		float walk = MathHelper.cos(limbAngle * 0.6662f) * 0.9f * limbDistance;
		rightLeg.pitch = walk;
		leftLeg.pitch = -walk;
		cape.pitch = 0.12f + limbDistance * 0.5f + MathHelper.sin(age * 0.1f) * 0.05f;
		boolean evil = e.isEvil();
		box.visible = evil;
		if (evil) {
			// Both hands hold Pandora's Box out in front; the lid creaks open
			rightArm.pitch = -1.1f + MathHelper.sin(age * 0.2f) * 0.05f;
			rightArm.yaw = -0.35f;
			leftArm.pitch = -1.1f + MathHelper.sin(age * 0.2f) * 0.05f;
			leftArm.yaw = 0.35f;
			rightArm.roll = 0f;
			leftArm.roll = 0f;
			lid.pitch = -e.lidOpen() * 1.3f;
			box.yaw = MathHelper.sin(age * 0.05f) * 0.1f;
		} else {
			// Winding up to hurl a crate
			float swing = MathHelper.sin(e.handSwingProgress * MathHelper.PI);
			rightArm.pitch = -walk * 0.6f - swing * 2.2f;
			rightArm.yaw = 0f;
			rightArm.roll = 0.1f;
			leftArm.pitch = walk * 0.6f;
			leftArm.yaw = 0f;
			leftArm.roll = -0.1f;
		}
	}
}
