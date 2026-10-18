package com.phantompowers;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;

import java.util.EnumMap;
import java.util.List;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

public final class ModItems {
	public static final Item ECTOPLASM = Registry.register(Registries.ITEM, PhantomMod.id("ectoplasm"),
			new Item(new Item.Settings()));

	public static final Item THERMOS = Registry.register(Registries.ITEM, PhantomMod.id("thermos"),
			new ThermosItem(new Item.Settings().maxDamage(40).rarity(Rarity.UNCOMMON)));

	public static final Item HOSTILE_GHOST_EGG = Registry.register(Registries.ITEM, PhantomMod.id("hostile_ghost_spawn_egg"),
			new SpawnEggItem(ModEntities.HOSTILE_GHOST, 0x1C2B22, 0x46FF8C, new Item.Settings()));
	public static final Item FRIENDLY_GHOST_EGG = Registry.register(Registries.ITEM, PhantomMod.id("friendly_ghost_spawn_egg"),
			new SpawnEggItem(ModEntities.FRIENDLY_GHOST, 0xD8F4FF, 0x6FB8FF, new Item.Settings()));
	public static final Item GHOUL_SPAWN_EGG = Registry.register(Registries.ITEM, PhantomMod.id("ghoul_spawn_egg"),
			new SpawnEggItem(ModEntities.GHOUL, 0x9AB4A6, 0x1E2024, new Item.Settings()));
	public static final Item SPECTER_KNIGHT_SPAWN_EGG = Registry.register(Registries.ITEM, PhantomMod.id("specter_knight_spawn_egg"),
			new SpawnEggItem(ModEntities.SPECTER_KNIGHT, 0x96AFCD, 0x78E6FF, new Item.Settings()));
	public static final Item LOST_SOUL_SPAWN_EGG = Registry.register(Registries.ITEM, PhantomMod.id("lost_soul_spawn_egg"),
			new SpawnEggItem(ModEntities.LOST_SOUL, 0xB0D6EC, 0x465A82, new Item.Settings()));
	public static final Item GHOST_YETI_SPAWN_EGG = Registry.register(Registries.ITEM, PhantomMod.id("ghost_yeti_spawn_egg"),
			new SpawnEggItem(ModEntities.GHOST_YETI, 0xECF2FA, 0x788CAA, new Item.Settings()));
	public static final Item GHOST_JAILER_EGG = Registry.register(Registries.ITEM, PhantomMod.id("ghost_jailer_spawn_egg"),
			new SpawnEggItem(ModEntities.GHOST_JAILER, 0x2A1838, 0x8A9097, new Item.Settings()));

	public static final Item GHOST_NET = Registry.register(Registries.ITEM, PhantomMod.id("ghost_net"),
			new GhostNetItem(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));

	public static final Item ECTO_LAUNCHER = Registry.register(Registries.ITEM, PhantomMod.id("ecto_launcher"),
			new EctoLauncherItem(new Item.Settings().maxDamage(300).rarity(Rarity.RARE)));
	public static final Item REV_EGG = Registry.register(Registries.ITEM, PhantomMod.id("rev_spawn_egg"),
			new net.minecraft.item.SpawnEggItem(ModEntities.REV, 0x1C1C22, 0xC8CCD6, new Item.Settings()));
	public static final Item ROXY_EGG = Registry.register(Registries.ITEM, PhantomMod.id("roxy_spawn_egg"),
			new net.minecraft.item.SpawnEggItem(ModEntities.ROXY, 0x1C9696, 0xFF50B4, new Item.Settings()));
	public static final Item BIKER_COUPLE_EGG = Registry.register(Registries.ITEM, PhantomMod.id("biker_couple_spawn_egg"),
			new BikerCoupleItem(new Item.Settings().rarity(Rarity.RARE)));

