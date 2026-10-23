package com.phantompowers.world;

import com.phantompowers.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * The big Amity Park: a 500 x 500 town. The original town (Fenton lab house, Casper High, the Nasty Burger, the park
 * and the rest) sits in the middle, and around it are a road grid with more neighbourhoods, shops, offices, parks and
 * landmarks like the Fenton Works ops tower.
 */
public final class AmityPark500 {
	public static final int HALF = 250;

	private AmityPark500() {}

	static Block block(String name) {
		Block b = Registries.BLOCK.get(Identifier.ofVanilla(name));
		if (b == Blocks.AIR && !name.equals("air")) throw new IllegalArgumentException(name);
		return b;
	}

	static BlockState s(String name) {
		return block(name).getDefaultState();
	}

	/** Fill a box, with y measured from ground level. */
	static void fill(AmityPark.Builder b, int x0, int y0, int z0, int x1, int y1, int z1, String block) {
		b.fill(x0, b.g + y0, z0, x1, b.g + y1, z1, s(block));
	}

	/** Set one block, with y measured from ground level. */
	static void set(AmityPark.Builder b, int x, int y, int z, String block) {
		b.set(x, b.g + y, z, s(block));
	}

	public static List<Runnable> plan(AmityPark.Builder b) {
		return plan(b, true);
	}

	/**
	 * The build as small jobs. {@code big = false} builds just the original middle of town (used when the town has
	 * to be built all at once, like the automatic tests, where the whole 500 x 500 town would freeze the server).
	 */
	public static List<Runnable> plan(AmityPark.Builder b, boolean big) {
		List<Runnable> t = new ArrayList<>();
		int half = big ? HALF : 128;
		t.add(b::pickLawn);
		for (int x = -half; x < half; x += 16)
			for (int z = -half; z < half; z += 8) {
				final int a = x, c = z;
				t.add(() -> terrain(b, a, c));
			}
		t.add(b::roads);
		t.add(() -> b.labHouse(14, -44));
		t.add(() -> b.library(62, -44));
		t.add(() -> {
			b.school(-98, -98);
			b.schoolExtras(-98, -98);
		});
		t.add(() -> b.field(-50, -98));
		t.add(() -> b.diner(-48, -30));
		t.add(() -> b.store(-24, -28));
		t.add(() -> b.townHall(-98, -42));
		t.add(() -> b.apartments(-48, 12));
		t.add(() -> b.park(8, 8));
		t.add(() -> b.house(-96, 14, 0, false));
		int style = 1;
		for (int lot : new int[]{8, 58}) {
			int s1 = style++, s2 = style++;
			t.add(() -> b.house(lot + 3, -74, s1, true));
			t.add(() -> b.house(lot + 24, -74, s2, true));
		}
		for (int lot : new int[]{8, 58, -50, -100}) {
			int s1 = style++, s2 = style++;
			t.add(() -> b.house(lot + 3, 60, s1, false));
			t.add(() -> b.house(lot + 24, 60, s2, false));
		}
		if (big) for (int c : new int[]{-224, -160, -112, 0, 112, 176, 224}) {
			for (int z = -HALF; z < HALF; z += 16) {
				final int from = z, to = Math.min(z + 15, HALF - 1);
				t.add(() -> road(b, false, c, from, to));
				t.add(() -> road(b, true, c, from, to));
			}
		}
		if (big) for (int i = 0; i < 67; i++) {
			final int lot = i;
			t.add(() -> lot(b, lot));
		}
		t.add(() -> {
			b.lamps();
			b.welcome();
		});
		t.add(() -> landmarks(b, big));
		t.add(b::connect);
		return t;
	}

