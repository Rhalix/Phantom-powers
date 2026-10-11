package com.phantompowers.client;

import com.phantompowers.network.ShockwavePayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Glowing sound-wave rings: the guitar's Power Chord and the bosses' ground shockwaves. */
public final class ClientShockwaves {
	private static final class Wave {
		final Vec3d origin;
		final Vec3d dir;
		final float range;
		final int color;
		final int kind;
		int age;

		Wave(ShockwavePayload p) {
			this.origin = new Vec3d(p.x(), p.y(), p.z());
			Vec3d d = new Vec3d(p.dx(), p.dy(), p.dz());
			this.dir = d.lengthSquared() < 1.0E-6 ? new Vec3d(0, 1, 0) : d.normalize();
			this.range = p.range();
			this.color = p.color();
			this.kind = p.kind();
		}

		int rings() { return kind == 0 ? 7 : 3; }
		float spacing() { return kind == 0 ? 1.6f : 3.5f; }
		float speed() { return kind == 0 ? 1.5f : 0.9f; }
		int life() { return (int) (range / speed() + rings() * spacing()) + 3; }
	}

	private static final List<Wave> WAVES = new ArrayList<>();

	private ClientShockwaves() {}

	public static void add(ShockwavePayload p) {
		Wave w = new Wave(p);
		WAVES.add(w);
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null) return;
		DustParticleEffect dust = new DustParticleEffect(new Vector3f(((w.color >> 16) & 255) / 255f,
				((w.color >> 8) & 255) / 255f, (w.color & 255) / 255f), 1.5f);
		if (w.kind == 0) {
			// Musical notes and sparks riding the wave
			for (int i = 0; i < 24; i++) {
				double t = Math.random() * w.range;
				double spread = 0.4 + t * 0.4;
				Vec3d at = w.origin.add(w.dir.multiply(t)).add((Math.random() - 0.5) * spread, (Math.random() - 0.5) * spread, (Math.random() - 0.5) * spread);
				mc.world.addParticle(i % 3 == 0 ? ParticleTypes.NOTE : ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z,
						i % 3 == 0 ? Math.random() : w.dir.x * 0.3, w.dir.y * 0.3, w.dir.z * 0.3);
			}
		} else {
			for (int i = 0; i < 40; i++) {
				double a = i * Math.PI * 2 / 40;
				mc.world.addParticle(dust, w.origin.x + Math.cos(a) * 0.8, w.origin.y + 0.2, w.origin.z + Math.sin(a) * 0.8,
						Math.cos(a) * 0.6, 0.05, Math.sin(a) * 0.6);
			}
			mc.world.addParticle(ParticleTypes.EXPLOSION, w.origin.x, w.origin.y + 0.5, w.origin.z, 0, 0, 0);
		}
	}

	public static void tick() {
		WAVES.removeIf(w -> ++w.age > w.life());
	}

	public static void clear() {
		WAVES.clear();
	}

	public static void render(WorldRenderContext ctx) {
		if (WAVES.isEmpty()) return;
		VertexConsumerProvider consumers = ctx.consumers();
		MatrixStack ms = ctx.matrixStack();
		if (consumers == null || ms == null) return;
		Vec3d cam = ctx.camera().getPos();
		float partial = ctx.tickCounter().getTickDelta(true);
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
		ms.push();
		ms.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f m = ms.peek().getPositionMatrix();
		for (Wave w : WAVES) {
			int cr = (w.color >> 16) & 255, cg = (w.color >> 8) & 255, cb = w.color & 255;
			for (int k = 0; k < w.rings(); k++) {
				float a = w.age + partial - k * w.spacing();
				if (a < 0) continue;
				float dist = a * w.speed();
				if (dist > w.range) continue;
				float frac = dist / w.range;
				int alpha = (int) (235 * Math.pow(1 - frac, 0.6));
				boolean light = k % 2 == 1;
				int r = light ? (cr + 255) / 2 : cr, g = light ? (cg + 255) / 2 : cg, b = light ? (cb + 255) / 2 : cb;
				float phase = a * 0.7f + k * 1.3f;
				if (w.kind == 0) {
					Vec3d c = w.origin.add(w.dir.multiply(dist + 0.6));
					float radius = 0.45f + dist * 0.42f;
					ring(vc, m, c, w.dir, radius, 0.16f + 0.18f * (1 - frac), r, g, b, alpha, phase, 0.08f);
					ring(vc, m, c, w.dir, radius, 0.05f, 255, 255, 255, alpha * 3 / 4, phase, 0.08f);
				} else {
					Vec3d c = w.origin.add(0, 0.08, 0);
					float radius = Math.max(0.3f, dist);
					ring(vc, m, c, new Vec3d(0, 1, 0), radius, 0.45f, r, g, b, alpha, phase, 0.04f);
					wall(vc, m, c, radius, 0.5f + 0.7f * (1 - frac), r, g, b, alpha * 2 / 3, phase);
				}
			}
		}
		ms.pop();
	}

	/** A wobbly glowing band around {@code normal}, soft on both edges. */
	private static void ring(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d normal, float radius, float width,
			int r, int g, int b, int alpha, float phase, float wobble) {
		Vec3d u = normal.crossProduct(new Vec3d(0, 1, 0));
		if (u.lengthSquared() < 1.0E-4) u = normal.crossProduct(new Vec3d(1, 0, 0));
		u = u.normalize();
		Vec3d v = normal.crossProduct(u).normalize();
		int seg = 48;
		for (int i = 0; i < seg; i++) {
			float t1 = (float) (i * Math.PI * 2 / seg), t2 = (float) ((i + 1) * Math.PI * 2 / seg);
			float r1 = radius * (1 + wobble * MathHelper.sin(t1 * 7 + phase));
			float r2 = radius * (1 + wobble * MathHelper.sin(t2 * 7 + phase));
			Vec3d d1 = u.multiply(MathHelper.cos(t1)).add(v.multiply(MathHelper.sin(t1)));
			Vec3d d2 = u.multiply(MathHelper.cos(t2)).add(v.multiply(MathHelper.sin(t2)));
			Vec3d in1 = c.add(d1.multiply(Math.max(0, r1 - width))), mid1 = c.add(d1.multiply(r1)), out1 = c.add(d1.multiply(r1 + width));
			Vec3d in2 = c.add(d2.multiply(Math.max(0, r2 - width))), mid2 = c.add(d2.multiply(r2)), out2 = c.add(d2.multiply(r2 + width));
			soft(vc, m, in1, in2, mid2, mid1, r, g, b, 0, alpha);
			soft(vc, m, mid1, mid2, out2, out1, r, g, b, alpha, 0);
		}
	}

	/** A short glowing wall rising from a ground ring, fading to nothing at the top. */
	private static void wall(VertexConsumer vc, Matrix4f m, Vec3d c, float radius, float height, int r, int g, int b, int alpha, float phase) {
		int seg = 48;
		for (int i = 0; i < seg; i++) {
			float t1 = (float) (i * Math.PI * 2 / seg), t2 = (float) ((i + 1) * Math.PI * 2 / seg);
			float h1 = height * (1 + 0.35f * MathHelper.sin(t1 * 5 + phase * 1.5f));
			float h2 = height * (1 + 0.35f * MathHelper.sin(t2 * 5 + phase * 1.5f));
			Vec3d p1 = c.add(MathHelper.cos(t1) * radius, 0, MathHelper.sin(t1) * radius);
			Vec3d p2 = c.add(MathHelper.cos(t2) * radius, 0, MathHelper.sin(t2) * radius);
			soft(vc, m, p1, p2, p2.add(0, h2, 0), p1.add(0, h1, 0), r, g, b, alpha, 0);
		}
	}

	/** Quad p1-p2 at alpha a1, p3-p4 at alpha a2, drawn from both sides. */
	private static void soft(VertexConsumer vc, Matrix4f m, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int r, int g, int b, int a1, int a2) {
		v(vc, m, p1, r, g, b, a1); v(vc, m, p2, r, g, b, a1); v(vc, m, p3, r, g, b, a2); v(vc, m, p4, r, g, b, a2);
		v(vc, m, p4, r, g, b, a2); v(vc, m, p3, r, g, b, a2); v(vc, m, p2, r, g, b, a1); v(vc, m, p1, r, g, b, a1);
	}

	private static void v(VertexConsumer vc, Matrix4f m, Vec3d p, int r, int g, int b, int a) {
		vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a);
	}
}
