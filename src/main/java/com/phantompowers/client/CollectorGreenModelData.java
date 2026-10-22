package com.phantompowers.client;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

/** Your model for the Collector when he's dazed (the small green ghost out of his armour). */
@SuppressWarnings("unused")
public final class CollectorGreenModelData {
	private CollectorGreenModelData() {}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(2, 128).cuboid(-2.7f, 1f, -1.5f, 5.4f, 9.5f, 3f),
				ModelTransform.of(0f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_coat = p_body.addChild("coat", ModelPartBuilder.create(),
				ModelTransform.of(0f, 11f, 0f, 0.08f, 0f, 0f));
		ModelPartData p_tail = p_coat.addChild("tail", ModelPartBuilder.create(),
				ModelTransform.of(0f, 6f, 0f, 0.15f, 0f, 0f));
		ModelPartData p_pack = p_body.addChild("pack", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_cannon = p_body.addChild("cannon", ModelPartBuilder.create(),
				ModelTransform.of(3f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(22, 128).cuboid(-3f, -8f, -1.7f, 6f, 9.5f, 3.4f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_green_leg = p_body.addChild("right_green_leg", ModelPartBuilder.create()
				.uv(124, 128).cuboid(-0.55f, 0f, -0.55f, 1.1f, 10.7f, 1.1f)
				.uv(132, 128).cuboid(-1.25f, 10f, -1.9f, 2.5f, 1.2f, 3f),
				ModelTransform.of(-1.5f, 10f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_green_leg = p_body.addChild("left_green_leg", ModelPartBuilder.create()
				.uv(146, 128).cuboid(-0.55f, 0f, -0.55f, 1.1f, 10.7f, 1.1f)
				.uv(154, 128).cuboid(-1.25f, 10f, -1.9f, 2.5f, 1.2f, 3f),
				ModelTransform.of(1.5f, 10f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(44, 128).cuboid(-0.8f, -1.5f, -0.65f, 1.3f, 11.5f, 1.3f)
				.uv(53, 128).cuboid(-1.6f, 8.8f, -1f, 2.8f, 2.7f, 2f)
				.uv(66, 128).cuboid(-1.4f, 11f, -1f, 0.55f, 1.7f, 0.8f)
				.uv(72, 128).cuboid(-0.6f, 11f, -1f, 0.55f, 1.7f, 0.8f)
				.uv(78, 128).cuboid(0.2f, 11f, -1f, 0.55f, 1.7f, 0.8f),
				ModelTransform.of(-5f, 3f, 0f, 0f, 0f, 0f));
		ModelPartData p_staff = p_right_arm.addChild("staff", ModelPartBuilder.create(),
				ModelTransform.of(-1f, 10f, -0.5f, -0.25f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(84, 128).cuboid(0f, -1.5f, -0.65f, 1.3f, 11.5f, 1.3f)
				.uv(93, 128).cuboid(-0.8f, 8.8f, -1f, 2.8f, 2.7f, 2f)
				.uv(106, 128).cuboid(-0.6f, 11f, -1f, 0.55f, 1.7f, 0.8f)
				.uv(112, 128).cuboid(0.2f, 11f, -1f, 0.55f, 1.7f, 0.8f)
				.uv(118, 128).cuboid(1f, 11f, -1f, 0.55f, 1.7f, 0.8f),
				ModelTransform.of(5f, 3f, 0f, 0f, 0f, 0f));
		ModelPartData p_lantern = p_left_arm.addChild("lantern", ModelPartBuilder.create(),
				ModelTransform.of(1f, 11f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 256);
	}
}
