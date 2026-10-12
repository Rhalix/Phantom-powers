package com.phantompowers.world;

import com.phantompowers.ModBlocks;
import com.phantompowers.ModEntities;
import com.phantompowers.ModItems;
import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.entity.GhostJailerEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Ghost Prisons come in two designs:
 *  - The Big House: a huge walled penitentiary with two-tier cell blocks, guard towers with searchlights,
 *    a gatehouse, an exercise yard and the Jailer's hall.
 *  - The Spire: a tall round cell tower, four floors of cells ringed around an open atrium.
 * Both have a green beacon beam shining into the sky so you can find them from far away.
 */
public final class GhostPrison {
	private GhostPrison() {}

	/** Which design a prison uses. Must match the first random draw in build(). */
	public static boolean isSpire(long key) {
		return Random.create(key).nextInt(2) == 1;
	}

	/** Where the Jailer floats (relative to the prison center). */
	public static BlockPos bossSpot(BlockPos center, boolean spire) {
		return spire ? center.add(0, 12, 0) : center.add(0, 1, -2);
	}

	public static GhostJailerEntity build(ServerWorld w, BlockPos center, Random r) {
		boolean spire = r.nextInt(2) == 1;
		return buildDesign(w, center, r, spire);
	}

	public static GhostJailerEntity buildDesign(ServerWorld w, BlockPos center, Random r, boolean spire) {
		if (spire) buildSpire(new B(w, center, r));
		else buildBigHouse(new B(w, center, r));
		return spawnJailer(w, bossSpot(center, spire), spire ? 8f : 22f, true);
	}