	/** Level a 16 x 8 patch: dirt underneath, grass on top, air above. */
	static void terrain(AmityPark.Builder b, int x0, int z0) {
		BlockState dirt = s("dirt"), air = s("air"), grass = s("grass_block");
		for (int x = x0; x < Math.min(HALF, x0 + 16); x++) {
			for (int z = z0; z < Math.min(HALF, z0 + 8); z++) {
				int top = AmityPark.surface(b.w, b.c.getX() + x, b.c.getZ() + z);
				int lo = Math.min(top - 2, b.g - 8), hi = Math.max(top + 8, b.g + 64);
				for (int y = lo; y < b.g - 1; y++) if (!b.get(x, y, z).equals(dirt)) b.set(x, y, z, dirt);
				b.set(x, b.g - 1, z, grass);
				for (int y = b.g; y <= hi; y++) if (!b.get(x, y, z).equals(air)) b.set(x, y, z, air);
			}
		}
	}

	/** A 13-wide road (gray, with a dashed yellow centre line and pavement edges) along x or z, with street lamps. */
	static void road(AmityPark.Builder b, boolean alongX, int c, int from, int to) {
		for (int i = from; i <= to; i++) {
			for (int o = -6; o <= 6; o++) {
				int x = alongX ? i : c + o, z = alongX ? c + o : i;
				String m = Math.abs(o) >= 5 ? "smooth_stone" : "gray_concrete";
				if (o == 0 && Math.floorMod(i, 8) < 4) m = "yellow_concrete";
				set(b, x, -1, z, m);
			}
			if (Math.floorMod(i, 32) == 0) {
				int lx = alongX ? i : c + 6, lz = alongX ? c + 6 : i;
				b.lamp(lx, b.g, lz);
			}
		}
	}

