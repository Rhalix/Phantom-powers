package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.network.DomeHitPayload;
import com.phantompowers.network.DomePayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The Ecto-Shield dome, drawn for real: a shimmering hexagon-plated green hemisphere that grows up out of the
 * ground, glows brighter at its rim and base, ripples out from wherever a ghost slams into it, and is fed by
 * pulsing beams rising from each emitter.
 */
public final class ClientDomes {
	private static final Identifier HEX = PhantomMod.id("textures/misc/ecto_dome.png");
	private static final int LAT = 22, LON = 72;
	private static final float CELL = 2.2f;
	private static final int GROW = 24, COLLAPSE = 16;

	private static final class Dome {
		Vec3d center;
		float radius;
		List<BlockPos> emitters = List.of();
		int born, seen, downAt = -1;
	}

	private record Hit(int dome, Vec3d at, int time) {}

	private static final Map<Integer, Dome> DOMES = new HashMap<>();
	private static final List<Hit> HITS = new ArrayList<>();
	private static int now;

	private ClientDomes() {}

	public static void update(DomePayload p) {
		Dome d = DOMES.get(p.id());
		if (!p.up()) {
			if (d != null && d.downAt < 0) d.downAt = now;
			return;
		}
		if (d == null || d.downAt >= 0) {
			d = new Dome();
			d.born = now;
			DOMES.put(p.id(), d);
		}
		d.center = new Vec3d(p.x(), p.y(), p.z());
		d.radius = p.radius();
		d.emitters = p.emitters();
		d.seen = now;
	}

	public static void hit(DomeHitPayload p) {
		HITS.add(new Hit(p.domeId(), new Vec3d(p.x(), p.y(), p.z()), now));
		if (HITS.size() > 24) HITS.remove(0);
	}

	public static void clear() {
		DOMES.clear();
		HITS.clear();
	}

	public static void tick(MinecraftClient mc) {
		now++;
		HITS.removeIf(h -> now - h.time > 26);
		Iterator<Dome> it = DOMES.values().iterator();
		while (it.hasNext()) {
			Dome d = it.next();
			if (d.downAt >= 0 ? now - d.downAt > COLLAPSE : now - d.seen > 70) {
				it.remove();
				continue;
			}
			if (mc.world == null || mc.player == null || d.downAt >= 0) continue;
			if (mc.player.squaredDistanceTo(d.center) > 120 * 120) continue;
			// sparks crawling over the base ring and up the emitter beams
			double a = mc.world.random.nextDouble() * Math.PI * 2;
			mc.world.addParticle(ParticleTypes.ELECTRIC_SPARK, d.center.x + Math.cos(a) * d.radius, d.center.y + 0.1, d.center.z + Math.sin(a) * d.radius, 0, 0.05, 0);
			for (BlockPos e : d.emitters) {
				if (mc.world.random.nextInt(3) == 0) {
					mc.world.addParticle(ParticleTypes.GLOW, e.getX() + 0.5, e.getY() + 1.1, e.getZ() + 0.5, 0, 0.25, 0);
				}
			}
		}
	}

	/** 0..1 how "raised" the dome is right now (grows in, collapses out). */
	private static float raised(Dome d, float partial) {
		float t = now + partial;
		float grow = MathHelper.clamp((t - d.born) / GROW, 0f, 1f);
		grow = 1f - (1f - grow) * (1f - grow) * (1f - grow);
		if (d.downAt >= 0) grow *= 1f - MathHelper.clamp((t - d.downAt) / COLLAPSE, 0f, 1f);
		return grow;
	}

