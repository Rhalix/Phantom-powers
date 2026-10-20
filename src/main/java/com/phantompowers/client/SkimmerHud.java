package com.phantompowers.client;

import com.phantompowers.entity.SkimmerEntity;
import com.phantompowers.network.SkimmerRadarPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Cockpit HUD for the Ecto-Skimmer: a lair-tracking compass strip across the top, a ship status panel
 * (hull, speed, boost, upgrades), the list of scanner contacts, and a camera that pulls out to show off
 * climbing in and out.
 */
public final class SkimmerHud {
	private static final int GREEN = 0xFF5CFF9A, DIM = 0xFF2E8A55, PANEL = 0x99061410, WHITE = 0xFFE8FFF0, WARN = 0xFFFF5A5A;
	private static List<SkimmerRadarPayload.Blip> blips = new ArrayList<>();
	private static boolean inZone;
	private static int ghosts = -1;
	private static int radarAge = 999;

	private static Perspective savedPerspective;
	private static int showOffTicks;
	private static boolean wasAboard;

	private SkimmerHud() {}

	public static void update(SkimmerRadarPayload p) {
		blips = new ArrayList<>(p.blips());
		inZone = p.inZone();
		ghosts = p.ghostsNearby();
		radarAge = 0;
	}

	/** Pull the camera out while climbing in or out, then put it back how you had it. */
	public static void tick(MinecraftClient mc) {
		radarAge++;
		if (mc.player == null) return;
		boolean aboard = mc.player.getVehicle() instanceof SkimmerEntity;
		boolean boarding = aboard && ((SkimmerEntity) mc.player.getVehicle()).state() == SkimmerEntity.BOARDING;
		if (aboard && !wasAboard && boarding) startShowOff(mc, SkimmerEntity.B_READY + 4);
		if (!aboard && wasAboard) startShowOff(mc, SkimmerEntity.E_DONE);
		wasAboard = aboard;
		if (showOffTicks > 0 && --showOffTicks == 0 && savedPerspective != null) {
			mc.options.setPerspective(savedPerspective);
			savedPerspective = null;
		}
		if (!aboard) blips = blips.isEmpty() ? blips : (radarAge > 60 ? new ArrayList<>() : blips);
		// pilot inputs
		SkimmerEntity.clientBoost = aboard && mc.options.sprintKey.isPressed();
		SkimmerEntity.clientUp = aboard && mc.options.jumpKey.isPressed();
		SkimmerEntity.clientDown = false;
	}

	private static void startShowOff(MinecraftClient mc, int ticks) {
		if (savedPerspective == null) savedPerspective = mc.options.getPerspective();
		mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
		showOffTicks = ticks;
	}

	public static void render(DrawContext ctx, RenderTickCounter counter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.options.hudHidden || !(mc.player.getVehicle() instanceof SkimmerEntity ship)) return;
		if (mc.getDebugHud().shouldShowDebugHud()) return;
		float partial = counter.getTickDelta(true);
		TextRenderer tr = mc.textRenderer;
		int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();

		if (ship.state() == SkimmerEntity.BOARDING) {
			float t = ship.stateTime(partial);
			String msg = t < SkimmerEntity.B_SEATED ? "CANOPY OPEN" : t < SkimmerEntity.B_CLOSED ? "SEALING COCKPIT..." : "ENGINES SPOOLING UP...";
			int a = (int) (180 + 75 * MathHelper.sin(t * 0.6f));
			ctx.drawCenteredTextWithShadow(tr, msg, w / 2, h / 2 + 30, (a << 24) | 0x5CFF9A);
			bar(ctx, w / 2 - 50, h / 2 + 42, 100, 4, Math.min(1f, t / SkimmerEntity.B_READY), GREEN);
			return;
		}

		compass(ctx, tr, mc, ship, w);

		// ---- status panel, bottom left
		int px = 6, py = h - 92;
		ctx.fill(px, py, px + 132, py + 86, PANEL);
		ctx.fill(px, py, px + 132, py + 1, GREEN);
		ctx.drawTextWithShadow(tr, "ECTO-SKIMMER", px + 4, py + 4, GREEN);
		float hull = ship.health() / SkimmerEntity.MAX_HEALTH;
		ctx.drawTextWithShadow(tr, "HULL", px + 4, py + 16, DIM);
		bar(ctx, px + 34, py + 18, 92, 5, hull, hull < 0.3f ? WARN : GREEN);
		double speed = ship.getVelocity().length() * 20;
		ctx.drawTextWithShadow(tr, "SPD", px + 4, py + 27, DIM);
		ctx.drawTextWithShadow(tr, String.format("%.1f m/s", speed), px + 34, py + 27, WHITE);
		ctx.drawTextWithShadow(tr, "BST", px + 4, py + 38, DIM);
		bar(ctx, px + 34, py + 40, 92, 5, ship.boostLevel(), 0xFF8AE8FF);
		StringBuilder ups = new StringBuilder();
		String[] tags = {"CAN", "THR", "SHD", "SCN", "PHS"};
		for (int i = 0; i < tags.length; i++) if (ship.has(1 << i)) ups.append(tags[i]).append(' ');
		ctx.drawTextWithShadow(tr, ups.length() == 0 ? "no upgrades" : ups.toString().trim(), px + 4, py + 50, ups.length() == 0 ? DIM : 0xFFB8FFE0);
		if (ship.phasing()) ctx.drawTextWithShadow(tr, "PHASED", px + 88, py + 4, 0xFFC8B4FF);
		ctx.drawTextWithShadow(tr, "Space up  Ctrl boost", px + 4, py + 64, DIM);
		ctx.drawTextWithShadow(tr, "RMB fire  Sneak exit", px + 4, py + 74, DIM);

