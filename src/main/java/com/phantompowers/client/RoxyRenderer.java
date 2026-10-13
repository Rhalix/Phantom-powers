package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.RoxyEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class RoxyRenderer extends MobEntityRenderer<RoxyEntity, RoxyModel> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/roxy.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/roxy_glow.png");

	public RoxyRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new RoxyModel(ctx.getPart(RoxyModel.LAYER)), 0.5f);
		this.addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, RoxyEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				getContextModel().render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			}
		});
	}

	@Override
	public Identifier getTexture(RoxyEntity e) {
		return TEXTURE;
	}

	@Override
	protected void scale(RoxyEntity e, MatrixStack matrices, float amount) {
		float s = 1.0f;
		matrices.scale(s, s, s);
	}

	@Override
	protected int getBlockLight(RoxyEntity e, BlockPos pos) {
		return 15;
	}
}
