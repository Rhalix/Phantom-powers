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
	private static final String[] SHORT = {"BLAST", "PHASE", "POSSESS", "WAIL", "TWISTER", "INVIS", "FROST", "FISTS", "SHIELD", "CLONES", "CONSTRUCT", "SPIN", "STINGER", "ABSORB"};
	private static final int SLOT = 20, GAP = 2;

	private GhostHud() {}

	public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!ClientGhost.powers && ClientGhost.acne > 0 && mc.player != null && !mc.options.hudHidden && mc.currentScreen == null && ClientGhost.cutscene <= 0) {
			// a human with ecto acne: a countdown chip centred above the hotbar, so it never runs off screen
			int secs = ClientGhost.acne / 20;
			String txt = "ECTO ACNE  " + secs / 60 + ":" + String.format("%02d", secs % 60);
			int tw = mc.textRenderer.getWidth(txt) + 10;
			int x0 = ctx.getScaledWindowWidth() / 2 - tw / 2, y0 = ctx.getScaledWindowHeight() - 66;
			float tm = mc.player.age + tickCounter.getTickDelta(true);
			ctx.fill(x0, y0, x0 + tw, y0 + 11, 0xD0101814);
			ctx.fill(x0, y0, x0 + 2, y0 + 11, 0xFF9CFF40);
			ctx.fill(x0 + tw - 2, y0, x0 + tw, y0 + 11, 0xFF9CFF40);
			ctx.drawText(mc.textRenderer, txt, x0 + 5, y0 + 2, (int) (tm / 6) % 2 == 0 ? 0xFFB8FF60 : 0xFF7FD040, false);
		}
		if (!ClientGhost.powers || mc.player == null || mc.options.hudHidden || mc.currentScreen != null || ClientGhost.cutscene > 0) return;
		TextRenderer tr = mc.textRenderer;
		float time = mc.player.age + tickCounter.getTickDelta(true);

		KeyBinding[] keys = PhantomClient.skillKeys();
		int count = 0;
		for (int i = 0; i < keys.length; i++) if (shown(i)) count++;
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
		boolean dark = ClientGhost.darkMode;
		// hero form: ecto green; dark form: toxic green with hot-pink sparks; human: pale grey-green
		int accent = ghost ? (dark ? 0xFF8CFF4A : 0xFF4DFF88) : 0xFF9AB8A6;
		int spark = dark && ghost ? 0xFF4FC8 : 0xC8FFDC;

		// --- skill orbs
		int y = -SLOT;
		int x = 0;
		for (int i = 0; i < keys.length; i++) {
			if (!shown(i)) continue;
			slot(ctx, tr, x, y, i, keys[i], ghost, time, accent);
			x += SLOT + GAP;
		}

		// --- ectoplasm energy bar: a wobbling, dripping tube of glowing goo
		int ey = y - 11;
		float e = MathHelper.clamp(ClientGhost.energy / GhostPowers.MAX_ENERGY, 0f, 1f);
		int inner = barW - 2;
		int fill = (int) (inner * e);
		// misty outline
		ctx.fill(1, ey - 2, barW - 1, ey + 8, 0x30000000 | (accent & 0xFFFFFF) & 0x30FFFFFF);
		ctx.fill(0, ey - 1, barW, ey + 7, 0xB0060C08);
		ctx.fill(-1, ey, 0, ey + 6, 0x60000000 | (accent & 0xFFFFFF));
		ctx.fill(barW, ey, barW + 1, ey + 6, 0x60000000 | (accent & 0xFFFFFF));
		int top = accent | 0xFF000000, bottom = dark && ghost ? 0xFF1E8A28 : 0xFF149A4A;
		for (int col = 0; col < fill; col++) {
			// the surface ripples
			int wave = (int) Math.round(Math.sin(time * 0.25 + col * 0.45) * 0.8 + Math.sin(time * 0.11 + col * 0.21) * 0.6);
			int yTop = ey + 1 + Math.max(0, wave);
			ctx.fillGradient(1 + col, yTop, 2 + col, ey + 6, top, bottom);
		}
		if (fill > 2) {
			// shine, bubbles rising through the goo, and the odd drip falling off the bottom
			ctx.fill(2, ey + 2, 1 + fill, ey + 3, 0x50FFFFFF);
			for (int b = 0; b < 4; b++) {
				int bx = 2 + (int) ((b * 37 + time * 0.7f) % Math.max(1, fill - 2));
				int by = ey + 5 - (int) ((time * 0.15f + b * 1.3f) % 4);
				ctx.fill(bx, by, bx + 1, by + 1, 0xA0000000 | spark);
			}
			for (int d = 0; d < 3; d++) {
				float ph = (time * 0.05f + d * 0.37f) % 1f;
				int dx = 3 + (int) ((d * 53) % Math.max(1, fill - 3));
				int dy = ey + 7 + (int) (ph * 6);
				int da = (int) (200 * (1 - ph));
				ctx.fill(dx, dy, dx + 1, dy + 2, (da << 24) | (accent & 0xFFFFFF));
			}
		}
		// little wisps drifting up off the bar
		for (int k = 0; k < 3; k++) {
			float ph = (time * 0.03f + k * 0.33f) % 1f;
			int wx = (int) ((k * 41 + 13) % Math.max(1, barW)) + (int) (Math.sin(time * 0.1 + k) * 2);
			int wy = ey - 2 - (int) (ph * 9);
			ctx.fill(wx, wy, wx + 1, wy + 1, ((int) (150 * (1 - ph)) << 24) | spark);
		}
		String pct = (int) (e * 100) + "%";
		ctx.getMatrices().push();
		ctx.getMatrices().translate(barW - 1, ey + 1.5f, 0);
		ctx.getMatrices().scale(0.5f, 0.5f, 1f);
		ctx.drawText(tr, pct, -tr.getWidth(pct), 0, 0xFFEFFFF4, false);
		ctx.getMatrices().pop();

		// --- status: glowing ghost text
		int sy = ey - 13;
		String status = ghost ? (dark ? "DARK GHOST" : "GHOST") : "HUMAN";
		if (ClientGhost.exhaust > 0 && !ghost) status = "DRAINED " + (ClientGhost.exhaust / 20 + 1) + "s";
		String extra = ClientGhost.possessing ? "possessing" : ClientGhost.intangible ? "intangible" : ClientGhost.invisible ? "invisible" : "";
		float bob = (float) Math.sin(time * 0.08) * 0.6f;
		ctx.getMatrices().push();
		ctx.getMatrices().translate(0, bob, 0);
		ctx.drawText(tr, status, 1, sy + 1, 0x50000000 | (accent & 0xFFFFFF), false);
		ctx.drawText(tr, status, 0, sy, accent, false);
		int lx = tr.getWidth(status) + 6;
		String lv = "Lv " + ClientGhost.level;
		ctx.drawTextWithShadow(tr, lv, lx, sy, 0xFFEFEFEF);
		lx += tr.getWidth(lv) + 6;
		if (ClientGhost.points > 0 || ghost) {
			String pts = "\u2726" + ClientGhost.points;
			ctx.drawTextWithShadow(tr, pts, lx, sy, 0xFFFFE066);
			lx += tr.getWidth(pts) + 6;
		}
		if (!extra.isEmpty()) ctx.drawTextWithShadow(tr, extra, lx, sy, 0xFF7FF6FF);
		ctx.getMatrices().pop();
		if (ClientGhost.acne > 0) {
			int secs = ClientGhost.acne / 20;
			String a2 = "ECTO ACNE " + secs / 60 + ":" + String.format("%02d", secs % 60);
			int ay = sy - 11;
			ctx.drawText(tr, a2, 0, ay, (int) (time / 6) % 2 == 0 ? 0xFFB8FF60 : 0xFF7FD040, true);
		}
		if (!ghost && ClientGhost.exhaust <= 0) {
			String hint = "[" + PhantomClient.keyName(PhantomClient.TRANSFORM) + "] go ghost";
			ctx.getMatrices().push();
			ctx.getMatrices().translate(0, sy - 9 - (ClientGhost.acne > 0 ? 10 : 0), 0);
			ctx.getMatrices().scale(0.75f, 0.75f, 1f);
			ctx.drawTextWithShadow(tr, hint, 0, 0, 0xFFB8C4BC);
			ctx.getMatrices().pop();
		}
		ctx.getMatrices().pop();
	}

	/** Only powers in your loadout get a slot (dark powers only while you're in your dark form). */
	private static boolean shown(int i) {
		return ClientGhost.has(i) && ClientGhost.equipped(i) && (!GhostPowers.darkOnly(i) || ClientGhost.darkMode);
	}

	/** One power: a dark, see-through spirit orb with a pulsing glow rim, the key in the middle, and a misty cooldown. */
	private static void slot(DrawContext ctx, TextRenderer tr, int x, int y, int skill, KeyBinding key, boolean ghost, float time, int accent) {
		int cd = skill == GhostPowers.WAIL ? ClientGhost.wailCooldown : skill == GhostPowers.TORNADO ? ClientGhost.tornadoCooldown
				: skill == GhostPowers.FROST ? ClientGhost.frostCooldown : 0;
		int maxCd = skill == GhostPowers.WAIL ? 260 : skill == GhostPowers.TORNADO ? 1200 : 200;
		boolean on = (skill == GhostPowers.INTANGIBLE && ClientGhost.intangible) || (skill == GhostPowers.INVISIBLE && ClientGhost.invisible)
				|| (skill == GhostPowers.FISTS && FistGlowFeature.active(MinecraftClient.getInstance().player))
				|| (skill == GhostPowers.SHIELD && com.phantompowers.GhostFlags.CLIENT_SHIELDED.contains(MinecraftClient.getInstance().player.getUuid()));
		boolean usable = ghost || skill == GhostPowers.INTANGIBLE || skill == GhostPowers.INVISIBLE;
		int col = on ? 0x7FF6FF : GhostPowers.darkOnly(skill) ? 0xFF4FC8 : (accent & 0xFFFFFF);
		float pulse = 0.6f + 0.4f * (float) Math.sin(time * 0.15 + skill);
		int rim = usable && cd <= 0 ? (int) ((on ? 255 : 170) * pulse) : 70;
		// soft glow halo
		if (usable) {
			ctx.fill(x - 1, y + 2, x + SLOT + 1, y + SLOT - 2, ((rim / 4) << 24) | col);
			ctx.fill(x + 2, y - 1, x + SLOT - 2, y + SLOT + 1, ((rim / 4) << 24) | col);
		}
		// an octagonal orb: rim then a dark see-through core
		octagon(ctx, x, y, SLOT, (rim << 24) | col);
		octagon(ctx, x + 1, y + 1, SLOT - 2, usable ? 0xC0081410 : 0xC0101210);
		ctx.fillGradient(x + 3, y + 2, x + SLOT - 3, y + 6, 0x30FFFFFF, 0);
		// misty cooldown filling down from the top
		if (cd > 0) {
			int hh = (int) ((SLOT - 4) * Math.min(1f, cd / (float) maxCd));
			ctx.fill(x + 2, y + 2, x + SLOT - 2, y + 2 + hh, 0xB0000000);
			ctx.fill(x + 2, y + 2 + hh, x + SLOT - 2, y + 3 + hh, 0x60FFFFFF);
		}
		// a wisp rising off an active power
		if (on) {
			float ph = (time * 0.06f + skill * 0.2f) % 1f;
			int wx = x + SLOT / 2 + (int) (Math.sin(time * 0.2) * 3), wy = y - (int) (ph * 8);
			ctx.fill(wx, wy, wx + 1, wy + 2, ((int) (200 * (1 - ph)) << 24) | col);
		}
		String k = PhantomClient.keyName(key);
		if (k.length() > 2) k = k.substring(0, 2);
		int kc = usable ? 0xFFFFFFFF : 0xFF7E8681;
		ctx.drawTextWithShadow(tr, k, x + (SLOT - tr.getWidth(k)) / 2, y + 4, kc);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(x + SLOT / 2f, y + 14f, 0);
		ctx.getMatrices().scale(0.45f, 0.45f, 1f);
		String label = cd > 0 ? (cd / 20 + 1) + "s" : SHORT[skill];
		int lc = cd > 0 ? 0xFFFF8866 : usable ? (0xFF000000 | col) : 0xFF7E8681;
		ctx.drawText(tr, label, -tr.getWidth(label) / 2, 0, lc, false);
		ctx.getMatrices().pop();
	}

	/** A filled octagon (a square with its corners clipped). */
	private static void octagon(DrawContext ctx, int x, int y, int size, int color) {
		int c = Math.max(2, size / 5);
		ctx.fill(x + c, y, x + size - c, y + size, color);
		ctx.fill(x, y + c, x + c, y + size - c, color);
		ctx.fill(x + size - c, y + c, x + size, y + size - c, color);
		ctx.fill(x + 1, y + 1, x + c, y + c, color);
		ctx.fill(x + size - c, y + 1, x + size - 1, y + c, color);
		ctx.fill(x + 1, y + size - c, x + c, y + size - 1, color);
		ctx.fill(x + size - c, y + size - c, x + size - 1, y + size - 1, color);
	}
}
