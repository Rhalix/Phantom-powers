package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.CrateKingEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class CrateKingRenderer extends MobEntityRenderer<CrateKingEntity, CrateKingModel> {
	private static final Identifier NORMAL = PhantomMod.id("textures/entity/crate_king.png");
	private static final Identifier EVIL = PhantomMod.id("textures/entity/crate_king_evil.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/crate_king_glow.png");
	private static final Identifier EVIL_GLOW = PhantomMod.id("textures/entity/crate_king_evil_glow.png");

	public CrateKingRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new CrateKingModel(ctx.getPart(CrateKingModel.LAYER)), 0.0f);
		this.addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, CrateKingEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				getContextModel().render(matrices, consumers.getBuffer(RenderLayer.getEyes(e.isEvil() ? EVIL_GLOW : GLOW)),
						0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			}
		});
	}

	@Override
	public Identifier getTexture(CrateKingEntity e) {
		return e.isEvil() ? EVIL : NORMAL;
	}

	@Override
	protected void scale(CrateKingEntity e, MatrixStack matrices, float amount) {
		float s = e.isEvil() ? 1.85f : 1.7f;
		matrices.scale(s, s, s);
	}

	@Override
	protected int getBlockLight(CrateKingEntity e, BlockPos pos) {
		return 15;
	}
}
