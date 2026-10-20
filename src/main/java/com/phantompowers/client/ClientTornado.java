package com.phantompowers.client;

import com.phantompowers.network.TornadoPayload;
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
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The Wail Tornado, drawn as real geometry: a swaying funnel that widens toward the top, made of glowing
 * spiral streaks spinning at different speeds, a bright twisting core with crackling energy bolts,
 * a dusty skirt at the base and debris whipping around it.
 */
public final class ClientTornado {
	private static final class Twister {
		Vec3d prev, cur;
		int age, life, fadeOut = -1;
		float scale = 1f;
		boolean dark;

		Twister(Vec3d at, int age) {
			prev = cur = at;
			this.age = age;
		}
	}

	private static final Map<Integer, Twister> ACTIVE = new HashMap<>();
	private static final float HEIGHT = 17f;
	private static final int LAYERS = 30, SEGS = 36;
	private static final DustParticleEffect GREEN = new DustParticleEffect(new Vector3f(0.3f, 1f, 0.5f), 1.6f);
	private static final DustParticleEffect RED = new DustParticleEffect(new Vector3f(1f, 0.31f, 0.78f), 1.4f);
	private static final DustParticleEffect PALE = new DustParticleEffect(new Vector3f(0.85f, 1f, 0.9f), 1.2f);

	private ClientTornado() {}

	public static void update(TornadoPayload p) {
		Vec3d at = new Vec3d(p.x(), p.y(), p.z());
		Twister t = ACTIVE.get(p.id());
		if (t == null) {
			t = new Twister(at, p.age());
			t.dark = p.id() >= 1_000_000;
			ACTIVE.put(p.id(), t);
		} else {
			t.prev = t.cur;
			t.cur = at;
			t.age = p.age();
			t.life = 0;
		}
		t.scale = p.scale() <= 0 ? 1f : p.scale();
		if (p.done()) t.fadeOut = 0;
	}

	public static void clear() {
		ACTIVE.clear();
	}

	public static void tick(MinecraftClient mc) {
		Iterator<Twister> it = ACTIVE.values().iterator();
		Random r = Random.create();
		while (it.hasNext()) {
			Twister t = it.next();
			t.life++;
			if (t.fadeOut >= 0 && ++t.fadeOut > 12) {
				it.remove();
				continue;
			}
			if (t.life > 60) { // lost track of it
				it.remove();
				continue;
			}
			Vec3d c = t.cur;
			if (mc.world == null || t.fadeOut >= 0) continue;
			// Debris and dust whipping round the funnel
			float sc = t.scale;
			for (int i = 0; i < (sc < 0.6f ? 2 : 4); i++) {
				double y = r.nextDouble() * HEIGHT * 0.9;
				double rad = radius(y) * (0.9 + r.nextDouble() * 0.35);
				y *= sc;
				rad *= sc;
				double a = r.nextDouble() * Math.PI * 2;
				double x = c.x + Math.cos(a) * rad, z = c.z + Math.sin(a) * rad;
				double vx = -Math.sin(a) * 0.55, vz = Math.cos(a) * 0.55;
				mc.world.addParticle(i == 0 ? ParticleTypes.CLOUD : ParticleTypes.POOF, x, c.y + y, z, vx, 0.08, vz);
			}
			for (int i = 0; i < 3; i++) {
				double a = r.nextDouble() * Math.PI * 2, rad = (1.5 + r.nextDouble() * 2.5) * sc;
				mc.world.addParticle(i == 0 ? PALE : (t.dark ? RED : GREEN), c.x + Math.cos(a) * rad, c.y + 0.2 + r.nextDouble() * 0.8, c.z + Math.sin(a) * rad, 0, 0, 0);
			}
		}
	}

	/** Funnel radius at a given height: narrow at the ground, flaring out at the top. */
	private static double radius(double y) {
		return 0.6 + 0.16 * y + 0.011 * y * y;
	}

	/** The whole funnel sways like a real twister. */
	private static double swayX(double y, float time) {
		return Math.sin(time * 0.045 + y * 0.13) * 0.045 * y * y * 0.25 + Math.sin(time * 0.11 + y * 0.4) * 0.15;
	}

	private static double swayZ(double y, float time) {
		return Math.cos(time * 0.038 + y * 0.11) * 0.04 * y * y * 0.25 + Math.cos(time * 0.13 + y * 0.35) * 0.15;
	}

	public static void render(WorldRenderContext ctx) {
		if (ACTIVE.isEmpty()) return;
		VertexConsumerProvider consumers = ctx.consumers();
		MatrixStack ms = ctx.matrixStack();
		if (consumers == null || ms == null) return;
		float partial = ctx.tickCounter().getTickDelta(true);
		Vec3d cam = ctx.camera().getPos();
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
		ms.push();
		ms.translate(-cam.x, -cam.y, -cam.z);
		for (Twister t : ACTIVE.values()) {
			Vec3d c = t.prev.lerp(t.cur, partial);
			ms.push();
			ms.translate(c.x, c.y, c.z);
			ms.scale(t.scale, t.scale, t.scale);
			ms.translate(-c.x, -c.y, -c.z);
			Matrix4f m = ms.peek().getPositionMatrix();
			darkNow = t.dark;
			draw(vc, m, t, c, partial);
			ms.pop();
		}
		ms.pop();
	}

