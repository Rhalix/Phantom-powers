package com.phantompowers;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/** Two-block-tall furniture you can open and store things in (fridge, locker). */
public class TallStorageBlock extends HorizontalFacingBlock implements BlockEntityProvider {
	public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
	private final VoxelShape[] shapes = new VoxelShape[4];
	private final SoundEvent openSound;
	private final MapCodec<TallStorageBlock> codec;

	/** box = {x0, z0, x1, z1} in pixels for the block facing north. */
	public TallStorageBlock(Settings settings, double[] box, SoundEvent openSound) {
		super(settings);
		double x0 = box[0], z0 = box[1], x1 = box[2], z1 = box[3];
		shapes[Direction.NORTH.getHorizontal()] = Block.createCuboidShape(x0, 0, z0, x1, 16, z1);
		shapes[Direction.EAST.getHorizontal()] = Block.createCuboidShape(16 - z1, 0, x0, 16 - z0, 16, x1);
		shapes[Direction.SOUTH.getHorizontal()] = Block.createCuboidShape(16 - x1, 0, 16 - z1, 16 - x0, 16, 16 - z0);
		shapes[Direction.WEST.getHorizontal()] = Block.createCuboidShape(z0, 0, 16 - x1, z1, 16, 16 - x0);
		this.openSound = openSound;
		this.codec = createCodec(s -> new TallStorageBlock(s, box, openSound));
		setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(HALF, DoubleBlockHalf.LOWER));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return codec;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapes[state.get(FACING).getHorizontal()];
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockPos pos = ctx.getBlockPos();
		World w = ctx.getWorld();
		if (pos.getY() >= w.getTopY() - 1 || !w.getBlockState(pos.up()).canReplace(ctx)) return null;
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite()).with(HALF, DoubleBlockHalf.LOWER);
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		if (state.get(HALF) == DoubleBlockHalf.LOWER) return true;
		BlockState below = world.getBlockState(pos.down());
		return below.isOf(this) && below.get(HALF) == DoubleBlockHalf.LOWER;
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		DoubleBlockHalf half = state.get(HALF);
		if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
			// the other half went missing: this half goes too
			if (!neighborState.isOf(this) || neighborState.get(HALF) == half) return Blocks.AIR.getDefaultState();
		}
		return state;
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		// breaking the top in creative shouldn't drop the bottom
		if (!world.isClient && player.isCreative() && state.get(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos below = pos.down();
			BlockState b = world.getBlockState(below);
			if (b.isOf(this) && b.get(HALF) == DoubleBlockHalf.LOWER) {
				world.setBlockState(below, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
				world.syncWorldEvent(player, 2001, below, Block.getRawIdFromState(b));
			}
		}
		return super.onBreak(world, pos, state, player);
	}

	private static BlockPos bottom(BlockState state, BlockPos pos) {
		return state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) return ActionResult.SUCCESS;
		BlockPos at = bottom(state, pos);
		if (world.getBlockEntity(at) instanceof FurnitureStorageBlockEntity be) {
			player.openHandledScreen(be);
			world.playSound(null, at, openSound, SoundCategory.BLOCKS, 0.7f, 1.0f);
		}
		return ActionResult.CONSUME;
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof FurnitureStorageBlockEntity be) {
			ItemScatterer.spawn(world, pos, be.inventory);
			world.updateComparators(pos, this);
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return state.get(HALF) == DoubleBlockHalf.LOWER ? new FurnitureStorageBlockEntity(pos, state) : null;
	}
}
