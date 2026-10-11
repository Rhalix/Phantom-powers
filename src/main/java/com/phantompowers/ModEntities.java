package com.phantompowers;

import com.phantompowers.entity.CrateKingEntity;
import com.phantompowers.entity.CrateProjectileEntity;
import com.phantompowers.entity.FriendlyGhostEntity;
import com.phantompowers.entity.GliderEntity;
import com.phantompowers.entity.VoltageEntity;
import com.phantompowers.entity.RoomGhostEntity;
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

	public static final EntityType<GliderEntity> GLIDER = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("hover_glider"),
			EntityType.Builder.<GliderEntity>create(GliderEntity::new, SpawnGroup.MISC)
					.dimensions(1.6f, 0.3f).maxTrackingRange(10).build("hover_glider"));

	public static final EntityType<CrateKingEntity> CRATE_KING = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("crate_king"),
			EntityType.Builder.create(CrateKingEntity::new, SpawnGroup.MONSTER)
					.dimensions(1.4f, 3.6f).maxTrackingRange(12).makeFireImmune().build("crate_king"));

	public static final EntityType<CrateProjectileEntity> CRATE_PROJECTILE = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("crate_projectile"),
			EntityType.Builder.<CrateProjectileEntity>create(CrateProjectileEntity::new, SpawnGroup.MISC)
					.dimensions(0.6f, 0.6f).maxTrackingRange(6).trackingTickInterval(10).build("crate_projectile"));

	public static final EntityType<VoltageEntity> VOLTAGE = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("voltage"),
			EntityType.Builder.create(VoltageEntity::new, SpawnGroup.MONSTER)
					.dimensions(0.9f, 2.9f).maxTrackingRange(12).makeFireImmune().build("voltage"));

	public static final EntityType<RoomGhostEntity> ROOM_GHOST = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("room_ghost"),
			EntityType.Builder.create(RoomGhostEntity::new, SpawnGroup.MONSTER)
					.dimensions(0.7f, 1.9f).maxTrackingRange(10).build("room_ghost"));

	private ModEntities() {}

	public static void init() {
		FabricDefaultAttributeRegistry.register(HOSTILE_GHOST, HostileGhostEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(FRIENDLY_GHOST, FriendlyGhostEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GHOST_JAILER, GhostJailerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CRATE_KING, CrateKingEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(VOLTAGE, VoltageEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ROOM_GHOST, RoomGhostEntity.createAttributes());
	}
}
