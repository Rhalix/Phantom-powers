package com.phantompowers.client;

import com.phantompowers.network.SlotsPayload;

/** What's in your Power Slots (client copy). */
public final class ClientSlots {
	public static int[] slots = {-1, -1, -1, -1};
	public static int[] gadgets = {0, 1, 2, 3};

	private ClientSlots() {}

	public static void update(SlotsPayload p) {
		if (p.slots().length == 4) slots = p.slots();
		if (p.gadgets().length == 4) gadgets = p.gadgets();
	}
}
