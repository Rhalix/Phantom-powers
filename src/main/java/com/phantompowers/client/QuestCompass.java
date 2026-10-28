package com.phantompowers.client;

import com.phantompowers.mixin.client.BossBarHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * A compass bar along the top of the screen: north, east, south and west slide past as you turn, and a glowing
 * marker shows which way your current story objective is (with how far away it is).
 */
public final class QuestCompass {
	private static final int H = 14, SPAN = 180;   // shows 180 degrees: 90 to each side
	private static final String[] CARDINAL = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

	private QuestCompass() {}

	public static void render(DrawContext ctx, RenderTickCounter counter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity p = mc.player;
		if (p == null || mc.options.hudHidden || mc.currentScreen != null || ClientGhost.cutscene > 0 || StoryHud.path() == 0) return;
		if (mc.getDebugHud().shouldShowDebugHud()) return;
		int bars = ((BossBarHudAccessor) mc.inGameHud.getBossBarHud()).phantom$bossBars().size();
		// centred at the top, but kept clear of the objective panel in the top-left corner
		int sw = ctx.getScaledWindowWidth(), W = Math.max(90, Math.min(200, sw - 182));
		int x0 = Math.max(180, sw / 2 - W / 2), cx = x0 + W / 2, y = 3 + Math.min(bars, 4) * 19;
		float yaw = p.getYaw(counter.getTickDelta(true));
		int col = StoryHud.color();

		ctx.fill(x0, y, x0 + W, y + H, 0x99050A08);
		ctx.fill(x0, y + H - 1, x0 + W, y + H, (col & 0x00FFFFFF) | 0x66000000);
		// ticks every 15 degrees, letters every 45
		for (int deg = 0; deg < 360; deg += 15) {
			float rel = MathHelper.wrapDegrees(deg - yaw);
			if (Math.abs(rel) > SPAN / 2f) continue;
			int x = cx + Math.round(rel / (SPAN / 2f) * (W / 2f - 6));
			if (deg % 45 == 0) {
				String s = CARDINAL[deg / 45];
				int c = s.length() == 1 ? (s.equals("N") ? 0xFFFF6A6A : 0xFFFFFFFF) : 0xFFB8C4BE;
				ctx.drawCenteredTextWithShadow(mc.textRenderer, s, x, y + 3, c);
			} else {
				ctx.fill(x, y + 4, x + 1, y + 9, 0x88FFFFFF);
			}
		}
		// the middle
		ctx.fill(cx, y - 1, cx + 1, y + 2, 0xFFFFFFFF);

		if (!StoryHud.hasTarget()) return;
		String dim = mc.world.getRegistryKey().getValue().toString();
		Vec3d target = StoryHud.target();
		boolean elsewhere = !dim.equals(StoryHud.targetDim());
		if (elsewhere) {
			String where = StoryHud.targetDim().contains("ghost_zone") ? "Objective is in the Ghost Zone" : "Objective is back in the Overworld";
			ctx.drawCenteredTextWithShadow(mc.textRenderer, where, cx, y + H + 2, 0xFFFFE680);
			return;
		}
		double dx = target.x - p.getX(), dz = target.z - p.getZ(), dy = target.y - p.getY();
		float bearing = (float) (MathHelper.atan2(-dx, dz) * MathHelper.DEGREES_PER_RADIAN);
		float rel = MathHelper.wrapDegrees(bearing - yaw);
		boolean off = Math.abs(rel) > SPAN / 2f;
		int mx = cx + Math.round(MathHelper.clamp(rel, -SPAN / 2f, SPAN / 2f) / (SPAN / 2f) * (W / 2f - 6));
		float pulse = 0.6f + 0.4f * MathHelper.sin((mc.world.getTime() + counter.getTickDelta(true)) * 0.25f);
		int a = (int) (255 * pulse) << 24;
		int mark = (col & 0x00FFFFFF) | a;
		if (off) {
			// pointing off the edge: an arrow telling you which way to turn
			String arrow = rel < 0 ? "<<" : ">>";
			ctx.drawCenteredTextWithShadow(mc.textRenderer, arrow, mx, y + 3, mark | 0xFF000000);
		} else {
			// a diamond marker
			for (int i = 0; i < 4; i++) ctx.fill(mx - i, y + 3 + i, mx + i + 1, y + 4 + i, mark);
			for (int i = 0; i < 3; i++) ctx.fill(mx - i, y + 9 - i, mx + i + 1, y + 10 - i, mark);
		}
		int dist = (int) Math.sqrt(dx * dx + dz * dz);
		String label = dist < 3 ? "here" : dist + "m";
		if (dy > 4) label += " ↑";
		else if (dy < -4) label += " ↓";
		ctx.drawCenteredTextWithShadow(mc.textRenderer, label, MathHelper.clamp(mx, x0 + 12, x0 + W - 12), y + H + 2, col | 0xFF000000);
	}
}
