package com.phantompowers;

import com.phantompowers.entity.CrateKingEntity;
import com.phantompowers.entity.CrateProjectileEntity;
import com.phantompowers.entity.FriendlyGhostEntity;
import com.phantompowers.entity.GliderEntity;
import com.phantompowers.entity.VoltageEntity;
import com.phantompowers.entity.WishkeeperEntity;
import com.phantompowers.entity.RoomGhostEntity;
import com.phantompowers.entity.GhostJailerEntity;
import com.phantompowers.entity.HostileGhostEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
	public static final EntityType<com.phantompowers.entity.MissileEntity> MISSILE = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("wrist_missile"),
			EntityType.Builder.<com.phantompowers.entity.MissileEntity>create(com.phantompowers.entity.MissileEntity::new, SpawnGroup.MISC)
					.dimensions(0.3f, 0.3f).maxTrackingRange(8).trackingTickInterval(1).build("wrist_missile"));

	public static final EntityType<com.phantompowers.entity.EctoConstructEntity> ECTO_CONSTRUCT = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("ecto_construct"),
			EntityType.Builder.<com.phantompowers.entity.EctoConstructEntity>create(com.phantompowers.entity.EctoConstructEntity::new, SpawnGroup.MISC)
					.dimensions(1.0f, 1.0f).maxTrackingRange(10).trackingTickInterval(1).makeFireImmune().build("ecto_construct"));

	public static final EntityType<com.phantompowers.entity.GhostCloneEntity> GHOST_CLONE = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("ghost_clone"),
			EntityType.Builder.create(com.phantompowers.entity.GhostCloneEntity::new, SpawnGroup.MISC)
					.dimensions(0.6f, 1.8f).maxTrackingRange(10).trackingTickInterval(2).build("ghost_clone"));

	public static final EntityType<com.phantompowers.entity.GhostBikeEntity> GHOST_BIKE = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("ghost_bike"),
			EntityType.Builder.<com.phantompowers.entity.GhostBikeEntity>create(com.phantompowers.entity.GhostBikeEntity::new, SpawnGroup.MISC)
					.dimensions(1.1f, 1.2f).maxTrackingRange(10).build("ghost_bike"));

	public static final EntityType<com.phantompowers.entity.RevEntity> REV = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("rev"),
			EntityType.Builder.create(com.phantompowers.entity.RevEntity::new, SpawnGroup.MONSTER)
					.dimensions(1.7f, 2.3f).maxTrackingRange(12).trackingTickInterval(1).makeFireImmune().build("rev"));

	public static final EntityType<com.phantompowers.entity.RoxyEntity> ROXY = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("roxy"),
			EntityType.Builder.create(com.phantompowers.entity.RoxyEntity::new, SpawnGroup.MONSTER)
					.dimensions(0.7f, 2.0f).maxTrackingRange(12).makeFireImmune().build("roxy"));

	public static final EntityType<com.phantompowers.entity.ShadeEntity> SHADE = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("shade"),
			EntityType.Builder.create(com.phantompowers.entity.ShadeEntity::new, SpawnGroup.MONSTER)
					.dimensions(1.0f, 2.8f).maxTrackingRange(10).makeFireImmune().build("shade"));

	public static final EntityType<com.phantompowers.entity.KissEntity> KISS = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("kiss"),
			EntityType.Builder.<com.phantompowers.entity.KissEntity>create(com.phantompowers.entity.KissEntity::new, SpawnGroup.MISC)
					.dimensions(0.4f, 0.4f).maxTrackingRange(8).trackingTickInterval(2).build("kiss"));

	public static final EntityType<com.phantompowers.entity.PinkOrbEntity> PINK_ORB = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("pink_orb"),
			EntityType.Builder.<com.phantompowers.entity.PinkOrbEntity>create(com.phantompowers.entity.PinkOrbEntity::new, SpawnGroup.MISC)
					.dimensions(0.5f, 0.5f).maxTrackingRange(8).trackingTickInterval(2).build("pink_orb"));

	public static final EntityType<com.phantompowers.entity.GhostNetProjectile> GHOST_NET_PROJECTILE = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("ghost_net_projectile"),
			EntityType.Builder.<com.phantompowers.entity.GhostNetProjectile>create(com.phantompowers.entity.GhostNetProjectile::new, SpawnGroup.MISC)
					.dimensions(0.4f, 0.4f).maxTrackingRange(8).trackingTickInterval(2).build("ghost_net_projectile"));

	public static final EntityType<com.phantompowers.entity.GhostNetTrapEntity> GHOST_NET_TRAP = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("ghost_net_trap"),
			EntityType.Builder.<com.phantompowers.entity.GhostNetTrapEntity>create(com.phantompowers.entity.GhostNetTrapEntity::new, SpawnGroup.MISC)
					.dimensions(0.5f, 0.5f).maxTrackingRange(10).trackingTickInterval(1).makeFireImmune().build("ghost_net_trap"));

	public static final EntityType<com.phantompowers.entity.GhoulEntity> GHOUL = Registry.register(Registries.ENTITY_TYPE, PhantomMod.id("ghoul"),
			EntityType.Builder.create(com.phantompowers.entity.GhoulEntity::new, SpawnGroup.MONSTER).dimensions(0.7f, 2.2f).maxTrackingRange(10).build("ghoul"));
	public static final EntityType<com.phantompowers.entity.SpecterKnightEntity> SPECTER_KNIGHT = Registry.register(Registries.ENTITY_TYPE, PhantomMod.id("specter_knight"),
			EntityType.Builder.create(com.phantompowers.entity.SpecterKnightEntity::new, SpawnGroup.MONSTER).dimensions(0.7f, 2.1f).maxTrackingRange(10).build("specter_knight"));
	public static final EntityType<com.phantompowers.entity.LostSoulEntity> LOST_SOUL = Registry.register(Registries.ENTITY_TYPE, PhantomMod.id("lost_soul"),
			EntityType.Builder.create(com.phantompowers.entity.LostSoulEntity::new, SpawnGroup.CREATURE).dimensions(0.6f, 2.0f).maxTrackingRange(10).build("lost_soul"));
	public static final EntityType<com.phantompowers.entity.GhostYetiEntity> GHOST_YETI = Registry.register(Registries.ENTITY_TYPE, PhantomMod.id("ghost_yeti"),
			EntityType.Builder.create(com.phantompowers.entity.GhostYetiEntity::new, SpawnGroup.CREATURE).dimensions(1.4f, 2.4f).maxTrackingRange(10).build("ghost_yeti"));

	public static final EntityType<com.phantompowers.entity.PlayerCloneEntity> PLAYER_CLONE = Registry.register(Registries.ENTITY_TYPE, PhantomMod.id("player_clone"),
			EntityType.Builder.create(com.phantompowers.entity.PlayerCloneEntity::new, SpawnGroup.MISC).dimensions(0.6f, 1.8f).maxTrackingRange(10).build("player_clone"));

	public static final EntityType<com.phantompowers.entity.DoloraEntity> DOLORA = Registry.register(Registries.ENTITY_TYPE, PhantomMod.id("dolora"),
			EntityType.Builder.create(com.phantompowers.entity.DoloraEntity::new, SpawnGroup.MONSTER).dimensions(0.7f, 2.0f).maxTrackingRange(12).makeFireImmune().build("dolora"));

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
					.dimensions(1.45f, 3.8f).maxTrackingRange(12).makeFireImmune().build("ghost_jailer"));

	public static final EntityType<GliderEntity> GLIDER = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("hover_glider"),
			EntityType.Builder.<GliderEntity>create(GliderEntity::new, SpawnGroup.MISC)
					.dimensions(1.6f, 0.3f).maxTrackingRange(10).build("hover_glider"));

	public static final EntityType<com.phantompowers.entity.SkimmerEntity> SKIMMER = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("ecto_skimmer"),
			EntityType.Builder.<com.phantompowers.entity.SkimmerEntity>create(com.phantompowers.entity.SkimmerEntity::new, SpawnGroup.MISC)
					.dimensions(2.6f, 2.0f).maxTrackingRange(12).build("ecto_skimmer"));

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

	public static final EntityType<WishkeeperEntity> WISHKEEPER = Registry.register(Registries.ENTITY_TYPE,
			PhantomMod.id("wishkeeper"),
			EntityType.Builder.create(WishkeeperEntity::new, SpawnGroup.MONSTER)
					.dimensions(1.1f, 3.3f).maxTrackingRange(12).makeFireImmune().build("wishkeeper"));

	private ModEntities() {}

	public static void init() {
		FabricDefaultAttributeRegistry.register(HOSTILE_GHOST, HostileGhostEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(PLAYER_CLONE, com.phantompowers.entity.PlayerCloneEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(DOLORA, com.phantompowers.entity.DoloraEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GHOUL, com.phantompowers.entity.GhoulEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SPECTER_KNIGHT, com.phantompowers.entity.SpecterKnightEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(LOST_SOUL, com.phantompowers.entity.LostSoulEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GHOST_YETI, com.phantompowers.entity.GhostYetiEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(FRIENDLY_GHOST, FriendlyGhostEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GHOST_JAILER, GhostJailerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CRATE_KING, CrateKingEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(REV, com.phantompowers.entity.RevEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GHOST_CLONE, com.phantompowers.entity.GhostCloneEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ROXY, com.phantompowers.entity.RoxyEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SHADE, com.phantompowers.entity.ShadeEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(VOLTAGE, VoltageEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ROOM_GHOST, RoomGhostEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(WISHKEEPER, WishkeeperEntity.createAttributes());
	}
}
