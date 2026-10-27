package com.phantompowers.client;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

/** Your model (from your updated jar). */
@SuppressWarnings("unused")
public final class PaulinaModelData {
	private PaulinaModelData() {}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(37, 2).cuboid(-4.3f, -8.4f, -4.25f, 8.6f, 1.3f, 8.5f)
				.uv(75, 2).cuboid(-4.3f, -7.6f, 2.7f, 8.6f, 12.5f, 1.8f)
				.uv(99, 2).cuboid(-3.8f, 4.4f, 2.9f, 7.6f, 3.4f, 1.55f)
				.uv(121, 2).cuboid(-2.9f, 7.5f, 3f, 5.8f, 2.1f, 1.35f)
				.uv(139, 2).cuboid(-4.6f, -7.2f, -3.8f, 1.2f, 7.7f, 7f)
				.uv(159, 2).cuboid(-2.7f, -6.8f, 4.4f, 0.4f, 13.1f, 0.2f)
				.uv(164, 2).cuboid(3.4f, -7.2f, -3.8f, 1.2f, 7.7f, 7f)
				.uv(184, 2).cuboid(2.3f, -6.8f, 4.4f, 0.4f, 13.1f, 0.2f)
				.uv(189, 2).cuboid(-3.9f, -7.45f, -4.45f, 7.8f, 0.85f, 0.55f)
				.uv(209, 2).cuboid(-3.5f, -6.8f, -4.58f, 6.5f, 0.18f, 0.2f)
				.uv(226, 2).cuboid(-4.6f, -0.4f, 2.6f, 1.2f, 4.4f, 1.8f)
				.uv(235, 2).cuboid(-3.85f, 4.1f, 3.4f, 1.1f, 3.3f, 1.35f)
				.uv(243, 2).cuboid(3.4f, -0.4f, 2.6f, 1.2f, 4.4f, 1.8f)
				.uv(2, 21).cuboid(2.75f, 4.1f, 3.4f, 1.1f, 3.3f, 1.35f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_pink_hair_clip = p_head.addChild("pink_hair_clip", ModelPartBuilder.create()
				.uv(144, 52).cuboid(-0.4f, -1.05f, -0.15f, 0.8f, 2.1f, 0.35f)
				.uv(150, 52).cuboid(-0.2f, -0.7f, -0.3f, 0.35f, 1.35f, 0.2f),
				ModelTransform.of(-2.85f, -7.1f, -4.55f, 0f, 0f, -0.55f));
		ModelPartData p_right_wavy_front_hair = p_head.addChild("right_wavy_front_hair", ModelPartBuilder.create()
				.uv(155, 52).cuboid(-0.68f, 0f, -0.25f, 1.36f, 3.1f, 1.3f)
				.uv(164, 52).cuboid(-0.85f, 2.7f, -0.1f, 1.7f, 2.9f, 1.2f)
				.uv(173, 52).cuboid(-0.65f, 5.1f, 0f, 1.3f, 2.6f, 1.1f)
				.uv(181, 52).cuboid(-0.42f, 7.4f, 0.15f, 0.84f, 1.4f, 0.8f)
				.uv(188, 52).cuboid(-0.4f, 0.45f, -0.4f, 0.25f, 2.1f, 0.2f)
				.uv(192, 52).cuboid(0.35f, 3.1f, -0.25f, 0.2f, 1.7f, 0.2f),
				ModelTransform.of(-2.9f, -0.1f, -2.65f, 0f, 0f, -0.04f));
		ModelPartData p_left_wavy_front_hair = p_head.addChild("left_wavy_front_hair", ModelPartBuilder.create()
				.uv(196, 52).cuboid(-0.68f, 0f, -0.25f, 1.36f, 3.1f, 1.3f)
				.uv(205, 52).cuboid(-0.85f, 2.7f, -0.1f, 1.7f, 2.9f, 1.2f)
				.uv(214, 52).cuboid(-0.65f, 5.1f, 0f, 1.3f, 2.6f, 1.1f)
				.uv(222, 52).cuboid(-0.42f, 7.4f, 0.15f, 0.84f, 1.4f, 0.8f)
				.uv(229, 52).cuboid(-0.4f, 0.45f, -0.4f, 0.25f, 2.1f, 0.2f)
				.uv(233, 52).cuboid(0.35f, 3.1f, -0.25f, 0.2f, 1.7f, 0.2f),
				ModelTransform.of(2.9f, -0.1f, -2.65f, 0f, 0f, 0.04f));
		ModelPartData p_hat = r.addChild("hat", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(10, 21).cuboid(-3.65f, 0f, -1.95f, 7.3f, 4.8f, 3.9f)
				.uv(36, 21).cuboid(-3.35f, 4.7f, -1.85f, 6.7f, 2.5f, 3.7f)
				.uv(60, 21).cuboid(-3.4f, 6.65f, -1.9f, 6.8f, 0.55f, 3.8f)
				.uv(85, 21).cuboid(-3.35f, 7.2f, -1.75f, 6.7f, 3.9f, 3.5f)
				.uv(109, 21).cuboid(-3.5f, 11.1f, -1.9f, 7f, 1f, 3.8f)
				.uv(134, 21).cuboid(-3.28f, 0.2f, -2.06f, 0.26f, 3f, 0.18f)
				.uv(138, 21).cuboid(3.02f, 0.2f, -2.06f, 0.26f, 3f, 0.18f)
				.uv(142, 21).cuboid(-3.5f, 11.1f, -2f, 7f, 0.16f, 0.15f)
				.uv(160, 21).cuboid(-0.15f, 11.3f, -2.1f, 0.3f, 0.3f, 0.15f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(164, 21).cuboid(-2.1f, -2.1f, -1.6f, 3.2f, 2.6f, 3.2f)
				.uv(180, 21).cuboid(-2.12f, 0.05f, -1.62f, 3.24f, 0.55f, 3.24f)
				.uv(196, 21).cuboid(-2f, 0.6f, -1.5f, 3f, 8.9f, 3f)
				.uv(211, 21).cuboid(-2.18f, 0.45f, -1.68f, 3.36f, 0.25f, 3.36f),
				ModelTransform.of(-4.7f, 2.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(228, 21).cuboid(-1.1f, -2.1f, -1.6f, 3.2f, 2.6f, 3.2f)
				.uv(2, 36).cuboid(-1.12f, 0.05f, -1.62f, 3.24f, 0.55f, 3.24f)
				.uv(18, 36).cuboid(-1f, 0.6f, -1.5f, 3f, 8.9f, 3f)
				.uv(33, 36).cuboid(-1.18f, 0.45f, -1.68f, 3.36f, 0.25f, 3.36f),
				ModelTransform.of(4.7f, 2.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(50, 36).cuboid(-1.65f, -0.05f, -1.7f, 3.3f, 8.8f, 3.4f)
				.uv(67, 36).cuboid(-1.65f, 6.8f, -1.8f, 3.3f, 0.5f, 0.2f)
				.uv(77, 36).cuboid(-1.5f, 8.7f, -1.5f, 3f, 2.25f, 3f)
				.uv(92, 36).cuboid(-1.75f, 10.9f, -2.3f, 3.5f, 0.9f, 4.1f)
				.uv(111, 36).cuboid(-1.8f, 11.7f, -2.4f, 3.6f, 0.5f, 4.3f)
				.uv(130, 36).cuboid(-1.4f, 11.05f, -2.5f, 2.8f, 0.65f, 0.35f)
				.uv(140, 36).cuboid(-0.1f, 0.55f, -1.82f, 0.18f, 7.7f, 0.18f)
				.uv(144, 36).cuboid(-1.25f, 0.8f, 1.68f, 2.5f, 1.8f, 0.18f)
				.uv(153, 36).cuboid(-1.05f, 1f, 1.85f, 2.1f, 1.4f, 0.12f)
				.uv(161, 36).cuboid(-1.72f, 8.25f, -1.77f, 3.44f, 0.5f, 3.54f)
				.uv(178, 36).cuboid(-0.85f, 10.65f, -1.85f, 1.7f, 0.7f, 0.4f)
				.uv(186, 36).cuboid(-1f, 10.7f, -2.02f, 2f, 0.12f, 0.18f)
				.uv(194, 36).cuboid(-1f, 10.9f, -2.02f, 2f, 0.12f, 0.18f)
				.uv(202, 36).cuboid(-1f, 11.1f, -2.02f, 2f, 0.12f, 0.18f)
				.uv(210, 36).cuboid(-1.82f, 11f, -1.8f, 0.18f, 0.6f, 2.8f),
				ModelTransform.of(-1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(219, 36).cuboid(-1.65f, -0.05f, -1.7f, 3.3f, 8.8f, 3.4f)
				.uv(236, 36).cuboid(-1.65f, 6.8f, -1.8f, 3.3f, 0.5f, 0.2f)
				.uv(2, 52).cuboid(-1.5f, 8.7f, -1.5f, 3f, 2.25f, 3f)
				.uv(17, 52).cuboid(-1.75f, 10.9f, -2.3f, 3.5f, 0.9f, 4.1f)
				.uv(36, 52).cuboid(-1.8f, 11.7f, -2.4f, 3.6f, 0.5f, 4.3f)
				.uv(55, 52).cuboid(-1.4f, 11.05f, -2.5f, 2.8f, 0.65f, 0.35f)
				.uv(65, 52).cuboid(-1.6f, 0.55f, -1.82f, 0.18f, 7.7f, 0.18f)
				.uv(69, 52).cuboid(-1.25f, 0.8f, 1.68f, 2.5f, 1.8f, 0.18f)
				.uv(78, 52).cuboid(-1.05f, 1f, 1.85f, 2.1f, 1.4f, 0.12f)
				.uv(86, 52).cuboid(-1.72f, 8.25f, -1.77f, 3.44f, 0.5f, 3.54f)
				.uv(103, 52).cuboid(-0.85f, 10.65f, -1.85f, 1.7f, 0.7f, 0.4f)
				.uv(111, 52).cuboid(-1f, 10.7f, -2.02f, 2f, 0.12f, 0.18f)
				.uv(119, 52).cuboid(-1f, 10.9f, -2.02f, 2f, 0.12f, 0.18f)
				.uv(127, 52).cuboid(-1f, 11.1f, -2.02f, 2f, 0.12f, 0.18f)
				.uv(135, 52).cuboid(-1.82f, 11f, -1.8f, 0.18f, 0.6f, 2.8f),
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