	static void lot(AmityPark.Builder b, int i) {
		switch (i) {
			case 0 -> building(b, -215, -215, 34, 34, 3, "mansion", "Manson Residence", -160);
			case 1 -> building(b, -215, -151, 34, 30, 2, "warehouse", "Industrial Warehouse", -112);
			case 2 -> building(b, -215, -103, 34, 34, 3, "civic", "Amity Museum", 0);
			case 3 -> building(b, -215, 9, 34, 34, 2, "civic", "Public Pool", 112);
			case 4 -> building(b, -215, 121, 34, 34, 3, "library", "Historic Library", 176);
			case 5 -> building(b, -215, 185, 34, 30, 2, "hospital", "Neighborhood Clinic", 224);
			case 6 -> building(b, -151, -215, 30, 34, 3, "hospital", "Amity Medical", -160);
			case 7 -> building(b, -151, -151, 30, 30, 4, "apartment", "Downtown Apartments", -112);
			case 8 -> residence(b, -151, -103, 1, false, -112);
			case 9 -> residence(b, -151, -23, 2, true, 0);
			case 10 -> residence(b, -151, 9, 3, false, 0);
			case 11 -> residence(b, -151, 89, 4, true, 112);
			case 12 -> residence(b, -151, 121, 5, false, 112);
			case 13 -> residence(b, -151, 153, 6, true, 176);
			case 14 -> residence(b, -151, 201, 0, true, 224);
			case 15 -> building(b, -103, -215, 48, 34, 2, "mall", "Amity Park Mall", -160);
			case 16 -> residence(b, -103, -135, 2, true, -112);
			case 17 -> residence(b, -81, -135, 3, true, -112);
			case 18 -> residence(b, -59, -135, 4, true, -112);
			case 19 -> residence(b, -37, -135, 5, true, -112);
			case 20 -> residence(b, -103, 121, 6, false, 112);
			case 21 -> residence(b, -103, 153, 0, true, 176);
			case 22 -> residence(b, -81, 121, 1, false, 112);
			case 23 -> residence(b, -81, 153, 2, true, 176);
			case 24 -> residence(b, -59, 121, 3, false, 112);
			case 25 -> residence(b, -59, 153, 4, true, 176);
			case 26 -> residence(b, -37, 121, 5, false, 112);
			case 27 -> residence(b, -37, 153, 6, true, 176);
			case 28 -> residence(b, -103, 201, 0, true, 224);
			case 29 -> residence(b, -81, 201, 1, true, 224);
			case 30 -> residence(b, -59, 201, 2, true, 224);
			case 31 -> residence(b, -37, 201, 3, true, 224);
			case 32 -> building(b, 9, -215, 34, 34, 5, "office", "Vladco Offices", -160);
			case 33 -> residence(b, 9, -135, 5, true, -112);
			case 34 -> residence(b, 31, -135, 6, true, -112);
			case 35 -> residence(b, 53, -135, 0, true, -112);
			case 36 -> residence(b, 75, -135, 1, true, -112);
			case 37 -> residence(b, 9, 121, 2, false, 112);
			case 38 -> residence(b, 9, 153, 3, true, 176);
			case 39 -> residence(b, 31, 121, 4, false, 112);
			case 40 -> residence(b, 31, 153, 5, true, 176);
			case 41 -> residence(b, 53, 121, 6, false, 112);
			case 42 -> residence(b, 53, 153, 0, true, 176);
			case 43 -> residence(b, 75, 121, 1, false, 112);
			case 44 -> residence(b, 75, 153, 2, true, 176);
			case 45 -> residence(b, 9, 201, 3, true, 224);
			case 46 -> residence(b, 31, 201, 4, true, 224);
			case 47 -> residence(b, 53, 201, 5, true, 224);
			case 48 -> residence(b, 75, 201, 6, true, 224);
			case 49 -> building(b, 121, -215, 34, 34, 3, "lab", "Axion Labs", -160);
			case 50 -> building(b, 121, -151, 34, 30, 2, "cinema", "Cinema", -112);
			case 51 -> residence(b, 121, -103, 2, false, -112);
			case 52 -> residence(b, 121, -23, 3, true, 0);
			case 53 -> residence(b, 143, -103, 4, false, -112);
			case 54 -> residence(b, 143, -23, 5, true, 0);
			case 55 -> residence(b, 121, 9, 6, false, 0);
			case 56 -> residence(b, 121, 89, 0, true, 112);
			case 57 -> residence(b, 143, 9, 1, false, 0);
			case 58 -> residence(b, 143, 89, 2, true, 112);
			case 59 -> building(b, 121, 121, 34, 34, 1, "pavilion", "East Park Pavilion", 176);
			case 60 -> building(b, 121, 185, 34, 30, 1, "pavilion", "Garden Pavilion", 224);
			case 61 -> building(b, 185, -215, 30, 34, 2, "civic", "City Fire Station", -160);
			case 62 -> building(b, 185, -151, 30, 30, 2, "civic", "Community Center", -112);
			case 63 -> building(b, 185, -103, 30, 34, 4, "apartment", "Riverfront Apartments", 0);
			case 64 -> building(b, 185, 9, 30, 34, 2, "mall", "Shopping Arcade", 112);
			case 65 -> green(b, 185, 121, 30, 34, "East Park Lake");
			case 66 -> green(b, 185, 185, 30, 30, "Community Gardens");

			default -> {}
		}
	}

	/** A path from a front door out to the road. */
	static void walkway(AmityPark.Builder b, int x, int z0, int z1) {
		fill(b, x - 1, -1, Math.min(z0, z1), x + 1, -1, Math.max(z0, z1), "smooth_stone");
	}

	static void residence(AmityPark.Builder b, int x, int z, int style, boolean frontSouth, int roadZ) {
		b.house(x, z, style, frontSouth);
		walkway(b, x + 7, frontSouth ? z + 14 : z - 1, roadZ);
	}

