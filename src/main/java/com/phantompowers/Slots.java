package com.phantompowers;

import com.phantompowers.network.SlotsPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Power slots: instead of a key for every power, you put up to four powers in slots and press Power Slot 1-4.
 * Ghost hunters in their suit get four gadget slots on the same keys (wrist missiles, backpack cannons, cannon
 * auto/manual, radar).
 */
public final class Slots {
	public static final int COUNT = 4;
	public static final int G_MISSILES = 0, G_CANNONS = 1, G_CANNON_MODE = 2, G_RADAR = 3;
	public static final String[] GADGETS = {"Wrist Missiles", "Backpack Cannons", "Cannons: Auto / Manual", "Hunter Radar"};

	private Slots() {}

	/** Fill empty slots from the old-style loadout the first time, and keep the loadout matching the slots. */
	public static void ensure(GhostData d) {
		boolean empty = true;
		for (int s : d.slots) if (s >= 0) empty = false;
		if (empty && d.loadout != 0) {
			int k = 0;
			for (int i = 0; i < GhostPowers.SKILL_COUNT && k < COUNT; i++) if ((d.loadout & (1 << i)) != 0) d.slots[k++] = i;
		}
		int bits = 0;
		for (int s : d.slots) if (s >= 0 && d.has(s)) bits |= 1 << s;
		d.loadout = bits;
	}

	public static int slotOf(GhostData d, int skill) {
		for (int i = 0; i < COUNT; i++) if (d.slots[i] == skill) return i;
		return -1;
	}

	/** Put a power in a slot (skill -1 empties it). */
	public static void assign(ServerPlayerEntity p, GhostData d, int slot, int skill) {
		if (slot < 0 || slot >= COUNT) return;
		if (skill >= 0) {
			if (skill >= GhostPowers.SKILL_COUNT || !d.has(skill)) {
				GhostPowers.bar(p, "Unlock that power first (Ghost Skills).", Formatting.YELLOW);
				return;
			}
			int was = slotOf(d, skill);
			if (was >= 0) d.slots[was] = d.slots[slot];   // swap
		}
		d.slots[slot] = skill;
		ensure(d);
		GhostState.of(p.getServer()).markDirty();
		GhostPowers.syncNow(p);
	}

	public static void assignGadget(ServerPlayerEntity p, GhostData d, int slot, int gadget) {
		if (slot < 0 || slot >= COUNT || gadget < 0 || gadget >= GADGETS.length) return;
		for (int i = 0; i < COUNT; i++) if (d.gadgets[i] == gadget) d.gadgets[i] = d.gadgets[slot];
		d.gadgets[slot] = gadget;
		GhostState.of(p.getServer()).markDirty();
		GhostPowers.syncNow(p);
	}

	/** Equip from the Ghost Skills screen: into the first free slot (or take it out). */
	public static void toggle(ServerPlayerEntity p, GhostData d, int skill) {
		int at = slotOf(d, skill);
		if (at >= 0) {
			d.slots[at] = -1;
			GhostPowers.bar(p, GhostPowers.NAMES[skill] + " taken out of Power Slot " + (at + 1) + ".", Formatting.GRAY);
		} else if (!d.has(skill)) {
			GhostPowers.bar(p, "Unlock " + GhostPowers.NAMES[skill] + " first.", Formatting.YELLOW);
			return;
		} else {
			int free = -1;
			for (int i = 0; i < COUNT && free < 0; i++) if (d.slots[i] < 0) free = i;
			if (free < 0) {
				GhostPowers.bar(p, "All " + COUNT + " Power Slots are full. Take one out first (or swap in the Phantom Menu).", Formatting.YELLOW);
				return;
			}
			d.slots[free] = skill;
			p.sendMessage(Text.literal(GhostPowers.NAMES[skill] + " is now on Power Slot " + (free + 1) + ".").formatted(Formatting.GREEN), true);
		}
		ensure(d);
		GhostState.of(p.getServer()).markDirty();
		GhostPowers.syncNow(p);
	}

	/** A newly unlocked power goes into a free slot if there is one. */
	public static void autoAdd(GhostData d, int skill) {
		if (slotOf(d, skill) >= 0) return;
		for (int i = 0; i < COUNT; i++) if (d.slots[i] < 0) {
			d.slots[i] = skill;
			break;
		}
		ensure(d);
	}

	public static void send(ServerPlayerEntity p, GhostData d) {
		if (ServerPlayNetworking.canSend(p, SlotsPayload.ID)) ServerPlayNetworking.send(p, new SlotsPayload(d.slots.clone(), d.gadgets.clone()));
	}
}
