package com.phantompowers.world;

import com.phantompowers.ModBlocks;
import com.phantompowers.PhantomMod;
import com.phantompowers.PortalLogic;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Every door in the Ghost Zone opens into its own room. */
public final class GhostRooms {
	public static final RegistryKey<World> ROOMS = RegistryKey.of(RegistryKeys.WORLD, PhantomMod.id("ghost_rooms"));
	public static final int SPACING = 64;
	public static final int BASE_Y = 64;
	private static final Map<UUID, Integer> LAST_USE = new HashMap<>();

	private GhostRooms() {}

	public static boolean isDoorWorld(World w) {
		RegistryKey<World> k = w.getRegistryKey();
		return k.equals(PortalLogic.GHOST_ZONE) || k.equals(ROOMS);
	}

	public static BlockPos origin(int index) {
		return new BlockPos(index * SPACING, BASE_Y, 0);
	}

	public static void queueUse(ServerPlayerEntity p, ServerWorld w, BlockPos pos, BlockState state) {
		BlockPos lower = state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos.toImmutable();
		PortalLogic.later(() -> useDoor(p, w, lower));
	}

	private static void useDoor(ServerPlayerEntity p, ServerWorld w, BlockPos lower) {
		if (p.isRemoved() || !p.isAlive() || p.getWorld() != w) return;
		MinecraftServer server = p.getServer();
		int now = server.getTicks();
		Integer last = LAST_USE.get(p.getUuid());
		if (last != null && now - last < 20) return;
		LAST_USE.put(p.getUuid(), now);

		ServerWorld rooms = server.getWorld(ROOMS);
		if (rooms == null) {
			p.sendMessage(Text.literal("The door won't budge... (rooms dimension missing)").formatted(Formatting.RED), true);
			return;
		}
		RoomState rs = RoomState.of(server);

		if (w.getRegistryKey().equals(ROOMS)) {
			leave(p, rs, lower);
			return;
		}

		RoomState.Room room = rs.roomFor(w.getRegistryKey().getValue() + "|" + lower.asLong());
		room.returnDim = w.getRegistryKey().getValue().toString();
		room.returnPos = p.getBlockPos();
		if (!room.built) {
			RoomBuilder.Result r = RoomBuilder.build(rooms, room.index, origin(room.index));
			room.built = true;
			room.name = r.name();
			room.ex = r.entry().x;
			room.ey = r.entry().y;
			room.ez = r.entry().z;
			room.yaw = r.yaw();
		}
		rs.markDirty();
		p.teleport(rooms, room.ex, room.ey, room.ez, room.yaw, 0f);
		rooms.playSound(null, room.ex, room.ey, room.ez, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.BLOCKS, 1.0f, 0.6f);
		rooms.playSound(null, room.ex, room.ey, room.ez, SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.AMBIENT, 1.0f, 0.5f);
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(room.name).formatted(Formatting.GREEN)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("Use the door behind you to leave").formatted(Formatting.GRAY)));
	}

	private static void leave(ServerPlayerEntity p, RoomState rs, BlockPos door) {
		int index = Math.floorDiv(door.getX() + SPACING / 2, SPACING);
		RoomState.Room room = rs.byIndex(index);
		MinecraftServer server = p.getServer();
		ServerWorld dest = null;
		if (room != null && room.returnDim != null) {
			dest = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(room.returnDim)));
		}
		if (dest == null || room.returnPos == null) {
			ServerWorld zone = server.getWorld(PortalLogic.GHOST_ZONE);
			if (zone != null) PortalLogic.sendToGhostZone(p, zone);
			return;
		}
		BlockPos back = room.returnPos;
		ensurePlatform(dest, back);
		p.teleport(dest, back.getX() + 0.5, back.getY(), back.getZ() + 0.5, p.getYaw() + 180f, 0f);
		dest.playSound(null, back.getX(), back.getY(), back.getZ(), SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.BLOCKS, 1.0f, 0.6f);
	}

	/** Make sure there's something to stand on when you come back out (doors float over the void). */
	private static void ensurePlatform(ServerWorld w, BlockPos feet) {
		if (!w.getBlockState(feet.down()).getCollisionShape(w, feet.down()).isEmpty()) return;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos b = feet.add(dx, -1, dz);
				if (w.getBlockState(b).isAir()) w.setBlockState(b, ModBlocks.ECTO_STONE.getDefaultState());
			}
		}
	}

	/** Builds one room of every theme far away, for testing. Returns how many were built. */
	public static int testBuildAll(ServerWorld rooms) {
		int built = 0;
		for (int i = 0; i < RoomBuilder.THEME_COUNT; i++) {
			RoomBuilder.Result r = RoomBuilder.build(rooms, 100000 + i, origin(100000 + i));
			if (r != null) built++;
		}
		return built;
	}
}
