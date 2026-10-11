package com.phantompowers;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
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

	private ModItems() {}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(e -> e.add(ECTOPLASM));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> e.add(THERMOS));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e -> {
			e.add(HOSTILE_GHOST_EGG);
			e.add(FRIENDLY_GHOST_EGG);
			e.add(GHOST_JAILER_EGG);
		});
	}
}
