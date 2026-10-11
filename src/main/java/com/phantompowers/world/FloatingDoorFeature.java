package com.phantompowers.world;

import com.mojang.serialization.Codec;
import com.phantompowers.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** The floating doors drifting around the Ghost Zone, each on a little chunk of ecto rock. */
public class FloatingDoorFeature extends Feature<DefaultFeatureConfig> {
	public FloatingDoorFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();

		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int dy = -3; dy <= 2; dy++) {
					if (!world.isAir(origin.add(dx, dy, dz))) return false;
				}
			}
		}

		Direction facing = Direction.Type.HORIZONTAL.random(random);
		Block door = random.nextBoolean() ? Blocks.CRIMSON_DOOR : Blocks.WARPED_DOOR;
		BlockState lower = door.getDefaultState().with(DoorBlock.FACING, facing).with(DoorBlock.HALF, DoubleBlockHalf.LOWER);
		BlockState upper = lower.with(DoorBlock.HALF, DoubleBlockHalf.UPPER);

		// A small floating rock to stand on in front of the door
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				world.setBlockState(origin.add(dx, -1, dz), ModBlocks.ECTO_GRASS.getDefaultState(), Block.NOTIFY_LISTENERS);
				if (random.nextInt(3) != 0) world.setBlockState(origin.add(dx, -2, dz), ModBlocks.ECTO_STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		}
		if (random.nextBoolean()) world.setBlockState(origin.down(3), ModBlocks.ECTO_STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
		world.setBlockState(origin, lower, Block.NOTIFY_LISTENERS);
		world.setBlockState(origin.up(), upper, Block.NOTIFY_LISTENERS);
		return true;
	}
}
