package com.phantompowers;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {
	public static final Block GHOST_PORTAL = Registry.register(Registries.BLOCK, PhantomMod.id("ghost_portal"),
			new GhostPortalBlock(AbstractBlock.Settings.create()
					.noCollision()
					.strength(1.0f)
					.luminance(state -> 12)
					.sounds(BlockSoundGroup.GLASS)
					.nonOpaque()
					.dropsNothing()
					.pistonBehavior(PistonBehavior.BLOCK)
					.mapColor(MapColor.LIME)));

	public static final Block ECTO_STONE = Registry.register(Registries.BLOCK, PhantomMod.id("ecto_stone"),
			new Block(AbstractBlock.Settings.create()
					.mapColor(MapColor.PURPLE)
					.strength(1.5f, 6.0f)
					.requiresTool()
					.sounds(BlockSoundGroup.STONE)));

	public static final Block ECTO_GRASS = Registry.register(Registries.BLOCK, PhantomMod.id("ecto_grass"),
			new Block(AbstractBlock.Settings.create()
					.mapColor(MapColor.LIME)
					.strength(1.5f, 6.0f)
					.requiresTool()
					.luminance(state -> 6)
					.sounds(BlockSoundGroup.NYLIUM)));

	public static final Item ECTO_STONE_ITEM = Registry.register(Registries.ITEM, PhantomMod.id("ecto_stone"),
			new BlockItem(ECTO_STONE, new Item.Settings()));
	public static final Item ECTO_GRASS_ITEM = Registry.register(Registries.ITEM, PhantomMod.id("ecto_grass"),
			new BlockItem(ECTO_GRASS, new Item.Settings()));

	private ModBlocks() {}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
			entries.add(ECTO_STONE_ITEM);
			entries.add(ECTO_GRASS_ITEM);
		});
	}
}
