package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.entity.FriendlyGhostEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ghost Hunter Suit visor tracker: a small ring around the crosshair with little arrows pointing at nearby
 * ghosts (green) and hostile mobs (red). The closest one shows its distance.
 */
public final class SuitTrackerHud {
	private static final double RANGE = 40;
	private static final int RADIUS = 21;
	private static final int MAX_ARROWS = 8;

	private SuitTrackerHud() {}

	public static void render(DrawContext ctx, RenderTickCounter counter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity me = mc.player;
		if (me == null || mc.world == null || mc.options.hudHidden || mc.currentScreen != null || ClientGhost.cutscene > 0) return;
		if (!GhostFlags.CLIENT_SUITED.contains(me.getUuid()) || GhostFlags.CLIENT_GHOSTS.contains(me.getUuid())) return;
		float partial = counter.getTickDelta(true);

		List<LivingEntity> found = new ArrayList<>();
		for (Entity e : mc.world.getEntities()) {
			if (!(e instanceof LivingEntity le) || e == me || !e.isAlive()) continue;
			boolean ghost = e instanceof AbstractGhostEntity;
			boolean ghostPlayer = e instanceof PlayerEntity && GhostFlags.CLIENT_GHOSTS.contains(e.getUuid());
			if (!(ghost || ghostPlayer || e instanceof Monster)) continue;
			if (e.squaredDistanceTo(me) > RANGE * RANGE) continue;
			found.add(le);
		}
		int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
		int t = me.age;

		// Faint tech ring: dotted circle with four ticks, slowly turning
		MatrixStack ms = ctx.getMatrices();
		for (int i = 0; i < 36; i++) {
			if (i % 3 == 2) continue;
			double a = i * Math.PI * 2 / 36 + t * 0.01;
			int x = cx + (int) Math.round(Math.cos(a) * RADIUS), y = cy + (int) Math.round(Math.sin(a) * RADIUS);
			ctx.fill(x, y, x + 1, y + 1, i % 9 == 0 ? 0xAA66E0FF : 0x4466E0FF);
		}
		if (found.isEmpty()) return;
		found.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(me)));

		Vec3d eye = me.getCameraPosVec(partial);
		float yaw = me.getYaw(partial);
		for (int i = 0; i < Math.min(MAX_ARROWS, found.size()); i++) {
			LivingEntity e = found.get(i);
			Vec3d to = e.getLerpedPos(partial).add(0, e.getHeight() / 2, 0).subtract(eye);
			// Angle of the target relative to where you're facing (0 = straight ahead = top of the ring)
			double targetYaw = Math.toDegrees(Math.atan2(-to.x, to.z));
			float rel = MathHelper.wrapDegrees((float) (targetYaw - yaw));
			boolean friendly = e instanceof FriendlyGhostEntity;
			boolean ghost = e instanceof AbstractGhostEntity || e instanceof PlayerEntity;
			int color = friendly ? 0xFF66FFAA : ghost ? 0xFF33FF66 : 0xFFFF4444;
			double dist = Math.sqrt(to.lengthSquared());
			float near = (float) MathHelper.clamp(1 - dist / RANGE, 0.25, 1);
			int alpha = (int) (110 + 145 * near);
			color = (color & 0x00FFFFFF) | (alpha << 24);
			boolean blink = dist < 8 && (t / 4) % 2 == 0;

			ms.push();
			ms.translate(cx + 0.5f, cy + 0.5f, 0);
			ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rel));
			// Chevron pointing outward at the top of the ring
			int r = RADIUS + 3;
			ctx.fill(-1, -r - 4, 1, -r - 3, color);
			ctx.fill(-2, -r - 3, 2, -r - 2, color);
			ctx.fill(-3, -r - 2, -1, -r - 1, color);
			ctx.fill(1, -r - 2, 3, -r - 1, color);
			if (blink) ctx.fill(-1, -r, 1, -r + 1, 0xFFFFFFFF);
			ms.pop();

			if (i == 0) {
				// Distance readout for the closest
				double rad = Math.toRadians(rel);
				int tx = cx + (int) (Math.sin(rad) * (RADIUS + 12));
				int ty = cy - (int) (Math.cos(rad) * (RADIUS + 12));
				String s = (int) dist + "m";
				ms.push();
				ms.translate(tx, ty, 0);
				ms.scale(0.5f, 0.5f, 1f);
				int w = mc.textRenderer.getWidth(s);
				ctx.drawTextWithShadow(mc.textRenderer, s, -w / 2, -4, color | 0xFF000000);
				ms.pop();
			}
		}
	}
}
