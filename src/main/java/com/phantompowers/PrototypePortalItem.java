package com.phantompowers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * A mini prototype ghost portal. Switch it on and it blasts unstable ghost energy right in your face:
 * if you're a half-ghost, you get ecto acne, and five minutes later your dark side wakes up.
 */
public class PrototypePortalItem extends Item {
	public PrototypePortalItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack, true);
		DarkPowers.portalBlast((ServerPlayerEntity) user);
		user.getItemCooldownManager().set(this, 100);
		return TypedActionResult.success(stack);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Unstable! Right-click to switch it on").formatted(Formatting.RED));
		tooltip.add(Text.literal("Half-ghosts get ecto acne... then turn dark").formatted(Formatting.DARK_GRAY));
	}
}
