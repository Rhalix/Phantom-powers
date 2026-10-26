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
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
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
		com.phantompowers.world.GhostZoneFeatures.init();

		// Networking
		PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BeamPayload.ID, BeamPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(InvisiblePayload.ID, InvisiblePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RingPayload.ID, RingPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(GhostSensePayload.ID, GhostSensePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.phantompowers.network.EntityFxPayload.ID, com.phantompowers.network.EntityFxPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.phantompowers.network.ShockwavePayload.ID, com.phantompowers.network.ShockwavePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.phantompowers.network.TornadoPayload.ID, com.phantompowers.network.TornadoPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.phantompowers.network.ShieldHitPayload.ID, com.phantompowers.network.ShieldHitPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.phantompowers.network.DomePayload.ID, com.phantompowers.network.DomePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.phantompowers.network.DomeHitPayload.ID, com.phantompowers.network.DomeHitPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.phantompowers.network.SkimmerRadarPayload.ID, com.phantompowers.network.SkimmerRadarPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.phantompowers.network.SparkPayload.ID, com.phantompowers.network.SparkPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.phantompowers.network.StoryPayload.ID, com.phantompowers.network.StoryPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(com.phantompowers.network.StoryChoicePayload.ID, com.phantompowers.network.StoryChoicePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(com.phantompowers.network.StoryChoicePayload.ID,
				(payload, context) -> context.server().execute(() -> Story.choose(context.player(), payload.path())));
		PayloadTypeRegistry.playS2C().register(com.phantompowers.network.SlotsPayload.ID, com.phantompowers.network.SlotsPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(com.phantompowers.network.PartyPayload.ID, com.phantompowers.network.PartyPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(com.phantompowers.network.MenuActionPayload.ID, com.phantompowers.network.MenuActionPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(com.phantompowers.network.MenuActionPayload.ID,
				(payload, context) -> context.server().execute(() -> menuAction(context.player(), payload)));
		PayloadTypeRegistry.playC2S().register(SkillPayload.ID, SkillPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(com.phantompowers.network.SuitColorsPayload.ID, com.phantompowers.network.SuitColorsPayload.CODEC);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.phantompowers.network.SuitColorsPayload.ID,
				(payload, context) -> context.server().execute(() -> GhostPowers.setSuitColors(context.player(), payload.colors())));
		PayloadTypeRegistry.playC2S().register(com.phantompowers.network.WorkbenchPayload.ID, com.phantompowers.network.WorkbenchPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(com.phantompowers.network.WorkbenchPayload.ID,
				(payload, context) -> context.server().execute(() -> HunterPaint.handle(context.player(), payload.pos(), payload.action(), payload.value())));
		PayloadTypeRegistry.playC2S().register(com.phantompowers.network.ClonePayload.ID, com.phantompowers.network.ClonePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(com.phantompowers.network.ClonePayload.ID,
				(payload, context) -> context.server().execute(() -> CloningPodBlock.start(context.player(), payload.pos(), payload.female())));
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

		// Punching the inside of a Thermos: fight your way out
		net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
			if (!world.isClient && player instanceof ServerPlayerEntity sp && world.getBlockState(pos).isOf(ModBlocks.THERMOS_WALL)) {
				ThermosPrison.onPunch(sp, pos);
			}
			return ActionResult.PASS;
		});

		// A possessed player is just a puppet: they can't use items (so no Thermos on the ghost inside them)
		net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!world.isClient && !GhostFlags.HOSTS.isEmpty() && GhostFlags.HOSTS.contains(player.getUuid())) {
				player.sendMessage(net.minecraft.text.Text.literal("You're possessed! You can't control your hands!").formatted(net.minecraft.util.Formatting.GREEN), true);
				return net.minecraft.util.TypedActionResult.fail(player.getStackInHand(hand));
			}
			return net.minecraft.util.TypedActionResult.pass(player.getStackInHand(hand));
		});

		// a half-built town doesn't carry over into a different world
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> com.phantompowers.world.AmityPark.reset());
		// Amity Park starts going up as soon as the world loads, so it's ready by the time you want it
		ServerLifecycleEvents.SERVER_STARTED.register(server -> com.phantompowers.world.AmityPark.request(server));

		// Ticking
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			PortalLogic.tick(server);
			GhostPowers.tick(server);
			ThermosItem.tick();
			GhostSense.tick(server);
			GhostZoneLife.tick(server);
			DarkPowers.tickTwisters();
			com.phantompowers.entity.BossMoves.tick(server);
			ThermosPrison.tick(server);
			CloningPodBlock.tick(server);
			ShieldDomes.tick(server);
			Story.tick(server);
			com.phantompowers.world.AmityPark.tick(server);
			com.phantompowers.world.TownRepair.tick(server);
		});

		// Amity Park grows back ten seconds after it gets damaged
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> com.phantompowers.world.TownRepair.beforeBreak(world, player, pos, state));

		// Intangible ghosts can't be hurt; possessed hosts can't be hit by their possessor
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			if (com.phantompowers.entity.SkimmerEntity.pilotProtected(entity, source)) return false;
			if (!Party.allowDamage(entity, source)) return false;
			return GhostPowers.allowDamage(entity, source);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayerEntity p) GhostPowers.onDeath(p);
			GhostPowers.onAnyDeath(entity);
			Story.onDeath(entity, source);
			HunterPaint.dropMaterials(entity, source);
		});

		// Hits by players who ate ghost essences
		net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (!world.isClient && player instanceof ServerPlayerEntity sp && entity instanceof net.minecraft.entity.LivingEntity le) {
				// Punch a frozen, badly hurt enemy and it shatters
				if (FrostLogic.tryShatter(sp, le)) return ActionResult.SUCCESS;
				GhostPowers.onPlayerHit(sp, le);
				GhostPowers.onFistPunch(sp, le);
			}
			return ActionResult.PASS;
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
				GhostPowers.onRespawn(newPlayer));

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			GhostPowers.onJoin(handler.player);
			server.execute(() -> {
				GhostData sd = GhostState.of(server).data(handler.player.getUuid());
				if (sd.storyPath != Story.NONE) Story.send(handler.player, sd, false);
				else handler.player.sendMessage(net.minecraft.text.Text.literal("Type /story to start Story Mode!").formatted(net.minecraft.util.Formatting.GREEN), false);
			});
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			GhostPowers.onLeave(handler.player);
			Party.onLeaveGame(handler.player);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			GhostPowers.onServerStopping();
			ShieldDomes.clearAll(server);
			com.phantompowers.entity.BossMoves.restoreAll();
			FrostLogic.clearAll();
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				PhantomCommands.register(dispatcher));
	}

	private static void menuAction(ServerPlayerEntity p, com.phantompowers.network.MenuActionPayload a) {
		switch (a.kind()) {
			case 0 -> Party.handle(p, a.value(), a.text());
			case 1 -> {
				if (a.value() == 0) Story.open(p);
				else if (a.value() == 1) Story.restart(p);
				else if (a.value() == 2) Story.fastTravel(p);
			}
			case 2 -> {
				GhostData d = GhostState.of(p.getServer()).data(p.getUuid());
				int slot = a.value() / 64, what = a.value() % 64;
				if ("hunter".equals(a.text())) Slots.assignGadget(p, d, slot, what);
				else Slots.assign(p, d, slot, what == 63 ? -1 : what);
			}
			default -> {}
		}
	}
}
