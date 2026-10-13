package com.phantompowers.client;

import com.phantompowers.GhostPowers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;

/**
 * The ghost ability bar, tucked in next to the hotbar: a status chip, a glowing energy bar and one slot per
 * unlocked power showing its key, a cooldown sweep, and a highlight when it's switched on.
 */
public final class GhostHud {
	private static final String[] SHORT = {"BLAST", "PHASE", "POSSESS", "WAIL", "TWISTER", "INVIS", "FROST", "FISTS", "SHIELD", "CLONES"};
	private static final int SLOT = 20, GAP = 2;

	private GhostHud() {}

	public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!ClientGhost.powers || mc.player == null || mc.options.hudHidden || mc.currentScreen != null || ClientGhost.cutscene > 0) return;
		TextRenderer tr = mc.textRenderer;
		float time = mc.player.age + tickCounter.getTickDelta(true);

		KeyBinding[] keys = PhantomClient.skillKeys();
		int count = 0;
		for (int i = 0; i < keys.length; i++) if (ClientGhost.has(i)) count++;
		int barW = Math.max(96, count * (SLOT + GAP) - GAP);

		int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
		int x0 = w / 2 + 91 + 10;
		int room = w - x0 - 4;
		float scale = room >= barW ? 1f : Math.max(0.5f, room / (float) barW);

		ctx.getMatrices().push();
		ctx.getMatrices().translate(x0, h - 2, 0);
		ctx.getMatrices().scale(scale, scale, 1f);
		// Local coordinates: (0,0) is the bottom-left corner of the bar
		boolean ghost = ClientGhost.transformed;
		int accent = ghost ? 0xFF4DFF88 : 0xFF9AA7A0;

		// --- skill slots
		int y = -SLOT;
		int x = 0;
		for (int i = 0; i < keys.length; i++) {
			if (!ClientGhost.has(i)) continue;
			slot(ctx, tr, x, y, i, keys[i], ghost, time);
			x += SLOT + GAP;
		}

		// --- energy bar
		int ey = y - 9;
		float e = MathHelper.clamp(ClientGhost.energy / GhostPowers.MAX_ENERGY, 0f, 1f);
		int fill = (int) ((barW - 2) * e);
		ctx.fill(-1, ey - 1, barW + 1, ey + 6, 0xC0101814);
		ctx.fill(0, ey, barW, ey + 5, 0xFF1C2A22);
		if (fill > 0) {
			ctx.fillGradient(1, ey + 1, 1 + fill, ey + 4, 0xFF9CFFC0, 0xFF22C860);
			ctx.fill(1, ey + 1, 1 + fill, ey + 2, 0x80FFFFFF);
			// moving shimmer
			int sx = (int) ((time * 2.2f) % (barW + 30)) - 15;
			if (sx > 1 && sx < fill - 3) ctx.fill(sx, ey + 1, sx + 3, ey + 4, 0x60FFFFFF);
		}
		for (int q = 1; q < 4; q++) ctx.fill(barW * q / 4, ey + 1, barW * q / 4 + 1, ey + 4, 0x50000000);

		// --- status chip
		int sy = ey - 12;
		String status = ghost ? "GHOST" : "HUMAN";
		if (ClientGhost.exhaust > 0 && !ghost) status = "DRAINED " + (ClientGhost.exhaust / 20 + 1) + "s";
		String extra = ClientGhost.possessing ? "possessing" : ClientGhost.intangible ? "intangible" : ClientGhost.invisible ? "invisible" : "";
		int chipW = tr.getWidth(status) + 8;
		ctx.fill(0, sy, chipW, sy + 10, 0xD0101814);
		ctx.fill(0, sy, 2, sy + 10, accent);
		ctx.drawText(tr, status, 5, sy + 1, accent, false);
		String lv = "Lv " + ClientGhost.level;
		int lx = chipW + 4;
		ctx.drawTextWithShadow(tr, lv, lx, sy + 1, 0xFFEFEFEF);
		lx += tr.getWidth(lv) + 6;
		if (ClientGhost.points > 0 || ghost) {
			String pts = "✦" + ClientGhost.points;
			ctx.drawTextWithShadow(tr, pts, lx, sy + 1, 0xFFFFE066);
			lx += tr.getWidth(pts) + 6;
		}
		if (!extra.isEmpty()) ctx.drawTextWithShadow(tr, extra, lx, sy + 1, 0xFF7FF6FF);
		if (!ghost && ClientGhost.exhaust <= 0) {
			String hint = "[" + PhantomClient.keyName(PhantomClient.TRANSFORM) + "] go ghost";
			ctx.getMatrices().push();
			ctx.getMatrices().translate(0, sy - 8, 0);
			ctx.getMatrices().scale(0.75f, 0.75f, 1f);
			ctx.drawTextWithShadow(tr, hint, 0, 0, 0xFFB8C4BC);
			ctx.getMatrices().pop();
		}
		ctx.getMatrices().pop();
	}

	private static void slot(DrawContext ctx, TextRenderer tr, int x, int y, int skill, KeyBinding key, boolean ghost, float time) {
		int cd = skill == GhostPowers.WAIL ? ClientGhost.wailCooldown : skill == GhostPowers.TORNADO ? ClientGhost.tornadoCooldown
				: skill == GhostPowers.FROST ? ClientGhost.frostCooldown : 0;
		int maxCd = skill == GhostPowers.WAIL ? 260 : skill == GhostPowers.TORNADO ? 1200 : 200;
		boolean on = (skill == GhostPowers.INTANGIBLE && ClientGhost.intangible) || (skill == GhostPowers.INVISIBLE && ClientGhost.invisible)
				|| (skill == GhostPowers.FISTS && FistGlowFeature.active(MinecraftClient.getInstance().player))
				|| (skill == GhostPowers.SHIELD && com.phantompowers.GhostFlags.CLIENT_SHIELDED.contains(MinecraftClient.getInstance().player.getUuid()));
		boolean usable = ghost || skill == GhostPowers.INTANGIBLE || skill == GhostPowers.INVISIBLE;

		int border = on ? 0xFF7FF6FF : usable && cd <= 0 ? 0xFF3FD978 : 0xFF4A524D;
		if (on) {
			int glow = (int) (90 + 60 * Math.sin(time * 0.25));
			ctx.fill(x - 1, y - 1, x + SLOT + 1, y + SLOT + 1, glow << 24 | 0x7FF6FF);
		}
		ctx.fill(x, y, x + SLOT, y + SLOT, border);
		ctx.fillGradient(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, usable ? 0xF0213328 : 0xF01A1D1B, usable ? 0xF00E1712 : 0xF0101211);
		// cooldown sweep from the top
		if (cd > 0) {
			int hh = (int) ((SLOT - 2) * Math.min(1f, cd / (float) maxCd));
			ctx.fill(x + 1, y + 1, x + SLOT - 1, y + 1 + hh, 0xB0000000);
		}
		String k = PhantomClient.keyName(key);
		if (k.length() > 2) k = k.substring(0, 2);
		int kc = usable ? 0xFFFFFFFF : 0xFF7E8681;
		ctx.drawTextWithShadow(tr, k, x + (SLOT - tr.getWidth(k)) / 2, y + 3, kc);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(x + SLOT / 2f, y + 13.5f, 0);
		ctx.getMatrices().scale(0.5f, 0.5f, 1f);
		String label = cd > 0 ? (cd / 20 + 1) + "s" : SHORT[skill];
		int lc = cd > 0 ? 0xFFFF8866 : usable ? 0xFF9CFFC0 : 0xFF7E8681;
		ctx.drawText(tr, label, -tr.getWidth(label) / 2, 0, lc, false);
		ctx.getMatrices().pop();
	}
}
