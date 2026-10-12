package com.phantompowers.client;

import com.phantompowers.entity.GhostNetProjectile;
import com.phantompowers.entity.GhostNetTrapEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;

/**
 * Draws the Ghost Net as a glowing cube of energy mesh: a faint shimmering skin, a grid of bright strands with
 * pulses running along them, thick glowing edges, corner nodes and electric arcs crackling across the faces.
 */
public final class GhostNetRenderer {
	private GhostNetRenderer() {}

	/** The sprung net around a trapped ghost. */
	public static class Trap extends EntityRenderer<GhostNetTrapEntity> {
		public Trap(EntityRendererFactory.Context ctx) {
			super(ctx);
		}

		@Override
		public boolean shouldRender(GhostNetTrapEntity entity, Frustum frustum, double x, double y, double z) {
			return entity.squaredDistanceTo(x, y, z) < 96 * 96;
		}

		@Override
		public void render(GhostNetTrapEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
			matrices.push();
			Entity target = e.getWorld().getEntityById(e.targetId());
			if (target != null) {
				Vec3d off = target.getLerpedPos(tickDelta).subtract(e.getLerpedPos(tickDelta));
				matrices.translate(off.x, off.y, off.z);
			}
			float time = e.age + tickDelta;
			float pop = Math.min(1f, time / 6f);
			pop = 1f - (1f - pop) * (1f - pop) * (1f - pop);
			float overshoot = pop < 1f ? 1f : 1f + 0.06f * MathHelper.sin(Math.min(time - 6f, 12f) * 0.9f) * Math.max(0f, 1f - (time - 6f) / 12f);
			float struggle = e.struggleAmount();
			// Thrashing makes the cage jolt and bulge
			Random r = Random.create((long) (time * 0.5f) * 977L + e.getId());
			float shake = struggle * 0.06f;
			matrices.translate((r.nextFloat() - 0.5f) * shake, 0, (r.nextFloat() - 0.5f) * shake);
			float w = e.boxWidth() * pop * overshoot * (1f + struggle * 0.05f * MathHelper.sin(time * 1.3f));
			float h = e.boxHeight() * pop * overshoot;
			matrices.translate(0, (e.boxHeight() - h) * 0.5f - 0.2f, 0);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 0.6f));
			cube(matrices, consumers, w, h, time, struggle, true, e.getId());
			matrices.pop();
			super.render(e, yaw, tickDelta, matrices, consumers, light);
		}

		@Override
		public Identifier getTexture(GhostNetTrapEntity entity) {
			return Identifier.ofVanilla("textures/misc/white.png");
		}
	}

	/** A thrown net: a small cube tumbling through the air. */
	public static class Thrown extends EntityRenderer<GhostNetProjectile> {
		public Thrown(EntityRendererFactory.Context ctx) {
			super(ctx);
		}

		@Override
		public void render(GhostNetProjectile e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
			matrices.push();
			float time = e.age + tickDelta;
			float s = 0.36f;
			matrices.translate(0, 0.12f, 0);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 25f));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(time * 17f));
			matrices.translate(0, -s / 2, 0);
			cube(matrices, consumers, s, s, time, 0f, false, e.getId());
			matrices.pop();
			super.render(e, yaw, tickDelta, matrices, consumers, light);
		}

		@Override
		public Identifier getTexture(GhostNetProjectile entity) {
			return Identifier.ofVanilla("textures/misc/white.png");
		}
	}

	/** The glowing mesh cube, from y=0 to y=h, centred on x/z. */
	static void cube(MatrixStack matrices, VertexConsumerProvider consumers, float w, float h, float time, float struggle,
			boolean big, int seed) {
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
		Matrix4f m = matrices.peek().getPositionMatrix();
		float hw = w / 2;
		// colour drifts from cyan-green toward angry orange as the victim fights it
		int cr = (int) MathHelper.lerp(struggle, 90, 255), cg = (int) MathHelper.lerp(struggle, 255, 170), cb = (int) MathHelper.lerp(struggle, 220, 90);
		float flicker = 0.85f + 0.15f * MathHelper.sin(time * 2.7f);

		// faint skin on all six faces
		int skin = (int) ((big ? 26 : 60) * flicker);
		face(vc, m, -hw, 0, -hw, hw, 0, -hw, hw, h, -hw, -hw, h, -hw, cr, cg, cb, skin);
		face(vc, m, hw, 0, hw, -hw, 0, hw, -hw, h, hw, hw, h, hw, cr, cg, cb, skin);
		face(vc, m, -hw, 0, hw, -hw, 0, -hw, -hw, h, -hw, -hw, h, hw, cr, cg, cb, skin);
		face(vc, m, hw, 0, -hw, hw, 0, hw, hw, h, hw, hw, h, -hw, cr, cg, cb, skin);
		face(vc, m, -hw, h, -hw, hw, h, -hw, hw, h, hw, -hw, h, hw, cr, cg, cb, skin);
		face(vc, m, -hw, 0, hw, hw, 0, hw, hw, 0, -hw, -hw, 0, -hw, cr, cg, cb, skin);

		// grid strands on every face, with a pulse travelling up them
		float t = big ? 0.022f : 0.012f;
		int nx = Math.max(2, Math.round(w / 0.32f)), ny = Math.max(2, Math.round(h / 0.32f));
		for (int i = 1; i < nx; i++) {
			float x = -hw + w * i / nx;
			for (int side = 0; side < 4; side++) {
				for (int j = 0; j < ny; j++) {
					float y0 = h * j / ny, y1 = h * (j + 1) / ny;
					int a = strandAlpha(time, y0 / Math.max(0.01f, h), i, flicker);
					sideLine(vc, m, side, hw, x, y0, x, y1, t, cr, cg, cb, a);
				}
			}
			// top and bottom
			line(vc, m, x, h, -hw, x, h, hw, t, cr, cg, cb, (int) (150 * flicker));
			line(vc, m, x, 0, -hw, x, 0, hw, t, cr, cg, cb, (int) (110 * flicker));
			line(vc, m, -hw, h, x, hw, h, x, t, cr, cg, cb, (int) (150 * flicker));
			line(vc, m, -hw, 0, x, hw, 0, x, t, cr, cg, cb, (int) (110 * flicker));
		}
		for (int j = 1; j < ny; j++) {
			float y = h * j / ny;
			int a = strandAlpha(time, (float) j / ny, j * 3, flicker);
			for (int side = 0; side < 4; side++) sideLine(vc, m, side, hw, -hw, y, hw, y, t, cr, cg, cb, a);
		}

		// thick bright edges
		float et = big ? 0.045f : 0.025f;
		int ea = (int) (255 * flicker);
		float[][] corners = {{-hw, -hw}, {hw, -hw}, {hw, hw}, {-hw, hw}};
		for (int k = 0; k < 4; k++) {
			float[] a = corners[k], b = corners[(k + 1) % 4];
			line(vc, m, a[0], 0, a[1], b[0], 0, b[1], et, 220, 255, 250, ea);
			line(vc, m, a[0], h, a[1], b[0], h, b[1], et, 220, 255, 250, ea);
			line(vc, m, a[0], 0, a[1], a[0], h, a[1], et, 220, 255, 250, ea);
			// glowing corner nodes
			node(vc, m, a[0], 0, a[1], big ? 0.09f : 0.05f, ea);
			node(vc, m, a[0], h, a[1], big ? 0.09f : 0.05f, ea);
		}

		// electric arcs crackling across the faces
		if (big) {
			Random r = Random.create((long) (time / 2f) * 7919L + seed);
			int arcs = 2 + (int) (struggle * 4);
			for (int k = 0; k < arcs; k++) {
				int side = r.nextInt(4);
				float x = -hw + r.nextFloat() * w, y = r.nextFloat() * h;
				for (int s = 0; s < 5; s++) {
					float x2 = MathHelper.clamp(x + (r.nextFloat() - 0.5f) * 0.5f, -hw, hw);
					float y2 = MathHelper.clamp(y + (r.nextFloat() - 0.5f) * 0.5f, 0, h);
					sideLine(vc, m, side, hw + 0.01f, x, y, x2, y2, 0.018f, 235, 255, 255, 230);
					x = x2;
					y = y2;
				}
			}
		}
	}

	private static int strandAlpha(float time, float frac, int i, float flicker) {
		float pulse = (time * 0.06f + i * 0.17f) % 1f;
		float d = Math.abs(frac - pulse);
		float boost = Math.max(0f, 1f - d * 6f);
		return (int) Math.min(255, (95 + 160 * boost) * flicker);
	}

	/** A line drawn on one of the four side faces (side 0..3), given in that face's own x/y. */
	private static void sideLine(VertexConsumer vc, Matrix4f m, int side, float hw, float x0, float y0, float x1, float y1,
			float t, int r, int g, int b, int a) {
		switch (side) {
			case 0 -> line(vc, m, x0, y0, -hw, x1, y1, -hw, t, r, g, b, a);
			case 1 -> line(vc, m, x0, y0, hw, x1, y1, hw, t, r, g, b, a);
			case 2 -> line(vc, m, -hw, y0, x0, -hw, y1, x1, t, r, g, b, a);
			default -> line(vc, m, hw, y0, x0, hw, y1, x1, t, r, g, b, a);
		}
	}

	/** A glowing strand: two crossed thin quads so it reads from every angle. */
	private static void line(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1,
			float t, int r, int g, int b, int a) {
		float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
		// pick two offsets perpendicular-ish to the strand
		float ox1, oy1, oz1, ox2, oy2, oz2;
		if (Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
			ox1 = t; oy1 = 0; oz1 = 0; ox2 = 0; oy2 = 0; oz2 = t;
		} else if (Math.abs(dx) > Math.abs(dz)) {
			ox1 = 0; oy1 = t; oz1 = 0; ox2 = 0; oy2 = 0; oz2 = t;
		} else {
			ox1 = t; oy1 = 0; oz1 = 0; ox2 = 0; oy2 = t; oz2 = 0;
		}
		face(vc, m, x0 - ox1, y0 - oy1, z0 - oz1, x0 + ox1, y0 + oy1, z0 + oz1, x1 + ox1, y1 + oy1, z1 + oz1, x1 - ox1, y1 - oy1, z1 - oz1, r, g, b, a);
		face(vc, m, x0 - ox2, y0 - oy2, z0 - oz2, x0 + ox2, y0 + oy2, z0 + oz2, x1 + ox2, y1 + oy2, z1 + oz2, x1 - ox2, y1 - oy2, z1 - oz2, r, g, b, a);
	}

	private static void node(VertexConsumer vc, Matrix4f m, float x, float y, float z, float s, int a) {
		line(vc, m, x - s, y, z, x + s, y, z, s, 255, 255, 255, a);
		line(vc, m, x, y - s, z, x, y + s, z, s, 255, 255, 255, a);
	}

	private static void face(VertexConsumer vc, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4, int r, int g, int b, int a) {
		if (a <= 0) return;
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
