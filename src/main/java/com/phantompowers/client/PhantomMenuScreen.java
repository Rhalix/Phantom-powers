package com.phantompowers.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** The Phantom Menu: story mode, party, power slots and ghost skills, all in one ghostly place. */
public class PhantomMenuScreen extends Screen {
	private int left, top, w = 220, h = 196;

	public PhantomMenuScreen() {
		super(Text.literal("Phantom Menu"));
	}

	@Override
	protected void init() {
		left = (width - w) / 2;
		top = (height - h) / 2;
		int bx = left + 30, bw = w - 60, y = top + 40;
		addDrawableChild(new GhostlyUi.GhostButton(bx, y, bw, 20, Text.literal("Story Mode"), b -> client.setScreen(new StoryScreen()), GhostlyUi.GREEN));
		addDrawableChild(new GhostlyUi.GhostButton(bx, y + 26, bw, 20, Text.literal("Party"), b -> client.setScreen(new PartyScreen(this)), GhostlyUi.CYAN));
		addDrawableChild(new GhostlyUi.GhostButton(bx, y + 52, bw, 20, Text.literal("Power Slots"), b -> client.setScreen(new SlotsScreen(this)), GhostlyUi.GREEN));
		var skills = addDrawableChild(new GhostlyUi.GhostButton(bx, y + 78, bw, 20, Text.literal("Ghost Skills"), b -> client.setScreen(new GhostSkillsScreen(this)), GhostlyUi.GREEN));
		skills.active = ClientGhost.powers;
		addDrawableChild(new GhostlyUi.GhostButton(bx, y + 112, bw, 20, Text.literal("Close"), b -> close(), 0x8899AA));
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		GhostlyUi.panel(ctx, left, top, w, h, GhostlyUi.GREEN);
		GhostlyUi.title(ctx, textRenderer, "PHANTOM MENU", left + w / 2, top + 12, GhostlyUi.GREEN);
		String story = StoryHud.path() == 0 ? "Story: not started" : "Story: " + com.phantompowers.Story.PATHS[StoryHud.path()];
		ctx.drawCenteredTextWithShadow(textRenderer, story, left + w / 2, top + h - 30, 0xFFA8C8B8);
		String party = ClientParty.members.size() > 1 ? "Party: " + ClientParty.members.size() + " players" : "Party: just you";
		ctx.drawCenteredTextWithShadow(textRenderer, party, left + w / 2, top + h - 18, 0xFFA8C8B8);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
