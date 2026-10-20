package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Wrist missile targeting: while you hold R the view zooms in a touch and a red hunter HUD appears:
 * a spinning reticle, range readout, and lock-on brackets that snap shut around whatever you aim at.
 */
public final class MissileSight {
	private static float amount, prevAmount, lock, prevLock;
	private static Entity target;

	private MissileSight() {}

	private static boolean aiming(MinecraftClient mc) {
		return mc.player != null && !GhostFlags.CLIENT_AIMING.isEmpty() && GhostFlags.CLIENT_AIMING.contains(mc.player.getUuid());
	}

	public static void tick(MinecraftClient mc) {
		prevAmount = amount;
		prevLock = lock;
		boolean on = aiming(mc);
		amount = MathHelper.clamp(amount + (on ? 0.2f : -0.3f), 0f, 1f);
		Entity t = null;
		if (on && mc.player != null) {
			Vec3d eye = mc.player.getEyePos(), end = eye.add(mc.player.getRotationVec(1f).multiply(48));
			EntityHitResult hit = ProjectileUtil.raycast(mc.player, eye, end, new Box(eye, end).expand(1.0),
					e -> e instanceof LivingEntity && e != mc.player && !e.isSpectator() && e.isAlive(), 48 * 48);
			if (hit != null) t = hit.getEntity();
		}
		if (t != target) lock = 0f;
		target = t;
		lock = t == null ? 0f : Math.min(1f, lock + 0.12f);
		if (on && mc.world != null && mc.player != null && mc.player.age % 2 == 0) {
			// laser dot where you're pointing
			HitResult h = mc.player.raycast(48, 1f, false);
			Vec3d at = t != null ? t.getBoundingBox().getCenter() : h.getPos();
			mc.world.addParticle(new net.minecraft.particle.DustParticleEffect(new org.joml.Vector3f(1f, 0.1f, 0.1f), 0.7f), at.x, at.y, at.z, 0, 0, 0);
		}
	}

	public static float fovMultiplier() {
		return 1f - 0.18f * amount;
	}

	public static void render(DrawContext ctx, RenderTickCounter counter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		float pt = counter.getTickDelta(true);
		float a = MathHelper.lerp(pt, prevAmount, amount);
		if (a <= 0.02f || mc.player == null || mc.options.hudHidden) return;
		int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
		int cx = w / 2, cy = h / 2;
		float time = mc.player.age + pt;
		float l = MathHelper.lerp(pt, prevLock, lock);
		boolean locked = l >= 1f;
		int col = locked ? 0xFF4040 : 0xFF6A6A;
		int alpha = (int) (a * 230);
		int c = alpha << 24 | col;
		int dim = (alpha / 2) << 24 | col;
		// faint red vignette at the edges
		int edge = (int) (a * 70) << 24 | 0x400000;
		ctx.fillGradient(0, 0, w, h / 6, edge, 0);
		ctx.fillGradient(0, h - h / 6, w, h, 0, edge);
		// spinning reticle ring (8 segments)
		int r = 22;
		for (int i = 0; i < 48; i++) {
			if (i % 6 > 3) continue;
			double ang = i * Math.PI * 2 / 48 + time * 0.05;
			int x = cx + (int) Math.round(Math.cos(ang) * r), y = cy + (int) Math.round(Math.sin(ang) * r);
			ctx.fill(x, y, x + 1, y + 1, c);
		}
		ctx.fill(cx - 12, cy, cx - 4, cy + 1, c);
		ctx.fill(cx + 5, cy, cx + 13, cy + 1, c);
		ctx.fill(cx, cy - 12, cx + 1, cy - 4, c);
		ctx.fill(cx, cy + 5, cx + 1, cy + 13, c);
		ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, c);
		// lock-on brackets close in as the lock builds
		if (target != null) {
			int b = (int) MathHelper.lerp(l, 34, 14);
			int k = 5;
			ctx.fill(cx - b, cy - b, cx - b + k, cy - b + 1, c);
			ctx.fill(cx - b, cy - b, cx - b + 1, cy - b + k, c);
			ctx.fill(cx + b - k + 1, cy - b, cx + b + 1, cy - b + 1, c);
			ctx.fill(cx + b, cy - b, cx + b + 1, cy - b + k, c);
			ctx.fill(cx - b, cy + b, cx - b + k, cy + b + 1, c);
			ctx.fill(cx - b, cy + b - k + 1, cx - b + 1, cy + b + 1, c);
			ctx.fill(cx + b - k + 1, cy + b, cx + b + 1, cy + b + 1, c);
			ctx.fill(cx + b, cy + b - k + 1, cx + b + 1, cy + b + 1, c);
			String name = target.getName().getString().toUpperCase();
			String status = locked ? ((int) (time / 4) % 2 == 0 ? "LOCKED" : "") : "LOCKING...";
			ctx.drawTextWithShadow(mc.textRenderer, name, cx + 30, cy - 18, c);
			ctx.drawTextWithShadow(mc.textRenderer, status, cx + 30, cy - 8, locked ? 0xFFFF3030 : dim);
			String rng = String.format("%.1fm", target.distanceTo(mc.player));
			ctx.drawTextWithShadow(mc.textRenderer, rng, cx + 30, cy + 2, dim);
		}
		String hint = "WRIST MISSILES  -  release [" + PhantomClient.keyName(PhantomClient.gadgetKey(com.phantompowers.Slots.G_MISSILES)) + "] to fire";
		ctx.getMatrices().push();
		ctx.getMatrices().translate(cx, cy + 34, 0);
		ctx.getMatrices().scale(0.75f, 0.75f, 1f);
		ctx.drawCenteredTextWithShadow(mc.textRenderer, hint, 0, 0, dim);
		ctx.getMatrices().pop();
	}
}
