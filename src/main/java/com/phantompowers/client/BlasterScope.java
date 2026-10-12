package com.phantompowers.client;

import com.phantompowers.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Ghost Blaster scope: hold sneak with the blaster to zoom in and see a green targeting reticle. */
public final class BlasterScope {
	private static float zoom; // 0..1, eased in and out

	private BlasterScope() {}

	public static boolean wantsScope(PlayerEntity p) {
		MinecraftClient mc = MinecraftClient.getInstance();
		return p != null && p == mc.player && p.isSneaking() && p.getMainHandStack().isOf(ModItems.GHOST_BLASTER)
				&& mc.options.getPerspective().isFirstPerson() && mc.currentScreen == null;
	}

	public static void tick(MinecraftClient mc) {
		boolean on = wantsScope(mc.player);
		zoom = MathHelper.clamp(zoom + (on ? 0.25f : -0.34f), 0f, 1f);
	}

	/** How much the field of view shrinks (1 = normal, 0.25 = 4x zoom). */
	public static float fovMultiplier() {
		return 1f - 0.72f * zoom;
	}

	public static boolean active() {
		return zoom > 0.6f;
	}

	public static void render(DrawContext ctx, RenderTickCounter counter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (zoom <= 0.05f || mc.player == null || mc.options.hudHidden) return;
		int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
		int cx = w / 2, cy = h / 2;
		int r = (int) (Math.min(w, h) * 0.42f);
		int a = (int) (zoom * 255) << 24;
		int black = a;
		// Dark mask around a round lens
		for (int y = 0; y < h; y++) {
			int dy = y - cy;
			if (Math.abs(dy) >= r) {
				ctx.fill(0, y, w, y + 1, black);
				continue;
			}
			int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
			ctx.fill(0, y, cx - half, y + 1, black);
			ctx.fill(cx + half, y, w, y + 1, black);
		}
		// Lens tint and rim
		ctx.fill(cx - r, cy - r, cx + r, cy + r, 0);
		int green = (int) (zoom * 220) << 24 | 0x33FF77;
		int dim = (int) (zoom * 120) << 24 | 0x33FF77;
		for (int i = 0; i < 180; i++) {
			double ang = i * Math.PI * 2 / 180;
			int x = cx + (int) Math.round(Math.cos(ang) * (r - 1)), y = cy + (int) Math.round(Math.sin(ang) * (r - 1));
			ctx.fill(x, y, x + 1, y + 1, green);
		}
		// Reticle: crosshair lines with a gap, range ticks and a center dot
		ctx.fill(cx - r + 6, cy, cx - 6, cy + 1, dim);
		ctx.fill(cx + 6, cy, cx + r - 6, cy + 1, dim);
		ctx.fill(cx, cy - r + 6, cx + 1, cy - 6, dim);
		ctx.fill(cx, cy + 6, cx + 1, cy + r - 6, dim);
		for (int k = 1; k <= 4; k++) {
			int d = k * r / 5;
			ctx.fill(cx - 3, cy + d, cx + 4, cy + d + 1, dim);
			ctx.fill(cx + d, cy - 2, cx + d + 1, cy + 3, dim);
			ctx.fill(cx - d, cy - 2, cx - d + 1, cy + 3, dim);
		}
		ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, green);
		// Brackets
		int b = 10;
		ctx.fill(cx - b, cy - b, cx - b + 4, cy - b + 1, green);
		ctx.fill(cx - b, cy - b, cx - b + 1, cy - b + 4, green);
		ctx.fill(cx + b - 3, cy - b, cx + b + 1, cy - b + 1, green);
		ctx.fill(cx + b, cy - b, cx + b + 1, cy - b + 4, green);
		ctx.fill(cx - b, cy + b, cx - b + 4, cy + b + 1, green);
		ctx.fill(cx - b, cy + b - 3, cx - b + 1, cy + b + 1, green);
		ctx.fill(cx + b - 3, cy + b, cx + b + 1, cy + b + 1, green);
		ctx.fill(cx + b, cy + b - 3, cx + b + 1, cy + b + 1, green);
		// Readout: range to what you're aiming at, and its name if it's a creature
		if (zoom > 0.6f && mc.cameraEntity != null) {
			HitResult hit = mc.cameraEntity.raycast(96, counter.getTickDelta(true), false);
			Vec3d eye = mc.cameraEntity.getCameraPosVec(counter.getTickDelta(true));
			String range = hit.getType() == HitResult.Type.MISS ? "---" : String.format("%.1fm", hit.getPos().distanceTo(eye));
			Entity target = mc.targetedEntity;
			if (mc.crosshairTarget instanceof EntityHitResult eh) target = eh.getEntity();
			ctx.drawTextWithShadow(mc.textRenderer, "RNG " + range, cx + 14, cy + 6, 0xFF66FF99);
			if (target != null) {
				ctx.drawTextWithShadow(mc.textRenderer, target.getName().getString().toUpperCase(), cx + 14, cy - 14, 0xFFFF5555);
			}
			ctx.drawTextWithShadow(mc.textRenderer, "ZOOM x4", cx - r + 14, cy + r - 22, 0xAA66FF99);
		}
	}
}
