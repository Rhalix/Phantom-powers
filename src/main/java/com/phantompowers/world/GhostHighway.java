package com.phantompowers.world;

import com.phantompowers.BikerCoupleItem;
import com.phantompowers.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * The Biker Couple's lair: a floating ghost highway. A ring road with lane markings and curbs loops around
 * an island, with street lamps, a jump ramp and a little roadside garage in the middle.
 */
public final class GhostHighway {
	private GhostHighway() {}

	public static void build(ServerWorld w, BlockPos c, Random r) {
		BlockState air = Blocks.AIR.getDefaultState();
		int R = 26;
		for (int x = -R - 3; x <= R + 3; x++)
			for (int z = -R - 3; z <= R + 3; z++) {
				double d = Math.sqrt(x * x + z * z) + r.nextDouble() * 1.2;
				if (d > R + 3) continue;
				set(w, c, x, -1, z, ModBlocks.ECTO_GRASS.getDefaultState());
				int depth = 2 + (int) ((R + 3 - d) * 0.45);
				for (int y = -2; y >= -1 - depth; y--) set(w, c, x, y, z, ModBlocks.ECTO_STONE.getDefaultState());
				for (int y = 0; y <= 12; y++) set(w, c, x, y, z, air);
			}
		// the ring road: asphalt, dashed centre line, curbs
		for (int x = -R; x <= R; x++)
			for (int z = -R; z <= R; z++) {
				double d = Math.sqrt(x * x + z * z);
				if (d < R - 7 || d > R) continue;
				BlockState s = Blocks.BLACK_CONCRETE.getDefaultState();
				if (d > R - 1 || d < R - 6) s = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
				else if (Math.abs(d - (R - 3.5)) < 0.5) {
					double ang = Math.atan2(z, x);
					if (((int) Math.floor(ang * 12)) % 2 == 0) s = Blocks.YELLOW_CONCRETE.getDefaultState();
				}
				set(w, c, x, -1, z, s);
			}
		// street lamps around the outside
		for (int i = 0; i < 12; i++) {
			double a = i * Math.PI * 2 / 12;
			int x = (int) Math.round(Math.cos(a) * (R + 1.5)), z = (int) Math.round(Math.sin(a) * (R + 1.5));
			for (int y = 0; y <= 4; y++) set(w, c, x, y, z, Blocks.DEEPSLATE_TILE_WALL.getDefaultState());
			set(w, c, x, 5, z, Blocks.SEA_LANTERN.getDefaultState());
		}
		// a jump ramp on the road
		for (int k = 0; k < 6; k++)
			for (int wd = -2; wd <= 2; wd++) set(w, c, R - 4 + wd, k / 2, -6 + k, Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState());
		// roadside garage in the middle
		for (int x = -5; x <= 5; x++)
			for (int z = -4; z <= 4; z++)
				for (int y = 0; y <= 5; y++) {
					boolean wall = Math.abs(x) == 5 || Math.abs(z) == 4;
					boolean roof = y == 5;
					BlockState s = roof ? Blocks.DARK_OAK_PLANKS.getDefaultState()
							: wall ? (y == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState() : Blocks.SPRUCE_PLANKS.getDefaultState()) : air;
					if (z == -4 && Math.abs(x) <= 2 && y <= 3) s = air;                                 // garage door opening
					if (wall && y == 2 && Math.abs(x) == 5 && Math.abs(z) <= 1) s = Blocks.GLASS_PANE.getDefaultState();
					set(w, c, x, y, z, s);
				}
		set(w, c, 0, 6, -4, Blocks.REDSTONE_LAMP.getDefaultState());
		set(w, c, -3, 0, 2, Blocks.ANVIL.getDefaultState());
		set(w, c, 3, 0, 2, Blocks.BARREL.getDefaultState());
		set(w, c, 4, 0, 2, Blocks.BARREL.getDefaultState());
		set(w, c, 0, 0, 3, Blocks.CHEST.getDefaultState());
		spawnBosses(w, c);
	}

	/** The couple: Rev on his bike with Roxy on the back, parked on the road. */
	public static void spawnBosses(ServerWorld w, BlockPos c) {
		var rev = BikerCoupleItem.spawnCouple(w, Vec3d.ofBottomCenter(c.add(0, 0, -22)), 90f);
		if (rev != null) rev.setHome(c.add(0, 1, 0), 30f);
	}

	private static void set(ServerWorld w, BlockPos c, int x, int y, int z, BlockState s) {
		w.setBlockState(c.add(x, y, z), s, net.minecraft.block.Block.NOTIFY_LISTENERS);
	}
}
