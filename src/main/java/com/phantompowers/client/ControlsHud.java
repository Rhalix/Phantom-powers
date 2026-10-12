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

/** A small "how to use" guide in the bottom-right corner. */
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
					"Frost Breath (hold)"};
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

		TextRenderer tr = mc.textRenderer;
		int width = 0;
		for (Line l : lines) width = Math.max(width, tr.getWidth(l.key()) + 6 + tr.getWidth(l.text()));
		int lineH = 10;
		int sw = (int) (ctx.getScaledWindowWidth() / SCALE);
		int sh = (int) (ctx.getScaledWindowHeight() / SCALE);
		int x = sw - width - 8;
		int y = sh - lines.size() * lineH - 8;

		ctx.getMatrices().push();
		ctx.getMatrices().scale(SCALE, SCALE, 1f);
		ctx.fill(x - 4, y - 4, sw - 3, sh - 4, 0x70000000);
		for (Line l : lines) {
			ctx.drawTextWithShadow(tr, l.key(), x, y, 0xFFFFEE55);
			ctx.drawTextWithShadow(tr, l.text(), x + tr.getWidth(l.key()) + 6, y, 0xFFE0E0E0);
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
