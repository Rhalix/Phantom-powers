package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.entity.AbstractGhostEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Ghost Hunter Suit visor: every ghost nearby (even invisible ones) shows up as a glowing
 * infrared outline, through walls too.
 */
public final class SuitVision {
	/** Infrared orange-red. */
	public static final int INFRARED = 0xFF4A1A;
	private static final double RANGE = 64;

	private SuitVision() {}

	/** Is the local player wearing the deployed suit (and human, not in ghost form)? */
	public static boolean active() {
		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity me = mc.player;
		if (me == null || GhostFlags.CLIENT_SUITED.isEmpty()) return false;
		return GhostFlags.CLIENT_SUITED.contains(me.getUuid()) && !GhostFlags.CLIENT_GHOSTS.contains(me.getUuid());
	}

	public static boolean isGhost(Entity e) {
		if (e instanceof AbstractGhostEntity) return true;
		return e instanceof PlayerEntity p && !p.isMainPlayer() && !GhostFlags.CLIENT_GHOSTS.isEmpty() && GhostFlags.CLIENT_GHOSTS.contains(p.getUuid());
	}

	/** Should this entity get the infrared outline for the local player? */
	public static boolean sees(Entity e) {
		if (!isGhost(e) || !e.isAlive() || !active()) return false;
		PlayerEntity me = MinecraftClient.getInstance().player;
		return me != null && e.squaredDistanceTo(me) < RANGE * RANGE;
	}

	/** Players who used Invisibility: nothing of them should show (unless a suit is looking). */
	public static boolean isInvisibleGhost(Entity e) {
		return e instanceof PlayerEntity p && !GhostFlags.CLIENT_INVISIBLE.isEmpty() && GhostFlags.CLIENT_INVISIBLE.contains(p.getUuid());
	}
}
