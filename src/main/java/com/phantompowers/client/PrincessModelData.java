package com.phantompowers.client;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

/** Your model for Paige's townsperson (player-shaped, slim arms). */
@SuppressWarnings("unused")
public final class PrincessModelData {
	private PrincessModelData() {}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(37, 2).cuboid(-4.3f, -8.5f, -4.3f, 8.6f, 1.5f, 8.6f)
				.uv(75, 2).cuboid(-4.25f, -7.8f, 2.5f, 8.5f, 8.3f, 1.9f)
				.uv(99, 2).cuboid(-4.6f, -7.4f, -3.7f, 1.2f, 7.8f, 6.4f)
				.uv(118, 2).cuboid(3.4f, -7.4f, -3.7f, 1.2f, 7.8f, 6.4f)
				.uv(137, 2).cuboid(-4.45f, -7.5f, -4.45f, 8.9f, 0.85f, 8.9f)
				.uv(176, 2).cuboid(-3.5f, -9.4f, -3.4f, 7f, 1f, 6.8f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_braid = p_head.addChild("braid", ModelPartBuilder.create()
				.uv(164, 21).cuboid(-1.6f, 0f, 0f, 2.8f, 1.65f, 1.6f)
				.uv(176, 21).cuboid(-1.09f, 1.25f, 0f, 2.58f, 1.65f, 1.6f)
				.uv(188, 21).cuboid(-1.38f, 2.5f, 0f, 2.36f, 1.65f, 1.6f)
				.uv(199, 21).cuboid(-0.87f, 3.75f, 0f, 2.14f, 1.65f, 1.6f)
				.uv(210, 21).cuboid(-1.16f, 5f, 0f, 1.92f, 1.65f, 1.6f)
				.uv(221, 21).cuboid(-0.65f, 6.25f, 0f, 1.7f, 1.65f, 1.6f)
				.uv(231, 21).cuboid(-0.94f, 7.5f, 0f, 1.48f, 1.65f, 1.6f)
				.uv(241, 21).cuboid(-0.65f, 8.3f, -0.1f, 1.3f, 0.55f, 1.8f)
				.uv(2, 37).cuboid(-0.6f, 8.7f, 0.1f, 1.2f, 1.3f, 1.3f),
				ModelTransform.of(0f, -2.5f, 4.15f, 0f, 0f, 0f));
		ModelPartData p_hat = r.addChild("hat", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(207, 2).cuboid(-3.8f, 0f, -2.1f, 7.6f, 9.8f, 4.2f)
				.uv(2, 21).cuboid(-3.9f, 9f, -2.2f, 7.8f, 1f, 4.4f)
				.uv(30, 21).cuboid(-2.5f, 0.1f, -2.35f, 5f, 1f, 0.35f)
				.uv(44, 21).cuboid(-2.35f, 1.1f, -2.5f, 0.7f, 1.6f, 0.4f)
				.uv(50, 21).cuboid(1.65f, 1.1f, -2.5f, 0.7f, 1.6f, 0.4f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_gown = p_body.addChild("gown", ModelPartBuilder.create()
				.uv(10, 37).cuboid(-4f, 0f, -2.5f, 8f, 3f, 5f)
				.uv(39, 37).cuboid(-4.65f, 3f, -3f, 9.3f, 3f, 6f)
				.uv(73, 37).cuboid(-5.3f, 6f, -3.5f, 10.6f, 3f, 7f)
				.uv(112, 37).cuboid(-6f, 9f, -4f, 12f, 3f, 8f)
				.uv(155, 37).cuboid(-6.5f, 11f, -4.4f, 13f, 1f, 8.8f),
				ModelTransform.of(0f, 10f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(56, 21).cuboid(-2.4f, -2f, -1.7f, 3.3f, 8.7f, 3.4f)
				.uv(73, 21).cuboid(-2.8f, 6f, -2f, 4.1f, 1.8f, 4f)
				.uv(93, 21).cuboid(-2.4f, 7.7f, -1.7f, 3.3f, 1.7f, 3.4f),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(110, 21).cuboid(-0.9f, -2f, -1.7f, 3.3f, 8.7f, 3.4f)
				.uv(127, 21).cuboid(-1.3f, 6f, -2f, 4.1f, 1.8f, 4f)
				.uv(147, 21).cuboid(-0.9f, 7.7f, -1.7f, 3.3f, 1.7f, 3.4f),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
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
