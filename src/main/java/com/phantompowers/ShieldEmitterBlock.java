package com.phantompowers;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.List;

/**
 * Ecto-Shield Emitter: a little tripod pylon with an ecto crystal on top. Place a few around your base, link them
 * to a Shield Remote, and press the remote to raise a green dome that ghosts can't get through.
 */
public class ShieldEmitterBlock extends Block {
	public static final MapCodec<ShieldEmitterBlock> CODEC = createCodec(ShieldEmitterBlock::new);
	public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
	private static final VoxelShape SHAPE = VoxelShapes.union(
			Block.createCuboidShape(2, 0, 2, 14, 3, 14),
			Block.createCuboidShape(5, 3, 5, 11, 12, 11),
			Block.createCuboidShape(4, 12, 4, 12, 16, 12));

	public ShieldEmitterBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(ACTIVE, false));
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld sw) ShieldDomes.onEmitterGone(sw, pos);
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (!state.get(ACTIVE)) {
			if (random.nextInt(4) == 0) world.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 0, 0.02, 0);
			return;
		}
		for (int i = 0; i < 2; i++) {
			world.addParticle(ParticleTypes.GLOW, pos.getX() + 0.5 + random.nextGaussian() * 0.15, pos.getY() + 1.0, pos.getZ() + 0.5 + random.nextGaussian() * 0.15, 0, 0.06, 0);
		}
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
		tooltip.add(Text.literal("Place a few around your base (or just around you)").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Right-click them with a Shield Remote to link").formatted(Formatting.DARK_GRAY));
	}
}
