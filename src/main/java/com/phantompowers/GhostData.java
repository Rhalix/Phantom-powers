package com.phantompowers;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

/** Everything the mod remembers about one player. */
public class GhostData {
	// Saved
	public boolean powers;
	public boolean pendingPowers;
	public boolean transformed;
	public int points;
	public int totalPoints;
	/** Essences eaten (bitmask), lost on death. */
	public int eaten;
	public int hitCount;
	public int formTicks;
	public int unlocked;
	public float energy = GhostPowers.MAX_ENERGY;
	public String respawnDim;
	public BlockPos respawnPos;
	public String portalDim;
	/** Where to send you back when you break out of a Thermos. */
	public String thermosDim;
	/** Ecto Fists switched on (not saved). */
	public boolean fists;
	/** Ecto Shield: key held / shield actually up (not saved). */
	public boolean shieldHeld, shieldUp;
	/** Chosen ghost suit look (saved). */
	public int suitVariant;
	/** Suit colors per part: body, stripes, gloves, boots, trail glow, flags (bit 0 = glowing stripes). Saved. */
	public int[] suitColors = SuitColors.CLASSIC.clone();
	/** Powers equipped in the loadout (bitmask, at most LOADOUT_MAX). Saved. */
	public int loadout;
	public boolean loadoutInit;
	/** The dark half-ghost: unlocked after the prototype portal's ecto acne wears off. Saved. */
	public boolean dark;
	public net.minecraft.util.math.Vec3d portalCenter;
	public boolean missileAim;
	/** Use the dark form when going ghost (can be switched in Ghost Skills). Saved. */
	public boolean darkMode;
	/** Ticks of ecto acne left. Saved. */
	public int acneTicks;
	// dark powers (not saved)
	public boolean spinning;
	public Entity stingTarget;
	public int stingTicks, absorbTicks, constructCooldown, stingCooldown, absorbCooldown, constructKind;
	/** Hunter backpack cannons out (not saved). */
	public boolean cannons;
	public int missileCooldown, cannonCooldown;
	/** Which Thermos cell this player is locked in (saved). */
	public BlockPos thermosCell;
	public BlockPos thermosPos;
	public BlockPos portalPos;

	// Not saved (reset on restart)
	public boolean intangible;
	public boolean invisible;
	public Entity holding;
	public boolean heldPrevAi;
	public int cutscene;
	public int cutsceneKind;
	public int wailTicks;
	public boolean wailHeld;
	public boolean holdMadeIntangible;
	public boolean suitOn;
	public boolean cutsceneToGhost;
	public net.minecraft.util.math.Vec3d cutsceneAnchor;
	public Entity possessing;
	public boolean prevAiDisabled;
	public int possessTicks;
	public int blastCooldown;
	public int wailCooldown;
	public int tornadoCooldown;
	public int frostCooldown;
	public int frostTicks;
	public int hostAbilityCooldown;
	public boolean frostHeld;
	public int exhaust;
	public int syncTimer;

	public boolean equipped(int skill) {
		return (loadout & (1 << skill)) != 0;
	}

	public boolean has(int skill) {
		return (unlocked & (1 << skill)) != 0;
	}

	public NbtCompound toNbt() {
		NbtCompound n = new NbtCompound();
		n.putBoolean("powers", powers);
		n.putBoolean("pending", pendingPowers);
		n.putInt("points", points);
		n.putInt("totalPoints", totalPoints);
		n.putInt("eaten", eaten);
		n.putInt("formTicks", formTicks);
		n.putInt("unlocked", unlocked);
		n.putFloat("energy", energy);
		n.putInt("suitVariant", suitVariant);
		n.putIntArray("suitColors", suitColors);
		n.putInt("loadout", loadout);
		n.putBoolean("loadoutInit", loadoutInit);
		n.putBoolean("dark", dark);
		n.putBoolean("darkMode", darkMode);
		n.putInt("acne", acneTicks);
		if (respawnDim != null && respawnPos != null) {
			n.putString("respawnDim", respawnDim);
			n.putLong("respawnPos", respawnPos.asLong());
		}
		if (portalDim != null && portalPos != null) {
			n.putString("portalDim", portalDim);
			n.putLong("portalPos", portalPos.asLong());
		}
		if (thermosDim != null && thermosPos != null) {
			n.putString("thermosDim", thermosDim);
			n.putLong("thermosPos", thermosPos.asLong());
			if (thermosCell != null) n.putLong("thermosCell", thermosCell.asLong());
		}
		return n;
	}

	public static GhostData fromNbt(NbtCompound n) {
		GhostData d = new GhostData();
		d.powers = n.getBoolean("powers");
		d.pendingPowers = n.getBoolean("pending");
		d.points = n.getInt("points");
		d.totalPoints = n.getInt("totalPoints");
		d.eaten = n.getInt("eaten");
		d.formTicks = n.getInt("formTicks");
		d.unlocked = n.getInt("unlocked");
		d.suitVariant = n.getInt("suitVariant");
		int[] sc = n.getIntArray("suitColors");
		if (sc.length == SuitColors.PARTS) d.suitColors = sc;
		d.loadout = n.getInt("loadout");
		d.loadoutInit = n.getBoolean("loadoutInit");
		d.dark = n.getBoolean("dark");
		d.darkMode = n.getBoolean("darkMode");
		d.acneTicks = n.getInt("acne");
		d.energy = n.contains("energy") ? n.getFloat("energy") : GhostPowers.MAX_ENERGY;
		if (n.contains("respawnDim")) {
			d.respawnDim = n.getString("respawnDim");
			d.respawnPos = BlockPos.fromLong(n.getLong("respawnPos"));
		}
		if (n.contains("portalDim")) {
			d.portalDim = n.getString("portalDim");
			d.portalPos = BlockPos.fromLong(n.getLong("portalPos"));
		}
		if (n.contains("thermosDim")) {
			d.thermosDim = n.getString("thermosDim");
			d.thermosPos = BlockPos.fromLong(n.getLong("thermosPos"));
			if (n.contains("thermosCell")) d.thermosCell = BlockPos.fromLong(n.getLong("thermosCell"));
		}
		return d;
	}
}
