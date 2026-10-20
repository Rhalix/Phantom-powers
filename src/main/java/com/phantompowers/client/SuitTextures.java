package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.PhantomMod;
import com.phantompowers.SuitColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builds suit and tail textures on the fly from the plain white templates, painting each part
 * (suit, stripes, gloves, boots) in the colours the player picked, while keeping the template's shading.
 */
public final class SuitTextures {
	public record Set(Identifier suit, Identifier glow, Identifier tail, int[] colors) {}

	private static final Identifier SUIT_TEMPLATE = PhantomMod.id("textures/entity/ghost_suit.png");
	private static final Identifier TAIL_TEMPLATE = PhantomMod.id("textures/entity/ghost_tail.png");
	private static final int MAX = 32;
	private static final Map<String, Set> CACHE = new LinkedHashMap<>(16, 0.75f, true);
	private static NativeImage suitBase, tailBase;
	private static boolean failed;
	private static int counter;

	private SuitTextures() {}

	/** The colours a player is wearing (the customizer's live preview wins for yourself while it's open). */
	private static final Identifier DARK_SUIT = PhantomMod.id("textures/entity/ghost_suit_dark.png");
	private static final Identifier DARK_GLOW = PhantomMod.id("textures/entity/ghost_suit_dark_glow.png");

