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

	public static final Block GHOST_CRATE = Registry.register(Registries.BLOCK, PhantomMod.id("ghost_crate"),
			new Block(AbstractBlock.Settings.create()
					.mapColor(MapColor.OAK_TAN)
					.strength(1.5f)
					.sounds(BlockSoundGroup.WOOD)));
	public static final Item GHOST_CRATE_ITEM = Registry.register(Registries.ITEM, PhantomMod.id("ghost_crate"),
			new BlockItem(GHOST_CRATE, new Item.Settings()));

	/** Ghost Zone biome blocks. */
	public static final Block SHADOW_GRASS = Registry.register(Registries.BLOCK, PhantomMod.id("shadow_grass"),
			new Block(AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(1.5f, 6.0f).requiresTool()
					.luminance(state -> 3).sounds(BlockSoundGroup.NYLIUM)));
	public static final Block FROST_ECTO = Registry.register(Registries.BLOCK, PhantomMod.id("frost_ecto"),
			new Block(AbstractBlock.Settings.create().mapColor(MapColor.LIGHT_BLUE).strength(0.8f).slipperiness(0.98f)
					.luminance(state -> 7).sounds(BlockSoundGroup.SNOW)));
	public static final Block SPECTRAL_CRYSTAL = Registry.register(Registries.BLOCK, PhantomMod.id("spectral_crystal"),
			new Block(AbstractBlock.Settings.create().mapColor(MapColor.CYAN).strength(1.5f).requiresTool()
					.luminance(state -> 12).sounds(BlockSoundGroup.AMETHYST_BLOCK)));
	public static final Item SHADOW_GRASS_ITEM = Registry.register(Registries.ITEM, PhantomMod.id("shadow_grass"), new BlockItem(SHADOW_GRASS, new Item.Settings()));
	public static final Item FROST_ECTO_ITEM = Registry.register(Registries.ITEM, PhantomMod.id("frost_ecto"), new BlockItem(FROST_ECTO, new Item.Settings()));
	public static final Item SPECTRAL_CRYSTAL_ITEM = Registry.register(Registries.ITEM, PhantomMod.id("spectral_crystal"), new BlockItem(SPECTRAL_CRYSTAL, new Item.Settings()));

	public static final Item ECTO_STONE_ITEM = Registry.register(Registries.ITEM, PhantomMod.id("ecto_stone"),
			new BlockItem(ECTO_STONE, new Item.Settings()));
	public static final Item ECTO_GRASS_ITEM = Registry.register(Registries.ITEM, PhantomMod.id("ecto_grass"),
			new BlockItem(ECTO_GRASS, new Item.Settings()));

	/** Lab-built Ghost Portal: a ring of these (plus Hazard Plating trim) around the opening. */
	public static final Block PORTAL_FRAME = Registry.register(Registries.BLOCK, PhantomMod.id("ghost_portal_frame"),
			new Block(AbstractBlock.Settings.create()
					.mapColor(MapColor.IRON_GRAY)
					.strength(5.0f, 6.0f)
					.requiresTool()
					.luminance(state -> 3)
					.sounds(BlockSoundGroup.METAL)));
	public static final Item PORTAL_FRAME_ITEM = Registry.register(Registries.ITEM, PhantomMod.id("ghost_portal_frame"),
			new BlockItem(PORTAL_FRAME, new Item.Settings()));

	/** A small, unstable, home-made ghost portal. Switch it on and it blasts you in the face. */
	public static final Block PROTOTYPE_PORTAL = Registry.register(Registries.BLOCK, PhantomMod.id("prototype_portal"),
			new PrototypePortalBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.IRON_GRAY)
					.strength(2.5f, 6.0f)
					.nonOpaque()
					.luminance(state -> 7)
					.sounds(BlockSoundGroup.METAL)));

	public static final Block HAZARD_PLATING = Registry.register(Registries.BLOCK, PhantomMod.id("hazard_plating"),
			new Block(AbstractBlock.Settings.create()
					.mapColor(MapColor.YELLOW)
					.strength(5.0f, 6.0f)
					.requiresTool()
					.sounds(BlockSoundGroup.METAL)));
	public static final Item HAZARD_PLATING_ITEM = Registry.register(Registries.ITEM, PhantomMod.id("hazard_plating"),
			new BlockItem(HAZARD_PLATING, new Item.Settings()));

	/** The inside of a Thermos: unbreakable, but a trapped half-ghost can punch their way out. */
	public static final Block THERMOS_WALL = Registry.register(Registries.BLOCK, PhantomMod.id("thermos_wall"),
			new Block(AbstractBlock.Settings.create()
					.mapColor(MapColor.IRON_GRAY)
					.strength(-1.0f, 3600000.0f)
					.dropsNothing()
					.luminance(state -> 9)
					.sounds(BlockSoundGroup.METAL)));

	private ModBlocks() {}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
			entries.add(ECTO_STONE_ITEM);
			entries.add(ECTO_GRASS_ITEM);
			entries.add(GHOST_CRATE_ITEM);
			entries.add(PORTAL_FRAME_ITEM);
			entries.add(HAZARD_PLATING_ITEM);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
			entries.add(PORTAL_FRAME_ITEM);
			entries.add(HAZARD_PLATING_ITEM);
		});
	}
}
