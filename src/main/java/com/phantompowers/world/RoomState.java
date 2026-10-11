package com.phantompowers.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Remembers which Ghost Zone door leads to which room. */
public class RoomState extends PersistentState {
	public static final PersistentState.Type<RoomState> TYPE =
			new PersistentState.Type<>(RoomState::new, RoomState::fromNbt, null);

	public static final class Room {
		public int index;
		public boolean built;
		public String name = "";
		public String returnDim;
		public BlockPos returnPos;
		public double ex, ey, ez;
		public float yaw;
	}

	private final Map<String, Integer> doors = new HashMap<>();
	private final java.util.Set<Long> prisons = new java.util.HashSet<>();

	public boolean prisonBuilt(long key) {
		return prisons.contains(key);
	}

	public void markPrison(long key) {
		prisons.add(key);
		markDirty();
	}
	private final List<Room> rooms = new ArrayList<>();

	public static RoomState of(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "phantom_rooms");
	}

	public Room roomFor(String doorKey) {
		Integer i = doors.get(doorKey);
		if (i == null) {
			i = rooms.size();
			doors.put(doorKey, i);
			Room r = new Room();
			r.index = i;
			rooms.add(r);
			markDirty();
		}
		return rooms.get(i);
	}

	public Room byIndex(int i) {
		return i >= 0 && i < rooms.size() ? rooms.get(i) : null;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		NbtCompound d = new NbtCompound();
		doors.forEach(d::putInt);
		nbt.put("doors", d);
		NbtList list = new NbtList();
		for (Room r : rooms) {
			NbtCompound n = new NbtCompound();
			n.putInt("index", r.index);
			n.putBoolean("built", r.built);
			n.putString("name", r.name);
			if (r.returnDim != null && r.returnPos != null) {
				n.putString("returnDim", r.returnDim);
				n.putLong("returnPos", r.returnPos.asLong());
			}
			n.putDouble("ex", r.ex);
			n.putDouble("ey", r.ey);
			n.putDouble("ez", r.ez);
			n.putFloat("yaw", r.yaw);
			list.add(n);
		}
		nbt.put("rooms", list);
		nbt.putLongArray("prisons", prisons.stream().mapToLong(Long::longValue).toArray());
		return nbt;
	}

	public static RoomState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		RoomState s = new RoomState();
		NbtCompound d = nbt.getCompound("doors");
		for (String k : d.getKeys()) s.doors.put(k, d.getInt(k));
		NbtList list = nbt.getList("rooms", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			NbtCompound n = list.getCompound(i);
			Room r = new Room();
			r.index = n.getInt("index");
			r.built = n.getBoolean("built");
			r.name = n.getString("name");
			if (n.contains("returnDim")) {
				r.returnDim = n.getString("returnDim");
				r.returnPos = BlockPos.fromLong(n.getLong("returnPos"));
			}
			r.ex = n.getDouble("ex");
			r.ey = n.getDouble("ey");
			r.ez = n.getDouble("ez");
			r.yaw = n.getFloat("yaw");
			s.rooms.add(r);
		}
		for (long k : nbt.getLongArray("prisons")) s.prisons.add(k);
		return s;
	}
}
