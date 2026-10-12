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
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/ghost_tail.png");
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
		if (!(e instanceof PlayerEntity p) || GhostFlags.CLIENT_GHOSTS.isEmpty() || !GhostFlags.CLIENT_GHOSTS.contains(p.getUuid())) return false;
		if (p.hasVehicle() || p.isSwimming() || p.isFallFlying()) return false;
		// Only while really flying (not for jumps or falls)
		if (p.isMainPlayer()) return p.getAbilities().flying;
		return GhostFlags.CLIENT_FLYING.contains(p.getUuid());
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
				continue;
			}
			AIR.merge(id, 1, Integer::sum);
			if (p.isOnGround() || p.isTouchingWater()) AIR.put(id, 0);
			if (!isTailed(p) || p.isInvisible()) continue;
			// Wisp trail from the tip of the tail, thicker the faster you go
			Vec3d move = new Vec3d(p.getX() - p.prevX, p.getY() - p.prevY, p.getZ() - p.prevZ);
			double speed = move.length();
			Vec3d back = move.lengthSquared() > 1.0E-4 ? move.normalize().multiply(-1) : new Vec3d(0, -1, 0);
			Vec3d tip = p.getPos().add(0, 0.25, 0).add(back.multiply(0.4));
			int n = speed > 0.3 ? 3 : speed > 0.05 ? 2 : (p.age % 3 == 0 ? 1 : 0);
			for (int i = 0; i < n; i++) {
				double t = i / 3.0;
				Vec3d at = tip.subtract(move.multiply(t));
				mc.world.addParticle(i % 2 == 0 ? WISP : GREEN, at.x + (p.getRandom().nextDouble() - 0.5) * 0.25, at.y + (p.getRandom().nextDouble() - 0.5) * 0.25,
						at.z + (p.getRandom().nextDouble() - 0.5) * 0.25, 0, 0.005, 0);
			}
			if (speed > 0.2 && p.age % 2 == 0) mc.world.addParticle(ParticleTypes.END_ROD, tip.x, tip.y, tip.z, back.x * 0.02, 0.01, back.z * 0.02);
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
		s1.pitch = 0.15f + speed * 0.9f;
		s2.pitch = 0.25f + speed * 0.35f + sway * 0.12f;
		s3.pitch = 0.35f + speed * 0.25f + sway2 * 0.18f;
		s4.pitch = 0.5f + speed * 0.15f + MathHelper.sin(age * 0.18f - 2.4f) * 0.25f;
		s2.roll = sway * 0.12f * (1 - speed * 0.5f);
		s3.roll = sway2 * 0.18f * (1 - speed * 0.5f);
		s4.roll = MathHelper.sin(age * 0.18f - 2.4f) * 0.25f;
		matrices.push();
		getContextModel().body.rotate(matrices);
		// The body's pivot is at the neck; the tail hangs from the hips
		root.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)), 0xF000F0, OverlayTexture.DEFAULT_UV, 0xE6FFFFFF);
		matrices.pop();
	}
}
