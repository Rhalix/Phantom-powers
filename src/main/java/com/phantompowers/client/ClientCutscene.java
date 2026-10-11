package com.phantompowers.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;

/** The hold-V transformation cutscene: camera turns to face you, movie bars, a shout. */
public final class ClientCutscene {
	private static boolean active;
	private static Perspective previous;
	private static int shown;

	private ClientCutscene() {}

	public static void tick(MinecraftClient mc) {
		boolean running = ClientGhost.cutscene > 0 && mc.player != null;
		GameOptions o = mc.options;
		if (running && !active) {
			active = true;
			shown = 0;
			previous = o.getPerspective();
			o.setPerspective(Perspective.THIRD_PERSON_FRONT);
		}
		if (running) {
			shown++;
			o.forwardKey.setPressed(false);
			o.backKey.setPressed(false);
			o.leftKey.setPressed(false);
			o.rightKey.setPressed(false);
			o.jumpKey.setPressed(false);
			o.sneakKey.setPressed(false);
		} else if (active) {
			reset(mc);
		}
	}

	public static void reset(MinecraftClient mc) {
		if (active && previous != null && mc.options != null) mc.options.setPerspective(previous);
		active = false;
		previous = null;
	}

	public static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
		if (!active) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		int w = ctx.getScaledWindowWidth();
		int h = ctx.getScaledWindowHeight();
		float slide = Math.min(1f, (shown + tickCounter.getTickDelta(true)) / 6f);
		int bar = (int) (h * 0.12f * slide);
		ctx.fill(0, 0, w, bar, 0xFF000000);
		ctx.fill(0, h - bar, w, h, 0xFF000000);

		if (shown > 6) {
			String line = ClientGhost.cutsceneToGhost ? "I'M GOING GHOST!" : "Going human...";
			int color = ClientGhost.cutsceneToGhost ? 0xFF66FF99 : 0xFFDDDDDD;
			ctx.getMatrices().push();
			ctx.getMatrices().translate(w / 2f, h - bar / 2f - 6, 0);
			ctx.getMatrices().scale(2f, 2f, 1f);
			ctx.drawCenteredTextWithShadow(mc.textRenderer, line, 0, 0, color);
			ctx.getMatrices().pop();
		}
	}
}
