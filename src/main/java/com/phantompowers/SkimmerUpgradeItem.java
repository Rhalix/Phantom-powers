package com.phantompowers;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** A bolt-on upgrade for the Ecto-Skimmer: right-click the ship with it to install. */
public class SkimmerUpgradeItem extends Item {
	public final int bit;
	public final String title, blurb;

	public SkimmerUpgradeItem(int bit, String title, String blurb, Settings settings) {
		super(settings);
		this.bit = bit;
		this.title = title;
		this.blurb = blurb;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal(blurb).formatted(Formatting.AQUA));
		tooltip.add(Text.literal("Right-click an Ecto-Skimmer to install").formatted(Formatting.DARK_GRAY));
	}
}
