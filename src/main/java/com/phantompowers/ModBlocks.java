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

	public static final Block HUNTER_WORKBENCH = Registry.register(Registries.BLOCK, PhantomMod.id("hunter_workbench"),
			new HunterWorkbenchBlock(AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).strength(2.5f, 6.0f).nonOpaque()
					.luminance(state -> 6).sounds(BlockSoundGroup.METAL)));
	public static final Item HUNTER_WORKBENCH_ITEM = Registry.register(Registries.ITEM, PhantomMod.id("hunter_workbench"),
			new BlockItem(HUNTER_WORKBENCH, new Item.Settings()));

	public static final Block CLONING_POD = Registry.register(Registries.BLOCK, PhantomMod.id("cloning_pod"),
			new CloningPodBlock(AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).strength(3.0f, 6.0f).nonOpaque()
					.luminance(state -> state.get(CloningPodBlock.ACTIVE) ? 13 : 4).sounds(BlockSoundGroup.METAL)));
	public static final Item CLONING_POD_ITEM = Registry.register(Registries.ITEM, PhantomMod.id("cloning_pod"),
			new BlockItem(CLONING_POD, new Item.Settings().maxCount(1)));

	public static final Block SHIELD_EMITTER = Registry.register(Registries.BLOCK, PhantomMod.id("shield_emitter"),
			new ShieldEmitterBlock(AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).strength(2.0f, 6.0f).nonOpaque()
					.luminance(state -> state.get(ShieldEmitterBlock.ACTIVE) ? 11 : 3).sounds(BlockSoundGroup.METAL)));
	public static final Item SHIELD_EMITTER_ITEM = Registry.register(Registries.ITEM, PhantomMod.id("shield_emitter"),
			new BlockItem(SHIELD_EMITTER, new Item.Settings().maxCount(16)));

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

	// ------------------------------------------------------------- furniture
	public static final Block OAK_CHAIR = furniture("oak_chair", new double[]{2, 0, 2, 14, 10, 14}, 0.55, BlockSoundGroup.WOOD);
	public static final Block BAR_STOOL = furniture("bar_stool", new double[]{4, 0, 4, 12, 12, 12}, 0.78, BlockSoundGroup.METAL);
	public static final Block SOFA = register("sofa", new SofaBlock(AbstractBlock.Settings.create().mapColor(MapColor.LIGHT_BLUE).strength(1.5f).nonOpaque()
			.sounds(BlockSoundGroup.WOOL), new double[]{0, 0, 0, 16, 12, 16}, 0.50));
	public static final Block DINING_TABLE = furniture("dining_table", new double[]{0, 0, 0, 16, 16, 16}, -1, BlockSoundGroup.WOOD);
	public static final Block COFFEE_TABLE = furniture("coffee_table", new double[]{0, 0, 0, 16, 8, 16}, -1, BlockSoundGroup.WOOD);
	public static final Block FLOOR_LAMP = furniture("floor_lamp", new double[]{2, 0, 2, 14, 16, 14}, -1, BlockSoundGroup.WOOD, true);
	public static final Block TELEVISION = furniture("television", new double[]{0, 0, 4, 16, 15, 12}, -1, BlockSoundGroup.METAL, false);
	public static final Block KITCHEN_COUNTER = furniture("kitchen_counter", new double[]{0, 0, 0, 16, 16, 16}, -1, BlockSoundGroup.WOOD);
	public static final Block FRIDGE = register("fridge", new TallStorageBlock(AbstractBlock.Settings.create().mapColor(MapColor.WHITE).strength(2f).nonOpaque()
			.sounds(BlockSoundGroup.METAL), new double[]{1, 1, 15, 15}, net.minecraft.sound.SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN));
	public static final Block SCHOOL_DESK = furniture("school_desk", new double[]{1, 0, 2, 15, 13, 14}, -1, BlockSoundGroup.WOOD);
	public static final Block LOCKER = register("locker", new TallStorageBlock(AbstractBlock.Settings.create().mapColor(MapColor.LIGHT_GRAY).strength(2f).nonOpaque()
			.sounds(BlockSoundGroup.METAL), new double[]{1, 2, 15, 16}, net.minecraft.sound.SoundEvents.BLOCK_IRON_DOOR_OPEN));
	public static final Block LAB_CONSOLE = furniture("lab_console", new double[]{0, 0, 4, 16, 16, 16}, -1, BlockSoundGroup.METAL, false);
	public static final Block BOOKCASE = furniture("bookcase", new double[]{0, 0, 8, 16, 16, 16}, -1, BlockSoundGroup.WOOD);
	public static final Block POTTED_PLANT = furniture("potted_plant", new double[]{3, 0, 3, 13, 16, 13}, -1, BlockSoundGroup.AZALEA_LEAVES);

	public static final Block[] FURNITURE = {OAK_CHAIR, BAR_STOOL, SOFA, DINING_TABLE, COFFEE_TABLE, FLOOR_LAMP, TELEVISION, KITCHEN_COUNTER, FRIDGE,
			SCHOOL_DESK, LOCKER, LAB_CONSOLE, BOOKCASE, POTTED_PLANT};

	public static final net.minecraft.block.entity.BlockEntityType<FurnitureStorageBlockEntity> STORAGE_FURNITURE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
			PhantomMod.id("storage_furniture"), net.minecraft.block.entity.BlockEntityType.Builder.create(FurnitureStorageBlockEntity::new, FRIDGE, LOCKER).build(null));

	private static Block register(String name, Block b) {
		Registry.register(Registries.BLOCK, PhantomMod.id(name), b);
		Registry.register(Registries.ITEM, PhantomMod.id(name), new BlockItem(b, new Item.Settings()));
		return b;
	}

	private static Block furniture(String name, double[] box, double seat, BlockSoundGroup sound) {
		return furniture(name, box, seat, sound, 0);
	}

	private static Block furniture(String name, double[] box, double seat, BlockSoundGroup sound, boolean lamp) {
		return furniture(name, box, seat, sound, lamp ? 14 : 6);
	}

	private static Block furniture(String name, double[] box, double seat, BlockSoundGroup sound, int light) {
		Block b = Registry.register(Registries.BLOCK, PhantomMod.id(name), new FurnitureBlock(AbstractBlock.Settings.create()
				.mapColor(MapColor.OAK_TAN).strength(1.5f).nonOpaque().sounds(sound).luminance(state -> light), box, seat));
		Registry.register(Registries.ITEM, PhantomMod.id(name), new BlockItem(b, new Item.Settings()));
		return b;
	}

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
