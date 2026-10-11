package com.phantompowers;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/** A ghost's essence. Eat it to take that ghost's power for yourself, until you die. */
public class EssenceItem extends Item {
	public static final FoodComponent FOOD = new FoodComponent.Builder().nutrition(2).saturationModifier(0.3f).alwaysEdible().build();

	private final int power;

	public EssenceItem(int power, Settings settings) {
		super(settings.food(FOOD));
		this.power = power;
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		ItemStack result = super.finishUsing(stack, world, user);
		if (!world.isClient && user instanceof ServerPlayerEntity p) GhostPowers.eatEssence(p, power);
		return result;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Eat to gain: " + GhostPowers.ESSENCE_POWERS[power]).formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.literal("Lasts until you die").formatted(Formatting.DARK_GRAY));
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}
}
