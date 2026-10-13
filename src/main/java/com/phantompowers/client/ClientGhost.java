package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.network.SyncPayload;

/** The local player's ghost status, as last told by the server. */
public final class ClientGhost {
	public static boolean powers;
	public static boolean transformed;
	public static boolean intangible;
	public static boolean possessing;
	public static boolean invisible;
	public static float energy;
	public static int points;
	public static int progress;
	public static int unlocked;
	public static int wailCooldown;
	public static int tornadoCooldown;
	public static int exhaust;
	public static int cutscene;
	public static boolean cutsceneToGhost;
	public static int level = 1;
	public static int cutsceneKind;

	private ClientGhost() {}

	public static void update(SyncPayload p) {
		powers = p.powers();
		transformed = p.transformed();
		intangible = p.intangible();
		possessing = p.possessing();
		invisible = p.invisible();
		energy = p.energy();
		points = p.points();
		progress = p.progress();
		unlocked = p.unlocked();
		wailCooldown = p.wailCooldown();
		tornadoCooldown = p.tornadoCooldown();
		exhaust = p.exhaust();
		cutscene = p.cutscene();
		cutsceneToGhost = p.cutsceneToGhost();
		level = p.level();
		cutsceneKind = p.cutsceneKind();
		holding = p.holding();
		frostCooldown = p.frostCooldown();
		hostAbility = p.hostAbility();
		hostId = p.hostId();
		suitVariant = p.suitVariant();
		GhostFlags.clientSelfIntangible = intangible;
		GhostFlags.clientSelfHolding = holding;
	}

	public static boolean holding;
	public static int frostCooldown;
	public static int hostAbility = -1;
	public static int hostId = -1;
	public static int suitVariant;

	public static void reset() {
		holding = false;
		hostAbility = -1;
		hostId = -1;
		GhostFlags.clientSelfHolding = false;
		powers = transformed = intangible = possessing = invisible = false;
		energy = 0;
		points = progress = unlocked = wailCooldown = tornadoCooldown = exhaust = cutscene = 0;
	}

	public static boolean has(int skill) {
		return (unlocked & (1 << skill)) != 0;
	}
}
