package com.phantompowers.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Real 3D transformation particles: glowing tumbling cubes and crystal shards with light trails. While the rings
 * sweep they spiral round you, riding the rings up and down; when the change finishes they burst outward.
 */
public final class ClientSparks {
	private static final class Mote {
		Vec3d pos, prev, vel;
		final int entityId;           // >= 0: orbits this entity
		double angle, radius, height, spin, rise;
		final Quaternionf rot = new Quaternionf();
		final Vector3f axis;
		final float rotSpeed, size;
		final int life, r, g, b;
		final boolean shard;
		int age;

		Mote(int entityId, Vec3d pos, Vec3d vel, float size, int life, int color, boolean shard, Random rnd) {
			this.entityId = entityId;
			this.pos = this.prev = pos;
			this.vel = vel;
			this.size = size;
			this.life = life;
			this.r = (color >> 16) & 255;
			this.g = (color >> 8) & 255;
			this.b = color & 255;
			this.shard = shard;
			this.axis = new Vector3f(rnd.nextFloat() - 0.5f, rnd.nextFloat() - 0.5f, rnd.nextFloat() - 0.5f).normalize();
			this.rotSpeed = 0.15f + rnd.nextFloat() * 0.3f;
			rot.rotateAxis(rnd.nextFloat() * 6.28f, axis);
		}
	}

	private static final List<Mote> MOTES = new ArrayList<>();
	private static final Random RND = Random.create();
	private static final int MAX = 600;

	private ClientSparks() {}

	public static void clear() {
		MOTES.clear();
	}

	/** One tick of a transformation on this entity: spiral motes riding the ring band at height fraction {@code band}. */
	public static void swirl(Entity e, float band, int color, int count) {
		if (MOTES.size() > MAX) return;
		for (int i = 0; i < count; i++) {
			double a = RND.nextDouble() * Math.PI * 2;
			Mote m = new Mote(e.getId(), e.getPos(), Vec3d.ZERO, 0.035f + RND.nextFloat() * 0.05f, 16 + RND.nextInt(14),
					RND.nextInt(4) == 0 ? 0xF0FFF8 : color, RND.nextInt(3) == 0, RND);
			m.angle = a;
			m.radius = 0.55 + RND.nextDouble() * 0.35;
			m.height = MathHelper.clamp(band + (RND.nextDouble() - 0.5) * 0.25, 0, 1) * e.getHeight();
			m.spin = (0.22 + RND.nextDouble() * 0.15) * (RND.nextBoolean() ? 1 : -1);
			m.rise = (RND.nextDouble() - 0.3) * 0.04;
			m.pos = m.prev = orbitPos(e.getPos(), m);
			MOTES.add(m);
		}
	}

	/** The change is complete: a burst of shards flying out in every direction. */
	public static void burst(Vec3d at, int color, int count) {
		for (int i = 0; i < count && MOTES.size() < MAX + 200; i++) {
			Vec3d dir = new Vec3d(RND.nextGaussian(), RND.nextGaussian() * 0.6 + 0.3, RND.nextGaussian()).normalize();
			double speed = 0.18 + RND.nextDouble() * 0.25;
			MOTES.add(new Mote(-1, at.add(dir.multiply(0.4)), dir.multiply(speed), 0.04f + RND.nextFloat() * 0.07f, 18 + RND.nextInt(16),
					RND.nextInt(4) == 0 ? 0xF0FFF8 : color, RND.nextBoolean(), RND));
		}
	}

