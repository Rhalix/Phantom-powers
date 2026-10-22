package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.CollectorEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** The Collector, and his Upgraded look (armour, shoulder launcher, glowing tablet) in stage two. */
public class CollectorRenderer extends MobEntityRenderer<CollectorEntity, CollectorModel> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/collector.png");
	private static final Identifier UPGRADED = PhantomMod.id("textures/entity/collector_upgraded.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/collector_glow.png");
	private static final Identifier UPGRADED_GLOW = PhantomMod.id("textures/entity/collector_upgraded_glow.png");

	private final CollectorModel armored, green;

	public CollectorRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new CollectorModel(ctx.getPart(CollectorModel.LAYER)), 0.6f);
		this.armored = this.model;
		this.green = new CollectorModel(CollectorGreenModelData.getTexturedModelData().createModel());
		this.addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, CollectorEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				getContextModel().render(matrices, consumers.getBuffer(RenderLayer.getEyes(e.upgraded() ? UPGRADED_GLOW : GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			}
		});
	}

	@Override
	public void render(CollectorEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		this.model = SluckerModels.model(e, armored, green);
		super.render(e, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public Identifier getTexture(CollectorEntity e) {
		return SluckerModels.texture(e, TEXTURE, UPGRADED);
	}

	@Override
	protected void scale(CollectorEntity e, MatrixStack matrices, float amount) {
		float s = e.upgraded() ? 1.15f : 1.05f;
		matrices.scale(s, s, s);
	}

	@Override
	protected int getBlockLight(CollectorEntity e, BlockPos pos) {
		return 15;
	}
}