	/** The dark half-ghost wears its own evil suit design (black with jagged hot-pink seams), not the customizable one. */
	public static boolean dark(UUID id) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (GhostSuitScreen.previewing && mc.player != null && mc.player.getUuid().equals(id)) return false;
		return id != null && !GhostFlags.CLIENT_DARK.isEmpty() && GhostFlags.CLIENT_DARK.contains(id);
	}

	public static int[] colorsOf(UUID id) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (GhostSuitScreen.previewing && mc.player != null && mc.player.getUuid().equals(id)) return GhostSuitScreen.preview;
		if (dark(id)) return SuitColors.VILLAIN;
		int[] c = id == null ? null : GhostFlags.CLIENT_SUIT_COLORS.get(id);
		return c == null ? SuitColors.CLASSIC : c;
	}

	public static Set of(AbstractClientPlayerEntity p) {
		if (dark(p.getUuid())) {
			Set v = get(SuitColors.VILLAIN);
			return new Set(DARK_SUIT, DARK_GLOW, v.tail(), v.colors());
		}
		return get(colorsOf(p.getUuid()));
	}

	public static Set get(int[] colors) {
		int[] c = SuitColors.sanitize(colors);
		String key = Arrays.toString(c);
		Set s = CACHE.get(key);
		if (s != null) return s;
		if (!load()) return new Set(SUIT_TEMPLATE, null, TAIL_TEMPLATE, c);
		int n = counter++;
		Identifier suit = PhantomMod.id("dynamic/suit_" + n), tail = PhantomMod.id("dynamic/tail_" + n);
		Identifier glow = (c[SuitColors.FLAGS] & 1) != 0 ? PhantomMod.id("dynamic/suit_glow_" + n) : null;
		var tm = MinecraftClient.getInstance().getTextureManager();
		tm.registerTexture(suit, new NativeImageBackedTexture(paintSuit(c, false)));
		if (glow != null) tm.registerTexture(glow, new NativeImageBackedTexture(paintSuit(c, true)));
		tm.registerTexture(tail, new NativeImageBackedTexture(paintTail(c)));
		s = new Set(suit, glow, tail, c);
		CACHE.put(key, s);
		while (CACHE.size() > MAX) {
			var it = CACHE.entrySet().iterator();
			Set old = it.next().getValue();
			it.remove();
			tm.destroyTexture(old.suit);
			tm.destroyTexture(old.tail);
			if (old.glow != null) tm.destroyTexture(old.glow);
		}
		return s;
	}

	public static void clear() {
		var tm = MinecraftClient.getInstance().getTextureManager();
		for (Set s : CACHE.values()) {
			tm.destroyTexture(s.suit);
			tm.destroyTexture(s.tail);
			if (s.glow != null) tm.destroyTexture(s.glow);
		}
		CACHE.clear();
	}

	private static boolean load() {
		if (suitBase != null && tailBase != null) return true;
		if (failed) return false;
		try {
			var rm = MinecraftClient.getInstance().getResourceManager();
			try (InputStream in = rm.getResource(SUIT_TEMPLATE).orElseThrow().getInputStream()) {
				suitBase = NativeImage.read(in);
			}
			try (InputStream in = rm.getResource(TAIL_TEMPLATE).orElseThrow().getInputStream()) {
				tailBase = NativeImage.read(in);
			}
			return true;
		} catch (Exception e) {
			failed = true;
			org.slf4j.LoggerFactory.getLogger("phantom").warn("Couldn't load ghost suit templates", e);
			return false;
		}
	}

	/** Which part of the suit a template pixel belongs to. */
	static int part(int x, int y, int lum) {
		if (lum < 80) return SuitColors.STRIPES;
		// gloves: lower arm rows and the bottom of the hands
		if (x >= 40 && x < 56 && y >= 28 && y < 32) return SuitColors.GLOVES;
		if (x >= 32 && x < 48 && y >= 60) return SuitColors.GLOVES;
		if (x >= 47 && x < 52 && y >= 16 && y < 20) return SuitColors.GLOVES;
		if (x >= 39 && x < 44 && y >= 48 && y < 52) return SuitColors.GLOVES;
		// boots: lower leg rows and the soles
		if (x < 16 && y >= 28 && y < 32) return SuitColors.BOOTS;
		if (x >= 16 && x < 32 && y >= 60) return SuitColors.BOOTS;
		if (x >= 8 && x < 12 && y >= 16 && y < 20) return SuitColors.BOOTS;
		if (x >= 24 && x < 28 && y >= 48 && y < 52) return SuitColors.BOOTS;
		return SuitColors.BODY;
	}

	private static NativeImage paintSuit(int[] c, boolean glow) {
		NativeImage out = new NativeImage(suitBase.getWidth(), suitBase.getHeight(), true);
		for (int y = 0; y < suitBase.getHeight(); y++) {
			for (int x = 0; x < suitBase.getWidth(); x++) {
				int abgr = suitBase.getColor(x, y);
				int a = abgr >>> 24;
				if (a == 0) {
					out.setColor(x, y, 0);
					continue;
				}
				int lum = lum(abgr);
				int part = part(x, y, lum);
				if (glow) {
					out.setColor(x, y, part == SuitColors.STRIPES ? abgr(255, c[SuitColors.GLOW], 1f) : 0);
					continue;
				}
				out.setColor(x, y, abgr(a, c[part], shade(part, lum)));
			}
		}
		return out;
	}

	private static NativeImage paintTail(int[] c) {
		NativeImage out = new NativeImage(tailBase.getWidth(), tailBase.getHeight(), true);
		for (int y = 0; y < tailBase.getHeight(); y++) {
			for (int x = 0; x < tailBase.getWidth(); x++) {
				int abgr = tailBase.getColor(x, y);
				int a = abgr >>> 24;
				if (a == 0) {
					out.setColor(x, y, 0);
					continue;
				}
				int lum = lum(abgr);
				int part = lum < 80 ? SuitColors.STRIPES : SuitColors.BODY;
				int col = c[part];
				// glowing stripes show through the tail too
				if (part == SuitColors.STRIPES && (c[SuitColors.FLAGS] & 1) != 0) col = c[SuitColors.GLOW];
				out.setColor(x, y, abgr(a, col, shade(part, lum)));
			}
		}
		return out;
	}

	private static float shade(int part, int lum) {
		return part == SuitColors.STRIPES ? Math.min(1.1f, 0.85f + lum / 300f) : Math.min(1.05f, lum / 240f);
	}

	private static int lum(int abgr) {
		int r = abgr & 255, g = (abgr >> 8) & 255, b = (abgr >> 16) & 255;
		return (r * 3 + g * 6 + b) / 10;
	}

	private static int abgr(int a, int rgb, float k) {
		int r = Math.min(255, (int) (((rgb >> 16) & 255) * k));
		int g = Math.min(255, (int) (((rgb >> 8) & 255) * k));
		int b = Math.min(255, (int) ((rgb & 255) * k));
		return (a << 24) | (b << 16) | (g << 8) | r;
	}
}
