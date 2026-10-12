package com.phantompowers.world;

import com.phantompowers.ModEntities;
import com.phantompowers.ModItems;
import com.phantompowers.entity.AbstractGhostEntity;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * The inside of the Ghost Prison: a two-story admin block with a cafeteria, the warden's office,
 * an armory, an infirmary and a visiting room (Big House), or an admin wing (Spire).
 * No doors (doors in the Ghost Zone lead to other rooms!), just open doorways.
 */
public final class PrisonInterior {
	private PrisonInterior() {}

	private static final class P {
		final ServerWorld w;
		final BlockPos c;
		final Random r;

		P(ServerWorld w, BlockPos c, Random r) {
			this.w = w;
			this.c = c;
			this.r = r;
		}

		void set(int x, int y, int z, BlockState s) {
			w.setBlockState(c.add(x, y, z), s, Block.NOTIFY_LISTENERS);
		}

		void box(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
			for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
				for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
					for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, s);
		}

		void bars(int x, int y, int z, BlockState s) {
			BlockPos p = c.add(x, y, z);
			w.setBlockState(p, Block.postProcessState(s, w, p), Block.NOTIFY_ALL);
		}

		BlockState stair(Block b, Direction facing, boolean upsideDown) {
			return b.getDefaultState().with(StairsBlock.FACING, facing).with(StairsBlock.HALF, upsideDown ? BlockHalf.TOP : BlockHalf.BOTTOM);
		}

