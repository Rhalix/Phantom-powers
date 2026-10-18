package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.HunterGear;
import com.phantompowers.PhantomMod;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The wrist missile launcher: an armored bracer (white plates, a glowing energy strip, a little status screen) on the
 * hunter suit's right forearm. Hold R and the four-tube missile pod slides forward along its rail over your hand,
 * the hazard-striped cover flips open, and the laser sight and exhaust vents light up.
 */
public class WristGadgetFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("wrist_launcher"), "main");
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/wrist_launcher.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/wrist_launcher_glow.png");
	/** Aim progress per player: {previous, current}, 0 = arm down .. 1 = arm up and pod open. */
	private static final Map<UUID, float[]> AIM = new ConcurrentHashMap<>();
	private final ModelPart root, pod, cover;

	public WristGadgetFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx, ModelPart root) {
		super(ctx);
		this.root = root;
		this.pod = root.getChild("bracer").getChild("pod");
		this.cover = pod.getChild("cover");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_bracer = r.addChild("bracer", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-3.5f, 4.5f, -2.5f, 5f, 5f, 5f, new Dilation(-0.1f, 0f, -0.1f))
				.uv(10, 17).cuboid(-3.5f, 5f, -3.1f, 5f, 4f, 1f, new Dilation(-0.3f, 0f, -0.2f))
				.uv(20, 0).cuboid(-4.1f, 5f, -2f, 1f, 4f, 4f, new Dilation(-0.2f, 0f, 0f))
				.uv(48, 10).cuboid(0.65f, 5.5f, -1.5f, 1f, 3f, 3f, new Dilation(-0.25f, 0f, 0f))
				.uv(0, 10).cuboid(-4f, 9f, -3f, 6f, 1f, 6f, new Dilation(-0.4f, 0f, -0.4f))
				.uv(24, 10).cuboid(-4f, 4.1f, -3f, 6f, 1f, 6f, new Dilation(-0.35f, -0.1f, -0.35f))
				.uv(26, 17).cuboid(-4.45f, 6.1f, -1f, 1f, 2f, 2f, new Dilation(-0.4f, -0.1f, -0.1f))
				.uv(56, 10).cuboid(-2f, 4.5f, -3.6f, 2f, 5f, 1f, new Dilation(-0.2f, -0.1f, -0.3f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_pod = p_bracer.addChild("pod", ModelPartBuilder.create()
				.uv(30, 0).cuboid(-2f, -0.3f, -2.6f, 4f, 5f, 3f, new Dilation(0f, -0.3f, -0.4f))
				.uv(40, 17).cuboid(-2f, 4.2f, -2.05f, 4f, 1f, 2f, new Dilation(-0.3f, -0.2f, -0.05f))
				.uv(4, 23).cuboid(-1.9f, 4.9f, -2.02f, 2f, 1f, 1f, new Dilation(-0.3f, 0f, -0.08f))
				.uv(10, 23).cuboid(-0.1f, 4.9f, -2.02f, 2f, 1f, 1f, new Dilation(-0.3f, 0f, -0.08f))
				.uv(16, 23).cuboid(-1.9f, 4.9f, -1.07f, 2f, 1f, 1f, new Dilation(-0.3f, 0f, -0.08f))
				.uv(22, 23).cuboid(-0.1f, 4.9f, -1.07f, 2f, 1f, 1f, new Dilation(-0.3f, 0f, -0.08f))
				.uv(28, 23).cuboid(-1.4f, 5.35f, -2f, 1f, 1f, 1f, new Dilation(-0.15f, -0.25f, -0.25f))
				.uv(32, 23).cuboid(0.4f, 5.35f, -2f, 1f, 1f, 1f, new Dilation(-0.15f, -0.25f, -0.25f))
				.uv(36, 23).cuboid(-1.4f, 5.35f, -1.05f, 1f, 1f, 1f, new Dilation(-0.15f, -0.25f, -0.25f))
				.uv(40, 23).cuboid(0.4f, 5.35f, -1.05f, 1f, 1f, 1f, new Dilation(-0.15f, -0.25f, -0.25f))
				.uv(52, 17).cuboid(-2f, -0.8f, -2.15f, 4f, 1f, 2f, new Dilation(-0.4f, -0.2f, -0.25f))
				.uv(32, 17).cuboid(-2.65f, 0.8f, -1.6f, 1f, 3f, 1f, new Dilation(-0.35f, -0.2f, 0f))
				.uv(36, 17).cuboid(1.65f, 0.8f, -1.6f, 1f, 3f, 1f, new Dilation(-0.35f, -0.2f, 0f))
				.uv(22, 17).cuboid(-0.5f, 0.2f, -3f, 1f, 4f, 1f, new Dilation(-0.1f, -0.4f, -0.2f))
				.uv(0, 23).cuboid(0.75f, -1.7f, -2.65f, 1f, 2f, 1f, new Dilation(-0.35f, -0.1f, -0.35f)),
				ModelTransform.of(-1f, 4.6f, -3.3f, 0f, 0f, 0f));
		ModelPartData p_cover = p_pod.addChild("cover", ModelPartBuilder.create()
				.uv(0, 17).cuboid(-2f, -0.3f, -0.7f, 4f, 5f, 1f, new Dilation(0f, -0.3f, -0.3f)),
				ModelTransform.of(0f, 0f, -2.2f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 64, 64);
	}

	public static boolean wearing(PlayerEntity p) {
		return !GhostFlags.CLIENT_SUITED.isEmpty() && GhostFlags.CLIENT_SUITED.contains(p.getUuid())
				&& HunterGear.hasMissiles(p.getEquippedStack(EquipmentSlot.CHEST));
	}

	public static void tick(MinecraftClient mc) {
		if (mc.world == null) {
			AIM.clear();
			return;
		}
		for (PlayerEntity p : mc.world.getPlayers()) {
			boolean aiming = !GhostFlags.CLIENT_AIMING.isEmpty() && GhostFlags.CLIENT_AIMING.contains(p.getUuid());
			float[] a = AIM.get(p.getUuid());
			if (a == null) {
				if (!aiming) continue;
				a = new float[2];
				AIM.put(p.getUuid(), a);
			}
			a[0] = a[1];
			a[1] = MathHelper.clamp(a[1] + (aiming ? 0.25f : -0.2f), 0f, 1f);
			if (!aiming && a[0] == 0f && a[1] == 0f) AIM.remove(p.getUuid());
		}
	}

	public static void clear() {
		AIM.clear();
	}

	/** Smoothed aim amount for posing the arm. */
	public static float aim(Entity e, float partial) {
		float[] a = AIM.get(e.getUuid());
		if (a == null) return 0f;
		float t = MathHelper.lerp(partial, a[0], a[1]);
		return t * t * (3 - 2 * t);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity p,
			float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
		if (p.isInvisible() || !wearing(p)) return;
		float e = aim(p, tickDelta);
		// pod slides forward over the back of the hand, the cover flips open
		pod.pivotY = 4.6f + 3f * e;
		cover.pitch = -1.0f * e;
		matrices.push();
		getContextModel().rightArm.rotate(matrices);
		if (p.getSkinTextures().model() == SkinTextures.Model.SLIM) {
			matrices.translate(0.5f / 16f, 0, 0);
			matrices.scale(0.82f, 1f, 1f);
		}
		root.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, OverlayTexture.DEFAULT_UV);
		if (e > 0.05f) {
			int a = (int) (255 * e);
			root.render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV, a << 24 | 0xFFFFFF);
		}
		matrices.pop();
	}
}
