package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.VoltageEntity;
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

/** Voltage: spiky electric hair, studded jacket, ghostly tail, and an electric guitar slung across her. */
public class VoltageModel extends SinglePartEntityModel<VoltageEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("voltage"), "main");
	private final ModelPart root, head, hair, body, tail1, tail2, tail3, rightArm, leftArm, guitar;

	public VoltageModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.head = root.getChild("head");
		this.hair = head.getChild("hair");
		this.body = root.getChild("body");
		this.tail1 = body.getChild("tail1");
		this.tail2 = tail1.getChild("tail2");
		this.tail3 = tail2.getChild("tail3");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.guitar = body.getChild("guitar");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData head = r.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4f, -8f, -4f, 8, 8, 8),
				ModelTransform.pivot(0f, 0f, 0f));
		head.addChild("hair", ModelPartBuilder.create()
						.uv(32, 0).cuboid(-4.5f, -9f, -4.5f, 9, 3, 9, new Dilation(0.05f))   // hair cap
						.uv(68, 0).cuboid(-1f, -15f, -2f, 2, 6, 2)      // tall center spike
						.uv(68, 0).cuboid(-3.5f, -13f, -1f, 2, 4, 2)    // side spikes
						.uv(68, 0).cuboid(1.5f, -13f, -1f, 2, 4, 2)
						.uv(68, 0).cuboid(-1f, -12f, 2.5f, 2, 4, 2)     // back spike
						.uv(76, 0).cuboid(-5f, -8f, -3f, 1, 6, 6)       // side locks
						.uv(76, 0).cuboid(4f, -8f, -3f, 1, 6, 6)
						.uv(90, 0).cuboid(-4f, -8f, 4f, 8, 9, 1),       // long back hair
				ModelTransform.NONE);
		ModelPartData body = r.addChild("body", ModelPartBuilder.create().uv(0, 16).cuboid(-4f, 0f, -2f, 8, 9, 4, new Dilation(0.1f)),
				ModelTransform.pivot(0f, 0f, 0f));
		ModelPartData t1 = body.addChild("tail1", ModelPartBuilder.create().uv(24, 16).cuboid(-3f, 0f, -2f, 6, 5, 4),
				ModelTransform.pivot(0f, 9f, 0f));
		ModelPartData t2 = t1.addChild("tail2", ModelPartBuilder.create().uv(44, 16).cuboid(-2f, 0f, -1.5f, 4, 4, 3),
				ModelTransform.pivot(0f, 5f, 0f));
		t2.addChild("tail3", ModelPartBuilder.create().uv(58, 16).cuboid(-1f, 0f, -1f, 2, 4, 2),
				ModelTransform.pivot(0f, 4f, 0f));
		body.addChild("right_arm", ModelPartBuilder.create().uv(0, 30).cuboid(-2f, -1f, -1.5f, 3, 10, 3),
				ModelTransform.pivot(-5f, 1f, 0f));
		body.addChild("left_arm", ModelPartBuilder.create().uv(12, 30).mirrored().cuboid(-1f, -1f, -1.5f, 3, 10, 3),
				ModelTransform.pivot(5f, 1f, 0f));
		ModelPartData guitar = body.addChild("guitar", ModelPartBuilder.create()
						.uv(0, 44).cuboid(-4f, -3.5f, -1f, 8, 7, 2)      // body
						.uv(20, 44).cuboid(-3f, 3.5f, -1f, 6, 2, 2)      // lower bout
						.uv(36, 44).cuboid(-0.75f, -15f, -0.75f, 1.5f, 11.5f, 1.5f)   // neck
						.uv(44, 44).cuboid(-1.5f, -18.5f, -1f, 3, 3.5f, 1.5f)          // headstock
						.uv(54, 44).cuboid(-2f, -1.5f, -1.4f, 4, 1, 0.5f)              // pickups
						.uv(54, 44).cuboid(-2f, 1f, -1.4f, 4, 1, 0.5f),
				ModelTransform.of(0.5f, 6f, -3.5f, 0f, 0f, 0.95f));
		guitar.addChild("strings", ModelPartBuilder.create().uv(64, 44).cuboid(-0.5f, -15f, -1.2f, 1, 18, 0.1f),
				ModelTransform.NONE);
		return TexturedModelData.of(data, 128, 64);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(VoltageEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		float bob = MathHelper.sin(age * 0.09f) * 1.0f;
		head.pivotY = bob;
		body.pivotY = bob;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		// headbanging while she strums
		float strum = e.strum();
		head.pitch += strum * MathHelper.sin(age * 1.2f) * 0.35f;
		hair.roll = MathHelper.sin(age * (e.isEncore() ? 0.6f : 0.25f)) * 0.04f;

		tail1.pitch = 0.15f + MathHelper.sin(age * 0.12f) * 0.12f;
		tail2.pitch = 0.2f + MathHelper.sin(age * 0.12f - 0.8f) * 0.18f;
		tail3.pitch = 0.25f + MathHelper.sin(age * 0.12f - 1.6f) * 0.25f;
		tail1.roll = MathHelper.sin(age * 0.07f) * 0.1f;

		// Fretting hand on the neck, strumming hand on the strings
		leftArm.pitch = -0.9f;
		leftArm.yaw = -0.5f;
		leftArm.roll = -0.6f;
		rightArm.pitch = -0.7f + MathHelper.sin(age * (0.8f + strum)) * (0.15f + strum * 0.35f);
		rightArm.yaw = 0.4f;
		rightArm.roll = 0.1f;
		guitar.roll = 0.95f + strum * 0.08f * MathHelper.sin(age * 1.2f);
	}
}
