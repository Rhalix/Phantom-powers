package com.phantompowers.world;

import com.phantompowers.ModEntities;
import com.phantompowers.entity.RoomGhostEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;

/** Builds the unique room behind a Ghost Zone door. Same index + world seed = same room. */
public final class RoomBuilder {
	public record Result(String name, Vec3d entry, float yaw) {}

	private static final String[] MOODS = {
			"Whispering", "Forgotten", "Restless", "Hollow", "Silent", "Endless", "Crooked", "Lonely",
			"Flickering", "Ancient", "Twisted", "Shivering", "Lost", "Midnight", "Glowing", "Shadowed"};

	private interface Theme {
		String name();

		void build(Ctx c);
	}

	/** Small helper with room-relative coordinates. Interior is x 0..W-1, y 1..H, z 0..D-1. */
	static final class Ctx {
		final ServerWorld w;
		final BlockPos o;
		final Random r;
		int W, D, H;

		Ctx(ServerWorld w, BlockPos o, Random r) {
			this.w = w;
			this.o = o;
			this.r = r;
		}

		void set(int x, int y, int z, BlockState s) {
			w.setBlockState(o.add(x, y, z), s, Block.NOTIFY_LISTENERS);
		}

		void setConnected(int x, int y, int z, BlockState s) {
			BlockPos p = o.add(x, y, z);
			w.setBlockState(p, Block.postProcessState(s, w, p), Block.NOTIFY_ALL);
		}

		BlockState get(int x, int y, int z) {
			return w.getBlockState(o.add(x, y, z));
		}

		boolean isAir(int x, int y, int z) {
			return get(x, y, z).isAir();
		}

		/** Keeps the walkway from the entrance clear. */
		boolean inPath(int x, int z) {
			return Math.abs(x - W / 2) <= 1 && z <= 2;
		}

		void shell(BlockState floor, BlockState wall, BlockState ceiling) {
			for (int x = -1; x <= W; x++) {
				for (int z = -1; z <= D; z++) {
					for (int y = -1; y <= H + 1; y++) {
						boolean edge = x < 0 || x >= W || z < 0 || z >= D;
						BlockState s;
						if (y <= 0) s = floor;
						else if (y == H + 1) s = ceiling;
						else if (edge) s = wall;
						else s = Blocks.AIR.getDefaultState();
						set(x, y, z, s);
					}
				}
			}
		}

		void grid(int y, int step, BlockState s) {
			for (int x = 1; x < W - 1; x += step)
				for (int z = 1; z < D - 1; z += step) set(x, y, z, s);
		}

		void pillar(int x, int z, BlockState s) {
			for (int y = 1; y <= H; y++) set(x, y, z, s);
		}

		void hangingLanterns(BlockState lantern, int count) {
			for (int i = 0; i < count; i++) set(1 + r.nextInt(W - 2), H, 1 + r.nextInt(D - 2), lantern);
		}

		void chest(int x, int z, String loot) {
			if (inPath(x, z)) x = W - 2;
			Direction face = Direction.Type.HORIZONTAL.random(r);
			set(x, 1, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, face));
			fillLoot(x, 1, z, loot);
		}

