package com.phantompowers.client;

import com.phantompowers.network.MenuActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

final class MenuNet {
	private MenuNet() {}

	static void send(int kind, int value, String text) {
		if (ClientPlayNetworking.canSend(MenuActionPayload.ID)) ClientPlayNetworking.send(new MenuActionPayload(kind, value, text == null ? "" : text));
	}
}
