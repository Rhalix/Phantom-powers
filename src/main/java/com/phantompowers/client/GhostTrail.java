package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Flying ghosts leave a glowing spectral streak behind them: a ribbon that's white-hot near you and fades
 * through green to teal, with a twisting thinner strand wound around it and a few sparkles.
 */
public final class GhostTrail {
	private static final int LENGTH = 26;
	private static final Map<UUID, ArrayDeque<Vec3d>> TRAILS = new HashMap<>();
	private static int clock;

	private GhostTrail() {}

	public static void clear() {
		TRAILS.clear();
	}

	public static void tick(MinecraftClient mc) {
		clock++;
		if (mc.world == null) {
			TRAILS.clear();
			return;
		}
		List<UUID> seen = new ArrayList<>();
		for (PlayerEntity p : mc.world.getPlayers()) {
			UUID id = p.getUuid();
			boolean flying = GhostFlags.CLIENT_FLYING.contains(id) && GhostFlags.CLIENT_GHOSTS.contains(id) && !p.isInvisible();
			ArrayDeque<Vec3d> trail = TRAILS.get(id);
			Vec3d move = new Vec3d(p.getX() - p.prevX, p.getY() - p.prevY, p.getZ() - p.prevZ);
			if (flying && move.lengthSquared() > 0.0016) {
				if (trail == null) TRAILS.put(id, trail = new ArrayDeque<>());
				Vec3d back = move.normalize().multiply(-1);
				trail.addFirst(p.getPos().add(0, 0.55, 0).add(back.multiply(0.45)));
				while (trail.size() > LENGTH) trail.removeLast();
				// a few sparkles shed off the streak
				if (clock % 3 == 0 && move.length() > 0.15) {
					Vec3d s = trail.peekLast();
					mc.world.addParticle(ParticleTypes.END_ROD, s.x, s.y, s.z, back.x * 0.01, 0.01, back.z * 0.01);
				}
			} else if (trail != null) {
				// shrink away when you stop
				trail.pollLast();
				trail.pollLast();
			}
			seen.add(id);
		}
		TRAILS.entrySet().removeIf(en -> en.getValue().isEmpty() || !seen.contains(en.getKey()));
	}

	public static void render(WorldRenderContext ctx) {
		if (TRAILS.isEmpty()) return;
		VertexConsumerProvider consumers = ctx.consumers();
		MatrixStack ms = ctx.matrixStack();
		if (consumers == null || ms == null) return;
		float partial = ctx.tickCounter().getTickDelta(true);
		Vec3d cam = ctx.camera().getPos();
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
		ms.push();
		ms.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f m = ms.peek().getPositionMatrix();
		MinecraftClient mc = MinecraftClient.getInstance();
		for (Map.Entry<UUID, ArrayDeque<Vec3d>> en : TRAILS.entrySet()) {
			List<Vec3d> pts = new ArrayList<>(en.getValue());
			if (pts.size() < 3) continue;
			// stick the newest point to the player so the streak never lags behind them
			PlayerEntity p = mc.world == null ? null : mc.world.getPlayerByUuid(en.getKey());
			if (p != null && GhostFlags.CLIENT_FLYING.contains(en.getKey())) {
				Vec3d now = p.getLerpedPos(partial).add(0, 0.55, 0);
				Vec3d mv = new Vec3d(p.getX() - p.prevX, p.getY() - p.prevY, p.getZ() - p.prevZ);
				if (mv.lengthSquared() > 1.0E-4) now = now.add(mv.normalize().multiply(-0.45));
				pts.set(0, now);
			}
			boolean self = p == mc.player && mc.options.getPerspective().isFirstPerson();
			float fade = self ? 0.35f : 1f;
			int v = p instanceof net.minecraft.client.network.AbstractClientPlayerEntity cp ? GhostSuitFeature.variantOf(cp) : 0;
			int[] col = COLORS[Math.max(0, Math.min(COLORS.length - 1, v))];
			ribbon(vc, m, pts, cam, 0.32f, fade, 0f, 0f, col);
			ribbon(vc, m, pts, cam, 0.07f, fade, 0.22f, clock + partial, col);
		}
		ms.pop();
	}

	/** A camera-facing strip along the points, tapering to nothing; optionally wound around the path (twist). */
	/** Streak colours per suit look: {head r,g,b, tail r,g,b}. */
	private static final int[][] COLORS = {
			{235, 255, 235, 40, 255, 200},   // classic: white-hot to green-teal
			{235, 235, 245, 110, 90, 150},   // shadow: white to dusky violet
			{210, 255, 225, 40, 255, 130},   // ecto glow: bright green
			{255, 225, 225, 225, 40, 60},    // crimson
			{240, 250, 255, 70, 165, 255},   // frostbite: icy blue
			{255, 240, 200, 150, 80, 230}};  // royal: gold to purple

	private static void ribbon(VertexConsumer vc, Matrix4f m, List<Vec3d> pts, Vec3d cam, float width, float fade, float twist, float time, int[] col) {
		int n = pts.size();
		Vec3d[] left = new Vec3d[n], right = new Vec3d[n];
		int[] r = new int[n], g = new int[n], b = new int[n], a = new int[n];
		for (int i = 0; i < n; i++) {
			Vec3d p = pts.get(i);
			Vec3d dir = (i < n - 1 ? pts.get(i + 1) : p).subtract(i > 0 ? pts.get(i - 1) : p);
			if (dir.lengthSquared() < 1.0E-6) dir = new Vec3d(0, 1, 0);
			Vec3d toCam = cam.subtract(p);
			Vec3d side = dir.crossProduct(toCam);
			side = side.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : side.normalize();
			float f = (float) i / (n - 1);
			if (twist > 0) {
				Vec3d up = side.crossProduct(dir.normalize());
				double ang = time * 0.5 - i * 0.6;
				p = p.add(side.multiply(Math.cos(ang) * twist)).add(up.multiply(Math.sin(ang) * twist));
			}
			double w = width * (1 - f) * (0.7 + 0.3 * (1 - f));
			left[i] = p.add(side.multiply(w));
			right[i] = p.subtract(side.multiply(w));
			// white-hot -> green -> teal
			r[i] = (int) MathHelper.lerp(f, col[0], col[3]);
			g[i] = (int) MathHelper.lerp(f, col[1], col[4]);
			b[i] = (int) MathHelper.lerp(f, col[2], col[5]);
			a[i] = (int) (255 * fade * (1 - f) * (twist > 0 ? 0.9f : 0.6f));
		}
		for (int i = 0; i < n - 1; i++) {
			put(vc, m, left[i], r[i], g[i], b[i], a[i]);
			put(vc, m, right[i], r[i], g[i], b[i], a[i]);
			put(vc, m, right[i + 1], r[i + 1], g[i + 1], b[i + 1], a[i + 1]);
			put(vc, m, left[i + 1], r[i + 1], g[i + 1], b[i + 1], a[i + 1]);
			put(vc, m, left[i + 1], r[i + 1], g[i + 1], b[i + 1], a[i + 1]);
			put(vc, m, right[i + 1], r[i + 1], g[i + 1], b[i + 1], a[i + 1]);
			put(vc, m, right[i], r[i], g[i], b[i], a[i]);
			put(vc, m, left[i], r[i], g[i], b[i], a[i]);
		}
	}

	private static void put(VertexConsumer vc, Matrix4f m, Vec3d p, int r, int g, int b, int a) {
		vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a);
	}
}
