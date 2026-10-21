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
				.uv(2, 2).cuboid(-4f, 0f, -2.15f, 8f, 7.5f, 4.3f)
				.uv(30, 2).cuboid(-3.1f, 7.4f, -1.9f, 6.2f, 3.7f, 3.8f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(53, 2).cuboid(-4f, -8f, -3.5f, 8f, 8f, 7f)
				.uv(86, 2).cuboid(-3.45f, -5.35f, -3.75f, 3.1f, 0.25f, 0.3f)
				.uv(96, 2).cuboid(-3.45f, -4.15f, -3.75f, 3.1f, 0.25f, 0.3f)
				.uv(106, 2).cuboid(-3.45f, -5.35f, -3.75f, 0.25f, 1.45f, 0.3f)
				.uv(111, 2).cuboid(-0.6f, -5.35f, -3.75f, 0.25f, 1.45f, 0.3f)
				.uv(116, 2).cuboid(0.4f, -5.35f, -3.75f, 3.1f, 0.25f, 0.3f)
				.uv(126, 2).cuboid(0.4f, -4.15f, -3.75f, 3.1f, 0.25f, 0.3f)
				.uv(136, 2).cuboid(0.4f, -5.35f, -3.75f, 0.25f, 1.45f, 0.3f)
				.uv(141, 2).cuboid(3.25f, -5.35f, -3.75f, 0.25f, 1.45f, 0.3f)
				.uv(146, 2).cuboid(-0.4f, -4.85f, -3.8f, 0.8f, 0.25f, 0.35f)
				.uv(152, 2).cuboid(-4.15f, -2.7f, -3.8f, 0.45f, 0.55f, 0.6f)
				.uv(158, 2).cuboid(-4.3f, -1.9f, -3.75f, 0.75f, 1.8f, 0.5f)
				.uv(164, 2).cuboid(-4.5f, -0.4f, -3.75f, 1.15f, 0.5f, 0.5f)
				.uv(171, 2).cuboid(3.9f, -2.7f, -3.8f, 0.45f, 0.55f, 0.6f)
				.uv(177, 2).cuboid(3.75f, -1.9f, -3.75f, 0.75f, 1.8f, 0.5f)
				.uv(183, 2).cuboid(3.55f, -0.4f, -3.75f, 1.15f, 0.5f, 0.5f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_red_hair = p_head.addChild("red_hair", ModelPartBuilder.create()
				.uv(190, 2).cuboid(-4.3f, -9f, -3.8f, 8.6f, 2f, 7.6f)
				.uv(226, 2).cuboid(-4.25f, -7f, 2.4f, 8.5f, 7f, 1.4f)
				.uv(2, 20).cuboid(-4.2f, -7.8f, -3.95f, 8.4f, 1.5f, 0.7f)
				.uv(24, 20).cuboid(-4.5f, -7f, -2.6f, 1f, 4.5f, 5.5f)
				.uv(40, 20).cuboid(3.5f, -7f, -2.6f, 1f, 4.5f, 5.5f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_swept_spike_0 = p_red_hair.addChild("swept_spike_0", ModelPartBuilder.create()
				.uv(56, 20).cuboid(-1.5f, -4f, -3.4f, 3f, 4f, 6.8f),
				ModelTransform.of(-3f, -8.8f, 0f, 0f, 0f, 0.35f));
		ModelPartData p_swept_spike_1 = p_red_hair.addChild("swept_spike_1", ModelPartBuilder.create()
				.uv(79, 20).cuboid(-1.5f, -3.8f, -3.4f, 3f, 3.8f, 6.8f),
				ModelTransform.of(0f, -9f, 0f, 0f, 0f, 0.25f));
		ModelPartData p_swept_spike_2 = p_red_hair.addChild("swept_spike_2", ModelPartBuilder.create()
				.uv(102, 20).cuboid(-1f, -3f, -3.4f, 2f, 3f, 6.8f),
				ModelTransform.of(2.2f, -8.4f, 0f, 0f, 0f, 0.3f));
		ModelPartData p_skirt = p_body.addChild("skirt", ModelPartBuilder.create()
				.uv(123, 20).cuboid(-4f, 0f, -2.1f, 8f, 3.8f, 4.2f),
				ModelTransform.of(0f, 11f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(151, 20).cuboid(-2f, -1f, -1.7f, 3f, 8.6f, 3.4f)
				.uv(167, 20).cuboid(-2f, 7.5f, -1.75f, 3f, 0.8f, 3.5f)
				.uv(183, 20).cuboid(-1.9f, 8.3f, -1.6f, 2.8f, 2.6f, 3.2f),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(198, 20).cuboid(-1f, -1f, -1.7f, 3f, 8.6f, 3.4f)
				.uv(214, 20).cuboid(-1f, 7.5f, -1.75f, 3f, 0.8f, 3.5f)
				.uv(230, 20).cuboid(-0.9f, 8.3f, -1.6f, 2.8f, 2.6f, 3.2f),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_clipboard = p_left_arm.addChild("clipboard", ModelPartBuilder.create(),
				ModelTransform.of(1.2f, 9f, -1f, -0.9f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(2, 35).cuboid(-1.65f, 0f, -1.5f, 3.3f, 9f, 3f)
				.uv(18, 35).cuboid(-1.75f, 8.8f, -1.7f, 3.5f, 1.8f, 3.4f)
				.uv(35, 35).cuboid(-1.75f, 9.7f, -2.7f, 3.5f, 1f, 4.4f)
				.uv(54, 35).cuboid(-1.4f, 10.4f, 0.6f, 2.8f, 1.5f, 1f),
				ModelTransform.of(-2f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(65, 35).cuboid(-1.65f, 0f, -1.5f, 3.3f, 9f, 3f)
				.uv(81, 35).cuboid(-1.75f, 8.8f, -1.7f, 3.5f, 1.8f, 3.4f)
				.uv(98, 35).cuboid(-1.75f, 9.7f, -2.7f, 3.5f, 1f, 4.4f)
				.uv(117, 35).cuboid(-1.4f, 10.4f, 0.6f, 2.8f, 1.5f, 1f),
				ModelTransform.of(2f, 12f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 256);
	}

	public static TexturedModelData miseryModel() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-4.8f, 0f, -2.5f, 9.6f, 3.3f, 5f)
				.uv(35, 2).cuboid(-3.8f, 3.3f, -2f, 7.6f, 5.7f, 4f)
				.uv(62, 2).cuboid(-2.5f, 9f, -1.5f, 5f, 4.1f, 3f),
				ModelTransform.of(0f, -2f, 0f, 0.08f, 0f, 0f));
		ModelPartData p_hood = p_body.addChild("hood", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(81, 2).cuboid(-4.5f, -8f, -3.5f, 9f, 8f, 7f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_ghost_hair_spike_0 = p_head.addChild("ghost_hair_spike_0", ModelPartBuilder.create()
				.uv(116, 2).cuboid(-1f, -5f, 0f, 2f, 5f, 3f),
				ModelTransform.of(-4f, -8f, -1.4f, 0f, 0f, -0.45f));
		ModelPartData p_ghost_hair_spike_1 = p_head.addChild("ghost_hair_spike_1", ModelPartBuilder.create()
				.uv(129, 2).cuboid(-1f, -3f, 0f, 2f, 3f, 3f),
				ModelTransform.of(-1.8f, -8f, -1.5f, 0f, 0f, -0.35f));
		ModelPartData p_ghost_hair_spike_2 = p_head.addChild("ghost_hair_spike_2", ModelPartBuilder.create()
				.uv(142, 2).cuboid(-0.75f, -4f, 0f, 1.5f, 4f, 3f),
				ModelTransform.of(3f, -8f, -1.5f, 0f, 0f, 0.3f));
		ModelPartData p_tail1 = p_body.addChild("tail1", ModelPartBuilder.create()
				.uv(154, 2).cuboid(-2.35f, 0f, -1.3f, 4.7f, 6.3f, 2.6f),
				ModelTransform.of(0f, 13f, 0f, 0.03f, 0f, 0f));
		ModelPartData p_tail2 = p_tail1.addChild("tail2", ModelPartBuilder.create()
				.uv(172, 2).cuboid(-3f, 0f, -1.3f, 3.2f, 6.3f, 2.6f),
				ModelTransform.of(0f, 6f, 0f, 0.03f, 0f, 0f));
		ModelPartData p_tail3 = p_tail2.addChild("tail3", ModelPartBuilder.create()
				.uv(187, 2).cuboid(-0.85f, 0f, -1.3f, 1.7f, 6.3f, 2.6f)
				.uv(199, 2).cuboid(-0.5f, 5.3f, -1.3f, 4f, 1.5f, 2.6f)
				.uv(216, 2).cuboid(2.2f, 3f, -1.3f, 1.4f, 3.5f, 2.6f),
				ModelTransform.of(0f, 6f, 0f, 0.03f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(227, 2).cuboid(-2.5f, -1.1f, -2.3f, 4f, 2.5f, 4.6f)
				.uv(2, 20).cuboid(-2f, 1f, -1.9f, 3f, 8f, 3.8f)
				.uv(19, 20).cuboid(-2.5f, 8.5f, -2.1f, 4f, 2.5f, 4.2f)
				.uv(39, 20).cuboid(-2.5f, 10.8f, -2.2f, 1f, 2.1f, 1.4f)
				.uv(47, 20).cuboid(-1.15f, 10.8f, -2.2f, 1f, 2.1f, 1.4f)
				.uv(55, 20).cuboid(0.2f, 10.8f, -2.2f, 1f, 2.1f, 1.4f),
				ModelTransform.of(-6f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(63, 20).cuboid(-1.5f, -1.1f, -2.3f, 4f, 2.5f, 4.6f)
				.uv(84, 20).cuboid(-1f, 1f, -1.9f, 3f, 8f, 3.8f)
				.uv(101, 20).cuboid(-1.5f, 8.5f, -2.1f, 4f, 2.5f, 4.2f)
				.uv(121, 20).cuboid(-1.5f, 10.8f, -2.2f, 1f, 2.1f, 1.4f)
				.uv(129, 20).cuboid(-0.15f, 10.8f, -2.2f, 1f, 2.1f, 1.4f)
				.uv(137, 20).cuboid(1.2f, 10.8f, -2.2f, 1f, 2.1f, 1.4f),
				ModelTransform.of(6f, 0f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 256);
	}

	public static TexturedModelData queenModel() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-4f, 0f, -2.1f, 8f, 5.7f, 4.2f)
				.uv(30, 2).cuboid(-2.7f, 5.6f, -1.5f, 5.4f, 3f, 3f)
				.uv(50, 2).cuboid(-3.6f, 8.5f, -1.9f, 7.2f, 1.3f, 3.8f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(75, 2).cuboid(-4f, -8f, -3.5f, 8f, 8f, 7f)
				.uv(108, 2).cuboid(-3.45f, -5.35f, -3.75f, 3.1f, 0.25f, 0.3f)
				.uv(118, 2).cuboid(-3.45f, -4.15f, -3.75f, 3.1f, 0.25f, 0.3f)
				.uv(128, 2).cuboid(-3.45f, -5.35f, -3.75f, 0.25f, 1.45f, 0.3f)
				.uv(133, 2).cuboid(-0.6f, -5.35f, -3.75f, 0.25f, 1.45f, 0.3f)
				.uv(138, 2).cuboid(0.4f, -5.35f, -3.75f, 3.1f, 0.25f, 0.3f)
				.uv(148, 2).cuboid(0.4f, -4.15f, -3.75f, 3.1f, 0.25f, 0.3f)
				.uv(158, 2).cuboid(0.4f, -5.35f, -3.75f, 0.25f, 1.45f, 0.3f)
				.uv(163, 2).cuboid(3.25f, -5.35f, -3.75f, 0.25f, 1.45f, 0.3f)
				.uv(168, 2).cuboid(-0.4f, -4.85f, -3.8f, 0.8f, 0.25f, 0.35f)
				.uv(174, 2).cuboid(-4.15f, -2.7f, -3.8f, 0.45f, 0.55f, 0.6f)
				.uv(180, 2).cuboid(-4.3f, -1.9f, -3.75f, 0.75f, 1.8f, 0.5f)
				.uv(186, 2).cuboid(-4.5f, -0.4f, -3.75f, 1.15f, 0.5f, 0.5f)
				.uv(193, 2).cuboid(3.9f, -2.7f, -3.8f, 0.45f, 0.55f, 0.6f)
				.uv(199, 2).cuboid(3.75f, -1.9f, -3.75f, 0.75f, 1.8f, 0.5f)
				.uv(205, 2).cuboid(3.55f, -0.4f, -3.75f, 1.15f, 0.5f, 0.5f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_hair_back = p_head.addChild("hair_back", ModelPartBuilder.create(),
				ModelTransform.of(0f, -6f, 4f, 0.12f, 0f, 0f));
		ModelPartData p_hair_tail = p_hair_back.addChild("hair_tail", ModelPartBuilder.create(),
				ModelTransform.of(0f, 10f, 0.5f, 0.15f, 0f, 0f));
		ModelPartData p_red_hair = p_head.addChild("red_hair", ModelPartBuilder.create()
				.uv(212, 2).cuboid(-4.3f, -9f, -3.8f, 8.6f, 2f, 7.6f)
				.uv(2, 20).cuboid(-4.25f, -7f, 2.4f, 8.5f, 7f, 1.4f)
				.uv(25, 20).cuboid(-4.2f, -7.8f, -3.95f, 8.4f, 1.5f, 0.7f)
				.uv(47, 20).cuboid(-4.5f, -7f, -2.6f, 1f, 4.5f, 5.5f)
				.uv(63, 20).cuboid(3.5f, -7f, -2.6f, 1f, 4.5f, 5.5f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_swept_spike_0 = p_red_hair.addChild("swept_spike_0", ModelPartBuilder.create()
				.uv(79, 20).cuboid(-1.5f, -4f, -3.4f, 3f, 4f, 6.8f),
				ModelTransform.of(-3f, -8.8f, 0f, 0f, 0f, 0.35f));
		ModelPartData p_swept_spike_1 = p_red_hair.addChild("swept_spike_1", ModelPartBuilder.create()
				.uv(102, 20).cuboid(-1.5f, -3.8f, -3.4f, 3f, 3.8f, 6.8f),
				ModelTransform.of(0f, -9f, 0f, 0f, 0f, 0.25f));
		ModelPartData p_swept_spike_2 = p_red_hair.addChild("swept_spike_2", ModelPartBuilder.create()
				.uv(125, 20).cuboid(-1f, -3f, -3.4f, 2f, 3f, 6.8f),
				ModelTransform.of(2.2f, -8.4f, 0f, 0f, 0f, 0.3f));
		ModelPartData p_skirt1 = p_body.addChild("skirt1", ModelPartBuilder.create()
				.uv(146, 20).cuboid(-3.8f, -1.7f, -2f, 7.6f, 3.2f, 4f),
				ModelTransform.of(0f, 11f, 0f, 0.05f, 0f, 0f));
		ModelPartData p_skirt2 = p_skirt1.addChild("skirt2", ModelPartBuilder.create(),
				ModelTransform.of(0f, 7f, 0f, 0.1f, 0f, 0f));
		ModelPartData p_skirt3 = p_skirt2.addChild("skirt3", ModelPartBuilder.create(),
				ModelTransform.of(0f, 6f, 0f, 0.15f, 0f, 0f));
		ModelPartData p_orbs = p_body.addChild("orbs", ModelPartBuilder.create(),
				ModelTransform.of(0f, 4f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_evil_leg = p_body.addChild("right_evil_leg", ModelPartBuilder.create()
				.uv(173, 20).cuboid(-1.6f, 0f, -1.5f, 3.2f, 3.2f, 3f)
				.uv(189, 20).cuboid(-1.7f, 2.7f, -1.7f, 3.4f, 8.7f, 3.4f)
				.uv(206, 20).cuboid(-1.8f, 10f, -2.7f, 3.6f, 1.6f, 4.5f)
				.uv(226, 20).cuboid(-1.6f, 10.8f, 0.7f, 3.2f, 1.5f, 1f),
				ModelTransform.of(-1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_evil_leg = p_body.addChild("left_evil_leg", ModelPartBuilder.create()
				.uv(238, 20).cuboid(-1.6f, 0f, -1.5f, 3.2f, 3.2f, 3f)
				.uv(2, 36).cuboid(-1.7f, 2.7f, -1.7f, 3.4f, 8.7f, 3.4f)
				.uv(19, 36).cuboid(-1.8f, 10f, -2.7f, 3.6f, 1.6f, 4.5f)
				.uv(39, 36).cuboid(-1.6f, 10.8f, 0.7f, 3.2f, 1.5f, 1f),
				ModelTransform.of(1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(51, 36).cuboid(-2f, -1f, -1.7f, 3f, 4.5f, 3.4f)
				.uv(67, 36).cuboid(-2f, 3.4f, -1.65f, 3f, 7.4f, 3.3f)
				.uv(83, 36).cuboid(-2.05f, 10.6f, -1.75f, 3.1f, 1.7f, 3.5f),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(100, 36).cuboid(-1f, -1f, -1.7f, 3f, 4.5f, 3.4f)
				.uv(116, 36).cuboid(-1f, 3.4f, -1.65f, 3f, 7.4f, 3.3f)
				.uv(132, 36).cuboid(-1.05f, 10.6f, -1.75f, 3.1f, 1.7f, 3.5f),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 256);
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
