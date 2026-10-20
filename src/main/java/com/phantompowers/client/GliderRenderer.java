package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.GliderEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class GliderRenderer extends EntityRenderer<GliderEntity> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/hover_glider.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/hover_glider_glow.png");
	private final GliderModel model;

	public GliderRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.model = new GliderModel(ctx.getPart(GliderModel.LAYER));
		this.shadowRadius = 0.6f;
	}

	@Override
	public void render(GliderEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		matrices.push();
		matrices.translate(0.0, 0.18 + entity.bob(tickDelta), 0.0);
		float y = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getYaw());
		float p = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - y));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-p));
		matrices.scale(-1f, -1f, 1f);
		matrices.translate(0.0, -1.5, 0.0);
		Identifier tex = entity.paint() >= 0 ? PaintTextures.painted(TEXTURE, entity.paint()) : TEXTURE;
		model.render(matrices, consumers.getBuffer(model.getLayer(tex)), light, OverlayTexture.DEFAULT_UV, -1);
		model.render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW)), light, OverlayTexture.DEFAULT_UV, -1);
		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public Identifier getTexture(GliderEntity entity) {
		return TEXTURE;
	}
}
