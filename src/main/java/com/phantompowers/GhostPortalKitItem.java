package com.phantompowers;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Builds the full-size lab Ghost Portal (frame, hazard trim and the switch inside) right where you click. */
public class GhostPortalKitItem extends Item {
	public GhostPortalKitItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		if (!(ctx.getWorld() instanceof ServerWorld w)) return ActionResult.SUCCESS;
		PlayerEntity p = ctx.getPlayer();
		Direction facing = ctx.getHorizontalPlayerFacing();
		Direction.Axis axis = facing.getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;
		Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		BlockPos ground = ctx.getBlockPos().offset(ctx.getSide() == Direction.UP ? Direction.UP : ctx.getSide()).down();
		BlockPos base = ground.offset(along, -(PortalLogic.LAB_W + 1) / 2).up();
		BlockPos.Mutable m = new BlockPos.Mutable();
		for (int i = 0; i <= PortalLogic.LAB_W + 1; i++) {
			for (int j = 0; j <= PortalLogic.LAB_H + 1; j++) {
				m.set(base.offset(along, i).up(j));
				if (!w.getBlockState(m).isReplaceable()) {
					if (p != null) p.sendMessage(Text.literal("Not enough room: the lab portal needs a 6 wide, 7 tall space.").formatted(Formatting.RED), true);
					return ActionResult.FAIL;
				}
			}
		}
		Set<BlockPos> inside = PortalLogic.buildLabPortal(w, base, axis, Block.NOTIFY_ALL, false);
		BlockPos button = inside.stream().min(Comparator.comparingInt(BlockPos::getY).thenComparingInt(b -> b.getX() + b.getZ())).orElse(null);
		if (button != null) {
			w.setBlockState(button, Blocks.STONE_BUTTON.getDefaultState().with(ButtonBlock.FACE, BlockFace.FLOOR)
					.with(ButtonBlock.FACING, facing.getOpposite()), Block.NOTIFY_ALL);
		}
		w.playSound(null, base, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1f, 0.7f);
		w.playSound(null, base, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1f, 0.8f);
		if (p != null) {
			p.sendMessage(Text.literal("Ghost Portal built! Press the switch inside it to turn it on.").formatted(Formatting.GREEN), true);
			if (!p.getAbilities().creativeMode) ctx.getStack().decrement(1);
		}
		return ActionResult.CONSUME;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Builds the big lab Ghost Portal where you click").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("4x5 opening with clipped corners, hazard trim and a switch").formatted(Formatting.DARK_GRAY));
	}
}
