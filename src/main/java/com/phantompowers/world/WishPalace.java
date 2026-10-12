package com.phantompowers.world;

import com.phantompowers.ModBlocks;
import com.phantompowers.ModEntities;
import com.phantompowers.entity.WishkeeperEntity;
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
 * The Wishkeeper's palace: a floating island with a domed golden pavilion on a ring of pillars,
 * a reflecting pool, treasure piles and a golden beacon you can see from far away.
 */
public final class WishPalace {
	private WishPalace() {}

	private static void set(ServerWorld w, BlockPos c, int x, int y, int z, BlockState s) {
		w.setBlockState(c.add(x, y, z), s, Block.NOTIFY_LISTENERS);
	}

	public static WishkeeperEntity build(ServerWorld w, BlockPos c, Random r) {
		BlockState air = Blocks.AIR.getDefaultState();
		final int ISL = 22, R = 11, DOME = 10;
		for (int x = -ISL; x <= ISL; x++)
			for (int z = -ISL; z <= ISL; z++) {
				double d = Math.sqrt(x * x + z * z) + r.nextDouble() * 1.5;
				if (d > ISL) continue;
				set(w, c, x, -1, z, ModBlocks.ECTO_GRASS.getDefaultState());
				int depth = 2 + (int) ((ISL - d) * 0.5);
				for (int y = -2; y >= -1 - depth; y--) set(w, c, x, y, z, ModBlocks.ECTO_STONE.getDefaultState());
				for (int y = 0; y <= 24; y++) set(w, c, x, y, z, air);
			}
		// Round sandstone plaza with a gold-inlaid star
		for (int x = -R - 2; x <= R + 2; x++)
			for (int z = -R - 2; z <= R + 2; z++) {
				double d = Math.sqrt(x * x + z * z);
				if (d > R + 2) continue;
				boolean star = (Math.abs(x) == Math.abs(z) || x == 0 || z == 0) && d < R;
				set(w, c, x, -1, z, d > R + 1 ? Blocks.CUT_SANDSTONE.getDefaultState()
						: star ? Blocks.GOLD_BLOCK.getDefaultState()
						: ((x + z) % 2 == 0 ? Blocks.SMOOTH_SANDSTONE.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState()));
			}
		// Ring of pillars with gold capitals
		for (int k = 0; k < 10; k++) {
			double a = k * Math.PI * 2 / 10;
			int px = (int) Math.round(Math.cos(a) * R), pz = (int) Math.round(Math.sin(a) * R);
			for (int y = 0; y <= 7; y++) set(w, c, px, y, pz, Blocks.QUARTZ_PILLAR.getDefaultState());
			set(w, c, px, 8, pz, Blocks.GOLD_BLOCK.getDefaultState());
			set(w, c, px, 0, pz, Blocks.CHISELED_SANDSTONE.getDefaultState());
			set(w, c, px, 7, pz, Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
		}
		// The dome: gold rim, then a stained-glass shell
		for (int x = -R; x <= R; x++)
			for (int z = -R; z <= R; z++)
				for (int y = 0; y <= DOME; y++) {
					double d = Math.sqrt(x * x + z * z + (y * 1.1) * (y * 1.1));
					if (d > R + 0.5 || d < R - 0.6) continue;
					int yy = 8 + y;
					BlockState s = y == 0 ? Blocks.GOLD_BLOCK.getDefaultState()
							: y > DOME - 2 ? Blocks.GOLD_BLOCK.getDefaultState()
							: ((x * 3 + z) % 4 == 0 ? Blocks.ORANGE_STAINED_GLASS.getDefaultState() : Blocks.YELLOW_STAINED_GLASS.getDefaultState());
					set(w, c, x, yy, z, s);
				}
		// Hanging lanterns inside
		for (int k = 0; k < 6; k++) {
			double a = k * Math.PI * 2 / 6;
			set(w, c, (int) Math.round(Math.cos(a) * 6), 12, (int) Math.round(Math.sin(a) * 6), Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		}
		// Reflecting pool around a golden pedestal
		for (int x = -3; x <= 3; x++)
			for (int z = -3; z <= 3; z++) {
				double d = Math.sqrt(x * x + z * z);
				if (d > 3.3) continue;
				set(w, c, x, -1, z, d < 2.5 ? Blocks.WATER.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState());
				set(w, c, x, -2, z, Blocks.PRISMARINE_BRICKS.getDefaultState());
			}
		set(w, c, 0, -1, 0, Blocks.GOLD_BLOCK.getDefaultState());
		set(w, c, 0, 0, 0, Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
		set(w, c, 0, 1, 0, Blocks.GOLD_BLOCK.getDefaultState());
		// Treasure piles and chests
		for (int[] t : new int[][]{{-7, 0, -5}, {7, 0, -5}, {-7, 0, 5}, {7, 0, 5}}) {
			set(w, c, t[0], t[1], t[2], Blocks.GOLD_BLOCK.getDefaultState());
			set(w, c, t[0] + 1, t[1], t[2], Blocks.RAW_GOLD_BLOCK.getDefaultState());
			set(w, c, t[0], t[1] + 1, t[2], Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.getDefaultState());
		}
		chest(w, c, -8, 0, 0, Direction.EAST, "chests/desert_pyramid", r);
		chest(w, c, 8, 0, 0, Direction.WEST, "chests/buried_treasure", r);
		chest(w, c, 0, 0, -8, Direction.SOUTH, "chests/end_city_treasure", r);
		// Golden beacon on top of the dome
		int top = 8 + DOME + 1;
		for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) set(w, c, x, top - 1, z, Blocks.GOLD_BLOCK.getDefaultState());
		set(w, c, 0, top, 0, Blocks.BEACON.getDefaultState());
		set(w, c, 0, top + 1, 0, Blocks.YELLOW_STAINED_GLASS.getDefaultState());
		for (int yy = top + 2; yy < w.getTopY() - c.getY(); yy++) {
			if (!w.getBlockState(c.add(0, yy, 0)).isAir()) set(w, c, 0, yy, 0, air);
		}
		// Entrance arch on the south side
		for (int x = -2; x <= 2; x++) set(w, c, x, -1, R + 2, Blocks.CUT_SANDSTONE.getDefaultState());

		return spawnBoss(w, c);
	}

	public static WishkeeperEntity spawnBoss(ServerWorld w, BlockPos c) {
		WishkeeperEntity k = ModEntities.WISHKEEPER.create(w);
		if (k == null) return null;
		k.refreshPositionAndAngles(c.getX() + 0.5, c.getY() + 3, c.getZ() + 0.5, 180f, 0f);
		k.setHome(c.add(0, 3, 0), 14f);
		k.setPersistent();
		w.spawnEntity(k);
		return k;
	}

	private static void chest(ServerWorld w, BlockPos c, int x, int y, int z, Direction face, String loot, Random r) {
		set(w, c, x, y, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, face));
		BlockEntity be = w.getBlockEntity(c.add(x, y, z));
		if (be instanceof LootableContainerBlockEntity lc) {
			RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.ofVanilla(loot));
			lc.setLootTable(key, r.nextLong());
		}
	}
}
