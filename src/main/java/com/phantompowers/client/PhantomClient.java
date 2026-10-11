package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.GhostPowers;
import com.phantompowers.ModBlocks;
import com.phantompowers.PhantomMod;
import com.phantompowers.network.BeamPayload;
import com.phantompowers.network.InvisiblePayload;
import com.phantompowers.network.SkillPayload;
import com.phantompowers.network.SyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
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

	/** Key bound to each skill, in skill order (used by the HUD and skills screen). */
	public static KeyBinding[] skillKeys() {
		return new KeyBinding[]{BLAST, INTANGIBLE, POSSESS, WAIL, TORNADO, INVISIBLE};
	}

	private static boolean intangibleHeld;

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

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientBeams.tick(client);
			if (client.player == null) return;
			while (TRANSFORM.wasPressed()) send(GhostPowers.A_TRANSFORM);
			while (BLAST.wasPressed()) send(GhostPowers.A_BLAST);
			while (INTANGIBLE.wasPressed()) send(GhostPowers.A_INTANGIBLE);
			while (POSSESS.wasPressed()) send(GhostPowers.A_POSSESS);
			while (WAIL.wasPressed()) send(GhostPowers.A_WAIL);
			while (TORNADO.wasPressed()) send(GhostPowers.A_TORNADO);
			while (INVISIBLE.wasPressed()) send(GhostPowers.A_INVISIBLE);
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
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientGhost.reset();
			GhostFlags.clientSelfIntangible = false;
			GhostFlags.CLIENT_INVISIBLE.clear();
			ClientBeams.clear();
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
		WorldRenderEvents.AFTER_ENTITIES.register(ClientBeams::render);
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
