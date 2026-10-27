package com.phantompowers.client;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

/** Your model (from your updated jar). */
@SuppressWarnings("unused")
public final class SamDanceModelData {
	private SamDanceModelData() {}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(37, 2).cuboid(-4.3f, -8.5f, -4.3f, 8.6f, 1.5f, 8.6f)
				.uv(75, 2).cuboid(-4.25f, -7.5f, 2.6f, 8.5f, 8f, 1.9f)
				.uv(99, 2).cuboid(-4.6f, -7.5f, -3.8f, 1.2f, 8f, 7f)
				.uv(119, 2).cuboid(-4.68f, -4.3f, -3.6f, 1.36f, 0.75f, 6.5f)
				.uv(138, 2).cuboid(-5.15f, -7.7f, -1.7f, 1.8f, 2.5f, 4.4f)
				.uv(154, 2).cuboid(-5.2f, -5.9f, -1.8f, 1.9f, 0.45f, 4.6f)
				.uv(170, 2).cuboid(-3.05f, -1.5f, 3.5f, 1.5f, 5.8f, 1.6f)
				.uv(180, 2).cuboid(3.4f, -7.5f, -3.8f, 1.2f, 8f, 7f)
				.uv(200, 2).cuboid(3.32f, -4.3f, -3.6f, 1.36f, 0.75f, 6.5f)
				.uv(219, 2).cuboid(3.35f, -7.7f, -1.7f, 1.8f, 2.5f, 4.4f)
				.uv(235, 2).cuboid(3.3f, -5.9f, -1.8f, 1.9f, 0.45f, 4.6f)
				.uv(2, 21).cuboid(1.55f, -1.5f, 3.5f, 1.5f, 5.8f, 1.6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_fringe = p_head.addChild("right_fringe", ModelPartBuilder.create(),
				ModelTransform.of(-1.7f, -7f, -4.35f, 0f, 0f, -0.12f));
		ModelPartData p_left_fringe = p_head.addChild("left_fringe", ModelPartBuilder.create(),
				ModelTransform.of(1.7f, -7f, -4.35f, 0f, 0f, 0.12f));
		ModelPartData p_right_formal_fringe = p_head.addChild("right_formal_fringe", ModelPartBuilder.create()
				.uv(232, 21).cuboid(-2f, -0.5f, -0.2f, 4f, 1.7f, 0.6f),
				ModelTransform.of(-1.7f, -7f, -4.35f, 0f, 0f, 0.22f));
		ModelPartData p_right_goth_spike_0 = p_head.addChild("right_goth_spike_0", ModelPartBuilder.create()
				.uv(245, 21).cuboid(-0.65f, -2.9f, -0.8f, 1.3f, 3.5f, 1.6f)
				.uv(2, 36).cuboid(-0.4f, -2.6f, -0.95f, 0.7f, 0.7f, 0.2f),
				ModelTransform.of(-2.2f, -7.7f, 1f, 0f, -0.3f, 0.45f));
		ModelPartData p_right_goth_spike_1 = p_head.addChild("right_goth_spike_1", ModelPartBuilder.create()
				.uv(7, 36).cuboid(-0.65f, -2.9f, -0.8f, 1.3f, 3.5f, 1.6f)
				.uv(16, 36).cuboid(-0.4f, -2.6f, -0.95f, 0.7f, 0.7f, 0.2f),
				ModelTransform.of(-3.1f, -7.45f, 0.2f, 0f, -0.3f, 0.63f));
		ModelPartData p_right_goth_spike_2 = p_head.addChild("right_goth_spike_2", ModelPartBuilder.create()
				.uv(21, 36).cuboid(-0.65f, -2.9f, -0.8f, 1.3f, 3.5f, 1.6f)
				.uv(30, 36).cuboid(-0.4f, -2.6f, -0.95f, 0.7f, 0.7f, 0.2f),
				ModelTransform.of(-4f, -7.2f, -0.6f, 0f, -0.3f, 0.81f));
		ModelPartData p_left_formal_fringe = p_head.addChild("left_formal_fringe", ModelPartBuilder.create()
				.uv(35, 36).cuboid(-2f, -0.5f, -0.2f, 4f, 1.7f, 0.6f),
				ModelTransform.of(1.7f, -7f, -4.35f, 0f, 0f, -0.22f));
		ModelPartData p_left_goth_spike_0 = p_head.addChild("left_goth_spike_0", ModelPartBuilder.create()
				.uv(48, 36).cuboid(-0.65f, -2.9f, -0.8f, 1.3f, 3.5f, 1.6f)
				.uv(57, 36).cuboid(-0.4f, -2.6f, -0.95f, 0.7f, 0.7f, 0.2f),
				ModelTransform.of(2.2f, -7.7f, 1f, 0f, 0.3f, -0.45f));
		ModelPartData p_left_goth_spike_1 = p_head.addChild("left_goth_spike_1", ModelPartBuilder.create()
				.uv(62, 36).cuboid(-0.65f, -2.9f, -0.8f, 1.3f, 3.5f, 1.6f)
				.uv(71, 36).cuboid(-0.4f, -2.6f, -0.95f, 0.7f, 0.7f, 0.2f),
				ModelTransform.of(3.1f, -7.45f, 0.2f, 0f, 0.3f, -0.63f));
		ModelPartData p_left_goth_spike_2 = p_head.addChild("left_goth_spike_2", ModelPartBuilder.create()
				.uv(76, 36).cuboid(-0.65f, -2.9f, -0.8f, 1.3f, 3.5f, 1.6f)
				.uv(85, 36).cuboid(-0.4f, -2.6f, -0.95f, 0.7f, 0.7f, 0.2f),
				ModelTransform.of(4f, -7.2f, -0.6f, 0f, 0.3f, -0.81f));
		ModelPartData p_hat = r.addChild("hat", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(12, 21).cuboid(-3.5f, 0.1f, -2f, 7f, 7.6f, 4f)
				.uv(37, 21).cuboid(-1.1f, 0.5f, -2.25f, 2.2f, 6.8f, 0.35f)
				.uv(46, 21).cuboid(-3.65f, 7.1f, -2.1f, 7.3f, 0.8f, 4.2f)
				.uv(72, 21).cuboid(-2f, 12.3f, -3.08f, 0.6f, 0.14f, 0.2f)
				.uv(77, 21).cuboid(-1.77f, 12.08f, -3.08f, 0.14f, 0.58f, 0.2f)
				.uv(81, 21).cuboid(1.5f, 15.4f, -3.39f, 0.6f, 0.14f, 0.2f)
				.uv(86, 21).cuboid(1.73f, 15.18f, -3.39f, 0.14f, 0.58f, 0.2f)
				.uv(90, 21).cuboid(-1.6f, 19.4f, -3.79f, 0.6f, 0.14f, 0.2f)
				.uv(95, 21).cuboid(-1.37f, 19.18f, -3.79f, 0.14f, 0.58f, 0.2f)
				.uv(99, 21).cuboid(1.7f, 21.6f, -4.01f, 0.6f, 0.14f, 0.2f)
				.uv(104, 21).cuboid(1.93f, 21.38f, -4.01f, 0.14f, 0.58f, 0.2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_purple_gown = p_body.addChild("purple_gown", ModelPartBuilder.create()
				.uv(90, 36).cuboid(-3.9f, 0f, -2.4f, 7.8f, 1.06f, 4.8f)
				.uv(119, 36).cuboid(-4.025f, 1f, -2.5f, 8.05f, 1.06f, 5f)
				.uv(149, 36).cuboid(-4.15f, 2f, -2.6f, 8.3f, 1.06f, 5.2f)
				.uv(179, 36).cuboid(-4.275f, 3f, -2.7f, 8.55f, 1.06f, 5.4f)
				.uv(210, 36).cuboid(-4.4f, 4f, -2.8f, 8.8f, 1.06f, 5.6f)
				.uv(2, 46).cuboid(-4.525f, 5f, -2.9f, 9.05f, 1.06f, 5.8f)
				.uv(35, 46).cuboid(-4.65f, 6f, -3f, 9.3f, 1.06f, 6f)
				.uv(69, 46).cuboid(-4.775f, 7f, -3.1f, 9.55f, 1.06f, 6.2f)
				.uv(104, 46).cuboid(-4.9f, 8f, -3.2f, 9.8f, 1.06f, 6.4f)
				.uv(140, 46).cuboid(-5.025f, 9f, -3.3f, 10.05f, 1.06f, 6.6f)
				.uv(177, 46).cuboid(-5.15f, 10f, -3.4f, 10.3f, 1.06f, 6.8f)
				.uv(215, 46).cuboid(-5.275f, 11f, -3.5f, 10.55f, 1.06f, 7f)
				.uv(2, 58).cuboid(-5.4f, 12f, -3.6f, 10.8f, 1.06f, 7.2f)
				.uv(41, 58).cuboid(-5.525f, 13f, -3.7f, 11.05f, 1.06f, 7.4f)
				.uv(81, 58).cuboid(-5.65f, 14f, -3.8f, 11.3f, 1.06f, 7.6f)
				.uv(122, 58).cuboid(-5.775f, 15f, -3.9f, 11.55f, 1.06f, 7.8f),
				ModelTransform.of(0f, 7.8f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_gown_fold = p_body.addChild("right_gown_fold", ModelPartBuilder.create()
				.uv(164, 58).cuboid(-0.35f, 0f, -0.15f, 0.7f, 14.7f, 0.35f),
				ModelTransform.of(-2.5f, 8.1f, -2.8f, -0.12f, 0f, 0.1f));
		ModelPartData p_left_gown_fold = p_body.addChild("left_gown_fold", ModelPartBuilder.create()
				.uv(170, 58).cuboid(-0.35f, 0f, -0.15f, 0.7f, 14.7f, 0.35f),
				ModelTransform.of(2.5f, 8.1f, -2.8f, -0.12f, 0f, -0.1f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(108, 21).cuboid(-2f, -2f, -1.5f, 3f, 4f, 3f)
				.uv(123, 21).cuboid(-2.05f, 2f, -1.55f, 3.1f, 6.5f, 3.1f)
				.uv(139, 21).cuboid(-2.1f, 7.6f, -1.6f, 3.2f, 0.6f, 3.2f)
				.uv(155, 21).cuboid(-2f, 8.3f, -1.5f, 3f, 1.1f, 3f),
				ModelTransform.of(-4.7f, 2.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(170, 21).cuboid(-1f, -2f, -1.5f, 3f, 4f, 3f)
				.uv(185, 21).cuboid(-1.05f, 2f, -1.55f, 3.1f, 6.5f, 3.1f)
				.uv(201, 21).cuboid(-1.1f, 7.6f, -1.6f, 3.2f, 0.6f, 3.2f)
				.uv(217, 21).cuboid(-1f, 8.3f, -1.5f, 3f, 1.1f, 3f),
				ModelTransform.of(4.7f, 2.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_formal_handbag = p_left_arm.addChild("formal_handbag", ModelPartBuilder.create()
				.uv(176, 58).cuboid(-1.7f, 0.7f, -1f, 3.4f, 2.8f, 2f)
				.uv(190, 58).cuboid(-1.8f, 1f, -0.7f, 0.4f, 2.2f, 1.4f)
				.uv(197, 58).cuboid(-1.1f, -0.6f, -0.4f, 0.35f, 1.5f, 0.8f)
				.uv(203, 58).cuboid(0.75f, -0.6f, -0.4f, 0.35f, 1.5f, 0.8f)
				.uv(209, 58).cuboid(-1.1f, -0.65f, -0.4f, 2.2f, 0.4f, 0.8f)
				.uv(218, 58).cuboid(-0.7f, 1.6f, -1.2f, 1.4f, 0.8f, 0.25f)
				.uv(225, 58).cuboid(-0.66f, 1.3f, -1.3f, 0.32f, 0.55f, 0.25f)
				.uv(230, 58).cuboid(-0.43f, 1.85f, -1.48f, 0.16f, 0.14f, 0.12f)
				.uv(234, 58).cuboid(0.34f, 1.3f, -1.3f, 0.32f, 0.55f, 0.25f)
				.uv(239, 58).cuboid(0.27f, 1.85f, -1.48f, 0.16f, 0.14f, 0.12f),
				ModelTransform.of(1.8f, 10f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create(),
				ModelTransform.of(-1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create(),
				ModelTransform.of(1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_sleeve = r.addChild("right_sleeve", ModelPartBuilder.create(),
				ModelTransform.of(-5f, 2.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_sleeve = r.addChild("left_sleeve", ModelPartBuilder.create(),
				ModelTransform.of(5f, 2.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_pants = r.addChild("right_pants", ModelPartBuilder.create(),
				ModelTransform.of(-1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_pants = r.addChild("left_pants", ModelPartBuilder.create(),
				ModelTransform.of(1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_jacket = r.addChild("jacket", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_ear = r.addChild("ear", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_cloak = r.addChild("cloak", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 256);
	}
}
