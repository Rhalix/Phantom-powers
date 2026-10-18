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

/** Ghost hunter upgrade parts: right-click while wearing the suit pack to install (the glider kit goes on the glider). */
public class UpgradeItem extends Item {
	private final String kind;

	public UpgradeItem(String kind, Settings settings) {
		super(settings);
		this.kind = kind;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (kind.equals("glider")) return TypedActionResult.pass(stack);
		if (world.isClient) return TypedActionResult.success(stack, true);
		if (HunterGear.install((ServerPlayerEntity) user, kind)) {
			stack.decrementUnlessCreative(1, user);
			return TypedActionResult.success(stack);
		}
		return TypedActionResult.fail(stack);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		String line = switch (kind) {
			case "tier" -> "Right-click while wearing the suit pack: +1 suit tier (max Mk 4)";
			case "missiles" -> "Installs wrist missiles. Suit on: press R to fire homing missiles";
			case "cannons" -> "Installs backpack cannons. Suit on: press B to deploy auto-turrets";
			default -> "Right-click your Hover Glider to upgrade it (max 3)";
		};
		tooltip.add(Text.literal(line).formatted(Formatting.AQUA));
	}
}