		BlockState slabTop(Block b) {
			return b.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP);
		}

		void chest(int x, int y, int z, Direction face, String loot) {
			set(x, y, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, face));
			BlockEntity be = w.getBlockEntity(c.add(x, y, z));
			if (be instanceof LootableContainerBlockEntity lc) {
				RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.ofVanilla(loot));
				lc.setLootTable(key, r.nextLong());
			}
		}

		void bed(int x, int y, int z, Direction facing) {
			BlockState foot = Blocks.WHITE_BED.getDefaultState().with(BedBlock.FACING, facing).with(BedBlock.PART, BedPart.FOOT);
			BlockState head = Blocks.WHITE_BED.getDefaultState().with(BedBlock.FACING, facing).with(BedBlock.PART, BedPart.HEAD);
			set(x, y, z, foot);
			BlockPos h = BlockPos.ORIGIN.offset(facing);
			set(x + h.getX(), y, z + h.getZ(), head);
		}

		void ghost(int x, int y, int z, boolean hostile, String name, float radius) {
			AbstractGhostEntity g = hostile ? ModEntities.HOSTILE_GHOST.create(w) : ModEntities.FRIENDLY_GHOST.create(w);
			if (g == null) return;
			BlockPos at = c.add(x, y, z);
			g.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360f, 0f);
			g.setHome(at, radius);
			g.setPersistent();
			if (name != null) g.setCustomName(Text.literal(name));
			w.spawnEntity(g);
		}
	}

	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	// ---------------------------------------------------------------- rooms

	/** Cafeteria: serving line along the back wall, long tables with benches, trays and food. */
	private static void cafeteria(P p, int x1, int z1, int x2, int z2, int y, boolean servingOnWest) {
		int xa = Math.min(x1, x2), xb = Math.max(x1, x2), za = Math.min(z1, z2), zb = Math.max(z1, z2);
		// checkered floor
		for (int x = xa; x <= xb; x++)
			for (int z = za; z <= zb; z++)
				p.set(x, y - 1, z, (x + z) % 2 == 0 ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState());
		// serving line
		int sx = servingOnWest ? xa : xb;
		int cx = servingOnWest ? xa + 1 : xb - 1;
		for (int z = za; z <= zb; z++) {
			int k = z - za;
			BlockState back = switch (k % 5) {
				case 0 -> Blocks.SMOKER.getDefaultState();
				case 1 -> Blocks.CAULDRON.getDefaultState();
				case 2 -> Blocks.BARREL.getDefaultState();
				case 3 -> Blocks.FURNACE.getDefaultState();
				default -> Blocks.CRAFTING_TABLE.getDefaultState();
			};
			p.set(sx, y, z, back);
			p.set(cx, y, z, Blocks.SMOOTH_QUARTZ.getDefaultState());           // counter
			if (k % 3 == 1) p.set(cx, y + 1, z, Blocks.CAKE.getDefaultState());
			else if (k % 3 == 2) p.set(cx, y + 1, z, Blocks.LIGHT_GRAY_CARPET.getDefaultState()); // trays
			p.set(sx, y + 2, z, Blocks.IRON_TRAPDOOR.getDefaultState());         // vent hood
		}
		p.set(cx, y + 1, za, Blocks.CAULDRON.getDefaultState());
		// tables: rows of slab tables with stair benches either side
		int tx0 = servingOnWest ? xa + 4 : xa + 1;
		int tx1 = servingOnWest ? xb - 1 : xb - 4;
		for (int z = za + 1; z <= zb - 1; z += 4) {
			for (int x = tx0; x <= tx1; x++) {
				p.set(x, y, z, p.slabTop(Blocks.SPRUCE_SLAB));
				if (z - 1 >= za) p.set(x, y, z - 1, p.stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
				if (z + 1 <= zb) p.set(x, y, z + 1, p.stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
				if (p.r.nextInt(3) == 0) p.set(x, y + 1, z, p.r.nextBoolean() ? Blocks.LIGHT_GRAY_CARPET.getDefaultState() : Blocks.FLOWER_POT.getDefaultState());
			}
		}
		for (int x = xa + 2; x <= xb - 1; x += 4)
			for (int z = za + 1; z <= zb; z += 4) p.set(x, y + 4, z, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		p.chest(servingOnWest ? xa + 1 : xb - 1, y, zb, servingOnWest ? Direction.EAST : Direction.WEST, "chests/village/village_plains_house");
		// ghostly inmates at lunch
		p.ghost((tx0 + tx1) / 2, y, za + 2, false, "Hungry Inmate", 4f);
		p.ghost((tx0 + tx1) / 2, y, zb - 2, p.r.nextBoolean(), "Lunch Lady Ghost", 3f);
	}

	/** Warden's office: green carpet, big desk with a lectern, filing cabinets, bookshelves and a safe. */
	private static void office(P p, int x1, int z1, int x2, int z2, int y, Direction deskFaces) {
		int xa = Math.min(x1, x2), xb = Math.max(x1, x2), za = Math.min(z1, z2), zb = Math.max(z1, z2);
		for (int x = xa; x <= xb; x++)
			for (int z = za; z <= zb; z++) {
				p.set(x, y - 1, z, Blocks.DARK_OAK_PLANKS.getDefaultState());
				if (x > xa && x < xb && z > za && z < zb) p.set(x, y, z, Blocks.GREEN_CARPET.getDefaultState());
			}
		// bookshelves along the back wall, filing cabinets (barrels) along the side
		for (int x = xa; x <= xb; x++) {
			p.set(x, y, za, x % 3 == 0 ? Blocks.CHISELED_BOOKSHELF.getDefaultState() : Blocks.BOOKSHELF.getDefaultState());
			p.set(x, y + 1, za, Blocks.BOOKSHELF.getDefaultState());
		}
		for (int z = za + 1; z <= zb - 1; z++) {
			p.set(xb, y, z, Blocks.BARREL.getDefaultState());
			if (z % 2 == 0) p.set(xb, y + 1, z, Blocks.BARREL.getDefaultState());
		}
		// the desk
		int dx = (xa + xb) / 2, dz = za + 2;
		for (int x = dx - 1; x <= dx + 1; x++) p.set(x, y, dz, p.stair(Blocks.DARK_OAK_STAIRS, deskFaces, true));
		p.set(dx - 1, y + 1, dz, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, deskFaces.getOpposite()));
		p.set(dx + 1, y + 1, dz, Blocks.POTTED_WITHER_ROSE.getDefaultState());
		p.set(dx, y + 1, dz, Blocks.CANDLE.getDefaultState());
		p.set(dx, y, dz - 1, p.stair(Blocks.POLISHED_BLACKSTONE_STAIRS, deskFaces, false)); // warden's chair
		p.set(xa + 1, y, zb - 1, Blocks.JUKEBOX.getDefaultState());
		p.set(xa + 1, y + 3, (za + zb) / 2, Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		p.set(xb - 1, y + 3, (za + zb) / 2, Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		// the warden's safe
		p.set(xa + 1, y, za + 1, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.SOUTH));
		if (p.w.getBlockEntity(p.c.add(xa + 1, y, za + 1)) instanceof ChestBlockEntity safe) {
			safe.setStack(4, new ItemStack(ModItems.THERMOS));
			safe.setStack(11, new ItemStack(Items.EMERALD, 4 + p.r.nextInt(8)));
			safe.setStack(13, new ItemStack(Items.GOLD_INGOT, 3 + p.r.nextInt(5)));
			safe.setStack(15, new ItemStack(ModItems.ECTOPLASM, 6 + p.r.nextInt(6)));
			if (p.r.nextBoolean()) safe.setStack(22, new ItemStack(ModItems.GHOST_BLASTER));
		}
		p.ghost(dx, y, dz + 2, true, "Office Clerk", 3f);
	}

	/** Armory: weapon racks (chests), iron storage cage, anvil and grindstone. */
	private static void armory(P p, int x1, int z1, int x2, int z2, int y) {
		int xa = Math.min(x1, x2), xb = Math.max(x1, x2), za = Math.min(z1, z2), zb = Math.max(z1, z2);
		for (int x = xa; x <= xb; x++)
			for (int z = za; z <= zb; z++) p.set(x, y - 1, z, Blocks.POLISHED_ANDESITE.getDefaultState());
		for (int x = xa + 1; x <= xb - 1; x++) {
			p.bars(x, y, zb - 2, Blocks.IRON_BARS.getDefaultState());
			p.bars(x, y + 1, zb - 2, Blocks.IRON_BARS.getDefaultState());
		}
		p.chest(xa + 1, y, zb, Direction.NORTH, "chests/village/village_weaponsmith");
		p.chest(xb - 1, y, zb, Direction.NORTH, "chests/village/village_armorer");
		p.set(xa, y, za + 1, Blocks.ANVIL.getDefaultState());
		p.set(xa, y, za + 2, Blocks.GRINDSTONE.getDefaultState());
		p.set(xa, y, za + 3, Blocks.SMITHING_TABLE.getDefaultState());
		p.set((xa + xb) / 2, y + 3, (za + zb) / 2, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		p.ghost((xa + xb) / 2, y, za + 1, true, "Armory Guard", 3f);
	}

	/** Infirmary: a row of beds, a brewing stand and a medicine cabinet. */
	private static void infirmary(P p, int x1, int z1, int x2, int z2, int y) {
		int xa = Math.min(x1, x2), xb = Math.max(x1, x2), za = Math.min(z1, z2), zb = Math.max(z1, z2);
		for (int x = xa; x <= xb; x++)
			for (int z = za; z <= zb; z++) p.set(x, y - 1, z, Blocks.WHITE_TERRACOTTA.getDefaultState());
		for (int z = za + 1; z <= zb - 1; z += 3) p.bed(xb - 1, y, z, Direction.WEST);
		p.set(xa, y, za, Blocks.BREWING_STAND.getDefaultState());
		p.set(xa, y, za + 1, Blocks.CAULDRON.getDefaultState());
		p.chest(xa, y, za + 2, Direction.EAST, "chests/igloo_chest");
		p.set((xa + xb) / 2, y + 3, (za + zb) / 2, Blocks.SEA_LANTERN.getDefaultState());
		p.ghost(xa + 2, y, (za + zb) / 2, false, "Ghost Nurse", 3f);
	}

	/** Visiting room: booths split by glass, stools on both sides. */
	private static void visiting(P p, int x1, int z1, int x2, int z2, int y) {
		int xa = Math.min(x1, x2), xb = Math.max(x1, x2), za = Math.min(z1, z2), zb = Math.max(z1, z2);
		int mid = (za + zb) / 2;
		for (int x = xa; x <= xb; x++)
			for (int z = za; z <= zb; z++) p.set(x, y - 1, z, Blocks.SMOOTH_STONE.getDefaultState());
		for (int x = xa; x <= xb; x++) {
			p.set(x, y, mid, Blocks.SMOOTH_STONE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.DOUBLE));
			p.bars(x, y + 1, mid, Blocks.GLASS_PANE.getDefaultState());
			p.bars(x, y + 2, mid, Blocks.GLASS_PANE.getDefaultState());
			if (x % 3 == 0) {
				p.set(x, y, mid - 1, Blocks.OAK_FENCE.getDefaultState());
				p.set(x, y, mid + 1, Blocks.OAK_FENCE.getDefaultState());
				p.set(x, y + 1, mid - 1, Blocks.OAK_PRESSURE_PLATE.getDefaultState());
				p.set(x, y + 1, mid + 1, Blocks.OAK_PRESSURE_PLATE.getDefaultState());
			}
			if (x % 3 == 1) {
				p.bars(x, y + 1, mid - 1, Blocks.IRON_BARS.getDefaultState());
				p.bars(x, y + 1, mid + 1, Blocks.IRON_BARS.getDefaultState());
			}
		}
		p.set((xa + xb) / 2, y + 3, mid - 2, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		p.ghost((xa + xb) / 2, y, mid + 2, false, "Visitor", 2f);
	}

	// ---------------------------------------------------------------- buildings

	/** Big House: a two-story admin block in the yard between the cell blocks. */
	public static void adminBlock(ServerWorld w, BlockPos center, Random r) {
		P p = new P(w, center, r);
		int X = 15, Z1 = 8, Z2 = 25, H = 12;
		BlockState wall = Blocks.DEEPSLATE_TILES.getDefaultState();
		BlockState trim = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
		BlockState floor2 = Blocks.POLISHED_DEEPSLATE.getDefaultState();
		// shell
		for (int x = -X; x <= X; x++)
			for (int z = Z1; z <= Z2; z++)
				for (int y = 0; y <= H; y++) {
					boolean edge = x == -X || x == X || z == Z1 || z == Z2;
					BlockState s = y == H ? trim : edge ? ((y == 0 || y == 6) ? trim : wall) : (y == 6 ? floor2 : AIR);
					p.set(x, y, z, s);
				}
		for (int x = -X; x <= X; x++) for (int z = Z1; z <= Z2; z++) p.set(x, -1, z, Blocks.POLISHED_DEEPSLATE.getDefaultState());
		// barred windows on both floors
		for (int x = -X + 2; x <= X - 2; x += 3) {
			for (int y : new int[]{2, 3, 8, 9}) {
				p.bars(x, y, Z1, Blocks.IRON_BARS.getDefaultState());
				p.bars(x, y, Z2, Blocks.IRON_BARS.getDefaultState());
			}
		}
		// central hallway with open doorways at both ends
		for (int z = Z1 + 1; z <= Z2 - 1; z++) {
			p.set(-2, 0, z, wall);
			p.set(2, 0, z, wall);
			for (int y = 1; y <= 5; y++) {
				p.set(-2, y, z, (z % 4 == 0 && y == 3) ? Blocks.SOUL_LANTERN.getDefaultState() : wall);
				p.set(2, y, z, (z % 4 == 0 && y == 3) ? Blocks.SOUL_LANTERN.getDefaultState() : wall);
			}
		}
		for (int y = 0; y <= 3; y++)
			for (int x = -1; x <= 1; x++) {
				p.set(x, y, Z1, AIR);
				p.set(x, y, Z2, AIR);
			}
		for (int z = Z1; z <= Z2; z++) p.set(0, -1, z, Blocks.POLISHED_BLACKSTONE.getDefaultState());
		// doorways off the hallway into each room
		for (int y = 0; y <= 2; y++) {
			p.set(-2, y, 12, AIR);
			p.set(2, y, 12, AIR);
		}
		// wall between the office and the armory (east side)
		for (int x = 3; x <= X - 1; x++) for (int y = 0; y <= 5; y++) p.set(x, y, 17, wall);
		for (int y = 0; y <= 2; y++) p.set(9, y, 17, AIR);
		// ground floor: cafeteria (west), warden's office + armory (east)
		cafeteria(p, -X + 1, Z1 + 1, -3, Z2 - 1, 0, true);
		office(p, 3, Z1 + 1, X - 1, 16, 0, Direction.SOUTH);
		armory(p, 3, 18, X - 1, Z2 - 1, 0);
		// staircase in the hallway up to the second floor
		for (int i = 0; i < 6; i++) {
			int z = Z2 - 2 - i;
			for (int x = -1; x <= 1; x++) {
				p.set(x, i, z, p.stair(Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.NORTH, false));
				for (int y = 0; y < i; y++) p.set(x, y, z, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
				p.set(x, 6, z, AIR);
			}
		}
		// second floor: visiting room (west), infirmary (east)
		for (int z = Z1 + 1; z <= Z2 - 1; z++)
			for (int y = 7; y <= 11; y++) {
				p.set(-2, y, z, wall);
				p.set(2, y, z, wall);
			}
		for (int y = 7; y <= 9; y++) {
			p.set(-2, y, 14, AIR);
			p.set(2, y, 14, AIR);
		}
		visiting(p, -X + 1, Z1 + 1, -3, Z2 - 1, 7);
		infirmary(p, 3, Z1 + 1, X - 1, Z2 - 1, 7);
		for (int z = Z1 + 2; z <= Z2 - 2; z += 4) p.set(0, 11, z, Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
		// sign over the entrance
		for (int x = -3; x <= 3; x++) p.set(x, 5, Z1 - 1, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.getDefaultState());
		p.set(0, 4, Z1 - 1, Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
	}

	/** Spire: a one-story admin wing south of the tower, joined to its entrance. */
	public static void spireWing(ServerWorld w, BlockPos center, Random r, int towerRadius) {
		P p = new P(w, center, r);
		int Z1 = towerRadius + 1, Z2 = towerRadius + 13, X = 10, H = 7;
		BlockState wall = Blocks.DEEPSLATE_BRICKS.getDefaultState();
		BlockState trim = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
		for (int x = -X; x <= X; x++)
			for (int z = Z1; z <= Z2; z++) {
				p.set(x, -1, z, Blocks.POLISHED_DEEPSLATE.getDefaultState());
				p.set(x, -2, z, Blocks.DEEPSLATE.getDefaultState());
				for (int y = 0; y <= H; y++) {
					boolean edge = x == -X || x == X || z == Z2 || (z == Z1 && Math.abs(x) > 1);
					p.set(x, y, z, y == H ? trim : edge ? wall : AIR);
				}
			}
		for (int x = -X + 2; x <= X - 2; x += 3) for (int y = 2; y <= 3; y++) p.bars(x, y, Z2, Blocks.IRON_BARS.getDefaultState());
		// hallway walls
		for (int z = Z1; z <= Z2 - 1; z++)
			for (int y = 0; y <= H - 1; y++) {
				p.set(-2, y, z, (z % 4 == 0 && y == 3) ? Blocks.SOUL_LANTERN.getDefaultState() : wall);
				p.set(2, y, z, (z % 4 == 0 && y == 3) ? Blocks.SOUL_LANTERN.getDefaultState() : wall);
			}
		for (int y = 0; y <= 3; y++) for (int x = -1; x <= 1; x++) p.set(x, y, Z2, AIR);
		for (int y = 0; y <= 2; y++) {
			p.set(-2, y, Z1 + 6, AIR);
			p.set(2, y, Z1 + 6, AIR);
		}
		cafeteria(p, -X + 1, Z1 + 1, -3, Z2 - 1, 0, true);
		office(p, 3, Z1 + 1, X - 1, Z2 - 1, 0, Direction.SOUTH);
	}
}