	public static final Item BIKE_KEYS = Registry.register(Registries.ITEM, PhantomMod.id("rev_bike_keys"),
			new GhostBikeKeysItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
	public static final Item HEART_CHARM = Registry.register(Registries.ITEM, PhantomMod.id("heart_charm"),
			new HeartCharmItem(new Item.Settings().maxDamage(250).rarity(Rarity.EPIC)));

	public static final Item SHADE_TENDRIL = Registry.register(Registries.ITEM, PhantomMod.id("shade_tendril"),
			new ShadeTendrilItem(new Item.Settings().maxDamage(300).rarity(Rarity.EPIC)));
	public static final Item SHADE_EGG = Registry.register(Registries.ITEM, PhantomMod.id("shade_spawn_egg"),
			new net.minecraft.item.SpawnEggItem(ModEntities.SHADE, 0x221A30, 0xFF8C28, new Item.Settings()));

	public static final Item PROTOTYPE_PORTAL = Registry.register(Registries.ITEM, PhantomMod.id("prototype_portal"),
			new net.minecraft.item.BlockItem(ModBlocks.PROTOTYPE_PORTAL, new Item.Settings().maxCount(1).rarity(Rarity.EPIC)) {
				@Override
				public void appendTooltip(net.minecraft.item.ItemStack stack, TooltipContext context, java.util.List<net.minecraft.text.Text> tooltip,
						net.minecraft.item.tooltip.TooltipType type) {
					tooltip.add(net.minecraft.text.Text.literal("Place it, then right-click to switch it on").formatted(net.minecraft.util.Formatting.RED));
					tooltip.add(net.minecraft.text.Text.literal("Unstable! Blasts whoever's in front of it...").formatted(net.minecraft.util.Formatting.DARK_GRAY));
				}
			});
	public static final Item SUIT_UPGRADE = Registry.register(Registries.ITEM, PhantomMod.id("suit_upgrade_module"),
			new UpgradeItem("tier", new Item.Settings().rarity(Rarity.RARE)));
	public static final Item GLIDER_UPGRADE = Registry.register(Registries.ITEM, PhantomMod.id("glider_upgrade_kit"),
			new UpgradeItem("glider", new Item.Settings().rarity(Rarity.RARE)));
	public static final Item WRIST_MISSILES = Registry.register(Registries.ITEM, PhantomMod.id("wrist_missile_module"),
			new UpgradeItem("missiles", new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
	public static final Item BACKPACK_CANNONS = Registry.register(Registries.ITEM, PhantomMod.id("backpack_cannon_module"),
			new UpgradeItem("cannons", new Item.Settings().maxCount(1).rarity(Rarity.RARE)));

	public static final Item HOVER_GLIDER = Registry.register(Registries.ITEM, PhantomMod.id("hover_glider"),
			new HoverGliderItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));

	public static final RegistryEntry<ArmorMaterial> HUNTER_MATERIAL = Registry.registerReference(Registries.ARMOR_MATERIAL,
			PhantomMod.id("ghost_hunter"),
			new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
				m.put(ArmorItem.Type.BOOTS, 3);
				m.put(ArmorItem.Type.LEGGINGS, 6);
				m.put(ArmorItem.Type.CHESTPLATE, 8);
				m.put(ArmorItem.Type.HELMET, 3);
				m.put(ArmorItem.Type.BODY, 8);
			}), 15, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, () -> Ingredient.ofItems(ECTOPLASM),
					List.of(new ArmorMaterial.Layer(PhantomMod.id("ghost_hunter"))), 3.0f, 0.1f));

	public static final Item HUNTER_SUIT = Registry.register(Registries.ITEM, PhantomMod.id("ghost_hunter_suit"),
			new ArmorItem(HUNTER_MATERIAL, ArmorItem.Type.CHESTPLATE,
					new Item.Settings().maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(37)).rarity(Rarity.RARE).fireproof()));

	public static final Item PANDORAS_BOX = Registry.register(Registries.ITEM, PhantomMod.id("pandoras_box"),
			new PandorasBoxItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof()));
	public static final Item CRATE_KING_EGG = Registry.register(Registries.ITEM, PhantomMod.id("crate_king_spawn_egg"),
			new SpawnEggItem(ModEntities.CRATE_KING, 0xB98A55, 0x7A2FD0, new Item.Settings()));

	public static final Item GHOST_BLASTER = Registry.register(Registries.ITEM, PhantomMod.id("ghost_blaster"),
			new GhostBlasterItem(new Item.Settings().maxDamage(750).rarity(Rarity.RARE)));

	public static final Item THUNDER_GUITAR = Registry.register(Registries.ITEM, PhantomMod.id("thunder_guitar"),
			new ThunderGuitarItem(new Item.Settings().maxDamage(900).rarity(Rarity.EPIC).fireproof()
					.attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.DIAMOND, 5, -2.5f))));
	public static final Item VOLTAGE_EGG = Registry.register(Registries.ITEM, PhantomMod.id("voltage_spawn_egg"),
			new SpawnEggItem(ModEntities.VOLTAGE, 0x1B1426, 0xD94DFF, new Item.Settings()));

	public static final Item[] ESSENCES = {
			essence("hostile_ghost_essence", GhostPowers.ESS_HOSTILE, Rarity.UNCOMMON),
			essence("friendly_ghost_essence", GhostPowers.ESS_FRIENDLY, Rarity.UNCOMMON),
			essence("jailer_essence", GhostPowers.ESS_JAILER, Rarity.EPIC),
			essence("crate_king_essence", GhostPowers.ESS_CRATE, Rarity.EPIC),
			essence("voltage_essence", GhostPowers.ESS_VOLTAGE, Rarity.EPIC),
			essence("wishkeeper_essence", GhostPowers.ESS_WISH, Rarity.EPIC),
			essence("rev_essence", GhostPowers.ESS_REV, Rarity.EPIC),
			essence("roxy_essence", GhostPowers.ESS_ROXY, Rarity.EPIC),
			essence("shade_essence", GhostPowers.ESS_SHADE, Rarity.EPIC)};

	public static final Item WISHKEEPER_EGG = Registry.register(Registries.ITEM, PhantomMod.id("wishkeeper_spawn_egg"),
			new SpawnEggItem(ModEntities.WISHKEEPER, 0x2FC9B8, 0xF2C230, new Item.Settings()));
	public static final Item WISHING_LAMP = Registry.register(Registries.ITEM, PhantomMod.id("wishing_lamp"),
			new WishingLampItem(new Item.Settings().maxDamage(30).rarity(Rarity.EPIC).fireproof()));

	private static Item essence(String name, int power, Rarity rarity) {
		return Registry.register(Registries.ITEM, PhantomMod.id(name), new EssenceItem(power, new Item.Settings().rarity(rarity)));
	}

	private ModItems() {}

	public static void init() {
		// Our own creative tab with every item and block in the mod
		Registry.register(Registries.ITEM_GROUP, PhantomMod.id("main"), net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
				.icon(() -> new net.minecraft.item.ItemStack(THERMOS))
				.displayName(net.minecraft.text.Text.translatable("itemGroup.phantom.main"))
				.entries((context, entries) -> {
					for (Item item : Registries.ITEM) {
						if (Registries.ITEM.getId(item).getNamespace().equals("phantom")) entries.add(item);
					}
				})
				.build());
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(e -> e.add(ECTOPLASM));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(e -> {
			for (Item i : ESSENCES) e.add(i);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> {
			e.add(THERMOS);
			e.add(HOVER_GLIDER);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(e -> {
			e.add(HUNTER_SUIT);
			e.add(GHOST_BLASTER);
			e.add(GHOST_NET);
			e.add(THUNDER_GUITAR);
			e.add(PANDORAS_BOX);
			e.add(WISHING_LAMP);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e -> {
			e.add(HOSTILE_GHOST_EGG);
			e.add(FRIENDLY_GHOST_EGG);
			e.add(GHOST_JAILER_EGG);
			e.add(CRATE_KING_EGG);
			e.add(VOLTAGE_EGG);
			e.add(WISHKEEPER_EGG);
		});
	}
}
