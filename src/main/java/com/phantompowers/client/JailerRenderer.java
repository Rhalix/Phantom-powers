package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.GhostJailerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class JailerRenderer extends MobEntityRenderer<GhostJailerEntity, JailerModel> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/ghost_jailer.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/ghost_jailer_glow.png");

	public JailerRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new JailerModel(ctx.getPart(JailerModel.LAYER)), 0.0f);
		this.addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, GhostJailerEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				getContextModel().render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW)),
						0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			}
		});
	}

	@Override
	public Identifier getTexture(GhostJailerEntity e) {
		return TEXTURE;
	}

	@Override
	protected void scale(GhostJailerEntity e, MatrixStack matrices, float amount) {
		float s = 1.75f;
		matrices.scale(s, s, s);
	}

	@Override
	protected int getBlockLight(GhostJailerEntity e, BlockPos pos) {
		return 15;
	}
}
