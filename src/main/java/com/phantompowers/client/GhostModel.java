package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.AbstractGhostEntity;
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

/** A floating ghost: big head, reaching arms, and a body that tapers into a wavy tail. */
public class GhostModel<T extends AbstractGhostEntity> extends SinglePartEntityModel<T> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("ghost"), "main");

	private final ModelPart root;
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart tail1;
	private final ModelPart tail2;
	private final ModelPart tail3;
	private final ModelPart rightArm;
	private final ModelPart leftArm;

	public GhostModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.tail1 = body.getChild("tail1");
		this.tail2 = tail1.getChild("tail2");
		this.tail3 = tail2.getChild("tail3");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		r.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4f, -8f, -4f, 8, 8, 8, new Dilation(0.2f)),
				ModelTransform.pivot(0f, 2f, 0f));
		ModelPartData body = r.addChild("body", ModelPartBuilder.create().uv(0, 16).cuboid(-4f, 0f, -2.5f, 8, 7, 5),
				ModelTransform.pivot(0f, 2f, 0f));
		ModelPartData t1 = body.addChild("tail1", ModelPartBuilder.create().uv(0, 30).cuboid(-3f, 0f, -2f, 6, 5, 4),
				ModelTransform.pivot(0f, 7f, 0f));
		ModelPartData t2 = t1.addChild("tail2", ModelPartBuilder.create().uv(0, 40).cuboid(-2f, 0f, -1.5f, 4, 4, 3),
				ModelTransform.pivot(0f, 5f, 0f));
		t2.addChild("tail3", ModelPartBuilder.create().uv(0, 48).cuboid(-1f, 0f, -1f, 2, 4, 2),
				ModelTransform.pivot(0f, 4f, 0f));
		body.addChild("right_arm", ModelPartBuilder.create().uv(32, 16).cuboid(-1f, 0f, -1f, 2, 8, 2),
				ModelTransform.pivot(-5f, 1f, 0f));
		body.addChild("left_arm", ModelPartBuilder.create().uv(40, 16).mirrored().cuboid(-1f, 0f, -1f, 2, 8, 2),
				ModelTransform.pivot(5f, 1f, 0f));
		return TexturedModelData.of(data, 64, 64);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		float bob = MathHelper.sin(age * 0.09f) * 1.2f;
		head.pivotY = 2f + bob;
		body.pivotY = 2f + bob;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;

		// Wavy tail
		tail1.pitch = 0.15f + MathHelper.sin(age * 0.12f) * 0.12f;
		tail2.pitch = 0.2f + MathHelper.sin(age * 0.12f - 0.8f) * 0.18f;
		tail3.pitch = 0.25f + MathHelper.sin(age * 0.12f - 1.6f) * 0.25f;
		tail1.roll = MathHelper.sin(age * 0.07f) * 0.08f;
		tail2.roll = MathHelper.sin(age * 0.07f - 0.7f) * 0.12f;
		tail3.roll = MathHelper.sin(age * 0.07f - 1.4f) * 0.18f;

		// Arms: drifting, or reaching out when attacking
		float sway = MathHelper.sin(age * 0.067f) * 0.1f;
		float reach = entity.isAttacking() ? -1.45f : -0.35f;
		rightArm.pitch = reach + sway;
		leftArm.pitch = reach - sway;
		rightArm.roll = 0.12f + MathHelper.cos(age * 0.09f) * 0.05f;
		leftArm.roll = -0.12f - MathHelper.cos(age * 0.09f) * 0.05f;
	}
}
