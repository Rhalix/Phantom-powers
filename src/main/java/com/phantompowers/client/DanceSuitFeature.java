package com.phantompowers.client;

import com.phantompowers.ModItems;
import com.phantompowers.PhantomMod;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;

/**
 * The suit and tie in 3D: a tie with a knot that sticks out from the shirt, raised satin lapels, shirt collar
 * points, a pocket square and buttons, drawn on top of the flat suit texture.
 */
public class DanceSuitFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/dance_suit_bits.png");
	private static ModelPart bits;

	public DanceSuitFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
		super(ctx);
	}

	/** Texture areas: red (0,0) tie, black satin (16,0) lapels, white (0,16) shirt, gold (16,16) buttons. */
	private static ModelPart bits() {
		if (bits != null) return bits;
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		float front = -3.05f;   // just in front of the jacket
		// the tie: knot, then the blade getting a little wider toward the tip
		root.addChild("knot", ModelPartBuilder.create().uv(0, 0).cuboid(-0.8f, 0.4f, front - 0.9f, 1.6f, 1.3f, 0.9f), ModelTransform.NONE);
		root.addChild("tie_top", ModelPartBuilder.create().uv(0, 0).cuboid(-0.6f, 1.6f, front - 0.6f, 1.2f, 3.2f, 0.5f), ModelTransform.NONE);
		root.addChild("tie_low", ModelPartBuilder.create().uv(0, 0).cuboid(-0.8f, 4.6f, front - 0.6f, 1.6f, 2.6f, 0.5f), ModelTransform.NONE);
		// shirt collar points either side of the knot
		root.addChild("collar_l", ModelPartBuilder.create().uv(0, 16).cuboid(-0.5f, -0.4f, -0.3f, 1.0f, 1.4f, 0.4f),
				ModelTransform.of(-1.3f, 0.6f, front - 0.25f, 0f, 0f, -0.6f));
		root.addChild("collar_r", ModelPartBuilder.create().uv(0, 16).cuboid(-0.5f, -0.4f, -0.3f, 1.0f, 1.4f, 0.4f),
				ModelTransform.of(1.3f, 0.6f, front - 0.25f, 0f, 0f, 0.6f));
		// raised satin lapels following the V of the jacket
		root.addChild("lapel_l", ModelPartBuilder.create().uv(16, 0).cuboid(-0.6f, 0f, -0.35f, 1.2f, 5.6f, 0.35f),
				ModelTransform.of(-2.2f, 0.3f, front, 0f, 0f, -0.36f));
		root.addChild("lapel_r", ModelPartBuilder.create().uv(16, 0).cuboid(-0.6f, 0f, -0.35f, 1.2f, 5.6f, 0.35f),
				ModelTransform.of(2.2f, 0.3f, front, 0f, 0f, 0.36f));
		// pocket square peeking out of the breast pocket
		root.addChild("pocket", ModelPartBuilder.create().uv(0, 16).cuboid(1.6f, 2.0f, front - 0.3f, 1.4f, 0.9f, 0.3f), ModelTransform.NONE);
		root.addChild("pocket_tip", ModelPartBuilder.create().uv(0, 0).cuboid(2.0f, 1.6f, front - 0.25f, 0.6f, 0.5f, 0.25f), ModelTransform.NONE);
		// buttons
		root.addChild("button1", ModelPartBuilder.create().uv(16, 16).cuboid(-0.35f, 8.4f, front - 0.25f, 0.7f, 0.7f, 0.25f), ModelTransform.NONE);
		root.addChild("button2", ModelPartBuilder.create().uv(16, 16).cuboid(-0.35f, 9.9f, front - 0.25f, 0.7f, 0.7f, 0.25f), ModelTransform.NONE);
		bits = TexturedModelData.of(data, 32, 32).createModel();
		return bits;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity player,
			float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
		if (player.isInvisible() || !player.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.DANCE_JACKET)) return;
		matrices.push();
		getContextModel().body.rotate(matrices);
		bits().render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, OverlayTexture.DEFAULT_UV);
		matrices.pop();
	}
}
