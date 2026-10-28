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
		if (StoryHud.path() != 0) {
			initInfo();
			return;
		}
		cw = Math.min(130, (width - 40) / 3);
		ch = 150;
		left = (width - (cw * 3 + 20)) / 2;
		top = Math.max(40, (height - ch) / 2 + 10);
		for (int i = 0; i < 3; i++) {
			final int path = i + 1;
			addDrawableChild(new GhostlyUi.GhostButton(left + i * (cw + 10) + 10, top + ch - 28, cw - 20, 20, Text.literal("Choose"), b -> {
				ClientPlayNetworking.send(new StoryChoicePayload(path));
				close();
			}, COLOR[i] & 0xFFFFFF));
		}
	}

	private boolean confirmRestart;

	/** Story already running: mission, objective, fast travel, restart. */
	private void initInfo() {
		cw = Math.min(300, width - 20);
		ch = 196;
		left = (width - cw) / 2;
		top = (height - ch) / 2;
		int acc = COLOR[StoryHud.path() - 1] & 0xFFFFFF;
		addDrawableChild(new GhostlyUi.GhostButton(left + 14, top + ch - 82, cw - 28, 20, Text.literal("Movies & Specials"),
				b -> client.setScreen(new SpecialsScreen(this)), GhostlyUi.CYAN));
		addDrawableChild(new GhostlyUi.GhostButton(left + 14, top + ch - 56, cw - 28, 20, Text.literal("Fast travel to Amity Park"),
				b -> { MenuNet.send(1, 2, ""); close(); }, acc));
		addDrawableChild(new GhostlyUi.GhostButton(left + 14, top + ch - 30, 130, 20,
				Text.literal(confirmRestart ? "Click again to restart" : "Restart story"), b -> {
					if (confirmRestart) {
						MenuNet.send(1, 1, "");
						close();
					} else {
						confirmRestart = true;
						clearChildren();
						initInfo();
					}
				}, GhostlyUi.PINK));
		addDrawableChild(new GhostlyUi.GhostButton(left + cw - 74, top + ch - 30, 60, 20, Text.literal("Back"), b -> close(), 0x8899AA));
	}

	@Override
	public void tick() {
		ticks++;
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		float t = ticks + delta;
		if (StoryHud.path() != 0) {
			int acc = COLOR[StoryHud.path() - 1] & 0xFFFFFF;
			GhostlyUi.panel(ctx, left, top, cw, ch, acc);
			GhostlyUi.title(ctx, textRenderer, "STORY MODE", left + cw / 2, top + 10, acc);
			ctx.drawCenteredTextWithShadow(textRenderer, NAME[StoryHud.path() - 1] + " PATH", left + cw / 2, top + 24, 0xFF000000 | acc);
			int ly = top + 42;
			for (net.minecraft.text.OrderedText line : textRenderer.wrapLines(Text.literal(StoryHud.title()), cw - 24)) {
				ctx.drawCenteredTextWithShadow(textRenderer, line, left + cw / 2, ly, 0xFFFFE680);
				ly += 10;
			}
			ly += 4;
			for (net.minecraft.text.OrderedText line : textRenderer.wrapLines(Text.literal("Objective: " + StoryHud.objective()), cw - 24)) {
				ctx.drawCenteredTextWithShadow(textRenderer, line, left + cw / 2, ly, 0xFFE0F0EA);
				ly += 10;
			}
			return;
		}
		GhostlyUi.title(ctx, textRenderer, "STORY MODE", width / 2, top - 30, GhostlyUi.GREEN);
		ctx.drawCenteredTextWithShadow(textRenderer, "Choose your path. You can't change it without restarting the story.", width / 2, top - 16, 0xFFB0C8BC);
		for (int i = 0; i < 3; i++) {
			int x = left + i * (cw + 10), col = COLOR[i];
			boolean hover = mouseX >= x && mouseX < x + cw && mouseY >= top && mouseY < top + ch;
			GhostlyUi.panel(ctx, x, top, cw, ch, col & 0xFFFFFF);
			if (hover) ctx.fill(x, top, x + cw, top + ch, 0x12FFFFFF);
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
