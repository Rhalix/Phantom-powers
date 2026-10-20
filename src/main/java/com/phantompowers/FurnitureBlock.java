package com.phantompowers;

import com.mojang.serialization.MapCodec;
import com.phantompowers.entity.SeatEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Furniture: chairs, stools, sofas, tables, lamps, TVs, kitchen counters, fridges, desks, lockers, lab consoles.
 * Faces the way you place it; chairs, stools and sofas can be sat on (right-click with an empty hand).
 */
public class FurnitureBlock extends HorizontalFacingBlock {
	private final VoxelShape[] shapes = new VoxelShape[4];
	private final double seatHeight;   // < 0 = can't sit
	private final MapCodec<FurnitureBlock> codec;

	/** box = {x0, y0, z0, x1, y1, z1} in pixels, for the model facing north; it's turned to match the other facings. */
	public FurnitureBlock(Settings settings, double[] box, double seatHeight) {
		super(settings);
		double x0 = box[0], y0 = box[1], z0 = box[2], x1 = box[3], y1 = box[4], z1 = box[5];
		shapes[Direction.NORTH.getHorizontal()] = Block.createCuboidShape(x0, y0, z0, x1, y1, z1);
		shapes[Direction.EAST.getHorizontal()] = Block.createCuboidShape(16 - z1, y0, x0, 16 - z0, y1, x1);
		shapes[Direction.SOUTH.getHorizontal()] = Block.createCuboidShape(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
		shapes[Direction.WEST.getHorizontal()] = Block.createCuboidShape(z0, y0, 16 - x1, z1, y1, 16 - x0);
		this.seatHeight = seatHeight;
		this.codec = createCodec(s -> new FurnitureBlock(s, box, seatHeight));
		setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return codec;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapes[state.get(FACING).getHorizontal()];
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (seatHeight < 0 || !player.getMainHandStack().isEmpty() || player.hasVehicle()) return ActionResult.PASS;
		if (!(world instanceof ServerWorld sw)) return ActionResult.SUCCESS;
		if (!sw.getEntitiesByClass(SeatEntity.class, new Box(pos), e -> true).isEmpty()) return ActionResult.CONSUME;   // taken
		SeatEntity seat = ModEntities.SEAT.create(sw);
		if (seat == null) return ActionResult.PASS;
		float yaw = state.get(FACING).asRotation();
		seat.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + seatHeight - 0.15, pos.getZ() + 0.5, yaw, 0);
		sw.spawnEntity(seat);
		player.startRiding(seat);
		player.setYaw(yaw);
		return ActionResult.CONSUME;
	}
}
