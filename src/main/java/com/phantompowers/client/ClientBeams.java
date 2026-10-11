package com.phantompowers.client;

import com.phantompowers.network.BeamPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Draws Ecto Blast beams as glowing green rays. */
public final class ClientBeams {
	private static final int LIFE = 7;

	private static final class Beam {
		final Vec3d from;
		final Vec3d to;
		final int color;
		int age;

		Beam(Vec3d from, Vec3d to, int color) {
			this.from = from;
			this.to = to;
			this.color = color;
		}
	}

	private static final List<Beam> BEAMS = new ArrayList<>();

	private ClientBeams() {}

	public static void add(BeamPayload p) {
		Beam b = new Beam(new Vec3d(p.x1(), p.y1(), p.z1()), new Vec3d(p.x2(), p.y2(), p.z2()), p.color());
		DustParticleEffect dust = new DustParticleEffect(new Vector3f(((p.color() >> 16) & 255) / 255f,
				((p.color() >> 8) & 255) / 255f, (p.color() & 255) / 255f), 1.2f);
		BEAMS.add(b);
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null) return;
		// Sparks along the beam and a splash where it hits
		Vec3d d = b.to.subtract(b.from);
		double len = d.length();
		Vec3d dir = len > 0 ? d.multiply(1 / len) : Vec3d.ZERO;
		for (double t = 0; t < len; t += 0.5) {
			Vec3d v = b.from.add(dir.multiply(t));
			mc.world.addParticle(dust, v.x, v.y, v.z, 0, 0, 0);
		}
		for (int i = 0; i < 16; i++) {
			mc.world.addParticle(dust, b.to.x, b.to.y, b.to.z,
					(Math.random() - 0.5) * 0.4, Math.random() * 0.3, (Math.random() - 0.5) * 0.4);
		}
		mc.world.addParticle(ParticleTypes.FLASH, b.to.x, b.to.y, b.to.z, 0, 0, 0);
	}

	public static void tick(MinecraftClient client) {
		BEAMS.removeIf(b -> ++b.age > LIFE);
	}

	public static void clear() {
		BEAMS.clear();
	}

	public static void render(WorldRenderContext ctx) {
		if (BEAMS.isEmpty()) return;
		VertexConsumerProvider consumers = ctx.consumers();
		MatrixStack ms = ctx.matrixStack();
		if (consumers == null || ms == null) return;
		Vec3d cam = ctx.camera().getPos();
		float partial = ctx.tickCounter().getTickDelta(true);
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
		ms.push();
		ms.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f m = ms.peek().getPositionMatrix();
		for (Beam b : BEAMS) {
			float fade = Math.max(0f, 1f - (b.age + partial) / LIFE);
			float grow = Math.min(1f, (b.age + partial + 1) / 2f);
			int cr = (b.color >> 16) & 255, cg = (b.color >> 8) & 255, cbl = b.color & 255;
			box(vc, m, b.from, b.to, 0.05f * grow, 200 + cr / 5, 200 + cg / 5, 200 + cbl / 5, (int) (255 * fade));
			box(vc, m, b.from, b.to, 0.13f * grow, cr, cg, cbl, (int) (170 * fade));
			box(vc, m, b.from, b.to, 0.24f * grow, cr * 3 / 4, cg * 3 / 4, cbl * 3 / 4, (int) (70 * fade));
		}
		ms.pop();
	}

	/** A square tube from a to b, drawn from both sides. */
	private static void box(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, float w, int r, int g, int bl, int alpha) {
		if (alpha <= 0) return;
		Vec3d dir = b.subtract(a).normalize();
		Vec3d u = dir.crossProduct(new Vec3d(0, 1, 0));
		if (u.lengthSquared() < 1.0E-4) u = new Vec3d(1, 0, 0);
		u = u.normalize().multiply(w);
		Vec3d v = dir.crossProduct(u).normalize().multiply(w);
		Vec3d[] ring = {u.add(v), u.negate().add(v), u.negate().subtract(v), u.subtract(v)};
		for (int i = 0; i < 4; i++) {
			Vec3d c1 = ring[i];
			Vec3d c2 = ring[(i + 1) % 4];
			Vec3d p1 = a.add(c1), p2 = a.add(c2), p3 = b.add(c2), p4 = b.add(c1);
			quad(vc, m, p1, p2, p3, p4, r, g, bl, alpha);
			quad(vc, m, p4, p3, p2, p1, r, g, bl, alpha);
		}
	}

	private static void quad(VertexConsumer vc, Matrix4f m, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int r, int g, int b, int a) {
		vc.vertex(m, (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, a);
		vc.vertex(m, (float) p2.x, (float) p2.y, (float) p2.z).color(r, g, b, a);
		vc.vertex(m, (float) p3.x, (float) p3.y, (float) p3.z).color(r, g, b, a);
		vc.vertex(m, (float) p4.x, (float) p4.y, (float) p4.z).color(r, g, b, a);
	}
}
