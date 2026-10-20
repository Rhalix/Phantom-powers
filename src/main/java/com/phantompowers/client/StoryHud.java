package com.phantompowers.client;

import com.phantompowers.network.StoryPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

/** The story objective tracker in the top-left corner. */
public final class StoryHud {
	private static StoryPayload current;
	private static int changedAt, ticks;

	private StoryHud() {}

	public static void update(StoryPayload p) {
		if (current == null || !current.objective().equals(p.objective())) changedAt = ticks;
		current = p;
		if (p.openPicker()) MinecraftClient.getInstance().setScreen(new StoryScreen());
	}

	public static void clear() {
		current = null;
	}

	public static void tick() {
		ticks++;
	}

	public static void render(DrawContext ctx, RenderTickCounter counter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (current == null || current.path() == 0 || mc.options.hudHidden || mc.currentScreen != null || ClientGhost.cutscene > 0) return;
		if (mc.getDebugHud().shouldShowDebugHud()) return;
		int col = current.path() == 2 ? 0xFFFF4FC8 : current.path() == 3 ? 0xFF5AE1FF : 0xFF55FF99;
		int w = 170, x = 4, y = 4;
		List<OrderedText> obj = mc.textRenderer.wrapLines(Text.literal(current.objective()), w - 10);
		List<OrderedText> title = mc.textRenderer.wrapLines(Text.literal(current.title()), w - 10);
		int h = 8 + title.size() * 10 + obj.size() * 10 + 10;
		ctx.fill(x, y, x + w, y + h, 0x99050A08);
		ctx.fill(x, y, x + 2, y + h, col);
		int ty = y + 4;
		for (OrderedText t : title) {
			ctx.drawTextWithShadow(mc.textRenderer, t, x + 6, ty, col);
			ty += 10;
		}
		boolean fresh = ticks - changedAt < 60 && (ticks / 5) % 2 == 0;
		for (OrderedText t : obj) {
			ctx.drawTextWithShadow(mc.textRenderer, t, x + 6, ty, fresh ? 0xFFFFFF80 : 0xFFE0E8E4);
			ty += 10;
		}
		// progress pips
		for (int i = 0; i < current.steps(); i++) {
			int px = x + 6 + i * 9;
			ctx.fill(px, ty + 2, px + 7, ty + 5, i < current.step() ? col : 0x55FFFFFF);
		}
	}
}
