package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.network.SyncPayload;

/** The local player's ghost status, as last told by the server. */
public final class ClientGhost {
	public static boolean powers;
	public static boolean transformed;
	public static boolean intangible;
	public static boolean possessing;
	public static float energy;
	public static int points;
	public static int progress;
	public static int unlocked;
	public static int wailCooldown;
	public static int tornadoCooldown;
	public static int exhaust;

	private ClientGhost() {}

	public static void update(SyncPayload p) {
		powers = p.powers();
		transformed = p.transformed();
		intangible = p.intangible();
		possessing = p.possessing();
		energy = p.energy();
		points = p.points();
		progress = p.progress();
		unlocked = p.unlocked();
		wailCooldown = p.wailCooldown();
		tornadoCooldown = p.tornadoCooldown();
		exhaust = p.exhaust();
		GhostFlags.clientSelfIntangible = intangible;
	}

	public static void reset() {
		powers = transformed = intangible = possessing = false;
		energy = 0;
		points = progress = unlocked = wailCooldown = tornadoCooldown = exhaust = 0;
	}

	public static boolean has(int skill) {
		return (unlocked & (1 << skill)) != 0;
	}
}
