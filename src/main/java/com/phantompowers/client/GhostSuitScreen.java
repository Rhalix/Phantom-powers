package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.SuitColors;
import com.phantompowers.network.SuitColorsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * Paint your own ghost suit: pick a part (suit, stripes, gloves, boots, trail &amp; glow), choose any colour
 * with the hue bar and shade square (or a quick swatch / preset), and watch yourself wear it live.
 * The flying tail and the trail follow the same colours.
 */
public class GhostSuitScreen extends Screen {
	/** While open, your own model wears the colours being edited. */
	public static boolean previewing;
	public static int[] preview = SuitColors.CLASSIC.clone();
	/** Show the flying tail on the preview. */
	public static boolean previewTail;

	private static final int[] SWATCHES = {0xF4F6FA, 0x16161E, 0x8A9098, 0xB01830, 0xFF6A1A, 0xF0C840, 0x55FF88, 0x1E8A4A,
			0x46AAFF, 0x2A3CB0, 0x8A3CE0, 0xFF66CC};

	private final Screen parent;
	private int panelW, panelH, left, top;
	private int part = SuitColors.BODY;
	private float hue, sat, val;
	private int dragging; // 0 none, 1 square, 2 hue
	// layout
	private int previewW, cx, sqX, sqY, sqW, sqH, hueY;
	private ButtonWidget[] partButtons;
	private ButtonWidget glowButton, tailButton;

