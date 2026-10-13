package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.GhostBikeEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Rev's black sport bike, now yours to ride (uses the bike half of Rev's model). */
public class GhostBikeRenderer extends EntityRenderer<GhostBikeEntity> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/rev.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/rev_glow.png");
	private final RevModel model;

	public GhostBikeRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.model = new RevModel(ctx.getPart(RevModel.LAYER));
		this.shadowRadius = 0.7f;
	}

	@Override
	public void render(GhostBikeEntity e, float yaw, float tickDelta, MatrixStack ms, VertexConsumerProvider consumers, int light) {
		ms.push();
		float y = MathHelper.lerpAngleDegrees(tickDelta, e.prevYaw, e.getYaw());
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - y));
		ms.scale(-1f, -1f, 1f);
		ms.scale(1.15f, 1.15f, 1.15f);
		ms.translate(0.0, -1.501, 0.0);
		boolean moving = e.getVelocity().horizontalLengthSquared() > 0.001;
		model.poseBikeOnly(e.wheelRoll(tickDelta), e.age + tickDelta, moving);
		model.render(ms, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, OverlayTexture.DEFAULT_UV, -1);
		model.render(ms, consumers.getBuffer(RenderLayer.getEyes(GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
		ms.pop();
		super.render(e, yaw, tickDelta, ms, consumers, light);
	}

	@Override
	public Identifier getTexture(GhostBikeEntity entity) {
		return TEXTURE;
	}
}
