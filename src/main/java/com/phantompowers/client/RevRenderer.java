package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.RevEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class RevRenderer extends MobEntityRenderer<RevEntity, RevModel> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/rev.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/rev_glow.png");

	public RevRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new RevModel(ctx.getPart(RevModel.LAYER)), 1.0f);
		this.addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, RevEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				getContextModel().render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			}
		});
	}

	@Override
	public Identifier getTexture(RevEntity e) {
		return TEXTURE;
	}

	@Override
	protected void scale(RevEntity e, MatrixStack matrices, float amount) {
		float s = 1.15f;
		matrices.scale(s, s, s);
	}

	@Override
	protected int getBlockLight(RevEntity e, BlockPos pos) {
		return Math.max(8, super.getBlockLight(e, pos));
	}
}
