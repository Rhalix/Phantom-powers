package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.PhantomMod;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Ghost form: a white suit drawn over your own skin, like a layer of armor. Your face,
 * skin color and hairstyle stay yours.
 */
public class GhostSuitFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/ghost_suit.png");
	private final BipedEntityModel<AbstractClientPlayerEntity> suit;

	public GhostSuitFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx,
			BipedEntityModel<AbstractClientPlayerEntity> suit) {
		super(ctx);
		this.suit = suit;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity player,
			float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
		if (GhostFlags.CLIENT_GHOSTS.isEmpty() || !GhostFlags.CLIENT_GHOSTS.contains(player.getUuid())) return;
		if (player.isInvisible()) return;
		getContextModel().copyBipedStateTo(suit);
		suit.setVisible(true);
		suit.head.visible = false;
		suit.hat.visible = false;
		suit.render(matrices, consumers.getBuffer(RenderLayer.getArmorCutoutNoCull(TEXTURE)), light, OverlayTexture.DEFAULT_UV, -1);
	}
}
