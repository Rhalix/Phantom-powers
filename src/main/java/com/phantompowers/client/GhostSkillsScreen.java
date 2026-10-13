package com.phantompowers.client;

import com.phantompowers.GhostPowers;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

/** Spend stat points to unlock ghost powers. Opened from the inventory tab or the K key. */
public class GhostSkillsScreen extends Screen {
	private static final int PANEL_W = 320;
	private static final int ROW_H = 21;
	private static final String[] ROMAN = {"I", "II", "III", "IV", "V"};

	private final Screen parent;
	private final ButtonWidget[] unlockButtons = new ButtonWidget[GhostPowers.SKILL_COUNT];
	private int left;
	private int top;

	public GhostSkillsScreen(Screen parent) {
		super(Text.literal("Ghost Skills"));
		this.parent = parent;
	}

	private int panelHeight() {
		return 46 + GhostPowers.SKILL_COUNT * ROW_H + 30;
	}

	@Override
	protected void init() {
		left = (width - PANEL_W) / 2;
		top = Math.max(4, (height - panelHeight()) / 2);
		for (int i = 0; i < GhostPowers.SKILL_COUNT; i++) {
			final int skill = i;
			unlockButtons[i] = addDrawableChild(ButtonWidget.builder(Text.literal("Unlock"),
							b -> PhantomClient.send(GhostPowers.A_UNLOCK + skill))
					.dimensions(left + PANEL_W - 76, top + 40 + i * ROW_H + 1, 68, 19)
					.build());
		}
		addDrawableChild(ButtonWidget.builder(Text.literal("Customize Suit"), b -> client.setScreen(new GhostSuitScreen(this)))
				.dimensions(width / 2 - 104, top + panelHeight() - 26, 100, 20)
				.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
				.dimensions(width / 2 + 4, top + panelHeight() - 26, 80, 20)
				.build());
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		ctx.fill(left, top, left + PANEL_W, top + panelHeight(), 0xE0101814);
		ctx.fill(left, top, left + PANEL_W, top + 2, 0xFF33FF77);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		KeyBinding[] keys = PhantomClient.skillKeys();
		for (int i = 0; i < GhostPowers.SKILL_COUNT; i++) {
			boolean has = ClientGhost.has(i);
			boolean afford = ClientGhost.points >= GhostPowers.COSTS[i];
			unlockButtons[i].active = ClientGhost.powers && !has && afford;
			unlockButtons[i].setMessage(Text.literal(has ? "Unlocked" : afford ? "Unlock" : GhostPowers.COSTS[i] + " pts"));
		}
		super.render(ctx, mouseX, mouseY, delta);

		ctx.drawCenteredTextWithShadow(textRenderer, "Ghost Skills", width / 2, top + 8, 0xFF66FF99);
		int lvl = ClientGhost.level;
		int hearts = (int) Math.min(20, 2 + (lvl - 1));
		String pts = ClientGhost.powers
				? "Ghost Level " + lvl + " (+" + hearts + " hearts, Strength " + ROMAN[Math.min(4, (lvl - 1) / 7)] + ")   Points: " + ClientGhost.points
				: "You don't have ghost powers yet.";
		ctx.drawCenteredTextWithShadow(textRenderer, pts, width / 2, top + 22, 0xFFFFEE55);

		for (int i = 0; i < GhostPowers.SKILL_COUNT; i++) {
			int y = top + 40 + i * ROW_H;
			boolean has = ClientGhost.has(i);
			int nameColor = has ? 0xFF66FF99 : 0xFFBBBBBB;
			String title = GhostPowers.NAMES[i] + "  [" + PhantomClient.keyName(keys[i]) + "]";
			if (!has) title += "  - " + GhostPowers.COSTS[i] + " pts";
			ctx.drawTextWithShadow(textRenderer, title, left + 10, y + 2, nameColor);
			ctx.getMatrices().push();
			ctx.getMatrices().translate(left + 10, y + 12.5f, 0);
			ctx.getMatrices().scale(0.75f, 0.75f, 1f);
			ctx.drawTextWithShadow(textRenderer, GhostPowers.DESCRIPTIONS[i], 0, 0, 0xFF8A9A90);
			ctx.getMatrices().pop();
		}
	}

	@Override
	public void close() {
		if (client != null) client.setScreen(parent);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
