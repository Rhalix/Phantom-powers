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

	public static KeyBinding LOCK_ON;
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
	public static KeyBinding DUPLICATE;
	public static KeyBinding CONSTRUCT;
	public static KeyBinding SPIN;
	public static KeyBinding STINGER;
	public static KeyBinding ABSORB;
	public static KeyBinding RADAR;
	public static KeyBinding CANNON;
	public static final KeyBinding[] SLOT = new KeyBinding[4];
	public static KeyBinding MENU;
	private static final boolean[] slotHeld = new boolean[4];
	private static final int[] slotAction = {-1, -1, -1, -1};
	public static KeyBinding CANNON_MODE;

	private static final int[] PRESS = {GhostPowers.A_BLAST, GhostPowers.A_INTANGIBLE, GhostPowers.A_POSSESS, GhostPowers.A_WAIL, GhostPowers.A_TORNADO,
			GhostPowers.A_INVISIBLE, GhostPowers.A_FROST, GhostPowers.A_FISTS, GhostPowers.A_SHIELD, GhostPowers.A_DUPLICATE, GhostPowers.A_CONSTRUCT,
			GhostPowers.A_SPIN, GhostPowers.A_STINGER, GhostPowers.A_ABSORB};
	private static final int[] RELEASE = {GhostPowers.A_BLAST_RELEASE, GhostPowers.A_INTANGIBLE_RELEASE, -1, GhostPowers.A_WAIL_RELEASE, -1,
			-1, GhostPowers.A_FROST_RELEASE, -1, GhostPowers.A_SHIELD_RELEASE, -1, -1, -1, -1, -1};

	private static boolean conflictsFixed;

	/**
	 * Old per-power keys that sit on the same key as a Power Slot (R, G, H, B from before slots existed) are
	 * unbound once, so the slot gets the key. They can still be bound to something else in Controls.
	 */
	private static void fixConflicts(net.minecraft.client.MinecraftClient client) {
		if (conflictsFixed) return;
		conflictsFixed = true;
		boolean changed = false;
		KeyBinding[] old = {BLAST, INTANGIBLE, POSSESS, WAIL, TORNADO, INVISIBLE, FROST, FISTS, SHIELD, DUPLICATE, CONSTRUCT, SPIN, STINGER, ABSORB, CANNON, CANNON_MODE};
		for (KeyBinding k : old) {
			if (k.isUnbound()) continue;
			for (KeyBinding sl : SLOT) {
				if (sl.equals(k)) {
					k.setBoundKey(InputUtil.UNKNOWN_KEY);
					changed = true;
				}
			}
		}
		if (MENU.equals(STINGER) && !STINGER.isUnbound()) { STINGER.setBoundKey(InputUtil.UNKNOWN_KEY); changed = true; }
		if (changed) {
			KeyBinding.updateKeysByCode();
			client.options.write();
		}
	}

	/** A per-power key that's bound to the same key as a Power Slot is ignored (the slot wins). */
	private static boolean shadowed(KeyBinding k) {
		if (k.isUnbound()) return false;
		for (KeyBinding s : SLOT) if (s != null && s.equals(k)) return true;
		return false;
	}

	/** Key that triggers each skill, in skill order (used by the HUD and skills screen): its Power Slot, or its own key. */
	public static KeyBinding[] skillKeys() {
		KeyBinding[] own = {BLAST, INTANGIBLE, POSSESS, WAIL, TORNADO, INVISIBLE, FROST, FISTS, SHIELD, DUPLICATE, CONSTRUCT, SPIN, STINGER, ABSORB};
		KeyBinding[] out = own.clone();
		for (int i = 0; i < 4; i++) {
			int sk = ClientSlots.slots[i];
			if (sk >= 0 && sk < out.length) out[sk] = SLOT[i];
		}
		return out;
	}

	/** Key for a hunter gadget (its slot). */
	public static KeyBinding gadgetKey(int gadget) {
		for (int i = 0; i < 4; i++) if (ClientSlots.gadgets[i] == gadget) return SLOT[i];
		return SLOT[0];
	}

	private static boolean intangibleHeld;
	private static int transformHeld;
	private static int suitHeld;
	private static int fireDelay;
	private static boolean wailHeld;
	private static boolean frostHeld;
	private static boolean jumpHeld;
	private static boolean shieldHeld;
	private static boolean blastHeld;
	private static final int HOLD_TICKS = 10;

	@Override
	public void onInitializeClient() {
		TRANSFORM = key("transform", GLFW.GLFW_KEY_V);
		BLAST = key("blast", GLFW.GLFW_KEY_UNKNOWN);
		INTANGIBLE = key("intangible", GLFW.GLFW_KEY_UNKNOWN);
		POSSESS = key("possess", GLFW.GLFW_KEY_UNKNOWN);
		WAIL = key("wail", GLFW.GLFW_KEY_UNKNOWN);
		TORNADO = key("tornado", GLFW.GLFW_KEY_UNKNOWN);
		INVISIBLE = key("invisible", GLFW.GLFW_KEY_UNKNOWN);
		SKILLS = key("skills", GLFW.GLFW_KEY_K);
		SUIT = key("suit_up", GLFW.GLFW_KEY_B);
		FROST = key("frost", GLFW.GLFW_KEY_UNKNOWN);
		FISTS = key("fists", GLFW.GLFW_KEY_UNKNOWN);
		SHIELD = key("shield", GLFW.GLFW_KEY_UNKNOWN);
		DUPLICATE = key("duplicate", GLFW.GLFW_KEY_UNKNOWN);
		CONSTRUCT = key("construct", GLFW.GLFW_KEY_UNKNOWN);
		SPIN = key("spin", GLFW.GLFW_KEY_UNKNOWN);
		STINGER = key("stinger", GLFW.GLFW_KEY_UNKNOWN);
		ABSORB = key("absorb", GLFW.GLFW_KEY_UNKNOWN);
		RADAR = key("hunter_radar", GLFW.GLFW_KEY_N);
		CANNON = key("cannon", GLFW.GLFW_KEY_UNKNOWN);
		// the four power keys sit right around WASD so you hardly have to move your fingers
		SLOT[0] = key("power_1", GLFW.GLFW_KEY_R);
		SLOT[1] = key("power_2", GLFW.GLFW_KEY_C);
		SLOT[2] = key("power_3", GLFW.GLFW_KEY_X);
		SLOT[3] = key("power_4", GLFW.GLFW_KEY_Z);
		LOCK_ON = key("lock_on", GLFW.GLFW_KEY_G);
		MENU = key("menu", GLFW.GLFW_KEY_GRAVE_ACCENT);
		CANNON_MODE = key("cannon_mode", GLFW.GLFW_KEY_UNKNOWN);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientBeams.tick(client);
			ClientRings.tick();
			LookTransition.tick();
			ClientTornado.tick(client);
			ClientDomes.tick(client);
			ClientSparks.tick(client);
			StoryHud.tick();
			SkimmerHud.tick(client);
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
			WristGadgetFeature.tick(client);
			MissileSight.tick(client);
			if (client.player == null) return;
			fixConflicts(client);

			// V: tap = quick change, hold = transformation cutscene
			while (TRANSFORM.wasPressed()) { }
			if (TRANSFORM.isPressed()) {
				transformHeld++;
				if (transformHeld == HOLD_TICKS) send(GhostPowers.A_TRANSFORM_CUTSCENE);
			} else {
				if (transformHeld > 0 && transformHeld < HOLD_TICKS) send(GhostPowers.A_TRANSFORM);
				transformHeld = 0;
			}
			while (BLAST.wasPressed() && !shadowed(BLAST)) send(GhostPowers.A_BLAST);
			// R released: fire the wrist missiles you were aiming
			boolean blastNow = BLAST.isPressed() && !shadowed(BLAST);
			if (blastHeld && !blastNow) send(GhostPowers.A_BLAST_RELEASE);
			blastHeld = blastNow;
			while (INTANGIBLE.wasPressed() && !shadowed(INTANGIBLE)) send(GhostPowers.A_INTANGIBLE);
			while (POSSESS.wasPressed() && !shadowed(POSSESS)) send(GhostPowers.A_POSSESS);
			while (WAIL.wasPressed() && !shadowed(WAIL)) send(GhostPowers.A_WAIL);
			// Spamming jump fights a Thermos pulling you in
			boolean jumpNow = client.options.jumpKey.isPressed();
			if (jumpNow && !jumpHeld && ClientGhost.powers && client.currentScreen == null) send(GhostPowers.A_STRUGGLE);
			jumpHeld = jumpNow;
			// Z: hold to keep breathing frost
			while (FROST.wasPressed() && !shadowed(FROST)) send(GhostPowers.A_FROST);
			boolean frostNow = FROST.isPressed() && !shadowed(FROST);
			if (frostHeld && !frostNow) send(GhostPowers.A_FROST_RELEASE);
			frostHeld = frostNow;
			boolean wailNow = WAIL.isPressed() && !shadowed(WAIL);
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
			if (client.player.getVehicle() instanceof com.phantompowers.entity.SkimmerEntity && client.options.useKey.isPressed() && client.currentScreen == null) {
				if (fireDelay <= 0) {
					send(GhostPowers.A_SKIMMER_FIRE);
					fireDelay = 5;
				}
			}
			if (fireDelay > 0) fireDelay--;
			while (TORNADO.wasPressed() && !shadowed(TORNADO)) send(GhostPowers.A_TORNADO);
			while (INVISIBLE.wasPressed() && !shadowed(INVISIBLE)) send(GhostPowers.A_INVISIBLE);
			while (FISTS.wasPressed() && !shadowed(FISTS)) send(GhostPowers.A_FISTS);
			// Y: hold to keep the Ecto Shield up
			while (SHIELD.wasPressed() && !shadowed(SHIELD)) send(GhostPowers.A_SHIELD);
			while (DUPLICATE.wasPressed() && !shadowed(DUPLICATE)) send(GhostPowers.A_DUPLICATE);
			while (CONSTRUCT.wasPressed() && !shadowed(CONSTRUCT)) send(GhostPowers.A_CONSTRUCT);
			while (SPIN.wasPressed() && !shadowed(SPIN)) send(GhostPowers.A_SPIN);
			while (STINGER.wasPressed() && !shadowed(STINGER)) send(GhostPowers.A_STINGER);
			while (ABSORB.wasPressed() && !shadowed(ABSORB)) send(GhostPowers.A_ABSORB);
			while (RADAR.wasPressed()) RadarSettings.cycle();
			while (CANNON.wasPressed() && !shadowed(CANNON)) send(client.player.isSneaking() ? GhostPowers.A_CANNON_STOW : GhostPowers.A_CANNON);
			while (CANNON_MODE.wasPressed() && !shadowed(CANNON_MODE)) send(GhostPowers.A_CANNON_MODE);
			boolean shieldNow = SHIELD.isPressed() && !shadowed(SHIELD);
			if (shieldHeld && !shieldNow) send(GhostPowers.A_SHIELD_RELEASE);
			shieldHeld = shieldNow;
			while (SKILLS.wasPressed()) client.setScreen(new GhostSkillsScreen(null));
			while (LOCK_ON.wasPressed()) LockOn.toggle(client);
			LockOn.tick(client);

			// Letting go of G lets go of whatever you were holding intangible
			boolean held = INTANGIBLE.isPressed() && !shadowed(INTANGIBLE);
			if (intangibleHeld && !held) send(GhostPowers.A_INTANGIBLE_RELEASE);
			intangibleHeld = held;

			// Power Slots 1-4: the powers you put in them (or your suit gadgets while you're a suited-up hunter)
			while (MENU.wasPressed()) client.setScreen(new PhantomMenuScreen());
			boolean hunter = GhostFlags.CLIENT_SUITED.contains(client.player.getUuid()) && !GhostFlags.CLIENT_GHOSTS.contains(client.player.getUuid());
			for (int i = 0; i < 4; i++) {
				while (SLOT[i].wasPressed()) {
					if (hunter) {
						int g = ClientSlots.gadgets[i];
						slotAction[i] = g == com.phantompowers.Slots.G_MISSILES ? GhostPowers.A_BLAST_RELEASE : -1;
						if (g == com.phantompowers.Slots.G_MISSILES) send(GhostPowers.A_BLAST);
						else if (g == com.phantompowers.Slots.G_CANNONS) send(client.player.isSneaking() ? GhostPowers.A_CANNON_STOW : GhostPowers.A_CANNON);
						else if (g == com.phantompowers.Slots.G_CANNON_MODE) send(GhostPowers.A_CANNON_MODE);
						else if (g == com.phantompowers.Slots.G_RADAR) RadarSettings.cycle();
					} else {
						int skill = ClientSlots.slots[i];
						if (skill < 0) {
							client.player.sendMessage(net.minecraft.text.Text.literal("Power Slot " + (i + 1) + " is empty. Put a power in it from the Phantom Menu ("
									+ keyName(MENU) + ").").formatted(net.minecraft.util.Formatting.GRAY), true);
							continue;
						}
						send(PRESS[skill]);
						slotAction[i] = RELEASE[skill];
					}
				}
				boolean down = SLOT[i].isPressed();
				if (slotHeld[i] && !down && slotAction[i] >= 0) {
					send(slotAction[i]);
					slotAction[i] = -1;
				}
				slotHeld[i] = down;
			}
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
			GhostFlags.CLIENT_SUIT_COLORS.clear();
			for (int i = 0; i < payload.colorIds().size(); i++) GhostFlags.CLIENT_SUIT_COLORS.put(payload.colorIds().get(i), payload.colors().get(i));
			GhostFlags.CLIENT_DARK.clear();
			GhostFlags.CLIENT_DARK.addAll(payload.dark());
			GhostFlags.CLIENT_ACNE.clear();
			GhostFlags.CLIENT_ACNE.addAll(payload.acne());
			GhostFlags.CLIENT_CANNONS.clear();
			GhostFlags.CLIENT_CANNONS.addAll(payload.cannons());
			GhostFlags.CLIENT_AIMING.clear();
			GhostFlags.CLIENT_AIMING.addAll(payload.aiming());
		});
		ClientPlayNetworking.registerGlobalReceiver(RingPayload.ID, (payload, context) -> ClientRings.add(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.phantompowers.network.ShieldHitPayload.ID, (payload, context) -> ClientShield.hit(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.phantompowers.network.SkimmerRadarPayload.ID, (payload, context) -> SkimmerHud.update(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.phantompowers.network.SparkPayload.ID, (payload, context) -> ClientSparks.handle(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.phantompowers.network.StoryPayload.ID, (payload, context) -> StoryHud.update(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.phantompowers.network.SlotsPayload.ID, (payload, context) -> ClientSlots.update(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.phantompowers.network.PartyPayload.ID, (payload, context) -> ClientParty.update(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.phantompowers.network.DomePayload.ID, (payload, context) -> ClientDomes.update(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.phantompowers.network.DomeHitPayload.ID, (payload, context) -> ClientDomes.hit(payload));
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
		EntityModelLayerRegistry.registerModelLayer(SkimmerModel.LAYER, SkimmerModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.SKIMMER, SkimmerRenderer::new);
		EntityRendererRegistry.register(ModEntities.SEAT, net.minecraft.client.render.entity.EmptyEntityRenderer::new);
		com.phantompowers.entity.SkimmerEntity.sendAction = PhantomClient::send;
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
		EntityRendererRegistry.register(ModEntities.GHOST_CLONE, GhostCloneRenderer::new);
		EntityRendererRegistry.register(ModEntities.PINK_ORB, EnergyProjectileRenderer.Orb::new);
		EntityRendererRegistry.register(ModEntities.GHOST_NET_TRAP, GhostNetRenderer.Trap::new);
		EntityRendererRegistry.register(ModEntities.MISSILE, MissileRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(DoloraModel.COUNSELOR, DoloraModel::counselorModel);
		EntityModelLayerRegistry.registerModelLayer(DoloraModel.MISERY, DoloraModel::miseryModel);
		EntityModelLayerRegistry.registerModelLayer(DoloraModel.QUEEN, DoloraModel::queenModel);
		EntityRendererRegistry.register(ModEntities.DOLORA, DoloraRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(PlayerCloneRenderer.HAIR_F, PlayerCloneRenderer::hairFemale);
		EntityModelLayerRegistry.registerModelLayer(PlayerCloneRenderer.HAIR_M, PlayerCloneRenderer::hairMale);
		EntityModelLayerRegistry.registerModelLayer(PlayerCloneRenderer.GEAR_F, PlayerCloneRenderer::gearFemale);
		EntityModelLayerRegistry.registerModelLayer(PlayerCloneRenderer.GEAR_M, PlayerCloneRenderer::gearMale);
		EntityModelLayerRegistry.registerModelLayer(PlayerCloneRenderer.HUMAN_F, PlayerCloneRenderer::humanFemale);
		EntityModelLayerRegistry.registerModelLayer(PlayerCloneRenderer.HUMAN_M, PlayerCloneRenderer::humanMale);
		EntityRendererRegistry.register(ModEntities.PLAYER_CLONE, PlayerCloneRenderer::new);
		com.phantompowers.HunterWorkbenchBlock.openScreen = pos -> net.minecraft.client.MinecraftClient.getInstance().setScreen(new WorkbenchScreen(pos));
		PaintTextures.init();
		net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(com.phantompowers.ModItems.ECTO_SWORD, new EctoSwordRenderer());
		// painted hunter gear: the paintable parts of each model are tinted with the item's paint colour
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register((stack, tint) -> tint != 0 ? -1
				: 0xFF000000 | (com.phantompowers.HunterPaint.paint(stack) >= 0 ? com.phantompowers.HunterPaint.paint(stack)
				: stack.isOf(com.phantompowers.ModItems.ECTO_LAUNCHER) ? 0x9CA4B6 : stack.isOf(com.phantompowers.ModItems.THERMOS) ? 0xDFE6F0 : 0xE2E8F2),
				com.phantompowers.ModItems.GHOST_BLASTER, com.phantompowers.ModItems.ECTO_LAUNCHER, com.phantompowers.ModItems.THERMOS);
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register((stack, tint) -> tint != 1 ? -1
				: 0xFF000000 | (com.phantompowers.HunterPaint.paint(stack) >= 0 ? com.phantompowers.HunterPaint.paint(stack)
				: stack.isOf(com.phantompowers.ModItems.HOVER_GLIDER) ? 0x656B79 : 0x43444F),
				com.phantompowers.ModItems.HOVER_GLIDER, com.phantompowers.ModItems.HUNTER_SUIT);
		net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, tctx, ttype, lines) -> {
			if (com.phantompowers.HunterPaint.paintable(stack) && com.phantompowers.HunterPaint.paint(stack) >= 0)
				lines.add(net.minecraft.text.Text.literal("Paint: " + String.format("#%06X", com.phantompowers.HunterPaint.paint(stack))).withColor(com.phantompowers.HunterPaint.paint(stack)));
			if (com.phantompowers.HunterPaint.upgradable(stack) && com.phantompowers.HunterPaint.tier(stack) > 0)
				lines.add(net.minecraft.text.Text.literal("Upgrade level " + com.phantompowers.HunterPaint.tier(stack)).formatted(net.minecraft.util.Formatting.AQUA));
		});
		com.phantompowers.CloningPodBlock.openChoice = pos -> net.minecraft.client.MinecraftClient.getInstance().setScreen(new CloneChoiceScreen(pos));
		BlockRenderLayerMap.INSTANCE.putBlock(com.phantompowers.ModBlocks.CLONING_POD, RenderLayer.getTranslucent());
		EntityModelLayerRegistry.registerModelLayer(WalkerModel.GHOUL, WalkerModel::ghoul);
		EntityModelLayerRegistry.registerModelLayer(WalkerModel.KNIGHT, WalkerModel::specterKnight);
		EntityModelLayerRegistry.registerModelLayer(WalkerModel.SOUL, WalkerModel::lostSoul);
		EntityModelLayerRegistry.registerModelLayer(WalkerModel.YETI, WalkerModel::ghostYeti);
		EntityRendererRegistry.register(ModEntities.GHOUL, ctx -> new WalkerRenderer<>(ctx, WalkerModel.Kind.GHOUL, "ghoul", 1.0f, 0.4f));
		EntityRendererRegistry.register(ModEntities.SPECTER_KNIGHT, ctx -> new WalkerRenderer<>(ctx, WalkerModel.Kind.KNIGHT, "specter_knight", 1.0f, 0.45f));
		EntityRendererRegistry.register(ModEntities.LOST_SOUL, ctx -> new WalkerRenderer<>(ctx, WalkerModel.Kind.SOUL, "lost_soul", 0.95f, 0.3f));
		EntityRendererRegistry.register(ModEntities.GHOST_YETI, ctx -> new WalkerRenderer<>(ctx, WalkerModel.Kind.YETI, "ghost_yeti", 1.0f, 0.9f));
		EntityRendererRegistry.register(ModEntities.ECTO_CONSTRUCT, ConstructRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(DarkCapeFeature.LAYER, DarkCapeFeature::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(CannonFeature.LAYER, CannonFeature::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(AcneFeature.LAYER, AcneFeature::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(WristGadgetFeature.LAYER, WristGadgetFeature::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.CRATE_PROJECTILE, ctx -> new FlyingItemEntityRenderer<>(ctx, 1.6f, false));
		EntityRendererRegistry.register(ModEntities.FOOD_PROJECTILE, ctx -> new FlyingItemEntityRenderer<>(ctx, 1.2f, false));
		EntityModelLayerRegistry.registerModelLayer(ChefModel.LAYER, ChefModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(ChefMeatModel.LAYER, ChefMeatModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.CHEF_GHOULASH, ChefRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(RageWyvernModel.LAYER, RageWyvernModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.RAGE_WYVERN, RageWyvernRenderer::new);
		EntityRendererRegistry.register(ModEntities.TOWNSPERSON, TownspersonRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(CollectorModel.LAYER, CollectorModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.COLLECTOR, CollectorRenderer::new);
		EntityRendererRegistry.register(ModEntities.WYVERN_FIREBALL, ctx -> new FlyingItemEntityRenderer<>(ctx, 1.4f, true));
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
				helper.register(new DanceSuitFeature(playerRenderer));
				helper.register(new GhostTail(playerRenderer, ctx.getPart(GhostTail.LAYER)));
				helper.register(new DarkCapeFeature(playerRenderer, ctx.getPart(DarkCapeFeature.LAYER)));
				helper.register(new CannonFeature(playerRenderer, ctx.getPart(CannonFeature.LAYER)));
				helper.register(new WristGadgetFeature(playerRenderer, ctx.getPart(WristGadgetFeature.LAYER)));
				helper.register(new AcneFeature(playerRenderer, ctx.getPart(AcneFeature.LAYER)));
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
			ClientDomes.clear();
			ClientSparks.clear();
			StoryHud.clear();
			GhostFlags.CLIENT_FISTS.clear();
			GhostFlags.CLIENT_SHIELDED.clear();
			GhostFlags.CLIENT_SUIT_COLORS.clear();
			GhostFlags.CLIENT_DARK.clear();
			GhostFlags.CLIENT_ACNE.clear();
			GhostFlags.CLIENT_CANNONS.clear();
			GhostFlags.CLIENT_AIMING.clear();
			WristGadgetFeature.clear();
			GhostTrail.clear();
			ClientShockwaves.clear();
			ClientEntityFx.clear();
			ClientGhostSense.clear();
			ClientCutscene.reset(client);
			SuitTextures.clear();
			PaintTextures.clear();
			com.phantompowers.PortalSwirls.ACTIVE.clear();
		});

		// "Ghost Skills" tab on the inventory screen
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof InventoryScreen) {
				int l = (width - 176) / 2, t = (height - 166) / 2;
				Screens.getButtons(screen).add(new GhostlyUi.GhostButton(l, t - 22, 86, 20, Text.literal("Phantom Menu"),
						b -> client.setScreen(new PhantomMenuScreen()), GhostlyUi.GREEN));
			}
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
			ClientDomes.render(ctx);
			ClientSparks.render(ctx);
			ClientShield.render(ctx);
			GhostTrail.render(ctx);
			ClientGhostSense.render(ctx);
			ClientShockwaves.render(ctx);
			ClientEntityFx.render(ctx);
			PortalSwirlRenderer.render(ctx);
		});
		HudRenderCallback.EVENT.register(ClientCutscene::renderHud);
		HudRenderCallback.EVENT.register(ClientGhostSense::renderHud);
		HudRenderCallback.EVENT.register(ControlsHud::render);
		HudRenderCallback.EVENT.register(SuitTrackerHud::render);
		HudRenderCallback.EVENT.register(BlasterScope::render);
		HudRenderCallback.EVENT.register(MissileSight::render);
		HudRenderCallback.EVENT.register(SkimmerHud::render);
		HudRenderCallback.EVENT.register(StoryHud::render);
		HudRenderCallback.EVENT.register(QuestCompass::render);
		HudRenderCallback.EVENT.register(LockOn::render);
		// make sure our textures load (and say why if they don't)
		net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(
				new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
					@Override
					public net.minecraft.util.Identifier getFabricId() {
						return com.phantompowers.PhantomMod.id("texture_check");
					}

					@Override
					public void reload(net.minecraft.resource.ResourceManager manager) {
						TextureCheck.run(manager);
					}
				});
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				client.execute(() -> TextureCheck.tellPlayer(client)));
		WorldRenderEvents.START.register(ctx -> LockOn.frame(net.minecraft.client.MinecraftClient.getInstance()));
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GHOST_PORTAL, RenderLayer.getTranslucent());
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), ModBlocks.COFFEE_TABLE, ModBlocks.POTTED_PLANT);
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