	public GhostSuitScreen(Screen parent) {
		super(Text.literal("Ghost Suit"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		if (!previewing) {
			int[] mine = client != null && client.player != null ? GhostFlags.CLIENT_SUIT_COLORS.get(client.player.getUuid()) : null;
			preview = SuitColors.sanitize(mine == null ? SuitColors.CLASSIC : mine);
		}
		previewing = true;
		panelW = Math.min(400, width - 12);
		panelH = Math.min(250, height - 12);
		left = (width - panelW) / 2;
		top = (height - panelH) / 2;
		previewW = Math.max(90, Math.min(130, panelW / 3));
		cx = left + previewW + 12;
		int rightW = left + panelW - 8 - cx;

		// part tabs
		partButtons = new ButtonWidget[SuitColors.PART_NAMES.length];
		int tabW = rightW / partButtons.length;
		for (int i = 0; i < partButtons.length; i++) {
			final int k = i;
			partButtons[i] = addDrawableChild(ButtonWidget.builder(Text.literal(SuitColors.PART_NAMES[i].replace("Trail & Glow", "Glow")), b -> selectPart(k))
					.dimensions(cx + i * tabW, top + 22, tabW - 2, 16).build());
		}
		// colour picker area
		sqX = cx;
		sqY = top + 44;
		sqW = Math.min(rightW - 70, 150);
		sqH = Math.max(40, Math.min(70, panelH - 175));
		hueY = sqY + sqH + 4;
		// presets (two rows of small buttons)
		int presetY = hueY + 10 + 30;
		int perRow = 4;
		int pw = rightW / perRow;
		for (int i = 0; i < SuitColors.PRESETS.length; i++) {
			final int k = i;
			addDrawableChild(ButtonWidget.builder(Text.literal(SuitColors.PRESET_NAMES[i]), b -> {
						preview = SuitColors.PRESETS[k].clone();
						selectPart(part);
					})
					.dimensions(cx + (i % perRow) * pw, presetY + 10 + (i / perRow) * 18, pw - 2, 16).build());
		}
		glowButton = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> preview[SuitColors.FLAGS] ^= 1)
				.dimensions(sqX + sqW + 6, sqY + 30, left + panelW - 8 - (sqX + sqW + 6), 16).build());
		tailButton = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> previewTail = !previewTail)
				.dimensions(left + 8, top + panelH - 26, previewW - 4, 20).build());
		int bw = Math.min(80, (rightW - 4) / 2);
		addDrawableChild(ButtonWidget.builder(Text.literal("Wear it"), b -> {
			ClientPlayNetworking.send(new SuitColorsPayload(preview.clone()));
			close();
		}).dimensions(left + panelW - 8 - bw * 2 - 4, top + panelH - 26, bw, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close())
				.dimensions(left + panelW - 8 - bw, top + panelH - 26, bw, 20).build());
		selectPart(part);
	}

	private void selectPart(int k) {
		part = k;
		float[] hsv = toHsv(preview[k]);
		hue = hsv[0];
		sat = hsv[1];
		val = hsv[2];
	}

	private void apply() {
		preview[part] = fromHsv(hue, sat, val);
	}

	@Override
	public void close() {
		previewing = false;
		previewTail = false;
		if (client != null) client.setScreen(parent);
	}

	@Override
	public void removed() {
		previewing = false;
		previewTail = false;
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		int edge = preview[SuitColors.GLOW] | 0xFF000000;
		for (int i = 4; i >= 1; i--) ctx.fill(left - i, top - i, left + panelW + i, top + panelH + i, (0x10 * (5 - i)) << 24 | (edge & 0xFFFFFF));
		ctx.fillGradient(left, top, left + panelW, top + panelH, 0xF00A1A12, 0xF0050A08);
		ctx.fill(left, top, left + panelW, top + 2, edge);
		// preview stage with a glowing floor
		ctx.fillGradient(left + 8, top + 22, left + 8 + previewW - 4, top + panelH - 30, 0x60000000, 0xA0000000);
		int fy = top + panelH - 40;
		ctx.fill(left + 16, fy, left + previewW - 4, fy + 2, (edge & 0xFFFFFF) | 0x80000000);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		for (int i = 0; i < partButtons.length; i++) partButtons[i].active = i != part;
		glowButton.setMessage(Text.literal("Glow: " + ((preview[SuitColors.FLAGS] & 1) != 0 ? "ON" : "OFF")));
		tailButton.setMessage(Text.literal(previewTail ? "Show legs" : "Show tail"));
		super.render(ctx, mouseX, mouseY, delta);
		TextRenderer tr = textRenderer;
		ctx.drawCenteredTextWithShadow(tr, "Customize Ghost Suit", left + panelW / 2, top + 8, 0xFF66FF99);
		if (client != null && client.player != null) {
			InventoryScreen.drawEntity(ctx, left + 10, top + 26, left + 4 + previewW, top + panelH - 32,
					Math.min(52, (panelH - 70) / 3), 0.0625f, mouseX, mouseY, client.player);
		}

		// shade square: saturation left->right, brightness top->bottom
		ctx.fill(sqX - 1, sqY - 1, sqX + sqW + 1, sqY + sqH + 1, 0xFF000000);
		for (int x = 0; x < sqW; x++) {
			int bright = 0xFF000000 | fromHsv(hue, x / (float) (sqW - 1), 1f);
			ctx.fillGradient(sqX + x, sqY, sqX + x + 1, sqY + sqH, bright, 0xFF000000);
		}
		int mx = sqX + (int) (sat * (sqW - 1)), my = sqY + (int) ((1 - val) * (sqH - 1));
		ctx.fill(mx - 2, my - 2, mx + 3, my + 3, 0xFFFFFFFF);
		ctx.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFF000000 | preview[part]);
		// hue bar
		ctx.fill(sqX - 1, hueY - 1, sqX + sqW + 1, hueY + 7, 0xFF000000);
		for (int x = 0; x < sqW; x++) ctx.fill(sqX + x, hueY, sqX + x + 1, hueY + 6, 0xFF000000 | fromHsv(x / (float) (sqW - 1), 1f, 1f));
		int hx = sqX + (int) (hue * (sqW - 1));
		ctx.fill(hx - 1, hueY - 2, hx + 2, hueY + 8, 0xFFFFFFFF);

		// current colour + hex next to the square
		int rx = sqX + sqW + 6, rw = left + panelW - 8 - rx;
		ctx.fill(rx, sqY, rx + rw, sqY + 24, 0xFF000000);
		ctx.fill(rx + 1, sqY + 1, rx + rw - 1, sqY + 23, 0xFF000000 | preview[part]);
		String hex = String.format("#%06X", preview[part]);
		ctx.drawCenteredTextWithShadow(tr, hex, rx + rw / 2, sqY + 8, val > 0.6f && sat < 0.5f ? 0xFF202020 : 0xFFFFFFFF);
		// mini palette of every part
		int py = sqY + 52;
		for (int i = 0; i < SuitColors.PART_NAMES.length; i++) {
			int bx = rx + i * (rw / SuitColors.PART_NAMES.length);
			int bw = rw / SuitColors.PART_NAMES.length - 2;
			ctx.fill(bx, py, bx + bw, py + 10, i == part ? 0xFFFFFFFF : 0xFF000000);
			ctx.fill(bx + 1, py + 1, bx + bw - 1, py + 9, 0xFF000000 | preview[i]);
		}

		// quick swatches
		int swY = hueY + 12;
		int rightW = left + panelW - 8 - cx;
		int sw = Math.min(18, rightW / SWATCHES.length);
		for (int i = 0; i < SWATCHES.length; i++) {
			int x = cx + i * sw;
			boolean hov = mouseX >= x && mouseX < x + sw - 2 && mouseY >= swY && mouseY < swY + 14;
			ctx.fill(x, swY, x + sw - 2, swY + 14, hov ? 0xFFFFFFFF : 0xFF000000);
			ctx.fill(x + 1, swY + 1, x + sw - 3, swY + 13, 0xFF000000 | SWATCHES[i]);
		}
		ctx.drawTextWithShadow(tr, "Presets", cx, hueY + 10 + 30, 0xFFB8C8BE);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (super.mouseClicked(mx, my, button)) return true;
		if (button != 0) return false;
		if (mx >= sqX && mx < sqX + sqW && my >= sqY && my < sqY + sqH) {
			dragging = 1;
			pick(mx, my);
			return true;
		}
		if (mx >= sqX && mx < sqX + sqW && my >= hueY - 2 && my < hueY + 8) {
			dragging = 2;
			pick(mx, my);
			return true;
		}
		int swY = hueY + 12;
		int rightW = left + panelW - 8 - cx;
		int sw = Math.min(18, rightW / SWATCHES.length);
		if (my >= swY && my < swY + 14 && mx >= cx && mx < cx + sw * SWATCHES.length) {
			preview[part] = SWATCHES[(int) ((mx - cx) / sw)];
			selectPart(part);
			return true;
		}
		// click a part in the mini palette to edit it
		int rx = sqX + sqW + 6, rw = left + panelW - 8 - rx, py = sqY + 52;
		if (my >= py && my < py + 10 && mx >= rx && mx < rx + rw) {
			selectPart(MathHelper.clamp((int) ((mx - rx) / (rw / SuitColors.PART_NAMES.length)), 0, SuitColors.PART_NAMES.length - 1));
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (dragging != 0) {
			pick(mx, my);
			return true;
		}
		return super.mouseDragged(mx, my, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		dragging = 0;
		return super.mouseReleased(mx, my, button);
	}

	private void pick(double mx, double my) {
		if (dragging == 1) {
			sat = MathHelper.clamp((float) ((mx - sqX) / (sqW - 1)), 0f, 1f);
			val = MathHelper.clamp(1f - (float) ((my - sqY) / (sqH - 1)), 0f, 1f);
		} else if (dragging == 2) {
			hue = MathHelper.clamp((float) ((mx - sqX) / (sqW - 1)), 0f, 1f);
		}
		apply();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	static int fromHsv(float h, float s, float v) {
		return MathHelper.hsvToRgb(Math.min(h, 0.9999f), s, v) & 0xFFFFFF;
	}

	static float[] toHsv(int rgb) {
		float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
		float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
		float h = 0;
		if (d > 0) {
			if (max == r) h = ((g - b) / d) % 6;
			else if (max == g) h = (b - r) / d + 2;
			else h = (r - g) / d + 4;
			h /= 6;
			if (h < 0) h += 1;
		}
		return new float[]{h, max == 0 ? 0 : d / max, max};
	}
}