	private static boolean darkNow;

	private static void draw(VertexConsumer vc, Matrix4f m, Twister t, Vec3d c, float partial) {
		{
			float time = t.age + partial;
			float fade = Math.min(1f, time / 20f);
			if (t.fadeOut >= 0) fade *= Math.max(0f, 1f - (t.fadeOut + partial) / 12f);
			if (fade <= 0) return;
			// Outer haze, main shell of spiral streaks, inner bright core
			shell(vc, m, c, time, fade, 1.18, -0.35f, 2, 0.16f, 70, 220, 120);
			shell(vc, m, c, time, fade, 1.0, -0.55f, 3, 0.55f, 90, 255, 150);
			shell(vc, m, c, time, fade, 0.62, -0.8f, 4, 0.40f, 170, 255, 205);
			shell(vc, m, c, time, fade, 0.22, -1.2f, 2, 0.85f, 235, 255, 240);
			bolts(vc, m, c, time, fade);
			skirt(vc, m, c, time, fade);
		}
	}

	/**
	 * One spinning shell of the funnel. Brightness follows spiral stripes that climb as they turn,
	 * so the funnel looks like it's twisting upward.
	 */
	private static void shell(VertexConsumer vc, Matrix4f m, Vec3d c, float time, float fade, double scale,
			float spinSpeed, int stripes, float strength, int r, int g, int b) {
		float spin = time * spinSpeed;
		for (int i = 0; i < LAYERS; i++) {
			double y0 = HEIGHT * i / LAYERS, y1 = HEIGHT * (i + 1) / LAYERS;
			double r0 = radius(y0) * scale, r1 = radius(y1) * scale;
			double x0 = c.x + swayX(y0, time), z0 = c.z + swayZ(y0, time);
			double x1 = c.x + swayX(y1, time), z1 = c.z + swayZ(y1, time);
			// fade at the very bottom and top
			float vert0 = edgeFade(y0), vert1 = edgeFade(y1);
			for (int k = 0; k < SEGS; k++) {
				double a0 = k * Math.PI * 2 / SEGS, a1 = (k + 1) * Math.PI * 2 / SEGS;
				int al00 = alpha(a0, y0, spin, stripes, strength * fade * vert0);
				int al10 = alpha(a1, y0, spin, stripes, strength * fade * vert0);
				int al01 = alpha(a0, y1, spin, stripes, strength * fade * vert1);
				int al11 = alpha(a1, y1, spin, stripes, strength * fade * vert1);
				if (al00 + al10 + al01 + al11 < 8) continue;
				float ax = (float) (x0 + Math.cos(a0) * r0), az = (float) (z0 + Math.sin(a0) * r0);
				float bx = (float) (x0 + Math.cos(a1) * r0), bz = (float) (z0 + Math.sin(a1) * r0);
				float cx = (float) (x1 + Math.cos(a1) * r1), cz = (float) (z1 + Math.sin(a1) * r1);
				float dx = (float) (x1 + Math.cos(a0) * r1), dz = (float) (z1 + Math.sin(a0) * r1);
				float yb = (float) (c.y + y0), yt = (float) (c.y + y1);
				vc.vertex(m, ax, yb, az).color(tr(r, g), tg(r, g), tb(b), al00);
				vc.vertex(m, bx, yb, bz).color(tr(r, g), tg(r, g), tb(b), al10);
				vc.vertex(m, cx, yt, cz).color(tr(r, g), tg(r, g), tb(b), al11);
				vc.vertex(m, dx, yt, dz).color(tr(r, g), tg(r, g), tb(b), al01);
				vc.vertex(m, dx, yt, dz).color(tr(r, g), tg(r, g), tb(b), al01);
				vc.vertex(m, cx, yt, cz).color(tr(r, g), tg(r, g), tb(b), al11);
				vc.vertex(m, bx, yb, bz).color(tr(r, g), tg(r, g), tb(b), al10);
				vc.vertex(m, ax, yb, az).color(tr(r, g), tg(r, g), tb(b), al00);
			}
		}
	}

	private static float edgeFade(double y) {
		float bottom = (float) Math.min(1.0, 0.35 + y / 2.0);
		float top = (float) Math.min(1.0, (HEIGHT - y) / 2.5);
		return Math.max(0f, Math.min(bottom, top));
	}

	private static int alpha(double angle, double y, float spin, int stripes, float strength) {
		double s = Math.sin(stripes * (angle + spin) - y * 0.75);
		double streak = Math.pow(Math.max(0, s), 3);
		double v = (0.18 + 0.82 * streak) * strength;
		return MathHelper.clamp((int) (v * 255), 0, 255);
	}

