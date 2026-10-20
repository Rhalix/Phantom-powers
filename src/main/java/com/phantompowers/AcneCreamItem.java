package com.phantompowers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

/**
 * Ecto-Clear Cream: rub it on (hold right-click) to clear up ecto acne and stop the change into the dark half-ghost.
 * Sneak + right-click someone with it to treat them instead.
 */
public class AcneCreamItem extends Item {
	public AcneCreamItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		user.setCurrentHand(hand);
		return TypedActionResult.consume(user.getStackInHand(hand));
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.BRUSH;
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return 32;
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (!world.isClient && user instanceof ServerPlayerEntity p) treat(p, p, stack);
		return stack;
	}

	@Override
	public net.minecraft.util.ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
		if (entity instanceof ServerPlayerEntity target && user instanceof ServerPlayerEntity sp) {
			treat(sp, target, stack);
			return net.minecraft.util.ActionResult.SUCCESS;
		}
		return net.minecraft.util.ActionResult.PASS;
	}

	private static void treat(ServerPlayerEntity user, ServerPlayerEntity target, ItemStack stack) {
		GhostData d = GhostState.of(target.getServer()).data(target.getUuid());
		if (d.acneTicks <= 0) {
			user.sendMessage(Text.literal(target == user ? "You don't have ecto acne." : target.getName().getString() + " doesn't have ecto acne.")
					.formatted(Formatting.GRAY), true);
			return;
		}
		DarkPowers.endAcne(target, false);
		if (target != user) user.sendMessage(Text.literal("You cleared up " + target.getName().getString() + "'s ecto acne.").formatted(Formatting.GREEN), true);
		stack.decrementUnlessCreative(1, user);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Hold right-click: clear your ecto acne").formatted(Formatting.GREEN));
		tooltip.add(Text.literal("Right-click a player: treat them").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Stops the change into the dark half-ghost").formatted(Formatting.DARK_GRAY));
	}
}
