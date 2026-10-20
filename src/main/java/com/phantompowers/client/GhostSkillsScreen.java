package com.phantompowers.client;

import com.phantompowers.GhostPowers;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Ghost Skills: unlock powers with stat points and pick a loadout of up to six for your HUD.
 * Drawn as a haunted ecto panel (drifting wisps, dripping slime edge, wavering title) that sizes itself to
 * the window and scrolls when the list doesn't fit.
 */
public class GhostSkillsScreen extends Screen {
	private static final int ROW_H = 30, HEADER = 74, FOOTER = 32;
	private static final String[] ROMAN = {"I", "II", "III", "IV", "V"};
	private static final String[] SHORT = {"BLAST", "PHASE", "POSSESS", "WAIL", "TWISTER", "INVIS", "FROST", "FISTS", "SHIELD", "CLONES",
			"CONSTRUCT", "SPIN", "STINGER", "ABSORB"};

	private final Screen parent;
	private int panelW, panelH, left, top;
	private double scroll;
	private ButtonWidget darkButton;
	private final List<float[]> wisps = new ArrayList<>();
	private final Random rand = Random.create();
	private int ticks;

	public GhostSkillsScreen(Screen parent) {
		super(Text.literal("Ghost Skills"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		panelW = Math.min(380, width - 12);
		panelH = Math.min(HEADER + FOOTER + GhostPowers.SKILL_COUNT * ROW_H + 8, height - 12);
		left = (width - panelW) / 2;
		top = (height - panelH) / 2;
		int by = top + panelH - 26;
		int bw = Math.min(100, (panelW - 24) / 3);
		addDrawableChild(ButtonWidget.builder(Text.literal("Customize Suit"), b -> client.setScreen(new GhostSuitScreen(this)))
				.dimensions(left + 8, by, bw, 20).build());
		darkButton = addDrawableChild(ButtonWidget.builder(Text.literal("Dark Form"), b -> PhantomClient.send(GhostPowers.A_DARK_TOGGLE))
				.dimensions(left + panelW / 2 - bw / 2, by, bw, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
				.dimensions(left + panelW - 8 - bw, by, bw, 20).build());
		clampScroll();
	}

	private int listTop() {
		return top + HEADER;
	}

	private int listBottom() {
		return top + panelH - FOOTER;
	}

	private int maxScroll() {
		return Math.max(0, GhostPowers.SKILL_COUNT * ROW_H - (listBottom() - listTop()));
	}

	private void clampScroll() {
		scroll = MathHelper.clamp(scroll, 0, maxScroll());
	}

	@Override
	public void tick() {
		ticks++;
		if (wisps.size() < 18 && rand.nextInt(3) == 0) {
			// x, y, speed, size, phase
			wisps.add(new float[]{left + rand.nextFloat() * panelW, top + panelH, 0.3f + rand.nextFloat() * 0.5f, 1 + rand.nextInt(3), rand.nextFloat() * 6f});
		}
		wisps.removeIf(w -> (w[1] -= w[2]) < top + 4);
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		boolean dark = ClientGhost.darkMode;
		int edge = dark ? 0xFF1FE05A : 0xFF33FF77;
		float t = ticks + delta;
		// soft outer glow
		for (int i = 4; i >= 1; i--) {
			int a = 0x10 * (5 - i);
			ctx.fill(left - i, top - i, left + panelW + i, top + panelH + i, a << 24 | (edge & 0xFFFFFF));
		}
		ctx.fillGradient(left, top, left + panelW, top + panelH, dark ? 0xF0041208 : 0xF00A1A12, 0xF0050A08);
		// dripping ectoplasm along the top edge
		ctx.fill(left, top, left + panelW, top + 2, edge);
		for (int x = left + 6; x < left + panelW - 6; x += 9) {
			int seed = (x * 7919) & 0xFF;
			float len = 3 + (seed % 7) + 2.5f * MathHelper.sin(t * 0.05f + seed);
			ctx.fill(x, top + 2, x + 2, top + 2 + (int) len, edge & 0xB0FFFFFF);
			ctx.fill(x, top + 2 + (int) len, x + 2, top + 3 + (int) len, edge & 0x60FFFFFF);
		}
		ctx.fill(left, top + panelH - 1, left + panelW, top + panelH, edge & 0x80FFFFFF);
		// drifting wisps
		for (float[] w : wisps) {
			float wx = w[0] + MathHelper.sin(t * 0.06f + w[4]) * 4;
			float life = (w[1] - top) / panelH;
			int a = (int) (110 * life);
			int s = (int) w[3];
			ctx.fill((int) wx, (int) w[1], (int) wx + s, (int) w[1] + s, a << 24 | (dark ? 0x6CFF6C : 0xA0FFC8));
		}
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		darkButton.visible = ClientGhost.dark;
		darkButton.setMessage(Text.literal(ClientGhost.darkMode ? "Light Form" : "Dark Form"));
		super.render(ctx, mouseX, mouseY, delta);
		TextRenderer tr = textRenderer;
		float t = ticks + delta;
		boolean dark = ClientGhost.darkMode;
		int accent = dark ? 0xFF8CFF4A : 0xFF66FF99;

		// wavering title
		String title = dark ? "DARK GHOST SKILLS" : "GHOST SKILLS";
		int tw = tr.getWidth(title);
		int tx = left + (panelW - tw) / 2;
		for (int i = 0; i < title.length(); i++) {
			String ch = String.valueOf(title.charAt(i));
			float oy = MathHelper.sin(t * 0.12f + i * 0.55f) * 1.5f;
			ctx.getMatrices().push();
			ctx.getMatrices().translate(0, oy, 0);
			ctx.drawTextWithShadow(tr, ch, tx, top + 8, accent);
			ctx.getMatrices().pop();
			tx += tr.getWidth(ch);
		}

		int lvl = ClientGhost.level;
		int hearts = Math.min(20, 2 + (lvl - 1));
		String info = ClientGhost.powers
				? "Level " + lvl + "  ·  +" + hearts + " hearts  ·  Strength " + ROMAN[Math.min(4, (lvl - 1) / 7)] + "  ·  " + ClientGhost.points + " pts"
				: "You don't have ghost powers yet.";
		drawFit(ctx, info, left + panelW / 2, top + 22, panelW - 16, 0xFFFFEE55, true);

		// loadout bar
		int equipped = Integer.bitCount(ClientGhost.loadout);
		String lt = "LOADOUT " + equipped + "/" + GhostPowers.LOADOUT_MAX;
		int slotW = Math.min(46, (panelW - 24 - tr.getWidth(lt) - 8) / GhostPowers.LOADOUT_MAX);
		int lx = left + (panelW - (tr.getWidth(lt) + 8 + slotW * GhostPowers.LOADOUT_MAX)) / 2;
		int ly = top + 36;
		ctx.drawTextWithShadow(tr, lt, lx, ly + 5, 0xFFB8C8BE);
		int sx = lx + tr.getWidth(lt) + 8;
		int[] eq = new int[GhostPowers.LOADOUT_MAX];
		int n = 0;
		for (int i = 0; i < GhostPowers.SKILL_COUNT && n < eq.length; i++) if (ClientGhost.equipped(i)) eq[n++] = i;
		for (int k = 0; k < GhostPowers.LOADOUT_MAX; k++) {
			int x = sx + k * slotW;
			boolean fill = k < n;
			ctx.fill(x, ly, x + slotW - 2, ly + 18, fill ? (accent & 0xFFFFFF | 0x90000000) : 0x50FFFFFF);
			ctx.fill(x + 1, ly + 1, x + slotW - 3, ly + 17, fill ? 0xF0102018 : 0xC0080C0A);
			if (fill) small(ctx, SHORT[eq[k]], x + (slotW - 2) / 2f, ly + 7, 0.5f, accent);
		}

		// skill list (clipped and scrollable)
		int y0 = listTop(), y1 = listBottom();
		ctx.fill(left + 6, y0 - 4, left + panelW - 6, y0 - 3, accent & 0x40FFFFFF);
		ctx.enableScissor(left + 2, y0, left + panelW - 2, y1);
		KeyBinding[] keys = PhantomClient.skillKeys();
		String hover = null;
		for (int i = 0; i < GhostPowers.SKILL_COUNT; i++) {
			int y = y0 + i * ROW_H - (int) scroll;
			if (y + ROW_H < y0 || y > y1) continue;
			boolean has = ClientGhost.has(i);
			boolean on = ClientGhost.equipped(i);
			boolean darkLocked = GhostPowers.darkOnly(i) && !ClientGhost.dark;
			boolean rowHover = mouseX >= left + 6 && mouseX < left + panelW - 6 && mouseY >= Math.max(y, y0) && mouseY < Math.min(y + ROW_H - 2, y1);
			int bg = on ? (dark ? 0x6014501E : 0x60104020) : 0x40000000;
			if (rowHover) bg += 0x18000000;
			ctx.fill(left + 6, y, left + panelW - 6, y + ROW_H - 2, bg);
			ctx.fill(left + 6, y, left + 8, y + ROW_H - 2, has ? (GhostPowers.darkOnly(i) ? 0xFFFF4FC8 : 0xFF3FD978) : 0xFF3A403C);

			// right-side button
			int bw = 62, bx = left + panelW - 12 - bw, byy = y + 5;
			String label;
			int bc;
			if (darkLocked) {
				label = "Dark only";
				bc = 0xFF5A2A32;
			} else if (!has) {
				boolean afford = ClientGhost.points >= GhostPowers.COSTS[i];
				label = afford ? "Unlock" : GhostPowers.COSTS[i] + " pts";
				bc = afford && ClientGhost.powers ? 0xFF2A8A50 : 0xFF2E3431;
			} else {
				label = on ? "Equipped" : "Equip";
				bc = on ? (dark ? 0xFF22A83C : 0xFF2FB866) : 0xFF35503F;
			}
			boolean bHover = mouseX >= bx && mouseX < bx + bw && mouseY >= byy && mouseY < byy + 18 && mouseY >= y0 && mouseY < y1;
			ctx.fill(bx, byy, bx + bw, byy + 18, bHover ? 0xFFE0FFE8 : 0xFF0A100C);
			ctx.fill(bx + 1, byy + 1, bx + bw - 1, byy + 17, bc);
			ctx.drawCenteredTextWithShadow(tr, label, bx + bw / 2, byy + 5, 0xFFFFFFFF);

			// name + key, then a one-line description that's trimmed to fit (full text in the tooltip)
			int textW = bx - (left + 14) - 6;
			String name = GhostPowers.NAMES[i] + "  [" + PhantomClient.keyName(keys[i]) + "]";
			int nameColor = has ? (GhostPowers.darkOnly(i) ? 0xFFFF7FD8 : 0xFF7DFFA8) : 0xFFBBBBBB;
			ctx.drawTextWithShadow(tr, tr.trimToWidth(name, textW), left + 14, y + 5, nameColor);
			String desc = GhostPowers.DESCRIPTIONS[i];
			int descW = (int) (textW / 0.75f);
			String shown = tr.getWidth(desc) > descW ? tr.trimToWidth(desc, descW - tr.getWidth("...")) + "..." : desc;
			ctx.getMatrices().push();
			ctx.getMatrices().translate(left + 14, y + 17, 0);
			ctx.getMatrices().scale(0.75f, 0.75f, 1f);
			ctx.drawTextWithShadow(tr, shown, 0, 0, 0xFF8FA398);
			ctx.getMatrices().pop();
			if (rowHover && !bHover && !shown.equals(desc)) hover = desc;
		}
		ctx.disableScissor();
		// scroll bar
		if (maxScroll() > 0) {
			int track = y1 - y0;
			int thumb = Math.max(16, track * track / (track + maxScroll()));
			int ty = y0 + (int) ((track - thumb) * (scroll / maxScroll()));
			ctx.fill(left + panelW - 5, y0, left + panelW - 3, y1, 0x40FFFFFF);
			ctx.fill(left + panelW - 5, ty, left + panelW - 3, ty + thumb, accent);
		}
		if (hover != null) ctx.drawOrderedTooltip(tr, tr.wrapLines(Text.literal(hover), 220), mouseX, mouseY);
		if (ClientGhost.acne > 0) {
			int s = ClientGhost.acne / 20;
			small(ctx, "Ecto acne: your dark side wakes up in " + s / 60 + ":" + String.format("%02d", s % 60), left + panelW / 2f,
					top + panelH - FOOTER + 1, 0.7f, 0xFFB8FF60);
		}
	}

	private void small(DrawContext ctx, String s, float cx, float y, float scale, int color) {
		ctx.getMatrices().push();
		ctx.getMatrices().translate(cx, y, 0);
		ctx.getMatrices().scale(scale, scale, 1f);
		ctx.drawText(textRenderer, s, -textRenderer.getWidth(s) / 2, 0, color, false);
		ctx.getMatrices().pop();
	}

	/** Centered text that shrinks to fit the given width. */
	private void drawFit(DrawContext ctx, String s, int cx, int y, int maxW, int color, boolean shadow) {
		int w = textRenderer.getWidth(s);
		float k = w > maxW ? maxW / (float) w : 1f;
		ctx.getMatrices().push();
		ctx.getMatrices().translate(cx, y, 0);
		ctx.getMatrices().scale(k, k, 1f);
		ctx.drawText(textRenderer, s, -w / 2, 0, color, shadow);
		ctx.getMatrices().pop();
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (super.mouseClicked(mx, my, button)) return true;
		int y0 = listTop(), y1 = listBottom();
		if (button != 0 || my < y0 || my >= y1) return false;
		int i = (int) ((my - y0 + scroll) / ROW_H);
		if (i < 0 || i >= GhostPowers.SKILL_COUNT) return false;
		int y = y0 + i * ROW_H - (int) scroll;
		int bw = 62, bx = left + panelW - 12 - bw;
		if (mx < bx || mx >= bx + bw || my < y + 5 || my >= y + 23) return false;
		if (GhostPowers.darkOnly(i) && !ClientGhost.dark) return true;
		if (!ClientGhost.has(i)) PhantomClient.send(GhostPowers.A_UNLOCK + i);
		else PhantomClient.send(GhostPowers.A_EQUIP + i);
		client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
				net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0f));
		return true;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		scroll -= v * ROW_H * 0.75;
		clampScroll();
		return true;
	}

	@Override
	public void close() {
		if (client != null) client.setScreen(parent);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
