package com.phantompowers.world;

import com.phantompowers.FurnitureBlock;
import com.phantompowers.GhostFlags;
import com.phantompowers.ModBlocks;
import com.phantompowers.ModEntities;
import com.phantompowers.entity.SeatEntity;
import com.phantompowers.entity.TownspersonEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Amity Park is a lived-in town: wherever a player is in town, people are around them, eating at the Nasty Burger and
 * the dining tables, browsing the shelves in the shops, and strolling along the pavements and park paths. They come
 * and go as you move around, so the town never feels empty and never fills up.
 */
public final class TownLife {
	public static final String TAG = "phantom_ambient";
	static final int WALKERS = 9, SHOPPERS = 5, DINERS = 6, MOST = 22;
	private static final Random R = Random.create();

	private TownLife() {}

	public static void tick(MinecraftServer server) {
		int t = server.getTicks();
		if (t % 20 != 7) return;
		AmityPark.State st = AmityPark.state(server);
		if (!st.built || AmityPark.building()) return;
		ServerWorld w = server.getOverworld();
		if (t % 100 == 7) cleanup(w);
		if (!w.getGameRules().getBoolean(net.minecraft.world.GameRules.DO_MOB_SPAWNING)) return;
		for (ServerPlayerEntity p : w.getPlayers()) {
			if (p.isSpectator() || !AmityPark.inTown(st, p.getBlockPos())) continue;
			populate(w, st.center, p);
		}
	}

	/** Townsfolk nobody is near any more go home (vanish). */
	private static void cleanup(ServerWorld w) {
		for (TownspersonEntity v : w.getEntitiesByType(TypeFilter.instanceOf(TownspersonEntity.class), v -> v.getCommandTags().contains(TAG))) {
			if (GhostFlags.HOSTS.contains(v.getUuid())) continue;
			if (w.getClosestPlayer(v.getX(), v.getY(), v.getZ(), 96, false) == null) {
				v.stopRiding();
				v.discard();
			}
		}
	}

	private static void populate(ServerWorld w, BlockPos c, ServerPlayerEntity p) {
		List<TownspersonEntity> near = w.getEntitiesByClass(TownspersonEntity.class, p.getBoundingBox().expand(64, 24, 64),
				v -> v.getCommandTags().contains(TAG));
		if (near.size() >= MOST) return;
		int walk = 0, shop = 0, eat = 0;
		for (TownspersonEntity v : near) {
			switch (v.activity()) {
				case TownspersonEntity.EATING -> eat++;
				case TownspersonEntity.SHOPPING -> shop++;
				default -> walk++;
			}
		}
		int made = 0;
		for (int tries = 0; tries < 30 && made < 2; tries++) {
			int x = p.getBlockX() + R.nextInt(97) - 48, z = p.getBlockZ() + R.nextInt(97) - 48;
			int rx = x - c.getX(), rz = z - c.getZ();
			if (Math.abs(rx) > AmityPark500.HALF - 6 || Math.abs(rz) > AmityPark500.HALF - 6 || reserved(rx, rz)) continue;
			if (!w.isChunkLoaded(ChunkPos.toLong(x >> 4, z >> 4))) continue;
			int kind = R.nextInt(3);
			BlockPos at = null;
			int what = TownspersonEntity.WALKING;
			if (kind == 0 && eat < DINERS) {
				at = seat(w, c, x, z);
				what = TownspersonEntity.EATING;
			} else if (kind == 1 && shop < SHOPPERS) {
				at = shelf(w, c, x, z);
				what = TownspersonEntity.SHOPPING;
			} else if (walk < WALKERS) {
				at = pavement(w, c, x, z);
				// people don't pop up out of nowhere right next to you on the street
				if (at != null && at.getSquaredDistance(p.getPos()) < 18 * 18) at = null;
			}
			if (at == null || !spawn(w, at, what)) continue;
			made++;
			switch (what) {
				case TownspersonEntity.EATING -> eat++;
				case TownspersonEntity.SHOPPING -> shop++;
				default -> walk++;
			}
		}
	}

	/** Places where the story has its own people (Casper High and the Fenton house): no random visitors there. */
	static boolean reserved(int rx, int rz) {
		return (rx >= -102 && rx <= -20 && rz >= -102 && rz <= -56) || (rx >= 10 && rx <= 46 && rz >= -48 && rz <= -18);
	}

	private static boolean loaded(ServerWorld w, int x, int z) {
		return w.isChunkLoaded(ChunkPos.toLong(x >> 4, z >> 4));
	}

	private static boolean taken(ServerWorld w, BlockPos pos) {
		return !w.getEntitiesByClass(SeatEntity.class, new Box(pos), e -> true).isEmpty()
				|| !w.getEntitiesByClass(TownspersonEntity.class, new Box(pos).expand(0.4), e -> true).isEmpty();
	}

