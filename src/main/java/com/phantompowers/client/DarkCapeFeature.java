package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.PhantomMod;
import net.minecraft.client.model.Dilation;
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

/**
 * The dark half-ghost's regalia: a tall flared collar, spiked pauldrons, a jewelled clasp and a long
 * black cape with a crimson lining. The cape is six rows of three panels that curve around your back;
 * each row bends on its own, so the cloth ripples, streams out behind you when you run or fly,
 * swings when you turn, and settles back down when you stop.
 */
public class DarkCapeFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("dark_cape"), "main");
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/dark_cape.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/dark_cape_glow.png");
	private static final int ROWS = 6;

	private final ModelPart root, pauldronLeft, pauldronRight;
	private final ModelPart[] rows = new ModelPart[ROWS], left = new ModelPart[ROWS], right = new ModelPart[ROWS];

	public DarkCapeFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx, ModelPart root) {
		super(ctx);
		this.root = root;
		this.pauldronLeft = root.getChild("pauldron_left");
		this.pauldronRight = root.getChild("pauldron_right");
		ModelPart parent = root;
		for (int i = 0; i < ROWS; i++) {
			rows[i] = parent.getChild("cape" + i);
			left[i] = rows[i].getChild("cape" + i + "_l");
			right[i] = rows[i].getChild("cape" + i + "_r");
			parent = rows[i];
		}
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_collar_back = r.addChild("collar_back", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-5f, -7f, -0.5f, 10f, 7f, 1f)
				.uv(0, 30).cuboid(-1f, -8f, -0.5f, 2f, 1f, 1f),
				ModelTransform.of(0f, 0f, 2.2f, -0.3f, 0f, 0f));
		ModelPartData p_collar_left = p_collar_back.addChild("collar_left", ModelPartBuilder.create()
				.uv(22, 0).cuboid(0f, -6f, -0.5f, 4f, 6f, 1f)
				.uv(38, 25).cuboid(3f, -7.75f, -0.5f, 1f, 2f, 1f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(4.5f, 0f, 0f, 0f, -0.8f, 0.12f));
		ModelPartData p_collar_right = p_collar_back.addChild("collar_right", ModelPartBuilder.create()
				.uv(32, 0).cuboid(-4f, -6f, -0.5f, 4f, 6f, 1f)
				.uv(42, 25).cuboid(-4f, -7.75f, -0.5f, 1f, 2f, 1f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(-4.5f, 0f, 0f, 0f, 0.8f, -0.12f));
		ModelPartData p_pauldron_left = r.addChild("pauldron_left", ModelPartBuilder.create()
				.uv(42, 0).cuboid(-1f, -1.5f, -2.5f, 5f, 2f, 5f, new Dilation(0.25f, 0f, 0.25f))
				.uv(46, 25).cuboid(1f, -3.5f, -0.5f, 1f, 2f, 1f)
				.uv(6, 30).cuboid(2.5f, -2.5f, -0.5f, 1f, 1f, 1f),
				ModelTransform.of(5f, 0f, 0f, 0f, 0f, -0.25f));
		ModelPartData p_pauldron_right = r.addChild("pauldron_right", ModelPartBuilder.create()
				.uv(0, 8).cuboid(-4f, -1.5f, -2.5f, 5f, 2f, 5f, new Dilation(0.25f, 0f, 0.25f))
				.uv(50, 25).cuboid(-2f, -3.5f, -0.5f, 1f, 2f, 1f)
				.uv(10, 30).cuboid(-3.5f, -2.5f, -0.5f, 1f, 1f, 1f),
				ModelTransform.of(-5f, 0f, 0f, 0f, 0f, 0.25f));
		ModelPartData p_clasp = r.addChild("clasp", ModelPartBuilder.create()
				.uv(54, 25).cuboid(-1f, -1f, -0.5f, 2f, 2f, 1f)
				.uv(14, 30).cuboid(-4f, -1.75f, -0.25f, 3f, 1f, 1f, new Dilation(0f, -0.25f, -0.25f))
				.uv(22, 30).cuboid(1f, -1.75f, -0.25f, 3f, 1f, 1f, new Dilation(0f, -0.25f, -0.25f)),
				ModelTransform.of(0f, 1f, -2.4f, 0f, 0f, 0f));
		ModelPartData p_cape0 = r.addChild("cape0", ModelPartBuilder.create()
				.uv(20, 8).cuboid(-3f, 0f, 0f, 6f, 4f, 1f),
				ModelTransform.of(0f, 0.5f, 2.6f, 0.12f, 0f, 0f));
		ModelPartData p_cape0_l = p_cape0.addChild("cape0_l", ModelPartBuilder.create()
				.uv(34, 8).cuboid(0f, 0f, 0f, 3f, 4f, 1f),
				ModelTransform.of(3f, 0f, 0f, 0f, 0.45f, 0f));
		ModelPartData p_cape0_r = p_cape0.addChild("cape0_r", ModelPartBuilder.create()
				.uv(42, 8).cuboid(-3f, 0f, 0f, 3f, 4f, 1f),
				ModelTransform.of(-3f, 0f, 0f, 0f, -0.45f, 0f));
		ModelPartData p_cape1 = p_cape0.addChild("cape1", ModelPartBuilder.create()
				.uv(50, 8).cuboid(-3f, 0f, 0f, 6f, 4f, 1f),
				ModelTransform.of(0f, 4f, 0f, 0.03f, 0f, 0f));
		ModelPartData p_cape1_l = p_cape1.addChild("cape1_l", ModelPartBuilder.create()
				.uv(0, 15).cuboid(0f, 0f, 0f, 3f, 4f, 1f),
				ModelTransform.of(3f, 0f, 0f, 0f, 0.45f, 0f));
		ModelPartData p_cape1_r = p_cape1.addChild("cape1_r", ModelPartBuilder.create()
				.uv(8, 15).cuboid(-3f, 0f, 0f, 3f, 4f, 1f),
				ModelTransform.of(-3f, 0f, 0f, 0f, -0.45f, 0f));
		ModelPartData p_cape2 = p_cape1.addChild("cape2", ModelPartBuilder.create()
				.uv(16, 15).cuboid(-3.5f, 0f, 0f, 7f, 4f, 1f),
				ModelTransform.of(0f, 4f, 0f, 0.03f, 0f, 0f));
		ModelPartData p_cape2_l = p_cape2.addChild("cape2_l", ModelPartBuilder.create()
				.uv(32, 15).cuboid(0f, 0f, 0f, 3f, 4f, 1f),
				ModelTransform.of(3.5f, 0f, 0f, 0f, 0.45f, 0f));
		ModelPartData p_cape2_r = p_cape2.addChild("cape2_r", ModelPartBuilder.create()
				.uv(40, 15).cuboid(-3f, 0f, 0f, 3f, 4f, 1f),
				ModelTransform.of(-3.5f, 0f, 0f, 0f, -0.45f, 0f));
		ModelPartData p_cape3 = p_cape2.addChild("cape3", ModelPartBuilder.create()
				.uv(48, 15).cuboid(-3.5f, 0f, 0f, 7f, 4f, 1f),
				ModelTransform.of(0f, 4f, 0f, 0.03f, 0f, 0f));
		ModelPartData p_cape3_l = p_cape3.addChild("cape3_l", ModelPartBuilder.create()
				.uv(0, 20).cuboid(0f, 0f, 0f, 4f, 4f, 1f),
				ModelTransform.of(3.5f, 0f, 0f, 0f, 0.45f, 0f));
		ModelPartData p_cape3_r = p_cape3.addChild("cape3_r", ModelPartBuilder.create()
				.uv(10, 20).cuboid(-4f, 0f, 0f, 4f, 4f, 1f),
				ModelTransform.of(-3.5f, 0f, 0f, 0f, -0.45f, 0f));
		ModelPartData p_cape4 = p_cape3.addChild("cape4", ModelPartBuilder.create()
				.uv(20, 20).cuboid(-4f, 0f, 0f, 8f, 4f, 1f),
				ModelTransform.of(0f, 4f, 0f, 0.03f, 0f, 0f));
		ModelPartData p_cape4_l = p_cape4.addChild("cape4_l", ModelPartBuilder.create()
				.uv(38, 20).cuboid(0f, 0f, 0f, 4f, 4f, 1f),
				ModelTransform.of(4f, 0f, 0f, 0f, 0.45f, 0f));
		ModelPartData p_cape4_r = p_cape4.addChild("cape4_r", ModelPartBuilder.create()
				.uv(48, 20).cuboid(-4f, 0f, 0f, 4f, 4f, 1f),
				ModelTransform.of(-4f, 0f, 0f, 0f, -0.45f, 0f));
		ModelPartData p_cape5 = p_cape4.addChild("cape5", ModelPartBuilder.create()
				.uv(0, 25).cuboid(-4f, 0f, 0f, 8f, 4f, 1f),
				ModelTransform.of(0f, 4f, 0f, 0.03f, 0f, 0f));
		ModelPartData p_cape5_l = p_cape5.addChild("cape5_l", ModelPartBuilder.create()
				.uv(18, 25).cuboid(0f, 0f, 0f, 4f, 4f, 1f),
				ModelTransform.of(4f, 0f, 0f, 0f, 0.45f, 0f));
		ModelPartData p_cape5_r = p_cape5.addChild("cape5_r", ModelPartBuilder.create()
				.uv(28, 25).cuboid(-4f, 0f, 0f, 4f, 4f, 1f),
				ModelTransform.of(-4f, 0f, 0f, 0f, -0.45f, 0f));
		return TexturedModelData.of(data, 64, 64);
	}

	public static boolean wearing(AbstractClientPlayerEntity p) {
		return !GhostFlags.CLIENT_DARK.isEmpty() && GhostFlags.CLIENT_DARK.contains(p.getUuid())
				&& (GhostFlags.CLIENT_GHOSTS.contains(p.getUuid()) || previewing(p));
	}

	private static boolean previewing(AbstractClientPlayerEntity p) {
		return GhostSuitScreen.previewing && p == net.minecraft.client.MinecraftClient.getInstance().player;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity p,
			float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
		if (p.isInvisible() || !wearing(p)) return;
		animate(p, limbAngle, limbDistance, tickDelta, age);

		var vc = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
		var glow = consumers.getBuffer(RenderLayer.getEyes(GLOW));
		PlayerEntityModel<AbstractClientPlayerEntity> body = getContextModel();
		// collar, clasp and cape ride on the torso
		pauldronLeft.visible = pauldronRight.visible = false;
		matrices.push();
		body.body.rotate(matrices);
		root.render(matrices, vc, light, OverlayTexture.DEFAULT_UV, -1);
		root.render(matrices, glow, 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
		matrices.pop();
		// pauldrons ride on the shoulders, so they lift with your arms
		pauldronLeft.visible = pauldronRight.visible = true;
		pauldronLeft.setPivot(0f, -2f, 0f);
		pauldronRight.setPivot(0f, -2f, 0f);
		for (int side = 0; side < 2; side++) {
			ModelPart arm = side == 0 ? body.leftArm : body.rightArm;
			ModelPart pd = side == 0 ? pauldronLeft : pauldronRight;
			matrices.push();
			arm.rotate(matrices);
			pd.render(matrices, vc, light, OverlayTexture.DEFAULT_UV, -1);
			pd.render(matrices, glow, 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			matrices.pop();
		}
	}

	/** Cloth motion, driven by the same cape-lag physics vanilla capes use, plus layered ripples. */
	private void animate(AbstractClientPlayerEntity p, float limbAngle, float limbDistance, float td, float age) {
		double dx = MathHelper.lerp(td, p.prevCapeX, p.capeX) - MathHelper.lerp(td, p.prevX, p.getX());
		double dy = MathHelper.lerp(td, p.prevCapeY, p.capeY) - MathHelper.lerp(td, p.prevY, p.getY());
		double dz = MathHelper.lerp(td, p.prevCapeZ, p.capeZ) - MathHelper.lerp(td, p.prevZ, p.getZ());
		float yaw = MathHelper.lerp(td, p.prevBodyYaw, p.bodyYaw) * MathHelper.RADIANS_PER_DEGREE;
		double sin = MathHelper.sin(yaw), cos = -MathHelper.cos(yaw);
		float fall = MathHelper.clamp((float) dy * 10f, -6f, 32f);
		float forward = MathHelper.clamp((float) (dx * sin + dz * cos) * 100f, 0f, 150f);
		float sideways = MathHelper.clamp((float) (dx * cos - dz * sin) * 100f, -20f, 20f);
		float stride = MathHelper.lerp(td, p.prevStrideDistance, p.strideDistance);
		float bob = MathHelper.sin(MathHelper.lerp(td, p.prevHorizontalSpeed, p.horizontalSpeed) * 6f) * 32f * stride;
		float liftDeg = 6f + forward / 2f + fall + bob + (p.isInSneakingPose() ? 25f : 0f);
		float lift = MathHelper.clamp(liftDeg, 0f, 95f) * MathHelper.RADIANS_PER_DEGREE;
		// how hard the wind is blowing through the cloth (0 = standing still)
		float speed = MathHelper.clamp(forward / 90f + Math.abs(fall) / 40f, 0f, 1f);
		// flying Superman-style the body is already level, so the cape lies along your back and snaps in the wind
		float lean = SupermanFlight.lean(p, td);
		lift *= 1f - lean * 0.9f;
		float flap = 0.04f + speed * 0.22f + lean * 0.18f;
		float freq = 0.12f + speed * 0.28f + lean * 0.2f;
		// keep it off your legs while you walk
		float legs = Math.abs(MathHelper.cos(limbAngle * 0.6662f)) * Math.min(1f, limbDistance) * 0.35f * (1f - lean);

		rows[0].pitch = 0.1f + lift * 0.55f + legs * 0.6f + MathHelper.sin(age * freq) * flap * 0.25f;
		rows[0].roll = sideways * 0.5f * MathHelper.RADIANS_PER_DEGREE;
		rows[0].yaw = -sideways * 0.25f * MathHelper.RADIANS_PER_DEGREE;
		for (int i = 0; i < ROWS; i++) {
			float k = (i + 1) / (float) ROWS; // lower rows move more
			float wave = MathHelper.sin(age * freq - i * 0.95f);
			float wave2 = MathHelper.sin(age * freq * 1.7f - i * 1.3f + 1.1f);
			if (i > 0) {
				// gravity curls the cloth back down; wind keeps it streaming
				rows[i].pitch = (lift * 0.09f) + legs * 0.12f - lift * 0.05f * k + wave * flap * (0.6f + k) + 0.02f;
				rows[i].roll = wave2 * flap * 0.35f * k + sideways * 0.15f * k * MathHelper.RADIANS_PER_DEGREE;
			}
			// the side panels wrap around you when still and open up in the wind
			float wrap = 0.5f - speed * 0.3f - lean * 0.25f + (1 - k) * 0.08f;
			left[i].yaw = wrap + wave2 * flap * 0.8f * k;
			right[i].yaw = -wrap + MathHelper.sin(age * freq * 1.7f - i * 1.3f + 2.4f) * flap * 0.8f * k;
			left[i].pitch = right[i].pitch = 0f;
		}
	}
}
