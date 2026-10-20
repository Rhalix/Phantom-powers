package com.phantompowers.client;

import com.phantompowers.network.StoryChoicePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/** Story mode: choose your path. Half-ghost, evil half-ghost, or ghost hunter. */
public class StoryScreen extends Screen {
	private static final String[] NAME = {"HALF-GHOST", "EVIL HALF-GHOST", "GHOST HUNTER"};
	private static final String[] MISSION = {"My Molecules Got All Rearranged", "My Molecules Got All Rearranged", "Ghost Must Die"};
	private static final String[][] DESC = {
			{"A lab accident turns you", "half human, half ghost.", "Use your powers to", "protect Amity Park."},
			{"An unstable prototype", "portal brings out your", "dark side. Pink fire,", "black suit, no mercy."},
			{"No powers, all gadgets:", "the suit, blasters, an", "ecto sword and more.", "Ghosts must die!"}};
	private static final int[] COLOR = {0xFF55FF99, 0xFFFF4FC8, 0xFF5AE1FF};
	private int ticks, cw, ch, left, top;

	public StoryScreen() {
		super(Text.literal("Story Mode"));
	}

	@Override
	protected void init() {
		cw = Math.min(130, (width - 40) / 3);
		ch = 150;
		left = (width - (cw * 3 + 20)) / 2;
		top = Math.max(40, (height - ch) / 2 + 10);
		for (int i = 0; i < 3; i++) {
			final int path = i + 1;
			addDrawableChild(ButtonWidget.builder(Text.literal("Choose"), b -> {
				ClientPlayNetworking.send(new StoryChoicePayload(path));
				close();
			}).dimensions(left + i * (cw + 10) + 10, top + ch - 28, cw - 20, 20).build());
		}
	}

	@Override
	public void tick() {
		ticks++;
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		float t = ticks + delta;
		ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("STORY MODE").formatted(net.minecraft.util.Formatting.BOLD), width / 2, top - 30, 0xFF66FF99);
		ctx.drawCenteredTextWithShadow(textRenderer, "Choose your path. You can't change it without restarting the story.", width / 2, top - 16, 0xFFB0C8BC);
		for (int i = 0; i < 3; i++) {
			int x = left + i * (cw + 10), col = COLOR[i];
			boolean hover = mouseX >= x && mouseX < x + cw && mouseY >= top && mouseY < top + ch;
			int glow = hover ? 6 : 3;
			for (int k = glow; k >= 1; k--) ctx.fill(x - k, top - k, x + cw + k, top + ch + k, ((0x12 * (glow + 1 - k)) << 24) | (col & 0xFFFFFF));
			ctx.fillGradient(x, top, x + cw, top + ch, 0xF0101418, 0xF0060809);
			ctx.fill(x, top, x + cw, top + 2, col);
			// little animated emblem: a swirl of dots
			int cx = x + cw / 2, cy = top + 28;
			for (int d = 0; d < 10; d++) {
				double a = t * 0.08 * (i == 1 ? -1 : 1) + d * Math.PI / 5;
				int r = 9 + (d % 2) * 4;
				int px = cx + (int) (Math.cos(a) * r), py = cy + (int) (Math.sin(a) * r * 0.6);
				ctx.fill(px - 1, py - 1, px + 2, py + 2, col);
			}
			ctx.drawCenteredTextWithShadow(textRenderer, NAME[i], cx, top + 48, col);
			int ly = top + 62;
			for (net.minecraft.text.OrderedText line : textRenderer.wrapLines(Text.literal(MISSION[i]), cw - 10)) {
				ctx.drawCenteredTextWithShadow(textRenderer, line, cx, ly, 0xFFFFE680);
				ly += 10;
			}
			ly += 2;
			for (String line : DESC[i]) {
				ctx.drawCenteredTextWithShadow(textRenderer, line, cx, ly, 0xFFC8D4CE);
				ly += 10;
			}
			if (hover) {
				int a = (int) (60 + 40 * MathHelper.sin(t * 0.3f));
				ctx.fill(x, top + ch - 2, x + cw, top + ch, (a << 24) | (col & 0xFFFFFF));
			}
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
