package com.phantompowers.client;

import com.phantompowers.Episodes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Movies and specials: the TV movies and the holiday special, playable any time as their own missions. */
public class SpecialsScreen extends Screen {
	private static final int ROW = 30;
	private final Screen parent;
	private int left, top, w, h;

	public SpecialsScreen(Screen parent) {
		super(Text.literal("Movies and Specials"));
		this.parent = parent;
	}

	private static boolean playing() {
		return StoryHud.title() != null && StoryHud.title().startsWith("Special:");
	}

	@Override
	protected void init() {
		int n = Episodes.SPECIALS.size();
		w = Math.min(320, width - 20);
		h = 64 + n * ROW + (playing() ? 26 : 0);
		left = (width - w) / 2;
		top = Math.max(10, (height - h) / 2);
		for (int i = 0; i < n; i++) {
			final int index = i;
			var b = addDrawableChild(new GhostlyUi.GhostButton(left + w - 74, top + 40 + i * ROW, 60, 20, Text.literal("Play"), btn -> {
				MenuNet.send(1, 10 + index, "");
				close();
			}, GhostlyUi.GREEN));
			b.active = StoryHud.path() != 0 && !playing();
		}
		int y = top + 40 + n * ROW;
		if (playing()) {
			addDrawableChild(new GhostlyUi.GhostButton(left + 14, y, w - 28, 20, Text.literal("Leave this special (back to your story)"), btn -> {
				MenuNet.send(1, 9, "");
				close();
			}, GhostlyUi.PINK));
			y += 26;
		}
		addDrawableChild(new GhostlyUi.GhostButton(left + w - 74, y, 60, 20, Text.literal("Back"), btn -> client.setScreen(parent), 0x8899AA));
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		GhostlyUi.panel(ctx, left, top, w, h, GhostlyUi.CYAN);
		GhostlyUi.title(ctx, textRenderer, "MOVIES & SPECIALS", left + w / 2, top + 10, GhostlyUi.CYAN);
		String note = StoryHud.path() == 0 ? "Pick a story path first to play these." : "Play any time. Your story waits for you.";
		ctx.drawCenteredTextWithShadow(textRenderer, note, left + w / 2, top + 24, 0xFFB0C8BC);
		for (int i = 0; i < Episodes.SPECIALS.size(); i++) {
			var e = Episodes.SPECIALS.get(i);
			int y = top + 40 + i * ROW;
			ctx.drawTextWithShadow(textRenderer, e.title(), left + 14, y + 1, 0xFFFFE680);
			ctx.drawTextWithShadow(textRenderer, e.code() + " - " + e.steps().length + " objectives", left + 14, y + 11, 0xFFA8C8B8);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
