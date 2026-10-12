package com.phantompowers.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;

/**
 * During a transformation the player is drawn with their old skin, then this paints the new skin over just the
 * part the rings have already swept across, so the change follows the rings.
 */
public class TransitionFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	public TransitionFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
		super(ctx);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity player,
			float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
		LookTransition.T t = LookTransition.get(player);
		if (t == null || player.isInvisible()) return;
		double[] band = LookTransition.band(player, t, tickDelta);
		double cy = LookTransition.camY();
		var tex = LookTransition.texture(player, t.after);
		getContextModel().render(matrices,
				new ClipConsumer(consumers.getBuffer(RenderLayer.getEntityTranslucent(tex)), band[0] - cy, band[1] - cy),
				light, LivingEntityRenderer.getOverlay(player, 0f), -1);
	}
}
