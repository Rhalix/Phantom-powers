package com.phantompowers.world;

import com.phantompowers.ModBlocks;
import com.phantompowers.ModEntities;
import com.phantompowers.entity.DoloraEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Dolora's Gloom Manor: a lonely floating island with a dark, purple-glassed manor. Inside is her "counseling office",
 * a big gloomy hall with a fainting couch, a desk, bookshelves and soul-lantern chandeliers... and her, waiting.
 */
public final class GloomManor {
	private GloomManor() {}

	private static void set(ServerWorld w, BlockPos c, int x, int y, int z, BlockState s) {
		w.setBlockState(c.add(x, y, z), s, Block.NOTIFY_LISTENERS);
	}

	public static DoloraEntity build(ServerWorld w, BlockPos c, Random r) {
		BlockState air = Blocks.AIR.getDefaultState();
		final int ISL = 20, H = 11, HALF = 9;
		// island
		for (int x = -ISL; x <= ISL; x++)
			for (int z = -ISL; z <= ISL; z++) {
				double d = Math.sqrt(x * x + z * z) + r.nextDouble() * 1.5;
				if (d > ISL) continue;
				set(w, c, x, -1, z, ModBlocks.SHADOW_GRASS.getDefaultState());
				int depth = 2 + (int) ((ISL - d) * 0.5);
				for (int y = -2; y >= -1 - depth; y--) set(w, c, x, y, z, y > -4 ? Blocks.SOUL_SOIL.getDefaultState() : ModBlocks.ECTO_STONE.getDefaultState());
				for (int y = 0; y <= H + 6; y++) set(w, c, x, y, z, air);
			}
		// floor: checkered dark oak and purple carpet aisle
		for (int x = -HALF; x <= HALF; x++)
			for (int z = -HALF; z <= HALF; z++) {
				set(w, c, x, -1, z, ((x + z) & 1) == 0 ? Blocks.DARK_OAK_PLANKS.getDefaultState() : Blocks.SPRUCE_PLANKS.getDefaultState());
				if (Math.abs(x) <= 1 && z > -HALF) set(w, c, x, 0, z, Blocks.PURPLE_CARPET.getDefaultState());
			}
		// walls: deepslate brick pillars, purple and black stained glass windows
		for (int y = 0; y < H; y++)
			for (int i = -HALF; i <= HALF; i++) {
				boolean pillar = Math.abs(i) == HALF || i % 3 == 0;
				BlockState wall = pillar || y == 0 || y == H - 1 ? Blocks.DEEPSLATE_BRICKS.getDefaultState()
						: (y + i) % 4 == 0 ? Blocks.BLACK_STAINED_GLASS.getDefaultState() : Blocks.PURPLE_STAINED_GLASS.getDefaultState();
				set(w, c, i, y, -HALF, wall);
				set(w, c, i, y, HALF, wall);
				set(w, c, -HALF, y, i, wall);
				set(w, c, HALF, y, i, wall);
			}
		// doorway at the front
		for (int y = 0; y < 4; y++) for (int x = -1; x <= 1; x++) set(w, c, x, y, HALF, air);
		// stepped roof
		for (int k = 0; k <= HALF; k++) {
			int y = H + k / 2;
			for (int i = -HALF + k; i <= HALF - k; i++) {
				set(w, c, i, y, -HALF + k, Blocks.DEEPSLATE_TILES.getDefaultState());
				set(w, c, i, y, HALF - k, Blocks.DEEPSLATE_TILES.getDefaultState());
				set(w, c, -HALF + k, y, i, Blocks.DEEPSLATE_TILES.getDefaultState());
				set(w, c, HALF - k, y, i, Blocks.DEEPSLATE_TILES.getDefaultState());
			}
		}
		// a little spire with a glowing crystal
		for (int y = H + 5; y < H + 9; y++) set(w, c, 0, y, 0, Blocks.DEEPSLATE_BRICK_WALL.getDefaultState());
		set(w, c, 0, H + 9, 0, ModBlocks.SPECTRAL_CRYSTAL.getDefaultState());
		// chandeliers
		for (int[] p : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}, {0, 0}}) {
			set(w, c, p[0], H - 2, p[1], Blocks.CHAIN.getDefaultState());
			set(w, c, p[0], H - 3, p[1], Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		}
		// the "office" at the back: desk, couch, bookshelves
		for (int x = -3; x <= 3; x++) set(w, c, x, 0, -HALF + 1, Blocks.BOOKSHELF.getDefaultState());
		for (int x = -3; x <= 3; x++) set(w, c, x, 1, -HALF + 1, Blocks.BOOKSHELF.getDefaultState());
		for (int x = -2; x <= 2; x++) set(w, c, x, 0, -HALF + 3, Blocks.DARK_OAK_SLAB.getDefaultState().with(net.minecraft.block.SlabBlock.TYPE, net.minecraft.block.enums.SlabType.TOP));
		set(w, c, -1, 1, -HALF + 3, Blocks.CANDLE.getDefaultState().with(net.minecraft.block.CandleBlock.LIT, true));
		set(w, c, 1, 1, -HALF + 3, Blocks.BREWING_STAND.getDefaultState());
		for (int z = -2; z <= 2; z++) {
			set(w, c, -HALF + 1, 0, z, Blocks.PURPLE_WOOL.getDefaultState());
			set(w, c, -HALF + 2, 0, z, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
		}
		for (int z = -6; z <= 6; z += 3) {
			set(w, c, HALF - 1, 0, z, Blocks.BOOKSHELF.getDefaultState());
			set(w, c, HALF - 1, 1, z, Blocks.FLOWER_POT.getDefaultState());
		}
		return spawnBoss(w, c);
	}

	public static DoloraEntity spawnBoss(ServerWorld w, BlockPos c) {
		DoloraEntity d = ModEntities.DOLORA.create(w);
		if (d == null) return null;
		d.refreshPositionAndAngles(c.getX() + 0.5, c.getY(), c.getZ() - 4.5, 0, 0);
		d.setHome(c.add(0, 1, 0), 9f);
		d.setPersistent();
		w.spawnEntity(d);
		return d;
	}
}
