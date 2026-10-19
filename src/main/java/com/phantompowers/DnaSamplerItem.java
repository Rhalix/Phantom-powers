package com.phantompowers;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/** The DNA Sampler: right-click to take a sample of your own DNA (ouch). The filled sample goes into a Cloning Pod. */
public class DnaSamplerItem extends Item {
	private final boolean filled;

	public DnaSamplerItem(boolean filled, Settings settings) {
		super(settings);
		this.filled = filled;
	}

	public static UUID owner(ItemStack s) {
		NbtCompound n = s.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
		return n.containsUuid("DnaOwner") ? n.getUuid("DnaOwner") : null;
	}

	public static String ownerName(ItemStack s) {
		return s.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getString("DnaName");
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (filled) return TypedActionResult.pass(stack);
		if (!world.isClient) {
			user.damage(world.getDamageSources().generic(), 1f);
			world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 1f, 1.4f);
			ItemStack sample = new ItemStack(ModItems.DNA_SAMPLE);
			NbtCompound n = new NbtCompound();
			n.putUuid("DnaOwner", user.getUuid());
			n.putString("DnaName", user.getName().getString());
			sample.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
			stack.decrementUnlessCreative(1, user);
			if (!user.giveItemStack(sample)) user.dropItem(sample, false);
			user.sendMessage(Text.literal("You took a sample of your own DNA. Put it in a Cloning Pod!").formatted(Formatting.GREEN), true);
		}
		return TypedActionResult.success(stack, world.isClient);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		if (!filled) {
			tooltip.add(Text.literal("Right-click to sample your DNA").formatted(Formatting.GRAY));
			return;
		}
		String name = ownerName(stack);
		tooltip.add(Text.literal("DNA of: " + (name.isEmpty() ? "unknown" : name)).formatted(Formatting.GREEN));
		tooltip.add(Text.literal("Use it on a Cloning Pod").formatted(Formatting.GRAY));
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return filled;
	}
}
