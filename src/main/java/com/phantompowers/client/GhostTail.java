package com.phantompowers.client;

import com.phantompowers.GhostFlags;
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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flying in ghost form: your legs become a flowing ghost tail (white with black stripes, like the suit)
 * and you leave a glowing wisp trail behind you.
 */
public class GhostTail extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("ghost_tail"), "main");
	/** Hovering in place shows your legs; the tail only forms once you're actually moving. */
	private static final Map<UUID, Integer> MOVING = new ConcurrentHashMap<>();

	public static boolean moving(Entity e) {
		Integer m = MOVING.get(e.getUuid());
		return m != null && m > 0;
	}
	private static final DustParticleEffect WISP = new DustParticleEffect(new Vector3f(0.85f, 1f, 0.92f), 1.1f);
	private static final DustParticleEffect GREEN = new DustParticleEffect(new Vector3f(0.35f, 1f, 0.55f), 0.8f);
	/** Ticks each ghost has been off the ground (a quick hop doesn't count). */
	private static final Map<UUID, Integer> AIR = new ConcurrentHashMap<>();
	private static final int TAIL_AFTER = 4;

	private final ModelPart root, s1, s2, s3, s4;

	public GhostTail(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx, ModelPart root) {
		super(ctx);
		this.root = root;
		this.s1 = root.getChild("s1");
		this.s2 = s1.getChild("s2");
		this.s3 = s2.getChild("s3");
		this.s4 = s3.getChild("s4");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData a = r.addChild("s1", ModelPartBuilder.create().uv(0, 0).cuboid(-4f, 0f, -2f, 8, 4, 4, new Dilation(0.3f)),
				ModelTransform.pivot(0f, 12f, 0f));
		ModelPartData b = a.addChild("s2", ModelPartBuilder.create().uv(0, 8).cuboid(-3f, 0f, -1.5f, 6, 4, 3, new Dilation(0.2f)),
				ModelTransform.pivot(0f, 4f, 0f));
		ModelPartData c = b.addChild("s3", ModelPartBuilder.create().uv(0, 16).cuboid(-2f, 0f, -1f, 4, 4, 2, new Dilation(0.1f)),
				ModelTransform.pivot(0f, 4f, 0f));
		c.addChild("s4", ModelPartBuilder.create().uv(24, 0).cuboid(-1f, 0f, -0.5f, 2, 4, 1),
				ModelTransform.pivot(0f, 4f, 0f));
		return TexturedModelData.of(data, 64, 32);
	}

	/** Is this player in ghost form and flying (so they get a tail instead of legs)? */
	public static boolean isTailed(Entity e) {
		if (GhostSuitScreen.previewing && GhostSuitScreen.previewTail && e == MinecraftClient.getInstance().player) return true;
		if (!(e instanceof PlayerEntity p) || GhostFlags.CLIENT_GHOSTS.isEmpty() || !GhostFlags.CLIENT_GHOSTS.contains(p.getUuid())) return false;
		if (p.hasVehicle() || p.isSwimming() || p.isFallFlying()) return false;
		// Only while really flying (not for jumps or falls)
		boolean flying = p.isMainPlayer() ? p.getAbilities().flying : GhostFlags.CLIENT_FLYING.contains(p.getUuid());
		return flying && moving(p);
	}

	public static void tick(MinecraftClient mc) {
		if (mc.world == null) {
			AIR.clear();
			return;
		}
		if (GhostFlags.CLIENT_GHOSTS.isEmpty()) {
			AIR.clear();
			return;
		}
		for (PlayerEntity p : mc.world.getPlayers()) {
			UUID id = p.getUuid();
			if (!GhostFlags.CLIENT_GHOSTS.contains(id)) {
				AIR.remove(id);
				MOVING.remove(id);
				continue;
			}
			// moving counter with a little hysteresis: forms after ~2 ticks of motion, melts back into legs after ~8 still ticks
			double dx = p.getX() - p.prevX, dy = p.getY() - p.prevY, dz = p.getZ() - p.prevZ;
			double sp = Math.sqrt(dx * dx + dy * dy * 0.5 + dz * dz);
			int m = MOVING.getOrDefault(id, 0);
			if (sp > 0.06) m = Math.min(8, m + 4);
			else if (sp < 0.03) m = Math.max(0, m - 1);
			MOVING.put(id, m);
			AIR.merge(id, 1, Integer::sum);
			if (p.isOnGround() || p.isTouchingWater()) AIR.put(id, 0);
			if (!isTailed(p) || p.isInvisible()) continue;
			// (the glowing streak behind flying ghosts is drawn by GhostTrail)
		}
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity player,
			float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
		if (!isTailed(player) || player.isInvisible()) return;
		// Streams back the faster you fly, waves gently when you hover
		double dx = player.getX() - player.prevX, dz = player.getZ() - player.prevZ;
		float speed = (float) Math.min(1.0, Math.sqrt(dx * dx + dz * dz) * 2.5);
		float sway = MathHelper.sin(age * 0.18f);
		float sway2 = MathHelper.sin(age * 0.18f - 1.2f);
		// Flying Superman-style the tail trails straight out behind you; hovering it hangs and waves
		float straight = SupermanFlight.lean(player, tickDelta);
		float hang = 1f - straight;
		float wave = 0.35f + 0.65f * hang;
		s1.pitch = (0.15f + speed * 0.5f) * hang + 0.02f * straight;
		s2.pitch = (0.25f + speed * 0.2f) * hang + sway * 0.12f * wave;
		s3.pitch = (0.35f + speed * 0.15f) * hang + sway2 * 0.18f * wave;
		s4.pitch = (0.5f + speed * 0.1f) * hang + MathHelper.sin(age * 0.18f - 2.4f) * 0.25f * wave;
		s2.roll = sway * 0.12f * wave;
		s3.roll = sway2 * 0.18f * wave;
		s4.roll = MathHelper.sin(age * 0.18f - 2.4f) * 0.25f * wave;
		matrices.push();
		getContextModel().body.rotate(matrices);
		// The body's pivot is at the neck; the tail hangs from the hips
		root.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(SuitTextures.of(player).tail())), 0xF000F0, OverlayTexture.DEFAULT_UV, 0xE6FFFFFF);
		matrices.pop();
	}
}
