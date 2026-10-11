package com.phantompowers;

import com.phantompowers.network.BeamPayload;
import com.phantompowers.network.InvisiblePayload;
import com.phantompowers.network.GhostSensePayload;
import com.phantompowers.network.RingPayload;
import com.phantompowers.network.SkillPayload;
import com.phantompowers.network.SyncPayload;
import com.phantompowers.world.FloatingDoorFeature;
import com.phantompowers.world.GhostRooms;
import com.phantompowers.world.GhostZoneLife;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

public class PhantomMod implements ModInitializer {
	public static final String MOD_ID = "phantom";

	public static final RegistryKey<DamageType> GHOST_PORTAL_DAMAGE =
			RegistryKey.of(RegistryKeys.DAMAGE_TYPE, id("ghost_portal"));

	public static final Feature<DefaultFeatureConfig> FLOATING_DOOR =
			new FloatingDoorFeature(DefaultFeatureConfig.CODEC);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModSounds.init();
		ModBlocks.init();
		ModEntities.init();
		ModItems.init();
		Registry.register(Registries.FEATURE, id("floating_door"), FLOATING_DOOR);

		// Networking
		PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BeamPayload.ID, BeamPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(InvisiblePayload.ID, InvisiblePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RingPayload.ID, RingPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(GhostSensePayload.ID, GhostSensePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SkillPayload.ID, SkillPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SkillPayload.ID,
				(payload, context) -> GhostPowers.handleAction(context.player(), payload.action()));

		// Pressing a button inside a Ghost Portal frame
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			BlockState clicked = world.getBlockState(hit.getBlockPos());
			// Doors in the Ghost Zone lead to their own rooms
			if (clicked.getBlock() instanceof DoorBlock && !player.isSneaking() && GhostRooms.isDoorWorld(world)) {
				if (!world.isClient && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity sp) {
					GhostRooms.queueUse(sp, (ServerWorld) world, hit.getBlockPos(), clicked);
				}
				return ActionResult.SUCCESS;
			}
			if (!world.isClient && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity sp) {
				BlockState state = clicked;
				if (state.getBlock() instanceof ButtonBlock && !state.get(ButtonBlock.POWERED)) {
					PortalLogic.onButtonPressed(sp, (ServerWorld) world, hit.getBlockPos());
				}
			}
			return ActionResult.PASS;
		});

		// Ticking
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			PortalLogic.tick(server);
			GhostPowers.tick(server);
			ThermosItem.tick();
			GhostSense.tick(server);
			GhostZoneLife.tick(server);
		});

		// Intangible ghosts can't be hurt; possessed hosts can't be hit by their possessor
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			return GhostPowers.allowDamage(entity, source);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayerEntity p) GhostPowers.onDeath(p);
			GhostPowers.onAnyDeath(entity);
		});

		// Hits by players who ate ghost essences
		net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (!world.isClient && player instanceof ServerPlayerEntity sp && entity instanceof net.minecraft.entity.LivingEntity le) {
				GhostPowers.onPlayerHit(sp, le);
			}
			return ActionResult.PASS;
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
				GhostPowers.onRespawn(newPlayer));

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				GhostPowers.onJoin(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				GhostPowers.onLeave(handler.player));

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> GhostPowers.onServerStopping());

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				PhantomCommands.register(dispatcher));
	}
}
