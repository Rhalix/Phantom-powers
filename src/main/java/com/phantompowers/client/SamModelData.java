package com.phantompowers.client;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

/** Your model for Lena's townsperson (player-shaped, slim arms). */
@SuppressWarnings("unused")
public final class SamModelData {
	private SamModelData() {}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(37, 2).cuboid(-4.3f, -8.3f, 1.8f, 8.6f, 8.5f, 2.6f)
				.uv(63, 2).cuboid(-4.3f, -8.5f, -4.25f, 8.6f, 1.5f, 8.5f)
				.uv(101, 2).cuboid(-4.65f, -7.5f, -3.9f, 1.3f, 8f, 7.9f)
				.uv(123, 2).cuboid(-4.72f, -3.3f, -3.6f, 1.44f, 1.25f, 7.3f)
				.uv(144, 2).cuboid(3.35f, -7.5f, -3.9f, 1.3f, 8f, 7.9f)
				.uv(166, 2).cuboid(3.28f, -3.3f, -3.6f, 1.44f, 1.25f, 7.3f)
				.uv(187, 2).cuboid(-4.35f, -4f, -4.3f, 1.1f, 4.6f, 0.7f)
				.uv(194, 2).cuboid(3.25f, -3.7f, -4.3f, 1.1f, 4.3f, 0.7f)
				.uv(201, 2).cuboid(-3.2f, -6.5f, -4.57f, 2.7f, 0.35f, 0.2f)
				.uv(210, 2).cuboid(-1.3f, -9.2f, -0.6f, 2.6f, 1f, 2.6f)
				.uv(224, 2).cuboid(-1.85f, -11.2f, -1.15f, 3.7f, 2f, 3.7f)
				.uv(242, 2).cuboid(-1.8f, -11.25f, -1.2f, 1f, 0.35f, 3.7f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_fringe = p_head.addChild("right_fringe", ModelPartBuilder.create()
				.uv(158, 38).cuboid(-2f, -0.6f, -0.15f, 4f, 2.1f, 0.6f),
				ModelTransform.of(-1.7f, -7f, -4.35f, 0f, 0f, -0.12f));
		ModelPartData p_left_fringe = p_head.addChild("left_fringe", ModelPartBuilder.create()
				.uv(171, 38).cuboid(-2f, -0.6f, -0.15f, 4f, 2.1f, 0.6f),
				ModelTransform.of(1.7f, -7f, -4.35f, 0f, 0f, 0.12f));
		ModelPartData p_hat = r.addChild("hat", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(2, 21).cuboid(-3.65f, 0f, -2f, 7.3f, 5.5f, 4f)
				.uv(28, 21).cuboid(-3.3f, 5.4f, -1.9f, 6.6f, 3.3f, 3.8f)
				.uv(52, 21).cuboid(-3.3f, 8.7f, -1.8f, 6.6f, 1.4f, 3.6f)
				.uv(76, 21).cuboid(-4.3f, 10f, -2.4f, 8.6f, 4.5f, 4.8f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(106, 21).cuboid(-2f, -2f, -1.5f, 3f, 10.5f, 3f)
				.uv(121, 21).cuboid(-2.15f, 6.1f, -1.65f, 3.3f, 1.15f, 3.3f)
				.uv(138, 21).cuboid(-2f, 8.4f, -1.55f, 3f, 1.2f, 3.1f),
				ModelTransform.of(-5f, 2.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(154, 21).cuboid(-1f, -2f, -1.5f, 3f, 10.5f, 3f)
				.uv(169, 21).cuboid(-1.15f, 6.1f, -1.65f, 3.3f, 1.15f, 3.3f)
				.uv(186, 21).cuboid(-1f, 8.4f, -1.55f, 3f, 1.2f, 3.1f),
				ModelTransform.of(5f, 2.5f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(202, 21).cuboid(-1.65f, 1f, -1.65f, 3.3f, 5f, 3.3f)
				.uv(219, 21).cuboid(-1.8f, 5.7f, -1.8f, 3.6f, 5.7f, 3.6f)
				.uv(237, 21).cuboid(-0.85f, 6f, -1.95f, 0.65f, 4.3f, 0.25f)
				.uv(2, 38).cuboid(-1.9f, 10.5f, -2.9f, 3.8f, 1.5f, 4.8f)
				.uv(23, 38).cuboid(-1.15f, 6.2f, -2.1f, 2.3f, 0.4f, 0.3f)
				.uv(32, 38).cuboid(-0.75f, 6.25f, -2.3f, 1.5f, 0.2f, 0.2f)
				.uv(39, 38).cuboid(-1.95f, 11.65f, -2.95f, 3.9f, 0.6f, 4.9f),
				ModelTransform.of(-1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(60, 38).cuboid(-1.65f, 1f, -1.65f, 3.3f, 5f, 3.3f)
				.uv(77, 38).cuboid(-1.8f, 5.7f, -1.8f, 3.6f, 5.7f, 3.6f)
				.uv(95, 38).cuboid(-0.85f, 6f, -1.95f, 0.65f, 4.3f, 0.25f)
				.uv(100, 38).cuboid(-1.9f, 10.5f, -2.9f, 3.8f, 1.5f, 4.8f)
				.uv(121, 38).cuboid(-1.15f, 6.2f, -2.1f, 2.3f, 0.4f, 0.3f)
				.uv(130, 38).cuboid(-0.75f, 6.25f, -2.3f, 1.5f, 0.2f, 0.2f)
				.uv(137, 38).cuboid(-1.95f, 11.65f, -2.95f, 3.9f, 0.6f, 4.9f),
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
