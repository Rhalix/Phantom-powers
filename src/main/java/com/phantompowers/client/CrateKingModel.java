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

/**
 * The Crate King: a big cardboard box for a head, a ghostly body trailing into a wavy tail,
 * and long arms that carry a crate... or, in stage two, Pandora's Box with its lid creaking open.
 */
public class CrateKingModel extends SinglePartEntityModel<CrateKingEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("crate_king"), "main");

	private final ModelPart root, head, flapL, flapR, body, tail1, tail2, tail3, rightArm, leftArm, heldCrate, pandora, lid, horns;

	public CrateKingModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.head = root.getChild("head");
		this.flapL = head.getChild("flap_l");
		this.flapR = head.getChild("flap_r");
		this.horns = head.getChild("horns");
		this.body = root.getChild("body");
		this.tail1 = body.getChild("tail1");
		this.tail2 = tail1.getChild("tail2");
		this.tail3 = tail2.getChild("tail3");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.heldCrate = body.getChild("held_crate");
		this.pandora = body.getChild("pandora");
		this.lid = pandora.getChild("lid");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData head = r.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-5f, -10f, -5f, 10, 10, 10),
				ModelTransform.pivot(0f, 1f, 0f));
		// open cardboard flaps sticking up off the top of the box
		head.addChild("flap_l", ModelPartBuilder.create().uv(108, 0).cuboid(-5f, -4f, 0f, 5, 4, 0.5f),
				ModelTransform.of(0f, -10f, -5f, -0.5f, 0f, 0f));
		head.addChild("flap_r", ModelPartBuilder.create().uv(108, 0).cuboid(0f, -4f, 0f, 5, 4, 0.5f),
				ModelTransform.of(0f, -10f, 5f, 0.5f, 0f, 0f));
		head.addChild("horns", ModelPartBuilder.create()
				.uv(0, 40).cuboid(-5.5f, -14f, -1f, 2, 4, 2)
				.uv(0, 40).cuboid(3.5f, -14f, -1f, 2, 4, 2), ModelTransform.NONE);
		ModelPartData body = r.addChild("body", ModelPartBuilder.create().uv(40, 0).cuboid(-4f, 0f, -2.5f, 8, 8, 5, new Dilation(0.1f)),
				ModelTransform.pivot(0f, 1f, 0f));
		ModelPartData t1 = body.addChild("tail1", ModelPartBuilder.create().uv(66, 0).cuboid(-3f, 0f, -2f, 6, 5, 4),
				ModelTransform.pivot(0f, 8f, 0f));
		ModelPartData t2 = t1.addChild("tail2", ModelPartBuilder.create().uv(86, 0).cuboid(-2f, 0f, -1.5f, 4, 4, 3),
				ModelTransform.pivot(0f, 5f, 0f));
		t2.addChild("tail3", ModelPartBuilder.create().uv(100, 0).cuboid(-1f, 0f, -1f, 2, 4, 2),
				ModelTransform.pivot(0f, 4f, 0f));
		body.addChild("right_arm", ModelPartBuilder.create().uv(0, 20).cuboid(-1.5f, 0f, -1.5f, 3, 9, 3),
				ModelTransform.pivot(-5.5f, 1f, 0f));
		body.addChild("left_arm", ModelPartBuilder.create().uv(12, 20).mirrored().cuboid(-1.5f, 0f, -1.5f, 3, 9, 3),
				ModelTransform.pivot(5.5f, 1f, 0f));
		body.addChild("held_crate", ModelPartBuilder.create().uv(24, 20).cuboid(-3.5f, -3.5f, -3.5f, 7, 7, 7),
				ModelTransform.pivot(0f, 6f, -8f));
		ModelPartData pandora = body.addChild("pandora", ModelPartBuilder.create()
						.uv(52, 20).cuboid(-4f, -2.5f, -3f, 8, 5, 6)
						.uv(108, 20).cuboid(-1f, 0f, -3.6f, 2, 2, 1),
				ModelTransform.pivot(0f, 6f, -8f));
		pandora.addChild("lid", ModelPartBuilder.create().uv(80, 20).cuboid(-4f, -2f, -6f, 8, 2, 6, new Dilation(0.15f)),
				ModelTransform.pivot(0f, -2.5f, 3f));
		return TexturedModelData.of(data, 128, 64);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(CrateKingEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		boolean evil = e.isEvil();
		float bob = MathHelper.sin(age * 0.08f) * 1.0f;
		head.pivotY = 1f + bob;
		body.pivotY = 1f + bob;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE * 0.5f;
		head.roll = evil ? 0f : MathHelper.sin(age * 0.05f) * 0.08f;     // a silly wobble before he turns evil

		flapL.pitch = -0.5f - MathHelper.sin(age * 0.2f) * 0.15f;
		flapR.pitch = 0.5f + MathHelper.sin(age * 0.2f + 1f) * 0.15f;
		horns.visible = evil;

		tail1.pitch = 0.15f + MathHelper.sin(age * 0.12f) * 0.12f;
		tail2.pitch = 0.2f + MathHelper.sin(age * 0.12f - 0.8f) * 0.18f;
		tail3.pitch = 0.25f + MathHelper.sin(age * 0.12f - 1.6f) * 0.25f;
		tail1.roll = MathHelper.sin(age * 0.07f) * 0.1f;
		tail2.roll = MathHelper.sin(age * 0.07f - 0.7f) * 0.14f;

		// Arms hold the box out in front
		float sway = MathHelper.sin(age * 0.07f) * 0.06f;
		rightArm.pitch = -1.25f + sway;
		leftArm.pitch = -1.25f - sway;
		rightArm.roll = -0.35f;
		leftArm.roll = 0.35f;

		heldCrate.visible = !evil;
		pandora.visible = evil;
		heldCrate.pivotY = 6f + MathHelper.sin(age * 0.1f) * 0.6f;
		pandora.pivotY = 6f + MathHelper.sin(age * 0.1f) * 0.6f;
		lid.pitch = -e.lidOpen() * 1.6f - MathHelper.sin(age * 0.3f) * 0.05f * (evil ? 1 : 0);
	}
}
