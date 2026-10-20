package com.phantompowers.client;

import com.phantompowers.network.RingPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Transformation rings: a glowing ring appears at the waist, then splits in two.
 * One sweeps up over the head, the other down to the feet.
 */
public final class ClientRings {
	private static final class Rings {
		final int entityId;
		final int duration;
		final int style;
		int age;

		Rings(int entityId, int duration, int style) {
			this.entityId = entityId;
			this.duration = Math.max(4, duration);
			this.style = style;
		}
	}

	private static final List<Rings> ACTIVE = new ArrayList<>();
	private static final int SEGMENTS = 40;

	private ClientRings() {}

	public static void add(RingPayload p) {
		ACTIVE.removeIf(r -> r.entityId == p.entityId());
		ACTIVE.add(new Rings(p.entityId(), p.duration(), p.style()));
		LookTransition.start(p.entityId(), p.style(), p.duration());
	}

	/** How far along this entity's ghost transformation is (0..1), or -1 if it isn't transforming. */
	public static float transformProgress(int entityId, float partial) {
		for (Rings r : ACTIVE) {
			if (r.entityId == entityId && (r.style == 0 || r.style == 4)) return Math.min(1f, (r.age + partial) / r.duration);
		}
		return -1f;
	}

	/** How far along this entity's big wail finale is (0..1), or -1 if it isn't doing one. */
	public static float wailProgress(int entityId, float partial) {
		for (Rings r : ACTIVE) {
			if (r.entityId == entityId && r.style == 2) return Math.min(1f, (r.age + partial) / r.duration);
		}
		return -1f;
	}

	/** After going ghost/human, the pose eases back to normal over this many ticks instead of snapping. */
	public static final int HERO_RELEASE = 20;

	private static boolean hero(Rings r) {
		return r.style == 0 || r.style == 4;
	}

	public static void tick() {
		ACTIVE.removeIf(r -> ++r.age > r.duration + (hero(r) ? HERO_RELEASE : 0));
	}

	/** 0 while the transformation pose is held, rising to 1 as it relaxes back to normal afterwards. */
	public static float heroRelease(int entityId, float partial) {
		for (Rings r : ACTIVE) {
			if (r.entityId == entityId && hero(r)) return Math.max(0f, Math.min(1f, (r.age + partial - r.duration) / HERO_RELEASE));
		}
		return 0f;
	}

	public static void clear() {
		ACTIVE.clear();
	}

	public static void render(WorldRenderContext ctx) {
		if (ACTIVE.isEmpty()) return;
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
		for (Rings r : ACTIVE) {
			if (r.age > r.duration) continue; // rings are gone; only the pose is still relaxing
			Entity e = mc.world.getEntityById(r.entityId);
			if (e == null) continue;
			Vec3d pos = e.getLerpedPos(partial);
			float h = e.getHeight();
			float t = Math.min(1f, (r.age + partial) / r.duration);
			if (r.style == 2) continue; // wail finale: a pose, not rings
			if (r.style == 6 || r.style == 7) {
				suitForm(consumers, m, e, pos, r.style == 7 ? 1f - t : t, r.age + partial, partial, r.style == 7);
				vc = consumers.getBuffer(RenderLayer.getLightning());
				continue;
			}
			if (r.style == 1 || r.style == 5) {
				suitWrap(consumers, m, pos, h, t, r.age + partial);
				vc = consumers.getBuffer(RenderLayer.getLightning());
				continue;
			}
			// First 20%: one ring grows at the waist. Then it splits and the halves sweep apart.
			float appear = Math.min(1f, t / 0.2f);
			float split = Math.max(0f, (t - 0.2f) / 0.8f);
			float ease = split * split * (3 - 2 * split);
			float mid = h * 0.5f;
			float fade = t > 0.85f ? (1f - t) / 0.15f : 1f;
			float radius = (0.45f + 0.3f * appear) * Math.max(0.6f, e.getWidth() / 0.6f);
			int alpha = (int) (255 * fade);
			double up = mid + (h - mid + 0.1) * ease;
			double down = mid - (mid + 0.05) * ease;
			// the dark half-ghost's rings burn hot pink
			pink = !com.phantompowers.GhostFlags.CLIENT_DARK.isEmpty() && com.phantompowers.GhostFlags.CLIENT_DARK.contains(e.getUuid());
			ring(vc, m, pos.x, pos.y + up, pos.z, radius, alpha);
			if (split > 0) ring(vc, m, pos.x, pos.y + down, pos.z, radius, alpha);
		}
		ms.pop();
	}