	public static GhostJailerEntity spawnJailer(ServerWorld w, BlockPos at, float radius, boolean withGuards) {
		GhostJailerEntity jailer = ModEntities.GHOST_JAILER.create(w);
		if (jailer != null) {
			jailer.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180f, 0f);
			jailer.setHome(at, radius);
			jailer.setPersistent();
			w.spawnEntity(jailer);
		}
		if (withGuards) {
			for (int i = 0; i < 3; i++) {
				AbstractGhostEntity guard = ModEntities.HOSTILE_GHOST.create(w);
				if (guard == null) continue;
				guard.refreshPositionAndAngles(at.getX() + (i - 1) * 6, at.getY() + 1, at.getZ() + 6, 0f, 0f);
				guard.setCustomName(net.minecraft.text.Text.literal("Prison Guard"));
				guard.setHome(at, radius);
				guard.setPersistent();
				w.spawnEntity(guard);
			}
		}
		return jailer;
	}

	// ------------------------------------------------------------------ helpers

	private static final class B {
		final ServerWorld w;
		final BlockPos c;
		final Random r;

		B(ServerWorld w, BlockPos c, Random r) {
			this.w = w;
			this.c = c;
			this.r = r;
		}

		void set(int x, int y, int z, BlockState s) {
			w.setBlockState(c.add(x, y, z), s, Block.NOTIFY_LISTENERS);
		}

		void bars(int x, int y, int z) {
			BlockPos p = c.add(x, y, z);
			BlockState s = Blocks.IRON_BARS.getDefaultState();
			w.setBlockState(p, Block.postProcessState(s, w, p), Block.NOTIFY_ALL);
		}

		void box(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
			for (int x = x1; x <= x2; x++)
				for (int y = y1; y <= y2; y++)
					for (int z = z1; z <= z2; z++) set(x, y, z, s);
		}

		void island(int radius, int clearHeight) {
			for (int x = -radius; x <= radius; x++)
				for (int z = -radius; z <= radius; z++) {
					double d = Math.sqrt(x * x + z * z) + r.nextDouble() * 1.5;
					if (d > radius) continue;
					set(x, -1, z, ModBlocks.ECTO_GRASS.getDefaultState());
					int depth = 3 + (int) ((radius - d) * 0.5) + r.nextInt(2);
					for (int y = -2; y >= -1 - depth; y--) set(x, y, z, ModBlocks.ECTO_STONE.getDefaultState());
					for (int y = 0; y <= clearHeight; y++) set(x, y, z, Blocks.AIR.getDefaultState());
				}
		}

		/** A green beacon on a 3x3 iron base, with the sky cleared above so the beam shows. */
		void beacon(int x, int y, int z) {
			box(x - 1, y - 1, z - 1, x + 1, y - 1, z + 1, Blocks.IRON_BLOCK.getDefaultState());
			set(x, y, z, Blocks.BEACON.getDefaultState());
			set(x, y + 1, z, Blocks.LIME_STAINED_GLASS.getDefaultState());
			int top = w.getTopY() - c.getY() - 1;
			for (int yy = y + 2; yy <= top; yy++) {
				if (!w.getBlockState(c.add(x, yy, z)).isAir()) set(x, yy, z, Blocks.AIR.getDefaultState());
			}
		}

		void chest(int x, int y, int z, Direction face, String loot) {
			set(x, y, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, face));
			BlockEntity be = w.getBlockEntity(c.add(x, y, z));
			if (be instanceof LootableContainerBlockEntity lc) {
				RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.ofVanilla(loot));
				lc.setLootTable(key, r.nextLong());
			}
		}

		void gearChest(int x, int y, int z, Direction face) {
			set(x, y, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, face));
			if (w.getBlockEntity(c.add(x, y, z)) instanceof ChestBlockEntity chest) {
				chest.setStack(4, new ItemStack(ModItems.THERMOS));
				chest.setStack(10, new ItemStack(ModItems.ECTOPLASM, 8 + r.nextInt(8)));
				chest.setStack(13, new ItemStack(Items.DIAMOND, 2 + r.nextInt(3)));
				chest.setStack(16, new ItemStack(Items.GOLDEN_APPLE, 2));
				if (r.nextInt(3) == 0) chest.setStack(22, new ItemStack(ModItems.GHOST_BLASTER));
			}
		}

		void prisoner(int x, int y, int z) {
			int roll = r.nextInt(100);
			if (roll >= 38) return; // most cells stand empty
			boolean hostile = roll < 27;
			AbstractGhostEntity g = hostile ? ModEntities.HOSTILE_GHOST.create(w) : ModEntities.FRIENDLY_GHOST.create(w);
			if (g == null) return;
			BlockPos cell = c.add(x, y, z);
			g.refreshPositionAndAngles(cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5, r.nextFloat() * 360f, 0f);
			g.setHome(cell, 1.2f);
			g.setPersistent();
			w.spawnEntity(g);
		}
	}

	// ------------------------------------------------------------ The Big House

	private static void buildBigHouse(B b) {
		final int H = 30;
		Random r = b.r;
		BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
		BlockState cracked = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
		BlockState tile = Blocks.DEEPSLATE_TILES.getDefaultState();
		BlockState brick = Blocks.DEEPSLATE_BRICKS.getDefaultState();
		BlockState air = Blocks.AIR.getDefaultState();
		BlockState hangingSoul = Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true);

		b.island(38, 26);

		// Yard
		for (int x = -H; x <= H; x++)
			for (int z = -H; z <= H; z++) {
				boolean light = Math.floorMod(x, 7) == 0 && Math.floorMod(z, 7) == 0;
				b.set(x, -1, z, light ? Blocks.SEA_LANTERN.getDefaultState()
						: (Math.floorMod(x + z, 2) == 0 ? Blocks.POLISHED_BLACKSTONE.getDefaultState() : Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState()));
			}

		// Outer wall: tall, with a barbed-bars fence on top
		for (int i = -H; i <= H; i++) {
			for (int y = 0; y <= 12; y++) {
				BlockState s = r.nextInt(9) == 0 ? cracked : wall;
				b.set(i, y, -H, s);
				b.set(i, y, H, s);
				b.set(-H, y, i, s);
				b.set(H, y, i, s);
			}
			b.bars(i, 13, -H);
			b.bars(i, 13, H);
			b.bars(-H, 13, i);
			b.bars(H, 13, i);
		}

		// Gatehouse
		b.box(-5, 0, H - 3, 5, 15, H + 1, tile);
		b.box(-4, 0, H - 2, 4, 14, H, air);
		b.box(-2, 0, H - 3, 2, 5, H + 1, air);
		for (int x = -2; x <= 2; x++) b.bars(x, 5, H + 1);
		for (int x = -3; x <= 3; x += 2) b.bars(x, 11, H + 1);
		b.set(-3, 4, H + 2, Blocks.SOUL_LANTERN.getDefaultState());
		b.set(3, 4, H + 2, Blocks.SOUL_LANTERN.getDefaultState());

		// Guard towers with searchlights
		for (int sx : new int[]{-H, H}) {
			for (int sz : new int[]{-H, H}) {
				for (int x = -3; x <= 3; x++)
					for (int z = -3; z <= 3; z++)
						for (int y = 0; y <= 20; y++) {
							boolean shell = Math.abs(x) == 3 || Math.abs(z) == 3 || y == 0 || y == 20;
							b.set(sx + x, y, sz + z, shell ? tile : air);
						}
				for (int x = -3; x <= 3; x++) {
					b.bars(sx + x, 21, sz - 3);
					b.bars(sx + x, 21, sz + 3);
					b.bars(sx - 3, 21, sz + x);
					b.bars(sx + 3, 21, sz + x);
				}
				b.set(sx, 21, sz, Blocks.SEA_LANTERN.getDefaultState());
				b.set(sx, 22, sz, Blocks.END_ROD.getDefaultState());
				b.set(sx, 23, sz, Blocks.END_ROD.getDefaultState());
				for (int y = 4; y <= 16; y += 6) {
					b.set(sx + (sx < 0 ? 3 : -3), y, sz, air);
					b.bars(sx + (sx < 0 ? 3 : -3), y, sz);
				}
			}
		}

		// Two cell blocks, two tiers each, along the east and west walls
		for (int side : new int[]{1, -1}) {
			// building shell
			for (int x = 18; x <= 29; x++)
				for (int z = -25; z <= 25; z++)
					for (int y = 0; y <= 11; y++) {
						boolean outer = x == 29 || z == -25 || z == 25 || y == 11;
						b.set(x * side, y, z, outer ? brick : air);
					}
			for (int tier = 0; tier < 2; tier++) {
				int y0 = tier * 6;
				// tier floor / catwalk
				if (tier == 1) {
					for (int x = 21; x <= 28; x++)
						for (int z = -24; z <= 24; z++) b.set(x * side, 5, z, Blocks.POLISHED_DEEPSLATE.getDefaultState());
					for (int z = -24; z <= 24; z++) b.bars(20 * side, 6, z);
				}
				for (int k = 0; k < 8; k++) {
					int z0 = -23 + k * 6;
					for (int y = y0; y <= y0 + 4; y++) {
						for (int x = 24; x <= 28; x++) {
							b.set(x * side, y, z0 - 1, brick);
							b.set(x * side, y, z0 + 5, brick);
						}
						for (int z = z0; z <= z0 + 4; z++) b.bars(23 * side, y, z);
					}
					b.set(26 * side, y0 + 4, z0 + 2, hangingSoul);
					if (r.nextInt(3) == 0) b.set(28 * side, y0, z0 + 4, Blocks.COBWEB.getDefaultState());
					if (r.nextBoolean()) b.set(28 * side, y0 + 3, z0 + 1, Blocks.CHAIN.getDefaultState());
					if (r.nextInt(9) == 0) b.chest(27 * side, y0, z0 + 3, Direction.NORTH, "chests/simple_dungeon");
					else b.prisoner(26 * side, y0 + 1, z0 + 2);
				}
			}
			// staircase up to the catwalk
			for (int i = 0; i < 5; i++) {
				for (int x = 19; x <= 20; x++) {
					b.set(x * side, i, 22 - i, Blocks.POLISHED_BLACKSTONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
					for (int y = 0; y < i; y++) b.set(x * side, y, 22 - i, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
				}
			}
			// doorway into the cell block from the yard
			for (int z = -2; z <= 2; z++)
				for (int y = 0; y <= 3; y++) b.set(18 * side, y, z, air);
			for (int z = -24; z <= 24; z += 6) b.set(19 * side, 10, z, hangingSoul);
		}

		// The Jailer's hall, with the beacon tower above it
		b.box(-13, 0, -29, 13, 10, -16, tile);
		b.box(-12, 0, -28, 12, 9, -17, air);
		b.box(-2, 0, -16, 2, 4, -16, air);
		b.set(0, 0, -26, Blocks.POLISHED_BLACKSTONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
		b.set(-1, 0, -26, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState());
		b.set(1, 0, -26, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState());
		for (int x = -10; x <= 10; x += 5) b.set(x, 9, -22, hangingSoul);
		for (int z = -27; z <= -18; z += 3) {
			b.set(-12, 3, z, Blocks.CHAIN.getDefaultState());
			b.set(12, 3, z, Blocks.CHAIN.getDefaultState());
		}
		b.chest(-10, 0, -27, Direction.SOUTH, "chests/end_city_treasure");
		b.chest(10, 0, -27, Direction.SOUTH, "chests/buried_treasure");
		b.chest(10, 0, -19, Direction.WEST, "chests/stronghold_corridor");
		b.gearChest(-10, 0, -19, Direction.EAST);
		b.box(-2, 10, -24, 2, 22, -20, tile);
		b.beacon(0, 23, -22);

		// The admin block: cafeteria, warden's office, armory, infirmary and visiting room
		PrisonInterior.adminBlock(b.w, b.c, b.r);

		// Exercise yard centerpiece: a chained soul pillar
		for (int y = 0; y <= 5; y++) b.set(0, y, 4, Blocks.POLISHED_BASALT.getDefaultState());
		b.set(0, 6, 4, Blocks.SOUL_CAMPFIRE.getDefaultState());
		for (Direction d : Direction.Type.HORIZONTAL) {
			BlockPos o = BlockPos.ORIGIN.offset(d);
			b.set(o.getX(), 4, 4 + o.getZ(), Blocks.CHAIN.getDefaultState());
		}
	}

	// ----------------------------------------------------------------- The Spire

	private static void buildSpire(B b) {
		Random r = b.r;
		final int R = 13, FLOORS = 4, FH = 8, TOP = FLOORS * FH;
		BlockState wall = Blocks.DEEPSLATE_BRICKS.getDefaultState();
		BlockState band = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
		BlockState floor = Blocks.POLISHED_DEEPSLATE.getDefaultState();
		BlockState air = Blocks.AIR.getDefaultState();
		BlockState hangingSoul = Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true);

		b.island(28, TOP + 4);
		for (int x = -R - 1; x <= R + 1; x++)
			for (int z = -R - 1; z <= R + 1; z++) {
				double d = Math.sqrt(x * x + z * z);
				if (d > R + 0.5) continue;
				b.set(x, -1, z, d < 6 ? Blocks.SEA_LANTERN.getDefaultState() : floor);
				for (int y = 0; y <= TOP; y++) {
					if (d >= R - 0.5) {
						b.set(x, y, z, (y % FH == 0) ? band : wall);           // outer wall
						if (y % FH == 4 && Math.floorMod(x * 7 + z * 3, 5) == 0) b.bars(x, y, z);  // slit windows
					} else if (y > 0 && y % FH == 0 && d >= 7) {
						b.set(x, y, z, floor);                                  // ring floors
					}
				}
			}
		// Cells: 12 per floor around the atrium
		for (int f = 0; f < FLOORS; f++) {
			int y0 = f * FH + 1;
			for (int x = -R; x <= R; x++)
				for (int z = -R; z <= R; z++) {
					double d = Math.sqrt(x * x + z * z);
					if (d < 7 || d >= R - 0.5) continue;
					double ang = Math.toDegrees(Math.atan2(z, x));
					boolean divider = d >= 8 && Math.abs(Math.floorMod((int) Math.round(ang), 30) - 15) >= 14;
					for (int y = y0; y < y0 + FH - 1; y++) {
						if (d < 8) {
							if (y < y0 + 5) b.bars(x, y, z);
						} else if (divider) {
							b.set(x, y, z, wall);
						}
					}
				}
			for (int k = 0; k < 12; k++) {
				double a = Math.toRadians(k * 30 + 15);
				int cx = (int) Math.round(Math.cos(a) * 10), cz = (int) Math.round(Math.sin(a) * 10);
				b.set(cx, y0 + 5, cz, hangingSoul);
				if (r.nextInt(10) == 0) b.chest(cx, y0, cz, Direction.NORTH, "chests/simple_dungeon");
				else b.prisoner(cx, y0, cz);
			}
		}
		// Ground-floor entrance on the south side
		for (int x = -1; x <= 1; x++)
			for (int z = 7; z <= R + 1; z++)
				for (int y = 0; y <= 4; y++) b.set(x, y, z, air);
		b.set(-2, 3, R + 1, Blocks.SOUL_LANTERN.getDefaultState());
		b.set(2, 3, R + 1, Blocks.SOUL_LANTERN.getDefaultState());
		// Atrium chains and the Jailer's loot on the ground floor
		for (int y = TOP - 1; y > TOP - 10; y--) {
			b.set(-3, y, 0, Blocks.CHAIN.getDefaultState());
			b.set(3, y, 0, Blocks.CHAIN.getDefaultState());
		}
		b.gearChest(-2, 0, -4, Direction.SOUTH);
		b.chest(2, 0, -4, Direction.SOUTH, "chests/end_city_treasure");
		b.chest(0, 0, -5, Direction.SOUTH, "chests/buried_treasure");
		// Roof with battlements and the beacon
		for (int x = -R; x <= R; x++)
			for (int z = -R; z <= R; z++) {
				double d = Math.sqrt(x * x + z * z);
				if (d > R + 0.5) continue;
				b.set(x, TOP, z, band);
				if (d >= R - 0.5 && Math.floorMod(x + z, 2) == 0) b.set(x, TOP + 1, z, band);
			}
		b.beacon(0, TOP + 2, 0);
		b.set(0, TOP + 1, 0, Blocks.IRON_BLOCK.getDefaultState());
		// Admin wing on the south side: cafeteria and warden's office
		PrisonInterior.spireWing(b.w, b.c, b.r, R);
	}
}
