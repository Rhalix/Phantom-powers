package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.GliderEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

/**
 * Hover Glider: a sleek swept-back deck with a pointed nose, raised side rails,
 * two thruster pods with glowing nozzles, tail fins and twin nose cannons.
 */
public class GliderModel extends SinglePartEntityModel<GliderEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("hover_glider"), "main");
	private final ModelPart root;

	public GliderModel(ModelPart root) {
		super(RenderLayer::getEntityCutoutNoCull);
		this.root = root;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData deck = r.addChild("deck", ModelPartBuilder.create()
						// main deck plate
						.uv(0, 0).cuboid(-6f, -1f, -10f, 12, 2, 20)
						// raised side rails
						.uv(0, 22).cuboid(-7f, -2f, -8f, 1, 2, 16)
						.uv(0, 22).cuboid(6f, -2f, -8f, 1, 2, 16)
						// grip strip down the middle
						.uv(34, 22).cuboid(-2f, -1.5f, -7f, 4, 1, 14),
				ModelTransform.pivot(0f, 22f, 0f));
		// pointed nose (stepped taper)
		deck.addChild("nose1", ModelPartBuilder.create().uv(0, 40).cuboid(-5f, -1f, -13f, 10, 2, 3), ModelTransform.NONE);
		deck.addChild("nose2", ModelPartBuilder.create().uv(26, 40).cuboid(-3.5f, -1f, -15f, 7, 2, 2), ModelTransform.NONE);
		deck.addChild("nose3", ModelPartBuilder.create().uv(44, 40).cuboid(-2f, -0.8f, -16.5f, 4, 1.6f, 1.5f), ModelTransform.NONE);
		// twin nose cannons
		deck.addChild("cannons", ModelPartBuilder.create()
				.uv(56, 0).cuboid(-5.5f, 0.6f, -16f, 2, 2, 7)
				.uv(56, 0).cuboid(3.5f, 0.6f, -16f, 2, 2, 7), ModelTransform.NONE);
		// thruster pods under the back
		deck.addChild("thrusters", ModelPartBuilder.create()
				.uv(0, 46).cuboid(-7.5f, 1f, 2f, 4, 4, 9)
				.uv(0, 46).cuboid(3.5f, 1f, 2f, 4, 4, 9), ModelTransform.NONE);
		// glowing nozzles
		deck.addChild("nozzles", ModelPartBuilder.create()
				.uv(28, 46).cuboid(-7f, 1.5f, 11f, 3, 3, 1)
				.uv(28, 46).cuboid(4f, 1.5f, 11f, 3, 3, 1), ModelTransform.NONE);
		// tail fins
		deck.addChild("fin_l", ModelPartBuilder.create().uv(40, 46).cuboid(-0.5f, -5f, 0f, 1, 5, 4),
				ModelTransform.of(-5.5f, -1f, 7f, -0.35f, 0f, -0.25f));
		deck.addChild("fin_r", ModelPartBuilder.create().uv(40, 46).cuboid(-0.5f, -5f, 0f, 1, 5, 4),
				ModelTransform.of(5.5f, -1f, 7f, -0.35f, 0f, 0.25f));
		return TexturedModelData.of(data, 128, 64);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(GliderEntity entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
	}
}
