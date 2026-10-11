package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.GhostPowers;
import com.phantompowers.ModBlocks;
import com.phantompowers.PhantomMod;
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
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class PhantomClient implements ClientModInitializer {
	public static final String CATEGORY = "key.category.phantom";

	public static KeyBinding TRANSFORM;
	public static KeyBinding BLAST;
	public static KeyBinding INTANGIBLE;
	public static KeyBinding POSSESS;
	public static KeyBinding WAIL;

	@Override
	public void onInitializeClient() {
		TRANSFORM = key("transform", GLFW.GLFW_KEY_V);
		BLAST = key("blast", GLFW.GLFW_KEY_R);
		INTANGIBLE = key("intangible", GLFW.GLFW_KEY_G);
		POSSESS = key("possess", GLFW.GLFW_KEY_H);
		WAIL = key("wail", GLFW.GLFW_KEY_B);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;
			while (TRANSFORM.wasPressed()) send(GhostPowers.A_TRANSFORM);
			while (BLAST.wasPressed()) send(GhostPowers.A_BLAST);
			while (INTANGIBLE.wasPressed()) send(GhostPowers.A_INTANGIBLE);
			while (POSSESS.wasPressed()) send(GhostPowers.A_POSSESS);
			while (WAIL.wasPressed()) send(GhostPowers.A_WAIL);
		});

		ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) -> ClientGhost.update(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientGhost.reset();
			GhostFlags.clientSelfIntangible = false;
		});

		HudRenderCallback.EVENT.register(GhostHud::render);
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GHOST_PORTAL, RenderLayer.getTranslucent());
		DimensionRenderingRegistry.registerDimensionEffects(PhantomMod.id("ghost_zone"), new GhostZoneEffects());
	}

	private static KeyBinding key(String name, int code) {
		return KeyBindingHelper.registerKeyBinding(
				new KeyBinding("key.phantom." + name, InputUtil.Type.KEYSYM, code, CATEGORY));
	}

	private static void send(int action) {
		if (ClientPlayNetworking.canSend(SkillPayload.ID)) {
			ClientPlayNetworking.send(new SkillPayload(action));
		}
	}
}
