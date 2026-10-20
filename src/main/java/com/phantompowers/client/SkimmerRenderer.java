package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.SkimmerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SkimmerRenderer extends EntityRenderer<SkimmerEntity> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/ecto_skimmer.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/ecto_skimmer_glow.png");
	private final SkimmerModel model;

	public SkimmerRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.model = new SkimmerModel(ctx.getPart(SkimmerModel.LAYER));
		this.shadowRadius = 1.4f;
	}

	@Override
	public void render(SkimmerEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		matrices.push();
		float power = e.enginePower(tickDelta);
		float bob = MathHelper.sin((e.age + tickDelta) * 0.12f) * 0.05f * power;
		matrices.translate(0.0, bob + 0.12 * power, 0.0);
		float y = MathHelper.lerpAngleDegrees(tickDelta, e.prevYaw, e.getYaw());
		float p = MathHelper.lerp(tickDelta, e.prevPitch, e.getPitch());
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - y));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-p));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(e.roll(tickDelta)));
		matrices.scale(-1f, -1f, 1f);
		matrices.translate(0.0, -1.501, 0.0);
		Entity pilot = e.getFirstPassenger();
		model.pose(e, tickDelta, pilot != null ? pilot.getPitch(tickDelta) * MathHelper.RADIANS_PER_DEGREE : 0f);
		int color = e.phasing() ? 0x80FFFFFF : 0xFFFFFFFF;
		model.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)), light, OverlayTexture.DEFAULT_UV, color);
		int glow = (int) (0x60 + 0x9F * Math.max(0.25f, power));
		int glowColor = (glow << 24) | (glow << 16) | (glow << 8) | glow;
		model.render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW)), light, OverlayTexture.DEFAULT_UV, glowColor);
		matrices.pop();
		super.render(e, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public Identifier getTexture(SkimmerEntity entity) {
		return TEXTURE;
	}
}
