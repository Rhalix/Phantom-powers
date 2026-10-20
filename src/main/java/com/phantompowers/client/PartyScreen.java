package com.phantompowers.client;

import com.phantompowers.Party;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Party menu: invite players, accept invites, kick or leave. Party members can't hurt each other. */
public class PartyScreen extends Screen {
	private final Screen parent;
	private int left, top, w, h;
	private int page;

	public PartyScreen(Screen parent) {
		super(Text.literal("Party"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		w = Math.min(340, width - 20);
		h = Math.min(230, height - 20);
		left = (width - w) / 2;
		top = (height - h) / 2;
		MenuNet.send(0, Party.REFRESH, "");
		build();
	}

	void refresh() {
		clearChildren();
		build();
	}

	private void build() {
		String me = MinecraftClient.getInstance().getSession().getUsername();
		boolean leader = me.equals(ClientParty.leader);
		int col = left + 12, colW = w / 2 - 18, y = top + 46;
		// your party
		for (String m : ClientParty.members) {
			if (leader && !m.equals(me)) {
				addDrawableChild(new GhostlyUi.GhostButton(col + colW - 40, y, 40, 14, Text.literal("Kick"), b -> MenuNet.send(0, Party.KICK, m), GhostlyUi.PINK));
			}
			y += 18;
		}
		if (ClientParty.members.size() > 1) {
			addDrawableChild(new GhostlyUi.GhostButton(col, top + h - 74, colW, 18, Text.literal("Leave party"), b -> MenuNet.send(0, Party.LEAVE, ""), GhostlyUi.PINK));
		}
		// invite
		int col2 = left + w / 2 + 6;
		int perPage = Math.max(1, (h - 130) / 18);
		int start = page * perPage;
		y = top + 46;
		for (int i = start; i < Math.min(ClientParty.others.size(), start + perPage); i++) {
			String o = ClientParty.others.get(i);
			addDrawableChild(new GhostlyUi.GhostButton(col2 + colW - 46, y, 46, 14, Text.literal("Invite"), b -> MenuNet.send(0, Party.INVITE, o), GhostlyUi.CYAN));
			y += 18;
		}
		if (ClientParty.others.size() > perPage) {
			addDrawableChild(new GhostlyUi.GhostButton(col2, top + h - 74, 30, 16, Text.literal("<"), b -> { page = Math.max(0, page - 1); refresh(); }, GhostlyUi.CYAN));
			addDrawableChild(new GhostlyUi.GhostButton(col2 + 34, top + h - 74, 30, 16, Text.literal(">"), b -> {
				page = Math.min((ClientParty.others.size() - 1) / perPage, page + 1);
				refresh();
			}, GhostlyUi.CYAN));
		}
		// pending invite
		if (!ClientParty.inviteFrom.isEmpty()) {
			addDrawableChild(new GhostlyUi.GhostButton(left + w / 2 - 92, top + h - 30, 60, 18, Text.literal("Accept"), b -> MenuNet.send(0, Party.ACCEPT, ""), GhostlyUi.GREEN));
			addDrawableChild(new GhostlyUi.GhostButton(left + w / 2 - 28, top + h - 30, 60, 18, Text.literal("Decline"), b -> MenuNet.send(0, Party.DECLINE, ""), GhostlyUi.PINK));
		}
		addDrawableChild(new GhostlyUi.GhostButton(left + w - 70, top + h - 30, 60, 18, Text.literal("Back"), b -> client.setScreen(parent), 0x8899AA));
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		GhostlyUi.panel(ctx, left, top, w, h, GhostlyUi.CYAN);
		GhostlyUi.title(ctx, textRenderer, "PARTY", left + w / 2, top + 10, GhostlyUi.CYAN);
		ctx.drawCenteredTextWithShadow(textRenderer, "Party members can't hurt each other", left + w / 2, top + 22, 0xFF8AB4C4);
		int col = left + 12, col2 = left + w / 2 + 6, colW = w / 2 - 18;
		ctx.drawTextWithShadow(textRenderer, "Your party", col, top + 34, 0xFF5AE1FF);
		ctx.drawTextWithShadow(textRenderer, "Online players", col2, top + 34, 0xFF5AE1FF);
		ctx.fill(left + w / 2, top + 34, left + w / 2 + 1, top + h - 80, 0x445AE1FF);
		int y = top + 46;
		for (String m : ClientParty.members) {
			boolean lead = m.equals(ClientParty.leader);
			ctx.drawTextWithShadow(textRenderer, (lead ? "★ " : "  ") + m, col, y + 3, lead ? 0xFFFFE680 : 0xFFE0F0EA);
			y += 18;
		}
		if (ClientParty.members.size() <= 1) ctx.drawTextWithShadow(textRenderer, "Just you. Invite someone!", col, top + 64, 0xFF7A9A8E);
		y = top + 46;
		int perPage = Math.max(1, (h - 130) / 18);
		for (int i = page * perPage; i < Math.min(ClientParty.others.size(), page * perPage + perPage); i++) {
			String o = ClientParty.others.get(i);
			String shown = textRenderer.trimToWidth(o, colW - 52);
			ctx.drawTextWithShadow(textRenderer, shown, col2, y + 3, 0xFFE0F0EA);
			y += 18;
		}
		if (ClientParty.others.isEmpty()) ctx.drawTextWithShadow(textRenderer, "Nobody else is online", col2, top + 46, 0xFF7A9A8E);
		if (!ClientParty.inviteFrom.isEmpty()) {
			ctx.drawCenteredTextWithShadow(textRenderer, ClientParty.inviteFrom + " invited you to their party!", left + w / 2, top + h - 44, 0xFFFFE680);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
