package com.phantompowers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Who is currently phasing through things. Read by the mixins every tick, so it must be cheap. */
public final class GhostFlags {
	/** Players who turned themselves intangible (server side). */
	public static final Set<UUID> SELF = new HashSet<>();
	/** Entities a ghost turned intangible for a short time (server side). */
	public static final Map<UUID, TempPhase> TEMP = new HashMap<>();
	/** Mobs/players currently being possessed: they float and can't push or be pushed (server side). */
	public static final Set<UUID> HOSTS = new HashSet<>();

	/** Set on the client from the sync packet: is the local player intangible? */
	public static volatile boolean clientSelfIntangible;

	public static final class TempPhase {
		public final Entity entity;
		public int ticks;

		public TempPhase(Entity entity, int ticks) {
			this.entity = entity;
			this.ticks = ticks;
		}
	}

	private GhostFlags() {}

	/** Intangible = walks through blocks, takes no damage. */
	public static boolean isIntangible(Entity e) {
		World w = e.getWorld();
		if (w == null) return false;
		if (w.isClient) {
			return clientSelfIntangible && e instanceof PlayerEntity p && p.isMainPlayer();
		}
		if (SELF.isEmpty() && TEMP.isEmpty()) return false;
		UUID id = e.getUuid();
		return SELF.contains(id) || TEMP.containsKey(id);
	}

	/** Floats with no collision: intangible things and possessed hosts. */
	public static boolean isPhasing(Entity e) {
		if (isIntangible(e)) return true;
		World w = e.getWorld();
		return w != null && !w.isClient && !HOSTS.isEmpty() && HOSTS.contains(e.getUuid());
	}

	public static void clearAll() {
		SELF.clear();
		TEMP.clear();
		HOSTS.clear();
	}
}
