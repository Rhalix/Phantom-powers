package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.HunterPaint;
import com.phantompowers.PhantomMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Repainted textures for hunter gear worn or placed in the world (the suit and the Hover Glider): the dark armour
 * plating takes on the paint colour, keeping its shading, while the white trim, visor and glowing bits stay as they are.
 */
public final class PaintTextures {
	private static final Map<String, Identifier> CACHE = new HashMap<>();
	private static final Map<Identifier, NativeImage> BASES = new HashMap<>();
	private static int counter;

	private PaintTextures() {}

	public static void init() {
		GhostFlags.suitSkin = PaintTextures::suitSkin;
	}

	private static Identifier suitSkin(PlayerEntity p) {
		ItemStack pack = p.getEquippedStack(EquipmentSlot.CHEST);
		int paint = HunterPaint.paint(pack);
		return paint < 0 ? GhostFlags.SUIT_SKIN : painted(GhostFlags.SUIT_SKIN, paint);
	}

	/** The base texture with its dark, unsaturated parts repainted in the given colour. */
	public static Identifier painted(Identifier base, int color) {
		String key = base + "#" + Integer.toHexString(color);
		Identifier id = CACHE.get(key);
		if (id != null) return id;
		NativeImage src = load(base);
		if (src == null) return base;
		NativeImage out = new NativeImage(src.getWidth(), src.getHeight(), true);
		int pr = (color >> 16) & 255, pg = (color >> 8) & 255, pb = color & 255;
		for (int y = 0; y < src.getHeight(); y++) {
			for (int x = 0; x < src.getWidth(); x++) {
				int abgr = src.getColor(x, y);
				int a = abgr >>> 24, r = abgr & 255, g = (abgr >> 8) & 255, b = (abgr >> 16) & 255;
				int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
				float sat = max == 0 ? 0 : (max - min) / (float) max;
				float lum = 0.3f * r + 0.59f * g + 0.11f * b;
				if (a > 0 && sat < 0.3f && lum < 120) {
					float k = 0.22f + 0.78f * (lum / 120f);
					r = Math.min(255, (int) (pr * k));
					g = Math.min(255, (int) (pg * k));
					b = Math.min(255, (int) (pb * k));
				}
				out.setColor(x, y, (a << 24) | (b << 16) | (g << 8) | r);
			}
		}
		id = PhantomMod.id("dynamic/paint_" + counter++);
		MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(out));
		if (CACHE.size() > 48) {
			for (Identifier old : CACHE.values()) MinecraftClient.getInstance().getTextureManager().destroyTexture(old);
			CACHE.clear();
		}
		CACHE.put(key, id);
		return id;
	}

	private static NativeImage load(Identifier base) {
		NativeImage img = BASES.get(base);
		if (img != null) return img;
		try (InputStream in = MinecraftClient.getInstance().getResourceManager().getResource(base).orElseThrow().getInputStream()) {
			img = NativeImage.read(in);
			BASES.put(base, img);
			return img;
		} catch (Exception e) {
			return null;
		}
	}

	public static void clear() {
		for (Identifier id : CACHE.values()) MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
		CACHE.clear();
	}
}
