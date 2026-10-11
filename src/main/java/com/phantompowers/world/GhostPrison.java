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
 * A floating prison fortress: thick walls, four guard towers, two rows of barred cells full of
 * captured ghosts, and the Jailer's hall with the best loot. Beat the Ghost Jailer to free the prisoners.
 */
public final class GhostPrison {
	private static final int HALF = 20;

	private GhostPrison() {}

	private static final class B {
		final ServerWorld w;
		final BlockPos c;

		B(ServerWorld w, BlockPos c) {
			this.w = w;
			this.c = c;
		}

		void set(int x, int y, int z, BlockState s) {
			w.setBlockState(c.add(x, y, z), s, Block.NOTIFY_LISTENERS);
		}

		void setConnected(int x, int y, int z, BlockState s) {
			BlockPos p = c.add(x, y, z);
			w.setBlockState(p, Block.postProcessState(s, w, p), Block.NOTIFY_ALL);
		}

		void box(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
			for (int x = x1; x <= x2; x++)
				for (int y = y1; y <= y2; y++)
					for (int z = z1; z <= z2; z++) set(x, y, z, s);
		}
	}

	public static GhostJailerEntity build(ServerWorld w, BlockPos center, Random r) {
		B b = new B(w, center);
		BlockState air = Blocks.AIR.getDefaultState();
		BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
		BlockState cracked = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
		BlockState tile = Blocks.DEEPSLATE_TILES.getDefaultState();
		BlockState brick = Blocks.DEEPSLATE_BRICKS.getDefaultState();
		BlockState bars = Blocks.IRON_BARS.getDefaultState();
		BlockState soulLantern = Blocks.SOUL_LANTERN.getDefaultState();
		BlockState hangingSoul = soulLantern.with(LanternBlock.HANGING, true);

		// Floating island underneath
		for (int x = -27; x <= 27; x++) {
			for (int z = -27; z <= 27; z++) {
				double d = Math.sqrt(x * x + z * z) + r.nextDouble() * 1.5;
				if (d > 27) continue;
				b.set(x, -1, z, ModBlocks.ECTO_GRASS.getDefaultState());
				int depth = 2 + (int) ((27 - d) * 0.55) + r.nextInt(2);
				for (int y = -2; y >= -1 - depth; y--) b.set(x, y, z, ModBlocks.ECTO_STONE.getDefaultState());
				for (int y = 0; y <= 18; y++) b.set(x, y, z, air);
			}
		}

		// Courtyard floor
		for (int x = -HALF; x <= HALF; x++) {
			for (int z = -HALF; z <= HALF; z++) {
				boolean light = Math.floorMod(x, 6) == 0 && Math.floorMod(z, 6) == 0;
				b.set(x, -1, z, light ? Blocks.SEA_LANTERN.getDefaultState()
						: ((x + z) % 2 == 0 ? Blocks.POLISHED_BLACKSTONE.getDefaultState() : Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState()));
			}
		}

		// Outer walls with battlements and a gate
		for (int i = -HALF; i <= HALF; i++) {
			for (int y = 0; y <= 8; y++) {
				BlockState s = r.nextInt(8) == 0 ? cracked : wall;
				b.set(i, y, -HALF, s);
				b.set(i, y, HALF, s);
				b.set(-HALF, y, i, s);
				b.set(HALF, y, i, s);
			}
			if (Math.floorMod(i, 2) == 0) {
				b.set(i, 9, -HALF, wall);
				b.set(i, 9, HALF, wall);
				b.set(-HALF, 9, i, wall);
				b.set(HALF, 9, i, wall);
			}
		}
		b.box(-1, 0, HALF, 1, 3, HALF, air);
		for (int x = -1; x <= 1; x++) b.setConnected(x, 4, HALF, bars);
		b.set(-2, 3, HALF + 1, soulLantern);
		b.set(2, 3, HALF + 1, soulLantern);

		// Guard towers in the corners
		for (int sx : new int[]{-HALF, HALF}) {
			for (int sz : new int[]{-HALF, HALF}) {
				for (int x = -2; x <= 2; x++)
					for (int z = -2; z <= 2; z++)
						for (int y = 0; y <= 13; y++) {
							boolean shell = Math.abs(x) == 2 || Math.abs(z) == 2 || y == 0 || y == 13;
							b.set(sx + x, y, sz + z, shell ? tile : air);
						}
				for (int x = -2; x <= 2; x += 2)
					for (int z = -2; z <= 2; z += 2) b.set(sx + x, 14, sz + z, soulLantern);
				b.set(sx, 14, sz, Blocks.END_ROD.getDefaultState());
				b.set(sx, 15, sz, Blocks.END_ROD.getDefaultState());
			}
		}

		// Cell blocks along the east and west walls
		for (int side = 0; side < 2; side++) {
			int sign = side == 0 ? 1 : -1;
			int front = 14 * sign;
			for (int k = 0; k < 5; k++) {
				int z0 = -15 + k * 6;
				for (int x = 15; x <= 19; x++)
					for (int y = 0; y <= 5; y++) {
						b.set(x * sign, y, z0 - 1, brick);
						b.set(x * sign, y, z0 + 5, brick);
						b.set(x * sign, 5, z0, brick);
						for (int z = z0; z <= z0 + 4; z++) {
							if (y == 5) b.set(x * sign, 5, z, brick);
							else if (x > 15) b.set(x * sign, y, z, air);
						}
					}
				for (int z = z0; z <= z0 + 4; z++)
					for (int y = 0; y <= 4; y++) {
						b.set(15 * sign, y, z, air);
						b.setConnected(front, y, z, bars);
					}
				b.set(17 * sign, 4, z0 + 2, hangingSoul);
				if (r.nextBoolean()) b.set(19 * sign, 4, z0 + r.nextInt(5), Blocks.CHAIN.getDefaultState());
				if (r.nextInt(3) == 0) b.set(19 * sign, 0, z0 + 4, Blocks.COBWEB.getDefaultState());

				// Who's locked up in here?
				BlockPos cell = center.add(17 * sign, 1, z0 + 2);
				int roll = r.nextInt(100);
				if (roll < 60) prisoner(w, cell, true);
				else if (roll < 85) prisoner(w, cell, false);
				else chest(b, 18 * sign, 0, z0 + 3, "chests/simple_dungeon", r);
			}
		}

		// The Jailer's hall on the north side
		b.box(-8, 0, -19, 8, 7, -11, tile);
		b.box(-7, 0, -18, 7, 6, -12, air);
		b.box(-1, 0, -11, 1, 3, -11, air);
		b.set(0, 0, -17, Blocks.POLISHED_BLACKSTONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
		b.set(-1, 0, -17, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState());
		b.set(1, 0, -17, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState());
		for (int x = -6; x <= 6; x += 4) b.set(x, 6, -15, hangingSoul);
		for (int z = -17; z <= -13; z += 2) {
			b.set(-7, 2, z, Blocks.CHAIN.getDefaultState());
			b.set(7, 2, z, Blocks.CHAIN.getDefaultState());
		}
		chest(b, -6, 0, -17, "chests/end_city_treasure", r);
		chest(b, 6, 0, -17, "chests/buried_treasure", r);
		// The confiscated-gear chest: always holds a Thermos
		b.set(-6, 0, -13, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.EAST));
		BlockEntity be = w.getBlockEntity(center.add(-6, 0, -13));
		if (be instanceof ChestBlockEntity chest) {
			chest.setStack(4, new ItemStack(ModItems.THERMOS));
			chest.setStack(10, new ItemStack(ModItems.ECTOPLASM, 8 + r.nextInt(8)));
			chest.setStack(13, new ItemStack(Items.DIAMOND, 2 + r.nextInt(3)));
			chest.setStack(16, new ItemStack(Items.GOLDEN_APPLE, 2));
		}

		// Courtyard centerpiece: a chained soul pillar
		for (int y = 0; y <= 4; y++) b.set(0, y, 2, Blocks.POLISHED_BASALT.getDefaultState());
		b.set(0, 5, 2, Blocks.SOUL_CAMPFIRE.getDefaultState());
		for (Direction d : Direction.Type.HORIZONTAL) {
			BlockPos o = BlockPos.ORIGIN.offset(d);
			b.set(o.getX(), 3, 2 + o.getZ(), Blocks.CHAIN.getDefaultState());
		}

		// The boss and its guards
		GhostJailerEntity jailer = ModEntities.GHOST_JAILER.create(w);
		if (jailer != null) {
			jailer.refreshPositionAndAngles(center.getX() + 0.5, center.getY() + 1, center.getZ() - 6.5, 180f, 0f);
			jailer.setHome(center.add(0, 1, -2), 15f);
			jailer.setPersistent();
			w.spawnEntity(jailer);
		}
		for (int i = 0; i < 2; i++) {
			AbstractGhostEntity guard = ModEntities.HOSTILE_GHOST.create(w);
			if (guard == null) continue;
			guard.refreshPositionAndAngles(center.getX() + (i == 0 ? -8 : 8), center.getY() + 2, center.getZ() + 8, 0f, 0f);
			guard.setHome(center.add(0, 1, 2), 16f);
			guard.setPersistent();
			w.spawnEntity(guard);
		}
		return jailer;
	}

	private static void prisoner(ServerWorld w, BlockPos cell, boolean hostile) {
		AbstractGhostEntity g = hostile ? ModEntities.HOSTILE_GHOST.create(w) : ModEntities.FRIENDLY_GHOST.create(w);
		if (g == null) return;
		g.refreshPositionAndAngles(cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5, w.getRandom().nextFloat() * 360f, 0f);
		g.setHome(cell, 1.2f);
		g.setPersistent();
		w.spawnEntity(g);
	}

	private static void chest(B b, int x, int y, int z, String loot, Random r) {
		b.set(x, y, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.Type.HORIZONTAL.random(r)));
		BlockEntity be = b.w.getBlockEntity(b.c.add(x, y, z));
		if (be instanceof LootableContainerBlockEntity lc) {
			RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.ofVanilla(loot));
			lc.setLootTable(key, r.nextLong());
		}
	}
}