	public static void render(WorldRenderContext ctx) {
		if (DOMES.isEmpty()) return;
		VertexConsumerProvider consumers = ctx.consumers();
		MatrixStack ms = ctx.matrixStack();
		if (consumers == null || ms == null) return;
		float partial = ctx.tickCounter().getTickDelta(true);
		Vec3d cam = ctx.camera().getPos();
		ms.push();
		ms.translate(-cam.x, -cam.y, -cam.z);
		MatrixStack.Entry entry = ms.peek();
		Matrix4f m = entry.getPositionMatrix();
		// 1) the hex-plated shells
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getEyes(HEX));
		for (Map.Entry<Integer, Dome> e : DOMES.entrySet()) {
			Dome d = e.getValue();
			if (d.center.squaredDistanceTo(cam) > 200 * 200) continue;
			shell(vc, entry, m, e.getKey(), d, cam, partial);
		}
		// 2) the glowing base ring and the emitter beams
		VertexConsumer lc = consumers.getBuffer(RenderLayer.getLightning());
		for (Dome d : DOMES.values()) {
			if (d.center.squaredDistanceTo(cam) > 200 * 200) continue;
			beams(lc, m, d, partial);
		}
		ms.pop();
	}

	private static void shell(VertexConsumer vc, MatrixStack.Entry entry, Matrix4f m, int id, Dome d, Vec3d cam, float partial) {
		float up = raised(d, partial);
		if (up <= 0.01f) return;
		float t = now + partial;
		double r = d.radius * (0.15 + 0.85 * up);
		// while growing, the dome is only drawn up to a rising front
		double topLat = Math.PI / 2 * Math.min(1f, up * 1.25f);
		int lon = LON;
		int repU = Math.max(6, Math.round((float) (2 * Math.PI * r / CELL)));
		float vScale = (float) (r / (CELL * 0.866f));
		List<Hit> hits = new ArrayList<>();
		for (Hit h : HITS) if (h.dome == id) hits.add(h);
		float collapseFlicker = d.downAt >= 0 ? (MathHelper.sin(t * 2.3f) * 0.5f + 0.5f) : 1f;
		for (int i = 0; i < LAT; i++) {
			double la0 = topLat * i / LAT, la1 = topLat * (i + 1) / LAT;
			for (int k = 0; k < lon; k++) {
				double lo0 = Math.PI * 2 * k / lon, lo1 = Math.PI * 2 * (k + 1) / lon;
				float[] a = vert(d, r, la0, lo0, cam, t, hits, up, i == 0);
				float[] b = vert(d, r, la0, lo1, cam, t, hits, up, i == 0);
				float[] c = vert(d, r, la1, lo1, cam, t, hits, up, false);
				float[] dd = vert(d, r, la1, lo0, cam, t, hits, up, false);
				float u0 = (float) k / lon * repU, u1 = (float) (k + 1) / lon * repU;
				float v0 = (float) (la0 / (Math.PI / 2)) * vScale * 0.5f - t * 0.01f, v1 = (float) (la1 / (Math.PI / 2)) * vScale * 0.5f - t * 0.01f;
				quad(vc, entry, m, a, b, c, dd, u0, u1, v0, v1, collapseFlicker);
			}
		}
	}

	/** One dome vertex: position plus how bright the plating glows there. */
	private static float[] vert(Dome d, double r, double lat, double lon, Vec3d cam, float t, List<Hit> hits, float up, boolean base) {
		double cl = Math.cos(lat);
		double nx = Math.cos(lon) * cl, ny = Math.sin(lat), nz = Math.sin(lon) * cl;
		double x = d.center.x + nx * r, y = d.center.y + ny * r, z = d.center.z + nz * r;
		// rim glow: brighter where you see the dome edge-on
		double vx = x - cam.x, vy = y - cam.y, vz = z - cam.z;
		double vl = Math.sqrt(vx * vx + vy * vy + vz * vz) + 1e-4;
		double facing = Math.abs((nx * vx + ny * vy + nz * vz) / vl);
		float glow = 0.32f + 0.55f * (float) Math.pow(1 - facing, 2.5);
		// bright skirt along the ground and a scan line that sweeps up the dome
		glow += (float) Math.max(0, 1 - lat / 0.18) * 0.55f;
		double scan = (t * 0.012) % 1.0 * (Math.PI / 2);
		glow += (float) Math.exp(-Math.pow((lat - scan) / 0.05, 2)) * 0.35f;
		glow *= 0.85f + 0.15f * MathHelper.sin(t * 0.21f + (float) lon * 3f + (float) lat * 5f);
		// ripples from ghosts slamming into it
		for (Hit h : hits) {
			double hx = h.at.x - d.center.x, hy = h.at.y - d.center.y, hz = h.at.z - d.center.z;
			double hl = Math.sqrt(hx * hx + hy * hy + hz * hz) + 1e-4;
			double ang = Math.acos(MathHelper.clamp((hx * nx + hy * ny + hz * nz) / hl, -1, 1)) * r;
			float age = t - h.time;
			double front = age * 0.55;
			float fade = Math.max(0f, 1f - age / 26f);
			glow += (float) Math.exp(-Math.pow((ang - front) / 0.9, 2)) * 1.6f * fade;
			glow += (float) Math.exp(-ang * ang / 2.0) * 2.0f * Math.max(0f, 1f - age / 8f);
		}
		// the growing front glows white-hot
		if (up < 1f) {
			double front = Math.PI / 2 * Math.min(1f, up * 1.25f);
			glow += (float) Math.exp(-Math.pow((lat - front) / 0.08, 2)) * 1.2f;
		}
		return new float[]{(float) x, (float) y, (float) z, glow, (float) nx, (float) ny, (float) nz};
	}

	private static void quad(VertexConsumer vc, MatrixStack.Entry e, Matrix4f m, float[] a, float[] b, float[] c, float[] d,
			float u0, float u1, float v0, float v1, float flicker) {
		put(vc, e, m, a, u0, v0, flicker);
		put(vc, e, m, b, u1, v0, flicker);
		put(vc, e, m, c, u1, v1, flicker);
		put(vc, e, m, d, u0, v1, flicker);
		// and the inside face, so it shows from under the dome too
		put(vc, e, m, d, u0, v1, flicker);
		put(vc, e, m, c, u1, v1, flicker);
		put(vc, e, m, b, u1, v0, flicker);
		put(vc, e, m, a, u0, v0, flicker);
	}

	private static void put(VertexConsumer vc, MatrixStack.Entry e, Matrix4f m, float[] p, float u, float v, float flicker) {
		float g = Math.min(1.6f, p[3]) * flicker;
		int r = MathHelper.clamp((int) (70 * g + 60 * Math.max(0, g - 1)), 0, 255);
		int gr = MathHelper.clamp((int) (230 * g), 0, 255);
		int b = MathHelper.clamp((int) (130 * g + 60 * Math.max(0, g - 1)), 0, 255);
		vc.vertex(m, p[0], p[1], p[2]).color(r, gr, b, 255).texture(u, v).overlay(OverlayTexture.DEFAULT_UV)
				.light(0xF000F0).normal(e, p[4], p[5], p[6]);
	}

	private static void beams(VertexConsumer vc, Matrix4f m, Dome d, float partial) {
		float up = raised(d, partial);
		if (up <= 0.01f) return;
		float t = now + partial;
		double r = d.radius * (0.15 + 0.85 * up);
		// base ring on the ground
		int segs = 96;
		for (int k = 0; k < segs; k++) {
			double a0 = Math.PI * 2 * k / segs, a1 = Math.PI * 2 * (k + 1) / segs;
			int al = (int) (150 * up * (0.75 + 0.25 * MathHelper.sin(t * 0.3f + k * 0.7f)));
			ring(vc, m, d.center, r - 0.35, r + 0.25, a0, a1, 0.05, al);
		}
		// beams up from each emitter into the dome
		for (BlockPos e : d.emitters) {
			double ex = e.getX() + 0.5, ez = e.getZ() + 0.5, ey = e.getY() + 0.95;
			double h = Math.hypot(ex - d.center.x, ez - d.center.z);
			double topY = d.center.y + Math.sqrt(Math.max(0.5, r * r - h * h));
			if (topY < ey + 0.5) topY = ey + 0.5;
			float pulse = 0.65f + 0.35f * MathHelper.sin(t * 0.4f + e.getX());
			int al = (int) (200 * up * pulse);
			for (int s = 0; s < 2; s++) {
				double ang = s * Math.PI / 2 + t * 0.05;
				double w = 0.12;
				float dx = (float) (Math.cos(ang) * w), dz = (float) (Math.sin(ang) * w);
				beamQuad(vc, m, ex, ey, ez, topY, dx, dz, al);
			}
			// spinning halo around the crystal
			int hs = 20;
			double spin = t * 0.25;
			for (int k = 0; k < hs; k++) {
				double a0 = spin + Math.PI * 2 * k / hs, a1 = spin + Math.PI * 2 * (k + 1) / hs;
				int ha = (int) (180 * up * (k % 2 == 0 ? 1f : 0.45f));
				ring(vc, m, new Vec3d(ex, ey - 0.05 + 0.06 * MathHelper.sin(t * 0.2f), ez), 0.42, 0.55, a0, a1, 0, ha);
			}
		}
	}

	private static void ring(VertexConsumer vc, Matrix4f m, Vec3d c, double r0, double r1, double a0, double a1, double y, int al) {
		float x00 = (float) (c.x + Math.cos(a0) * r0), z00 = (float) (c.z + Math.sin(a0) * r0);
		float x01 = (float) (c.x + Math.cos(a1) * r0), z01 = (float) (c.z + Math.sin(a1) * r0);
		float x10 = (float) (c.x + Math.cos(a0) * r1), z10 = (float) (c.z + Math.sin(a0) * r1);
		float x11 = (float) (c.x + Math.cos(a1) * r1), z11 = (float) (c.z + Math.sin(a1) * r1);
		float yy = (float) (c.y + y);
		vc.vertex(m, x00, yy, z00).color(90, 255, 150, al);
		vc.vertex(m, x01, yy, z01).color(90, 255, 150, al);
		vc.vertex(m, x11, yy, z11).color(90, 255, 150, 0);
		vc.vertex(m, x10, yy, z10).color(90, 255, 150, 0);
		vc.vertex(m, x10, yy, z10).color(90, 255, 150, 0);
		vc.vertex(m, x11, yy, z11).color(90, 255, 150, 0);
		vc.vertex(m, x01, yy, z01).color(90, 255, 150, al);
		vc.vertex(m, x00, yy, z00).color(90, 255, 150, al);
	}

	private static void beamQuad(VertexConsumer vc, Matrix4f m, double x, double y0, double z, double y1, float dx, float dz, int al) {
		float fx = (float) x, fz = (float) z, b = (float) y0, tp = (float) y1;
		vc.vertex(m, fx - dx, b, fz - dz).color(150, 255, 190, al);
		vc.vertex(m, fx + dx, b, fz + dz).color(150, 255, 190, al);
		vc.vertex(m, fx + dx, tp, fz + dz).color(90, 255, 150, al / 4);
		vc.vertex(m, fx - dx, tp, fz - dz).color(90, 255, 150, al / 4);
		vc.vertex(m, fx - dx, tp, fz - dz).color(90, 255, 150, al / 4);
		vc.vertex(m, fx + dx, tp, fz + dz).color(90, 255, 150, al / 4);
		vc.vertex(m, fx + dx, b, fz + dz).color(150, 255, 190, al);
		vc.vertex(m, fx - dx, b, fz - dz).color(150, 255, 190, al);
	}
}
