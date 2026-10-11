package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.VoltageEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class VoltageRenderer extends MobEntityRenderer<VoltageEntity, VoltageModel> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/voltage.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/voltage_glow.png");
	private static final Identifier ENCORE_GLOW = PhantomMod.id("textures/entity/voltage_encore_glow.png");

	public VoltageRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new VoltageModel(ctx.getPart(VoltageModel.LAYER)), 0.0f);
		this.addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, VoltageEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				// Her hair crackles: flicker the glow
				boolean flash = e.isEncore() ? ((int) age % 3 != 0) : ((int) (age / 2) % 7 != 0);
				if (!flash) return;
				getContextModel().render(matrices, consumers.getBuffer(RenderLayer.getEyes(e.isEncore() ? ENCORE_GLOW : GLOW)),
						0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			}
		});
	}

	@Override
	public Identifier getTexture(VoltageEntity e) {
		return TEXTURE;
	}

	@Override
	protected void scale(VoltageEntity e, MatrixStack matrices, float amount) {
		float s = e.isEncore() ? 1.75f : 1.6f;
		matrices.scale(s, s, s);
	}

	@Override
	protected int getBlockLight(VoltageEntity e, BlockPos pos) {
		return 15;
	}
}
