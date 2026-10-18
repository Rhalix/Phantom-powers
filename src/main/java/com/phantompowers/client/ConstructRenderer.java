package com.phantompowers.client;

import com.phantompowers.entity.EctoConstructEntity;
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

/**
 * Ecto Constructs, drawn as huge see-through weapons of glowing green energy: they fade in high above
 * the target, hang there turning, then come crashing down and dissolve.
 */
public class ConstructRenderer extends EntityRenderer<EctoConstructEntity> {
	private static final String[] NAMES = {"construct_sword", "construct_hammer", "construct_fist"};
	private static final int[] TEX_H = {64, 64, 128};
	private static final Identifier[] TEX = new Identifier[3], GLOW = new Identifier[3];
	private static ModelPart[] models;

	static {
		for (int i = 0; i < 3; i++) {
			TEX[i] = PhantomMod.id("textures/entity/" + NAMES[i] + ".png");
			GLOW[i] = PhantomMod.id("textures/entity/" + NAMES[i] + "_glow.png");
		}
	}

	public ConstructRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		if (models == null) models = new ModelPart[]{sword().createModel(), hammer().createModel(), fist().createModel()};
	}

	private static TexturedModelData sword() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_sword = r.addChild("sword", ModelPartBuilder.create()
				.uv(122, 43).cuboid(-1f, -2f, -0.5f, 2f, 2f, 1f)
				.uv(62, 43).cuboid(-2f, -5f, -1f, 4f, 3f, 2f)
				.uv(0, 43).cuboid(-3f, -9f, -1f, 6f, 4f, 2f)
				.uv(10, 0).cuboid(-4f, -38f, -1f, 8f, 29f, 2f)
				.uv(0, 0).cuboid(-0.5f, -37f, -2f, 1f, 30f, 4f, new Dilation(0f, 0f, -0.4f))
				.uv(82, 43).cuboid(-5f, -34f, -0.5f, 1f, 3f, 1f)
				.uv(86, 43).cuboid(-5f, -28f, -0.5f, 1f, 3f, 1f)
				.uv(90, 43).cuboid(-5f, -22f, -0.5f, 1f, 3f, 1f)
				.uv(94, 43).cuboid(-5f, -16f, -0.5f, 1f, 3f, 1f)
				.uv(98, 43).cuboid(-5f, -10f, -0.5f, 1f, 3f, 1f)
				.uv(102, 43).cuboid(4f, -34f, -0.5f, 1f, 3f, 1f)
				.uv(106, 43).cuboid(4f, -28f, -0.5f, 1f, 3f, 1f)
				.uv(110, 43).cuboid(4f, -22f, -0.5f, 1f, 3f, 1f)
				.uv(114, 43).cuboid(4f, -16f, -0.5f, 1f, 3f, 1f)
				.uv(118, 43).cuboid(4f, -10f, -0.5f, 1f, 3f, 1f)
				.uv(16, 43).cuboid(-5f, -41f, -1.5f, 10f, 3f, 3f)
				.uv(70, 34).cuboid(-11f, -43f, -2f, 22f, 3f, 4f)
				.uv(114, 0).cuboid(-13f, -47f, -2f, 3f, 5f, 4f)
				.uv(0, 34).cuboid(10f, -47f, -2f, 3f, 5f, 4f)
				.uv(42, 43).cuboid(-14f, -50f, -1.5f, 2f, 3f, 3f)
				.uv(52, 43).cuboid(12f, -50f, -1.5f, 2f, 3f, 3f)
				.uv(94, 0).cuboid(-2f, -45f, -3f, 4f, 4f, 6f)
				.uv(30, 0).cuboid(-1.5f, -56f, -1.5f, 3f, 11f, 3f)
				.uv(50, 34).cuboid(-2.5f, -59f, -2.5f, 5f, 3f, 5f)
				.uv(74, 43).cuboid(-1f, -62f, -1f, 2f, 3f, 2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_ring_a = p_sword.addChild("ring_a", ModelPartBuilder.create()
				.uv(0, 49).cuboid(-7f, -0.5f, -7f, 14f, 1f, 1f)
				.uv(30, 49).cuboid(-7f, -0.5f, 6f, 14f, 1f, 1f)
				.uv(42, 0).cuboid(-7f, -0.5f, -6f, 1f, 1f, 12f)
				.uv(68, 0).cuboid(6f, -0.5f, -6f, 1f, 1f, 12f),
				ModelTransform.of(0f, -22f, 0f, 0.3f, 0f, 0f));
		ModelPartData p_ring_b = p_sword.addChild("ring_b", ModelPartBuilder.create()
				.uv(60, 49).cuboid(-5f, -0.5f, -5f, 10f, 1f, 1f)
				.uv(82, 49).cuboid(-5f, -0.5f, 4f, 10f, 1f, 1f)
				.uv(14, 34).cuboid(-5f, -0.5f, -4f, 1f, 1f, 8f)
				.uv(32, 34).cuboid(4f, -0.5f, -4f, 1f, 1f, 8f),
				ModelTransform.of(0f, -12f, 0f, -0.3f, 0.6f, 0f));
		return TexturedModelData.of(data, 128, 128);
	}

	private static TexturedModelData hammer() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_hammer = r.addChild("hammer", ModelPartBuilder.create()
				.uv(16, 0).cuboid(-9f, -13f, -6f, 18f, 13f, 12f)
				.uv(76, 0).cuboid(-10f, -12f, -5f, 1f, 11f, 10f)
				.uv(98, 0).cuboid(9f, -12f, -5f, 1f, 11f, 10f)
				.uv(36, 32).cuboid(-11f, -11f, -4f, 1f, 9f, 8f)
				.uv(54, 32).cuboid(10f, -11f, -4f, 1f, 9f, 8f)
				.uv(34, 50).cuboid(-8f, -14f, -5f, 16f, 1f, 10f)
				.uv(86, 50).cuboid(-6f, -11f, -7f, 12f, 9f, 1f)
				.uv(0, 67).cuboid(-6f, -11f, 6f, 12f, 9f, 1f)
				.uv(0, 32).cuboid(-1.5f, -17f, -7.5f, 3f, 3f, 15f)
				.uv(122, 67).cuboid(-12.75f, -8f, -0.5f, 2f, 2f, 1f, new Dilation(-0.25f, 0f, 0f))
				.uv(0, 77).cuboid(10.75f, -8f, -0.5f, 2f, 2f, 1f, new Dilation(-0.25f, 0f, 0f))
				.uv(26, 67).cuboid(-3f, -17f, -3f, 6f, 4f, 6f)
				.uv(0, 0).cuboid(-2f, -45f, -2f, 4f, 28f, 4f)
				.uv(74, 67).cuboid(-2.5f, -30f, -2.5f, 5f, 2f, 5f)
				.uv(94, 67).cuboid(-2.5f, -40f, -2.5f, 5f, 2f, 5f)
				.uv(50, 67).cuboid(-3f, -48f, -3f, 6f, 3f, 6f)
				.uv(114, 67).cuboid(-1f, -51f, -1f, 2f, 3f, 2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_ring_a = p_hammer.addChild("ring_a", ModelPartBuilder.create()
				.uv(6, 77).cuboid(-14f, -0.5f, -9f, 28f, 1f, 1f)
				.uv(64, 77).cuboid(-14f, -0.5f, 8f, 28f, 1f, 1f)
				.uv(72, 32).cuboid(-14f, -0.5f, -8f, 1f, 1f, 16f)
				.uv(0, 50).cuboid(13f, -0.5f, -8f, 1f, 1f, 16f),
				ModelTransform.of(0f, -7f, 0f, 0f, 0f, 0.4f));
		return TexturedModelData.of(data, 128, 128);
	}

	private static TexturedModelData fist() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_fist = r.addChild("fist", ModelPartBuilder.create()
				.uv(50, 0).cuboid(-8f, -17f, -5f, 16f, 11f, 11f)
				.uv(62, 46).cuboid(-8.2f, -7f, -7f, 4f, 7f, 8f, new Dilation(-0.2f, 0f, 0f))
				.uv(86, 46).cuboid(-4.1f, -7f, -7f, 4f, 7f, 8f, new Dilation(-0.2f, 0f, 0f))
				.uv(0, 62).cuboid(0f, -7f, -7f, 4f, 7f, 8f, new Dilation(-0.2f, 0f, 0f))
				.uv(24, 62).cuboid(4.1f, -7f, -7f, 4f, 7f, 8f, new Dilation(-0.2f, 0f, 0f))
				.uv(92, 77).cuboid(-7.7f, -1.6f, -6.6f, 3f, 2f, 7f, new Dilation(-0.1f, -0.4f, 0f))
				.uv(0, 91).cuboid(-3.6f, -1.6f, -6.6f, 3f, 2f, 7f, new Dilation(-0.1f, -0.4f, 0f))
				.uv(20, 91).cuboid(0.5f, -1.6f, -6.6f, 3f, 2f, 7f, new Dilation(-0.1f, -0.4f, 0f))
				.uv(40, 91).cuboid(4.6f, -1.6f, -6.6f, 3f, 2f, 7f, new Dilation(-0.1f, -0.4f, 0f))
				.uv(76, 27).cuboid(-11.5f, -15f, -6f, 4f, 10f, 7f)
				.uv(60, 91).cuboid(-11f, -8f, -8f, 7f, 3f, 4f)
				.uv(0, 46).cuboid(-9f, -20f, -6.5f, 18f, 3f, 13f)
				.uv(0, 0).cuboid(-7f, -36f, -5.5f, 14f, 16f, 11f)
				.uv(48, 62).cuboid(-7.5f, -31f, -6f, 15f, 2f, 12f)
				.uv(0, 77).cuboid(-7.5f, -26f, -6f, 15f, 2f, 12f)
				.uv(54, 77).cuboid(-5.5f, -42f, -4f, 11f, 6f, 8f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_ring_a = p_fist.addChild("ring_a", ModelPartBuilder.create()
				.uv(82, 91).cuboid(-11f, -0.5f, -10f, 22f, 1f, 1f)
				.uv(0, 100).cuboid(-11f, -0.5f, 9f, 22f, 1f, 1f)
				.uv(0, 27).cuboid(-11f, -0.5f, -9f, 1f, 1f, 18f)
				.uv(38, 27).cuboid(10f, -0.5f, -9f, 1f, 1f, 18f),
				ModelTransform.of(0f, -28f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 128);
	}

	@Override
	public void render(EctoConstructEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		int kind = MathHelper.clamp(e.kind(), 0, 2);
		float t = e.age + tickDelta;
		float slam = EctoConstructEntity.SLAM;
		float grow = MathHelper.clamp(t / 8f, 0f, 1f);
		float fade = t < slam ? 1f : MathHelper.clamp(1f - (t - slam - 6f) / (EctoConstructEntity.LIFE - slam - 6f), 0f, 1f);
		// height above the target: rises in, hovers, then drops like a stone
		float height;
		if (t < slam - 6) height = 7f + 1.5f * (1 - grow) + MathHelper.sin(t * 0.25f) * 0.25f;
		else if (t < slam) {
			float k = (t - (slam - 6)) / 6f;
			height = 7.4f * (1 - k * k);
		} else height = 0f;
		float spin = t < slam ? t * 6f : slam * 6f;
		matrices.push();
		matrices.translate(0, height, 0);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw + (kind == 0 ? spin * 0.3f : 0)));
		if (kind == 1 && t < slam) {
			// the hammer winds up, head high, then swings down onto the target
			float k = t < slam - 6 ? 0f : (t - (slam - 6)) / 6f;
			matrices.translate(0, 2.8, 0);
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180f - 180f * k * k));
			matrices.translate(0, -2.8, 0);
		}
		float s = 0.2f + 0.8f * grow;
		float pulse = 1f + MathHelper.sin(t * 0.6f) * 0.03f;
		matrices.scale(-s * pulse, -s * pulse, s * pulse);
		// rune rings spin around the weapon
		ModelPart body = models[kind].getChild(kind == 0 ? "sword" : kind == 1 ? "hammer" : "fist");
		ModelPart ringA = body.getChild("ring_a");
		ringA.yaw = t * 0.25f;
		if (kind == 0) body.getChild("ring_b").yaw = -t * 0.35f;
		int a = (int) (200 * fade);
		int argb = a << 24 | 0xFFFFFF;
		if (a > 4) {
			models[kind].render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEX[kind])), 0xF000F0, OverlayTexture.DEFAULT_UV, argb);
			models[kind].render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW[kind])), 0xF000F0, OverlayTexture.DEFAULT_UV,
					((int) (255 * fade)) << 24 | 0xFFFFFF);
		}
		matrices.pop();
		super.render(e, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public boolean shouldRender(EctoConstructEntity e, net.minecraft.client.render.Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public Identifier getTexture(EctoConstructEntity e) {
		return TEX[MathHelper.clamp(e.kind(), 0, 2)];
	}
}
