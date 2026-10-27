package com.phantompowers.client;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

/** Your model (from your updated jar). */
@SuppressWarnings("unused")
public final class PaulinaDanceModelData {
	private PaulinaDanceModelData() {}

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
				.uv(229, 21).cuboid(-0.4f, -1.05f, -0.15f, 0.8f, 2.1f, 0.35f)
				.uv(235, 21).cuboid(-0.2f, -0.7f, -0.3f, 0.35f, 1.35f, 0.2f),
				ModelTransform.of(-2.85f, -7.1f, -4.55f, 0f, 0f, -0.55f));
		ModelPartData p_right_wavy_front_hair = p_head.addChild("right_wavy_front_hair", ModelPartBuilder.create()
				.uv(240, 21).cuboid(-0.68f, 0f, -0.25f, 1.36f, 3.1f, 1.3f)
				.uv(2, 36).cuboid(-0.85f, 2.7f, -0.1f, 1.7f, 2.9f, 1.2f)
				.uv(11, 36).cuboid(-0.65f, 5.1f, 0f, 1.3f, 2.6f, 1.1f)
				.uv(19, 36).cuboid(-0.42f, 7.4f, 0.15f, 0.84f, 1.4f, 0.8f)
				.uv(26, 36).cuboid(-0.4f, 0.45f, -0.4f, 0.25f, 2.1f, 0.2f)
				.uv(30, 36).cuboid(0.35f, 3.1f, -0.25f, 0.2f, 1.7f, 0.2f),
				ModelTransform.of(-2.9f, -0.1f, -2.65f, 0f, 0f, -0.04f));
		ModelPartData p_left_wavy_front_hair = p_head.addChild("left_wavy_front_hair", ModelPartBuilder.create()
				.uv(34, 36).cuboid(-0.68f, 0f, -0.25f, 1.36f, 3.1f, 1.3f)
				.uv(43, 36).cuboid(-0.85f, 2.7f, -0.1f, 1.7f, 2.9f, 1.2f)
				.uv(52, 36).cuboid(-0.65f, 5.1f, 0f, 1.3f, 2.6f, 1.1f)
				.uv(60, 36).cuboid(-0.42f, 7.4f, 0.15f, 0.84f, 1.4f, 0.8f)
				.uv(67, 36).cuboid(-0.4f, 0.45f, -0.4f, 0.25f, 2.1f, 0.2f)
				.uv(71, 36).cuboid(0.35f, 3.1f, -0.25f, 0.2f, 1.7f, 0.2f),
				ModelTransform.of(2.9f, -0.1f, -2.65f, 0f, 0f, 0.04f));
		ModelPartData p_hat = r.addChild("hat", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(10, 21).cuboid(-3.55f, 1.15f, -1.95f, 7.1f, 6.6f, 3.9f)
				.uv(35, 21).cuboid(-3.6f, 1f, -2f, 7.2f, 0.55f, 4f)
				.uv(61, 21).cuboid(-3.65f, 7.25f, -2.1f, 7.3f, 0.7f, 4.2f)
				.uv(87, 21).cuboid(-1f, 1f, -2.45f, 0.6f, 0.55f, 0.45f)
				.uv(93, 21).cuboid(-1.75f, 0.65f, -2.45f, 0.6f, 0.55f, 0.45f)
				.uv(99, 21).cuboid(-2.5f, 0.3f, -2.45f, 0.6f, 0.55f, 0.45f)
				.uv(105, 21).cuboid(0.4f, 1f, -2.45f, 0.6f, 0.55f, 0.45f)
				.uv(111, 21).cuboid(1.15f, 0.65f, -2.45f, 0.6f, 0.55f, 0.45f)
				.uv(117, 21).cuboid(1.9f, 0.3f, -2.45f, 0.6f, 0.55f, 0.45f)
				.uv(123, 21).cuboid(-0.5f, 1f, -2.65f, 1f, 1f, 0.6f)
				.uv(130, 21).cuboid(-0.24f, 1.2f, -2.98f, 0.48f, 0.5f, 0.1f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_formal_gown = p_body.addChild("formal_gown", ModelPartBuilder.create()
				.uv(75, 36).cuboid(-3.9f, 0f, -2.4f, 7.8f, 1.06f, 4.8f)
				.uv(104, 36).cuboid(-4.025f, 1f, -2.5f, 8.05f, 1.06f, 5f)
				.uv(134, 36).cuboid(-4.15f, 2f, -2.6f, 8.3f, 1.06f, 5.2f)
				.uv(164, 36).cuboid(-4.275f, 3f, -2.7f, 8.55f, 1.06f, 5.4f)
				.uv(195, 36).cuboid(-4.4f, 4f, -2.8f, 8.8f, 1.06f, 5.6f)
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
				ModelTransform.of(0f, 7.9f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_draped_skirt = p_body.addChild("right_draped_skirt", ModelPartBuilder.create()
				.uv(164, 58).cuboid(-1.1f, 0f, -0.2f, 2.2f, 13.8f, 0.45f)
				.uv(173, 58).cuboid(0.75f, 0.5f, -0.3f, 0.3f, 12.3f, 0.2f),
				ModelTransform.of(-2.45f, 8f, -2.9f, -0.12f, 0f, 0.12f));
		ModelPartData p_left_draped_skirt = p_body.addChild("left_draped_skirt", ModelPartBuilder.create()
				.uv(177, 58).cuboid(-1.1f, 0f, -0.2f, 2.2f, 13.8f, 0.45f)
				.uv(186, 58).cuboid(-1.05f, 0.5f, -0.3f, 0.3f, 12.3f, 0.2f),
				ModelTransform.of(2.45f, 8f, -2.9f, -0.12f, 0f, -0.12f));
		ModelPartData p_center_gown_fold = p_body.addChild("center_gown_fold", ModelPartBuilder.create()
				.uv(190, 58).cuboid(-0.5f, 0f, -0.1f, 1f, 15.5f, 0.3f),
				ModelTransform.of(0f, 8.1f, -2.7f, -0.12f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(135, 21).cuboid(-2f, -2f, -1.5f, 3f, 2.6f, 3f)
				.uv(150, 21).cuboid(-2.05f, 0.6f, -1.55f, 3.1f, 8.9f, 3.1f)
				.uv(166, 21).cuboid(-2.1f, 0.5f, -1.6f, 3.2f, 0.55f, 3.2f),
				ModelTransform.of(-4.7f, 2.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(182, 21).cuboid(-1f, -2f, -1.5f, 3f, 2.6f, 3f)
				.uv(197, 21).cuboid(-1.05f, 0.6f, -1.55f, 3.1f, 8.9f, 3.1f)
				.uv(213, 21).cuboid(-1.1f, 0.5f, -1.6f, 3.2f, 0.55f, 3.2f),
				ModelTransform.of(4.7f, 2.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create(),
				ModelTransform.of(-1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create(),
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
