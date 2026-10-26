package com.phantompowers.world;

import com.phantompowers.FurnitureBlock;
import com.phantompowers.ModBlocks;
import com.phantompowers.ModItems;
import com.phantompowers.PortalLogic;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import java.util.Comparator;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

/**
 * Amity Park: a whole ghost-haunted town built once in the Overworld, on the flattest dry land the mod can find a
 * few hundred blocks from spawn. The ground slopes gently out into the surrounding land, lawns use whatever the
 * local ground is, and trees at the edges are taken away whole. Inside: Main Street and Elm Avenue with side
 * streets, the Ghost Lab house (family home on top, ghost lab with a Ghost Portal in the basement), Casper High,
 * the Burger Boo diner, a corner store, the library, the town hall, an apartment block, a big park, and streets of
 * furnished family houses.
 */
public final class AmityPark {
	public static final int R = 104, BLEND = 18;

	/** Remembers where the town was built. */
	public static final class State extends PersistentState {
		public static final PersistentState.Type<State> TYPE = new PersistentState.Type<>(State::new, State::fromNbt, null);
		public boolean built;
		/** Windows, fences and railings joined up to their neighbours (towns built before this fix get it done once). */
		public boolean connected;
		public BlockPos center = BlockPos.ORIGIN;
		/** Blocks players placed in town themselves: those don't grow back when broken. */
		public final it.unimi.dsi.fastutil.longs.LongOpenHashSet placed = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();

		static State fromNbt(NbtCompound n, RegistryWrapper.WrapperLookup lookup) {
			State s = new State();
			s.built = n.getBoolean("built");
			s.connected = n.getBoolean("connected");
			s.center = BlockPos.fromLong(n.getLong("center"));
			for (long l : n.getLongArray("placed")) s.placed.add(l);
			return s;
		}

		@Override
		public NbtCompound writeNbt(NbtCompound n, RegistryWrapper.WrapperLookup lookup) {
			n.putBoolean("built", built);
			n.putBoolean("connected", connected);
			n.putLong("center", center.asLong());
			n.putLongArray("placed", placed.toLongArray());
			return n;
		}
	}

	private AmityPark() {}

	public static State state(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(State.TYPE, "phantom_amity_park");
	}

	// ------------------------------------------------------------ building in the background

	private static final ChunkTicketType<ChunkPos> TICKET = ChunkTicketType.create("phantom_amity_park", Comparator.comparingLong(ChunkPos::toLong));
	private static final int TICKET_RADIUS = 19, NEED_RADIUS = 17;   // chunks within NEED_RADIUS of the centre must be loaded
	private static Job job;

	/** A town being built a bit at a time: find a site, load the land in the background, then build in small steps. */
	private static final class Job {
		int phase;            // 0 = finding a site, 1 = loading the land, 2 = building
		int ring, k;
		double bestScore = Double.MAX_VALUE;
		BlockPos best;
		BlockPos center;
		ChunkPos ticket;
		java.util.List<Runnable> tasks;
		int next;
		/** Rebuilding the buildings of an existing town in place (the land is left alone). */
		boolean rebuild;
	}

	/** Rebuild every building in an existing town (in the background), with the latest designs. Returns false if there's no town yet. */
	public static boolean rebuild(MinecraftServer server) {
		TownRepair.clear();
		State st = state(server);
		if (!st.built || job != null) return false;
		job = new Job();
		job.rebuild = true;
		job.best = st.center;
		job.center = st.center;
		job.ticket = new ChunkPos(st.center);
		server.getOverworld().getChunkManager().addTicket(TICKET, job.ticket, TICKET_RADIUS, job.ticket);
		job.phase = 1;
		return true;
	}

	/** Is the town being built or rebuilt right now? */
	public static boolean building() {
		return job != null;
	}

	/** Is this spot part of the built town (the ground it stands on, the lab basement and the buildings)? */
	public static boolean inTown(State st, BlockPos pos) {
		if (!st.built) return false;
		BlockPos c = st.center;
		return Math.abs(pos.getX() - c.getX()) <= AmityPark500.HALF && Math.abs(pos.getZ() - c.getZ()) <= AmityPark500.HALF
				&& pos.getY() >= c.getY() - 20 && pos.getY() <= c.getY() + 48;
	}

	/** Has the town been built? */
	public static boolean ready(MinecraftServer server) {
		return state(server).built;
	}

	/** Start building the town in the background (if it isn't built or being built). Returns true if it's ready. */
	public static boolean request(MinecraftServer server) {
		if (state(server).built) return true;
		if (job == null) {
			job = new Job();
			BlockPos spawn = server.getOverworld().getSpawnPos();
			job.best = new BlockPos(spawn.getX() + 320, 0, spawn.getZ() + 320);
		}
		return false;
	}

	/** How far along the town is, 0..100 (100 = ready). */
	public static int progress(MinecraftServer server) {
		if (state(server).built) return 100;
		if (job == null) return 0;
		return switch (job.phase) {
			case 0 -> Math.min(5, (job.ring * 12 + job.k) * 5 / 48);
			case 1 -> 5 + loadedPercent(server.getOverworld(), job.ticket) * 25 / 100;
			default -> 30 + job.next * 70 / Math.max(1, job.tasks.size());
		};
	}

	private static int loadedPercent(ServerWorld w, ChunkPos c) {
		int ok = 0, all = 0;
		for (int dx = -NEED_RADIUS; dx <= NEED_RADIUS; dx++) for (int dz = -NEED_RADIUS; dz <= NEED_RADIUS; dz++) {
			all++;
			if (w.isChunkLoaded(ChunkPos.toLong(c.x + dx, c.z + dz))) ok++;
		}
		return ok * 100 / all;
	}

	/** Called every server tick: does a few milliseconds of work on the town, so the game never freezes. */
	static boolean joins(BlockState s) {
		Block b = s.getBlock();
		return b instanceof net.minecraft.block.PaneBlock || b instanceof net.minecraft.block.FenceBlock || b instanceof net.minecraft.block.WallBlock
				|| b instanceof StairsBlock || b instanceof com.phantompowers.SofaBlock;
	}

	static void join(ServerWorld w, BlockPos p) {
		BlockState s = w.getBlockState(p);
		if (!joins(s)) return;
		BlockState n = Block.postProcessState(s, w, p);
		if (n != s) w.setBlockState(p, n, Block.NOTIFY_LISTENERS);
	}

	/** Fixing up a town built before windows joined up: the next column of x to do. */
	private static int repairX = Integer.MIN_VALUE;

	private static void repair(MinecraftServer server) {
		State st = state(server);
		ServerWorld w = server.getOverworld();
		BlockPos c = st.center;
		if (repairX == Integer.MIN_VALUE) repairX = -R;
		int outer = R;
		BlockPos.Mutable m = new BlockPos.Mutable();
		for (int x = repairX; x < Math.min(repairX + 3, outer + 1); x++) {
			for (int z = -outer; z <= outer; z++) {
				int cx = c.getX() + x, cz = c.getZ() + z;
				if (!w.isChunkLoaded(net.minecraft.util.math.ChunkPos.toLong(cx >> 4, cz >> 4))) continue;
				for (int y = c.getY() - 11; y <= c.getY() + 28; y++) {
					m.set(cx, y, cz);
					if (joins(w.getBlockState(m))) join(w, m.toImmutable());
				}
			}
		}
		repairX += 3;
		if (repairX > outer) {
			st.connected = true;
			st.markDirty();
			repairX = Integer.MIN_VALUE;
		}
	}

	public static void tick(MinecraftServer server) {
		if (job == null) {
			// an older town: join up its windows a few columns at a time once someone is nearby (its chunks are loaded)
			State st = state(server);
			if (st.built && !st.connected && server.getTicks() % 2 == 0
					&& server.getOverworld().isChunkLoaded(net.minecraft.util.math.ChunkPos.toLong(st.center.getX() >> 4, st.center.getZ() >> 4))) repair(server);
			return;
		}
		State st = state(server);
		ServerWorld w = server.getOverworld();
		if (st.built && !job.rebuild) {
			finish(w);
			return;
		}
		long until = System.nanoTime() + 25_000_000L;   // half a tick: quick, but the game keeps running smoothly
		switch (job.phase) {
			case 0 -> {
				ChunkGenerator gen = w.getChunkManager().getChunkGenerator();
				NoiseConfig noise = w.getChunkManager().getNoiseConfig();
				do {
					double[] r = candidate(w, gen, noise, job.ring, job.k);
					if (r != null && r[0] < job.bestScore) {
						job.bestScore = r[0];
						job.best = new BlockPos((int) r[1], 0, (int) r[2]);
					}
					if (++job.k == 12) {
						job.k = 0;
						job.ring++;
						if (job.bestScore < 18 || job.ring >= 4) {
							job.ticket = new ChunkPos(job.best);
							w.getChunkManager().addTicket(TICKET, job.ticket, TICKET_RADIUS, job.ticket);
							job.phase = 1;
							return;
						}
					}
				} while (System.nanoTime() < until);
			}
			case 1 -> {
				if (server.getTicks() % 10 != 0 || loadedPercent(w, job.ticket) < 100) return;
				if (!job.rebuild) {
					int g = groundLevel(w, job.best);
					job.center = new BlockPos(job.best.getX(), g, job.best.getZ());
				}
				Builder b = new Builder(w, job.center);
				b.redo = job.rebuild;
				job.tasks = b.plan();
				job.phase = 2;
			}
			default -> {
				do {
					job.tasks.get(job.next++).run();
				} while (job.next < job.tasks.size() && System.nanoTime() < until);
				if (job.next >= job.tasks.size()) {
					st.built = true;
					st.connected = true;
					st.center = job.center;
					st.markDirty();
					finish(w);
					com.phantompowers.Story.townReady(server, st.center);
				}
			}
		}
	}

	/** Forget a half-built town (the server is shutting down; it starts again from scratch next time). */
	public static void reset() {
		job = null;
	}

	private static void finish(ServerWorld w) {
		if (job != null && job.ticket != null) w.getChunkManager().removeTicket(TICKET, job.ticket, TICKET_RADIUS, job.ticket);
		job = null;
	}

	/** Build the town right now if it isn't there yet (the game waits); returns its centre (street level). */
	public static BlockPos ensure(MinecraftServer server) {
		State st = state(server);
		if (st.built) return st.center;
		ServerWorld w = server.getOverworld();
		finish(w);
		BlockPos site = findSite(w);
		int r = R + BLEND + 16;
		for (int cx = (site.getX() - r) >> 4; cx <= (site.getX() + r) >> 4; cx++)
			for (int cz = (site.getZ() - r) >> 4; cz <= (site.getZ() + r) >> 4; cz++) w.getChunk(cx, cz);
		int g = groundLevel(w, site);
		BlockPos c = new BlockPos(site.getX(), g, site.getZ());
		new Builder(w, c).build();
		st.built = true;
		st.connected = true;
		st.center = c;
		st.markDirty();
		return c;
	}

	/** Look around spawn for the flattest dry land (no ocean or river) without generating anything. */
	static BlockPos findSite(ServerWorld w) {
		ChunkGenerator gen = w.getChunkManager().getChunkGenerator();
		NoiseConfig noise = w.getChunkManager().getNoiseConfig();
		BlockPos spawn = w.getSpawnPos();
		BlockPos best = new BlockPos(spawn.getX() + 320, 0, spawn.getZ() + 320);
		double bestScore = Double.MAX_VALUE;
		for (int ring = 0; ring < 4; ring++) {
			for (int k = 0; k < 12; k++) {
				double[] r = candidate(w, gen, noise, ring, k);
				if (r != null && r[0] < bestScore) {
					bestScore = r[0];
					best = new BlockPos((int) r[1], 0, (int) r[2]);
				}
			}
			if (bestScore < 18) break;
		}
		return best;
	}

