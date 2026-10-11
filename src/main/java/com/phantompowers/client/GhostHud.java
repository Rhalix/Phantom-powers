package com.phantompowers.client;

import com.phantompowers.GhostPowers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;

/** The ghost powers panel on the left side of the screen. */
public final class GhostHud {
	private static final int GREEN = 0xFF55FF77;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFF8A8A8A;
	private static final int YELLOW = 0xFFFFEE55;
	private static final int AQUA = 0xFF66FFFF;

	private GhostHud() {}

	public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!ClientGhost.powers || mc.player == null || mc.options.hudHidden) return;
		TextRenderer tr = mc.textRenderer;

		int x = 6;
		int width = 170;
		int line = 11;
		int rows = 9;
		int height = rows * line + 10;
		int y = ctx.getScaledWindowHeight() / 2 - height / 2;

		ctx.fill(x - 4, y - 4, x + width, y + height - 4, 0x99000000);
		ctx.fill(x - 4, y - 4, x - 2, y + height - 4, ClientGhost.transformed ? GREEN : GRAY);

		// Header
		if (ClientGhost.transformed) {
			ctx.drawTextWithShadow(tr, "GHOST FORM", x, y, GREEN);
		} else {
			ctx.drawTextWithShadow(tr, "Human form  [" + keyName(PhantomClient.TRANSFORM) + "] go ghost", x, y, GRAY);
		}
		y += line;

		// Status
		String status = ClientGhost.possessing ? "POSSESSING" : ClientGhost.intangible ? "INTANGIBLE" : "";
		if (ClientGhost.exhaust > 0 && !ClientGhost.transformed) status = "Drained (" + (ClientGhost.exhaust / 20 + 1) + "s)";
		if (!status.isEmpty()) ctx.drawTextWithShadow(tr, status, x, y, AQUA);
		y += line;

		// Energy bar
		ctx.drawTextWithShadow(tr, "Ghost Energy", x, y, WHITE);
		y += line - 1;
		int barW = width - 10;
		int filled = (int) (barW * Math.max(0, Math.min(1, ClientGhost.energy / GhostPowers.MAX_ENERGY)));
		ctx.fill(x, y, x + barW, y + 5, 0xFF222222);
		ctx.fill(x, y, x + filled, y + 5, 0xFF33FF66);
		y += 9;

		// Stat points
		String pts = "Stat Points: " + ClientGhost.points;
		if (ClientGhost.transformed) {
			int secs = (GhostPowers.POINT_TICKS - ClientGhost.progress) / 20;
			pts += "  (+1 in " + secs + "s)";
		}
		ctx.drawTextWithShadow(tr, pts, x, y, YELLOW);
		y += line + 2;

		// Skills
		KeyBinding[] keys = {PhantomClient.BLAST, PhantomClient.INTANGIBLE, PhantomClient.POSSESS, PhantomClient.WAIL};
		for (int i = 0; i < 4; i++) {
			String key = "[" + keyName(keys[i]) + "] ";
			String text;
			int color;
			if (ClientGhost.has(i)) {
				text = key + GhostPowers.NAMES[i];
				if (i == GhostPowers.WAIL && ClientGhost.wailCooldown > 0) {
					text += " (" + (ClientGhost.wailCooldown / 20 + 1) + "s)";
					color = GRAY;
				} else {
					color = ClientGhost.transformed ? WHITE : GRAY;
				}
				if (i == GhostPowers.INTANGIBLE) text += "  (sneak: aim)";
			} else {
				boolean next = i == 0 || ClientGhost.has(i - 1);
				boolean afford = ClientGhost.points >= GhostPowers.COSTS[i];
				text = key + GhostPowers.NAMES[i] + " - " + GhostPowers.COSTS[i] + " pts";
				if (next && afford) {
					text = key + "Unlock " + GhostPowers.NAMES[i] + "!";
					color = YELLOW;
				} else {
					color = 0xFF555555;
				}
			}
			ctx.drawTextWithShadow(tr, text, x, y, color);
			y += line;
		}
	}

	private static String keyName(KeyBinding key) {
		return key.getBoundKeyLocalizedText().getString().toUpperCase();
	}
}
