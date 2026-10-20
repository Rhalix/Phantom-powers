package com.phantompowers.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * Shared look for the mod's menus: see-through ghostly panels with a glowing ecto border, wisps drifting up
 * through them, a slow shimmer sweeping across, wobbling glowing titles and translucent glowing buttons.
 */
public final class GhostlyUi {
	public static final int GREEN = 0x55FF99, PINK = 0xFF4FC8, CYAN = 0x5AE1FF;

	private GhostlyUi() {}

	public static float time() {
		MinecraftClient mc = MinecraftClient.getInstance();
		return (mc.world != null ? mc.world.getTime() : System.currentTimeMillis() / 50) + mc.getRenderTickCounter().getTickDelta(true);
	}

	/** A ghostly panel. accent is 0xRRGGBB. */
	public static void panel(DrawContext ctx, int x, int y, int w, int h, int accent) {
		float t = time();
		float pulse = 0.75f + 0.25f * MathHelper.sin(t * 0.08f);
		for (int i = 7; i >= 1; i--) {
			int a = (int) ((0x0C * (8 - i)) * pulse);
			ctx.fill(x - i, y - i, x + w + i, y + h + i, (a << 24) | accent);
		}
		ctx.fillGradient(x, y, x + w, y + h, 0xE6081210, 0xEE020604);
		// faint inner glow at the top
		ctx.fillGradient(x, y, x + w, y + Math.min(30, h), (0x30 << 24) | accent, 0x00000000);
		// drifting wisps
		for (int i = 0; i < Math.max(6, w / 18); i++) {
			float seed = i * 37.17f;
			float cycle = ((t * (0.35f + (i % 3) * 0.12f) + seed * 10) % (h + 20)) / (h + 20);
			int wx = x + 6 + (int) ((seed * 13.7f) % Math.max(1, w - 12) + MathHelper.sin(t * 0.05f + seed) * 6);
			int wy = y + h - (int) (cycle * h);
			int a = (int) (90 * MathHelper.sin(cycle * MathHelper.PI));
			if (a <= 4 || wy < y || wy > y + h - 3) continue;
			ctx.fill(wx, wy, wx + 2, wy + 3, (a << 24) | accent);
			ctx.fill(wx, wy + 3, wx + 1, wy + 7, ((a / 3) << 24) | accent);
		}
		// shimmer sweeping across
		int sx = x + (int) (((t * 2.2f) % (w + 80)) - 40);
		for (int k = 0; k < 24; k++) {
			int px = sx + k;
			if (px < x || px >= x + w) continue;
			int a = (int) (22 * MathHelper.sin(k / 24f * MathHelper.PI));
			ctx.fill(px, y + 1, px + 1, y + h - 1, (a << 24) | 0xFFFFFF);
		}
		// glowing edges
		int edge = 0xFF000000 | accent;
		ctx.fill(x, y, x + w, y + 1, edge);
		ctx.fill(x, y + h - 1, x + w, y + h, (0x99 << 24) | accent);
		ctx.fill(x, y, x + 1, y + h, (0x99 << 24) | accent);
		ctx.fill(x + w - 1, y, x + w, y + h, (0x99 << 24) | accent);
	}

	/** A wobbling glowing title. */
	public static void title(DrawContext ctx, TextRenderer tr, String s, int cx, int y, int accent) {
		float t = time();
		int w = tr.getWidth(s);
		int x = cx - w / 2;
		for (int i = 0; i < s.length(); i++) {
			String ch = s.substring(i, i + 1);
			int dy = (int) Math.round(MathHelper.sin(t * 0.15f + i * 0.6f) * 1.5);
			int glowA = (int) (60 + 40 * MathHelper.sin(t * 0.1f + i));
			ctx.drawText(tr, ch, x + 1, y + dy + 1, (glowA << 24) | accent, false);
			ctx.drawText(tr, ch, x, y + dy, 0xFF000000 | lighten(accent), true);
			x += tr.getWidth(ch);
		}
	}

	public static int lighten(int c) {
		int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
		return ((r + (255 - r) / 2) << 16) | ((g + (255 - g) / 2) << 8) | (b + (255 - b) / 2);
	}

	/** A see-through button that glows when you point at it. */
	public static class GhostButton extends ButtonWidget {
		private final int accent;

		public GhostButton(int x, int y, int w, int h, Text message, PressAction onPress, int accent) {
			super(x, y, w, h, message, onPress, DEFAULT_NARRATION_SUPPLIER);
			this.accent = accent;
		}

		@Override
		protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
			boolean hot = isSelected() && active;
			float t = time();
			int base = active ? (hot ? 0x70 : 0x34) : 0x14;
			if (hot) for (int i = 3; i >= 1; i--) ctx.fill(getX() - i, getY() - i, getRight() + i, getBottom() + i, ((0x14 * (4 - i)) << 24) | accent);
			ctx.fill(getX(), getY(), getRight(), getBottom(), (base << 24) | accent);
			ctx.fill(getX(), getY(), getRight(), getBottom(), 0x66000000);
			int edgeA = active ? (hot ? 0xFF : 0xAA) : 0x44;
			ctx.fill(getX(), getY(), getRight(), getY() + 1, (edgeA << 24) | accent);
			ctx.fill(getX(), getBottom() - 1, getRight(), getBottom(), ((edgeA / 2) << 24) | accent);
			if (hot) {
				int sx = getX() + (int) ((t * 3) % (getWidth() + 20)) - 10;
				for (int k = 0; k < 10; k++) {
					int px = sx + k;
					if (px > getX() && px < getRight()) ctx.fill(px, getY() + 1, px + 1, getBottom() - 1, 0x22FFFFFF);
				}
			}
			TextRenderer tr = MinecraftClient.getInstance().textRenderer;
			int color = active ? (hot ? 0xFFFFFFFF : 0xFF000000 | lighten(accent)) : 0xFF707A76;
			ctx.drawCenteredTextWithShadow(tr, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
		}
	}
}
