package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.WishkeeperEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class WishkeeperRenderer extends MobEntityRenderer<WishkeeperEntity, WishkeeperModel> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/wishkeeper.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/wishkeeper_glow.png");
	private static final Identifier STORM = PhantomMod.id("textures/entity/wishkeeper_storm.png");
	private static final Identifier STORM_GLOW = PhantomMod.id("textures/entity/wishkeeper_storm_glow.png");

	public WishkeeperRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new WishkeeperModel(ctx.getPart(WishkeeperModel.LAYER)), 0.0f);
		this.addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, WishkeeperEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				getContextModel().render(matrices, consumers.getBuffer(RenderLayer.getEyes(e.isStorm() ? STORM_GLOW : GLOW)),
						0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			}
		});
	}

	@Override
	public Identifier getTexture(WishkeeperEntity e) {
		return e.isStorm() ? STORM : TEXTURE;
	}

	@Override
	protected void scale(WishkeeperEntity e, MatrixStack matrices, float amount) {
		float s = e.isStorm() ? 1.6f : 1.5f;
		matrices.scale(s, s, s);
	}

	@Override
	protected int getBlockLight(WishkeeperEntity e, BlockPos pos) {
		return 15;
	}
}
