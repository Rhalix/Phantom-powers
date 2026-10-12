package com.phantompowers.client;

import com.phantompowers.GhostPowers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;

/** A small ghost powers panel on the left side of the screen. */
public final class GhostHud {
	private static final float SCALE = 0.65f;
	private static final int GREEN = 0xFF55FF77;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFF8A8A8A;
	private static final int YELLOW = 0xFFFFEE55;
	private static final int AQUA = 0xFF66FFFF;

	private GhostHud() {}

	public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!ClientGhost.powers || mc.player == null || mc.options.hudHidden || mc.currentScreen != null || ClientGhost.cutscene > 0) return;
		TextRenderer tr = mc.textRenderer;

		KeyBinding[] keys = PhantomClient.skillKeys();
		int unlockedCount = 0;
		for (int i = 0; i < keys.length; i++) if (ClientGhost.has(i)) unlockedCount++;

		int line = 10;
		int width = 120;
		int height = 34 + unlockedCount * line;
		int screenH = (int) (ctx.getScaledWindowHeight() / SCALE);

		ctx.getMatrices().push();
		ctx.getMatrices().scale(SCALE, SCALE, 1f);
		int x = 6;
		int y = screenH / 2 - height / 2;
		ctx.fill(x - 3, y - 3, x + width, y + height - 2, 0x80000000);
		ctx.fill(x - 3, y - 3, x - 1, y + height - 2, ClientGhost.transformed ? GREEN : GRAY);

		String status = (ClientGhost.transformed ? "GHOST" : "HUMAN [" + PhantomClient.keyName(PhantomClient.TRANSFORM) + "]") + "  Lv " + ClientGhost.level;
		int statusColor = ClientGhost.transformed ? GREEN : GRAY;
		if (ClientGhost.possessing) status += " - possessing";
		else if (ClientGhost.intangible) status += " - intangible";
		else if (ClientGhost.invisible) status += " - invisible";
		if (ClientGhost.exhaust > 0 && !ClientGhost.transformed) status = "Drained " + (ClientGhost.exhaust / 20 + 1) + "s";
		ctx.drawTextWithShadow(tr, status, x, y, statusColor);
		y += line;

		int filled = (int) (width * 0.9 * Math.max(0, Math.min(1, ClientGhost.energy / GhostPowers.MAX_ENERGY)));
		ctx.fill(x, y, x + (int) (width * 0.9), y + 4, 0xFF222222);
		ctx.fill(x, y, x + filled, y + 4, 0xFF33FF66);
		y += 7;

		String pts = "Points: " + ClientGhost.points;
		if (ClientGhost.transformed) pts += "  +1 in " + (GhostPowers.POINT_TICKS - ClientGhost.progress) / 20 + "s";
		ctx.drawTextWithShadow(tr, pts, x, y, YELLOW);
		y += line + 2;

		for (int i = 0; i < keys.length; i++) {
			if (!ClientGhost.has(i)) continue;
			String text = PhantomClient.keyName(keys[i]) + "  " + GhostPowers.NAMES[i];
			int cd = i == GhostPowers.WAIL ? ClientGhost.wailCooldown : i == GhostPowers.TORNADO ? ClientGhost.tornadoCooldown : i == GhostPowers.FROST ? ClientGhost.frostCooldown : 0;
			int color = ClientGhost.transformed ? WHITE : GRAY;
			if (cd > 0) {
				text += " " + (cd / 20 + 1) + "s";
				color = GRAY;
			}
			if ((i == GhostPowers.INTANGIBLE && ClientGhost.intangible) || (i == GhostPowers.INVISIBLE && ClientGhost.invisible)) color = AQUA;
			ctx.drawTextWithShadow(tr, text, x, y, color);
			y += line;
		}
		ctx.getMatrices().pop();
	}
}
