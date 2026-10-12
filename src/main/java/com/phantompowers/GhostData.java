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
		}
		return d;
	}
}
