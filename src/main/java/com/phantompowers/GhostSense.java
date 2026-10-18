package com.phantompowers;

import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.network.GhostSensePayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Your ghost sense goes off (a wisp of icy blue breath) when a ghost comes close (ghosts, ghost bosses and other half-ghosts in ghost form only). */
public final class GhostSense {
	private static final double RANGE = 16;
	private static final int COOLDOWN = 160;
	private static final Map<UUID, Set<Integer>> SEEN = new HashMap<>();
	private static final Map<UUID, Integer> LAST = new HashMap<>();

	private GhostSense() {}

	public static void tick(MinecraftServer server) {
		if (server.getTicks() % 10 != 0) return;
		GhostState st = GhostState.of(server);
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			GhostData d = st.data(p.getUuid());
			if (!d.powers || !p.isAlive() || p.isSpectator()) {
				SEEN.remove(p.getUuid());
				continue;
			}
			List<Entity> near = p.getServerWorld().getOtherEntities(p, p.getBoundingBox().expand(RANGE),
					e -> e.isAlive() && e != d.possessing && isGhost(st, p, e));
			Set<Integer> ids = new HashSet<>();
			for (Entity e : near) ids.add(e.getId());
			Set<Integer> prev = SEEN.getOrDefault(p.getUuid(), Set.of());
			boolean newcomer = false;
			for (Integer id : ids) if (!prev.contains(id)) { newcomer = true; break; }
			SEEN.put(p.getUuid(), ids);
			if (!newcomer) continue;
			int now = server.getTicks();
			Integer last = LAST.get(p.getUuid());
			if (last != null && now - last < COOLDOWN) continue;
			LAST.put(p.getUuid(), now);
			trigger(p);
		}
	}

	/** Only real ghosts set it off: ghosts, ghost bosses, and other half-ghosts in ghost form (not zombies, creepers and the like). */
	private static boolean isGhost(GhostState st, ServerPlayerEntity self, Entity e) {
		if (e instanceof com.phantompowers.entity.GhostCloneEntity clone && clone.owner().map(self.getUuid()::equals).orElse(false)) return false;
		if (e instanceof AbstractGhostEntity || FrostLogic.isBoss(e)) return true;
		return e instanceof ServerPlayerEntity other && st.data(other.getUuid()).transformed;
	}

	public static void trigger(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		GhostSensePayload payload = new GhostSensePayload(p.getId());
		for (ServerPlayerEntity viewer : PlayerLookup.around(w, p.getPos(), 48)) {
			if (ServerPlayNetworking.canSend(viewer, GhostSensePayload.ID)) ServerPlayNetworking.send(viewer, payload);
		}
		w.playSound(null, p.getX(), p.getEyeY(), p.getZ(), ModSounds.GHOST_SENSE, SoundCategory.PLAYERS, 0.9f, 1.0f);
	}

	public static void forget(UUID id) {
		SEEN.remove(id);
		LAST.remove(id);
	}
}
