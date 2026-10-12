package com.phantompowers;

import com.phantompowers.entity.GhostNetProjectile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** The Ghost Net: a folded cube of ecto-mesh. Throw it at a ghost and it bursts open into a glowing cage. */
public class GhostNetItem extends Item {
	public GhostNetItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.8f, 0.6f);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.4f, 2.0f);
		if (!world.isClient) {
			GhostNetProjectile net = new GhostNetProjectile(world, user);
			net.setItem(stack.copyWithCount(1));
			net.setVelocity(user, user.getPitch(), user.getYaw(), 0f, 1.7f, 0.6f);
			world.spawnEntity(net);
		}
		user.getItemCooldownManager().set(this, 20);
		stack.decrementUnlessCreative(1, user);
		return TypedActionResult.success(stack, world.isClient());
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Throw at a ghost or a half-ghost to trap them").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Trapped half-ghosts can spam jump to break free").formatted(Formatting.DARK_GRAY));
	}
}
