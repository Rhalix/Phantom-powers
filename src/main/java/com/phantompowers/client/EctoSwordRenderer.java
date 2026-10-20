package com.phantompowers.client;

import com.phantompowers.EctoSwordItem;
import com.phantompowers.PhantomMod;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/**
 * Draws the Hunter Ecto Sword: a chunky metal hilt (pommel, ridged grip, emitter with a glowing gem and side vanes)
 * and, when it's lit, a blade of light that shoots out of the emitter: a white-hot core inside layers of coloured
 * glow that shimmer while it hums.
 */
public final class EctoSwordRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
	private static final Identifier HILT = PhantomMod.id("textures/item/ecto_sword_hilt.png");
	private static final float HILT_LEN = 0.34f, BLADE_LEN = 1.25f;

	@Override
	public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack ms, VertexConsumerProvider consumers, int light, int overlay) {
		MinecraftClient mc = MinecraftClient.getInstance();
		long time = mc.world != null ? mc.world.getTime() : 0;
		float partial = mc.getRenderTickCounter().getTickDelta(true);
		float ext = EctoSwordItem.extension(stack, time, partial);
		boolean gui = mode == ModelTransformationMode.GUI || mode == ModelTransformationMode.FIXED || mode == ModelTransformationMode.GROUND;
		ms.push();
		// lie along the item's diagonal like a normal sword sprite (handle bottom-left, tip top-right)
		ms.translate(0.16, 0.16, 0.5);
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45f));
		if (gui) ms.scale(0.62f, 0.62f, 0.62f);
		hilt(ms, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(HILT)), light, overlay);
		if (ext > 0.01f) blade(ms, consumers, EctoSwordItem.bladeColor(stack), ext, time + partial);
		ms.pop();
	}

	private void hilt(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
		// pommel, grip, collar, emitter, and two little vanes
		box(ms, vc, -0.045f, 0.0f, -0.045f, 0.045f, 0.04f, 0.045f, 0.0f, 0.5f, light, overlay);
		box(ms, vc, -0.035f, 0.04f, -0.035f, 0.035f, 0.22f, 0.035f, 0.5f, 0.0f, light, overlay);
		box(ms, vc, -0.048f, 0.22f, -0.048f, 0.048f, 0.25f, 0.048f, 0.0f, 0.5f, light, overlay);
		box(ms, vc, -0.04f, 0.25f, -0.04f, 0.04f, HILT_LEN, 0.04f, 0.5f, 0.5f, light, overlay);
		box(ms, vc, 0.04f, 0.26f, -0.012f, 0.075f, 0.32f, 0.012f, 0.0f, 0.5f, light, overlay);
		box(ms, vc, -0.075f, 0.26f, -0.012f, -0.04f, 0.32f, 0.012f, 0.0f, 0.5f, light, overlay);
		// glowing gem on the emitter
		box(ms, vc, -0.015f, 0.27f, -0.046f, 0.015f, 0.3f, 0.046f, 0.5f, 0.75f, 0xF000F0, overlay);
	}

	private void blade(MatrixStack ms, VertexConsumerProvider consumers, int color, float ext, float t) {
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
		Matrix4f m = ms.peek().getPositionMatrix();
		int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
		float len = BLADE_LEN * ext;
		float y0 = HILT_LEN, y1 = HILT_LEN + len;
		float hum = 0.9f + 0.1f * MathHelper.sin(t * 1.7f) + 0.05f * MathHelper.sin(t * 5.3f);
		// white-hot core, then glow layers getting wider and fainter
		cube(vc, m, 0.016f, y0, y1, 245, 255, 250, 255);
		cube(vc, m, 0.03f * hum, y0, y1 + 0.01f, Math.min(255, r + 80), Math.min(255, g + 80), Math.min(255, b + 80), 200);
		cube(vc, m, 0.05f * hum, y0, y1 + 0.025f, r, g, b, 110);
		cube(vc, m, 0.08f * hum, y0 + 0.01f, y1 + 0.045f, r, g, b, 45);
	}

	/** A square-section glowing bar from y0 to y1 with a rounded-off (tapered) tip. */
	private static void cube(VertexConsumer vc, Matrix4f m, float h, float y0, float y1, int r, int g, int b, int a) {
		float tip = y1 + h * 1.2f;
		float[][] side = {{-h, -h, h, -h}, {h, -h, h, h}, {h, h, -h, h}, {-h, h, -h, -h}};
		for (float[] s : side) {
			quad(vc, m, s[0], y0, s[1], s[2], y0, s[3], s[2], y1, s[3], s[0], y1, s[1], r, g, b, a);
			// taper to a point
			quad(vc, m, s[0], y1, s[1], s[2], y1, s[3], 0, tip, 0, 0, tip, 0, r, g, b, a);
		}
		quad(vc, m, -h, y0, -h, -h, y0, h, h, y0, h, h, y0, -h, r, g, b, a);
	}

	private static void quad(VertexConsumer vc, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2,
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

	/** A textured box; (u0, v0) picks the quarter of the hilt texture to use. */
	private static void box(MatrixStack ms, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
			float u0, float v0, int light, int overlay) {
		MatrixStack.Entry e = ms.peek();
		Matrix4f m = e.getPositionMatrix();
		float u1 = u0 + 0.5f, v1 = v0 + (v0 >= 0.75f ? 0.25f : 0.5f);
		// -Z, +Z, -X, +X, -Y, +Y
		face(vc, e, m, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1, u0, v0, u1, v1, light, overlay);
		face(vc, e, m, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1, u0, v0, u1, v1, light, overlay);
		face(vc, e, m, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0, u0, v0, u1, v1, light, overlay);
		face(vc, e, m, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0, u0, v0, u1, v1, light, overlay);
		face(vc, e, m, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, u0, v0, u1, v1, light, overlay);
		face(vc, e, m, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, u0, v0, u1, v1, light, overlay);
	}

	private static void face(VertexConsumer vc, MatrixStack.Entry e, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
			float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz,
			float u0, float v0, float u1, float v1, int light, int overlay) {
		vc.vertex(m, ax, ay, az).color(255, 255, 255, 255).texture(u0, v1).overlay(overlay).light(light).normal(e, nx, ny, nz);
		vc.vertex(m, bx, by, bz).color(255, 255, 255, 255).texture(u0, v0).overlay(overlay).light(light).normal(e, nx, ny, nz);
		vc.vertex(m, cx, cy, cz).color(255, 255, 255, 255).texture(u1, v0).overlay(overlay).light(light).normal(e, nx, ny, nz);
		vc.vertex(m, dx, dy, dz).color(255, 255, 255, 255).texture(u1, v1).overlay(overlay).light(light).normal(e, nx, ny, nz);
	}
}