		void barrel(int x, int y, int z, String loot) {
			set(x, y, z, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.UP));
			if (loot != null) fillLoot(x, y, z, loot);
		}

		void fillLoot(int x, int y, int z, String loot) {
			BlockEntity be = w.getBlockEntity(o.add(x, y, z));
			if (be instanceof LootableContainerBlockEntity lc) {
				RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.ofVanilla(loot));
				lc.setLootTable(key, r.nextLong());
			}
		}

		void mob(EntityType<?> type, int x, int y, int z) {
			Entity e = type.spawn(w, o.add(x, y, z), SpawnReason.EVENT);
			if (e instanceof MobEntity m) m.setPersistent();
		}

		int rx() {
			return 1 + r.nextInt(Math.max(1, W - 2));
		}

		int rz() {
			return 3 + r.nextInt(Math.max(1, D - 4));
		}
	}

	private static final List<Theme> THEMES = List.of(
			theme("Library", RoomBuilder::library),
			theme("Frozen Hall", RoomBuilder::frozen),
			theme("Ember Chamber", RoomBuilder::ember),
			theme("Overgrown Garden", RoomBuilder::garden),
			theme("Treasure Vault", RoomBuilder::vault),
			theme("Ghost Prison", RoomBuilder::prison),
			theme("Clockwork Room", RoomBuilder::clockwork),
			theme("Crate Warehouse", RoomBuilder::warehouse),
			theme("Cloud Room", RoomBuilder::clouds),
			theme("Sunken Aquarium", RoomBuilder::aquarium),
			theme("Spirit Shrine", RoomBuilder::shrine));

	public static final int THEME_COUNT = THEMES.size();

	private static Theme theme(String name, java.util.function.Consumer<Ctx> body) {
		return new Theme() {
			public String name() {
				return name;
			}

			public void build(Ctx c) {
				body.accept(c);
			}
		};
	}

	private RoomBuilder() {}

	public static Result build(ServerWorld w, int index, BlockPos origin) {
		long seed = w.getSeed();
		Random r = Random.create(seed ^ (index * 0x9E3779B97F4A7C15L) ^ 0x5DEECE66DL);
		// Walk through every theme before any repeats, starting at a seed-based offset
		int themeIndex = Math.floorMod(index + (int) (seed % THEME_COUNT), THEME_COUNT);
		Theme theme = THEMES.get(themeIndex);

		Ctx c = new Ctx(w, origin, r);
		c.W = 11 + r.nextInt(3) * 2;
		c.D = 13 + r.nextInt(4) * 2;
		c.H = 5 + r.nextInt(3);
		theme.build(c);

		// Entrance: an exit door in the near wall, with a clear walkway
		int dx = c.W / 2;
		for (int z = 0; z <= 2; z++) {
			for (int y = 1; y <= 2; y++) {
				if (!c.get(dx, y, z).isAir() && c.get(dx, y, z).getFluidState().isEmpty()) c.set(dx, y, z, Blocks.AIR.getDefaultState());
			}
		}
		BlockState door = Blocks.WARPED_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.SOUTH);
		c.set(dx, 1, -1, door.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		c.set(dx, 2, -1, door.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));

		// Every room is haunted by its own kind of ghost (friendly in some rooms, hostile in others)
		boolean hostile = RoomGhostEntity.HOSTILE[themeIndex];
		int n = hostile ? 1 + r.nextInt(3) : 1 + r.nextInt(2);
		BlockPos home = origin.add(c.W / 2, 2, c.D / 2);
		for (int i = 0; i < n; i++) {
			RoomGhostEntity g = ModEntities.ROOM_GHOST.create(w);
			if (g == null) continue;
			g.setVariant(themeIndex);
			g.refreshPositionAndAngles(home.getX() + r.nextInt(5) - 2 + 0.5, home.getY(), home.getZ() + r.nextInt(5) - 2 + 0.5, r.nextFloat() * 360f, 0f);
			g.setHome(home, Math.max(2f, Math.min(c.W, c.D) / 2f - 1.5f));
			g.setPersistent();
			w.spawnEntity(g);
		}
		String haunting = " (" + RoomGhostEntity.NAMES[themeIndex] + (hostile ? ", hostile!)" : ", friendly)");

		String name = "The " + MOODS[r.nextInt(MOODS.length)] + " " + theme.name() + haunting;
		Vec3d entry = new Vec3d(origin.getX() + dx + 0.5, origin.getY() + 1, origin.getZ() + 1.5);
		return new Result(name, entry, 0f);
	}

	// ----------------------------------------------------------------- themes

	private static void library(Ctx c) {
		c.shell(Blocks.DARK_OAK_PLANKS.getDefaultState(), Blocks.BOOKSHELF.getDefaultState(), Blocks.DARK_OAK_PLANKS.getDefaultState());
		BlockState log = Blocks.DARK_OAK_LOG.getDefaultState();
		c.pillar(0, 0, log);
		c.pillar(c.W - 1, 0, log);
		c.pillar(0, c.D - 1, log);
		c.pillar(c.W - 1, c.D - 1, log);
		for (int z = 4; z < c.D - 3; z += 3) {
			int height = 2 + c.r.nextInt(2);
			for (int x = 1; x < c.W - 1; x++) {
				if (Math.abs(x - c.W / 2) <= 1) continue;
				if (c.r.nextInt(6) == 0) continue;
				for (int y = 1; y <= height; y++) c.set(x, y, z, Blocks.BOOKSHELF.getDefaultState());
			}
		}
		for (int z = 0; z < c.D; z++) c.set(c.W / 2, 1, z, Blocks.RED_CARPET.getDefaultState());
		c.set(c.W / 2, 1, c.D - 2, Blocks.ENCHANTING_TABLE.getDefaultState());
		c.hangingLanterns(Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true), 4 + c.r.nextInt(4));
		for (int i = 0; i < 6; i++) c.set(c.r.nextBoolean() ? 0 : c.W - 1, c.H, c.r.nextInt(c.D), Blocks.COBWEB.getDefaultState());
		c.chest(c.W / 2 + 2, c.D - 2, "chests/stronghold_library");
	}

	private static void frozen(Ctx c) {
		c.shell(Blocks.PACKED_ICE.getDefaultState(), Blocks.BLUE_ICE.getDefaultState(), Blocks.PACKED_ICE.getDefaultState());
		c.grid(0, 4, Blocks.SEA_LANTERN.getDefaultState());
		c.grid(c.H + 1, 4, Blocks.SEA_LANTERN.getDefaultState());
		for (int i = 0; i < 8; i++) {
			int x = c.rx(), z = c.rz();
			if (c.inPath(x, z)) continue;
			int up = 1 + c.r.nextInt(3);
			for (int y = 1; y <= up; y++) c.set(x, y, z, Blocks.PACKED_ICE.getDefaultState());
			int down = 1 + c.r.nextInt(2);
			for (int y = c.H; y > c.H - down; y--) c.set(c.rx(), y, c.rz(), Blocks.ICE.getDefaultState());
		}
		for (int x = 0; x < c.W; x++)
			for (int z = 0; z < c.D; z++)
				if (c.isAir(x, 1, z) && c.r.nextInt(3) == 0)
					c.set(x, 1, z, Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, 1 + c.r.nextInt(3)));
		c.chest(c.rx(), c.D - 2, "chests/igloo_chest");
		for (int i = 0, n = 1 + c.r.nextInt(2); i < n; i++) c.mob(EntityType.STRAY, c.rx(), 1, c.D - 3);
	}

	private static void ember(Ctx c) {
		c.shell(Blocks.POLISHED_BLACKSTONE.getDefaultState(), Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), Blocks.BLACKSTONE.getDefaultState());
		for (int x = 0; x < c.W; x++)
			for (int z = 0; z < c.D; z++)
				if (c.r.nextInt(5) == 0) c.set(x, 0, z, Blocks.MAGMA_BLOCK.getDefaultState());
		c.grid(c.H + 1, 3, Blocks.SHROOMLIGHT.getDefaultState());
		// Lava pits fenced with iron bars
		for (int pit = 0; pit < 2; pit++) {
			int px = 2 + c.r.nextInt(Math.max(1, c.W - 6));
			int pz = 5 + c.r.nextInt(Math.max(1, c.D - 9));
			for (int x = px; x < px + 2; x++)
				for (int z = pz; z < pz + 2; z++) c.set(x, 0, z, Blocks.LAVA.getDefaultState());
			for (int x = px - 1; x <= px + 2; x++)
				for (int z = pz - 1; z <= pz + 2; z++)
					if (x < px || x > px + 1 || z < pz || z > pz + 1) c.setConnected(x, 1, z, Blocks.IRON_BARS.getDefaultState());
		}
		c.pillar(1, c.D - 2, Blocks.GILDED_BLACKSTONE.getDefaultState());
		c.pillar(c.W - 2, c.D - 2, Blocks.GILDED_BLACKSTONE.getDefaultState());
		c.chest(c.W / 2, c.D - 2, "chests/nether_bridge");
		for (int i = 0, n = 1 + c.r.nextInt(2); i < n; i++) c.mob(EntityType.MAGMA_CUBE, c.rx(), 1, c.D - 3);
	}

	private static void garden(Ctx c) {
		c.shell(Blocks.MOSS_BLOCK.getDefaultState(), Blocks.MOSSY_STONE_BRICKS.getDefaultState(), Blocks.GLASS.getDefaultState());
		c.grid(0, 4, Blocks.VERDANT_FROGLIGHT.getDefaultState());
		Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET,
				Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.OXEYE_DAISY};
		for (int x = 0; x < c.W; x++)
			for (int z = 0; z < c.D; z++) {
				if (c.inPath(x, z) || !c.get(x, 0, z).isOf(Blocks.MOSS_BLOCK)) continue;
				int roll = c.r.nextInt(10);
				if (roll < 3) c.set(x, 1, z, flowers[c.r.nextInt(flowers.length)].getDefaultState());
				else if (roll == 3) c.set(x, 1, z, (c.r.nextBoolean() ? Blocks.AZALEA : Blocks.FLOWERING_AZALEA).getDefaultState());
			}
		// Leafy walls
		BlockState leaves = Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
		for (int i = 0; i < c.W * 2; i++) {
			int x = c.r.nextBoolean() ? 0 : c.W - 1;
			c.set(x, 1 + c.r.nextInt(c.H), c.r.nextInt(c.D), leaves);
		}
		// Little pond with lily pads
		int px = c.W / 2 - 1, pz = c.D / 2;
		for (int x = px; x < px + 3; x++)
			for (int z = pz; z < pz + 3; z++) {
				c.set(x, 0, z, Blocks.WATER.getDefaultState());
				c.set(x, 1, z, c.r.nextInt(3) == 0 ? Blocks.LILY_PAD.getDefaultState() : Blocks.AIR.getDefaultState());
			}
		c.chest(1, c.D - 2, "chests/jungle_temple");
	}

	private static void vault(Ctx c) {
		c.shell(Blocks.POLISHED_DEEPSLATE.getDefaultState(), Blocks.DEEPSLATE_TILES.getDefaultState(), Blocks.DEEPSLATE_TILES.getDefaultState());
		for (int x = 0; x < c.W; x++)
			for (int z = 0; z < c.D; z++)
				if ((x + z) % 2 == 0) c.set(x, 0, z, Blocks.GOLD_BLOCK.getDefaultState());
		c.grid(c.H + 1, 4, Blocks.GLOWSTONE.getDefaultState());
		for (int i = 0; i < 6; i++) {
			int x = c.rx(), z = c.rz();
			if (c.inPath(x, z)) continue;
			int h = 1 + c.r.nextInt(2);
			for (int y = 1; y <= h; y++) c.set(x, y, z, (c.r.nextBoolean() ? Blocks.RAW_GOLD_BLOCK : Blocks.GOLD_BLOCK).getDefaultState());
		}
		int chests = 2 + c.r.nextInt(3);
		for (int i = 0; i < chests; i++) {
			c.chest(1 + c.r.nextInt(c.W - 2), c.D - 2 - c.r.nextInt(3), i == 0 ? "chests/buried_treasure" : "chests/desert_pyramid");
		}
		for (int i = 0, n = 2 + c.r.nextInt(2); i < n; i++) c.mob(EntityType.ZOMBIE, c.rx(), 1, c.D - 4);
	}

	private static void prison(Ctx c) {
		c.W = Math.max(c.W, 13);
		c.shell(Blocks.DEEPSLATE_BRICKS.getDefaultState(), Blocks.DEEPSLATE_BRICKS.getDefaultState(), Blocks.DEEPSLATE_BRICKS.getDefaultState());
		BlockState bars = Blocks.IRON_BARS.getDefaultState();
		for (int side = 0; side < 2; side++) {
			int front = side == 0 ? 3 : c.W - 4;
			for (int z = 3; z < c.D; z++) {
				for (int y = 1; y <= c.H; y++) {
					if ((z - 3) % 4 == 0) {
						// cell divider wall
						int from = side == 0 ? 0 : c.W - 3, to = side == 0 ? 2 : c.W - 1;
						for (int x = from; x <= to; x++) c.set(x, y, z, Blocks.DEEPSLATE_BRICKS.getDefaultState());
					}
					c.setConnected(front, y, z, bars);
				}
			}
		}
		for (int i = 0; i < 4; i++) {
			int x = 5 + c.r.nextInt(Math.max(1, c.W - 10));
			for (int y = c.H; y > c.H - 2; y--) c.set(x, y, 3 + c.r.nextInt(c.D - 4), Blocks.CHAIN.getDefaultState());
		}
		c.hangingLanterns(Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true), 4);
		c.chest(1, 5, "chests/simple_dungeon");
		c.mob(EntityType.SKELETON, c.W - 2, 1, 5);
		if (c.r.nextBoolean()) c.mob(EntityType.SKELETON, 1, 1, 9);
	}

	private static void clockwork(Ctx c) {
		Block[] copper = {Blocks.COPPER_BLOCK, Blocks.EXPOSED_COPPER, Blocks.WEATHERED_COPPER, Blocks.OXIDIZED_COPPER};
		c.shell(Blocks.CUT_COPPER.getDefaultState(), Blocks.COPPER_BLOCK.getDefaultState(), Blocks.CUT_COPPER.getDefaultState());
		for (int x = -1; x <= c.W; x++)
			for (int z = -1; z <= c.D; z++)
				for (int y = 1; y <= c.H; y++)
					if (x < 0 || x >= c.W || z < 0 || z >= c.D) c.set(x, y, z, copper[c.r.nextInt(copper.length)].getDefaultState());
		// Lamps in the side walls, powered from behind
		for (int z = 2; z < c.D - 1; z += 3) {
			for (int side = 0; side < 2; side++) {
				int wx = side == 0 ? -1 : c.W;
				int bx = side == 0 ? -2 : c.W + 1;
				c.set(bx, 3, z, Blocks.REDSTONE_BLOCK.getDefaultState());
				c.set(wx, 3, z, Blocks.REDSTONE_LAMP.getDefaultState().with(RedstoneLampBlock.LIT, true));
			}
		}
		for (int i = 0; i < 5; i++) {
			int x = c.rx(), z = c.rz();
			if (c.inPath(x, z)) continue;
			for (int y = 1; y <= c.H; y++) c.set(x, y, z, Blocks.LIGHTNING_ROD.getDefaultState());
		}
		for (int i = 0; i < 6; i++) {
			int x = c.rx(), z = c.rz();
			if (!c.inPath(x, z)) c.set(x, 1, z, (c.r.nextBoolean() ? Blocks.OBSERVER : Blocks.PISTON).getDefaultState());
		}
		c.set(c.W / 2, 1, c.D - 2, Blocks.LODESTONE.getDefaultState());
		c.chest(c.W / 2 + 2, c.D - 2, "chests/ancient_city");
	}

	private static void warehouse(Ctx c) {
		c.shell(Blocks.SPRUCE_PLANKS.getDefaultState(), Blocks.STRIPPED_SPRUCE_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y),
				Blocks.SPRUCE_PLANKS.getDefaultState());
		c.hangingLanterns(Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true), 5);
		int stacks = 8 + c.r.nextInt(6);
		for (int i = 0; i < stacks; i++) {
			int x = c.rx(), z = c.rz();
			if (c.inPath(x, z) || !c.isAir(x, 1, z)) continue;
			int h = 1 + c.r.nextInt(3);
			for (int y = 1; y <= h; y++) c.barrel(x, y, z, c.r.nextInt(4) == 0 ? "chests/shipwreck_supply" : null);
		}
		c.chest(1, c.D - 2, "chests/village/village_weaponsmith");
	}

	private static void clouds(Ctx c) {
		c.shell(Blocks.WHITE_STAINED_GLASS.getDefaultState(), Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(),
				Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
		int blobs = 5 + c.r.nextInt(4);
		for (int b = 0; b < blobs; b++) {
			int cx = c.rx(), cz = c.rz(), cy = c.r.nextBoolean() ? 1 : 2 + c.r.nextInt(Math.max(1, c.H - 2));
			int rad = 1 + c.r.nextInt(2);
			for (int x = cx - rad; x <= cx + rad; x++)
				for (int z = cz - rad; z <= cz + rad; z++)
					for (int y = cy; y <= cy + 1; y++) {
						if (x < 0 || x >= c.W || z < 0 || z >= c.D || y < 1 || y > c.H || c.inPath(x, z)) continue;
						if ((x - cx) * (x - cx) + (z - cz) * (z - cz) <= rad * rad + 1) c.set(x, y, z, Blocks.WHITE_WOOL.getDefaultState());
					}
		}
		for (int i = 0; i < 8; i++) {
			int x = c.rx(), z = c.rz();
			if (c.isAir(x, c.H, z)) c.set(x, c.H, z, Blocks.END_ROD.getDefaultState().with(net.minecraft.block.EndRodBlock.FACING, Direction.DOWN));
		}
		c.grid(0, 5, Blocks.SEA_LANTERN.getDefaultState());
		c.chest(c.W / 2, c.D - 2, "chests/end_city_treasure");
	}

	private static void aquarium(Ctx c) {
		c.shell(Blocks.PRISMARINE_BRICKS.getDefaultState(), Blocks.DARK_PRISMARINE.getDefaultState(), Blocks.PRISMARINE_BRICKS.getDefaultState());
		c.grid(0, 3, Blocks.SEA_LANTERN.getDefaultState());
		// Fish tanks along both side walls
		for (int side = 0; side < 2; side++) {
			int x0 = side == 0 ? 0 : c.W - 2;
			int glass = side == 0 ? 2 : c.W - 3;
			for (int z = 3; z < c.D - 1; z++) {
				for (int y = 1; y <= c.H; y++) {
					c.set(x0, y, z, Blocks.WATER.getDefaultState());
					c.set(x0 + 1, y, z, Blocks.WATER.getDefaultState());
					c.set(glass, y, z, Blocks.GLASS.getDefaultState());
				}
				if (c.r.nextInt(3) == 0) c.set(x0, 1, z, Blocks.SEAGRASS.getDefaultState());
			}
			// Close off the tank ends so the water stays put
			for (int y = 1; y <= c.H; y++) {
				for (int x = x0; x <= x0 + 1; x++) {
					c.set(x, y, 2, Blocks.GLASS.getDefaultState());
					c.set(x, y, c.D - 1, Blocks.GLASS.getDefaultState());
				}
			}
		}
		for (int i = 0, n = 3 + c.r.nextInt(3); i < n; i++) {
			int x = c.r.nextBoolean() ? 0 : c.W - 2;
			c.mob(EntityType.TROPICAL_FISH, x, 2 + c.r.nextInt(Math.max(1, c.H - 2)), 4 + c.r.nextInt(Math.max(1, c.D - 6)));
		}
		c.chest(c.W / 2, c.D - 2, "chests/underwater_ruin_big");
	}

	private static void shrine(Ctx c) {
		c.shell(Blocks.CALCITE.getDefaultState(), Blocks.SMOOTH_QUARTZ.getDefaultState(), Blocks.AMETHYST_BLOCK.getDefaultState());
		for (int x = 2; x < c.W - 1; x += 4)
			for (int z = 4; z < c.D - 1; z += 4)
				if (!c.inPath(x, z)) c.pillar(x, z, Blocks.QUARTZ_PILLAR.getDefaultState());
		for (int i = 0; i < 10; i++) {
			int x = c.rx(), z = c.rz();
			if (!c.inPath(x, z) && c.isAir(x, 1, z)) c.set(x, 1, z, Blocks.AMETHYST_CLUSTER.getDefaultState());
		}
		for (int i = 0; i < 8; i++) {
			int x = c.rx(), z = c.rz();
			if (!c.inPath(x, z) && c.isAir(x, 1, z)) {
				c.set(x, 1, z, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 1 + c.r.nextInt(4)).with(CandleBlock.LIT, true));
			}
		}
		c.grid(c.H + 1, 4, Blocks.SEA_LANTERN.getDefaultState());
		c.set(c.W / 2, 1, c.D - 3, Blocks.BEACON.getDefaultState());
		c.chest(c.W / 2 + 2, c.D - 2, "chests/woodland_mansion");
		for (int i = 0, n = 1 + c.r.nextInt(3); i < n; i++) c.mob(EntityType.ALLAY, c.rx(), 2, c.D / 2);
	}
}