	/** How good a spot candidate (ring, k) is: {score (lower is flatter), x, z}, or null if it's in water. */
	private static double[] candidate(ServerWorld w, ChunkGenerator gen, NoiseConfig noise, int ring, int k) {
		BlockPos spawn = w.getSpawnPos();
		int sea = w.getSeaLevel();
		int dist = 300 + ring * 180;
		double a = k * Math.PI * 2 / 12 + ring * 0.4;
		int cx = spawn.getX() + (int) (Math.cos(a) * dist), cz = spawn.getZ() + (int) (Math.sin(a) * dist);
		double sum = 0, sum2 = 0;
		int n = 0;
		for (int dx = -R; dx <= R; dx += 26) {
			for (int dz = -R; dz <= R; dz += 26) {
				int x = cx + dx, z = cz + dz;
				int h = gen.getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG, w, noise);
				RegistryEntry<Biome> b = gen.getBiomeSource().getBiome(x >> 2, sea >> 2, z >> 2, noise.getMultiNoiseSampler());
				if (h < sea || b.isIn(BiomeTags.IS_OCEAN) || b.isIn(BiomeTags.IS_RIVER) || b.isIn(BiomeTags.IS_DEEP_OCEAN)) return null;
				sum += h;
				sum2 += (double) h * h;
				n++;
			}
		}
		double mean = sum / n, var = sum2 / n - mean * mean;
		return new double[]{var + Math.max(0, mean - (sea + 30)) * 4 + ring * 6, cx, cz};
	}

	/** Street level: the middle height of the real ground across the town. */
	static int groundLevel(ServerWorld w, BlockPos site) {
		java.util.List<Integer> hs = new java.util.ArrayList<>();
		for (int dx = -R; dx <= R; dx += 13)
			for (int dz = -R; dz <= R; dz += 13) hs.add(surface(w, site.getX() + dx, site.getZ() + dz));
		java.util.Collections.sort(hs);
		return Math.max(w.getSeaLevel() + 1, Math.min(150, hs.get(hs.size() / 2)));
	}

	/** Top of the actual ground here (ignoring leaves, logs and plants). */
	static int surface(ServerWorld w, int x, int z) {
		int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
		BlockPos.Mutable m = new BlockPos.Mutable(x, y, z);
		while (m.getY() > w.getBottomY() + 1) {
			BlockState s = w.getBlockState(m);
			if (!s.getFluidState().isEmpty()) break;
			if (!s.isIn(BlockTags.LOGS) && !s.isIn(BlockTags.LEAVES) && !s.isReplaceable() && s.isOpaque()) break;
			m.move(0, -1, 0);
		}
		return m.getY() + 1;
	}

	/** Where you arrive: the welcome sign at the west end of Main Street. */
	public static Vec3d entrance(BlockPos c) {
		return new Vec3d(c.getX() - R + 6.5, c.getY(), c.getZ() + 0.5);
	}

	/** The basement lab under the Ghost Lab house (the middle of the floor). */
	public static BlockPos lab(BlockPos c) {
		return c.add(Builder.LAB_X + 13, -9, Builder.LAB_Z + 11);
	}

	/** Centre of the lab's Ghost Portal opening. */
	public static Vec3d labPortal(BlockPos c) {
		return Vec3d.ofBottomCenter(c.add(Builder.LAB_X + 5, -9 + 3, Builder.LAB_Z + 2)).add(0.5, 0, 0);
	}

	/** Just outside Casper High's front doors. */
	/** The middle of the dance floor on Casper High's top floor. */
	public static Vec3d danceFloor(BlockPos c) {
		return new Vec3d(c.getX() - 75.5, c.getY() + 10, c.getZ() - 75.5);
	}

	/** Inside the girls' bathroom on Casper High's top floor. */
	public static Vec3d bathroom(BlockPos c) {
		return new Vec3d(c.getX() - 92.5, c.getY() + 10, c.getZ() - 75.5);
	}

	/** Towns built before the dance hall existed get Casper High's top floor and dance hall added now. */
	public static void ensureDanceHall(MinecraftServer server) {
		State st = state(server);
		if (!st.built || building()) return;
		ServerWorld w = server.getOverworld();
		BlockPos c = st.center;
		if (w.getBlockState(c.add(-76, 9, -76)).isOf(Blocks.SEA_LANTERN)) return;
		Builder b = new Builder(w, c);
		if (!w.getBlockState(c.add(-97, 9, -97)).isOf(Blocks.OAK_PLANKS)) AmityPark500.thirdFloor(b);
		AmityPark500.danceHall(b);
		b.connect();
	}

	public static Vec3d schoolDoor(BlockPos c) {
		return new Vec3d(c.getX() - 78, c.getY(), c.getZ() - 66.5);
	}

	/** The middle of Casper High's cafeteria (ground floor). */
	public static Vec3d cafeteria(BlockPos c) {
		return new Vec3d(c.getX() - 85.5, c.getY(), c.getZ() - 74.5);
	}

	/** Inside the cafeteria kitchen, behind the serving counter. */
	public static Vec3d cafeteriaKitchen(BlockPos c) {
		return new Vec3d(c.getX() - 68.5, c.getY(), c.getZ() - 74.5);
	}

	/** In front of the lab consoles in the basement lab. */
	public static Vec3d labConsoles(BlockPos c) {
		return new Vec3d(c.getX() + Builder.LAB_X + 6, c.getY() - 9, c.getZ() + Builder.LAB_Z + 8.5);
	}

	/** Just inside the park, by the corner nearest the town centre. */
	public static Vec3d parkGate(BlockPos c) {
		return new Vec3d(c.getX() + 14.5, c.getY(), c.getZ() + 12.5);
	}

	/** In front of the Quick Stop corner store. */
	public static Vec3d store(BlockPos c) {
		return new Vec3d(c.getX() - 15.5, c.getY(), c.getZ() - 10.5);
	}

	/** Among the reading tables in the library. */
	public static Vec3d library(BlockPos c) {
		return new Vec3d(c.getX() + 80.5, c.getY(), c.getZ() - 33.5);
	}

	/** Open grass in the park between the gate and the fountain. */
	public static Vec3d parkMeadow(BlockPos c) {
		return new Vec3d(c.getX() + 24.5, c.getY(), c.getZ() + 20.5);
	}

	/** Beside the park fountain, where the dance is. */
	public static Vec3d parkFountain(BlockPos c) {
		return new Vec3d(c.getX() + 38.5, c.getY(), c.getZ() + 29.5);
	}

	/** On the grass at the edge of the park pond. */
	public static Vec3d parkPond(BlockPos c) {
		return new Vec3d(c.getX() + 63.5, c.getY(), c.getZ() + 27.5);
	}

	/** Just outside the Ghost Lab house's front door. */
	public static Vec3d labHouseDoor(BlockPos c) {
		return new Vec3d(c.getX() + Builder.LAB_X + 14.5, c.getY(), c.getZ() + Builder.LAB_Z + 22.5);
	}

	static final class Builder {
		static final int LAB_X = 14, LAB_Z = -44;   // north-west corner of the Ghost Lab house (28 x 22)
		final ServerWorld w;
		final BlockPos c;
		final int g;
		final net.minecraft.util.math.random.Random rnd;
		BlockState lawn = Blocks.GRASS_BLOCK.getDefaultState(), under = Blocks.DIRT.getDefaultState();
		boolean redo;

		Builder(ServerWorld w, BlockPos c) {
			this.w = w;
			this.c = c;
			this.g = c.getY();
			this.rnd = net.minecraft.util.math.random.Random.create(c.asLong());
		}

		// ------------------------------------------------------------ helpers

		final java.util.List<BlockPos> joins = new java.util.ArrayList<>();

		void set(int x, int y, int z, BlockState s) {
			BlockPos p = c.add(x, y - g, z);
			w.setBlockState(p, s, Block.NOTIFY_LISTENERS);
			if (joins(s)) joins.add(p);
		}

		/** Glass panes, iron bars, fences, walls and stairs shape themselves to their neighbours, once everything is placed. */
		void connect() {
			for (BlockPos p : joins) join(w, p);
			joins.clear();
		}

		BlockState get(int x, int y, int z) {
			return w.getBlockState(c.add(x, y - g, z));
		}

		void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
			for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
				for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
					for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
		}

		void walls(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
			fill(x0, y0, z0, x1, y1, z0, s);
			fill(x0, y0, z1, x1, y1, z1, s);
			fill(x0, y0, z0, x0, y1, z1, s);
			fill(x1, y0, z0, x1, y1, z1, s);
		}

		void door(int x, int y, int z, Direction facing, Block door) {
			door(x, y, z, facing, door, net.minecraft.block.enums.DoorHinge.LEFT);
		}

		void door(int x, int y, int z, Direction facing, Block door, net.minecraft.block.enums.DoorHinge hinge) {
			BlockState s = door.getDefaultState().with(DoorBlock.FACING, facing).with(DoorBlock.HINGE, hinge).with(DoorBlock.OPEN, false);
			set(x, y, z, s.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
			set(x, y + 1, z, s.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		}

		/** A pair of doors at x and x + 1 (along the x axis) in a wall facing south or north. */
		void doubleDoor(int x, int y, int z, Direction facing, Block door) {
			boolean south = facing == Direction.SOUTH;
			// seen from outside, the left door hinges on the left and the right one on the right
			door(x, y, z, facing, door, south ? net.minecraft.block.enums.DoorHinge.RIGHT : net.minecraft.block.enums.DoorHinge.LEFT);
			door(x + 1, y, z, facing, door, south ? net.minecraft.block.enums.DoorHinge.LEFT : net.minecraft.block.enums.DoorHinge.RIGHT);
		}

		/** A piece of furniture facing this way (the way a person sitting in it would look). */
		void furn(int x, int y, int z, Block b, Direction facing) {
			if (b instanceof com.phantompowers.TallStorageBlock) {
				// fridges and lockers are two blocks tall
				BlockState s = b.getDefaultState().with(FurnitureBlock.FACING, facing);
				set(x, y, z, s.with(com.phantompowers.TallStorageBlock.HALF, DoubleBlockHalf.LOWER));
				set(x, y + 1, z, s.with(com.phantompowers.TallStorageBlock.HALF, DoubleBlockHalf.UPPER));
				return;
			}
			set(x, y, z, b.getDefaultState().with(FurnitureBlock.FACING, facing));
		}

		void bed(int x, int y, int z, Direction headTowards, Block bed) {
			set(x, y, z, bed.getDefaultState().with(BedBlock.FACING, headTowards).with(BedBlock.PART, BedPart.FOOT));
			BlockPos hp = new BlockPos(x, y, z).offset(headTowards);
			set(hp.getX(), y, hp.getZ(), bed.getDefaultState().with(BedBlock.FACING, headTowards).with(BedBlock.PART, BedPart.HEAD));
		}

		void rug(int x0, int z0, int x1, int z1, int y, Block carpet) {
			fill(x0, y, z0, x1, y, z1, carpet.getDefaultState());
		}

		void lamp(int x, int y, int z) {
			set(x, y, z, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		}

		/** A wall sign of the given wood with glowing text in the given colour. */
		void sign(Block wood, DyeColor color, int x, int y, int z, Direction facing, String... lines) {
			set(x, y, z, wood.getDefaultState().with(WallSignBlock.FACING, facing));
			if (w.getBlockEntity(c.add(x, y - g, z)) instanceof SignBlockEntity sb) {
				SignText t = new SignText();
				for (int i = 0; i < Math.min(4, lines.length); i++) t = t.withMessage(i, Text.literal(lines[i]));
				t = t.withColor(color).withGlowing(true);
				sb.setText(t, true);
				sb.markDirty();
			}
		}

		void sign(int x, int y, int z, Direction facing, boolean wall, String... lines) {
			if (wall) set(x, y, z, Blocks.OAK_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, facing));
			else set(x, y, z, Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, facing.getHorizontal() * 4));
			if (w.getBlockEntity(c.add(x, y - g, z)) instanceof SignBlockEntity sb) {
				SignText t = new SignText();
				for (int i = 0; i < Math.min(4, lines.length); i++) t = t.withMessage(i, Text.literal(lines[i]));
				t = t.withColor(DyeColor.BLACK).withGlowing(true);
				sb.setText(t, true);
				sb.markDirty();
			}
		}

		BlockState stairs(Block b, Direction facing) {
			return b.getDefaultState().with(StairsBlock.FACING, facing);
		}

		BlockState slab(Block b, boolean top) {
			return b.getDefaultState().with(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
		}

		void tree(int x, int z) {
			int h = 4 + rnd.nextInt(3);
			for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = h - 2; dy <= h + 1; dy++) {
				if (Math.abs(dx) + Math.abs(dz) + Math.max(0, dy - h) * 2 <= 3 && get(x + dx, g + dy, z + dz).isAir())
					set(x + dx, g + dy, z + dz, Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
			}
			fill(x, g, z, x, g + h, z, Blocks.OAK_LOG.getDefaultState());
		}

		// --------------------------------------------------------- the town

		/** Build it all at once (just the middle of town: the whole thing at once would freeze the server). */
		void build() {
			for (Runnable r : AmityPark500.plan(this, false)) r.run();
		}

		/** The town as a list of small jobs, so it can be built a little at a time without freezing the game. */
		java.util.List<Runnable> plan() {
			return AmityPark500.plan(this);
		}

		/** Level the town, and slope the edges smoothly into the land around it. */
		/** Use whatever the local ground is (grass, sand, snow...) for lawns. */
		void pickLawn() {
			BlockState top = w.getBlockState(new BlockPos(c.getX(), surface(w, c.getX(), c.getZ()) - 1, c.getZ()));
			if (top.isOf(Blocks.SAND) || top.isOf(Blocks.RED_SAND)) {
				lawn = top;
				under = top.isOf(Blocks.SAND) ? Blocks.SANDSTONE.getDefaultState() : Blocks.RED_SANDSTONE.getDefaultState();
			} else if (top.isOf(Blocks.PODZOL) || top.isOf(Blocks.MYCELIUM) || top.isOf(Blocks.SNOW_BLOCK) || top.isOf(Blocks.COARSE_DIRT)) {
				lawn = top;
			}
		}

		/** Level rows x0..x1 of the town (and its sloping edge). */
		void terrain(int x0, int x1) {
			int outer = R + BLEND;
			for (int x = x0; x <= x1; x++) {
				for (int z = -outer; z <= outer; z++) {
					int ax = c.getX() + x, az = c.getZ() + z;
					double out = Math.max(Math.abs(x), Math.abs(z)) - R;          // > 0 in the blend ring
					int natural = surface(w, ax, az);
					int target;
					if (out <= 0) target = g;
					else {
						double t = out / BLEND;
						t = t * t * (3 - 2 * t);
						target = (int) Math.round(MathHelper.lerp(t, g, natural));
						if (t > 0.97) target = natural;
					}
					BlockState colTop = w.getBlockState(new BlockPos(ax, natural - 1, az));
					BlockState surf = out > 0 && (colTop.isOf(Blocks.GRASS_BLOCK) || colTop.isOf(Blocks.SAND) || colTop.isOf(Blocks.RED_SAND)
							|| colTop.isOf(Blocks.PODZOL) || colTop.isOf(Blocks.SNOW_BLOCK)) ? colTop : lawn;
					// clear above the new ground (trees go whole, so nothing is left floating)
					int clearTop = Math.max(natural, target) + (out <= 0 ? 32 : 24);
					for (int y = target; y <= clearTop; y++) {
						if (!get(x, y, z).isAir()) set(x, y, z, Blocks.AIR.getDefaultState());
					}
					// fill below up to the new ground
					set(x, target - 1, z, surf);
					for (int y = target - 2; y >= target - 40; y--) {
						BlockState s = get(x, y, z);
						if (y < natural - 1 && s.isOpaque() && s.getFluidState().isEmpty() && !s.isIn(BlockTags.LOGS)) break;
						set(x, y, z, y >= target - 4 ? (surf.isOf(Blocks.SAND) || surf.isOf(Blocks.RED_SAND) ? under : Blocks.DIRT.getDefaultState())
								: Blocks.STONE.getDefaultState());
					}
					if (lawn.isOf(Blocks.SNOW_BLOCK) && out > 0 && rnd.nextInt(2) == 0) set(x, target, z, Blocks.SNOW.getDefaultState());
				}
			}
		}

		void roads() {
			BlockState asphalt = Blocks.BLACK_CONCRETE.getDefaultState(), line = Blocks.YELLOW_CONCRETE.getDefaultState(),
					walk = Blocks.SMOOTH_STONE.getDefaultState(), curb = Blocks.STONE_BRICKS.getDefaultState();
			// Main Street (east-west) and Elm Avenue (north-south), 7 wide
			for (int a = -R; a <= R; a++) {
				for (int b = -3; b <= 3; b++) {
					set(a, g - 1, b, asphalt);
					set(b, g - 1, a, asphalt);
				}
				if (Math.floorMod(a, 4) < 2 && Math.abs(a) > 3) {
					set(a, g - 1, 0, line);
					set(0, g - 1, a, line);
				}
				for (int b : new int[]{-5, -4, 4, 5}) if (Math.abs(a) > 5) {
					set(a, g - 1, b, Math.abs(b) == 4 ? curb : walk);
					set(b, g - 1, a, Math.abs(b) == 4 ? curb : walk);
				}
			}
			// side streets (east-west at z = -54 / 54) and the avenues out to the edge of town
			for (int zs : new int[]{-54, 54}) {
				for (int a = -R; a <= R; a++) {
					for (int b = -2; b <= 2; b++) set(a, g - 1, zs + b, asphalt);
					if (Math.abs(a) > 5) {
						set(a, g - 1, zs - 3, curb);
						set(a, g - 1, zs + 3, curb);
					}
				}
				for (int xs : new int[]{-54, 54}) {
					int from = zs < 0 ? -R : zs, to = zs < 0 ? zs : R;
					for (int a = from; a <= to; a++) for (int b = -2; b <= 2; b++) set(xs + b, g - 1, a, asphalt);
				}
			}
			// crosswalks
			for (int b = -3; b <= 3; b += 2) for (int a : new int[]{-5, 5}) {
				set(a, g - 1, b, Blocks.WHITE_CONCRETE.getDefaultState());
				set(b, g - 1, a, Blocks.WHITE_CONCRETE.getDefaultState());
			}
		}

		void lamps() {
			for (int a = -R + 6; a <= R - 6; a += 12) {
				for (int[] p : new int[][]{{a, -6}, {a, 6}, {-6, a}, {6, a}, {a, -58}, {a, 58}}) {
					if (Math.abs(p[0]) <= 7 && Math.abs(p[1]) <= 7) continue;
					if (!get(p[0], g, p[1]).isAir() || !get(p[0], g - 1, p[1]).isOpaque()) continue;
					fill(p[0], g, p[1], p[0], g + 3, p[1], Blocks.DARK_OAK_FENCE.getDefaultState());
					set(p[0], g + 4, p[1], Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, false));
				}
				// street trees between lamps
				for (int[] p : new int[][]{{a + 6, -7}, {a + 6, 7}}) {
					if (Math.abs(p[0]) < 8 || Math.abs(p[0]) > R - 4) continue;
					if (get(p[0], g, p[1]).isAir() && get(p[0], g - 1, p[1]).isOf(lawn.getBlock())) tree(p[0], p[1]);
				}
			}
		}

		void welcome() {
			int x = -R + 4;
			fill(x, g, -8, x, g + 3, -8, Blocks.DARK_OAK_LOG.getDefaultState());
			fill(x, g, -14, x, g + 3, -14, Blocks.DARK_OAK_LOG.getDefaultState());
			fill(x, g + 4, -14, x, g + 5, -8, Blocks.SPRUCE_PLANKS.getDefaultState());
			fill(x, g + 6, -14, x, g + 6, -8, slab(Blocks.SPRUCE_SLAB, false));
			sign(x + 1, g + 5, -11, Direction.EAST, true, "Welcome to", "AMITY PARK", "A NICE PLACE TO LIVE", "(mostly)");
			sign(x + 1, g + 4, -11, Direction.EAST, true, "Pop. 22,000", "and some ghosts", "", "Phantom Menu: `");
			for (int z = -13; z <= -9; z++) {
				set(x + 1, g, z, Blocks.LIME_STAINED_GLASS.getDefaultState());
				set(x + 2, g, z, rnd.nextBoolean() ? Blocks.POPPY.getDefaultState() : Blocks.CORNFLOWER.getDefaultState());
			}
		}

		// -------------------------------------------------------- buildings

		/** A box building with windows, floors every 5 blocks, a flat roof with a parapet and lights. */
		void shell(int x0, int z0, int wd, int dp, int floors, BlockState wall, BlockState trim, BlockState floor, BlockState glass) {
			int x1 = x0 + wd - 1, z1 = z0 + dp - 1, top = g + floors * 5;
			fill(x0, g, z0, x1, top + 1, z1, Blocks.AIR.getDefaultState());
			fill(x0, g - 1, z0, x1, g - 1, z1, floor);
			walls(x0, g, z0, x1, top - 1, z1, wall);
			for (int[] cn : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) fill(cn[0], g, cn[1], cn[0], top, cn[1], trim);
			for (int f = 1; f < floors; f++) {
				int fy = g + f * 5 - 1;
				fill(x0 + 1, fy, z0 + 1, x1 - 1, fy, z1 - 1, floor);
				walls(x0, fy, z0, x1, fy, z1, trim);
			}
			fill(x0, top - 1, z0, x1, top - 1, z1, trim);
			fill(x0 + 1, top - 1, z0 + 1, x1 - 1, top - 1, z1 - 1, floor);
			walls(x0, top, z0, x1, top, z1, trim);
			for (int f = 0; f < floors; f++) {
				int y = g + f * 5 + 1;
				for (int x = x0 + 2; x < x1 - 1; x += 3) {
					fill(x, y, z0, x, y + 1, z0, glass);
					fill(x, y, z1, x, y + 1, z1, glass);
				}
				for (int z = z0 + 2; z < z1 - 1; z += 3) {
					fill(x0, y, z, x0, y + 1, z, glass);
					fill(x1, y, z, x1, y + 1, z, glass);
				}
				for (int x = x0 + 3; x < x1 - 1; x += 5) for (int z = z0 + 3; z < z1 - 1; z += 5) lamp(x, g + f * 5 + 3, z);
			}
		}

		/** A staircase going up one floor (5 blocks) along +z from (x, z0), two wide. */
		void stairsUp(int x, int z0, int fromY, BlockState step, BlockState floor) {
			for (int i = 0; i < 5; i++) {
				fill(x, fromY + i, z0 + i, x + 1, fromY + i, z0 + i, step);
				fill(x, fromY + i + 1, z0 + i, x + 1, fromY + i + 3, z0 + i, Blocks.AIR.getDefaultState());
			}
			fill(x, fromY + 4, z0 - 1, x + 1, fromY + 4, z0 + 3, Blocks.AIR.getDefaultState());
			fill(x - 1, fromY + 5, z0, x - 1, fromY + 5, z0 + 3, Blocks.OAK_FENCE.getDefaultState());
		}

		/** Ghost Lab house: a two-storey family home over a ghost lab, with a rooftop sensor tower. */
		void labHouse(int x0, int z0) {
			int x1 = x0 + 27, z1 = z0 + 21;
			BlockState wall = Blocks.WHITE_CONCRETE.getDefaultState(), trim = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(),
					floor = Blocks.SPRUCE_PLANKS.getDefaultState(), glass = Blocks.GLASS_PANE.getDefaultState();
			for (int z = z1 + 1; z <= -6; z++) fill(x0 + 13, g - 1, z, x0 + 14, g - 1, z, Blocks.SMOOTH_STONE.getDefaultState());
			for (int x = x0; x <= x1; x++) if (x < x0 + 12 || x > x0 + 15) set(x, g, z1 + 4, Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState());
			for (int x = x0 + 2; x <= x1 - 2; x += 4) if (x < x0 + 12 || x > x0 + 15) set(x, g, z1 + 1, Blocks.ROSE_BUSH.getDefaultState());
			// ---- basement lab
			int lf = g - 10;
			fill(x0, lf, z0, x1, g - 1, z1, Blocks.POLISHED_DEEPSLATE.getDefaultState());
			fill(x0 + 1, lf + 1, z0 + 1, x1 - 1, g - 2, z1 - 1, Blocks.AIR.getDefaultState());
			fill(x0 + 1, lf, z0 + 1, x1 - 1, lf, z1 - 1, Blocks.SMOOTH_STONE.getDefaultState());
			for (int x = x0 + 1; x < x1; x++) set(x, lf, z0 + 9, Blocks.LIME_CONCRETE.getDefaultState());
			for (int z = z0 + 1; z < z1; z++) set(x0 + 18, lf, z, Blocks.LIME_CONCRETE.getDefaultState());
			for (int x = x0 + 1; x < x1; x++) {
				set(x, lf + 1, z0 + 1, Blocks.YELLOW_CONCRETE.getDefaultState());
				if (x % 4 == 0 && (x < x0 + 2 || x > x0 + 9)) fill(x, lf + 2, z0 + 1, x, g - 2, z0 + 1, Blocks.LIME_STAINED_GLASS.getDefaultState());
			}
			java.util.Set<BlockPos> inside = PortalLogic.buildLabPortal(w, c.add(x0 + 3, lf + 1 - g, z0 + 2), Direction.Axis.X, Block.NOTIFY_LISTENERS, false);
			BlockPos button = inside.stream().min((a, b) -> a.getY() != b.getY() ? a.getY() - b.getY() : a.getX() - b.getX()).orElse(null);
			if (button != null) w.setBlockState(button, Blocks.STONE_BUTTON.getDefaultState().with(ButtonBlock.FACE, BlockFace.FLOOR)
					.with(ButtonBlock.FACING, Direction.SOUTH), Block.NOTIFY_LISTENERS);
			sign(x0 + 10, lf + 3, z0 + 2, Direction.SOUTH, true, "GHOST PORTAL", "Step inside and", "press the switch", "(don't!)");
			for (int x = x0 + 3; x <= x0 + 8; x++) furn(x, lf + 1, z0 + 7, ModBlocks.LAB_CONSOLE, Direction.NORTH);
			furn(x0 + 5, lf + 1, z0 + 8, ModBlocks.OAK_CHAIR, Direction.NORTH);
			furn(x0 + 7, lf + 1, z0 + 8, ModBlocks.OAK_CHAIR, Direction.NORTH);
			for (int z = z0 + 3; z <= z0 + 8; z++) furn(x1 - 1, lf + 1, z, ModBlocks.LAB_CONSOLE, Direction.WEST);
			fill(x1 - 1, lf + 3, z0 + 3, x1 - 1, lf + 5, z0 + 8, Blocks.BLACK_STAINED_GLASS.getDefaultState());
			fill(x1 - 1, lf + 4, z0 + 4, x1 - 1, lf + 4, z0 + 7, Blocks.LIME_STAINED_GLASS.getDefaultState());
			fill(x0 + 11, lf + 1, z0 + 12, x0 + 15, lf + 1, z0 + 13, slab(Blocks.SMOOTH_STONE_SLAB, true));
			set(x0 + 11, lf + 2, z0 + 12, Blocks.BREWING_STAND.getDefaultState());
			set(x0 + 13, lf + 2, z0 + 12, Blocks.LANTERN.getDefaultState());
			set(x0 + 15, lf + 2, z0 + 13, Blocks.LIME_CANDLE.getDefaultState());
			furn(x0 + 13, lf + 1, z0 + 14, ModBlocks.BAR_STOOL, Direction.NORTH);
			set(x0 + 2, lf + 1, z1 - 2, ModBlocks.HUNTER_WORKBENCH.getDefaultState());
			set(x0 + 20, lf + 1, z0 + 11, ModBlocks.PROTOTYPE_PORTAL.getDefaultState());
			set(x0 + 24, lf + 1, z0 + 12, ModBlocks.CLONING_POD.getDefaultState());
			set(x0 + 24, lf + 1, z0 + 15, ModBlocks.CLONING_POD.getDefaultState());
			fill(x0 + 1, lf + 1, z0 + 11, x0 + 1, lf + 3, z0 + 15, Blocks.IRON_BLOCK.getDefaultState());
			fill(x0 + 1, lf + 2, z0 + 12, x0 + 1, lf + 2, z0 + 14, Blocks.LIME_STAINED_GLASS.getDefaultState());
			for (int z = z0 + 11; z <= z0 + 15; z++) set(x0 + 1, lf + 4, z, Blocks.REDSTONE_LAMP.getDefaultState());
			for (int x = x0 + 5; x <= x0 + 11; x++) furn(x, lf + 1, z1 - 1, ModBlocks.LOCKER, Direction.NORTH);
			lockers(x0 + 6, lf + 1, z1 - 2);
			sign(x0 + 8, lf + 2, z1 - 2, Direction.NORTH, true, "GEAR LOCKER", "Blaster, sword,", "ectoplasm...", "Hunters only");
			for (int x = x0 + 3; x < x1 - 4; x += 5) for (int z = z0 + 3; z < z1 - 1; z += 5) lamp(x, g - 2, z);
			// stairs down from the house (east side)
			for (int i = 0; i < 9; i++) {
				fill(x1 - 3, g - 1 - i, z1 - 2 - i, x1 - 2, g + 2 - i, z1 - 2 - i, Blocks.AIR.getDefaultState());
				fill(x1 - 3, g - 2 - i, z1 - 2 - i, x1 - 2, g - 2 - i, z1 - 2 - i, stairs(Blocks.POLISHED_DEEPSLATE_STAIRS, Direction.SOUTH));
			}
			// ---- the house
			shell(x0, z0, 28, 22, 2, wall, trim, floor, glass);
			fill(x1 - 3, g - 1, z1 - 5, x1 - 2, g - 1, z1 - 2, Blocks.AIR.getDefaultState());
			fill(x1 - 4, g, z1 - 6, x1 - 4, g, z1 - 2, Blocks.OAK_FENCE.getDefaultState());
			doubleDoor(x0 + 13, g, z1, Direction.SOUTH, Blocks.IRON_DOOR);
			fill(x0 + 13, g, z1 + 1, x0 + 14, g, z1 + 1, Blocks.STONE_PRESSURE_PLATE.getDefaultState());
			fill(x0 + 13, g, z1 - 1, x0 + 14, g, z1 - 1, Blocks.STONE_PRESSURE_PLATE.getDefaultState());
			fill(x0 + 9, g + 3, z1 + 1, x0 + 18, g + 3, z1 + 1, Blocks.BLACK_CONCRETE.getDefaultState());
			fill(x0 + 10, g + 3, z1 + 2, x0 + 17, g + 3, z1 + 2, Blocks.LIME_STAINED_GLASS.getDefaultState());
			sign(x0 + 12, g + 2, z1 + 1, Direction.SOUTH, true, "", "GHOST LAB", "Ghost research", "& hunting");
			sign(x0 + 15, g + 2, z1 + 1, Direction.SOUTH, true, "", "Experiments", "in progress!", "");
			rug(x0 + 12, z0 + 12, x0 + 15, z1 - 2, g, Blocks.GREEN_CARPET);
			// living room (south-west)
			int lx = x0 + 1, lz = z0 + 12;
			rug(lx + 2, lz + 2, lx + 8, lz + 6, g, Blocks.GRAY_CARPET);
			for (int x = lx + 3; x <= lx + 6; x++) furn(x, g, lz + 7, ModBlocks.SOFA, Direction.NORTH);
			furn(lx + 1, g, lz + 4, ModBlocks.SOFA, Direction.EAST);
			furn(lx + 1, g, lz + 5, ModBlocks.SOFA, Direction.EAST);
			furn(lx + 4, g, lz + 4, ModBlocks.COFFEE_TABLE, Direction.NORTH);
			furn(lx + 5, g, lz + 4, ModBlocks.COFFEE_TABLE, Direction.NORTH);
			furn(lx + 4, g, lz + 1, ModBlocks.TELEVISION, Direction.SOUTH);
			furn(lx + 5, g, lz + 1, ModBlocks.TELEVISION, Direction.SOUTH);
			furn(lx + 2, g, lz + 7, ModBlocks.FLOOR_LAMP, Direction.NORTH);
			furn(lx + 7, g, lz + 7, ModBlocks.FLOOR_LAMP, Direction.NORTH);
			furn(lx + 9, g, lz + 1, ModBlocks.BOOKCASE, Direction.SOUTH);
			furn(lx + 10, g, lz + 1, ModBlocks.BOOKCASE, Direction.SOUTH);
			furn(lx, g, lz + 1, ModBlocks.POTTED_PLANT, Direction.NORTH);
			fill(lx, g, lz, lx + 10, g + 3, lz, wall);
			fill(lx + 4, g, lz, lx + 5, g + 2, lz, Blocks.AIR.getDefaultState());
			// kitchen and dining (north-west)
			for (int x = x0 + 1; x <= x0 + 10; x++) furn(x, g, z0 + 1, ModBlocks.KITCHEN_COUNTER, Direction.SOUTH);
			furn(x0 + 1, g, z0 + 1, ModBlocks.FRIDGE, Direction.SOUTH);
			set(x0 + 4, g, z0 + 1, Blocks.SMOKER.getDefaultState().with(net.minecraft.block.AbstractFurnaceBlock.FACING, Direction.SOUTH));
			set(x0 + 6, g, z0 + 1, Blocks.WATER_CAULDRON.getDefaultState().with(net.minecraft.block.LeveledCauldronBlock.LEVEL, 3));
			set(x0 + 8, g + 1, z0 + 1, Blocks.FLOWER_POT.getDefaultState());
			for (int x = x0 + 4; x <= x0 + 7; x++) {
				furn(x, g, z0 + 6, ModBlocks.DINING_TABLE, Direction.NORTH);
				furn(x, g, z0 + 5, ModBlocks.OAK_CHAIR, Direction.SOUTH);
				furn(x, g, z0 + 7, ModBlocks.OAK_CHAIR, Direction.NORTH);
			}
			set(x0 + 5, g + 1, z0 + 6, Blocks.CAKE.getDefaultState());
			// ops room (north-east)
			for (int x = x1 - 9; x <= x1 - 4; x++) furn(x, g, z0 + 1, ModBlocks.LAB_CONSOLE, Direction.SOUTH);
			furn(x1 - 7, g, z0 + 3, ModBlocks.OAK_CHAIR, Direction.NORTH);
			furn(x1 - 5, g, z0 + 3, ModBlocks.OAK_CHAIR, Direction.NORTH);
			fill(x1 - 9, g + 2, z0 + 1, x1 - 4, g + 3, z0 + 1, Blocks.BLACK_STAINED_GLASS.getDefaultState());
			furn(x1 - 1, g, z0 + 5, ModBlocks.SOFA, Direction.WEST);
			furn(x1 - 1, g, z0 + 6, ModBlocks.SOFA, Direction.WEST);
			stairsUp(x1 - 3, z0 + 8, g, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH), floor);
			// ---- upstairs: two bedrooms, a bathroom, a hall
			int u = g + 5;
			fill(x0 + 13, u, z0 + 1, x0 + 13, u + 3, z1 - 1, wall);
			fill(x0 + 13, u, z0 + 10, x0 + 13, u + 2, z0 + 11, Blocks.AIR.getDefaultState());
			fill(x0 + 1, u, z0 + 11, x0 + 12, u + 3, z0 + 11, wall);
			fill(x0 + 6, u, z0 + 11, x0 + 7, u + 2, z0 + 11, Blocks.AIR.getDefaultState());
			bed(x0 + 2, u, z1 - 2, Direction.NORTH, Blocks.LIME_BED);
			furn(x0 + 4, u, z1 - 1, ModBlocks.FLOOR_LAMP, Direction.NORTH);
			furn(x0 + 9, u, z1 - 1, ModBlocks.SCHOOL_DESK, Direction.SOUTH);
			furn(x0 + 9, u, z1 - 2, ModBlocks.OAK_CHAIR, Direction.SOUTH);
			furn(x0 + 11, u, z0 + 13, ModBlocks.BOOKCASE, Direction.WEST);
			furn(x0 + 11, u, z0 + 14, ModBlocks.BOOKCASE, Direction.WEST);
			set(x0 + 1, u, z0 + 13, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.EAST));
			rug(x0 + 4, z0 + 14, x0 + 8, z0 + 18, u, Blocks.BLACK_CARPET);
			set(x0 + 6, u, z0 + 16, Blocks.WHITE_CARPET.getDefaultState());
			bed(x0 + 2, u, z0 + 2, Direction.NORTH, Blocks.ORANGE_BED);
			bed(x0 + 5, u, z0 + 2, Direction.NORTH, Blocks.PURPLE_BED);
			furn(x0 + 9, u, z0 + 1, ModBlocks.SCHOOL_DESK, Direction.NORTH);
			furn(x0 + 9, u, z0 + 2, ModBlocks.OAK_CHAIR, Direction.NORTH);
			furn(x0 + 11, u, z0 + 1, ModBlocks.POTTED_PLANT, Direction.NORTH);
			rug(x0 + 3, z0 + 5, x0 + 8, z0 + 8, u, Blocks.PINK_CARPET);
			fill(x0 + 14, u, z0 + 1, x1 - 5, u, z0 + 6, Blocks.WHITE_CARPET.getDefaultState());
			fill(x0 + 15, u, z0 + 1, x0 + 17, u, z0 + 2, Blocks.QUARTZ_BLOCK.getDefaultState());
			set(x0 + 16, u, z0 + 1, Blocks.WATER_CAULDRON.getDefaultState().with(net.minecraft.block.LeveledCauldronBlock.LEVEL, 3));
			set(x0 + 19, u, z0 + 1, Blocks.CAULDRON.getDefaultState());
			rug(x0 + 14, z0 + 14, x1 - 5, z1 - 2, u, Blocks.GREEN_CARPET);
			furn(x1 - 5, u, z1 - 1, ModBlocks.POTTED_PLANT, Direction.NORTH);
			// ---- rooftop sensor tower
			int rt = g + 10;
			fill(x0 + 9, rt, z0 + 7, x0 + 18, rt, z0 + 14, Blocks.IRON_BLOCK.getDefaultState());
			fill(x0 + 10, rt + 1, z0 + 8, x0 + 17, rt + 4, z0 + 13, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
			fill(x0 + 10, rt + 3, z0 + 8, x0 + 17, rt + 3, z0 + 13, Blocks.LIME_STAINED_GLASS.getDefaultState());
			fill(x0 + 11, rt + 3, z0 + 9, x0 + 16, rt + 3, z0 + 12, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
			fill(x0 + 9, rt + 5, z0 + 7, x0 + 18, rt + 5, z0 + 14, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
			fill(x0 + 13, rt + 6, z0 + 10, x0 + 13, rt + 10, z0 + 10, Blocks.IRON_BARS.getDefaultState());
			set(x0 + 13, rt + 11, z0 + 10, Blocks.LIGHTNING_ROD.getDefaultState());
			fill(x0 + 16, rt + 6, z0 + 11, x0 + 17, rt + 6, z0 + 12, Blocks.SMOOTH_QUARTZ.getDefaultState());
			set(x0 + 16, rt + 7, z0 + 11, Blocks.END_ROD.getDefaultState());
			set(x0 + 10, rt + 6, z0 + 12, Blocks.SEA_LANTERN.getDefaultState());
		}

		void lockers(int x, int y, int z) {
			set(x, y, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.NORTH));
			set(x + 3, y, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.NORTH));
			if (w.getBlockEntity(c.add(x, y - g, z)) instanceof ChestBlockEntity ch) {
				ch.setStack(4, new ItemStack(ModItems.ECTOPLASM, 2));
				ch.setStack(11, new ItemStack(ModItems.GHOST_BLASTER));
				ch.setStack(13, new ItemStack(ModItems.ECTO_SWORD));
				ch.setStack(15, new ItemStack(ModItems.THERMOS));
				ch.setStack(22, new ItemStack(ModItems.ECTOPLASM, 16));
			}
			if (w.getBlockEntity(c.add(x + 3, y - g, z)) instanceof ChestBlockEntity ch) {
				ch.setStack(10, new ItemStack(ModItems.DNA_SAMPLER));
				ch.setStack(12, new ItemStack(ModItems.GHOST_NET, 4));
				ch.setStack(14, new ItemStack(ModItems.SHIELD_REMOTE));
				ch.setStack(16, new ItemStack(ModBlocks.SHIELD_EMITTER_ITEM, 4));
				ch.setStack(22, new ItemStack(ModItems.ACNE_CREAM, 2));
				ch.setStack(2, new ItemStack(ModItems.JUMPSUIT_TOP));
				ch.setStack(3, new ItemStack(ModItems.JUMPSUIT_PANTS));
				ch.setStack(4, new ItemStack(ModItems.JUMPSUIT_BOOTS));
			}
		}

		/** Casper High: two floors of classrooms off a locker-lined hall, a cafeteria, an office and a library corner. */
		void school(int x0, int z0) {
			int x1 = x0 + 39, z1 = z0 + 29;
			shell(x0, z0, 40, 30, 2, Blocks.BRICKS.getDefaultState(), Blocks.STONE_BRICKS.getDefaultState(),
					Blocks.POLISHED_ANDESITE.getDefaultState(), Blocks.GLASS_PANE.getDefaultState());
			for (int x = x0 + 14; x <= x0 + 25; x += 3) fill(x, g, z1 + 3, x, g + 6, z1 + 3, Blocks.QUARTZ_PILLAR.getDefaultState());
			fill(x0 + 13, g + 7, z1 + 1, x0 + 26, g + 7, z1 + 4, Blocks.SMOOTH_QUARTZ.getDefaultState());
			fill(x0 + 13, g - 1, z1 + 1, x0 + 26, g - 1, z1 + 4, Blocks.SMOOTH_STONE.getDefaultState());
			doubleDoor(x0 + 19, g, z1, Direction.SOUTH, Blocks.OAK_DOOR);
			sign(x0 + 19, g + 6, z1 + 5, Direction.SOUTH, true, "", "CASPER HIGH", "Home of the", "Ghost Hunters!");
			for (int z = z1 + 5; z <= -57; z++) fill(x0 + 18, g - 1, z, x0 + 21, g - 1, z, Blocks.SMOOTH_STONE.getDefaultState());
			fill(x0 + 10, g, z1 + 4, x0 + 10, g + 9, z1 + 4, Blocks.IRON_BARS.getDefaultState());
			fill(x0 + 11, g + 7, z1 + 4, x0 + 12, g + 9, z1 + 4, Blocks.LIME_WOOL.getDefaultState());
			for (int f = 0; f < 2; f++) {
				int y = g + f * 5;
				int hz0 = z0 + 13, hz1 = z0 + 16;
				fill(x0 + 1, y, hz0 - 1, x1 - 1, y + 3, hz0 - 1, Blocks.WHITE_TERRACOTTA.getDefaultState());
				fill(x0 + 1, y, hz1 + 1, x1 - 1, y + 3, hz1 + 1, Blocks.WHITE_TERRACOTTA.getDefaultState());
				for (int x = x0 + 2; x < x1 - 1; x++) {
					if (Math.floorMod(x - x0, 9) == 4) continue;
					furn(x, y, hz0, ModBlocks.LOCKER, Direction.SOUTH);
					furn(x, y, hz1, ModBlocks.LOCKER, Direction.NORTH);
				}
				fill(x0 + 1, y - 1, hz0, x1 - 1, y - 1, hz1, Blocks.LIME_TERRACOTTA.getDefaultState());
				for (int x = x0 + 4; x < x1; x += 9) {
					fill(x, y, hz0 - 1, x, y + 1, hz0, Blocks.AIR.getDefaultState());
					fill(x, y, hz1, x, y + 1, hz1 + 1, Blocks.AIR.getDefaultState());
				}
				for (int room = 0; room < 2; room++) {
					int rx0 = x0 + 1 + room * 19, rx1 = rx0 + 17;
					if (room == 0) fill(rx1 + 1, y, z0 + 1, rx1 + 1, y + 3, hz0 - 2, Blocks.WHITE_TERRACOTTA.getDefaultState());
					fill(rx0 + 3, y + 1, z0 + 1, rx1 - 3, y + 2, z0 + 1, Blocks.GREEN_TERRACOTTA.getDefaultState());
					fill(rx0 + 3, y + 3, z0 + 1, rx1 - 3, y + 3, z0 + 1, Blocks.SPRUCE_PLANKS.getDefaultState());
					String[][] lessons = {{"TODAY:", "Ectoplasm 101", "Homework p.13", ""}, {"Ghost Science", "Quiz FRIDAY", "No phasing", "through walls!"},
							{"History of", "Amity Park", "Chapter 4", ""}, {"Math", "2 + 2 = 4", "(even for", "ghosts)"}};
					sign(rx0 + 8, y + 2, z0 + 2, Direction.SOUTH, true, lessons[(f * 2 + room) % lessons.length]);
					set(rx1 - 2, y + 2, z0 + 2, Blocks.BLUE_WALL_BANNER.getDefaultState().with(net.minecraft.block.WallBannerBlock.FACING, Direction.SOUTH));
					furn(rx0 + 8, y, z0 + 3, ModBlocks.DINING_TABLE, Direction.SOUTH);
					furn(rx0 + 8, y, z0 + 2, ModBlocks.OAK_CHAIR, Direction.SOUTH);
					for (int dx = 2; dx <= 14; dx += 3) for (int dz = 5; dz <= 9; dz += 2) {
						furn(rx0 + dx, y, z0 + dz, ModBlocks.SCHOOL_DESK, Direction.NORTH);
						furn(rx0 + dx, y, z0 + dz + 1, ModBlocks.OAK_CHAIR, Direction.NORTH);
					}
					furn(rx1 - 1, y, z0 + 1, ModBlocks.POTTED_PLANT, Direction.SOUTH);
					furn(rx0, y, z0 + 1, ModBlocks.BOOKCASE, Direction.EAST);
				}
				if (f == 0) {
					for (int x : new int[]{x0 + 3, x0 + 8, x0 + 13}) for (int z = hz1 + 4; z <= z1 - 3; z += 4) {
						furn(x, y, z, ModBlocks.DINING_TABLE, Direction.NORTH);
						furn(x + 1, y, z, ModBlocks.DINING_TABLE, Direction.NORTH);
						furn(x, y, z - 1, ModBlocks.BAR_STOOL, Direction.SOUTH);
						furn(x + 1, y, z - 1, ModBlocks.BAR_STOOL, Direction.SOUTH);
						furn(x, y, z + 1, ModBlocks.BAR_STOOL, Direction.NORTH);
						furn(x + 1, y, z + 1, ModBlocks.BAR_STOOL, Direction.NORTH);
					}
					for (int x = x0 + 26; x <= x0 + 33; x++) furn(x, y, hz1 + 3, ModBlocks.KITCHEN_COUNTER, Direction.SOUTH);
					furn(x0 + 34, y, hz1 + 3, ModBlocks.FRIDGE, Direction.SOUTH);
					fill(x0 + 25, y, hz1 + 2, x0 + 25, y + 3, z1 - 1, Blocks.WHITE_TERRACOTTA.getDefaultState());
					fill(x0 + 25, y, z1 - 4, x0 + 25, y + 1, z1 - 3, Blocks.AIR.getDefaultState());
					furn(x0 + 31, y, z1 - 4, ModBlocks.DINING_TABLE, Direction.SOUTH);
					furn(x0 + 31, y, z1 - 5, ModBlocks.OAK_CHAIR, Direction.SOUTH);
					furn(x0 + 31, y, z1 - 3, ModBlocks.OAK_CHAIR, Direction.NORTH);
					furn(x1 - 1, y, z1 - 6, ModBlocks.BOOKCASE, Direction.WEST);
					furn(x1 - 1, y, z1 - 5, ModBlocks.BOOKCASE, Direction.WEST);
				} else {
					for (int x = x0 + 2; x < x0 + 20; x += 2) furn(x, y, z1 - 1, ModBlocks.BOOKCASE, Direction.NORTH);
					for (int x = x0 + 4; x < x0 + 18; x += 4) {
						furn(x, y, z1 - 6, ModBlocks.DINING_TABLE, Direction.NORTH);
						furn(x, y, z1 - 7, ModBlocks.OAK_CHAIR, Direction.SOUTH);
						furn(x, y, z1 - 5, ModBlocks.OAK_CHAIR, Direction.NORTH);
					}
					rug(x0 + 22, hz1 + 3, x1 - 5, z1 - 2, y, Blocks.LIGHT_BLUE_CARPET);
					for (int x = x0 + 24; x < x1 - 5; x += 3) furn(x, y, z1 - 3, ModBlocks.SCHOOL_DESK, Direction.NORTH);
				}
			}
			stairsUp(x1 - 3, z0 + 19, g, stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH), Blocks.POLISHED_ANDESITE.getDefaultState());
		}

		/**
		 * What makes Casper High look like a school from outside: rows of classroom windows, a clock tower over the
		 * entrance, AMITY HIGH SCHOOL lettering, a yellow school bus in the bus loop, a bike rack, hedges, and trophy
		 * cases by the front doors.
		 */
		void schoolExtras(int x0, int z0) {
			int x1 = x0 + 39, z1 = z0 + 29;
			BlockState glass = Blocks.GLASS_PANE.getDefaultState(), brick = Blocks.BRICKS.getDefaultState(), trim = Blocks.STONE_BRICKS.getDefaultState();
			// classroom windows in long rows (brick posts every fourth block)
			for (int f = 0; f < 2; f++) {
				int y = g + f * 5 + 1;
				for (int x = x0 + 2; x <= x1 - 2; x++) {
					boolean post = Math.floorMod(x - x0, 4) == 1 || x == x0 + 19 || x == x0 + 25;
					BlockState b = post ? brick : glass;
					if (!(f == 0 && x >= x0 + 17 && x <= x0 + 22)) fill(x, y, z1, x, y + 1, z1, b);
					fill(x, y, z0, x, y + 1, z0, b);
				}
				for (int z = z0 + 2; z <= z1 - 2; z++) {
					boolean post = Math.floorMod(z - z0, 4) == 1 || z == z0 + 12 || z == z0 + 17;
					fill(x0, y, z, x0, y + 1, z, post ? brick : glass);
					fill(x1, y, z, x1, y + 1, z, post ? brick : glass);
				}
				// a stone sill under each row
				fill(x0 + 1, y - 1, z1, x1 - 1, y - 1, z1, f == 0 ? brick : trim);
			}
			doubleDoor(x0 + 19, g, z1, Direction.SOUTH, Blocks.OAK_DOOR);
			fill(x0 + 17, g, z1, x0 + 18, g + 2, z1, trim);
			fill(x0 + 21, g, z1, x0 + 22, g + 2, z1, trim);
			fill(x0 + 19, g + 2, z1, x0 + 20, g + 2, z1, Blocks.GLASS.getDefaultState());   // transom window over the doors
			// a proper school sign across the front of the porch: a green board in a white frame, lit lettering
			BlockState frame = Blocks.SMOOTH_QUARTZ.getDefaultState(), board = Blocks.GREEN_CONCRETE.getDefaultState();
			fill(x0 + 13, g + 4, z1 + 4, x0 + 26, g + 4, z1 + 4, frame);
			fill(x0 + 13, g + 5, z1 + 4, x0 + 13, g + 6, z1 + 4, frame);
			fill(x0 + 26, g + 5, z1 + 4, x0 + 26, g + 6, z1 + 4, frame);
			fill(x0 + 14, g + 5, z1 + 4, x0 + 25, g + 6, z1 + 4, board);
			set(x0 + 14, g + 6, z1 + 4, Blocks.SEA_LANTERN.getDefaultState());
			set(x0 + 25, g + 6, z1 + 4, Blocks.SEA_LANTERN.getDefaultState());
			fill(x0 + 13, g + 4, z1 + 5, x0 + 26, g + 6, z1 + 5, Blocks.AIR.getDefaultState());
			sign(Blocks.DARK_OAK_WALL_SIGN, DyeColor.WHITE, x0 + 18, g + 6, z1 + 5, Direction.SOUTH, "", "CASPER", "", "");
			sign(Blocks.DARK_OAK_WALL_SIGN, DyeColor.WHITE, x0 + 19, g + 6, z1 + 5, Direction.SOUTH, "", "HIGH", "", "");
			sign(Blocks.DARK_OAK_WALL_SIGN, DyeColor.WHITE, x0 + 20, g + 6, z1 + 5, Direction.SOUTH, "", "SCHOOL", "", "");
			sign(Blocks.DARK_OAK_WALL_SIGN, DyeColor.WHITE, x0 + 21, g + 6, z1 + 5, Direction.SOUTH, "", "Est. 1965", "", "");
			sign(Blocks.DARK_OAK_WALL_SIGN, DyeColor.LIME, x0 + 19, g + 5, z1 + 5, Direction.SOUTH, "", "Home of the", "", "");
			sign(Blocks.DARK_OAK_WALL_SIGN, DyeColor.LIME, x0 + 20, g + 5, z1 + 5, Direction.SOUTH, "", "Ghost Hunters!", "", "");
			// and a brick monument sign on the front lawn, with lamps on its posts and flowers along the bottom
			int mx = x0 + 10, mz = z1 + 8;
			fill(mx, g, mz, mx + 6, g, mz, trim);
			fill(mx, g + 1, mz, mx, g + 3, mz, brick);
			fill(mx + 6, g + 1, mz, mx + 6, g + 3, mz, brick);
			fill(mx + 1, g + 1, mz, mx + 5, g + 2, mz, board);
			fill(mx + 1, g + 3, mz, mx + 5, g + 3, mz, slab(Blocks.STONE_BRICK_SLAB, false));
			set(mx, g + 4, mz, Blocks.LANTERN.getDefaultState());
			set(mx + 6, g + 4, mz, Blocks.LANTERN.getDefaultState());
			for (int side = 0; side < 2; side++) {
				int sz = side == 0 ? mz + 1 : mz - 1;
				Direction face = side == 0 ? Direction.SOUTH : Direction.NORTH;
				int a = side == 0 ? 1 : 5, step = side == 0 ? 1 : -1;   // read left to right from either side
				sign(Blocks.DARK_OAK_WALL_SIGN, DyeColor.WHITE, mx + a + step, g + 2, sz, face, "", "CASPER", "", "");
				sign(Blocks.DARK_OAK_WALL_SIGN, DyeColor.WHITE, mx + a + 2 * step, g + 2, sz, face, "", "HIGH", "", "");
				sign(Blocks.DARK_OAK_WALL_SIGN, DyeColor.WHITE, mx + a + 3 * step, g + 2, sz, face, "", "SCHOOL", "", "");
				sign(Blocks.DARK_OAK_WALL_SIGN, DyeColor.LIME, mx + a + 2 * step, g + 1, sz, face, "Go Ghost", "Hunters!", "", "");
			}
			for (int x = mx; x <= mx + 6; x++) {
				set(x, g - 1, mz + 2, Blocks.GRASS_BLOCK.getDefaultState());
				set(x, g, mz + 2, (x % 2 == 0 ? Blocks.POPPY : Blocks.DANDELION).getDefaultState());
			}
			// clock tower over the entrance
			int t = g + 10;
			fill(x0 + 17, t, z1 - 5, x0 + 22, t + 5, z1, brick);
			walls(x0 + 17, t, z1 - 5, x0 + 22, t, z1, trim);
			walls(x0 + 17, t + 5, z1 - 5, x0 + 22, t + 5, z1, trim);
			fill(x0 + 18, t + 1, z1, x0 + 21, t + 4, z1, Blocks.WHITE_CONCRETE.getDefaultState());
			for (int[] tick : new int[][]{{18, 4}, {21, 4}, {18, 1}, {21, 1}}) set(x0 + tick[0], t + tick[1], z1, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
			set(x0 + 19, t + 3, z1, Blocks.BLACK_CONCRETE.getDefaultState());   // hands at three o'clock
			set(x0 + 19, t + 2, z1, Blocks.BLACK_CONCRETE.getDefaultState());
			set(x0 + 20, t + 2, z1, Blocks.BLACK_CONCRETE.getDefaultState());
			for (int i = 0; i < 3; i++) {
				for (int x = x0 + 17 + i; x <= x0 + 22 - i; x++) {
					set(x, t + 6 + i, z1 - 5 + i, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH));
					set(x, t + 6 + i, z1 - i, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH));
				}
				for (int z = z1 - 4 + i; z <= z1 - 1 - i; z++) {
					set(x0 + 17 + i, t + 6 + i, z, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST));
					set(x0 + 22 - i, t + 6 + i, z, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST));
				}
			}
			fill(x0 + 19, t + 8, z1 - 3, x0 + 20, t + 8, z1 - 2, Blocks.DEEPSLATE_TILES.getDefaultState());
			set(x0 + 19, t + 9, z1 - 3, Blocks.LIGHTNING_ROD.getDefaultState());
			// hedges along the front
			for (int x = x0; x <= x1; x++) {
				if (x >= x0 + 12 && x <= x0 + 27) continue;
				set(x, g, z1 + 1, Blocks.AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
			}
			// bike rack
			fill(x0 + 5, g, z1 + 4, x0 + 9, g, z1 + 4, Blocks.IRON_BARS.getDefaultState());
			// bus loop with a yellow school bus
			int bx = x0 + 28, bz = z1 + 7;
			fill(bx - 1, g - 1, bz - 1, bx + 11, g - 1, bz + 4, Blocks.BLACK_CONCRETE.getDefaultState());
			for (int x = bx - 1; x <= bx + 11; x += 2) set(x, g - 1, bz + 4, Blocks.YELLOW_CONCRETE.getDefaultState());
			bus(bx, bz);
			// trophy cases either side of the front doors (inside)
			for (int x : new int[]{x0 + 15, x0 + 16, x0 + 23, x0 + 24}) {
				set(x, g, z1 - 1, Blocks.POLISHED_DIORITE.getDefaultState());
				set(x, g + 1, z1 - 1, (x % 2 == 0 ? Blocks.GOLD_BLOCK : Blocks.EMERALD_BLOCK).getDefaultState());
				set(x, g + 2, z1 - 1, Blocks.POLISHED_DIORITE.getDefaultState());
				set(x, g + 1, z1 - 2, Blocks.GLASS_PANE.getDefaultState());
			}
			sign(x0 + 19, g + 3, z1 - 1, Direction.NORTH, true, "", "GO GHOST", "HUNTERS!", "");
		}

		/** A yellow school bus, 10 long and 3 wide, pointing east. */
		void bus(int bx, int bz) {
			BlockState yellow = Blocks.YELLOW_CONCRETE.getDefaultState(), black = Blocks.BLACK_CONCRETE.getDefaultState();
			fill(bx, g, bz, bx + 9, g + 2, bz + 2, yellow);
			fill(bx, g + 3, bz, bx + 9, g + 3, bz + 2, slab(Blocks.SMOOTH_STONE_SLAB, false));
			for (int x = bx + 1; x <= bx + 8; x++) if (x != bx + 5) {
				set(x, g + 2, bz, Blocks.BLACK_STAINED_GLASS.getDefaultState());
				set(x, g + 2, bz + 2, Blocks.BLACK_STAINED_GLASS.getDefaultState());
			}
			fill(bx + 9, g + 2, bz, bx + 9, g + 2, bz + 2, Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());   // windshield
			fill(bx, g + 2, bz + 1, bx, g + 2, bz + 1, Blocks.BLACK_STAINED_GLASS.getDefaultState());
			fill(bx, g, bz, bx + 9, g, bz, black);
			fill(bx, g, bz + 2, bx + 9, g, bz + 2, black);
			for (int x : new int[]{bx + 1, bx + 7}) for (int z : new int[]{bz, bz + 2}) set(x, g, z, Blocks.COAL_BLOCK.getDefaultState());
			set(bx + 9, g + 1, bz, Blocks.SEA_LANTERN.getDefaultState());
			set(bx + 9, g + 1, bz + 2, Blocks.SEA_LANTERN.getDefaultState());
			set(bx + 9, g, bz + 1, black);
			set(bx + 6, g + 1, bz, Blocks.RED_CONCRETE.getDefaultState());   // stop sign arm
			sign(bx + 4, g + 1, bz + 3, Direction.SOUTH, true, "", "SCHOOL BUS", "Casper High", "");
		}

		/** The sports field next to the school. */
		void field(int x0, int z0) {
			fill(x0 + 2, g - 1, z0 + 6, x0 + 40, g - 1, z0 + 32, Blocks.GREEN_CONCRETE.getDefaultState());
			fill(x0 + 2, g - 1, z0 + 19, x0 + 40, g - 1, z0 + 19, Blocks.WHITE_CONCRETE.getDefaultState());
			for (int z = z0 + 6; z <= z0 + 32; z++) {
				set(x0 + 1, g - 1, z, Blocks.RED_TERRACOTTA.getDefaultState());
				set(x0 + 41, g - 1, z, Blocks.RED_TERRACOTTA.getDefaultState());
			}
			for (int zz : new int[]{z0 + 6, z0 + 32}) {
				fill(x0 + 19, g, zz, x0 + 19, g + 3, zz, Blocks.WHITE_CONCRETE.getDefaultState());
				fill(x0 + 23, g, zz, x0 + 23, g + 3, zz, Blocks.WHITE_CONCRETE.getDefaultState());
				fill(x0 + 19, g + 3, zz, x0 + 23, g + 3, zz, Blocks.WHITE_CONCRETE.getDefaultState());
			}
			for (int i = 0; i < 3; i++) fill(x0 + 8, g + i, z0 + 2 + i, x0 + 34, g + i, z0 + 2 + i, stairs(Blocks.OAK_STAIRS, Direction.NORTH));
		}

		/** Burger Boo: booths by the windows, a counter with stools, a kitchen and a giant burger on the roof. */
		void diner(int x0, int z0) {
			int x1 = x0 + 21, z1 = z0 + 15;
			shell(x0, z0, 22, 16, 1, Blocks.RED_CONCRETE.getDefaultState(), Blocks.WHITE_CONCRETE.getDefaultState(),
					Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.GLASS_PANE.getDefaultState());
			for (int x = x0 + 1; x < x1; x++) for (int z = z0 + 1; z < z1; z++)
				set(x, g - 1, z, (x + z) % 2 == 0 ? Blocks.BLACK_CONCRETE.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState());
			doubleDoor(x0 + 10, g, z1, Direction.SOUTH, Blocks.OAK_DOOR);
			for (int x : new int[]{x0 + 1, x0 + 4, x0 + 7, x0 + 14, x0 + 17, x0 + 20}) {
				furn(x, g, z1 - 1, ModBlocks.SOFA, Direction.NORTH);
				furn(x, g, z1 - 2, ModBlocks.DINING_TABLE, Direction.NORTH);
				furn(x, g, z1 - 3, ModBlocks.SOFA, Direction.SOUTH);
			}
			for (int x = x0 + 3; x <= x0 + 18; x++) {
				furn(x, g, z0 + 6, ModBlocks.KITCHEN_COUNTER, Direction.SOUTH);
				if (x % 2 == 0) furn(x, g, z0 + 7, ModBlocks.BAR_STOOL, Direction.NORTH);
			}
			set(x0 + 6, g + 1, z0 + 6, Blocks.CAKE.getDefaultState());
			for (int x = x0 + 3; x < x1 - 1; x++) set(x, g, z0 + 1, (x % 3 == 0 ? Blocks.SMOKER : Blocks.FURNACE).getDefaultState()
					.with(net.minecraft.block.AbstractFurnaceBlock.FACING, Direction.SOUTH));
			furn(x0 + 1, g, z0 + 2, ModBlocks.FRIDGE, Direction.EAST);
			furn(x0 + 1, g, z0 + 3, ModBlocks.FRIDGE, Direction.EAST);
			fill(x0 + 8, g, z0 + 3, x0 + 13, g, z0 + 3, slab(Blocks.SMOOTH_STONE_SLAB, true));
			int r0 = g + 6;
			fill(x0 + 8, r0, z0 + 5, x0 + 13, r0, z0 + 10, Blocks.ORANGE_TERRACOTTA.getDefaultState());
			fill(x0 + 8, r0 + 1, z0 + 5, x0 + 13, r0 + 1, z0 + 10, Blocks.BROWN_CONCRETE.getDefaultState());
			fill(x0 + 7, r0 + 2, z0 + 4, x0 + 14, r0 + 2, z0 + 11, Blocks.LIME_CONCRETE.getDefaultState());
			fill(x0 + 8, r0 + 3, z0 + 5, x0 + 13, r0 + 3, z0 + 10, Blocks.YELLOW_CONCRETE.getDefaultState());
			fill(x0 + 8, r0 + 4, z0 + 5, x0 + 13, r0 + 5, z0 + 10, Blocks.ORANGE_TERRACOTTA.getDefaultState());
			sign(x0 + 9, g + 3, z1 + 1, Direction.SOUTH, true, "", "NASTY BURGER", "Open late.", "Very late.");
			for (int x = x0 + 2; x < x1 - 1; x += 5) {
				furn(x, g, z1 + 3, ModBlocks.DINING_TABLE, Direction.NORTH);
				furn(x, g, z1 + 2, ModBlocks.OAK_CHAIR, Direction.SOUTH);
				furn(x, g, z1 + 4, ModBlocks.OAK_CHAIR, Direction.NORTH);
			}
		}

		/** Corner store: shelves, fridges and a checkout. */
		void store(int x0, int z0) {
			int x1 = x0 + 15, z1 = z0 + 15;
			shell(x0, z0, 16, 16, 1, Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState(), Blocks.WHITE_CONCRETE.getDefaultState(),
					Blocks.SMOOTH_QUARTZ.getDefaultState(), Blocks.GLASS_PANE.getDefaultState());
			doubleDoor(x0 + 7, g, z1, Direction.SOUTH, Blocks.OAK_DOOR);
			for (int x = x0 + 2; x <= x0 + 12; x += 4) for (int z = z0 + 3; z <= z0 + 9; z++) {
				set(x, g, z, Blocks.BARREL.getDefaultState());
				set(x, g + 1, z, rnd.nextBoolean() ? Blocks.COMPOSTER.getDefaultState() : Blocks.BOOKSHELF.getDefaultState());
			}
			for (int x = x0 + 1; x < x1; x++) furn(x, g, z0 + 1, ModBlocks.FRIDGE, Direction.SOUTH);
			for (int x = x0 + 11; x <= x0 + 14; x++) furn(x, g, z1 - 3, ModBlocks.KITCHEN_COUNTER, Direction.SOUTH);
			furn(x0 + 13, g, z1 - 2, ModBlocks.BAR_STOOL, Direction.NORTH);
			sign(x0 + 6, g + 3, z1 + 1, Direction.SOUTH, true, "", "QUICK STOP", "Snacks, soda,", "ghost repellent");
		}

		/** The library: rows of bookcases, reading tables with lamps, and a cosy corner. */
		void library(int x0, int z0) {
			int x1 = x0 + 29, z1 = z0 + 23;
			shell(x0, z0, 30, 24, 2, Blocks.STONE_BRICKS.getDefaultState(), Blocks.CHISELED_STONE_BRICKS.getDefaultState(),
					Blocks.DARK_OAK_PLANKS.getDefaultState(), Blocks.GLASS_PANE.getDefaultState());
			doubleDoor(x0 + 14, g, z1, Direction.SOUTH, Blocks.DARK_OAK_DOOR);
			for (int z = z1 + 1; z <= -6; z++) fill(x0 + 14, g - 1, z, x0 + 15, g - 1, z, Blocks.SMOOTH_STONE.getDefaultState());
			sign(x0 + 13, g + 3, z1 + 1, Direction.SOUTH, true, "", "AMITY PARK", "PUBLIC LIBRARY", "Shh!");
			for (int f = 0; f < 2; f++) {
				int y = g + f * 5;
				for (int x = x0 + 2; x < x0 + 12; x += 3) for (int z = z0 + 2; z < z0 + 12; z++) {
					furn(x, y, z, ModBlocks.BOOKCASE, Direction.WEST);
					furn(x + 1, y, z, ModBlocks.BOOKCASE, Direction.EAST);
				}
				for (int x = x0 + 16; x < x1 - 4; x += 4) for (int z = z0 + 3; z < z0 + 12; z += 4) {
					furn(x, y, z, ModBlocks.DINING_TABLE, Direction.NORTH);
					furn(x + 1, y, z, ModBlocks.DINING_TABLE, Direction.NORTH);
					furn(x, y, z - 1, ModBlocks.OAK_CHAIR, Direction.SOUTH);
					furn(x + 1, y, z + 1, ModBlocks.OAK_CHAIR, Direction.NORTH);
					set(x, y + 1, z, Blocks.LANTERN.getDefaultState());
				}
				rug(x0 + 3, z0 + 15, x0 + 10, z1 - 2, y, Blocks.RED_CARPET);
				for (int x = x0 + 4; x <= x0 + 8; x++) furn(x, y, z1 - 1, ModBlocks.SOFA, Direction.NORTH);
				furn(x0 + 6, y, z0 + 17, ModBlocks.COFFEE_TABLE, Direction.NORTH);
				furn(x0 + 3, y, z1 - 1, ModBlocks.FLOOR_LAMP, Direction.NORTH);
				furn(x0 + 9, y, z1 - 1, ModBlocks.FLOOR_LAMP, Direction.NORTH);
				for (int x = x0 + 18; x < x1 - 4; x++) furn(x, y, z1 - 1, ModBlocks.BOOKCASE, Direction.NORTH);
			}
			for (int x = x0 + 18; x <= x0 + 22; x++) furn(x, g, z0 + 15, ModBlocks.KITCHEN_COUNTER, Direction.SOUTH);
			furn(x0 + 20, g, z0 + 14, ModBlocks.OAK_CHAIR, Direction.SOUTH);
			stairsUp(x1 - 3, z0 + 14, g, stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH), Blocks.DARK_OAK_PLANKS.getDefaultState());
		}

		/** Town hall: a columned front, a lobby with a reception desk, offices, and a council chamber upstairs. */
		void townHall(int x0, int z0) {
			int x1 = x0 + 35, z1 = z0 + 27;
			shell(x0, z0, 36, 28, 2, Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.QUARTZ_BLOCK.getDefaultState(),
					Blocks.POLISHED_DIORITE.getDefaultState(), Blocks.GLASS_PANE.getDefaultState());
			for (int x = x0 + 4; x <= x1 - 4; x += 4) fill(x, g, z1 + 3, x, g + 8, z1 + 3, Blocks.QUARTZ_PILLAR.getDefaultState());
			fill(x0 + 2, g + 9, z1 + 1, x1 - 2, g + 9, z1 + 4, Blocks.SMOOTH_QUARTZ.getDefaultState());
			fill(x0 + 2, g - 1, z1 + 1, x1 - 2, g - 1, z1 + 4, Blocks.SMOOTH_QUARTZ.getDefaultState());
			doubleDoor(x0 + 17, g, z1, Direction.SOUTH, Blocks.SPRUCE_DOOR);
			for (int z = z1 + 5; z <= -6; z++) fill(x0 + 16, g - 1, z, x0 + 19, g - 1, z, Blocks.SMOOTH_STONE.getDefaultState());
			sign(x0 + 16, g + 4, z1 + 1, Direction.SOUTH, true, "", "TOWN HALL", "Mayor's office", "Ghosts: no entry");
			rug(x0 + 14, z0 + 10, x0 + 21, z1 - 1, g, Blocks.RED_CARPET);
			for (int x = x0 + 13; x <= x0 + 22; x++) furn(x, g, z0 + 8, ModBlocks.KITCHEN_COUNTER, Direction.SOUTH);
			furn(x0 + 17, g, z0 + 7, ModBlocks.OAK_CHAIR, Direction.SOUTH);
			furn(x0 + 15, g, z0 + 7, ModBlocks.OAK_CHAIR, Direction.SOUTH);
			for (int z = z0 + 14; z <= z1 - 3; z += 4) {
				furn(x0 + 12, g, z, ModBlocks.SOFA, Direction.EAST);
				furn(x0 + 23, g, z, ModBlocks.SOFA, Direction.WEST);
				furn(x0 + 12, g, z + 1, ModBlocks.POTTED_PLANT, Direction.EAST);
				furn(x0 + 23, g, z + 1, ModBlocks.POTTED_PLANT, Direction.WEST);
			}
			fill(x0 + 10, g, z0 + 1, x0 + 10, g + 3, z1 - 1, Blocks.SMOOTH_SANDSTONE.getDefaultState());
			fill(x0 + 25, g, z0 + 1, x0 + 25, g + 3, z1 - 1, Blocks.SMOOTH_SANDSTONE.getDefaultState());
			fill(x0 + 10, g, z0 + 14, x0 + 10, g + 1, z0 + 14, Blocks.AIR.getDefaultState());
			fill(x0 + 25, g, z0 + 14, x0 + 25, g + 1, z0 + 14, Blocks.AIR.getDefaultState());
			for (int side = 0; side < 2; side++) {
				int ox = side == 0 ? x0 + 2 : x0 + 27;
				for (int z = z0 + 4; z <= z1 - 4; z += 6) {
					if (side == 0 && z >= z0 + 18) continue;   // the stairs are here
					furn(ox + 3, g, z, ModBlocks.DINING_TABLE, Direction.NORTH);
					furn(ox + 4, g, z, ModBlocks.DINING_TABLE, Direction.NORTH);
					furn(ox + 3, g, z - 1, ModBlocks.OAK_CHAIR, Direction.SOUTH);
					furn(ox + 4, g, z + 1, ModBlocks.OAK_CHAIR, Direction.NORTH);
					furn(side == 0 ? ox - 1 : ox + 6, g, z, ModBlocks.BOOKCASE, side == 0 ? Direction.EAST : Direction.WEST);
				}
			}
			int u = g + 5;
			rug(x0 + 4, z0 + 4, x1 - 4, z1 - 4, u, Blocks.BLUE_CARPET);
			for (int x = x0 + 8; x <= x1 - 8; x++) {
				furn(x, u, z0 + 13, ModBlocks.DINING_TABLE, Direction.NORTH);
				furn(x, u, z0 + 14, ModBlocks.DINING_TABLE, Direction.NORTH);
				if (x % 2 == 0) {
					furn(x, u, z0 + 12, ModBlocks.OAK_CHAIR, Direction.SOUTH);
					furn(x, u, z0 + 15, ModBlocks.OAK_CHAIR, Direction.NORTH);
				}
			}
			for (int x = x0 + 3; x < x1 - 2; x += 2) furn(x, u, z0 + 1, ModBlocks.BOOKCASE, Direction.SOUTH);
			stairsUp(x0 + 2, z0 + 20, g, stairs(Blocks.QUARTZ_STAIRS, Direction.SOUTH), Blocks.POLISHED_DIORITE.getDefaultState());
			int t = g + 10;
			fill(x0 + 14, t, z0 + 10, x0 + 21, t + 7, z0 + 17, Blocks.SMOOTH_QUARTZ.getDefaultState());
			fill(x0 + 16, t + 4, z0 + 9, x0 + 19, t + 6, z0 + 9, Blocks.WHITE_STAINED_GLASS.getDefaultState());
			set(x0 + 17, t + 5, z0 + 9, Blocks.BLACK_CONCRETE.getDefaultState());
			fill(x0 + 16, t + 4, z0 + 18, x0 + 19, t + 6, z0 + 18, Blocks.WHITE_STAINED_GLASS.getDefaultState());
			fill(x0 + 15, t + 8, z0 + 11, x0 + 20, t + 8, z0 + 16, Blocks.OXIDIZED_CUT_COPPER.getDefaultState());
			fill(x0 + 16, t + 9, z0 + 12, x0 + 19, t + 9, z0 + 15, Blocks.OXIDIZED_CUT_COPPER.getDefaultState());
			fill(x0 + 17, t + 10, z0 + 13, x0 + 18, t + 11, z0 + 14, Blocks.OXIDIZED_COPPER.getDefaultState());
			set(x0 + 17, t + 12, z0 + 13, Blocks.LIGHTNING_ROD.getDefaultState());
		}

		/** Apartment block: three floors, each with two furnished flats off a corridor. */
		void apartments(int x0, int z0) {
			int z1 = z0 + 21;
			shell(x0, z0, 34, 22, 3, Blocks.TERRACOTTA.getDefaultState(), Blocks.BROWN_TERRACOTTA.getDefaultState(),
					Blocks.OAK_PLANKS.getDefaultState(), Blocks.GLASS_PANE.getDefaultState());
			doubleDoor(x0 + 16, g, z0, Direction.NORTH, Blocks.OAK_DOOR);
			for (int z = 6; z < z0; z++) fill(x0 + 16, g - 1, z, x0 + 17, g - 1, z, Blocks.SMOOTH_STONE.getDefaultState());
			sign(x0 + 15, g + 3, z0 - 1, Direction.NORTH, true, "", "ELM COURT", "Apartments", "");
			for (int f = 0; f < 3; f++) {
				int y = g + f * 5;
				fill(x0 + 15, y, z0 + 1, x0 + 15, y + 3, z1 - 1, Blocks.WHITE_TERRACOTTA.getDefaultState());
				fill(x0 + 18, y, z0 + 1, x0 + 18, y + 3, z1 - 1, Blocks.WHITE_TERRACOTTA.getDefaultState());
				fill(x0 + 15, y, z0 + 4, x0 + 15, y + 1, z0 + 4, Blocks.AIR.getDefaultState());
				fill(x0 + 18, y, z0 + 4, x0 + 18, y + 1, z0 + 4, Blocks.AIR.getDefaultState());
				rug(x0 + 16, z0 + 1, x0 + 17, z1 - 1, y, Blocks.BROWN_CARPET);
				for (int side = 0; side < 2; side++) {
					int ax = side == 0 ? x0 + 1 : x0 + 19;
					int wallX = side == 0 ? ax : ax + 13;
					Direction in = side == 0 ? Direction.EAST : Direction.WEST;
					rug(ax + 3, z0 + 3, ax + 10, z0 + 8, y, side == 0 ? Blocks.CYAN_CARPET : Blocks.ORANGE_CARPET);
					for (int x = ax + 5; x <= ax + 7; x++) furn(x, y, z0 + 9, ModBlocks.SOFA, Direction.NORTH);
					furn(ax + 6, y, z0 + 6, ModBlocks.COFFEE_TABLE, Direction.NORTH);
					furn(ax + 6, y, z0 + 1, ModBlocks.TELEVISION, Direction.SOUTH);
					furn(ax + 3, y, z0 + 9, ModBlocks.FLOOR_LAMP, Direction.NORTH);
					furn(wallX, y, z0 + 1, ModBlocks.POTTED_PLANT, in);
					for (int z = z0 + 11; z <= z0 + 14; z++) furn(wallX, y, z, ModBlocks.KITCHEN_COUNTER, in);
					furn(wallX, y, z0 + 15, ModBlocks.FRIDGE, in);
					furn(ax + 6, y, z0 + 13, ModBlocks.DINING_TABLE, Direction.NORTH);
					furn(ax + 5, y, z0 + 13, ModBlocks.OAK_CHAIR, Direction.EAST);
					furn(ax + 7, y, z0 + 13, ModBlocks.OAK_CHAIR, Direction.WEST);
					fill(ax, y, z0 + 17, ax + 13, y + 3, z0 + 17, Blocks.WHITE_TERRACOTTA.getDefaultState());
					fill(ax + 6, y, z0 + 17, ax + 6, y + 1, z0 + 17, Blocks.AIR.getDefaultState());
					bed(ax + 2, y, z1 - 2, Direction.SOUTH, side == 0 ? Blocks.BLUE_BED : Blocks.RED_BED);
					furn(ax + 10, y, z1 - 1, ModBlocks.BOOKCASE, Direction.NORTH);
					furn(ax + 4, y, z1 - 1, ModBlocks.FLOOR_LAMP, Direction.NORTH);
				}
				if (f < 2) stairsUp(x0 + 16, z0 + 8, y, stairs(Blocks.OAK_STAIRS, Direction.SOUTH), Blocks.OAK_PLANKS.getDefaultState());
			}
		}

		/** The park: paths, a fountain, a pond, trees, benches and a little playground. */
		void park(int x0, int z0) {
			int x1 = x0 + 92, z1 = z0 + 42;
			int fx = x0 + 30, fz = (z0 + z1) / 2;
			for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
				boolean path = Math.abs(z - fz) <= 1 || Math.abs(x - fx) <= 1 || Math.abs(x - (x0 + 70)) <= 1;
				set(x, g - 1, z, path ? Blocks.GRAVEL.getDefaultState() : lawn);
				if (!path && get(x, g, z).isAir()) {
					int r = rnd.nextInt(20);
					if (r == 0) set(x, g, z, rnd.nextBoolean() ? Blocks.POPPY.getDefaultState() : Blocks.DANDELION.getDefaultState());
					else if (r < 4 && lawn.isOf(Blocks.GRASS_BLOCK)) set(x, g, z, Blocks.SHORT_GRASS.getDefaultState());
				}
			}
			fill(fx - 5, g - 1, fz - 5, fx + 5, g, fz + 5, Blocks.STONE_BRICKS.getDefaultState());
			fill(fx - 4, g, fz - 4, fx + 4, g, fz + 4, Blocks.WATER.getDefaultState());
			fill(fx, g, fz, fx, g + 4, fz, Blocks.QUARTZ_PILLAR.getDefaultState());
			set(fx, g + 5, fz, Blocks.SEA_LANTERN.getDefaultState());
			fill(fx - 1, g + 3, fz - 1, fx + 1, g + 3, fz + 1, slab(Blocks.QUARTZ_SLAB, false));
			set(fx, g + 3, fz, Blocks.QUARTZ_PILLAR.getDefaultState());
			int px = x0 + 55, pz = z0 + 12;
			for (int dx = -7; dx <= 7; dx++) for (int dz = -5; dz <= 5; dz++) {
				double d = (dx * dx) / 49.0 + (dz * dz) / 25.0;
				if (d < 1) {
					set(px + dx, g - 1, pz + dz, Blocks.WATER.getDefaultState());
					set(px + dx, g - 2, pz + dz, d < 0.5 ? Blocks.WATER.getDefaultState() : Blocks.SAND.getDefaultState());
					set(px + dx, g - 3, pz + dz, Blocks.SAND.getDefaultState());
					if (rnd.nextInt(14) == 0 && d < 0.7) set(px + dx, g, pz + dz, Blocks.LILY_PAD.getDefaultState());
				} else if (d < 1.4) set(px + dx, g - 1, pz + dz, Blocks.SAND.getDefaultState());
			}
			int qx = x0 + 80, qz = z0 + 30;
			fill(qx - 6, g - 1, qz - 6, qx + 6, g - 1, qz + 6, Blocks.SAND.getDefaultState());
			fill(qx - 4, g, qz, qx - 4, g + 3, qz, Blocks.RED_CONCRETE.getDefaultState());
			fill(qx - 4, g + 4, qz - 1, qx - 2, g + 4, qz + 1, Blocks.YELLOW_CONCRETE.getDefaultState());
			for (int i = 0; i < 4; i++) set(qx - 1 + i, g + 3 - i, qz, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
			fill(qx + 3, g, qz - 3, qx + 3, g + 3, qz - 3, Blocks.OAK_FENCE.getDefaultState());
			fill(qx + 3, g, qz + 3, qx + 3, g + 3, qz + 3, Blocks.OAK_FENCE.getDefaultState());
			fill(qx + 3, g + 4, qz - 3, qx + 3, g + 4, qz + 3, Blocks.OAK_PLANKS.getDefaultState());
			set(qx + 3, g + 3, qz - 1, Blocks.CHAIN.getDefaultState());
			set(qx + 3, g + 3, qz + 1, Blocks.CHAIN.getDefaultState());
			for (int i = 0; i < 28; i++) {
				int tx = x0 + 3 + rnd.nextInt(86), tz = z0 + 3 + rnd.nextInt(36);
				if (Math.abs(tx - fx) < 8 && Math.abs(tz - fz) < 8) continue;
				if (Math.abs(tx - px) < 10 && Math.abs(tz - pz) < 8) continue;
				if (Math.abs(tx - qx) < 9 && Math.abs(tz - qz) < 9) continue;
				if (Math.abs(tz - fz) <= 3 || Math.abs(tx - fx) <= 3 || Math.abs(tx - (x0 + 70)) <= 3) continue;
				tree(tx, tz);
			}
			for (int a = -28; a <= 28; a += 8) {
				if (Math.abs(a) < 8) continue;
				if (fx + a > x0 && fx + a < x1) furn(fx + a, g, fz + 2, ModBlocks.SOFA, Direction.NORTH);
				if (fz + a / 2 > z0 + 1 && fz + a / 2 < z1 - 1 && Math.abs(a / 2) > 5) furn(fx + 2, g, fz + a / 2, ModBlocks.SOFA, Direction.WEST);
			}
			sign(x0 + 1, g, z0 + 1, Direction.NORTH, false, "AMITY PARK", "Town Park", "Please don't feed", "the ghosts");
		}

		/** A two-storey family house with a garden, furnished inside. */
		void house(int x0, int z0, int style, boolean frontSouth) {
			BlockState[] walls = {Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState(), Blocks.WHITE_TERRACOTTA.getDefaultState(),
					Blocks.YELLOW_TERRACOTTA.getDefaultState(), Blocks.BIRCH_PLANKS.getDefaultState(), Blocks.PINK_TERRACOTTA.getDefaultState(),
					Blocks.LIME_TERRACOTTA.getDefaultState(), Blocks.SPRUCE_PLANKS.getDefaultState()};
			Block[] beds = {Blocks.RED_BED, Blocks.BLUE_BED, Blocks.GREEN_BED, Blocks.YELLOW_BED, Blocks.PURPLE_BED, Blocks.CYAN_BED};
			Block[] carpets = {Blocks.GRAY_CARPET, Blocks.BROWN_CARPET, Blocks.LIGHT_BLUE_CARPET, Blocks.RED_CARPET, Blocks.GREEN_CARPET};
			BlockState wall = walls[style % walls.length];
			Block carpet = carpets[style % carpets.length];
			int x1 = x0 + 15, z1 = z0 + 13;
			BlockState floor = Blocks.OAK_PLANKS.getDefaultState();
			fill(x0, g, z0, x1, g + 18, z1, Blocks.AIR.getDefaultState());
			fill(x0, g - 1, z0, x1, g - 1, z1, floor);
			walls(x0, g, z0, x1, g + 8, z1, wall);
			for (int[] cn : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) fill(cn[0], g, cn[1], cn[0], g + 8, cn[1], Blocks.STRIPPED_OAK_LOG.getDefaultState());
			fill(x0 + 1, g + 4, z0 + 1, x1 - 1, g + 4, z1 - 1, floor);
			fill(x0, g + 4, z0, x1, g + 4, z0, Blocks.STRIPPED_OAK_LOG.getDefaultState());
			fill(x0, g + 4, z1, x1, g + 4, z1, Blocks.STRIPPED_OAK_LOG.getDefaultState());
			int frontZ = frontSouth ? z1 : z0;
			for (int lvl : new int[]{g + 1, g + 6}) {
				for (int x = x0 + 2; x < x1 - 1; x += 4) {
					for (int wz : new int[]{z0, z1}) {
						// leave the wall around the front door solid (the door goes in the middle at x0 + 7)
						if (lvl == g + 1 && wz == frontZ && x + 1 >= x0 + 5 && x <= x0 + 9) continue;
						fill(x, lvl, wz, x + 1, lvl + 1, wz, Blocks.GLASS_PANE.getDefaultState());
					}
				}
				for (int z = z0 + 3; z < z1 - 2; z += 4) {
					fill(x0, lvl, z, x0, lvl + 1, z, Blocks.GLASS_PANE.getDefaultState());
					fill(x1, lvl, z, x1, lvl + 1, z, Blocks.GLASS_PANE.getDefaultState());
				}
			}
			int dz = frontSouth ? z1 : z0;
			Direction out = frontSouth ? Direction.SOUTH : Direction.NORTH;
			door(x0 + 7, g, dz, out, Blocks.OAK_DOOR);
			int step = frontSouth ? 1 : -1;
			fill(x0 + 5, g - 1, dz + step, x0 + 9, g - 1, dz + step * 2, Blocks.SPRUCE_PLANKS.getDefaultState());
			fill(x0 + 5, g + 3, dz + step, x0 + 9, g + 3, dz + step * 2, slab(Blocks.SPRUCE_SLAB, false));
			fill(x0 + 5, g, dz + step * 2, x0 + 5, g + 2, dz + step * 2, Blocks.OAK_FENCE.getDefaultState());
			fill(x0 + 9, g, dz + step * 2, x0 + 9, g + 2, dz + step * 2, Blocks.OAK_FENCE.getDefaultState());
			int streetZ = frontSouth ? -57 : 57;
			for (int z = dz + step * 3; frontSouth ? z <= streetZ : z >= streetZ; z += step) set(x0 + 7, g - 1, z, Blocks.DIRT_PATH.getDefaultState());
			for (int x = x0; x <= x1; x++) if (x != x0 + 7) set(x, g, dz + step * 5, Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState());
			for (int x = x0 + 1; x <= x1 - 1; x += 2) if (Math.abs(x - x0 - 7) > 2) set(x, g, dz + step, rnd.nextBoolean() ? Blocks.ROSE_BUSH.getDefaultState() : Blocks.PEONY.getDefaultState());
			// ground floor: living room at the front, kitchen at the back
			int iz0 = z0 + 1, iz1 = z1 - 1;
			int front = frontSouth ? iz1 : iz0, back = frontSouth ? iz0 : iz1;
			int toBack = frontSouth ? -1 : 1;
			Direction faceBack = frontSouth ? Direction.NORTH : Direction.SOUTH, faceFront = faceBack.getOpposite();
			rug(x0 + 2, Math.min(front, front + toBack * 4), x0 + 6, Math.max(front, front + toBack * 4), g, carpet);
			for (int x = x0 + 3; x <= x0 + 5; x++) furn(x, g, front, ModBlocks.SOFA, faceBack);
			furn(x0 + 4, g, front + toBack * 2, ModBlocks.COFFEE_TABLE, Direction.NORTH);
			furn(x0 + 4, g, front + toBack * 4, ModBlocks.TELEVISION, faceFront);
			furn(x0 + 1, g, front, ModBlocks.FLOOR_LAMP, Direction.NORTH);
			furn(x0 + 1, g, front + toBack * 3, ModBlocks.BOOKCASE, Direction.EAST);
			for (int x = x1 - 6; x <= x1 - 1; x++) furn(x, g, back, ModBlocks.KITCHEN_COUNTER, faceFront);
			furn(x1 - 1, g, back, ModBlocks.FRIDGE, faceFront);
			set(x1 - 4, g, back, Blocks.SMOKER.getDefaultState().with(net.minecraft.block.AbstractFurnaceBlock.FACING, faceFront));
			int tz = back - toBack * 3;
			furn(x1 - 4, g, tz, ModBlocks.DINING_TABLE, Direction.NORTH);
			furn(x1 - 3, g, tz, ModBlocks.DINING_TABLE, Direction.NORTH);
			furn(x1 - 5, g, tz, ModBlocks.OAK_CHAIR, Direction.EAST);
			furn(x1 - 2, g, tz, ModBlocks.OAK_CHAIR, Direction.WEST);
			furn(x1 - 1, g, front, ModBlocks.POTTED_PLANT, Direction.NORTH);
			for (int x = x0 + 3; x < x1 - 1; x += 5) lamp(x, g + 3, z0 + 7);
			// stairs to the bedroom floor (along the west side of the middle)
			// five steps: the last one sits level with the upstairs floor so you can walk straight off it
			for (int i = 0; i < 5; i++) {
				int sz = back - toBack * (2 + i);
				set(x0 + 9, g + i, sz, stairs(Blocks.OAK_STAIRS, faceFront));
				if (i < 4) set(x0 + 9, g + 4, sz, Blocks.AIR.getDefaultState());
				set(x0 + 9, g + i + 1, sz, Blocks.AIR.getDefaultState());
				set(x0 + 9, g + i + 2, sz, Blocks.AIR.getDefaultState());
			}
			// upstairs: two bedrooms
			int u = g + 5;
			fill(x0 + 7, u, z0 + 1, x0 + 7, u + 2, z1 - 1, wall);
			fill(x0 + 7, u, z0 + 6, x0 + 7, u + 1, z0 + 7, Blocks.AIR.getDefaultState());
			bed(x0 + 2, u, z0 + 2, Direction.NORTH, beds[style % beds.length]);
			furn(x0 + 4, u, z0 + 1, ModBlocks.FLOOR_LAMP, Direction.NORTH);
			furn(x0 + 5, u, z1 - 1, ModBlocks.SCHOOL_DESK, Direction.SOUTH);
			furn(x0 + 5, u, z1 - 2, ModBlocks.OAK_CHAIR, Direction.SOUTH);
			furn(x0 + 1, u, z1 - 1, ModBlocks.BOOKCASE, Direction.EAST);
			rug(x0 + 2, z0 + 5, x0 + 5, z0 + 9, u, carpet);
			bed(x1 - 2, u, z1 - 2, Direction.SOUTH, beds[(style + 2) % beds.length]);
			bed(x1 - 4, u, z1 - 2, Direction.SOUTH, beds[(style + 3) % beds.length]);
			set(x1 - 1, u, z0 + 1, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.WEST));
			furn(x1 - 1, u, z0 + 3, ModBlocks.POTTED_PLANT, Direction.WEST);
			for (int x = x0 + 3; x < x1 - 1; x += 5) lamp(x, u + 2, z0 + 7);
			// gabled roof
			fill(x0 + 1, g + 8, z0 + 1, x1 - 1, g + 8, z1 - 1, floor);
			for (int i = 0; i <= 8; i++) {
				if (z0 - 1 + i > z1 + 1 - i) break;
				for (int x = x0 - 1; x <= x1 + 1; x++) {
					set(x, g + 9 + i, z0 - 1 + i, stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));
					set(x, g + 9 + i, z1 + 1 - i, stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
				}
				if (z0 + i <= z1 - i) {
					fill(x0, g + 9 + i, z0 + i, x0, g + 9 + i, z1 - i, wall);
					fill(x1, g + 9 + i, z0 + i, x1, g + 9 + i, z1 - i, wall);
				}
			}
			fill(x1 - 3, g + 12, z0 + 6, x1 - 3, g + 16, z0 + 6, Blocks.BRICKS.getDefaultState());
		}
	}
}
