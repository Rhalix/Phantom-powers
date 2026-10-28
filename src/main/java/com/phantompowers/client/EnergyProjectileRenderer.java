package com.phantompowers.client;

import com.phantompowers.entity.KissEntity;
import com.phantompowers.entity.PinkOrbEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/** Glowing energy projectiles drawn as light: Roxy's heart-shaped kisses and the Ecto Launcher's pink orbs. */
public final class EnergyProjectileRenderer {
	private EnergyProjectileRenderer() {}

	private static final String[] HEART = {
			".##.##.",
			"#######",
			"#######",
			".#####.",
			"..###..",
			"...#..."};

	/** A pixel heart that always faces you, with a soft glow around it and a pulse. */
	public static class Kiss extends EntityRenderer<KissEntity> {
		public Kiss(EntityRendererFactory.Context ctx) {
			super(ctx);
		}

		/** A blown kiss: a glossy lipstick kiss print (red lips with a shine) wrapped in a pink glow, with little hearts circling it. */
		private static final String[] LIPS = {
				"..###...###..",
				".#####.#####.",
				"#############",
				"#ddddddddddd#",
				".##ss#######.",
				"..#ss######..",
				"....#####...."};
		private static final String[] MINI = {"#.#", "###", ".#."};

		@Override
		public void render(KissEntity e, float yaw, float tickDelta, MatrixStack ms, VertexConsumerProvider consumers, int light) {
			float t = e.age + tickDelta;
			boolean big = e.explosive();
			ms.push();
			ms.translate(0, 0.25, 0);
			ms.multiply(dispatcher.getRotation());
			// a playful wobble, and a little "pucker" squash as it flies
			ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(t * 0.5f) * 12f));
			float pucker = 1f + 0.12f * MathHelper.sin(t * 0.9f);
			float s = 0.06f * (big ? 1.35f : 1f);
			ms.scale(s * pucker, s / pucker, s);
			Matrix4f m = ms.peek().getPositionMatrix();
			int w = LIPS[0].length();
			// two passes, one buffer at a time: the pink glow first, then the solid lipstick on top
			VertexConsumer glow = consumers.getBuffer(RenderLayer.getLightning());
			for (int y = 0; y < LIPS.length; y++) {
				for (int x = 0; x < w; x++) {
					if (LIPS[y].charAt(x) == '.') continue;
					float px = x - w / 2f, py = LIPS.length / 2f - y - 1;
					quad(glow, m, px - 0.8f, py - 0.8f, px + 1.8f, py + 1.8f, 255, 70, 170, 45);
				}
			}
			VertexConsumer solid = consumers.getBuffer(RenderLayer.getDebugQuads());
			for (int y = 0; y < LIPS.length; y++) {
				for (int x = 0; x < w; x++) {
					char c = LIPS[y].charAt(x);
					if (c == '.') continue;
					float px = x - w / 2f, py = LIPS.length / 2f - y - 1;
					int r = 225, g = 22, b = 70;
					if (c == 'd') { r = 120; g = 0; b = 28; }
					else if (c == 's') { r = 255; g = 205; b = 220; }
					else if (y <= 1) { r = 245; g = 50; b = 95; }
					quad(solid, m, px, py, px + 1, py + 1, r, g, b, 255);
				}
			}
			glow = consumers.getBuffer(RenderLayer.getLightning());
			// two tiny hearts orbiting the kiss
			for (int k = 0; k < 2; k++) {
				float ang = t * 0.25f + k * MathHelper.PI;
				float ox = MathHelper.cos(ang) * 9f, oy = MathHelper.sin(ang) * 5f;
				for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) {
					if (MINI[y].charAt(x) != '#') continue;
					float px = ox + x * 0.8f - 1.2f, py = oy - y * 0.8f + 1.2f;
					quad(glow, m, px, py, px + 0.8f, py + 0.8f, 255, 120, 200, 230);
				}
			}
			ms.pop();
			super.render(e, yaw, tickDelta, ms, consumers, light);
		}

		@Override
		public Identifier getTexture(KissEntity entity) {
			return com.phantompowers.PhantomMod.id("textures/misc/white.png");
		}
	}

	/** A ball of pink energy: bright core, layered glow and two spinning rings. */
	public static class Orb extends EntityRenderer<PinkOrbEntity> {
		public Orb(EntityRendererFactory.Context ctx) {
			super(ctx);
		}

		@Override
		public void render(PinkOrbEntity e, float yaw, float tickDelta, MatrixStack ms, VertexConsumerProvider consumers, int light) {
			ms.push();
			ms.translate(0, 0.25, 0);
			float t = e.age + tickDelta;
			VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
			ms.push();
			ms.multiply(dispatcher.getRotation());
			Matrix4f m = ms.peek().getPositionMatrix();
			float pulse = 1f + 0.12f * MathHelper.sin(t * 1.1f);
			disc(vc, m, 0.75f * pulse, 255, 60, 170, 40);
			disc(vc, m, 0.5f * pulse, 255, 90, 200, 90);
			disc(vc, m, 0.32f * pulse, 255, 160, 230, 180);
			disc(vc, m, 0.18f, 255, 245, 250, 255);
			ms.pop();
			for (int k = 0; k < 2; k++) {
				ms.push();
				ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * (k == 0 ? 14 : -11)));
				ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70 + k * 40));
				ring(vc, ms.peek().getPositionMatrix(), 0.55f, 0.04f, 255, 120, 220, 200);
				ms.pop();
			}
			ms.pop();
			super.render(e, yaw, tickDelta, ms, consumers, light);
		}

		@Override
		public Identifier getTexture(PinkOrbEntity entity) {
			return com.phantompowers.PhantomMod.id("textures/misc/white.png");
		}
	}

	private static void disc(VertexConsumer vc, Matrix4f m, float r, int cr, int cg, int cb, int a) {
		int n = 16;
		for (int i = 0; i < n; i++) {
			double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
			float x0 = (float) Math.cos(a0) * r, y0 = (float) Math.sin(a0) * r, x1 = (float) Math.cos(a1) * r, y1 = (float) Math.sin(a1) * r;
			vc.vertex(m, 0, 0, 0).color(cr, cg, cb, a);
			vc.vertex(m, x0, y0, 0).color(cr, cg, cb, 0);
			vc.vertex(m, x1, y1, 0).color(cr, cg, cb, 0);
			vc.vertex(m, 0, 0, 0).color(cr, cg, cb, a);
			vc.vertex(m, 0, 0, 0).color(cr, cg, cb, a);
			vc.vertex(m, x1, y1, 0).color(cr, cg, cb, 0);
			vc.vertex(m, x0, y0, 0).color(cr, cg, cb, 0);
			vc.vertex(m, 0, 0, 0).color(cr, cg, cb, a);
		}
	}

	private static void ring(VertexConsumer vc, Matrix4f m, float r, float w, int cr, int cg, int cb, int a) {
		int n = 24;
		for (int i = 0; i < n; i++) {
			double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
			float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
			float ri = r - w, ro = r + w;
			vc.vertex(m, c0 * ri, s0 * ri, 0).color(cr, cg, cb, a);
			vc.vertex(m, c0 * ro, s0 * ro, 0).color(cr, cg, cb, a);
			vc.vertex(m, c1 * ro, s1 * ro, 0).color(cr, cg, cb, a);
			vc.vertex(m, c1 * ri, s1 * ri, 0).color(cr, cg, cb, a);
			vc.vertex(m, c1 * ri, s1 * ri, 0).color(cr, cg, cb, a);
			vc.vertex(m, c1 * ro, s1 * ro, 0).color(cr, cg, cb, a);
			vc.vertex(m, c0 * ro, s0 * ro, 0).color(cr, cg, cb, a);
			vc.vertex(m, c0 * ri, s0 * ri, 0).color(cr, cg, cb, a);
		}
	}

	private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, int r, int g, int b, int a) {
		vc.vertex(m, x0, y0, 0).color(r, g, b, a);
		vc.vertex(m, x1, y0, 0).color(r, g, b, a);
		vc.vertex(m, x1, y1, 0).color(r, g, b, a);
		vc.vertex(m, x0, y1, 0).color(r, g, b, a);
		vc.vertex(m, x0, y1, 0).color(r, g, b, a);
		vc.vertex(m, x1, y1, 0).color(r, g, b, a);
		vc.vertex(m, x1, y0, 0).color(r, g, b, a);
		vc.vertex(m, x0, y0, 0).color(r, g, b, a);
	}
}