	/** Crackling bolts of wail energy twisting up the core. */
	private static void bolts(VertexConsumer vc, Matrix4f m, Vec3d c, float time, float fade) {
		Random r = Random.create((long) (time / 2) * 31L + 7);
		for (int bolt = 0; bolt < 3; bolt++) {
			double phase = r.nextDouble() * Math.PI * 2;
			double px = c.x + swayX(0, time), py = c.y, pz = c.z + swayZ(0, time);
			int segs = 14;
			for (int i = 1; i <= segs; i++) {
				double y = HEIGHT * 0.9 * i / segs;
				double rad = radius(y) * 0.45;
				double a = phase + time * 0.6 + y * 0.5 + (r.nextDouble() - 0.5) * 0.9;
				double nx = c.x + swayX(y, time) + Math.cos(a) * rad;
				double nz = c.z + swayZ(y, time) + Math.sin(a) * rad;
				double ny = c.y + y;
				int al = (int) (210 * fade);
				ribbon(vc, m, px, py, pz, nx, ny, nz, 0.07f, 200, 255, 230, al);
				px = nx;
				py = ny;
				pz = nz;
			}
		}
	}

	/** A dusty swirling ring where the funnel touches the ground. */
	private static void skirt(VertexConsumer vc, Matrix4f m, Vec3d c, float time, float fade) {
		int segs = 40;
		for (int ring = 0; ring < 3; ring++) {
			double rin = 1.0 + ring * 1.1, rout = rin + 0.9;
			double y = c.y + 0.08 + ring * 0.25;
			for (int k = 0; k < segs; k++) {
				double a0 = k * Math.PI * 2 / segs, a1 = (k + 1) * Math.PI * 2 / segs;
				double s = Math.sin(3 * (a0 - time * (0.35 - ring * 0.08)) + ring);
				int al = (int) (MathHelper.clamp(0.15 + 0.5 * Math.max(0, s), 0, 1) * 140 * fade * (1 - ring * 0.25));
				if (al < 4) continue;
				float x0 = (float) (c.x + Math.cos(a0) * rin), z0 = (float) (c.z + Math.sin(a0) * rin);
				float x1 = (float) (c.x + Math.cos(a1) * rin), z1 = (float) (c.z + Math.sin(a1) * rin);
				float x2 = (float) (c.x + Math.cos(a1) * rout), z2 = (float) (c.z + Math.sin(a1) * rout);
				float x3 = (float) (c.x + Math.cos(a0) * rout), z3 = (float) (c.z + Math.sin(a0) * rout);
				float yy = (float) y;
				vc.vertex(m, x0, yy, z0).color(tr(150, 255), tg(150, 255), tb(190), al);
				vc.vertex(m, x1, yy, z1).color(tr(150, 255), tg(150, 255), tb(190), al);
				vc.vertex(m, x2, yy + 0.25f, z2).color(tr(80, 200), tg(80, 200), tb(120), 0);
				vc.vertex(m, x3, yy + 0.25f, z3).color(tr(80, 200), tg(80, 200), tb(120), 0);
				vc.vertex(m, x3, yy + 0.25f, z3).color(tr(80, 200), tg(80, 200), tb(120), 0);
				vc.vertex(m, x2, yy + 0.25f, z2).color(tr(80, 200), tg(80, 200), tb(120), 0);
				vc.vertex(m, x1, yy, z1).color(tr(150, 255), tg(150, 255), tb(190), al);
				vc.vertex(m, x0, yy, z0).color(tr(150, 255), tg(150, 255), tb(190), al);
			}
		}
	}

	// Dark Tornado: the same funnel in hot pink
	private static int tr(int r, int g) {
		return darkNow ? g : r;
	}

	private static int tg(int r, int g) {
		return darkNow ? (int) (r * 0.6f) : g;
	}

	private static int tb(int b) {
		return darkNow ? Math.min(255, (int) (b * 1.3f)) : b;
	}

	/** A thin glowing strip between two points, drawn as a cross so it shows from any side. */
	private static void ribbon(VertexConsumer vc, Matrix4f m, double x0, double y0, double z0, double x1, double y1, double z1,
			float w, int r, int g, int b, int a) {
		for (int k = 0; k < 2; k++) {
			float ox = k == 0 ? w : 0, oz = k == 0 ? 0 : w;
			float ax = (float) x0, ay = (float) y0, az = (float) z0, bx = (float) x1, by = (float) y1, bz = (float) z1;
			vc.vertex(m, ax - ox, ay, az - oz).color(tr(r, g), tg(r, g), tb(b), a);
			vc.vertex(m, ax + ox, ay, az + oz).color(tr(r, g), tg(r, g), tb(b), a);
			vc.vertex(m, bx + ox, by, bz + oz).color(tr(r, g), tg(r, g), tb(b), a);
			vc.vertex(m, bx - ox, by, bz - oz).color(tr(r, g), tg(r, g), tb(b), a);
			vc.vertex(m, bx - ox, by, bz - oz).color(tr(r, g), tg(r, g), tb(b), a);
			vc.vertex(m, bx + ox, by, bz + oz).color(tr(r, g), tg(r, g), tb(b), a);
			vc.vertex(m, ax + ox, ay, az + oz).color(tr(r, g), tg(r, g), tb(b), a);
			vc.vertex(m, ax - ox, ay, az - oz).color(tr(r, g), tg(r, g), tb(b), a);
		}
	}
}
