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
	/** Players currently inside a possessed body (server side): hostile mobs leave them alone. */
	public static final Set<UUID> POSSESSORS = new HashSet<>();
	/** Players wearing the deployed Ghost Hunter Suit, as told to this client. */
	public static final Set<UUID> CLIENT_SUITED = java.util.concurrent.ConcurrentHashMap.newKeySet();
	public static final net.minecraft.util.Identifier GHOST_SKIN = net.minecraft.util.Identifier.of("phantom", "textures/entity/ghost_form.png");
	public static final net.minecraft.util.Identifier SUIT_SKIN = net.minecraft.util.Identifier.of("phantom", "textures/entity/hunter_suit.png");

	/** The skin to draw this player with instead of their own, or null. */
	public static net.minecraft.util.Identifier clientSkinOverride(Entity e) {
		if (!(e instanceof PlayerEntity)) return null;
		UUID id = e.getUuid();
		if (!CLIENT_SUITED.isEmpty() && CLIENT_SUITED.contains(id) && (CLIENT_GHOSTS.isEmpty() || !CLIENT_GHOSTS.contains(id))) return SUIT_SKIN;
		return null;
	}

	/** Players in ghost form, as told to this client (for the ghost look). */
	public static final Set<UUID> CLIENT_GHOSTS = java.util.concurrent.ConcurrentHashMap.newKeySet();
	/** Invisible ghosts (server side). */
	public static final Set<UUID> INVISIBLE = new HashSet<>();
	/** Invisible ghosts, as told to this client. Read from the render thread. */
	public static final Set<UUID> CLIENT_INVISIBLE = java.util.concurrent.ConcurrentHashMap.newKeySet();

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

	/** Possessed hosts: float with no collision while the ghost steers them. */
	public static boolean isHost(Entity e) {
		World w = e.getWorld();
		return w != null && !w.isClient && !HOSTS.isEmpty() && HOSTS.contains(e.getUuid());
	}

	/** Should this entity get the glowing see-through ghost look on this client? */
	public static boolean clientGhostLook(Entity e) {
		return e instanceof PlayerEntity && !CLIENT_GHOSTS.isEmpty() && CLIENT_GHOSTS.contains(e.getUuid());
	}

	public static void clearAll() {
		SELF.clear();
		TEMP.clear();
		HOSTS.clear();
		INVISIBLE.clear();
		POSSESSORS.clear();
	}
}
