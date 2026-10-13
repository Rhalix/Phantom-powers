package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.GhostPowers;
import com.phantompowers.network.ShieldHitPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Ecto Shield: a dome of green energy all around you. Brightest at its edges (like a soap bubble), with a
 * honeycomb of glowing strands, energy bands sweeping up it, sparks orbiting, and ripples wherever it gets hit.
 */
public final class ClientShield {
	private record Hit(int entityId, Vec3d dir, int[] age) {}

	private static final List<Hit> HITS = new ArrayList<>();
	private static final Map<UUID, Integer> SHOWN = new HashMap<>();
	private static final int RINGS = 18, SEGS = 36;
	private static final DustParticleEffect MOTE = new DustParticleEffect(new Vector3f(0.5f, 1f, 0.7f), 0.7f);
	private static int clock;

	private ClientShield() {}

	public static void hit(ShieldHitPayload p) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null) return;
		var e = mc.world.getEntityById(p.entityId());
		if (e == null) return;
		Vec3d c = e.getPos().add(0, e.getHeight() / 2, 0);
		Vec3d d = new Vec3d(p.x(), p.y(), p.z()).subtract(c);
		if (d.lengthSquared() < 1.0E-4) return;
		HITS.add(new Hit(p.entityId(), d.normalize(), new int[]{0}));
	}

	public static void tick(MinecraftClient mc) {
		clock++;
		for (Iterator<Hit> it = HITS.iterator(); it.hasNext(); ) if (++it.next().age()[0] > 16) it.remove();
		if (mc.world == null || GhostFlags.CLIENT_SHIELDED.isEmpty()) {
			SHOWN.clear();
			return;
		}
		SHOWN.keySet().removeIf(id -> !GhostFlags.CLIENT_SHIELDED.contains(id));
		for (PlayerEntity p : mc.world.getPlayers()) {
			if (!GhostFlags.CLIENT_SHIELDED.contains(p.getUuid())) continue;
			SHOWN.merge(p.getUuid(), 1, Integer::sum);
			// sparks drifting round the dome
			if (clock % 2 == 0) {
				double a = clock * 0.35 + p.getId(), b = Math.sin(clock * 0.13) * 1.2;
				double r = GhostPowers.SHIELD_RADIUS;
				mc.world.addParticle(MOTE, p.getX() + Math.cos(a) * Math.cos(b) * r, p.getY() + p.getHeight() / 2 + Math.sin(b) * r,
						p.getZ() + Math.sin(a) * Math.cos(b) * r, 0, 0, 0);
			}
		}
	}

	public static void render(WorldRenderContext ctx) {
		if (GhostFlags.CLIENT_SHIELDED.isEmpty()) return;
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
		for (PlayerEntity p : mc.world.getPlayers()) {
			if (!GhostFlags.CLIENT_SHIELDED.contains(p.getUuid()) || p.isInvisible()) continue;
			Integer shown = SHOWN.get(p.getUuid());
			float grow = Math.min(1f, ((shown == null ? 0 : shown) + partial) / 5f);
			grow = 1f - (1f - grow) * (1f - grow);
			boolean self = p == mc.player && mc.options.getPerspective().isFirstPerson();
			Vec3d c = p.getLerpedPos(partial).add(0, p.getHeight() / 2, 0);
			float r = (float) (GhostPowers.SHIELD_RADIUS * grow * (1 + 0.02 * Math.sin((clock + partial) * 0.4)));
			dome(vc, m, c, r, cam, clock + partial, self ? 0.45f : 1f, p.getId());
		}
		ms.pop();
	}

	private static void dome(VertexConsumer vc, Matrix4f m, Vec3d c, float r, Vec3d cam, float time, float fade, int id) {
		List<Hit> hits = new ArrayList<>();
		for (Hit h : HITS) if (h.entityId() == id) hits.add(h);
		for (int i = 0; i < RINGS; i++) {
			double t0 = Math.PI * i / RINGS, t1 = Math.PI * (i + 1) / RINGS;
			for (int k = 0; k < SEGS; k++) {
				double p0 = 2 * Math.PI * k / SEGS + time * 0.01, p1 = 2 * Math.PI * (k + 1) / SEGS + time * 0.01;
				Vec3d n00 = dir(t0, p0), n01 = dir(t0, p1), n11 = dir(t1, p1), n10 = dir(t1, p0);
				int a00 = alpha(n00, c, r, cam, time, i, k, hits, fade), a01 = alpha(n01, c, r, cam, time, i, k + 1, hits, fade);
				int a11 = alpha(n11, c, r, cam, time, i + 1, k + 1, hits, fade), a10 = alpha(n10, c, r, cam, time, i + 1, k, hits, fade);
				quad(vc, m, c, r, n00, n01, n11, n10, a00, a01, a11, a10);
			}
		}
	}

	private static Vec3d dir(double theta, double phi) {
		return new Vec3d(Math.sin(theta) * Math.cos(phi), Math.cos(theta), Math.sin(theta) * Math.sin(phi));
	}

	/** Bright at the rim, honeycomb strands, rising energy bands and hit ripples. */
	private static int alpha(Vec3d n, Vec3d c, float r, Vec3d cam, float time, int ring, int seg, List<Hit> hits, float fade) {
		Vec3d p = c.add(n.multiply(r));
		Vec3d view = cam.subtract(p).normalize();
		double rim = 1 - Math.abs(n.dotProduct(view));
		double v = 0.06 + 0.55 * rim * rim;
		// honeycomb: offset every other ring so the cells stagger
		boolean strand = ring % 3 == 0 || ((seg + (ring / 3 % 2) * 2) % 4 == 0 && ring % 3 != 0);
		if (strand) v += 0.22;
		// energy band sweeping up the dome
		double y = n.y;
		double band = Math.sin(y * 6 - time * 0.25);
		v += 0.18 * Math.pow(Math.max(0, band), 6);
		for (Hit h : hits) {
			double ang = Math.acos(MathHelper.clamp(n.dotProduct(h.dir()), -1, 1));
			double front = h.age()[0] * 0.18;
			double wave = Math.exp(-Math.pow((ang - front) * 5, 2)) * (1 - h.age()[0] / 16.0);
			v += 0.9 * wave + (ang < 0.25 ? 0.6 * (1 - h.age()[0] / 16.0) : 0);
		}
		return MathHelper.clamp((int) (v * 255 * fade), 0, 255);
	}

	private static void quad(VertexConsumer vc, Matrix4f m, Vec3d c, float r, Vec3d a, Vec3d b, Vec3d cc, Vec3d d,
			int aa, int ab, int ac, int ad) {
		if (aa + ab + ac + ad < 6) return;
		float[][] pts = new float[4][];
		Vec3d[] ns = {a, b, cc, d};
		for (int i = 0; i < 4; i++) {
			pts[i] = new float[]{(float) (c.x + ns[i].x * r), (float) (c.y + ns[i].y * r), (float) (c.z + ns[i].z * r)};
		}
		int[] al = {aa, ab, ac, ad};
		for (int i = 0; i < 4; i++) vc.vertex(m, pts[i][0], pts[i][1], pts[i][2]).color(110, 255, 170, al[i]);
		for (int i = 3; i >= 0; i--) vc.vertex(m, pts[i][0], pts[i][1], pts[i][2]).color(110, 255, 170, al[i]);
	}
}
