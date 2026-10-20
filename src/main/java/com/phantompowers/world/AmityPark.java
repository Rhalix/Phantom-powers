package com.phantompowers.world;

import com.phantompowers.ModBlocks;
import com.phantompowers.ModItems;
import com.phantompowers.PortalLogic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;

/**
 * Amity Park: a little ghost-haunted town built once in the Overworld, a few hundred blocks from spawn. Streets,
 * the Ghost Lab house (a family home with a ghost-hunting lab and an unlit Ghost Portal in the basement), the
 * high school, a burger diner, the town hall, a park with a fountain, and a row of houses. Story mode starts here.
 */
public final class AmityPark {
	public static final int R = 58;

	/** Remembers where the town was built. */
	public static final class State extends PersistentState {
		public static final PersistentState.Type<State> TYPE = new PersistentState.Type<>(State::new, State::fromNbt, null);
		public boolean built;
		public BlockPos center = BlockPos.ORIGIN;

		static State fromNbt(NbtCompound n, RegistryWrapper.WrapperLookup lookup) {
			State s = new State();
			s.built = n.getBoolean("built");
			s.center = BlockPos.fromLong(n.getLong("center"));
			return s;
		}

		@Override
		public NbtCompound writeNbt(NbtCompound n, RegistryWrapper.WrapperLookup lookup) {
			n.putBoolean("built", built);
			n.putLong("center", center.asLong());
			return n;
		}
	}

	private AmityPark() {}

	public static State state(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(State.TYPE, "phantom_amity_park");
	}

