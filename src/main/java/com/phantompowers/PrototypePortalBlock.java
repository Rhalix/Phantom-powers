package com.phantompowers;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** The Prototype Ghost Portal, placed on the ground. It faces you when you put it down; right-click it to switch it on. */
public class PrototypePortalBlock extends HorizontalFacingBlock {
	public static final MapCodec<PrototypePortalBlock> CODEC = createCodec(PrototypePortalBlock::new);
	private static final VoxelShape NS = Block.createCuboidShape(1, 0, 4, 15, 16, 12);
	private static final VoxelShape EW = Block.createCuboidShape(4, 0, 1, 12, 16, 15);

	public PrototypePortalBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
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
		return state.get(FACING).getAxis() == Direction.Axis.Z ? NS : EW;
	}

	/** Keeps the little vortex in its frame swirling, with the odd spark. */
	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, net.minecraft.util.math.random.Random random) {
		Direction f = state.get(FACING);
		net.minecraft.util.math.Vec3d along = f.getAxis() == Direction.Axis.Z ? new net.minecraft.util.math.Vec3d(1, 0, 0) : new net.minecraft.util.math.Vec3d(0, 0, 1);
		PortalSwirls.report(net.minecraft.util.math.Vec3d.ofCenter(pos), along, 0.25, 0.375, true, world.getTime());
		if (random.nextInt(3) == 0) world.addParticle(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK,
				pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6, pos.getY() + 0.9, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) return ActionResult.SUCCESS;
		if (player instanceof ServerPlayerEntity sp) {
			Direction facing = state.get(FACING);
			// it needs a little room in front to blast you across
			if (!world.getBlockState(pos.offset(facing)).getCollisionShape(world, pos.offset(facing)).isEmpty()
					|| !world.getBlockState(pos.offset(facing, 2)).getCollisionShape(world, pos.offset(facing, 2)).isEmpty()) {
				GhostPowers.bar(sp, "Clear some space in front of the portal first.", Formatting.YELLOW);
				return ActionResult.CONSUME;
			}
			DarkPowers.startPortalCutscene(sp, pos, facing);
		}
		return ActionResult.CONSUME;
	}
}
