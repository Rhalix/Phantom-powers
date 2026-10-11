package com.phantompowers;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class GhostPortalBlock extends Block {
	public static final MapCodec<GhostPortalBlock> CODEC = createCodec(GhostPortalBlock::new);
	public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;

	private static final VoxelShape X_SHAPE = Block.createCuboidShape(0, 0, 6, 16, 16, 10);
	private static final VoxelShape Z_SHAPE = Block.createCuboidShape(6, 0, 0, 10, 16, 16);
	private static final DustParticleEffect GREEN = new DustParticleEffect(new Vector3f(0.2f, 1.0f, 0.35f), 1.0f);

	public GhostPortalBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(AXIS, Direction.Axis.X));
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return state.get(AXIS) == Direction.Axis.Z ? Z_SHAPE : X_SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}

	@Override
	protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (!world.isClient && entity instanceof ServerPlayerEntity player) {
			PortalLogic.requestTravel(player, pos);
		}
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		for (int i = 0; i < 2; i++) {
			world.addParticle(GREEN,
					pos.getX() + random.nextDouble(),
					pos.getY() + random.nextDouble(),
					pos.getZ() + random.nextDouble(),
					0, 0, 0);
		}
	}
}
