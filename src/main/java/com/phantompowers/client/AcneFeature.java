package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.PhantomMod;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Ecto acne: real little 3D zits (glowing green, with pale tips) bulging out of your face, throbbing gently. */
public class AcneFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("ecto_acne"), "main");
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/ecto_acne_3d.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/ecto_acne_3d_glow.png");
	private static final int COUNT = 20;
	private final ModelPart root;
	private final ModelPart[] zits = new ModelPart[COUNT];

	public AcneFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx, ModelPart root) {
		super(ctx);
		this.root = root;
		for (int i = 0; i < COUNT; i++) zits[i] = root.getChild("zit" + i);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		r.addChild("zit0", ModelPartBuilder.create().uv(0, 0).cuboid(-0.45f, -0.45f, -0.45f, 0.90f, 0.90f, 0.90f).uv(8, 0).cuboid(-0.20f, -0.20f, -0.45f, 0.41f, 0.41f, 0.41f), ModelTransform.pivot(-3.00f, -6.50f, -4.15f));
		r.addChild("zit1", ModelPartBuilder.create().uv(0, 0).cuboid(-0.55f, -0.55f, -0.55f, 1.10f, 1.10f, 1.10f).uv(8, 0).cuboid(-0.25f, -0.25f, -0.50f, 0.50f, 0.50f, 0.50f), ModelTransform.pivot(2.50f, -7.00f, -4.15f));
		r.addChild("zit2", ModelPartBuilder.create().uv(0, 0).cuboid(-0.45f, -0.45f, -0.45f, 0.90f, 0.90f, 0.90f).uv(8, 0).cuboid(-0.20f, -0.20f, -0.45f, 0.41f, 0.41f, 0.41f), ModelTransform.pivot(0.50f, -6.00f, -4.15f));
		r.addChild("zit3", ModelPartBuilder.create().uv(0, 0).cuboid(-0.65f, -0.65f, -0.65f, 1.30f, 1.30f, 1.30f).uv(8, 0).cuboid(-0.29f, -0.29f, -0.54f, 0.59f, 0.59f, 0.59f), ModelTransform.pivot(-1.50f, -7.20f, -4.15f));
		r.addChild("zit4", ModelPartBuilder.create().uv(0, 0).cuboid(-0.55f, -0.55f, -0.55f, 1.10f, 1.10f, 1.10f).uv(8, 0).cuboid(-0.25f, -0.25f, -0.50f, 0.50f, 0.50f, 0.50f), ModelTransform.pivot(3.00f, -2.50f, -4.15f));
		r.addChild("zit5", ModelPartBuilder.create().uv(0, 0).cuboid(-0.55f, -0.55f, -0.55f, 1.10f, 1.10f, 1.10f).uv(8, 0).cuboid(-0.25f, -0.25f, -0.50f, 0.50f, 0.50f, 0.50f), ModelTransform.pivot(-3.20f, -2.00f, -4.15f));
		r.addChild("zit6", ModelPartBuilder.create().uv(0, 0).cuboid(-0.45f, -0.45f, -0.45f, 0.90f, 0.90f, 0.90f).uv(8, 0).cuboid(-0.20f, -0.20f, -0.45f, 0.41f, 0.41f, 0.41f), ModelTransform.pivot(-2.20f, -1.20f, -4.15f));
		r.addChild("zit7", ModelPartBuilder.create().uv(0, 0).cuboid(-0.45f, -0.45f, -0.45f, 0.90f, 0.90f, 0.90f).uv(8, 0).cuboid(-0.20f, -0.20f, -0.45f, 0.41f, 0.41f, 0.41f), ModelTransform.pivot(1.80f, -1.50f, -4.15f));
		r.addChild("zit8", ModelPartBuilder.create().uv(0, 0).cuboid(-0.45f, -0.45f, -0.45f, 0.90f, 0.90f, 0.90f).uv(8, 0).cuboid(-0.20f, -0.20f, -0.45f, 0.41f, 0.41f, 0.41f), ModelTransform.pivot(0.00f, -3.00f, -4.15f));
		r.addChild("zit9", ModelPartBuilder.create().uv(0, 0).cuboid(-0.45f, -0.45f, -0.45f, 0.90f, 0.90f, 0.90f).uv(8, 0).cuboid(-0.20f, -0.20f, -0.45f, 0.41f, 0.41f, 0.41f), ModelTransform.pivot(3.30f, -4.60f, -4.15f));
		r.addChild("zit10", ModelPartBuilder.create().uv(0, 0).cuboid(-0.55f, -0.55f, -0.55f, 1.10f, 1.10f, 1.10f).uv(8, 0).cuboid(-0.25f, -0.25f, -0.50f, 0.50f, 0.50f, 0.50f), ModelTransform.pivot(-3.40f, -4.80f, -4.15f));
		r.addChild("zit11", ModelPartBuilder.create().uv(0, 0).cuboid(-0.65f, -0.65f, -0.65f, 1.30f, 1.30f, 1.30f).uv(8, 0).cuboid(-0.29f, -0.29f, -0.54f, 0.59f, 0.59f, 0.59f), ModelTransform.pivot(1.00f, -0.60f, -4.15f));
		r.addChild("zit12", ModelPartBuilder.create().uv(0, 0).cuboid(-0.55f, -0.55f, -0.55f, 1.10f, 1.10f, 1.10f).uv(8, 0).cuboid(-0.25f, -0.25f, -0.50f, 0.50f, 0.50f, 0.50f), ModelTransform.pivot(-0.80f, -0.40f, -4.15f));
		r.addChild("zit13", ModelPartBuilder.create().uv(0, 0).cuboid(-0.45f, -0.45f, -0.45f, 0.90f, 0.90f, 0.90f).uv(8, 0).cuboid(-0.20f, -0.20f, -0.45f, 0.41f, 0.41f, 0.41f), ModelTransform.pivot(2.60f, -5.60f, -4.15f));
		r.addChild("zit14", ModelPartBuilder.create().uv(0, 0).cuboid(-0.50f, -0.50f, -0.50f, 1.00f, 1.00f, 1.00f).uv(8, 0).cuboid(-0.23f, -0.23f, -0.23f, 0.45f, 0.45f, 0.45f), ModelTransform.pivot(-4.15f, -3.00f, -2.00f));
		r.addChild("zit15", ModelPartBuilder.create().uv(0, 0).cuboid(-0.50f, -0.50f, -0.50f, 1.00f, 1.00f, 1.00f).uv(8, 0).cuboid(-0.23f, -0.23f, -0.23f, 0.45f, 0.45f, 0.45f), ModelTransform.pivot(-4.15f, -5.00f, 1.00f));
		r.addChild("zit16", ModelPartBuilder.create().uv(0, 0).cuboid(-0.50f, -0.50f, -0.50f, 1.00f, 1.00f, 1.00f).uv(8, 0).cuboid(-0.23f, -0.23f, -0.23f, 0.45f, 0.45f, 0.45f), ModelTransform.pivot(-4.15f, -2.00f, 2.50f));
		r.addChild("zit17", ModelPartBuilder.create().uv(0, 0).cuboid(-0.50f, -0.50f, -0.50f, 1.00f, 1.00f, 1.00f).uv(8, 0).cuboid(-0.23f, -0.23f, -0.23f, 0.45f, 0.45f, 0.45f), ModelTransform.pivot(4.15f, -2.50f, -1.50f));
		r.addChild("zit18", ModelPartBuilder.create().uv(0, 0).cuboid(-0.50f, -0.50f, -0.50f, 1.00f, 1.00f, 1.00f).uv(8, 0).cuboid(-0.23f, -0.23f, -0.23f, 0.45f, 0.45f, 0.45f), ModelTransform.pivot(4.15f, -6.00f, 0.50f));
		r.addChild("zit19", ModelPartBuilder.create().uv(0, 0).cuboid(-0.50f, -0.50f, -0.50f, 1.00f, 1.00f, 1.00f).uv(8, 0).cuboid(-0.23f, -0.23f, -0.23f, 0.45f, 0.45f, 0.45f), ModelTransform.pivot(4.15f, -3.50f, 2.00f));
		return TexturedModelData.of(data, 16, 16);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity p,
			float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
		if (GhostFlags.CLIENT_ACNE.isEmpty() || !GhostFlags.CLIENT_ACNE.contains(p.getUuid()) || p.isInvisible()) return;
		for (int i = 0; i < COUNT; i++) {
			float s = 1f + 0.18f * MathHelper.sin(age * 0.15f + i * 1.7f);
			zits[i].xScale = zits[i].yScale = zits[i].zScale = s;
		}
		matrices.push();
		getContextModel().head.rotate(matrices);
		root.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, OverlayTexture.DEFAULT_UV);
		root.render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV,
				((int) (160 + 90 * MathHelper.sin(age * 0.2f))) << 24 | 0xFFFFFF);
		matrices.pop();
	}
}
