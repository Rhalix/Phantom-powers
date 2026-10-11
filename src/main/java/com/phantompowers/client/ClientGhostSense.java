package com.phantompowers.client;

import com.phantompowers.network.GhostSensePayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Ghost sense: a twisting wisp of icy blue breath curls out of your mouth, with frost
 * sparkles, and the edges of your screen frost over for a moment.
 */
public final class ClientGhostSense {
	private static final int LIFE = 40;
	private static final int POINTS = 30;
	private static final DustParticleEffect ICE = new DustParticleEffect(new Vector3f(0.65f, 0.88f, 1.0f), 0.8f);

	private static final class Wisp {
		final int entityId;
		int age;

		Wisp(int entityId) {
			this.entityId = entityId;
		}
	}

	private static final List<Wisp> WISPS = new ArrayList<>();
	private static int selfFrost;

	private ClientGhostSense() {}

	public static void add(GhostSensePayload p) {
		WISPS.removeIf(w -> w.entityId == p.entityId());
		WISPS.add(new Wisp(p.entityId()));
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player != null && mc.player.getId() == p.entityId()) selfFrost = LIFE;
	}

	public static void clear() {
		WISPS.clear();
		selfFrost = 0;
	}

	public static void tick(MinecraftClient mc) {
		if (selfFrost > 0) selfFrost--;
		if (mc.world == null) {
			WISPS.clear();
			return;
		}
		for (Wisp w : WISPS) {
			Entity e = mc.world.getEntityById(w.entityId);
			if (e == null || w.age > 22) continue;
			Vec3d mouth = mouth(e, 1f);
			Vec3d look = e.getRotationVec(1f);
			for (int i = 0; i < 2; i++) {
				mc.world.addParticle(ParticleTypes.SNOWFLAKE, mouth.x, mouth.y, mouth.z,
						look.x * 0.06 + (Math.random() - 0.5) * 0.02, 0.01 + Math.random() * 0.02, look.z * 0.06 + (Math.random() - 0.5) * 0.02);
			}
			mc.world.addParticle(ICE, mouth.x, mouth.y, mouth.z, look.x * 0.5, 0.05, look.z * 0.5);
		}
		WISPS.removeIf(w -> ++w.age > LIFE);
	}

	private static Vec3d mouth(Entity e, float partial) {
		Vec3d look = e.getRotationVec(partial);
		return e.getLerpedPos(partial).add(0, e.getStandingEyeHeight() - 0.15, 0).add(look.multiply(0.3));
	}

	public static void render(WorldRenderContext ctx) {
		if (WISPS.isEmpty()) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		VertexConsumerProvider consumers = ctx.consumers();
		MatrixStack ms = ctx.matrixStack();
		if (mc.world == null || consumers == null || ms == null) return;
		float partial = ctx.tickCounter().getTickDelta(true);
		Vec3d cam = ctx.camera().getPos();
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
		ms.push();
		ms.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f m = ms.peek().getPositionMatrix();
		for (Wisp w : WISPS) {
			Entity e = mc.world.getEntityById(w.entityId);
			if (e == null) continue;
			float t = (w.age + partial) / LIFE;
			float grow = Math.min(1f, t * 3f);
			float fade = t < 0.55f ? 1f : Math.max(0f, 1f - (t - 0.55f) / 0.45f);
			Vec3d mouth = mouth(e, partial);
			Vec3d look = e.getRotationVec(partial);
			Vec3d right = look.crossProduct(new Vec3d(0, 1, 0));
			if (right.lengthSquared() < 1.0E-4) right = new Vec3d(1, 0, 0);
			right = right.normalize();
			Vec3d up = right.crossProduct(look).normalize();
			for (int strand = 0; strand < 2; strand++) {
				Vec3d prev = null;
				for (int i = 0; i <= POINTS; i++) {
					float s = (float) i / POINTS;
					if (s > grow) break;
					float len = 1.6f * s;
					double a = s * 10.0 - (w.age + partial) * 0.45 + strand * Math.PI;
					double rad = 0.025 + 0.2 * s;
					Vec3d pt = mouth.add(look.multiply(len))
							.add(0, 0.35 * s * s + 0.15 * t, 0)
							.add(right.multiply(Math.cos(a) * rad))
							.add(up.multiply(Math.sin(a) * rad));
					if (prev != null) {
						int alpha = (int) (200 * fade * (1f - s * 0.8f));
						float width = 0.035f * (1f - s * 0.6f) + 0.01f;
						strip(vc, m, prev, pt, up, width, alpha);
						strip(vc, m, prev, pt, right, width, alpha);
					}
					prev = pt;
				}
			}
		}
		ms.pop();
	}

	private static void strip(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, Vec3d side, float w, int alpha) {
		if (alpha <= 0) return;
		Vec3d o = side.multiply(w);
		Vec3d p1 = a.add(o), p2 = a.subtract(o), p3 = b.subtract(o), p4 = b.add(o);
		int r = 175, g = 228, bl = 255;
		v(vc, m, p1, r, g, bl, alpha);
		v(vc, m, p2, r, g, bl, alpha);
		v(vc, m, p3, r, g, bl, alpha);
		v(vc, m, p4, r, g, bl, alpha);
		v(vc, m, p4, r, g, bl, alpha);
		v(vc, m, p3, r, g, bl, alpha);
		v(vc, m, p2, r, g, bl, alpha);
		v(vc, m, p1, r, g, bl, alpha);
	}

	private static void v(VertexConsumer vc, Matrix4f m, Vec3d p, int r, int g, int b, int a) {
		vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a);
	}

	/** Frost creeping in from the screen edges. */
	public static void renderHud(DrawContext ctx, RenderTickCounter counter) {
		if (selfFrost <= 0) return;
		int w = ctx.getScaledWindowWidth();
		int h = ctx.getScaledWindowHeight();
		float strength = Math.min(1f, selfFrost / 10f) * 0.55f;
		int steps = 14;
		for (int i = 0; i < steps; i++) {
			int alpha = (int) (strength * 255 * (1f - (float) i / steps) * (1f - (float) i / steps));
			int color = (alpha << 24) | 0xCFEFFF;
			int edge = i * 3;
			ctx.fill(edge, edge, edge + 3, h - edge, color);
			ctx.fill(w - edge - 3, edge, w - edge, h - edge, color);
			ctx.fill(edge + 3, edge, w - edge - 3, edge + 3, color);
			ctx.fill(edge + 3, h - edge - 3, w - edge - 3, h - edge, color);
		}
	}
}
