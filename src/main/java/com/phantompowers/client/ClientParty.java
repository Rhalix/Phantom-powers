package com.phantompowers.client;

import com.phantompowers.network.PartyPayload;

import java.util.List;

/** Your party (client copy). */
public final class ClientParty {
	public static List<String> members = List.of();
	public static List<String> others = List.of();
	public static String leader = "", inviteFrom = "";

	private ClientParty() {}

	public static void update(PartyPayload p) {
		members = p.members();
		others = p.others();
		leader = p.leader();
		inviteFrom = p.inviteFrom();
		if (net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof PartyScreen s) s.refresh();
	}
}