	/** A furnished multi-storey building with a sign by the door and a path to the road. */
	static void building(AmityPark.Builder b, int x, int z, int w, int d, int floors, String kind, String name, int roadZ) {
		String wall = kind.equals("mansion") ? "white_concrete" : kind.equals("warehouse") ? "stone_bricks"
				: kind.equals("lab") ? "iron_block" : kind.equals("hospital") ? "white_concrete" : "bricks";
		b.shell(x, z, w, d, floors, s(wall), s("quartz_block"), s("oak_planks"), s("light_blue_stained_glass_pane"));
		int door = x + w / 2;
		b.doubleDoor(door, b.g, z + d - 1, Direction.SOUTH, block("oak_door"));
		walkway(b, door, z + d, roadZ);
		b.sign(door - 1, b.g + 3, z + d, Direction.SOUTH, true, name, "Amity Park", "Entrance", "");
		for (int f = 0; f < floors; f++) {
			int y = b.g + f * 5;
			if (f < floors - 1) b.stairsUp(x + w - 4, z + d - 9, y, b.stairs(block("oak_stairs"), Direction.SOUTH), s("oak_planks"));
			for (int tx = x + 3; tx < x + w - 7; tx += 6) {
				b.furn(tx, y, z + 4, ModBlocks.DINING_TABLE, Direction.SOUTH);
				b.furn(tx, y, z + 3, ModBlocks.OAK_CHAIR, Direction.SOUTH);
				b.furn(tx, y, z + 5, ModBlocks.OAK_CHAIR, Direction.NORTH);
			}
			b.furn(x + 1, y, z + 2, ModBlocks.FRIDGE, Direction.EAST);
			b.furn(x + 1, y, z + 4, ModBlocks.KITCHEN_COUNTER, Direction.EAST);
			b.furn(x + 1, y, z + 6, ModBlocks.LOCKER, Direction.EAST);
			b.furn(x + w - 2, y, z + 2, ModBlocks.BOOKCASE, Direction.WEST);
			b.set(x + 2, y, z + 1, s("crafting_table"));
			b.set(x + 3, y, z + 1, s("smoker"));
			b.set(x + 4, y, z + 1, s("furnace"));
			b.set(door, y + 2, z, s("redstone_lamp"));
			b.furn(door, y + 2, z + 1, block("lever"), Direction.SOUTH);
			b.furn(x + 3, y, z + d - 3, ModBlocks.SOFA, Direction.NORTH);
			b.furn(x + 4, y, z + d - 3, ModBlocks.SOFA, Direction.NORTH);
			b.furn(x + 3, y, z + d - 5, ModBlocks.COFFEE_TABLE, Direction.NORTH);
			if (kind.equals("hospital") || kind.equals("apartment") || kind.equals("mansion"))
				for (int bx = x + 2; bx < x + w / 2 - 2; bx += 4) b.bed(bx, y, z + d - 7, Direction.SOUTH, block("white_bed"));
			if (kind.equals("library") || kind.equals("mall"))
				for (int bz = z + 8; bz < z + d - 5; bz += 3) b.furn(x + 1, y, bz, ModBlocks.BOOKCASE, Direction.EAST);
			if (kind.equals("lab"))
				for (int bz = z + 8; bz < z + d - 5; bz += 3) b.furn(x + 1, y, bz, ModBlocks.LAB_CONSOLE, Direction.EAST);
			for (int bx = x + 2; bx < x + w - 5; bx += 5) b.set(bx, y, z + d - 2, s("barrel"));
		}
	}

	/** A park: lawn, trees, crossing paths, benches and lamps, a pond and a sign. */
	static void green(AmityPark.Builder b, int x, int z, int w, int d, String name) {
		fill(b, x, -1, z, x + w - 1, -1, z + d - 1, "grass_block");
		for (int tx = x + 3; tx < x + w - 3; tx += 8)
			for (int tz = z + 3; tz < z + d - 3; tz += 8) b.tree(tx, tz);
		fill(b, x + w / 2 - 1, -1, z, x + w / 2 + 1, -1, z + d - 1, "smooth_stone");
		fill(b, x, -1, z + d / 2 - 1, x + w - 1, -1, z + d / 2 + 1, "smooth_stone");
		for (int tx = x + 3; tx < x + w - 3; tx += 8) {
			b.furn(tx, b.g, z + d / 2 + 3, ModBlocks.SOFA, Direction.NORTH);
			b.lamp(tx, b.g, z + d / 2 - 3);
		}
		fill(b, x + 4, -3, z + 4, x + Math.max(7, w / 3), -3, z + Math.max(7, d / 3), "stone_bricks");
		fill(b, x + 4, -2, z + 4, x + Math.max(7, w / 3), -1, z + Math.max(7, d / 3), "water");
		b.sign(x + w / 2, b.g, z, Direction.NORTH, false, name, "Amity Park", "Public gardens", "");
	}

