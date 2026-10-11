package com.phantompowers.world;

import com.phantompowers.ModBlocks;
import com.phantompowers.ModEntities;
import com.phantompowers.entity.CrateKingEntity;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The Crate King's lair: a floating warehouse arena walled with towering stacks of crates and barrels,
 * piles of boxes for cover, and his hoard of loot in the back.
 */
public final class CrateKeep {
	private static final int HALF = 18;

	private CrateKeep() {}

	public static CrateKingEntity build(ServerWorld w, BlockPos c, Random r) {
		BlockState air = Blocks.AIR.getDefaultState();
		BlockState crate = ModBlocks.GHOST_CRATE.getDefaultState();
		// Island
		for (int x = -24; x <= 24; x++) {
			for (int z = -24; z <= 24; z++) {
				double d = Math.sqrt(x * x + z * z) + r.nextDouble() * 1.5;
				if (d > 24) continue;
				set(w, c, x, -1, z, ModBlocks.ECTO_GRASS.getDefaultState());
				int depth = 2 + (int) ((24 - d) * 0.5) + r.nextInt(2);
				for (int y = -2; y >= -1 - depth; y--) set(w, c, x, y, z, ModBlocks.ECTO_STONE.getDefaultState());
				for (int y = 0; y <= 16; y++) set(w, c, x, y, z, air);
			}
		}
		// Warehouse floor
		for (int x = -HALF; x <= HALF; x++)
			for (int z = -HALF; z <= HALF; z++)
				set(w, c, x, -1, z, (Math.floorMod(x, 5) == 0 || Math.floorMod(z, 5) == 0)
						? Blocks.DARK_OAK_PLANKS.getDefaultState() : Blocks.SPRUCE_PLANKS.getDefaultState());
		// Walls of stacked crates and barrels, uneven like a pile
		for (int i = -HALF; i <= HALF; i++) {
			for (int[] edge : new int[][]{{i, -HALF}, {i, HALF}, {-HALF, i}, {HALF, i}}) {
				if (edge[1] == HALF && Math.abs(edge[0]) <= 2) continue; // entrance
				int h = 4 + r.nextInt(4);
				for (int y = 0; y < h; y++) {
					BlockState s = r.nextInt(5) == 0
							? Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.UP)
							: crate;
					set(w, c, edge[0], y, edge[1], s);
				}
			}
		}
		// Piles of boxes for cover
		for (int i = 0; i < 14; i++) {
			int px = r.nextInt(HALF * 2 - 6) - HALF + 3;
			int pz = r.nextInt(HALF * 2 - 6) - HALF + 3;
			if (Math.abs(px) < 4 && Math.abs(pz) < 4) continue;
			int size = 1 + r.nextInt(2);
			for (int x = 0; x < size; x++)
				for (int z = 0; z < size; z++) {
					int h = 1 + r.nextInt(3);
					for (int y = 0; y < h; y++) set(w, c, px + x, y, pz + z, crate);
				}
			if (r.nextBoolean()) set(w, c, px, 1 + r.nextInt(3), pz, Blocks.LANTERN.getDefaultState());
		}
		// Hanging lights over the arena
		for (int x = -12; x <= 12; x += 6)
			for (int z = -12; z <= 12; z += 6) {
				set(w, c, x, 12, z, ModBlocks.GHOST_CRATE.getDefaultState());
				set(w, c, x, 11, z, Blocks.CHAIN.getDefaultState());
				set(w, c, x, 10, z, Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
			}
		// The hoard
		String[] loot = {"chests/woodland_mansion", "chests/shipwreck_treasure", "chests/bastion_treasure", "chests/simple_dungeon"};
		for (int i = 0; i < 4; i++) {
			int x = -6 + i * 4;
			set(w, c, x, 0, -HALF + 2, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.SOUTH));
			BlockEntity be = w.getBlockEntity(c.add(x, 0, -HALF + 2));
			if (be instanceof LootableContainerBlockEntity lc) {
				RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.ofVanilla(loot[i]));
				lc.setLootTable(key, r.nextLong());
			}
		}
		// The king himself
		CrateKingEntity king = ModEntities.CRATE_KING.create(w);
		if (king != null) {
			king.refreshPositionAndAngles(c.getX() + 0.5, c.getY() + 2, c.getZ() + 0.5, 180f, 0f);
			king.setHome(c.add(0, 2, 0), 16f);
			king.setPersistent();
			w.spawnEntity(king);
		}
		return king;
	}

	private static void set(ServerWorld w, BlockPos c, int x, int y, int z, BlockState s) {
		w.setBlockState(c.add(x, y, z), s, Block.NOTIFY_LISTENERS);
	}
}