	/** A free chair, stool or sofa pulled up to a table or counter, on the ground or first floor. */
	static BlockPos seat(ServerWorld w, BlockPos c, int x, int z) {
		BlockPos.Mutable m = new BlockPos.Mutable();
		for (int floor : new int[]{0, 5}) {
			int y = c.getY() + floor;
			for (int dx = -5; dx <= 5; dx++)
				for (int dz = -5; dz <= 5; dz++) {
					if (!loaded(w, x + dx, z + dz)) continue;
					m.set(x + dx, y, z + dz);
					BlockState s = w.getBlockState(m);
					if (!(s.getBlock() instanceof FurnitureBlock f) || !f.seat()) continue;
					boolean table = false;
					for (Direction d : Direction.Type.HORIZONTAL) {
						Block n = w.getBlockState(m.offset(d)).getBlock();
						if (n == ModBlocks.DINING_TABLE || n == ModBlocks.KITCHEN_COUNTER || n == ModBlocks.COFFEE_TABLE) table = true;
					}
					if (table && w.getBlockState(m.up()).isAir() && !taken(w, m)) return m.toImmutable();
				}
		}
		return null;
	}

	private static boolean shelfBlock(BlockState s) {
		return s.isOf(Blocks.BARREL) || s.isOf(Blocks.BOOKSHELF) || s.isOf(Blocks.COMPOSTER) || s.isOf(ModBlocks.BOOKCASE) || s.isOf(ModBlocks.FRIDGE);
	}

	/** Indoors, standing room next to shelves, barrels or bookcases (the shops, the mall and the library). */
	static BlockPos shelf(ServerWorld w, BlockPos c, int x, int z) {
		BlockPos.Mutable m = new BlockPos.Mutable();
		for (int floor : new int[]{0, 5}) {
			int y = c.getY() + floor;
			for (int dx = -4; dx <= 4; dx++)
				for (int dz = -4; dz <= 4; dz++) {
					if (!loaded(w, x + dx, z + dz)) continue;
					m.set(x + dx, y, z + dz);
					if (!w.getBlockState(m).isAir() || !w.getBlockState(m.up()).isAir()) continue;
					BlockState below = w.getBlockState(m.down());
					if (below.getCollisionShape(w, m.down()).isEmpty() || below.isOf(Blocks.GRASS_BLOCK) || below.isOf(Blocks.SMOOTH_STONE)) continue;
					if (w.isSkyVisible(m)) continue;
					int shelves = 0;
					for (Direction d : Direction.Type.HORIZONTAL) if (shelfBlock(w.getBlockState(m.offset(d)))) shelves++;
					// a lone fridge in someone's kitchen isn't a shop
					boolean shop = shelves >= 2;
					for (Direction d : Direction.Type.HORIZONTAL) {
						BlockState n = w.getBlockState(m.offset(d));
						if (shelfBlock(n) && !n.isOf(ModBlocks.FRIDGE)) shop = true;
					}
					if (shop && !taken(w, m)) return m.toImmutable();
				}
		}
		return null;
	}

	/** Open-air pavement or path at street level. */
	static BlockPos pavement(ServerWorld w, BlockPos c, int x, int z) {
		int y = c.getY();
		BlockPos.Mutable m = new BlockPos.Mutable();
		for (int i = 0; i < 10; i++) {
			int px = x + R.nextInt(9) - 4, pz = z + R.nextInt(9) - 4;
			if (!loaded(w, px, pz)) continue;
			m.set(px, y, pz);
			BlockState below = w.getBlockState(m.down());
			if (!(below.isOf(Blocks.SMOOTH_STONE) || below.isOf(Blocks.GRAVEL))) continue;
			if (!w.getBlockState(m).getCollisionShape(w, m).isEmpty() || !w.getBlockState(m.up()).isAir()) continue;
			if (!w.isSkyVisible(m)) continue;
			return m.toImmutable();
		}
		return null;
	}

	static boolean spawn(ServerWorld w, BlockPos at, int what) {
		TownspersonEntity v = ModEntities.TOWNSPERSON.create(w);
		if (v == null) return false;
		v.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, R.nextFloat() * 360f, 0f);
		v.initialize(w, w.getLocalDifficulty(at), SpawnReason.EVENT, null);
		v.addCommandTag(TAG);
		v.startActivity(what, at);
		if (!w.spawnEntity(v)) return false;
		if (what == TownspersonEntity.EATING && !FurnitureBlock.sit(w, at, v)) v.startActivity(TownspersonEntity.WALKING, at);
		return true;
	}
}
