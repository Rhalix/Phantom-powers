package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.PhantomMod;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Ecto acne: glowing green zits all over your face after a prototype portal blast. */
public class AcneFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/ecto_acne.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/ecto_acne_glow.png");

	public AcneFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
		super(ctx);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity p,
			float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
		if (GhostFlags.CLIENT_ACNE.isEmpty() || !GhostFlags.CLIENT_ACNE.contains(p.getUuid()) || p.isInvisible()) return;
		ModelPart hat = getContextModel().hat;
		boolean was = hat.visible;
		hat.visible = true;
		hat.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)), light, OverlayTexture.DEFAULT_UV);
		int pulse = (int) (150 + 100 * MathHelper.sin(age * 0.2f));
		hat.render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV, pulse << 24 | 0xFFFFFF);
		hat.visible = was;
	}
}
