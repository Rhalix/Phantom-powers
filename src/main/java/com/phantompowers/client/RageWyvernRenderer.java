package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.RageWyvernEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class RageWyvernRenderer extends MobEntityRenderer<RageWyvernEntity, RageWyvernModel> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/rage_wyvern.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/rage_wyvern_glow.png");

	public RageWyvernRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new RageWyvernModel(ctx.getPart(RageWyvernModel.LAYER)), 1.2f);
		this.addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, RageWyvernEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				int c = e.berserk() ? 0xFFFF7050 : -1;
				getContextModel().render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV, c);
			}
		});
	}

	@Override
	public Identifier getTexture(RageWyvernEntity e) {
		return TEXTURE;
	}

	@Override
	protected void scale(RageWyvernEntity e, MatrixStack matrices, float amount) {
		float s = 1.35f;
		matrices.scale(s, s, s);
	}

	@Override
	protected int getBlockLight(RageWyvernEntity e, BlockPos pos) {
		return 15;
	}
}
