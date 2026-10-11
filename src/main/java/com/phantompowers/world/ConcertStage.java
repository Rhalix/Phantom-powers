package com.phantompowers.world;

import com.phantompowers.ModBlocks;
import com.phantompowers.ModEntities;
import com.phantompowers.entity.VoltageEntity;
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

/** Voltage's floating concert stage: a big stage, speaker towers, a lighting rig and a mosh pit. */
public final class ConcertStage {
	private ConcertStage() {}

	public static VoltageEntity build(ServerWorld w, BlockPos c, Random r) {
		BlockState air = Blocks.AIR.getDefaultState();
		for (int x = -24; x <= 24; x++)
			for (int z = -24; z <= 24; z++) {
				double d = Math.sqrt(x * x + z * z) + r.nextDouble() * 1.5;
				if (d > 24) continue;
				set(w, c, x, -1, z, ModBlocks.ECTO_GRASS.getDefaultState());
				int depth = 2 + (int) ((24 - d) * 0.5);
				for (int y = -2; y >= -1 - depth; y--) set(w, c, x, y, z, ModBlocks.ECTO_STONE.getDefaultState());
				for (int y = 0; y <= 16; y++) set(w, c, x, y, z, air);
			}
		// Mosh pit floor with light-up tiles
		for (int x = -16; x <= 16; x++)
			for (int z = -6; z <= 16; z++)
				set(w, c, x, -1, z, (Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0)
						? Blocks.SEA_LANTERN.getDefaultState() : Blocks.POLISHED_BLACKSTONE.getDefaultState());
		// Stage platform
		for (int x = -12; x <= 12; x++)
			for (int z = -16; z <= -7; z++)
				for (int y = 0; y <= 1; y++)
					set(w, c, x, y, z, y == 1 ? Blocks.BLACK_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState());
		for (int x = -12; x <= 12; x++) set(w, c, x, 1, -7, Blocks.PURPLE_CONCRETE.getDefaultState());
		// Back wall with a lightning bolt in lights
		for (int x = -12; x <= 12; x++)
			for (int y = 2; y <= 9; y++) set(w, c, x, y, -16, Blocks.BLACK_CONCRETE.getDefaultState());
		int[][] bolt = {{1, 9}, {0, 8}, {0, 7}, {-1, 6}, {1, 6}, {0, 5}, {1, 5}, {0, 4}, {-1, 3}};
		for (int[] p : bolt) set(w, c, p[0], p[1], -16, Blocks.GLOWSTONE.getDefaultState());
		// Speaker towers
		for (int sx : new int[]{-11, 9}) {
			for (int x = 0; x < 3; x++)
				for (int z = 0; z < 3; z++)
					for (int y = 2; y <= 9; y++) {
						BlockState s = (y % 3 == 0 && x == 1) ? Blocks.NOTE_BLOCK.getDefaultState() : Blocks.BLACK_CONCRETE.getDefaultState();
						if (z == 2 && y % 3 == 1 && x == 1) s = Blocks.JUKEBOX.getDefaultState();
						set(w, c, sx + x, y, -14 + z, s);
					}
		}
		// Lighting rig overhead
		for (int x = -12; x <= 12; x++) set(w, c, x, 12, -9, Blocks.IRON_BARS.getDefaultState());
		BlockState[] glass = {Blocks.MAGENTA_STAINED_GLASS.getDefaultState(), Blocks.PURPLE_STAINED_GLASS.getDefaultState(),
				Blocks.YELLOW_STAINED_GLASS.getDefaultState(), Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState()};
		for (int x = -10; x <= 10; x += 4) {
			set(w, c, x, 11, -9, Blocks.GLOWSTONE.getDefaultState());
			set(w, c, x, 10, -9, glass[Math.floorMod(x / 4, glass.length)]);
		}
		for (int y = 0; y <= 12; y++) {
			set(w, c, -13, y, -9, Blocks.IRON_BARS.getDefaultState());
			set(w, c, 13, y, -9, Blocks.IRON_BARS.getDefaultState());
		}
		// Backstage loot
		String[] loot = {"chests/bastion_treasure", "chests/ancient_city"};
		for (int i = 0; i < 2; i++) {
			int x = i == 0 ? -6 : 6;
			set(w, c, x, 2, -15, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.SOUTH));
			BlockEntity be = w.getBlockEntity(c.add(x, 2, -15));
			if (be instanceof LootableContainerBlockEntity lc) {
				lc.setLootTable(RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.ofVanilla(loot[i])), r.nextLong());
			}
		}
		set(w, c, 0, 2, -14, Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, false));

		VoltageEntity v = ModEntities.VOLTAGE.create(w);
		if (v != null) {
			v.refreshPositionAndAngles(c.getX() + 0.5, c.getY() + 3, c.getZ() - 11.5, 0f, 0f);
			v.setHome(c.add(0, 3, -4), 16f);
			v.setPersistent();
			w.spawnEntity(v);
		}
		return v;
	}

	private static void set(ServerWorld w, BlockPos c, int x, int y, int z, BlockState s) {
		w.setBlockState(c.add(x, y, z), s, Block.NOTIFY_LISTENERS);
	}
}
