package com.phantompowers.client;

import com.phantompowers.network.RingPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
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
		int age;

		Rings(int entityId, int duration) {
			this.entityId = entityId;
			this.duration = Math.max(4, duration);
		}
	}

	private static final List<Rings> ACTIVE = new ArrayList<>();
	private static final int SEGMENTS = 40;

	private ClientRings() {}

	public static void add(RingPayload p) {
		ACTIVE.removeIf(r -> r.entityId == p.entityId());
		ACTIVE.add(new Rings(p.entityId(), p.duration()));
	}

	public static void tick() {
		ACTIVE.removeIf(r -> ++r.age > r.duration);
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
			Entity e = mc.world.getEntityById(r.entityId);
			if (e == null) continue;
			Vec3d pos = e.getLerpedPos(partial);
			float h = e.getHeight();
			float t = Math.min(1f, (r.age + partial) / r.duration);
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
			ring(vc, m, pos.x, pos.y + up, pos.z, radius, alpha);
			if (split > 0) ring(vc, m, pos.x, pos.y + down, pos.z, radius, alpha);
		}
		ms.pop();
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
					x + c2 * radius, y, z + s2 * radius, x + c2 * inner, y, z + s2 * inner, 235, 250, 255, alpha);
			// outer wall (both sides)
			quad(vc, m, x + c1 * radius, y - halfH, z + s1 * radius, x + c1 * radius, y + halfH, z + s1 * radius,
					x + c2 * radius, y + halfH, z + s2 * radius, x + c2 * radius, y - halfH, z + s2 * radius, 170, 230, 255, alpha);
			// soft outer glow
			float glow = radius + 0.12f;
			quad(vc, m, x + c1 * radius, y, z + s1 * radius, x + c1 * glow, y, z + s1 * glow,
					x + c2 * glow, y, z + s2 * glow, x + c2 * radius, y, z + s2 * radius, 120, 255, 170, alpha / 3);
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
