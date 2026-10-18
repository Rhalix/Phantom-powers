package com.phantompowers.client;

import com.phantompowers.PortalSwirls;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Iterator;

/**
 * A swirling vortex across each ghost portal opening: glowing spiral arms that turn and twist in toward a bright
 * core, rings rippling outward and a soft rim glow. The prototype portal gets a small, flickery version.
 */
public final class PortalSwirlRenderer {
	private static final int ARMS = 6, SEGS = 26, RING_SEGS = 48;

	private PortalSwirlRenderer() {}

	public static void render(WorldRenderContext ctx) {
		if (PortalSwirls.ACTIVE.isEmpty()) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null) {
			PortalSwirls.ACTIVE.clear();
			return;
		}
		VertexConsumerProvider consumers = ctx.consumers();
		MatrixStack ms = ctx.matrixStack();
		if (consumers == null || ms == null) return;
		float partial = ctx.tickCounter().getTickDelta(true);
		long now = mc.world.getTime();
		float time = now + partial;
		Vec3d cam = ctx.camera().getPos();
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
		ms.push();
		ms.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f m = ms.peek().getPositionMatrix();
		Iterator<PortalSwirls.Swirl> it = PortalSwirls.ACTIVE.values().iterator();
		while (it.hasNext()) {
			PortalSwirls.Swirl s = it.next();
			long age = now - s.seen;
			if (age > 120 || age < -20 || s.center.squaredDistanceTo(cam) > 128 * 128) {
				it.remove();
				continue;
			}
			float fade = MathHelper.clamp((now - s.born + partial) / 20f, 0f, 1f) * MathHelper.clamp(1f - (age - 80) / 40f, 0f, 1f);
			if (s.small) fade *= 0.8f + 0.2f * MathHelper.sin(time * 1.7f);   // the prototype flickers
			if (fade <= 0.01f) continue;
			draw(vc, m, s, time, fade);
		}
		ms.pop();
	}

	private static void draw(VertexConsumer vc, Matrix4f m, PortalSwirls.Swirl s, float time, float fade) {
		double rw = s.halfW * 0.96, rh = s.halfH * 0.96;
		float speed = s.small ? 0.12f : 0.045f;
		// soft core glow (a fan of quads from the center)
		for (int k = 0; k < RING_SEGS; k++) {
			double a0 = k * Math.PI * 2 / RING_SEGS, a1 = (k + 1) * Math.PI * 2 / RING_SEGS;
			double cr = 0.42;
			quad(vc, m, s, 0, 0, 0, 0, Math.cos(a0) * rw * cr, Math.sin(a0) * rh * cr, Math.cos(a1) * rw * cr, Math.sin(a1) * rh * cr,
					230, 255, 240, (int) (150 * fade), 60, 255, 140, 0);
		}
		// spiral arms twisting into the center
		for (int arm = 0; arm < ARMS; arm++) {
			double base = arm * Math.PI * 2 / ARMS + time * speed;
			for (int i = 0; i < SEGS; i++) {
				double s0 = i / (double) SEGS, s1 = (i + 1) / (double) SEGS;
				double r0 = 0.08 + 0.92 * s0, r1 = 0.08 + 0.92 * s1;
				double a0 = base + s0 * 3.4, a1 = base + s1 * 3.4;
				double w0 = 0.32 * (1 - s0 * 0.6), w1 = 0.32 * (1 - s1 * 0.6);
				// inner and outer edge of the arm (angular width)
				double ia0 = a0 - w0, oa0 = a0 + w0 * 0.4, ia1 = a1 - w1, oa1 = a1 + w1 * 0.4;
				int al0 = (int) (210 * fade * Math.pow(1 - s0, 0.6)), al1 = (int) (210 * fade * Math.pow(1 - s1, 0.6));
				int g0 = (int) MathHelper.lerp((float) s0, 255, 200), g1 = (int) MathHelper.lerp((float) s1, 255, 200);
				int r0c = (int) MathHelper.lerp((float) s0, 200, 40), r1c = (int) MathHelper.lerp((float) s1, 200, 40);
				quadColor(vc, m, s,
						Math.cos(ia0) * r0 * rw, Math.sin(ia0) * r0 * rh,
						Math.cos(oa0) * r0 * rw, Math.sin(oa0) * r0 * rh,
						Math.cos(oa1) * r1 * rw, Math.sin(oa1) * r1 * rh,
						Math.cos(ia1) * r1 * rw, Math.sin(ia1) * r1 * rh,
						r0c, g0, 130, al0, r1c, g1, 120, al1);
			}
		}
		// rings rippling outward
		for (int ring = 0; ring < 3; ring++) {
			double ph = ((time * (s.small ? 0.03 : 0.012) + ring / 3.0) % 1.0);
			double rr = 0.15 + 0.85 * ph, th = 0.035;
			int al = (int) (120 * fade * Math.sin(ph * Math.PI));
			for (int k = 0; k < RING_SEGS; k++) {
				double a0 = k * Math.PI * 2 / RING_SEGS, a1 = (k + 1) * Math.PI * 2 / RING_SEGS;
				quadColor(vc, m, s,
						Math.cos(a0) * (rr - th) * rw, Math.sin(a0) * (rr - th) * rh,
						Math.cos(a1) * (rr - th) * rw, Math.sin(a1) * (rr - th) * rh,
						Math.cos(a1) * (rr + th) * rw, Math.sin(a1) * (rr + th) * rh,
						Math.cos(a0) * (rr + th) * rw, Math.sin(a0) * (rr + th) * rh,
						150, 255, 190, al, 150, 255, 190, al);
			}
		}
	}

	/** A quad from the center (fan) with separate inner and outer colours. */
	private static void quad(VertexConsumer vc, Matrix4f m, PortalSwirls.Swirl s, double ux0, double uy0, double ux1, double uy1,
			double ox0, double oy0, double ox1, double oy1, int r, int g, int b, int a, int r2, int g2, int b2, int a2) {
		quadColor(vc, m, s, ux0, uy0, ux1, uy1, ox1, oy1, ox0, oy0, r, g, b, a, r2, g2, b2, a2);
	}

	/** One quad in the portal plane (u along the opening, v up), drawn on both faces. Colour 1 on the first two corners, colour 2 on the last two. */
	private static void quadColor(VertexConsumer vc, Matrix4f m, PortalSwirls.Swirl s, double u0, double v0, double u1, double v1,
			double u2, double v2, double u3, double v3, int r, int g, int b, int a, int r2, int g2, int b2, int a2) {
		for (int side = -1; side <= 1; side += 2) {
			double off = side * (s.small ? 0.045 : 0.13);
			Vec3d p0 = pt(s, u0, v0, off), p1 = pt(s, u1, v1, off), p2 = pt(s, u2, v2, off), p3 = pt(s, u3, v3, off);
			vc.vertex(m, (float) p0.x, (float) p0.y, (float) p0.z).color(r, g, b, a);
			vc.vertex(m, (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, a);
			vc.vertex(m, (float) p2.x, (float) p2.y, (float) p2.z).color(r2, g2, b2, a2);
			vc.vertex(m, (float) p3.x, (float) p3.y, (float) p3.z).color(r2, g2, b2, a2);
			vc.vertex(m, (float) p3.x, (float) p3.y, (float) p3.z).color(r2, g2, b2, a2);
			vc.vertex(m, (float) p2.x, (float) p2.y, (float) p2.z).color(r2, g2, b2, a2);
			vc.vertex(m, (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, a);
			vc.vertex(m, (float) p0.x, (float) p0.y, (float) p0.z).color(r, g, b, a);
		}
	}

	private static Vec3d pt(PortalSwirls.Swirl s, double u, double v, double off) {
		return s.center.add(s.along.x * u + s.normal.x * off, v, s.along.z * u + s.normal.z * off);
	}
}
