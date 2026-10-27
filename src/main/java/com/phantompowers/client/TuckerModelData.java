package com.phantompowers.client;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

/** Your model (from your updated jar). */
@SuppressWarnings("unused")
public final class TuckerModelData {
	private TuckerModelData() {}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(37, 2).cuboid(-4.15f, -5f, -0.1f, 8.3f, 4.2f, 4.3f)
				.uv(66, 2).cuboid(-3.4f, -8.85f, -3.4f, 6.8f, 0.35f, 6.8f)
				.uv(97, 2).cuboid(-4.3f, -5.8f, 0.1f, 8.6f, 1.5f, 4.2f)
				.uv(126, 2).cuboid(-2.8f, -5.4f, -4.35f, 1.75f, 0.4f, 0.35f)
				.uv(134, 2).cuboid(1.05f, -5.4f, -4.35f, 1.75f, 0.4f, 0.35f)
				.uv(142, 2).cuboid(-3.6f, -4.35f, -4.5f, 3.3f, 0.45f, 0.5f)
				.uv(153, 2).cuboid(-3.55f, -2.45f, -4.5f, 3.2f, 0.4f, 0.5f)
				.uv(164, 2).cuboid(-3.55f, -4.15f, -4.5f, 0.35f, 1.95f, 0.5f)
				.uv(169, 2).cuboid(-0.7f, -4.15f, -4.5f, 0.35f, 1.95f, 0.5f)
				.uv(174, 2).cuboid(-4.18f, -4.15f, -4.1f, 0.36f, 0.4f, 4.7f)
				.uv(188, 2).cuboid(-4.3f, -3.9f, -0.8f, 0.6f, 1.65f, 1.4f)
				.uv(195, 2).cuboid(0.3f, -4.35f, -4.5f, 3.3f, 0.45f, 0.5f)
				.uv(206, 2).cuboid(0.35f, -2.45f, -4.5f, 3.2f, 0.4f, 0.5f)
				.uv(217, 2).cuboid(0.35f, -4.15f, -4.5f, 0.35f, 1.95f, 0.5f)
				.uv(222, 2).cuboid(3.2f, -4.15f, -4.5f, 0.35f, 1.95f, 0.5f)
				.uv(227, 2).cuboid(3.82f, -4.15f, -4.1f, 0.36f, 0.4f, 4.7f)
				.uv(241, 2).cuboid(3.7f, -3.9f, -0.8f, 0.6f, 1.65f, 1.4f)
				.uv(248, 2).cuboid(-0.5f, -3.9f, -4.55f, 1f, 0.35f, 0.55f)
				.uv(2, 21).cuboid(-0.5f, -3.4f, -4.5f, 1f, 1.05f, 0.6f)
				.uv(9, 21).cuboid(-4.15f, -8.7f, -4.2f, 8.3f, 1.9f, 8.4f)
				.uv(46, 21).cuboid(-4.25f, -6.85f, -4.15f, 8.5f, 1.5f, 8.3f)
				.uv(83, 21).cuboid(-0.35f, -9.15f, -0.35f, 0.7f, 0.4f, 0.7f)
				.uv(89, 21).cuboid(-2.3f, -8.77f, -3.4f, 0.2f, 0.18f, 6.8f)
				.uv(106, 21).cuboid(-4.21f, -6.8f, -2f, 0.12f, 1.2f, 4.8f)
				.uv(119, 21).cuboid(-3.65f, -4.4f, -4.75f, 3.4f, 0.25f, 0.25f)
				.uv(130, 21).cuboid(-4.02f, -3.95f, -4.35f, 0.44f, 0.5f, 0.5f)
				.uv(135, 21).cuboid(-3.88f, -3.85f, -4.64f, 0.16f, 0.18f, 0.12f)
				.uv(139, 21).cuboid(-4.32f, -3.6f, -0.55f, 0.64f, 0.85f, 0.9f)
				.uv(146, 21).cuboid(2.1f, -8.77f, -3.4f, 0.2f, 0.18f, 6.8f)
				.uv(163, 21).cuboid(4.09f, -6.8f, -2f, 0.12f, 1.2f, 4.8f)
				.uv(176, 21).cuboid(0.25f, -4.4f, -4.75f, 3.4f, 0.25f, 0.25f)
				.uv(187, 21).cuboid(3.58f, -3.95f, -4.35f, 0.44f, 0.5f, 0.5f)
				.uv(192, 21).cuboid(3.72f, -3.85f, -4.64f, 0.16f, 0.18f, 0.12f)
				.uv(196, 21).cuboid(3.68f, -3.6f, -0.55f, 0.64f, 0.85f, 0.9f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_backward_cap_brim = p_head.addChild("backward_cap_brim", ModelPartBuilder.create()
				.uv(135, 90).cuboid(-4.3f, -0.25f, 0f, 8.6f, 0.65f, 2.9f)
				.uv(161, 90).cuboid(-4.05f, -0.39f, 0.4f, 8.1f, 0.25f, 2.25f)
				.uv(185, 90).cuboid(-3.8f, 0.35f, 0.35f, 7.6f, 0.18f, 2.2f),
				ModelTransform.of(0f, -4.6f, 3.8f, 0.22f, 0f, 0f));
		ModelPartData p_hat = r.addChild("hat", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(203, 21).cuboid(-4.1f, 0f, -2.1f, 8.2f, 10.5f, 4.2f)
				.uv(2, 39).cuboid(-4.2f, 10.1f, -2.2f, 8.4f, 0.55f, 4.4f)
				.uv(31, 39).cuboid(-4.1f, 10.7f, -2.1f, 8.2f, 1.1f, 4.2f)
				.uv(59, 39).cuboid(-0.6f, 10.75f, -2.35f, 1.2f, 0.7f, 0.3f)
				.uv(65, 39).cuboid(-3.6f, 0.1f, 2.2f, 7.2f, 8.8f, 2.5f)
				.uv(88, 39).cuboid(-2.6f, 3.3f, 4.6f, 5.2f, 4.2f, 0.65f)
				.uv(103, 39).cuboid(-3.4f, 0.3f, 4.6f, 6.8f, 2.4f, 0.55f)
				.uv(121, 39).cuboid(-1f, -0.65f, 3f, 2f, 0.55f, 0.9f)
				.uv(130, 39).cuboid(-0.2f, 4f, 5.3f, 0.4f, 2.3f, 0.15f)
				.uv(135, 39).cuboid(-1f, 3.85f, 5.3f, 2f, 0.4f, 0.15f)
				.uv(143, 39).cuboid(-3.35f, -0.1f, -2.35f, 0.7f, 7.4f, 0.55f)
				.uv(149, 39).cuboid(-3.3f, 5.5f, -2.65f, 0.6f, 0.85f, 0.25f)
				.uv(154, 39).cuboid(2.65f, -0.1f, -2.35f, 0.7f, 7.4f, 0.55f)
				.uv(160, 39).cuboid(2.7f, 5.5f, -2.65f, 0.6f, 0.85f, 0.25f)
				.uv(165, 39).cuboid(-3.8f, 8.2f, 2.2f, 7.6f, 0.85f, 2.8f)
				.uv(189, 39).cuboid(-3f, 0.15f, 4.95f, 6f, 1.7f, 0.5f)
				.uv(205, 39).cuboid(-2.5f, 3.15f, 5.25f, 5f, 0.22f, 0.25f)
				.uv(219, 39).cuboid(0.9f, 3.25f, 5.45f, 0.3f, 0.8f, 0.2f)
				.uv(223, 39).cuboid(-4f, 3.5f, 2.7f, 0.8f, 3.7f, 2f)
				.uv(232, 39).cuboid(-4.05f, 3.3f, 2.65f, 0.9f, 0.55f, 2.1f)
				.uv(2, 54).cuboid(-3.35f, -0.32f, -2.4f, 0.7f, 0.5f, 5.7f)
				.uv(18, 54).cuboid(-3.22f, 0.2f, -2.68f, 0.15f, 4.8f, 0.15f)
				.uv(22, 54).cuboid(3.2f, 3.5f, 2.7f, 0.8f, 3.7f, 2f)
				.uv(31, 54).cuboid(3.15f, 3.3f, 2.65f, 0.9f, 0.55f, 2.1f)
				.uv(40, 54).cuboid(2.65f, -0.32f, -2.4f, 0.7f, 0.5f, 5.7f)
				.uv(56, 54).cuboid(2.78f, 0.2f, -2.68f, 0.15f, 4.8f, 0.15f)
				.uv(60, 54).cuboid(-1.6f, -0.2f, -2.3f, 1.3f, 0.55f, 0.4f)
				.uv(67, 54).cuboid(0.3f, -0.2f, -2.3f, 1.3f, 0.55f, 0.4f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(74, 54).cuboid(-3f, -2f, -2f, 4f, 6f, 4f)
				.uv(93, 54).cuboid(-3.1f, 3.5f, -2.1f, 4.2f, 1.25f, 4.2f)
				.uv(113, 54).cuboid(-3f, 4.7f, -1.9f, 4f, 5.3f, 3.8f)
				.uv(132, 54).cuboid(-2.45f, 8.3f, -2.02f, 1f, 0.65f, 0.2f)
				.uv(138, 54).cuboid(-2.8f, 2.8f, -2.1f, 3.6f, 0.25f, 0.2f),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(149, 54).cuboid(-1f, -2f, -2f, 4f, 6f, 4f)
				.uv(168, 54).cuboid(-1.1f, 3.5f, -2.1f, 4.2f, 1.25f, 4.2f)
				.uv(188, 54).cuboid(-1f, 4.7f, -1.9f, 4f, 5.3f, 3.8f)
				.uv(207, 54).cuboid(-0.45f, 8.3f, -2.02f, 1f, 0.65f, 0.2f)
				.uv(213, 54).cuboid(-0.8f, 2.8f, -2.1f, 3.6f, 0.25f, 0.2f),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(224, 54).cuboid(-2f, -0.1f, -2f, 4f, 10.4f, 4f)
				.uv(243, 54).cuboid(-2.18f, 3.5f, -1.25f, 0.35f, 3f, 2.5f)
				.uv(2, 72).cuboid(-2.3f, 3.35f, -1.4f, 0.48f, 0.55f, 2.8f)
				.uv(12, 72).cuboid(-1.85f, 6.8f, -2.15f, 3.7f, 0.3f, 0.25f)
				.uv(23, 72).cuboid(-2.1f, 10.3f, -2.7f, 4.2f, 1.5f, 4.9f)
				.uv(45, 72).cuboid(-2.15f, 11.7f, -2.8f, 4.3f, 0.5f, 5.1f)
				.uv(67, 72).cuboid(-0.8f, 10.2f, -2.25f, 1.6f, 0.7f, 0.4f)
				.uv(74, 72).cuboid(-1f, 10.65f, -2.92f, 2f, 0.12f, 0.18f)
				.uv(82, 72).cuboid(-1f, 10.87f, -2.92f, 2f, 0.12f, 0.18f)
				.uv(90, 72).cuboid(-1f, 11.09f, -2.92f, 2f, 0.12f, 0.18f)
				.uv(98, 72).cuboid(-1.8f, 3.5f, -2.2f, 1.6f, 2.4f, 0.35f)
				.uv(105, 72).cuboid(-1.65f, 3.85f, -2.5f, 1.3f, 1.8f, 0.2f)
				.uv(111, 72).cuboid(-1.85f, 3.45f, -2.58f, 1.7f, 0.5f, 0.25f)
				.uv(118, 72).cuboid(-1.2f, 3.6f, -2.85f, 0.25f, 0.22f, 0.12f)
				.uv(122, 72).cuboid(-2.04f, 9.75f, -2.05f, 4.08f, 0.55f, 4.1f)
				.uv(142, 72).cuboid(-1.9f, 10.5f, -2.92f, 3.8f, 0.8f, 0.5f)
				.uv(154, 72).cuboid(-1.8f, 10.5f, 1.8f, 3.6f, 1.1f, 0.45f)
				.uv(166, 72).cuboid(-1.95f, 11.62f, -3f, 3.9f, 0.35f, 0.2f),
				ModelTransform.of(-1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(178, 72).cuboid(-2f, -0.1f, -2f, 4f, 10.4f, 4f)
				.uv(197, 72).cuboid(1.83f, 3.5f, -1.25f, 0.35f, 3f, 2.5f)
				.uv(206, 72).cuboid(1.82f, 3.35f, -1.4f, 0.48f, 0.55f, 2.8f)
				.uv(216, 72).cuboid(-1.85f, 6.8f, -2.15f, 3.7f, 0.3f, 0.25f)
				.uv(227, 72).cuboid(-2.1f, 10.3f, -2.7f, 4.2f, 1.5f, 4.9f)
				.uv(2, 90).cuboid(-2.15f, 11.7f, -2.8f, 4.3f, 0.5f, 5.1f)
				.uv(24, 90).cuboid(-0.8f, 10.2f, -2.25f, 1.6f, 0.7f, 0.4f)
				.uv(31, 90).cuboid(-1f, 10.65f, -2.92f, 2f, 0.12f, 0.18f)
				.uv(39, 90).cuboid(-1f, 10.87f, -2.92f, 2f, 0.12f, 0.18f)
				.uv(47, 90).cuboid(-1f, 11.09f, -2.92f, 2f, 0.12f, 0.18f)
				.uv(55, 90).cuboid(0.25f, 3.5f, -2.2f, 1.6f, 2.4f, 0.35f)
				.uv(62, 90).cuboid(0.4f, 3.85f, -2.5f, 1.3f, 1.8f, 0.2f)
				.uv(68, 90).cuboid(0.2f, 3.45f, -2.58f, 1.7f, 0.5f, 0.25f)
				.uv(75, 90).cuboid(0.85f, 3.6f, -2.85f, 0.25f, 0.22f, 0.12f)
				.uv(79, 90).cuboid(-2.04f, 9.75f, -2.05f, 4.08f, 0.55f, 4.1f)
				.uv(99, 90).cuboid(-1.9f, 10.5f, -2.92f, 3.8f, 0.8f, 0.5f)
				.uv(111, 90).cuboid(-1.8f, 10.5f, 1.8f, 3.6f, 1.1f, 0.45f)
				.uv(123, 90).cuboid(-1.95f, 11.62f, -3f, 3.9f, 0.35f, 0.2f),
				ModelTransform.of(1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_sleeve = r.addChild("right_sleeve", ModelPartBuilder.create(),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_sleeve = r.addChild("left_sleeve", ModelPartBuilder.create(),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
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