	/** Build the town if it isn't there yet; returns its centre (ground level). */
	public static BlockPos ensure(MinecraftServer server) {
		State st = state(server);
		if (st.built) return st.center;
		ServerWorld w = server.getOverworld();
		BlockPos spawn = w.getSpawnPos();
		int cx = spawn.getX() + 320, cz = spawn.getZ() + 320;
		w.getChunk(new BlockPos(cx, 64, cz));
		int g = Math.max(w.getSeaLevel() + 1, Math.min(140, w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, cx, cz)));
		BlockPos c = new BlockPos(cx, g, cz);
		new Builder(w, c).build();
		st.built = true;
		st.center = c;
		st.markDirty();
		return c;
	}

	/** Where you arrive: the welcome sign at the west end of Main Street. */
	public static Vec3d entrance(BlockPos c) {
		return new Vec3d(c.getX() - R + 4.5, c.getY(), c.getZ() + 0.5);
	}

	/** The basement lab under the Ghost Lab house (the middle of the floor). */
	public static BlockPos lab(BlockPos c) {
		return c.add(Builder.LAB_X + 7, -8, Builder.LAB_Z + 8);
	}

	/** Centre of the lab's Ghost Portal opening. */
	public static Vec3d labPortal(BlockPos c) {
		return Vec3d.ofBottomCenter(c.add(Builder.LAB_X + 6, -8 + 3, Builder.LAB_Z + 2)).add(0.5, 0, 0);
	}

	/** Just inside the Ghost Lab house's front door. */
	public static Vec3d labHouseDoor(BlockPos c) {
		return new Vec3d(c.getX() + Builder.LAB_X + 8.5, c.getY(), c.getZ() + Builder.LAB_Z + 18.5);
	}

	static final class Builder {
		static final int LAB_X = 12, LAB_Z = -32;   // north-west corner of the Ghost Lab house (16 x 16)
		final ServerWorld w;
		final BlockPos c;
		final int g;
		final net.minecraft.util.math.random.Random rnd;

		Builder(ServerWorld w, BlockPos c) {
			this.w = w;
			this.c = c;
			this.g = c.getY();
			this.rnd = net.minecraft.util.math.random.Random.create(c.asLong());
		}

		void set(int x, int y, int z, BlockState s) {
			w.setBlockState(c.add(x, y - g, z), s, Block.NOTIFY_LISTENERS);
		}

		BlockState get(int x, int y, int z) {
			return w.getBlockState(c.add(x, y - g, z));
		}

		void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
			for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
				for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
					for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
		}

		/** Walls of a box (no top or bottom). */
		void walls(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
			fill(x0, y0, z0, x1, y1, z0, s);
			fill(x0, y0, z1, x1, y1, z1, s);
			fill(x0, y0, z0, x0, y1, z1, s);
			fill(x1, y0, z0, x1, y1, z1, s);
		}

		void door(int x, int y, int z, Direction facing, Block door) {
			set(x, y, z, door.getDefaultState().with(DoorBlock.FACING, facing).with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
			set(x, y + 1, z, door.getDefaultState().with(DoorBlock.FACING, facing).with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		}

		void sign(int x, int y, int z, Direction facing, boolean wall, String... lines) {
			if (wall) set(x, y, z, Blocks.OAK_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, facing));
			else set(x, y, z, Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, facing.getHorizontal() * 4));
			if (w.getBlockEntity(c.add(x, y - g, z)) instanceof SignBlockEntity sb) {
				SignText t = new SignText();
				for (int i = 0; i < Math.min(4, lines.length); i++) t = t.withMessage(i, Text.literal(lines[i]));
				t = t.withColor(DyeColor.BLACK).withGlowing(true);
				sb.setText(t, true);
				sb.markDirty();
			}
		}

		void build() {
			int top = g + 26;
			// ---- level the ground and clear the sky
			for (int x = -R - 2; x <= R + 2; x++) {
				for (int z = -R - 2; z <= R + 2; z++) {
					for (int y = g; y <= top; y++) if (!get(x, y, z).isAir()) set(x, y, z, Blocks.AIR.getDefaultState());
					set(x, g - 1, z, Blocks.GRASS_BLOCK.getDefaultState());
					for (int y = g - 2; y >= g - 12; y--) {
						BlockState s = get(x, y, z);
						if (!s.isAir() && s.getFluidState().isEmpty() && s.isOpaque()) break;
						set(x, y, z, y >= g - 4 ? Blocks.DIRT.getDefaultState() : Blocks.STONE.getDefaultState());
					}
				}
			}
			roads();
			labHouse();
			school(-54, -52);
			diner(-30, 10);
			townHall(-56, 12);
			park(10, 10);
			for (int i = 0; i < 3; i++) house(-48 + i * 16, 36, i);
			house(34, -52, 3);
			house(34, -26, 4);
			welcome();
			lamps();
		}

		void roads() {
			BlockState asphalt = Blocks.BLACK_CONCRETE.getDefaultState(), line = Blocks.YELLOW_CONCRETE.getDefaultState(),
					walk = Blocks.SMOOTH_STONE.getDefaultState(), curb = Blocks.STONE_BRICKS.getDefaultState();
			for (int a = -R; a <= R; a++) {
				for (int b = -3; b <= 3; b++) {
					set(a, g - 1, b, asphalt);
					set(b, g - 1, a, asphalt);
				}
				if (Math.floorMod(a, 4) < 2 && Math.abs(a) > 3) {
					set(a, g - 1, 0, line);
					set(0, g - 1, a, line);
				}
				for (int b : new int[]{-5, -4, 4, 5}) {
					if (Math.abs(a) > 3) {
						set(a, g - 1, b, b == -4 || b == 4 ? curb : walk);
						set(b, g - 1, a, b == -4 || b == 4 ? curb : walk);
					}
				}
			}
			// crosswalk stripes
			for (int b = -3; b <= 3; b++) for (int a : new int[]{-5, 5}) {
				if ((b & 1) == 0) {
					set(a, g - 1, b, Blocks.WHITE_CONCRETE.getDefaultState());
					set(b, g - 1, a, Blocks.WHITE_CONCRETE.getDefaultState());
				}
			}
		}

		void lamps() {
			for (int a = -R + 6; a <= R - 6; a += 12) {
				for (int[] p : new int[][]{{a, -6}, {a, 6}, {-6, a}, {6, a}}) {
					if (Math.abs(p[0]) <= 6 && Math.abs(p[1]) <= 6) continue;
					fill(p[0], g, p[1], p[0], g + 3, p[1], Blocks.DARK_OAK_FENCE.getDefaultState());
					set(p[0], g + 4, p[1], Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, false));
				}
			}
		}

		void welcome() {
			int x = -R + 3;
			fill(x, g, -8, x, g + 2, -8, Blocks.DARK_OAK_LOG.getDefaultState());
			fill(x, g, -12, x, g + 2, -12, Blocks.DARK_OAK_LOG.getDefaultState());
			fill(x, g + 3, -12, x, g + 4, -8, Blocks.SPRUCE_PLANKS.getDefaultState());
			sign(x + 1, g + 4, -10, Direction.EAST, true, "Welcome to", "AMITY PARK", "a nice quiet town", "(mostly)");
			sign(x + 1, g + 3, -10, Direction.EAST, true, "Pop. 22,000", "and some ghosts", "", "/phantom story");
			for (int z = -11; z <= -9; z++) set(x + 1, g, z, Blocks.LIME_STAINED_GLASS.getDefaultState());
		}

		/** The Ghost Lab house: a white family home with a rooftop sensor tower and a ghost lab in the basement. */
		void labHouse() {
			int x0 = LAB_X, z0 = LAB_Z, x1 = x0 + 15, z1 = z0 + 15;
			BlockState wall = Blocks.WHITE_CONCRETE.getDefaultState(), trim = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(),
					floor = Blocks.SPRUCE_PLANKS.getDefaultState(), glass = Blocks.GLASS_PANE.getDefaultState();
			// front path to the street
			for (int z = z1 + 1; z <= -5; z++) fill(x0 + 7, g - 1, z, x0 + 9, g - 1, z, Blocks.SMOOTH_STONE.getDefaultState());
			// basement lab
			int lf = g - 9;
			fill(x0, lf, z0, x1, g - 1, z1, Blocks.POLISHED_DEEPSLATE.getDefaultState());
			fill(x0 + 1, lf + 1, z0 + 1, x1 - 1, g - 2, z1 - 1, Blocks.AIR.getDefaultState());
			fill(x0 + 1, lf, z0 + 1, x1 - 1, lf, z1 - 1, Blocks.SMOOTH_STONE.getDefaultState());
			for (int x = x0 + 1; x < x1; x++) set(x, lf, z0 + 8, Blocks.LIME_CONCRETE.getDefaultState());   // safety line
			// the Ghost Portal (unlit) on the north wall
			java.util.Set<BlockPos> inside = PortalLogic.buildLabPortal(w, c.add(x0 + 4, lf + 1 - g, z0 + 2), Direction.Axis.X, Block.NOTIFY_LISTENERS, false);
			BlockPos button = inside.stream().min((a, b) -> a.getY() != b.getY() ? a.getY() - b.getY() : a.getX() - b.getX()).orElse(null);
			if (button != null) w.setBlockState(button, Blocks.STONE_BUTTON.getDefaultState().with(ButtonBlock.FACE, BlockFace.FLOOR)
					.with(ButtonBlock.FACING, Direction.SOUTH), Block.NOTIFY_LISTENERS);
			sign(x0 + 3, lf + 3, z0 + 3, Direction.SOUTH, false, "GHOST PORTAL", "Step inside and", "press the switch", "(don't!)");
			// lab equipment
			set(x0 + 2, lf + 1, z1 - 2, ModBlocks.HUNTER_WORKBENCH.getDefaultState());
			set(x1 - 2, lf + 1, z1 - 2, ModBlocks.PROTOTYPE_PORTAL.getDefaultState());
			set(x1 - 2, lf + 1, z0 + 10, ModBlocks.CLONING_POD.getDefaultState());
			fill(x0 + 1, lf + 1, z0 + 9, x0 + 1, lf + 2, z0 + 12, Blocks.IRON_BLOCK.getDefaultState());
			fill(x0 + 1, lf + 3, z0 + 9, x0 + 1, lf + 3, z0 + 12, Blocks.REDSTONE_LAMP.getDefaultState());
			set(x0 + 1, lf + 2, z0 + 10, Blocks.LIME_STAINED_GLASS.getDefaultState());
			set(x0 + 1, lf + 2, z0 + 11, Blocks.LIME_STAINED_GLASS.getDefaultState());
			fill(x1 - 1, lf + 1, z0 + 3, x1 - 1, lf + 1, z0 + 6, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
			set(x1 - 1, lf + 2, z0 + 4, Blocks.BREWING_STAND.getDefaultState());
			set(x1 - 1, lf + 2, z0 + 5, Blocks.TARGET.getDefaultState());
			lockers(x0 + 6, lf + 1, z1 - 1);
			sign(x0 + 7, lf + 2, z1 - 2, Direction.NORTH, true, "GEAR LOCKER", "Blaster, sword,", "ectoplasm...", "Ghost hunters only");
			// stairs down (east side)
			for (int i = 0; i < 8; i++) {
				fill(x1 - 3, g - 1 - i, z1 - 2 - i, x1 - 2, g + 2 - i, z1 - 2 - i, Blocks.AIR.getDefaultState());
				fill(x1 - 3, g - 2 - i, z1 - 2 - i, x1 - 2, g - 2 - i, z1 - 2 - i,
						Blocks.STONE_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
			}
			// the house: two floors
			fill(x0, g, z0, x1, g + 9, z1, Blocks.AIR.getDefaultState());
			fill(x0, g - 1, z0, x1, g - 1, z1, floor);
			fill(x1 - 3, g - 1, z1 - 2, x1 - 2, g - 1, z1 - 2, Blocks.AIR.getDefaultState());  // stairwell
			fill(x1 - 3, g - 1, z1 - 4, x1 - 2, g - 1, z1 - 3, Blocks.AIR.getDefaultState());
			// glowing floor tiles light the lab below
			for (int x = x0 + 2; x < x1 - 4; x += 3) for (int z = z0 + 2; z < z1 - 1; z += 3) set(x, g - 1, z, Blocks.SEA_LANTERN.getDefaultState());
			walls(x0, g, z0, x1, g + 8, z1, wall);
			fill(x0, g + 4, z0, x1, g + 4, z1, trim);
			fill(x0 + 1, g + 4, z0 + 1, x1 - 1, g + 4, z1 - 1, floor);
			fill(x0, g + 9, z0, x1, g + 9, z1, trim);
			for (int[] cn : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) fill(cn[0], g, cn[1], cn[0], g + 9, cn[1], trim);
			for (int lvl : new int[]{g + 1, g + 6}) {
				for (int a = 2; a <= 13; a += 4) {
					fill(x0 + a, lvl, z1, x0 + a + 1, lvl + 1, z1, glass);
					fill(x0 + a, lvl, z0, x0 + a + 1, lvl + 1, z0, glass);
					fill(x0, lvl, z0 + a, x0, lvl + 1, z0 + a + 1, glass);
					fill(x1, lvl, z0 + a, x1, lvl + 1, z0 + a + 1, glass);
				}
			}
			fill(x0 + 7, g, z1, x0 + 8, g + 2, z1, Blocks.AIR.getDefaultState());
			door(x0 + 7, g, z1, Direction.SOUTH, Blocks.IRON_DOOR);
			door(x0 + 8, g, z1, Direction.SOUTH, Blocks.IRON_DOOR);
			fill(x0 + 7, g, z1 + 1, x0 + 8, g, z1 + 1, Blocks.STONE_PRESSURE_PLATE.getDefaultState());
			fill(x0 + 7, g, z1 - 1, x0 + 8, g, z1 - 1, Blocks.STONE_PRESSURE_PLATE.getDefaultState());
			// big glowing sign over the door
			fill(x0 + 4, g + 3, z1 + 1, x0 + 11, g + 3, z1 + 1, Blocks.BLACK_CONCRETE.getDefaultState());
			fill(x0 + 5, g + 3, z1 + 2, x0 + 10, g + 3, z1 + 2, Blocks.LIME_STAINED_GLASS.getDefaultState());
			sign(x0 + 7, g + 2, z1 + 1, Direction.SOUTH, true, "", "GHOST LAB", "Ghost research", "& hunting");
			sign(x0 + 8, g + 2, z1 + 1, Direction.SOUTH, true, "", "Experiments", "in progress!", "");
			// furniture: kitchen, couch, beds upstairs, stairs up
			fill(x0 + 1, g, z0 + 1, x0 + 5, g, z0 + 1, Blocks.SMOOTH_QUARTZ.getDefaultState());
			set(x0 + 2, g, z0 + 1, Blocks.FURNACE.getDefaultState());
			set(x0 + 4, g, z0 + 1, Blocks.CAULDRON.getDefaultState());
			fill(x0 + 2, g, z0 + 8, x0 + 4, g, z0 + 8, Blocks.RED_WOOL.getDefaultState());
			set(x0 + 3, g, z0 + 6, Blocks.JUKEBOX.getDefaultState());
			for (int i = 0; i < 4; i++) {
				set(x0 + 1, g + i, z0 + 10 - i, Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
				set(x0 + 1, g + 4, z0 + 10 - i, Blocks.AIR.getDefaultState());
			}
			fill(x0 + 2, g + 5, z0 + 2, x0 + 3, g + 5, z0 + 4, Blocks.CYAN_WOOL.getDefaultState());
			fill(x0 + 12, g + 5, z0 + 2, x0 + 13, g + 5, z0 + 4, Blocks.ORANGE_WOOL.getDefaultState());
			set(x0 + 8, g + 5, z0 + 2, Blocks.BOOKSHELF.getDefaultState());
			set(x0 + 9, g + 5, z0 + 2, Blocks.CRAFTING_TABLE.getDefaultState());
			// rooftop sensor tower
			fill(x0 + 5, g + 10, z0 + 5, x0 + 10, g + 10, z0 + 10, Blocks.IRON_BLOCK.getDefaultState());
			fill(x0 + 6, g + 11, z0 + 6, x0 + 9, g + 13, z0 + 9, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
			fill(x0 + 6, g + 12, z0 + 6, x0 + 9, g + 12, z0 + 9, Blocks.LIME_STAINED_GLASS.getDefaultState());
			fill(x0 + 7, g + 12, z0 + 7, x0 + 8, g + 12, z0 + 8, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
			fill(x0 + 5, g + 14, z0 + 5, x0 + 10, g + 14, z0 + 10, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
			fill(x0 + 7, g + 15, z0 + 7, x0 + 7, g + 18, z0 + 7, Blocks.IRON_BARS.getDefaultState());
			set(x0 + 7, g + 19, z0 + 7, Blocks.LIGHTNING_ROD.getDefaultState());
			fill(x0 + 9, g + 15, z0 + 8, x0 + 10, g + 15, z0 + 9, Blocks.SMOOTH_QUARTZ.getDefaultState());   // dish
			set(x0 + 9, g + 16, z0 + 8, Blocks.END_ROD.getDefaultState());
			set(x0 + 6, g + 15, z0 + 9, Blocks.SEA_LANTERN.getDefaultState());
		}

		void lockers(int x, int y, int z) {
			set(x, y, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.NORTH));
			set(x + 3, y, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.NORTH));
			if (w.getBlockEntity(c.add(x, y - g, z)) instanceof ChestBlockEntity ch) {
				ch.setStack(4, new ItemStack(ModItems.ECTOPLASM, 2));
				ch.setStack(11, new ItemStack(ModItems.GHOST_BLASTER));
				ch.setStack(13, new ItemStack(ModItems.ECTO_SWORD));
				ch.setStack(15, new ItemStack(ModItems.THERMOS));
				ch.setStack(22, new ItemStack(ModItems.ECTOPLASM, 16));
			}
			if (w.getBlockEntity(c.add(x + 3, y - g, z)) instanceof ChestBlockEntity ch) {
				ch.setStack(10, new ItemStack(ModItems.DNA_SAMPLER));
				ch.setStack(12, new ItemStack(ModItems.GHOST_NET, 4));
				ch.setStack(14, new ItemStack(ModItems.SHIELD_REMOTE));
				ch.setStack(16, new ItemStack(ModBlocks.SHIELD_EMITTER_ITEM, 4));
				ch.setStack(22, new ItemStack(ModItems.ACNE_CREAM, 2));
			}
		}

		/** A plain building box with a flat roof, windows all round and a door facing the street. */
		void building(int x0, int z0, int w2, int d2, int h, BlockState wall, BlockState roof, BlockState trim, Direction front) {
			int x1 = x0 + w2 - 1, z1 = z0 + d2 - 1;
			fill(x0, g - 1, z0, x1, g - 1, z1, Blocks.SMOOTH_STONE.getDefaultState());
			walls(x0, g, z0, x1, g + h - 1, z1, wall);
			fill(x0, g + h, z0, x1, g + h, z1, roof);
			walls(x0, g + h, z0, x1, g + h + 1, z1, trim);
			fill(x0 + 1, g + h + 1, z0 + 1, x1 - 1, g + h + 1, z1 - 1, Blocks.AIR.getDefaultState());
			for (int y = g + 1; y < g + h - 1; y += 4) {
				for (int x = x0 + 2; x < x1 - 1; x += 3) {
					set(x, y, z0, Blocks.GLASS_PANE.getDefaultState());
					set(x, y + 1, z0, Blocks.GLASS_PANE.getDefaultState());
					set(x, y, z1, Blocks.GLASS_PANE.getDefaultState());
					set(x, y + 1, z1, Blocks.GLASS_PANE.getDefaultState());
				}
				for (int z = z0 + 2; z < z1 - 1; z += 3) {
					set(x0, y, z, Blocks.GLASS_PANE.getDefaultState());
					set(x0, y + 1, z, Blocks.GLASS_PANE.getDefaultState());
					set(x1, y, z, Blocks.GLASS_PANE.getDefaultState());
					set(x1, y + 1, z, Blocks.GLASS_PANE.getDefaultState());
				}
			}
			for (int y = g + 4; y < g + h; y += 4) fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, Blocks.SPRUCE_PLANKS.getDefaultState());
			int dx = (x0 + x1) / 2, dz = front == Direction.SOUTH ? z1 : z0;
			if (front == Direction.EAST || front == Direction.WEST) {
				dx = front == Direction.EAST ? x1 : x0;
				dz = (z0 + z1) / 2;
			}
			door(dx, g, dz, front, Blocks.OAK_DOOR);
			for (int x = x0 + 1; x < x1; x += 5) for (int z = z0 + 1; z < z1; z += 5) set(x, g + 3, z, Blocks.GLOWSTONE.getDefaultState());
		}

		void school(int x0, int z0) {
			building(x0, z0, 36, 22, 9, Blocks.BRICKS.getDefaultState(), Blocks.STONE_BRICKS.getDefaultState(), Blocks.STONE_BRICK_WALL.getDefaultState(), Direction.SOUTH);
			int x1 = x0 + 35, z1 = z0 + 21;
			// entrance columns and steps
			for (int x = x0 + 13; x <= x0 + 22; x += 3) fill(x, g, z1 + 2, x, g + 6, z1 + 2, Blocks.QUARTZ_PILLAR.getDefaultState());
			fill(x0 + 12, g + 7, z1 + 1, x0 + 23, g + 7, z1 + 3, Blocks.SMOOTH_QUARTZ.getDefaultState());
			fill(x0 + 16, g, z1, x0 + 19, g + 2, z1, Blocks.AIR.getDefaultState());
			door(x0 + 17, g, z1, Direction.SOUTH, Blocks.OAK_DOOR);
			door(x0 + 18, g, z1, Direction.SOUTH, Blocks.OAK_DOOR);
			sign(x0 + 17, g + 6, z1 + 4, Direction.SOUTH, true, "", "AMITY HIGH", "Home of the", "Ghost Hunters!");
			// sports field behind
			fill(x0 + 2, g - 1, z1 + 6, x1 - 2, g - 1, z1 + 12, Blocks.GREEN_CONCRETE.getDefaultState());
			fill(x0 + 2, g, z1 + 9, x0 + 2, g + 3, z1 + 9, Blocks.WHITE_CONCRETE.getDefaultState());
			fill(x1 - 2, g, z1 + 9, x1 - 2, g + 3, z1 + 9, Blocks.WHITE_CONCRETE.getDefaultState());
			// desks inside
			for (int x = x0 + 3; x < x1 - 2; x += 3) for (int z = z0 + 3; z < z0 + 9; z += 3) set(x, g, z, Blocks.OAK_PRESSURE_PLATE.getDefaultState());
			fill(x0 + 3, g, z0 + 1, x0 + 10, g + 2, z0 + 1, Blocks.GREEN_TERRACOTTA.getDefaultState());   // blackboard
		}

		void diner(int x0, int z0) {
			building(x0, z0, 14, 12, 5, Blocks.RED_CONCRETE.getDefaultState(), Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.YELLOW_CONCRETE.getDefaultState(), Direction.NORTH);
			fill(x0 + 2, g, z0 + 6, x0 + 11, g, z0 + 6, Blocks.SMOOTH_QUARTZ.getDefaultState());   // counter
			for (int x = x0 + 2; x <= x0 + 11; x += 3) set(x, g, z0 + 3, Blocks.RED_WOOL.getDefaultState());
			set(x0 + 4, g + 1, z0 + 6, Blocks.CAKE.getDefaultState());
			set(x0 + 10, g, z0 + 9, Blocks.SMOKER.getDefaultState());
			// giant burger on the roof
			fill(x0 + 5, g + 6, z0 + 4, x0 + 8, g + 6, z0 + 7, Blocks.ORANGE_TERRACOTTA.getDefaultState());
			fill(x0 + 5, g + 7, z0 + 4, x0 + 8, g + 7, z0 + 7, Blocks.BROWN_CONCRETE.getDefaultState());
			fill(x0 + 5, g + 8, z0 + 4, x0 + 8, g + 8, z0 + 7, Blocks.LIME_CONCRETE.getDefaultState());
			fill(x0 + 5, g + 9, z0 + 4, x0 + 8, g + 9, z0 + 7, Blocks.ORANGE_TERRACOTTA.getDefaultState());
			sign(x0 + 7, g + 3, z0 - 1, Direction.NORTH, true, "", "BURGER BOO", "Open late.", "Very late.");
		}

		void townHall(int x0, int z0) {
			building(x0, z0, 20, 18, 10, Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.SMOOTH_QUARTZ.getDefaultState(), Blocks.QUARTZ_BLOCK.getDefaultState(), Direction.NORTH);
			for (int x = x0 + 2; x <= x0 + 17; x += 3) fill(x, g, z0 - 2, x, g + 7, z0 - 2, Blocks.QUARTZ_PILLAR.getDefaultState());
			fill(x0 + 1, g + 8, z0 - 3, x0 + 18, g + 8, z0 - 1, Blocks.SMOOTH_QUARTZ.getDefaultState());
			fill(x0 + 7, g + 11, z0 + 7, x0 + 12, g + 15, z0 + 12, Blocks.SMOOTH_QUARTZ.getDefaultState());   // clock tower
			set(x0 + 9, g + 14, z0 + 6, Blocks.WHITE_STAINED_GLASS.getDefaultState());
			set(x0 + 10, g + 14, z0 + 6, Blocks.WHITE_STAINED_GLASS.getDefaultState());
			fill(x0 + 9, g + 16, z0 + 9, x0 + 10, g + 18, z0 + 10, Blocks.OXIDIZED_COPPER.getDefaultState());
			sign(x0 + 9, g + 4, z0 - 1, Direction.NORTH, true, "", "TOWN HALL", "Mayor's office", "Ghosts: no entry");
		}

		void park(int x0, int z0) {
			int x1 = x0 + 44, z1 = z0 + 44;
			for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
				boolean path = Math.abs(x - (x0 + x1) / 2) <= 1 || Math.abs(z - (z0 + z1) / 2) <= 1;
				set(x, g - 1, z, path ? Blocks.GRAVEL.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
				if (!path && rnd.nextInt(14) == 0) set(x, g, z, rnd.nextBoolean() ? Blocks.POPPY.getDefaultState() : Blocks.DANDELION.getDefaultState());
				else if (!path && rnd.nextInt(6) == 0) set(x, g, z, Blocks.SHORT_GRASS.getDefaultState());
			}
			// fountain
			int fx = (x0 + x1) / 2, fz = (z0 + z1) / 2;
			fill(fx - 4, g - 1, fz - 4, fx + 4, g, fz + 4, Blocks.STONE_BRICKS.getDefaultState());
			fill(fx - 3, g, fz - 3, fx + 3, g, fz + 3, Blocks.WATER.getDefaultState());
			fill(fx, g, fz, fx, g + 3, fz, Blocks.QUARTZ_PILLAR.getDefaultState());
			set(fx, g + 4, fz, Blocks.SEA_LANTERN.getDefaultState());
			// trees
			for (int i = 0; i < 12; i++) {
				int tx = x0 + 3 + rnd.nextInt(39), tz = z0 + 3 + rnd.nextInt(39);
				if (Math.abs(tx - fx) < 7 && Math.abs(tz - fz) < 7) continue;
				if (Math.abs(tx - fx) <= 2 || Math.abs(tz - fz) <= 2) continue;
				tree(tx, tz);
			}
			// benches along the paths
			for (int a = -16; a <= 16; a += 8) {
				if (Math.abs(a) < 6) continue;
				set(fx + a, g, fz + 3, Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
				set(fx + 3, g, fz + a, Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
			}
			sign(x0 + 1, g, z0 + 1, Direction.NORTH, false, "AMITY PARK", "Town Park", "Please don't feed", "the ghosts");
		}

		void tree(int x, int z) {
			int h = 4 + rnd.nextInt(3);
			for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = h - 2; dy <= h + 1; dy++) {
				if (Math.abs(dx) + Math.abs(dz) + Math.max(0, dy - h) * 2 <= 3) set(x + dx, g + dy, z + dz, Blocks.OAK_LEAVES.getDefaultState()
						.with(net.minecraft.block.LeavesBlock.PERSISTENT, true));
			}
			fill(x, g, z, x, g + h, z, Blocks.OAK_LOG.getDefaultState());
		}

		/** A suburban house with a sloped roof and a front yard. */
		void house(int x0, int z0, int style) {
			BlockState[] walls = {Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState(), Blocks.WHITE_TERRACOTTA.getDefaultState(),
					Blocks.YELLOW_TERRACOTTA.getDefaultState(), Blocks.BIRCH_PLANKS.getDefaultState(), Blocks.PINK_TERRACOTTA.getDefaultState()};
			BlockState wall = walls[style % walls.length];
			int x1 = x0 + 10, z1 = z0 + 9;
			fill(x0, g - 1, z0, x1, g - 1, z1, Blocks.OAK_PLANKS.getDefaultState());
			walls(x0, g, z0, x1, g + 3, z1, wall);
			for (int[] cn : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) fill(cn[0], g, cn[1], cn[0], g + 3, cn[1], Blocks.STRIPPED_OAK_LOG.getDefaultState());
			for (int x = x0 + 2; x < x1 - 1; x += 3) {
				set(x, g + 1, z0, Blocks.GLASS_PANE.getDefaultState());
				set(x, g + 1, z1, Blocks.GLASS_PANE.getDefaultState());
			}
			boolean north = z0 > 0;
			int dz = north ? z0 : z1;
			door((x0 + x1) / 2, g, dz, north ? Direction.NORTH : Direction.SOUTH, Blocks.OAK_DOOR);
			// gabled roof
			for (int i = 0; i <= 5; i++) {
				for (int x = x0 - 1; x <= x1 + 1; x++) {
					set(x, g + 4 + i, z0 - 1 + i, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
					set(x, g + 4 + i, z1 + 1 - i, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
				}
				if (z0 - 1 + i >= z1 + 1 - i) break;
				fill(x0, g + 4 + i, z0 + i, x0, g + 4 + i, z1 - i, wall);
				fill(x1, g + 4 + i, z0 + i, x1, g + 4 + i, z1 - i, wall);
			}
			fill(x0 + 1, g, z0 + 2, x0 + 2, g, z0 + 4, Blocks.RED_WOOL.getDefaultState());
			set(x1 - 2, g, z0 + 2, Blocks.CRAFTING_TABLE.getDefaultState());
			set(x0 + 5, g + 3, z0 + 4, Blocks.GLOWSTONE.getDefaultState());
			// little fence out front
			int fz = north ? z0 - 3 : z1 + 3;
			for (int x = x0; x <= x1; x++) if (x != (x0 + x1) / 2) set(x, g, fz, Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState());
		}
	}
}
