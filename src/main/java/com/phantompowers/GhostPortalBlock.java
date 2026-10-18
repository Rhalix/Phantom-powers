package com.phantompowers;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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

	private static final DustParticleEffect GREEN_BIG = new DustParticleEffect(new Vector3f(0.35f, 1.0f, 0.5f), 1.6f);
	private static final DustParticleEffect GREEN_SMALL = new DustParticleEffect(new Vector3f(0.6f, 1.0f, 0.7f), 0.6f);
	private static final DustParticleEffect DARK = new DustParticleEffect(new Vector3f(0.05f, 0.45f, 0.15f), 1.2f);

	/**
	 * A churning vortex of ecto energy: swirling motes that spiral across the surface, wisps drifting out of
	 * the portal, crackling sparks, rising soul flames and the low hum of the Ghost Zone.
	 */
	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		boolean xAxis = state.get(AXIS) == Direction.Axis.X;
		double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
		long t = world.getTime();
		PortalSwirls.reportGhostPortal(world, pos, state, xAxis);
		// Swirl: motes circling in the portal plane
		for (int i = 0; i < 3; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = 0.15 + random.nextDouble() * 0.45;
			double ox = Math.cos(a) * r, oy = Math.sin(a) * r;
			double spin = 0.06;
			double vx = -Math.sin(a) * spin, vy = Math.cos(a) * spin;
			double x = xAxis ? cx + ox : cx + (random.nextDouble() - 0.5) * 0.2;
			double z = xAxis ? cz + (random.nextDouble() - 0.5) * 0.2 : cz + ox;
			world.addParticle(i == 0 ? GREEN_BIG : i == 1 ? GREEN : DARK, x, cy + oy, z,
					xAxis ? vx : 0, vy, xAxis ? 0 : vx);
		}
		// Wisps drifting out of both faces
		if (random.nextInt(3) == 0) {
			double out = (random.nextBoolean() ? 1 : -1) * (0.02 + random.nextDouble() * 0.04);
			world.addParticle(GREEN_SMALL, cx + (random.nextDouble() - 0.5), cy + (random.nextDouble() - 0.5), cz + (random.nextDouble() - 0.5),
					xAxis ? 0 : out, 0.01, xAxis ? out : 0);
		}
		if (random.nextInt(6) == 0) {
			double out = (random.nextBoolean() ? 1 : -1) * 0.03;
			world.addParticle(ParticleTypes.END_ROD, cx + (random.nextDouble() - 0.5) * 0.8, cy + (random.nextDouble() - 0.5) * 0.8,
					cz + (random.nextDouble() - 0.5) * 0.8, xAxis ? 0 : out, 0.005, xAxis ? out : 0);
		}
		// Crackling sparks at the edges, now and then a jolt
		if (random.nextInt(5) == 0) {
			world.addParticle(ParticleTypes.ELECTRIC_SPARK, cx + (random.nextDouble() - 0.5), cy + (random.nextDouble() - 0.5),
					cz + (random.nextDouble() - 0.5), (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3);
		}
		if (random.nextInt(10) == 0) {
			world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, cx + (random.nextDouble() - 0.5) * 0.6, cy - 0.4,
					cz + (random.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
		}
		if (random.nextInt(14) == 0) {
			world.addParticle(ParticleTypes.GLOW, cx + (random.nextDouble() - 0.5), cy + (random.nextDouble() - 0.5), cz + (random.nextDouble() - 0.5), 0, 0.02, 0);
		}
		if ((t + pos.hashCode()) % 80 == 0 && random.nextInt(4) == 0) {
			world.playSound(cx, cy, cz, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.BLOCKS, 0.25f, 0.5f + random.nextFloat() * 0.2f, false);
		}
	}
}