		// ---- scanner contacts, bottom right
		int lx = w - 150, ly = h - 92;
		ctx.fill(lx, ly, w - 6, ly + 86, PANEL);
		ctx.fill(lx, ly, w - 6, ly + 1, GREEN);
		ctx.drawTextWithShadow(tr, "LAIR SCANNER", lx + 4, ly + 4, GREEN);
		if (!inZone) {
			ctx.drawTextWithShadow(tr, "No signals here.", lx + 4, ly + 18, DIM);
			ctx.drawTextWithShadow(tr, "Fly in the Ghost Zone", lx + 4, ly + 30, DIM);
			ctx.drawTextWithShadow(tr, "to find ghost lairs.", lx + 4, ly + 40, DIM);
		} else if (blips.isEmpty()) {
			ctx.drawTextWithShadow(tr, radarAge < 40 ? "Scanning..." : "No contacts", lx + 4, ly + 18, DIM);
		} else {
			Vec3d pos = ship.getPos();
			int row = 0;
			for (SkimmerRadarPayload.Blip b : blips) {
				if (row >= 5) break;
				double dx = b.x() + 0.5 - pos.x, dz = b.z() + 0.5 - pos.z;
				int dist = (int) Math.sqrt(dx * dx + dz * dz);
				float rel = relBearing(mc, dx, dz);
				String arrow = arrow(rel);
				int dy = b.y() - (int) pos.y;
				String alt = dy > 6 ? "^" : dy < -6 ? "v" : "";
				ctx.drawTextWithShadow(tr, arrow + " " + b.name(), lx + 4, ly + 16 + row * 11, row == 0 ? WHITE : 0xFFB8FFE0);
				String d = dist + "m" + alt;
				ctx.drawTextWithShadow(tr, d, w - 10 - tr.getWidth(d), ly + 16 + row * 11, GREEN);
				row++;
			}
			if (ghosts >= 0 && row < 6) ctx.drawTextWithShadow(tr, "Ghosts nearby: " + ghosts, lx + 4, ly + 16 + 5 * 11 + 3, ghosts > 0 ? WARN : DIM);
		}
	}

	private static void compass(DrawContext ctx, TextRenderer tr, MinecraftClient mc, SkimmerEntity ship, int w) {
		int cw = 240, cx = w / 2, top = 4;
		ctx.fill(cx - cw / 2, top, cx + cw / 2, top + 22, PANEL);
		ctx.fill(cx - cw / 2, top + 21, cx + cw / 2, top + 22, DIM);
		float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
		String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
		for (int deg = -180; deg < 180; deg += 15) {
			float rel = MathHelper.wrapDegrees(deg - yaw);
			if (Math.abs(rel) > 90) continue;
			int x = cx + (int) (rel / 90f * (cw / 2 - 6));
			boolean major = deg % 45 == 0;
			ctx.fill(x, top + 15, x + 1, top + (major ? 21 : 18), major ? GREEN : DIM);
			if (major) {
				String s = dirs[Math.floorMod(deg / 45 + 8, 8)];
				ctx.drawCenteredTextWithShadow(tr, s, x, top + 4, s.length() == 1 ? WHITE : DIM);
			}
		}
		ctx.fill(cx, top + 12, cx + 1, top + 22, WHITE);
		Vec3d pos = ship.getPos();
		int i = 0;
		for (SkimmerRadarPayload.Blip b : blips) {
			double dx = b.x() + 0.5 - pos.x, dz = b.z() + 0.5 - pos.z;
			float rel = relBearing(mc, dx, dz);
			boolean edge = Math.abs(rel) > 90;
			float clamped = MathHelper.clamp(rel, -90f, 90f);
			int x = cx + (int) (clamped / 90f * (cw / 2 - 6));
			int col = i == 0 ? 0xFFFFE066 : 0xFF66FFC8;
			// diamond marker
			for (int k = 0; k < 4; k++) ctx.fill(x - k, top + 24 + k, x + k + 1, top + 25 + k, col);
			for (int k = 0; k < 3; k++) ctx.fill(x - 2 + k, top + 28 + k, x + 3 - k, top + 29 + k, col);
			if (i == 0 || !edge) {
				int dist = (int) Math.sqrt(dx * dx + dz * dz);
				String label = (i == 0 ? b.name() + " " : "") + dist + "m";
				int lw = tr.getWidth(label);
				int lx = MathHelper.clamp(x - lw / 2, cx - cw / 2, cx + cw / 2 - lw);
				ctx.drawTextWithShadow(tr, label, lx, top + 33 + (i % 2) * 9, col);
			}
			i++;
			if (i >= 6) break;
		}
	}

	/** Bearing of a point relative to where you're looking, in degrees (-180..180, 0 = straight ahead). */
	private static float relBearing(MinecraftClient mc, double dx, double dz) {
		float target = (float) (MathHelper.atan2(-dx, dz) * MathHelper.DEGREES_PER_RADIAN);
		return MathHelper.wrapDegrees(target - mc.player.getYaw());
	}

	private static String arrow(float rel) {
		if (Math.abs(rel) < 22.5f) return "↑";
		if (Math.abs(rel) > 157.5f) return "↓";
		if (rel > 0) return rel < 67.5f ? "↗" : rel < 112.5f ? "→" : "↘";
		return rel > -67.5f ? "↖" : rel > -112.5f ? "←" : "↙";
	}

	private static void bar(DrawContext ctx, int x, int y, int w, int h, float f, int color) {
		ctx.fill(x, y, x + w, y + h, 0xAA000000);
		ctx.fill(x, y, x + Math.round(w * MathHelper.clamp(f, 0f, 1f)), y + h, color);
	}
}