	/** Fenton Works' ops tower, Casper High's third floor, the Nasty Burger sign and the big welcome billboard. */
	static void landmarks(AmityPark.Builder b, boolean big) {
		// the Fenton Works ops tower on top of the lab house
		fill(b, 22, 20, -38, 34, 20, -28, "iron_block");
		fill(b, 20, 21, -36, 36, 21, -30, "iron_block");
		b.walls(22, b.g + 21, -36, 34, b.g + 25, -30, s("light_blue_stained_glass_pane"));
		fill(b, 20, 26, -38, 36, 26, -28, "iron_block");
		fill(b, 26, 27, -35, 30, 28, -31, "gray_concrete");
		fill(b, 28, 29, -33, 28, 39, -33, "iron_bars");
		for (int i = 0; i < 3; i++) {
			fill(b, 16 + i * 2, 22 + i, -35, 18 + i * 2, 22 + i, -33, "iron_block");
			fill(b, 36 + i * 2, 22 + i, -35, 38 + i * 2, 22 + i, -33, "iron_block");
		}
		b.sign(26, b.g + 12, -21, Direction.SOUTH, true, "FENTON", "WORKS", "Ghost Research", "");
		b.stairsUp(35, -40, b.g + 10, b.stairs(block("oak_stairs"), Direction.SOUTH), s("iron_block"));
		b.stairsUp(35, -40, b.g + 15, b.stairs(block("oak_stairs"), Direction.SOUTH), s("iron_block"));
		b.door(34, b.g + 21, -33, Direction.EAST, block("oak_door"));
		b.sign(-80, b.g + 7, -68, Direction.SOUTH, true, "CASPER", "HIGH", "SCHOOL", "");
		b.sign(-41, b.g + 3, -14, Direction.SOUTH, true, "NASTY BURGER", "Burgers & fries", "Open for business", "");
		// Casper High's third floor
		fill(b, -97, 10, -97, -60, 14, -70, "air");
		b.fill(-98, b.g + 10, -98, -59, b.g + 14, -98, s("bricks"));
		b.fill(-98, b.g + 10, -69, -59, b.g + 14, -69, s("bricks"));
		b.fill(-98, b.g + 10, -98, -98, b.g + 14, -69, s("bricks"));
		b.fill(-59, b.g + 10, -98, -59, b.g + 14, -69, s("bricks"));
		b.fill(-97, b.g + 9, -97, -60, b.g + 9, -70, s("oak_planks"));
		b.fill(-98, b.g + 15, -98, -59, b.g + 15, -69, s("quartz_block"));
		b.stairsUp(-62, -79, b.g + 5, b.stairs(block("oak_stairs"), Direction.SOUTH), s("oak_planks"));
		for (int x = -95; x < -62; x += 4) {
			b.fill(x, b.g + 11, -69, x + 1, b.g + 12, -69, s("glass_pane"));
			b.fill(x, b.g + 11, -98, x + 1, b.g + 12, -98, s("glass_pane"));
		}
		for (int x = -94; x < -69; x += 5)
			for (int z = -94; z < -82; z += 5) {
				b.furn(x, b.g + 10, z, ModBlocks.SCHOOL_DESK, Direction.SOUTH);
				b.furn(x, b.g + 10, z - 1, ModBlocks.OAK_CHAIR, Direction.SOUTH);
			}
		if (!big) return;
		// the welcome billboard
		fill(b, -235, 0, 232, -235, 8, 232, "dark_oak_planks");
		fill(b, -215, 0, 232, -215, 8, 232, "dark_oak_planks");
		fill(b, -236, 6, 232, -214, 14, 232, "yellow_concrete");
		b.sign(-227, b.g + 11, 233, Direction.SOUTH, true, "AMITY PARK", "A NICE PLACE", "TO LIVE", "");
	}
}
