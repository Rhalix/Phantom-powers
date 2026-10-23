package com.phantompowers.world;

import com.phantompowers.ModBlocks;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Amity Park looks after itself: anything broken in the town (by players, explosions or fire) grows back ten seconds
 * later. Blocks players put down themselves are theirs, so those don't come back. Town blocks broken by a player drop
 * nothing (otherwise you could farm the town forever), and chests come back empty.
 */
public final class TownRepair {
	public static final int DELAY = 200;   // ticks (10 seconds)

	private record Entry(BlockState state, NbtCompound nbt, long due) {}

	private static final Long2ObjectOpenHashMap<Entry> PENDING = new Long2ObjectOpenHashMap<>();
	private static long now;

	private TownRepair() {}

	private static boolean watching(World world, BlockPos pos) {
		if (!(world instanceof ServerWorld w) || w != w.getServer().getOverworld()) return false;
		if (AmityPark.building()) return false;
		return AmityPark.inTown(AmityPark.state(w.getServer()), pos);
	}

	/** Remember what's here (and what's leaning on it) so it can grow back. */
	public static void record(World world, BlockPos pos) {
		if (!watching(world, pos)) return;
		AmityPark.State st = AmityPark.state(world.getServer());
		if (st.placed.contains(pos.asLong())) return;
		remember(world, pos);
		for (Direction d : Direction.values()) {
			BlockPos n = pos.offset(d);
			if (!st.placed.contains(n.asLong()) && AmityPark.inTown(st, n)) remember(world, n);
		}
	}

	private static void remember(World world, BlockPos pos) {
		long key = pos.asLong();
		if (PENDING.containsKey(key)) return;   // keep what was there originally
		BlockState s = world.getBlockState(pos);
		if (s.isAir() || s.isOf(ModBlocks.GHOST_PORTAL) || s.isIn(net.minecraft.registry.tag.BlockTags.FIRE)) return;
		NbtCompound nbt = null;
		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof SignBlockEntity) nbt = be.createNbt(world.getRegistryManager());
		PENDING.put(key, new Entry(s, nbt, now + DELAY));
	}

	/** A player is about to break a block. Returns false if we broke it for them (town blocks drop nothing). */
	public static boolean beforeBreak(World world, PlayerEntity player, BlockPos pos, BlockState state) {
		if (!(world instanceof ServerWorld w) || w != w.getServer().getOverworld()) return true;
		AmityPark.State st = AmityPark.state(w.getServer());
		if (!st.built || !AmityPark.inTown(st, pos)) return true;
		if (st.placed.remove(pos.asLong()) || placedPartner(world, st, pos, state)) {   // their own block: break it normally
			st.markDirty();
			return true;
		}
		if (AmityPark.building() || state.isOf(ModBlocks.GHOST_PORTAL)) return true;
		record(world, pos);
		w.breakBlock(pos, false, player);
		return false;
	}

	/** The other half of a door, bed or tall cupboard that the player placed. */
	private static boolean placedPartner(World world, AmityPark.State st, BlockPos pos, BlockState state) {
		for (Direction d : Direction.values()) {
			BlockPos n = pos.offset(d);
			if (st.placed.contains(n.asLong()) && world.getBlockState(n).isOf(state.getBlock())) {
				st.placed.remove(n.asLong());
				return true;
			}
		}
		return false;
	}

	/** A player placed a block in town: it's theirs. */
	public static void onPlaced(World world, BlockPos pos) {
		if (!(world instanceof ServerWorld w) || w != w.getServer().getOverworld()) return;
		AmityPark.State st = AmityPark.state(w.getServer());
		if (!AmityPark.inTown(st, pos)) return;
		st.placed.add(pos.asLong());
		st.markDirty();
	}

	public static void tick(MinecraftServer server) {
		now++;
		if (PENDING.isEmpty() || now % 10 != 0) return;
		ServerWorld w = server.getOverworld();
		List<Long> due = new ArrayList<>();
		for (var e : PENDING.long2ObjectEntrySet()) if (e.getValue().due() <= now) due.add(e.getLongKey());
		if (due.isEmpty()) return;
		due.sort(Comparator.comparingInt(l -> BlockPos.unpackLongY(l)));
		boolean sound = false;
		for (long key : due) {
			Entry e = PENDING.remove(key);
			BlockPos pos = BlockPos.fromLong(key);
			if (!w.isChunkLoaded(pos)) continue;
			BlockState cur = w.getBlockState(pos);
			if (cur.equals(e.state())) continue;
			if (!cur.isAir() && !cur.isReplaceable()) continue;   // somebody put something there since
			if (!e.state().getCollisionShape(w, pos).isEmpty()
					&& !w.getOtherEntities(null, new net.minecraft.util.math.Box(pos), en -> en instanceof net.minecraft.entity.LivingEntity).isEmpty()) {
				PENDING.put(key, new Entry(e.state(), e.nbt(), now + 40));   // someone's standing there: try again in a moment
				continue;
			}
			w.setBlockState(pos, e.state(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
			if (e.nbt() != null) {
				BlockEntity be = w.getBlockEntity(pos);
				if (be != null) {
					be.read(e.nbt(), w.getRegistryManager());
					be.markDirty();
					w.updateListeners(pos, e.state(), e.state(), Block.NOTIFY_LISTENERS);
				}
			}
			w.spawnParticles(com.phantompowers.GhostPowers.GREEN, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.01);
			if (!sound) {
				sound = true;
				w.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, net.minecraft.sound.SoundCategory.BLOCKS, 0.6f, 1.4f);
			}
		}
	}

	public static void clear() {
		PENDING.clear();
	}
}
