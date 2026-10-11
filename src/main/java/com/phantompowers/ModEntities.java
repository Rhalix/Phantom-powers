package com.phantompowers;

import com.phantompowers.entity.FriendlyGhostEntity;
import com.phantompowers.entity.GhostJailerEntity;
import com.phantompowers.entity.HostileGhostEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
	public static final EntityType<HostileGhostEntity> HOSTILE_GHOST = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("hostile_ghost"),
			EntityType.Builder.create(HostileGhostEntity::new, SpawnGroup.MONSTER)
					.dimensions(0.7f, 1.9f).maxTrackingRange(10).build("hostile_ghost"));

	public static final EntityType<FriendlyGhostEntity> FRIENDLY_GHOST = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("friendly_ghost"),
			EntityType.Builder.create(FriendlyGhostEntity::new, SpawnGroup.AMBIENT)
					.dimensions(0.7f, 1.9f).maxTrackingRange(10).build("friendly_ghost"));

	public static final EntityType<GhostJailerEntity> GHOST_JAILER = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("ghost_jailer"),
			EntityType.Builder.create(GhostJailerEntity::new, SpawnGroup.MONSTER)
					.dimensions(1.3f, 3.4f).maxTrackingRange(12).makeFireImmune().build("ghost_jailer"));

	private ModEntities() {}

	public static void init() {
		FabricDefaultAttributeRegistry.register(HOSTILE_GHOST, HostileGhostEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(FRIENDLY_GHOST, FriendlyGhostEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GHOST_JAILER, GhostJailerEntity.createAttributes());
	}
}