	public static void handle(com.phantompowers.network.SparkPayload p) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (p.mode() == 1) {
			Entity e = mc.world == null ? null : mc.world.getEntityById(p.entityId());
			if (e != null) swirl(e, p.band(), p.color(), p.count());
		} else {
			burst(new Vec3d(p.x(), p.y(), p.z()), p.color(), p.count());
		}
	}

	private static Vec3d orbitPos(Vec3d base, Mote m) {
		return base.add(Math.cos(m.angle) * m.radius, m.height, Math.sin(m.angle) * m.radius);
	}

	public static void tick(MinecraftClient mc) {
		if (MOTES.isEmpty()) return;
		Iterator<Mote> it = MOTES.iterator();
		while (it.hasNext()) {
			Mote m = it.next();
			m.prev = m.pos;
			if (++m.age > m.life) {
				it.remove();
				continue;
			}
			m.rot.rotateAxis(m.rotSpeed, m.axis);
			if (m.entityId >= 0) {
				Entity e = mc.world == null ? null : mc.world.getEntityById(m.entityId);
				if (e == null) {
					it.remove();
					continue;
				}
				m.angle += m.spin;
				m.radius += 0.012;
				m.height += m.rise;
				m.pos = orbitPos(e.getPos(), m);
			} else {
				m.vel = m.vel.multiply(0.88).add(0, 0.004, 0);
				m.pos = m.pos.add(m.vel);
			}
		}
	}

	public static void render(WorldRenderContext ctx) {
		if (MOTES.isEmpty()) return;
		VertexConsumerProvider consumers = ctx.consumers();
		MatrixStack ms = ctx.matrixStack();
		if (consumers == null || ms == null) return;
		float partial = ctx.tickCounter().getTickDelta(true);
		Vec3d cam = ctx.camera().getPos();
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
		ms.push();
		ms.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f mat = ms.peek().getPositionMatrix();
		for (Mote m : MOTES) {
			float t = (m.age + partial) / m.life;
			float fade = t < 0.15f ? t / 0.15f : 1f - Math.max(0f, (t - 0.6f) / 0.4f);
			if (fade <= 0f) continue;
			Vec3d p = m.prev.lerp(m.pos, partial);
			float s = m.size * (0.6f + 0.4f * fade);
			Quaternionf q = new Quaternionf(m.rot).rotateAxis(m.rotSpeed * partial, m.axis);
			int a = (int) (230 * fade);
			// bright core, then a larger faint halo around it
			if (m.shard) {
				octa(vc, mat, p, q, s * 0.8f, s * 1.6f, m.r, m.g, m.b, a);
				octa(vc, mat, p, q, s * 1.5f, s * 2.6f, m.r, m.g, m.b, a / 5);
			} else {
				cube(vc, mat, p, q, s, lighter(m.r), lighter(m.g), lighter(m.b), a);
				cube(vc, mat, p, q, s * 1.9f, m.r, m.g, m.b, a / 5);
			}
			// light trail behind it
			Vec3d tail = m.prev.lerp(m.pos, partial - 1.6f);
			trail(vc, mat, cam, tail, p, s * 0.7f, m.r, m.g, m.b, a / 2);
		}
		ms.pop();
	}

	private static int lighter(int c) {
		return Math.min(255, c + (255 - c) / 2);
	}

	private static final float[][] CUBE = {
			{-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1}, {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}};
	private static final int[][] CUBE_FACES = {{0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7}, {1, 5, 6, 2}, {3, 2, 6, 7}, {4, 5, 1, 0}};

	private static void cube(VertexConsumer vc, Matrix4f mat, Vec3d c, Quaternionf q, float s, int r, int g, int b, int a) {
		Vector3f[] v = new Vector3f[8];
		for (int i = 0; i < 8; i++) v[i] = q.transform(new Vector3f(CUBE[i][0] * s, CUBE[i][1] * s, CUBE[i][2] * s));
		for (int[] f : CUBE_FACES) {
			for (int k : f) vc.vertex(mat, (float) c.x + v[k].x, (float) c.y + v[k].y, (float) c.z + v[k].z).color(r, g, b, a);
			for (int i = 3; i >= 0; i--) vc.vertex(mat, (float) c.x + v[f[i]].x, (float) c.y + v[f[i]].y, (float) c.z + v[f[i]].z).color(r, g, b, a);
		}
	}

	/** A long crystal shard: an octahedron stretched along one axis. */
	private static void octa(VertexConsumer vc, Matrix4f mat, Vec3d c, Quaternionf q, float w, float h, int r, int g, int b, int a) {
		Vector3f top = q.transform(new Vector3f(0, h, 0)), bot = q.transform(new Vector3f(0, -h, 0));
		Vector3f[] ring = {q.transform(new Vector3f(w, 0, 0)), q.transform(new Vector3f(0, 0, w)), q.transform(new Vector3f(-w, 0, 0)), q.transform(new Vector3f(0, 0, -w))};
		for (int i = 0; i < 4; i++) {
			Vector3f p1 = ring[i], p2 = ring[(i + 1) % 4];
			tri(vc, mat, c, top, p1, p2, r, g, b, a);
			tri(vc, mat, c, bot, p2, p1, r, g, b, a);
		}
	}

	private static void tri(VertexConsumer vc, Matrix4f mat, Vec3d c, Vector3f p1, Vector3f p2, Vector3f p3, int r, int g, int b, int a) {
		float x = (float) c.x, y = (float) c.y, z = (float) c.z;
		vc.vertex(mat, x + p1.x, y + p1.y, z + p1.z).color(r, g, b, a);
		vc.vertex(mat, x + p2.x, y + p2.y, z + p2.z).color(r, g, b, a);
		vc.vertex(mat, x + p3.x, y + p3.y, z + p3.z).color(r, g, b, a);
		vc.vertex(mat, x + p3.x, y + p3.y, z + p3.z).color(r, g, b, a);
		// back side too
		vc.vertex(mat, x + p3.x, y + p3.y, z + p3.z).color(r, g, b, a);
		vc.vertex(mat, x + p2.x, y + p2.y, z + p2.z).color(r, g, b, a);
		vc.vertex(mat, x + p1.x, y + p1.y, z + p1.z).color(r, g, b, a);
		vc.vertex(mat, x + p1.x, y + p1.y, z + p1.z).color(r, g, b, a);
	}

	private static void trail(VertexConsumer vc, Matrix4f mat, Vec3d cam, Vec3d from, Vec3d to, float w, int r, int g, int b, int a) {
		Vec3d d = to.subtract(from);
		if (d.lengthSquared() < 1e-5) return;
		Vec3d side = d.crossProduct(to.subtract(cam)).normalize().multiply(w);
		if (!Double.isFinite(side.x)) return;
		float fx = (float) from.x, fy = (float) from.y, fz = (float) from.z, tx = (float) to.x, ty = (float) to.y, tz = (float) to.z;
		float sx = (float) side.x, sy = (float) side.y, sz = (float) side.z;
		vc.vertex(mat, fx, fy, fz).color(r, g, b, 0);
		vc.vertex(mat, tx - sx, ty - sy, tz - sz).color(r, g, b, a);
		vc.vertex(mat, tx + sx, ty + sy, tz + sz).color(r, g, b, a);
		vc.vertex(mat, fx, fy, fz).color(r, g, b, 0);
		vc.vertex(mat, fx, fy, fz).color(r, g, b, 0);
		vc.vertex(mat, tx + sx, ty + sy, tz + sz).color(r, g, b, a);
		vc.vertex(mat, tx - sx, ty - sy, tz - sz).color(r, g, b, a);
		vc.vertex(mat, fx, fy, fz).color(r, g, b, 0);
	}
}