	/**
	 * Ghost Hunter Suit deploying: dark armor bands with glowing cyan edges spiral up around the body,
	 * each one tightening onto you as it passes.
	 */
	private static void suitWrap(VertexConsumerProvider consumers, Matrix4f m, Vec3d pos, float h, float t, float time) {
		// Two passes, one per render layer: switching layers mid-draw isn't allowed.
		for (int pass = 0; pass < 2; pass++) {
			VertexConsumer vc = consumers.getBuffer(pass == 0 ? RenderLayer.getDebugQuads() : RenderLayer.getLightning());
			float fade = t > 0.85f ? (1f - t) / 0.15f : 1f;
			int a = (int) (235 * fade);
			for (int k = 0; k < 5; k++) {
				float lt = Math.max(0f, Math.min(1f, (t - k * 0.1f) / 0.55f));
				if (lt <= 0f || lt >= 1f) continue;
				double y = pos.y + 0.05 + (h - 0.1) * lt;
				float radius = 0.62f - 0.22f * lt;
				double spin = time * 0.35 + k * 1.3;
				int segs = 28;
				double arc = Math.PI * 2 * (0.55 + 0.45 * lt);
				for (int i = 0; i < segs; i++) {
					double a1 = spin + arc * i / segs, a2 = spin + arc * (i + 1) / segs;
					float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1), c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
					float x = (float) pos.x, z = (float) pos.z, yb = (float) y - 0.09f, yt = (float) y + 0.09f;
					if (pass == 0) {
						quad(vc, m, x + c1 * radius, yb, z + s1 * radius, x + c1 * radius, yt, z + s1 * radius,
								x + c2 * radius, yt, z + s2 * radius, x + c2 * radius, yb, z + s2 * radius, 22, 24, 30, a);
					} else {
						float r2 = radius + 0.012f;
						quad(vc, m, x + c1 * r2, yt, z + s1 * r2, x + c1 * r2, yt + 0.025f, z + s1 * r2,
								x + c2 * r2, yt + 0.025f, z + s2 * r2, x + c2 * r2, yt, z + s2 * r2, 90, 225, 255, a);
						quad(vc, m, x + c1 * r2, yb - 0.025f, z + s1 * r2, x + c1 * r2, yb, z + s1 * r2,
								x + c2 * r2, yb, z + s2 * r2, x + c2 * r2, yb - 0.025f, z + s2 * r2, 90, 225, 255, a);
					}
				}
			}
		}
	}

	private static boolean pink;

	/** Where each armour plate locks on: {x, y (from the feet), z, width, height}, in the order they fly out. */
	private static final float[][] PLATES = {
			{-0.13f, 0.08f, 0f, 0.28f, 0.2f}, {0.13f, 0.08f, 0f, 0.28f, 0.2f},          // boots
			{-0.13f, 0.42f, -0.14f, 0.24f, 0.3f}, {0.13f, 0.42f, -0.14f, 0.24f, 0.3f},  // shins
			{-0.13f, 0.78f, -0.14f, 0.24f, 0.28f}, {0.13f, 0.78f, -0.14f, 0.24f, 0.28f},// thighs
			{0f, 1.0f, -0.15f, 0.46f, 0.16f},                                           // belt
			{0f, 1.25f, -0.17f, 0.42f, 0.32f},                                          // chest plate
			{-0.36f, 1.0f, 0f, 0.22f, 0.32f}, {0.36f, 1.0f, 0f, 0.22f, 0.32f},          // gauntlets
			{-0.36f, 1.4f, 0f, 0.26f, 0.18f}, {0.36f, 1.4f, 0f, 0.26f, 0.18f},          // shoulder pads
			{0f, 1.72f, -0.26f, 0.42f, 0.36f}};                                         // helmet + visor

	/**
	 * Suiting up: armour plates unfold out of the backpack one after another, swing round the body on curving paths
	 * trailing cyan light, and lock into place from the boots up to the helmet.
	 */
	private static void suitForm(VertexConsumerProvider consumers, Matrix4f m, Entity e, Vec3d pos, float t, float time, float partial, boolean off) {
		float yaw = e instanceof net.minecraft.entity.LivingEntity le ? MathHelper.lerp(partial, le.prevBodyYaw, le.bodyYaw) : e.getYaw();
		double rad = Math.toRadians(-yaw);
		double cos = Math.cos(rad), sin = Math.sin(rad);
		// local -> world (local -z is in front of the player, +z behind)
		java.util.function.Function<double[], Vec3d> toWorld = l -> new Vec3d(pos.x + l[0] * cos - l[2] * sin, pos.y + l[1], pos.z - l[0] * sin - l[2] * cos);
		double[] pack = {0, 1.25, 0.34};
		int n = PLATES.length;
		for (int pass = 0; pass < 2; pass++) {
			VertexConsumer vc = consumers.getBuffer(pass == 0 ? RenderLayer.getDebugQuads() : RenderLayer.getLightning());
			for (int i = 0; i < n; i++) {
				float start = i / (float) (n + 3), dur = 3f / (n + 3);
				float k = (t - start) / dur;
				if (k <= 0f) continue;
				float[] pl = PLATES[i];
				float fade = k > 1f ? Math.max(0f, 1f - (k - 1f) * 2.5f) : 1f;
				if (fade <= 0f) continue;
				float ke = Math.min(1f, k);
				ke = ke * ke * (3 - 2 * ke);
				// arc out sideways and up from the pack, then in to the slot
				double side = (pl[0] == 0 ? (i % 2 == 0 ? 1 : -1) : Math.signum(pl[0])) * 0.55 * Math.sin(Math.PI * ke);
				double lx = pack[0] + (pl[0] - pack[0]) * ke + side;
				double ly = pack[1] + (pl[1] - pack[1]) * ke + 0.35 * Math.sin(Math.PI * ke);
				double lz = pack[2] + (pl[2] - pack[2]) * ke + 0.25 * Math.sin(Math.PI * ke);
				Vec3d c = toWorld.apply(new double[]{lx, ly, lz});
				Vec3d right = new Vec3d(cos, 0, -sin).multiply(pl[3] / 2), up = new Vec3d(0, pl[4] / 2, 0);
				// plates tumble on the way and settle flat when they lock
				double spin = (1 - ke) * 3.0;
				right = right.rotateY((float) spin);
				int a = (int) (230 * fade);
				if (pass == 0) {
					plate(vc, m, c, right, up, 44, 48, 58, a);
				} else {
					Vec3d r2 = right.multiply(1.08), u2 = up.multiply(1.12);
					edge(vc, m, c.add(u2), r2, 0.02, 90, 225, 255, a);
					edge(vc, m, c.subtract(u2), r2, 0.02, 90, 225, 255, a);
					if (k < 1f) {
						// a streak of light behind the flying plate
						Vec3d prev = toWorld.apply(new double[]{pack[0] + (pl[0] - pack[0]) * ke * 0.7, pack[1] + (pl[1] - pack[1]) * ke * 0.7 + 0.2, pack[2]});
						Vec3d d = c.subtract(prev);
						edge(vc, m, prev.add(d.multiply(0.5)), d.multiply(0.5), 0.015, 150, 240, 255, a / 2);
					} else if (!off) {
						// lock-on flash
						int fa = (int) (200 * fade);
						plate(vc, m, c, right.multiply(1.3), up.multiply(1.3), 120, 230, 255, fa / 2);
					}
				}
			}
		}
	}

	private static void plate(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d r, Vec3d u, int cr, int cg, int cb, int a) {
		Vec3d p1 = c.subtract(r).subtract(u), p2 = c.add(r).subtract(u), p3 = c.add(r).add(u), p4 = c.subtract(r).add(u);
		quad(vc, m, (float) p1.x, (float) p1.y, (float) p1.z, (float) p2.x, (float) p2.y, (float) p2.z,
				(float) p3.x, (float) p3.y, (float) p3.z, (float) p4.x, (float) p4.y, (float) p4.z, cr, cg, cb, a);
	}

	/** A thin glowing bar centred on c, spanning +-half. */
	private static void edge(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d half, double thick, int cr, int cg, int cb, int a) {
		Vec3d up = new Vec3d(0, thick, 0);
		plate(vc, m, c, half, up, cr, cg, cb, a);
	}

	/** A flat glowing band plus a thin outer wall, so it reads as a solid ring from any angle. */
	private static void ring(VertexConsumer vc, Matrix4f m, double cx, double cy, double cz, float radius, int alpha) {
		if (alpha <= 0) return;
		float inner = radius - 0.12f;
		float halfH = 0.05f;
		for (int i = 0; i < SEGMENTS; i++) {
			double a1 = i * Math.PI * 2 / SEGMENTS;
			double a2 = (i + 1) * Math.PI * 2 / SEGMENTS;
			float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
			float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
			float x = (float) cx, y = (float) cy, z = (float) cz;
			// flat band (both sides)
			quad(vc, m, x + c1 * inner, y, z + s1 * inner, x + c1 * radius, y, z + s1 * radius,
					x + c2 * radius, y, z + s2 * radius, x + c2 * inner, y, z + s2 * inner, pink ? 255 : 235, pink ? 200 : 250, pink ? 240 : 255, alpha);
			// outer wall (both sides)
			quad(vc, m, x + c1 * radius, y - halfH, z + s1 * radius, x + c1 * radius, y + halfH, z + s1 * radius,
					x + c2 * radius, y + halfH, z + s2 * radius, x + c2 * radius, y - halfH, z + s2 * radius, pink ? 255 : 170, pink ? 90 : 230, pink ? 210 : 255, alpha);
			// soft outer glow
			float glow = radius + 0.12f;
			quad(vc, m, x + c1 * radius, y, z + s1 * radius, x + c1 * glow, y, z + s1 * glow,
					x + c2 * glow, y, z + s2 * glow, x + c2 * radius, y, z + s2 * radius, pink ? 255 : 120, pink ? 79 : 255, pink ? 200 : 170, alpha / 3);
		}
	}

	private static void quad(VertexConsumer vc, Matrix4f m,
			float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4, int r, int g, int b, int a) {
		vc.vertex(m, x1, y1, z1).color(r, g, b, a);
		vc.vertex(m, x2, y2, z2).color(r, g, b, a);
		vc.vertex(m, x3, y3, z3).color(r, g, b, a);
		vc.vertex(m, x4, y4, z4).color(r, g, b, a);
		vc.vertex(m, x4, y4, z4).color(r, g, b, a);
		vc.vertex(m, x3, y3, z3).color(r, g, b, a);
		vc.vertex(m, x2, y2, z2).color(r, g, b, a);
		vc.vertex(m, x1, y1, z1).color(r, g, b, a);
	}
}
