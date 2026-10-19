package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.DoloraEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/** Dolora's three looks: the prim counselor, the masked shadow wraith, and the crowned Queen of Sorrow. */
public class DoloraModel extends EntityModel<DoloraEntity> {
	public static final EntityModelLayer COUNSELOR = new EntityModelLayer(PhantomMod.id("dolora"), "counselor");
	public static final EntityModelLayer MISERY = new EntityModelLayer(PhantomMod.id("dolora"), "misery");
	public static final EntityModelLayer QUEEN = new EntityModelLayer(PhantomMod.id("dolora"), "queen");

	private final ModelPart counselor, misery, queen;
	private int phase;

	public DoloraModel(ModelPart counselor, ModelPart misery, ModelPart queen) {
		super(RenderLayer::getEntityTranslucent);
		this.counselor = counselor;
		this.misery = misery;
		this.queen = queen;
	}

	public static TexturedModelData counselorModel() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-4f, 0f, -2f, 8f, 12f, 4f)
				.uv(58, 16).cuboid(-3f, -1f, -2.5f, 6f, 2f, 5f)
				.uv(16, 29).cuboid(-1f, 2.8f, -2.65f, 2f, 2f, 1f, new Dilation(-0.4f, -0.2f, -0.35f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(24, 0).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(38, 16).cuboid(-2.5f, -11f, -1.5f, 5f, 3f, 5f)
				.uv(26, 29).cuboid(-3.5f, -11.7f, -0.2f, 7f, 1f, 1f, new Dilation(0f, -0.2f, -0.2f))
				.uv(22, 29).cuboid(-0.5f, -12.5f, -1.2f, 1f, 2f, 1f, new Dilation(-0.2f, 0f, -0.2f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(84, 0).cuboid(-3.25f, -2f, -2f, 4f, 10f, 4f, new Dilation(-0.25f, 0f, 0f))
				.uv(80, 16).cuboid(-3.25f, 8f, -2f, 4f, 3f, 4f, new Dilation(-0.45f, 0f, -0.2f)),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(100, 0).cuboid(-0.75f, -2f, -2f, 4f, 10f, 4f, new Dilation(-0.25f, 0f, 0f))
				.uv(96, 16).cuboid(-0.75f, 8f, -2f, 4f, 3f, 4f, new Dilation(-0.45f, 0f, -0.2f)),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_clipboard = p_left_arm.addChild("clipboard", ModelPartBuilder.create()
				.uv(0, 16).cuboid(-0.5f, -3f, -4.25f, 1f, 7f, 6f, new Dilation(0f, 0f, -0.25f)),
				ModelTransform.of(1.2f, 9f, -1f, -0.9f, 0f, 0f));
		ModelPartData p_skirt = p_body.addChild("skirt", ModelPartBuilder.create()
				.uv(56, 0).cuboid(-4.5f, 0f, -2.5f, 9f, 11f, 5f, new Dilation(-0.1f, 0f, -0.1f)),
				ModelTransform.of(0f, 11f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(112, 16).cuboid(-2f, 9f, -2f, 4f, 3f, 4f, new Dilation(-0.3f, 0f, -0.2f))
				.uv(14, 16).cuboid(-1.5f, 0f, -1.5f, 3f, 9f, 3f),
				ModelTransform.of(-2f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(0, 29).cuboid(-2f, 9f, -2f, 4f, 3f, 4f, new Dilation(-0.3f, 0f, -0.2f))
				.uv(26, 16).cuboid(-1.5f, 0f, -1.5f, 3f, 9f, 3f),
				ModelTransform.of(2f, 12f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 64);
	}

	public static TexturedModelData miseryModel() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(64, 0).cuboid(-5f, 0f, -2.5f, 10f, 13f, 5f)
				.uv(0, 20).cuboid(-6f, -1f, -3f, 12f, 4f, 6f, new Dilation(0.2f)),
				ModelTransform.of(0f, -2f, 0f, 0.08f, 0f, 0f));
		ModelPartData p_hood = p_body.addChild("hood", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-5f, -10f, -5f, 10f, 10f, 10f)
				.uv(36, 20).cuboid(-4f, -12f, -3f, 8f, 3f, 7f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(88, 20).cuboid(-4f, -8.5f, -5.2f, 8f, 8f, 1f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail1 = p_body.addChild("tail1", ModelPartBuilder.create()
				.uv(94, 0).cuboid(-4.5f, 0f, -2.5f, 9f, 6f, 5f, new Dilation(0f, 0f, -0.2f)),
				ModelTransform.of(0f, 13f, 0f, 0.15f, 0f, 0f));
		ModelPartData p_tail2 = p_tail1.addChild("tail2", ModelPartBuilder.create()
				.uv(66, 20).cuboid(-3.5f, 0f, -2f, 7f, 6f, 4f, new Dilation(0f, 0f, -0.2f)),
				ModelTransform.of(0f, 6f, 0f, 0.15f, 0f, 0f));
		ModelPartData p_tail3 = p_tail2.addChild("tail3", ModelPartBuilder.create()
				.uv(106, 20).cuboid(-2f, 0f, -1.5f, 4f, 5f, 3f, new Dilation(0f, 0f, -0.3f)),
				ModelTransform.of(0f, 6f, 0f, 0.15f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(40, 0).cuboid(-2.25f, -1f, -1.5f, 3f, 16f, 3f, new Dilation(-0.25f, 0f, 0f))
				.uv(0, 30).cuboid(-2.3f, 15f, -1.7f, 1f, 4f, 1f, new Dilation(-0.1f, 0f, -0.1f))
				.uv(120, 20).cuboid(-1.1f, 14.75f, -1.7f, 1f, 5f, 1f, new Dilation(-0.1f, -0.25f, -0.1f))
				.uv(4, 30).cuboid(0.1f, 15f, -1.7f, 1f, 4f, 1f, new Dilation(-0.1f, 0f, -0.1f)),
				ModelTransform.of(-6f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(52, 0).cuboid(-0.75f, -1f, -1.5f, 3f, 16f, 3f, new Dilation(-0.25f, 0f, 0f))
				.uv(8, 30).cuboid(-0.7f, 15f, -1.7f, 1f, 4f, 1f, new Dilation(-0.1f, 0f, -0.1f))
				.uv(124, 20).cuboid(0.5f, 14.75f, -1.7f, 1f, 5f, 1f, new Dilation(-0.1f, -0.25f, -0.1f))
				.uv(12, 30).cuboid(1.7f, 15f, -1.7f, 1f, 4f, 1f, new Dilation(-0.1f, 0f, -0.1f)),
				ModelTransform.of(6f, 0f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 64);
	}

	public static TexturedModelData queenModel() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-4f, 0f, -2f, 8f, 12f, 4f)
				.uv(34, 28).cuboid(-6f, -1.25f, -3f, 12f, 2f, 6f, new Dilation(0f, -0.25f, 0f))
				.uv(86, 28).cuboid(-5f, -4f, 0.6f, 10f, 4f, 2f, new Dilation(0f, 0f, -0.4f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(24, 0).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(22, 16).cuboid(-4.5f, -8.9f, -4.5f, 9f, 2f, 9f, new Dilation(0f, -0.4f, 0f))
				.uv(24, 38).cuboid(-3.5f, -11.25f, -3.5f, 1f, 3f, 1f, new Dilation(0f, -0.25f, 0f))
				.uv(110, 28).cuboid(-1.5f, -12.15f, -4f, 1f, 4f, 1f, new Dilation(0f, -0.35f, 0f))
				.uv(114, 28).cuboid(0.5f, -12.15f, -4f, 1f, 4f, 1f, new Dilation(0f, -0.35f, 0f))
				.uv(28, 38).cuboid(2.5f, -11.25f, -3.5f, 1f, 3f, 1f, new Dilation(0f, -0.25f, 0f))
				.uv(0, 28).cuboid(-4f, -9.1f, -4.5f, 8f, 1f, 9f, new Dilation(0f, -0.1f, -0.3f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_hair_back = p_head.addChild("hair_back", ModelPartBuilder.create()
				.uv(0, 16).cuboid(-4.5f, 0f, -0.25f, 9f, 10f, 2f, new Dilation(0f, 0f, -0.25f)),
				ModelTransform.of(0f, -6f, 4f, 0.12f, 0f, 0f));
		ModelPartData p_hair_tail = p_hair_back.addChild("hair_tail", ModelPartBuilder.create()
				.uv(58, 16).cuboid(-4f, 0f, -0.9f, 8f, 9f, 2f, new Dilation(0f, 0f, -0.4f)),
				ModelTransform.of(0f, 10f, 0.5f, 0.15f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(56, 0).cuboid(-3.25f, -2f, -2f, 4f, 12f, 4f, new Dilation(-0.25f, 0f, 0f))
				.uv(0, 38).cuboid(-2.75f, 10f, -1.5f, 3f, 2f, 3f, new Dilation(-0.25f, 0f, 0f)),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(72, 0).cuboid(-0.75f, -2f, -2f, 4f, 12f, 4f, new Dilation(-0.25f, 0f, 0f))
				.uv(12, 38).cuboid(-0.25f, 10f, -1.5f, 3f, 2f, 3f, new Dilation(-0.25f, 0f, 0f)),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_skirt1 = p_body.addChild("skirt1", ModelPartBuilder.create()
				.uv(88, 0).cuboid(-5f, 0f, -3f, 10f, 7f, 6f),
				ModelTransform.of(0f, 11f, 0f, 0.05f, 0f, 0f));
		ModelPartData p_skirt2 = p_skirt1.addChild("skirt2", ModelPartBuilder.create()
				.uv(78, 16).cuboid(-4f, 0f, -2.5f, 8f, 6f, 5f),
				ModelTransform.of(0f, 7f, 0f, 0.1f, 0f, 0f));
		ModelPartData p_skirt3 = p_skirt2.addChild("skirt3", ModelPartBuilder.create()
				.uv(70, 28).cuboid(-2.5f, 0f, -1.5f, 5f, 5f, 3f),
				ModelTransform.of(0f, 6f, 0f, 0.15f, 0f, 0f));
		ModelPartData p_orbs = p_body.addChild("orbs", ModelPartBuilder.create()
				.uv(32, 38).cuboid(-11f, -1f, -1f, 2f, 2f, 2f)
				.uv(40, 38).cuboid(9f, -1f, -1f, 2f, 2f, 2f)
				.uv(48, 38).cuboid(-1f, -1f, 9f, 2f, 2f, 2f),
				ModelTransform.of(0f, 4f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 64);
	}

	@Override
	public void setAngles(DoloraEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		phase = e.phase();
		float yaw = headYaw * MathHelper.RADIANS_PER_DEGREE, pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		float swing = MathHelper.cos(limbAngle * 0.6662f) * Math.min(1f, limbDistance);
		float attack = MathHelper.sin(handSwingProgress * MathHelper.PI);
		boolean draining = e.drainId() >= 0;
		if (phase == DoloraEntity.HUMAN) {
			ModelPart body = counselor.getChild("body"), head = body.getChild("head");
			head.yaw = yaw;
			head.pitch = pitch;
			head.roll = MathHelper.sin(age * 0.05f) * 0.06f;       // a patronising little head tilt
			counselor.getChild("right_leg").pitch = swing * 1.1f;
			counselor.getChild("left_leg").pitch = -swing * 1.1f;
			ModelPart ra = counselor.getChild("right_arm"), la = counselor.getChild("left_arm");
			ra.pitch = -swing * 0.7f - attack * 1.6f;
			ra.roll = 0.05f;
			la.pitch = -0.55f + MathHelper.sin(age * 0.06f) * 0.04f;  // clipboard held up to read from
			la.yaw = 0.3f;
			body.getChild("skirt").pitch = Math.abs(swing) * 0.08f;
		} else if (phase == DoloraEntity.WRAITH) {
			ModelPart body = misery.getChild("body");
			ModelPart head = body.getChild("head");
			head.yaw = yaw * 0.7f;
			head.pitch = pitch * 0.7f;
			body.getChild("hood").yaw = head.yaw;
			body.getChild("hood").pitch = head.pitch;
			body.pivotY = -2f + MathHelper.sin(age * 0.08f) * 1.2f;
			ModelPart t1 = body.getChild("tail1"), t2 = t1.getChild("tail2"), t3 = t2.getChild("tail3");
			t1.pitch = 0.15f + MathHelper.sin(age * 0.12f) * 0.1f;
			t2.pitch = 0.15f + MathHelper.sin(age * 0.12f - 0.8f) * 0.14f;
			t3.pitch = 0.2f + MathHelper.sin(age * 0.12f - 1.6f) * 0.2f;
			t1.roll = MathHelper.sin(age * 0.09f) * 0.08f;
			t2.roll = MathHelper.sin(age * 0.09f - 0.8f) * 0.12f;
			ModelPart ra = misery.getChild("right_arm"), la = misery.getChild("left_arm");
			float reach = draining ? 1f : attack;
			ra.pivotY = la.pivotY = body.pivotY + 2f;
			ra.pitch = -0.3f + MathHelper.sin(age * 0.1f) * 0.15f - reach * 1.2f;
			la.pitch = -0.3f + MathHelper.cos(age * 0.11f) * 0.15f - reach * 1.2f;
			ra.roll = 0.25f + (draining ? MathHelper.sin(age * 0.6f) * 0.08f : 0f);
			la.roll = -0.25f - (draining ? MathHelper.sin(age * 0.6f) * 0.08f : 0f);
		} else {
			ModelPart body = queen.getChild("body"), head = body.getChild("head");
			head.yaw = yaw;
			head.pitch = pitch;
			body.pivotY = MathHelper.sin(age * 0.07f) * 1.0f;
			ModelPart hb = head.getChild("hair_back");
			hb.pitch = 0.15f + MathHelper.sin(age * 0.09f) * 0.06f - pitch * 0.5f;
			hb.getChild("hair_tail").pitch = 0.2f + MathHelper.sin(age * 0.09f - 1f) * 0.1f;
			ModelPart s1 = body.getChild("skirt1"), s2 = s1.getChild("skirt2"), s3 = s2.getChild("skirt3");
			s1.pitch = 0.05f + MathHelper.sin(age * 0.1f) * 0.05f;
			s2.pitch = 0.1f + MathHelper.sin(age * 0.1f - 0.8f) * 0.1f;
			s3.pitch = 0.15f + MathHelper.sin(age * 0.1f - 1.6f) * 0.15f;
			body.getChild("orbs").yaw = age * 0.09f;
			body.getChild("orbs").pivotY = 4f + MathHelper.sin(age * 0.15f) * 0.8f;
			ModelPart ra = queen.getChild("right_arm"), la = queen.getChild("left_arm");
			float cast = draining ? 1f : attack;
			ra.pivotY = la.pivotY = 2f + body.pivotY;
			ra.pitch = -0.2f - cast * 1.4f + MathHelper.sin(age * 0.07f) * 0.08f;
			la.pitch = -0.2f - cast * 1.4f + MathHelper.cos(age * 0.07f) * 0.08f;
			ra.roll = 0.35f + cast * 0.3f;
			la.roll = -0.35f - cast * 0.3f;
		}
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
		(phase == DoloraEntity.HUMAN ? counselor : phase == DoloraEntity.WRAITH ? misery : queen).render(matrices, vertices, light, overlay, color);
	}
}
