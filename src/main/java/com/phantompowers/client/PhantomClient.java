package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.GhostPowers;
import com.phantompowers.ModBlocks;
import com.phantompowers.PhantomMod;
import com.phantompowers.network.BeamPayload;
import com.phantompowers.network.InvisiblePayload;
import com.phantompowers.ModEntities;
import com.phantompowers.entity.GliderEntity;
import com.phantompowers.network.GhostSensePayload;
import com.phantompowers.network.RingPayload;
import com.phantompowers.network.SkillPayload;
import com.phantompowers.network.SyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PhantomClient implements ClientModInitializer {
	public static final String CATEGORY = "key.category.phantom";

	public static KeyBinding TRANSFORM;
	public static KeyBinding BLAST;
	public static KeyBinding INTANGIBLE;
	public static KeyBinding POSSESS;
	public static KeyBinding WAIL;
	public static KeyBinding TORNADO;
	public static KeyBinding INVISIBLE;
	public static KeyBinding SKILLS;
	public static KeyBinding SUIT;
	public static KeyBinding FROST;
	public static KeyBinding FISTS;
	public static KeyBinding SHIELD;

	/** Key bound to each skill, in skill order (used by the HUD and skills screen). */
	public static KeyBinding[] skillKeys() {
		return new KeyBinding[]{BLAST, INTANGIBLE, POSSESS, WAIL, TORNADO, INVISIBLE, FROST, FISTS, SHIELD};
	}

	private static boolean intangibleHeld;
	private static int transformHeld;
	private static int suitHeld;
	private static int fireDelay;
	private static boolean wailHeld;
	private static boolean frostHeld;
	private static boolean jumpHeld;
	private static boolean shieldHeld;
	private static final int HOLD_TICKS = 10;

	@Override
	public void onInitializeClient() {
		TRANSFORM = key("transform", GLFW.GLFW_KEY_V);
		BLAST = key("blast", GLFW.GLFW_KEY_R);
		INTANGIBLE = key("intangible", GLFW.GLFW_KEY_G);
		POSSESS = key("possess", GLFW.GLFW_KEY_H);
		WAIL = key("wail", GLFW.GLFW_KEY_B);
		TORNADO = key("tornado", GLFW.GLFW_KEY_N);
		INVISIBLE = key("invisible", GLFW.GLFW_KEY_J);
		SKILLS = key("skills", GLFW.GLFW_KEY_K);
		SUIT = key("suit", GLFW.GLFW_KEY_U);
		FROST = key("frost", GLFW.GLFW_KEY_Z);
		FISTS = key("fists", GLFW.GLFW_KEY_X);
		SHIELD = key("shield", GLFW.GLFW_KEY_Y);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientBeams.tick(client);
			ClientRings.tick();
			LookTransition.tick();
			ClientTornado.tick(client);
			ClientShield.tick(client);
			GhostTrail.tick(client);
			FistGlowFeature.tick(client);
			ClientShockwaves.tick();
			ClientEntityFx.tick();
			BlasterScope.tick(client);
			GhostTail.tick(client);
			SupermanFlight.tick(client);
			ClientGhostSense.tick(client);
			ClientCutscene.tick(client);
			if (client.player == null) return;

			// V: tap = quick change, hold = transformation cutscene
			while (TRANSFORM.wasPressed()) { }
			if (TRANSFORM.isPressed()) {
				transformHeld++;
				if (transformHeld == HOLD_TICKS) send(GhostPowers.A_TRANSFORM_CUTSCENE);
			} else {
				if (transformHeld > 0 && transformHeld < HOLD_TICKS) send(GhostPowers.A_TRANSFORM);
				transformHeld = 0;
			}
			while (BLAST.wasPressed()) send(GhostPowers.A_BLAST);
			while (INTANGIBLE.wasPressed()) send(GhostPowers.A_INTANGIBLE);
			while (POSSESS.wasPressed()) send(GhostPowers.A_POSSESS);
			while (WAIL.wasPressed()) send(GhostPowers.A_WAIL);
			// Spamming jump fights a Thermos pulling you in
			boolean jumpNow = client.options.jumpKey.isPressed();
			if (jumpNow && !jumpHeld && ClientGhost.powers && client.currentScreen == null) send(GhostPowers.A_STRUGGLE);
			jumpHeld = jumpNow;
			// Z: hold to keep breathing frost
			while (FROST.wasPressed()) send(GhostPowers.A_FROST);
			boolean frostNow = FROST.isPressed();
			if (frostHeld && !frostNow) send(GhostPowers.A_FROST_RELEASE);
			frostHeld = frostNow;
			boolean wailNow = WAIL.isPressed();
			if (wailHeld && !wailNow) send(GhostPowers.A_WAIL_RELEASE);
			wailHeld = wailNow;

			// U: tap = suit on/off, hold = suit-up cutscene
			while (SUIT.wasPressed()) { }
			if (SUIT.isPressed()) {
				suitHeld++;
				if (suitHeld == HOLD_TICKS) send(GhostPowers.A_SUIT_CUTSCENE);
			} else {
				if (suitHeld > 0 && suitHeld < HOLD_TICKS) send(GhostPowers.A_SUIT);
				suitHeld = 0;
			}

			// Rev's ghost bike: sprint = nitro, jump = hop
			boolean onBike = client.player.getVehicle() instanceof com.phantompowers.entity.GhostBikeEntity;
			com.phantompowers.entity.GhostBikeEntity.clientNitro = onBike && client.options.sprintKey.isPressed();
			com.phantompowers.entity.GhostBikeEntity.clientHop = onBike && client.options.jumpKey.isPressed();
			// Sprint on the Hover Glider = afterburners
			GliderEntity.clientBoost = client.options.sprintKey.isPressed() && client.player.getVehicle() instanceof GliderEntity;
			// Right-click on the Hover Glider fires its lasers
			if (client.player.getVehicle() instanceof GliderEntity && client.options.useKey.isPressed() && client.currentScreen == null) {
				if (fireDelay <= 0) {
					send(GhostPowers.A_GLIDER_FIRE);
					fireDelay = 6;
				}
			}
			if (fireDelay > 0) fireDelay--;
			while (TORNADO.wasPressed()) send(GhostPowers.A_TORNADO);
			while (INVISIBLE.wasPressed()) send(GhostPowers.A_INVISIBLE);
			while (FISTS.wasPressed()) send(GhostPowers.A_FISTS);
			// Y: hold to keep the Ecto Shield up
			while (SHIELD.wasPressed()) send(GhostPowers.A_SHIELD);
			boolean shieldNow = SHIELD.isPressed();
			if (shieldHeld && !shieldNow) send(GhostPowers.A_SHIELD_RELEASE);
			shieldHeld = shieldNow;
			while (SKILLS.wasPressed()) client.setScreen(new GhostSkillsScreen(null));

			// Letting go of G lets go of whatever you were holding intangible
			boolean held = INTANGIBLE.isPressed();
			if (intangibleHeld && !held) send(GhostPowers.A_INTANGIBLE_RELEASE);
			intangibleHeld = held;
		});

		ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) -> ClientGhost.update(payload));
		ClientPlayNetworking.registerGlobalReceiver(BeamPayload.ID, (payload, context) -> ClientBeams.add(payload));
		ClientPlayNetworking.registerGlobalReceiver(InvisiblePayload.ID, (payload, context) -> {
			GhostFlags.CLIENT_INVISIBLE.clear();
			GhostFlags.CLIENT_INVISIBLE.addAll(payload.players());
			GhostFlags.CLIENT_GHOSTS.clear();
			GhostFlags.CLIENT_GHOSTS.addAll(payload.ghosts());
			GhostFlags.CLIENT_SUITED.clear();
			GhostFlags.CLIENT_SUITED.addAll(payload.suited());
			GhostFlags.CLIENT_FLYING.clear();
			GhostFlags.CLIENT_FLYING.addAll(payload.flying());
			GhostFlags.CLIENT_FISTS.clear();
			GhostFlags.CLIENT_FISTS.addAll(payload.fists());
			GhostFlags.CLIENT_SHIELDED.clear();
			GhostFlags.CLIENT_SHIELDED.addAll(payload.shielded());
			GhostFlags.CLIENT_SUIT_VARIANT.clear();
			for (int i = 0; i < payload.variantIds().size(); i++) GhostFlags.CLIENT_SUIT_VARIANT.put(payload.variantIds().get(i), payload.variants().get(i));
		});
		ClientPlayNetworking.registerGlobalReceiver(RingPayload.ID, (payload, context) -> ClientRings.add(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.phantompowers.network.ShieldHitPayload.ID, (payload, context) -> ClientShield.hit(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.phantompowers.network.TornadoPayload.ID, (payload, context) -> ClientTornado.update(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.phantompowers.network.EntityFxPayload.ID, (payload, context) -> ClientEntityFx.update(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.phantompowers.network.ShockwavePayload.ID, (payload, context) -> ClientShockwaves.add(payload));
		ClientPlayNetworking.registerGlobalReceiver(GhostSensePayload.ID, (payload, context) -> ClientGhostSense.add(payload));

		// Ghost mobs
		EntityModelLayerRegistry.registerModelLayer(GhostModel.LAYER, GhostModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.HOSTILE_GHOST,
				ctx -> new GhostRenderer<>(ctx, PhantomMod.id("textures/entity/hostile_ghost.png"), 1.0f));
		EntityRendererRegistry.register(ModEntities.FRIENDLY_GHOST,
				ctx -> new GhostRenderer<>(ctx, PhantomMod.id("textures/entity/friendly_ghost.png"), 0.9f));
		EntityModelLayerRegistry.registerModelLayer(GliderModel.LAYER, GliderModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.GLIDER, GliderRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(CrateKingModel.LAYER, CrateKingModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.CRATE_KING, CrateKingRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(VoltageModel.LAYER, VoltageModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.VOLTAGE, VoltageRenderer::new);
		EntityRendererRegistry.register(ModEntities.ROOM_GHOST, RoomGhostRenderer::new);
		EntityRendererRegistry.register(ModEntities.GHOST_NET_PROJECTILE, GhostNetRenderer.Thrown::new);
		EntityModelLayerRegistry.registerModelLayer(RevModel.LAYER, RevModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.REV, RevRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(RoxyModel.LAYER, RoxyModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.ROXY, RoxyRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(ShadeModel.LAYER, ShadeModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.SHADE, ShadeRenderer::new);
		EntityRendererRegistry.register(ModEntities.KISS, EnergyProjectileRenderer.Kiss::new);
		EntityRendererRegistry.register(ModEntities.GHOST_BIKE, GhostBikeRenderer::new);
		EntityRendererRegistry.register(ModEntities.PINK_ORB, EnergyProjectileRenderer.Orb::new);
		EntityRendererRegistry.register(ModEntities.GHOST_NET_TRAP, GhostNetRenderer.Trap::new);
		EntityRendererRegistry.register(ModEntities.CRATE_PROJECTILE, ctx -> new FlyingItemEntityRenderer<>(ctx, 1.6f, false));
		EntityModelLayerRegistry.registerModelLayer(BackpackFeature.LAYER, BackpackFeature::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(GhostTail.LAYER, GhostTail::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(FistGlowFeature.WIDE, () -> FistGlowFeature.layer(false));
		EntityModelLayerRegistry.registerModelLayer(FistGlowFeature.SLIM, () -> FistGlowFeature.layer(true));
		EntityModelLayerRegistry.registerModelLayer(GhostSuitFeature.WIDE, () -> GhostSuitFeature.layer(false));
		EntityModelLayerRegistry.registerModelLayer(GhostSuitFeature.SLIM, () -> GhostSuitFeature.layer(true));
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
			if (renderer instanceof PlayerEntityRenderer playerRenderer) {
				helper.register(new BackpackFeature(playerRenderer, ctx.getPart(BackpackFeature.LAYER)));
				helper.register(new GuitarFeature(playerRenderer));
				helper.register(new GhostTail(playerRenderer, ctx.getPart(GhostTail.LAYER)));
				helper.register(new TransitionFeature(playerRenderer));
				helper.register(new FistGlowFeature(playerRenderer, ctx.getPart(FistGlowFeature.WIDE), ctx.getPart(FistGlowFeature.SLIM)));
				helper.register(new GhostSuitFeature(playerRenderer,
						ctx.getPart(GhostSuitFeature.WIDE), ctx.getPart(GhostSuitFeature.SLIM)));
			}
		});
		EntityModelLayerRegistry.registerModelLayer(JailerModel.LAYER, JailerModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.GHOST_JAILER, JailerRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(WishkeeperModel.LAYER, WishkeeperModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.WISHKEEPER, WishkeeperRenderer::new);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientGhost.reset();
			GhostFlags.clientSelfIntangible = false;
			GhostFlags.CLIENT_INVISIBLE.clear();
			GhostFlags.CLIENT_GHOSTS.clear();
			GhostFlags.CLIENT_SUITED.clear();
			GhostFlags.CLIENT_FLYING.clear();
			ClientBeams.clear();
			ClientRings.clear();
			LookTransition.clear();
			ClientTornado.clear();
			GhostFlags.CLIENT_FISTS.clear();
			GhostFlags.CLIENT_SHIELDED.clear();
			GhostFlags.CLIENT_SUIT_VARIANT.clear();
			GhostTrail.clear();
			ClientShockwaves.clear();
			ClientEntityFx.clear();
			ClientGhostSense.clear();
			ClientCutscene.reset(client);
		});

		// "Ghost Skills" tab on the inventory screen
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof InventoryScreen && ClientGhost.powers) {
				int left = (width - 176) / 2;
				int top = (height - 166) / 2;
				Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Ghost Skills"),
								b -> client.setScreen(new GhostSkillsScreen(screen)))
						.dimensions(left + 176 - 78, top - 22, 78, 20)
						.build());
			}
		});

		HudRenderCallback.EVENT.register(GhostHud::render);
		WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
			ClientBeams.render(ctx);
			ClientRings.render(ctx);
			ClientTornado.render(ctx);
			ClientShield.render(ctx);
			GhostTrail.render(ctx);
			ClientGhostSense.render(ctx);
			ClientShockwaves.render(ctx);
			ClientEntityFx.render(ctx);
		});
		HudRenderCallback.EVENT.register(ClientCutscene::renderHud);
		HudRenderCallback.EVENT.register(ClientGhostSense::renderHud);
		HudRenderCallback.EVENT.register(ControlsHud::render);
		HudRenderCallback.EVENT.register(SuitTrackerHud::render);
		HudRenderCallback.EVENT.register(BlasterScope::render);
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GHOST_PORTAL, RenderLayer.getTranslucent());
		DimensionRenderingRegistry.registerDimensionEffects(PhantomMod.id("ghost_zone"), new GhostZoneEffects());
	}

	private static KeyBinding key(String name, int code) {
		return KeyBindingHelper.registerKeyBinding(
				new KeyBinding("key.phantom." + name, InputUtil.Type.KEYSYM, code, CATEGORY));
	}

	public static void send(int action) {
		if (ClientPlayNetworking.canSend(SkillPayload.ID)) {
			ClientPlayNetworking.send(new SkillPayload(action));
		}
	}

	public static String keyName(KeyBinding key) {
		return key.getBoundKeyLocalizedText().getString().toUpperCase();
	}
}
