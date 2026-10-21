package com.phantompowers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

/** A sofa that joins up with sofas beside it (facing the same way): arms only on the ends of the row. */
public class SofaBlock extends FurnitureBlock {
	public enum Shape implements StringIdentifiable {
		/** On its own: both arms. */
		SINGLE("single"),
		/** End of a row with more sofa to the right: arm on the left only. */
		LEFT("left"),
		/** End of a row with more sofa to the left: arm on the right only. */
		RIGHT("right"),
		/** In the middle of a row: no arms. */
		MIDDLE("middle");

		private final String name;

		Shape(String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}
	}

	public static final EnumProperty<Shape> SHAPE = EnumProperty.of("shape", Shape.class);

	public SofaBlock(Settings settings, double[] box, double seatHeight) {
		super(settings, box, seatHeight);
		setDefaultState(getDefaultState().with(SHAPE, Shape.SINGLE));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, SHAPE);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState s = super.getPlacementState(ctx);
		return s == null ? null : s.with(SHAPE, shapeAt(ctx.getWorld(), ctx.getBlockPos(), s.get(FACING)));
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		if (direction.getAxis().isHorizontal()) return state.with(SHAPE, shapeAt(world, pos, state.get(FACING)));
		return state;
	}

	private boolean sofa(BlockView world, BlockPos p, Direction facing) {
		BlockState s = world.getBlockState(p);
		return s.getBlock() instanceof SofaBlock && s.get(FACING) == facing;
	}

	/** Which arms this sofa keeps, from the sofas beside it (left and right as you'd see them sitting in it). */
	public Shape shapeAt(BlockView world, BlockPos pos, Direction facing) {
		boolean left = sofa(world, pos.offset(facing.rotateYCounterclockwise()), facing);
		boolean right = sofa(world, pos.offset(facing.rotateYClockwise()), facing);
		return left && right ? Shape.MIDDLE : left ? Shape.RIGHT : right ? Shape.LEFT : Shape.SINGLE;
	}
}
