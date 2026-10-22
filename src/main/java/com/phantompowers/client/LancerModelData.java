package com.phantompowers.client;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

/** Your model for Mr. Pemberton's townsperson (player-shaped). */
@SuppressWarnings("unused")
public final class LancerModelData {
	private LancerModelData() {}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(37, 2).cuboid(-0.55f, -3.6f, -4.65f, 1.1f, 1.35f, 0.85f)
				.uv(44, 2).cuboid(-1.35f, -0.95f, -4.35f, 2.7f, 1.1f, 0.45f)
				.uv(54, 2).cuboid(-4.3f, -4.6f, -0.9f, 0.6f, 1.8f, 1.25f)
				.uv(61, 2).cuboid(3.7f, -4.6f, -0.9f, 0.6f, 1.8f, 1.25f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_hat = r.addChild("hat", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(68, 2).cuboid(-4.35f, 0f, -2.3f, 8.7f, 11.6f, 4.6f)
				.uv(98, 2).cuboid(-3.6f, 11.5f, -2.25f, 7.2f, 0.65f, 4.5f)
				.uv(125, 2).cuboid(-0.25f, 0.9f, -2.4f, 0.5f, 10.5f, 0.2f)
				.uv(130, 2).cuboid(-1.7f, -0.1f, -2.55f, 1.2f, 1.3f, 0.35f)
				.uv(137, 2).cuboid(0.55f, -0.1f, -2.55f, 1.2f, 1.3f, 0.35f)
				.uv(144, 2).cuboid(-0.65f, 0.7f, -2.75f, 1.3f, 1f, 0.45f)
				.uv(151, 2).cuboid(-0.55f, 1.65f, -2.75f, 1.1f, 6.8f, 0.4f)
				.uv(157, 2).cuboid(-0.65f, 8.3f, -2.75f, 1.3f, 0.8f, 0.4f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(164, 2).cuboid(-3f, -2f, -2f, 4f, 5.9f, 4f)
				.uv(183, 2).cuboid(-3.15f, 3.3f, -2.15f, 4.3f, 1.1f, 4.3f)
				.uv(204, 2).cuboid(-3f, 4.4f, -1.95f, 4f, 5.6f, 3.9f)
				.uv(223, 2).cuboid(-2.35f, 8.7f, -2.05f, 1f, 0.8f, 0.15f),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(229, 2).cuboid(-1f, -2f, -2f, 4f, 5.9f, 4f)
				.uv(2, 22).cuboid(-1.15f, 3.3f, -2.15f, 4.3f, 1.1f, 4.3f)
				.uv(23, 22).cuboid(-1f, 4.4f, -1.95f, 4f, 5.6f, 3.9f)
				.uv(42, 22).cuboid(-0.35f, 8.7f, -2.05f, 1f, 0.8f, 0.15f),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(48, 22).cuboid(-2f, 0f, -2f, 4f, 10.5f, 4f)
				.uv(67, 22).cuboid(-2.1f, 10.5f, -2.65f, 4.2f, 1.5f, 4.8f)
				.uv(88, 22).cuboid(-1.8f, 11f, -2.8f, 3.6f, 0.25f, 0.25f),
				ModelTransform.of(-1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(99, 22).cuboid(-2f, 0f, -2f, 4f, 10.5f, 4f)
				.uv(118, 22).cuboid(-2.1f, 10.5f, -2.65f, 4.2f, 1.5f, 4.8f)
				.uv(139, 22).cuboid(-1.8f, 11f, -2.8f, 3.6f, 0.25f, 0.25f),
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
