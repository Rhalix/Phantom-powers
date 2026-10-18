package com.phantompowers.client;

import com.phantompowers.entity.MissileEntity;
import com.phantompowers.PhantomMod;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.math.Vec3d;

/** A little red-and-white homing rocket with fins and a hot exhaust, pointing the way it flies. */
public class MissileRenderer extends EntityRenderer<MissileEntity> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/wrist_missile.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/wrist_missile_glow.png");
	private static ModelPart model, flame;

	public MissileRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		if (model == null) {
			model = model().createModel();
			flame = model.getChild("body").getChild("flame");
		}
	}

	private static TexturedModelData model() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-1f, -1f, -3f, 2f, 2f, 6f)
				.uv(8, 12).cuboid(-1f, -1f, 3f, 2f, 2f, 1f, new Dilation(-0.25f, -0.25f, 0f))
				.uv(0, 16).cuboid(-0.5f, -0.5f, 3.88f, 1f, 1f, 1f, new Dilation(0f, 0f, -0.12f))
				.uv(4, 16).cuboid(-0.5f, -0.5f, 4.5f, 1f, 1f, 1f, new Dilation(-0.25f))
				.uv(0, 8).cuboid(-1.5f, -1.5f, 0.25f, 3f, 3f, 1f, new Dilation(-0.35f, -0.35f, -0.25f))
				.uv(8, 8).cuboid(-2.75f, -0.5f, -3.4f, 2f, 1f, 3f, new Dilation(-0.25f, -0.3f, -0.4f))
				.uv(18, 8).cuboid(0.75f, -0.5f, -3.4f, 2f, 1f, 3f, new Dilation(-0.25f, -0.3f, -0.4f))
				.uv(16, 0).cuboid(-0.5f, -2.75f, -3.4f, 1f, 2f, 3f, new Dilation(-0.3f, -0.25f, -0.4f))
				.uv(24, 0).cuboid(-0.5f, 0.75f, -3.4f, 1f, 2f, 3f, new Dilation(-0.3f, -0.25f, -0.4f))
				.uv(14, 12).cuboid(-1f, -1f, -3.8f, 2f, 2f, 1f, new Dilation(-0.2f, -0.2f, -0.2f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_flame = p_body.addChild("flame", ModelPartBuilder.create()
				.uv(0, 12).cuboid(-1f, -1f, -1.6f, 2f, 2f, 2f, new Dilation(-0.4f))
				.uv(20, 12).cuboid(-0.5f, -0.5f, -2.8f, 1f, 1f, 2f, new Dilation(-0.05f, -0.05f, -0.4f))
				.uv(26, 12).cuboid(-0.5f, -0.5f, -4f, 1f, 1f, 2f, new Dilation(-0.2f, -0.2f, -0.4f)),
				ModelTransform.of(0f, 0f, -3.6f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 32, 32);
	}

	@Override
	public void render(MissileEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		Vec3d v = e.getVelocity();
		float ry = (float) Math.atan2(v.x, v.z);
		float rp = (float) Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z));
		matrices.push();
		matrices.translate(0, 0.15, 0);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(ry));
		matrices.multiply(RotationAxis.POSITIVE_X.rotation(-rp));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotation((e.age + tickDelta) * 0.6f));
		// flickering exhaust plume
		float f = 0.8f + 0.35f * MathHelper.sin((e.age + tickDelta) * 2.7f);
		flame.xScale = flame.yScale = 0.9f + 0.2f * f;
		flame.zScale = f;
		model.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), 0xF000F0, OverlayTexture.DEFAULT_UV);
		model.render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV);
		matrices.pop();
		super.render(e, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public Identifier getTexture(MissileEntity e) {
		return TEXTURE;
	}
}
