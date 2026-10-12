package com.phantompowers.client;

import com.phantompowers.GhostPowers;
import com.phantompowers.entity.GliderEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

/** A small "how to use" guide on the right edge of the screen. */
public final class ControlsHud {
	private static final float SCALE = 0.6f;

	private ControlsHud() {}

	private record Line(String key, String text) {}

	public static void render(DrawContext ctx, RenderTickCounter counter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.options.hudHidden || mc.currentScreen != null || ClientGhost.cutscene > 0 || mc.getDebugHud().shouldShowDebugHud()) return;
		List<Line> lines = new ArrayList<>();
		if (mc.player.getVehicle() instanceof GliderEntity) {
			lines.add(new Line("LOOK", "steer the glider"));
			lines.add(new Line(name(mc.options.forwardKey), "fly  /  " + name(mc.options.backKey) + " brake"));
			lines.add(new Line(name(mc.options.sprintKey), "afterburner (sprint fly)"));
			lines.add(new Line(name(mc.options.leftKey) + "/" + name(mc.options.rightKey), "strafe"));
			lines.add(new Line(name(mc.options.useKey), "fire lasers"));
			lines.add(new Line(name(mc.options.sneakKey), "hop off"));
		}
		var held = mc.player.getMainHandStack();
		String use = name(mc.options.useKey), sneak = name(mc.options.sneakKey);
		if (held.isOf(com.phantompowers.ModItems.GHOST_BLASTER)) lines.add(new Line(use, "fire the Ghost Blaster"));
		if (held.isOf(com.phantompowers.ModItems.THUNDER_GUITAR)) {
			lines.add(new Line(use, "Power Chord (giant sound wave)"));
			lines.add(new Line(sneak + "+" + use, "Lightning Riff"));
		}
		if (held.isOf(com.phantompowers.ModItems.THERMOS)) {
			lines.add(new Line(use, "trap the ghost you aim at"));
			lines.add(new Line(sneak + "+" + use, "release all ghosts"));
		}
		if (held.isOf(com.phantompowers.ModItems.GHOST_NET)) lines.add(new Line(use, "throw the Ghost Net"));
		if (held.isOf(com.phantompowers.ModItems.PANDORAS_BOX)) lines.add(new Line(use, "dark shockwave"));
		if (held.getItem() instanceof com.phantompowers.EssenceItem) lines.add(new Line(use + " (hold)", "eat: gain this ghost's power"));
		if (ClientGhost.powers) {
			lines.add(new Line(name(PhantomClient.TRANSFORM), ClientGhost.transformed ? "go human (hold: cutscene)" : "go ghost (hold: cutscene)"));
			KeyBinding[] keys = PhantomClient.skillKeys();
			String[] help = {
					"Ecto Blast",
					"intangible (sneak + hold: carry target)",
					"possess what you look at",
					"Ghostly Wail (hold to keep wailing)",
					"Wail Tornado",
					"invisible",
					"Frost Breath (hold)",
					"Ecto Fists (punch harder)"};
			for (int i = 0; i < keys.length; i++) {
				if (!ClientGhost.has(i)) continue;
				if (i == GhostPowers.BLAST && ClientGhost.possessing && ClientGhost.hostAbility >= 0) {
					String passive = com.phantompowers.HostPowers.PASSIVES[ClientGhost.hostAbility];
					lines.add(new Line(name(keys[i]), "host power: " + com.phantompowers.HostPowers.NAMES[ClientGhost.hostAbility]
							+ (passive.isEmpty() ? "" : "  (+" + passive + ")")));
					continue;
				}
				lines.add(new Line(name(keys[i]), help[i]));
			}
			lines.add(new Line(name(PhantomClient.SKILLS), "Ghost Skills (unlock powers)"));
		}
		if (mc.player.getInventory().getArmorStack(2).getItem() == com.phantompowers.ModItems.HUNTER_SUIT) {
			lines.add(new Line(name(PhantomClient.SUIT), "suit on/off (hold: cutscene)"));
		}
		if (lines.isEmpty()) return;

		// A tidy list on the right edge: key chips on the left, what they do on the right
		TextRenderer tr = mc.textRenderer;
		int keyW = 0, textW = 0;
		for (Line l : lines) {
			keyW = Math.max(keyW, tr.getWidth(l.key()));
			textW = Math.max(textW, tr.getWidth(l.text()));
		}
		int lineH = 12;
		int sw = (int) (ctx.getScaledWindowWidth() / SCALE);
		int sh = (int) (ctx.getScaledWindowHeight() / SCALE);
		int total = keyW + 8 + 6 + textW;
		int x = sw - total - 10;
		int y = sh / 2 - lines.size() * lineH / 2;

		ctx.getMatrices().push();
		ctx.getMatrices().scale(SCALE, SCALE, 1f);
		ctx.fill(x - 6, y - 6, sw - 4, y + lines.size() * lineH + 3, 0x66000000);
		ctx.fill(x - 6, y - 6, x - 4, y + lines.size() * lineH + 3, 0xFF3FD978);
		for (Line l : lines) {
			int kw = tr.getWidth(l.key());
			ctx.fill(x, y - 2, x + keyW + 8, y + 9, 0xFF1E2A23);
			ctx.fill(x, y + 8, x + keyW + 8, y + 9, 0xFF0C120E);
			ctx.drawText(tr, l.key(), x + 4 + (keyW - kw) / 2, y, 0xFFFFE066, false);
			ctx.drawTextWithShadow(tr, l.text(), x + keyW + 14, y, 0xFFE4ECE7);
			y += lineH;
		}
		ctx.getMatrices().pop();
	}

	private static String name(KeyBinding k) {
		return k.getBoundKeyLocalizedText().getString().toUpperCase();
	}

	@SuppressWarnings("unused")
	private static final int SKILLS = GhostPowers.SKILL_COUNT;
}
