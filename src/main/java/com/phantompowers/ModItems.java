package com.phantompowers;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
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
	public static final Item GHOST_JAILER_EGG = Registry.register(Registries.ITEM, PhantomMod.id("ghost_jailer_spawn_egg"),
			new SpawnEggItem(ModEntities.GHOST_JAILER, 0x2A1838, 0x8A9097, new Item.Settings()));

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
			}), 12, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, () -> Ingredient.ofItems(ECTOPLASM),
					List.of(new ArmorMaterial.Layer(PhantomMod.id("ghost_hunter"))), 2.0f, 0.05f));

	public static final Item HUNTER_SUIT = Registry.register(Registries.ITEM, PhantomMod.id("ghost_hunter_suit"),
			new ArmorItem(HUNTER_MATERIAL, ArmorItem.Type.CHESTPLATE,
					new Item.Settings().maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(30)).rarity(Rarity.RARE)));

	private ModItems() {}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(e -> e.add(ECTOPLASM));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> {
			e.add(THERMOS);
			e.add(HOVER_GLIDER);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(e -> e.add(HUNTER_SUIT));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e -> {
			e.add(HOSTILE_GHOST_EGG);
			e.add(FRIENDLY_GHOST_EGG);
			e.add(GHOST_JAILER_EGG);
		});
	}
}
