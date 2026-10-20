package com.phantompowers.client;

import com.phantompowers.GhostPowers;
import com.phantompowers.Slots;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Power Slots: pick which four powers go on your four slot keys (and, for ghost hunters, which suit gadget goes
 * on which key). Click a slot, then click what you want in it.
 */
public class SlotsScreen extends Screen {
	private final Screen parent;
	private int left, top, w, h;
	private int mode;       // 0 = ghost powers, 1 = hunter gadgets
	private int selected;

	public SlotsScreen(Screen parent) {
		super(Text.literal("Power Slots"));
		this.parent = parent;
		this.mode = ClientGhost.powers ? 0 : 1;
	}

	@Override
	protected void init() {
		w = Math.min(360, width - 20);
		h = Math.min(236, height - 16);
		left = (width - w) / 2;
		top = (height - h) / 2;
		build();
	}

	private void rebuild() {
		clearChildren();
		build();
	}

	private int accent() {
		return mode == 0 ? (ClientGhost.dark && ClientGhost.darkMode ? GhostlyUi.PINK : GhostlyUi.GREEN) : GhostlyUi.CYAN;
	}

	private void build() {
		int acc = accent();
		var g = addDrawableChild(new GhostlyUi.GhostButton(left + 12, top + 26, 110, 16, Text.literal("Ghost Powers"), b -> { mode = 0; rebuild(); }, GhostlyUi.GREEN));
		g.active = ClientGhost.powers && mode != 0;
		var hb = addDrawableChild(new GhostlyUi.GhostButton(left + 126, top + 26, 110, 16, Text.literal("Hunter Gadgets"), b -> { mode = 1; rebuild(); }, GhostlyUi.CYAN));
		hb.active = mode != 1;
		// the four slots
		int sw = (w - 24 - 18) / 4;
		for (int i = 0; i < Slots.COUNT; i++) {
			final int slot = i;
			String key = PhantomClient.keyName(PhantomClient.SLOT[i]);
			String what = mode == 0 ? (ClientSlots.slots[i] >= 0 ? GhostPowers.NAMES[ClientSlots.slots[i]] : "(empty)") : Slots.GADGETS[ClientSlots.gadgets[i]];
			var btn = addDrawableChild(new GhostlyUi.GhostButton(left + 12 + i * (sw + 6), top + 62, sw, 30,
					Text.literal("[" + key + "] " + textRenderer.trimToWidth(what, sw - 26)), b -> { selected = slot; rebuild(); }, i == selected ? 0xFFE680 : acc));
		}
		// choices for the selected slot
		int y = top + 112, x = left + 12, cw = (w - 24 - 8) / 3;
		int n = 0;
		if (mode == 0) {
			for (int s = 0; s < GhostPowers.SKILL_COUNT; s++) {
				if (!ClientGhost.has(s)) continue;
				final int skill = s;
				int bx = x + (n % 3) * (cw + 4), by = y + (n / 3) * 20;
				addDrawableChild(new GhostlyUi.GhostButton(bx, by, cw, 18, Text.literal(textRenderer.trimToWidth(GhostPowers.NAMES[s], cw - 6)),
						b -> MenuNet.send(2, selected * 64 + skill, "ghost"), GhostPowers.darkOnly(s) ? GhostlyUi.PINK : GhostlyUi.GREEN));
				n++;
			}
			addDrawableChild(new GhostlyUi.GhostButton(left + 12, top + h - 30, 80, 18, Text.literal("Empty slot"), b -> MenuNet.send(2, selected * 64 + 63, "ghost"), 0x8899AA));
		} else {
			for (int gd = 0; gd < Slots.GADGETS.length; gd++) {
				final int gadget = gd;
				int bx = x + (n % 2) * ((w - 28) / 2 + 4), by = y + (n / 2) * 22;
				addDrawableChild(new GhostlyUi.GhostButton(bx, by, (w - 28) / 2, 18, Text.literal(Slots.GADGETS[gd]),
						b -> MenuNet.send(2, selected * 64 + gadget, "hunter"), GhostlyUi.CYAN));
				n++;
			}
		}
		addDrawableChild(new GhostlyUi.GhostButton(left + w - 70, top + h - 30, 60, 18, Text.literal("Back"), b -> client.setScreen(parent), 0x8899AA));
	}

	@Override
	public void tick() {
		// keep the slot labels fresh when the server confirms a change
		if (lastSeen != java.util.Arrays.hashCode(ClientSlots.slots) * 31 + java.util.Arrays.hashCode(ClientSlots.gadgets)) {
			lastSeen = java.util.Arrays.hashCode(ClientSlots.slots) * 31 + java.util.Arrays.hashCode(ClientSlots.gadgets);
			rebuild();
		}
	}

	private int lastSeen;

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		int acc = accent();
		GhostlyUi.panel(ctx, left, top, w, h, acc);
		GhostlyUi.title(ctx, textRenderer, "POWER SLOTS", left + w / 2, top + 10, acc);
		ctx.drawTextWithShadow(textRenderer, mode == 0 ? "Your 4 power keys" : "Suit gadget keys (when your Hunter Suit is on)", left + 12, top + 50, 0xFFA8C8B8);
		ctx.drawTextWithShadow(textRenderer, "Put in slot " + (selected + 1) + ":", left + 12, top + 100, 0xFFFFE680);
		if (mode == 0 && !ClientGhost.powers) ctx.drawTextWithShadow(textRenderer, "You don't have ghost powers yet.", left + 12, top + 116, 0xFF7A9A8E);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
