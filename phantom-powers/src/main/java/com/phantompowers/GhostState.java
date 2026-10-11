package com.phantompowers;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Saves ghost data for every player inside the world save. */
public class GhostState extends PersistentState {
	public static final PersistentState.Type<GhostState> TYPE =
			new PersistentState.Type<>(GhostState::new, GhostState::fromNbt, null);

	private final Map<UUID, GhostData> players = new HashMap<>();

	public static GhostState of(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "phantom_ghosts");
	}

	public GhostData data(UUID id) {
		return players.computeIfAbsent(id, k -> new GhostData());
	}

	public void reset(UUID id) {
		players.remove(id);
		markDirty();
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		NbtList list = new NbtList();
		players.forEach((id, data) -> {
			NbtCompound entry = data.toNbt();
			entry.putUuid("id", id);
			list.add(entry);
		});
		nbt.put("players", list);
		return nbt;
	}

	public static GhostState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		GhostState state = new GhostState();
		NbtList list = nbt.getList("players", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			NbtCompound entry = list.getCompound(i);
			if (entry.containsUuid("id")) {
				state.players.put(entry.getUuid("id"), GhostData.fromNbt(entry));
			}
		}
		return state;
	}
}
