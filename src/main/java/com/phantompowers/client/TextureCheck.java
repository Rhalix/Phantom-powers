package com.phantompowers.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks every Phantom Powers character and armor texture can actually be loaded, and says which ones can't and why
 * (a broken file, or another resource pack replacing it). The results go in the game log, and you get a chat
 * message if anything's wrong.
 */
public final class TextureCheck {
	private static final Logger LOG = LoggerFactory.getLogger("phantom");
	private static final List<String> problems = new ArrayList<>();
	private static boolean told;

	private TextureCheck() {}

	public static void run(ResourceManager rm) {
		problems.clear();
		told = false;
		int ok = 0;
		for (String folder : new String[]{"textures/entity", "textures/models/armor", "textures/item", "textures/block"}) {
			Map<Identifier, List<Resource>> all = rm.findAllResources(folder, id -> id.getNamespace().equals("phantom") && id.getPath().endsWith(".png"));
			for (var e : all.entrySet()) {
				List<Resource> stack = e.getValue();
				if (stack.isEmpty()) continue;
				Resource top = stack.get(stack.size() - 1);
				if (stack.size() > 1 || !isOurs(top))
					LOG.warn("Phantom texture check: {} is being replaced by the resource pack '{}'", e.getKey(), top.getPackId());
				try (InputStream in = top.getInputStream(); NativeImage img = NativeImage.read(in)) {
					ok++;
				} catch (Throwable t) {
					String msg = e.getKey() + " (from '" + top.getPackId() + "'): " + t;
					problems.add(msg);
					LOG.error("Phantom texture check: couldn't load {}", msg);
				}
			}
		}
		LOG.info("Phantom texture check: {} textures loaded fine, {} problem(s)", ok, problems.size());
	}

	private static boolean isOurs(Resource r) {
		String id = r.getPackId();
		return id != null && (id.equals("phantom") || id.equals("fabric") || id.contains("phantom"));
	}

	/** Tell the player once per session if some textures couldn't be loaded. */
	public static void tellPlayer(MinecraftClient mc) {
		if (told || problems.isEmpty() || mc.player == null) return;
		told = true;
		mc.player.sendMessage(Text.literal("Phantom Powers: " + problems.size() + " texture(s) couldn't be loaded, so some things may look pink and black. "
				+ "The details are in logs/latest.log (search for \"Phantom texture check\").").formatted(Formatting.RED), false);
	}
}
