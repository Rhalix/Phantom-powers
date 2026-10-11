package com.phantompowers.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.phantompowers.PhantomMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/**
 * Ghost form keeps your own skin, face and hairstyle, but turns the hair white and the eyes glowing red.
 * We read your skin back from the GPU once, find the hair and eye pixels, and make a recolored copy.
 */
public final class GhostSkins {
	private static final Map<Identifier, Entry> CACHE = new HashMap<>();
	private static int counter;

	private record Entry(Identifier skin, Identifier eyes, int retryAt) {}

	private GhostSkins() {}

	/** The recolored skin, or null if it can't be made (then the normal skin is used). */
	public static Identifier skin(Identifier original) {
		Entry e = get(original);
		return e == null ? null : e.skin;
	}

	/** A texture holding only the red eyes, drawn glowing on top. */
	public static Identifier eyes(Identifier original) {
		Entry e = get(original);
		return e == null ? null : e.eyes;
	}

	private static Entry get(Identifier original) {
		if (original == null) return null;
		Entry e = CACHE.get(original);
		int now = (int) (System.currentTimeMillis() / 1000);
		if (e != null && (e.skin != null || now < e.retryAt)) return e.skin == null ? null : e;
		if (!RenderSystem.isOnRenderThread()) return null;
		Entry made;
		try {
			made = build(original);
		} catch (Throwable t) {
			made = null;
		}
		if (made == null) made = new Entry(null, null, now + 3);
		CACHE.put(original, made);
		return made.skin == null ? null : made;
	}

	public static void clear() {
		CACHE.clear();
	}

	private static Entry build(Identifier original) {
		MinecraftClient mc = MinecraftClient.getInstance();
		AbstractTexture tex = mc.getTextureManager().getTexture(original);
		if (tex == null) return null;
		int glId = tex.getGlId();
		RenderSystem.bindTexture(glId);
		int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
		if (w < 64 || h < 32 || w > 1024) return null;
		NativeImage img = new NativeImage(w, h, false);
		img.loadFromTextureImage(0, false);
		NativeImage eyes = new NativeImage(w, h, true);
		int s = w / 64;
		recolor(img, eyes, s);
		String n = "ghost_skin_" + (counter++);
		Identifier skinId = PhantomMod.id(n);
		Identifier eyesId = PhantomMod.id(n + "_eyes");
		mc.getTextureManager().registerTexture(skinId, new NativeImageBackedTexture(img));
		mc.getTextureManager().registerTexture(eyesId, new NativeImageBackedTexture(eyes));
		return new Entry(skinId, eyesId, 0);
	}

	// ---- color helpers (NativeImage stores ABGR) ----
	private static int r(int c) { return c & 0xFF; }
	private static int g(int c) { return (c >> 8) & 0xFF; }
	private static int b(int c) { return (c >> 16) & 0xFF; }
	private static int a(int c) { return (c >>> 24) & 0xFF; }
	private static int abgr(int a, int r, int g, int b) { return (a << 24) | (b << 16) | (g << 8) | r; }
	private static int lum(int c) { return (r(c) * 3 + g(c) * 6 + b(c)) / 10; }
	private static int dist(int c1, int c2) {
		int dr = r(c1) - r(c2), dg = g(c1) - g(c2), db = b(c1) - b(c2);
		return (int) Math.sqrt(dr * dr + dg * dg + db * db);
	}
	private static boolean nearWhite(int c) {
		int mx = Math.max(r(c), Math.max(g(c), b(c))), mn = Math.min(r(c), Math.min(g(c), b(c)));
		return lum(c) > 185 && mx - mn < 45;
	}

	/** Most common opaque color among the given pixels (colors bucketed a little so shading groups together). */
	private static int mode(NativeImage img, int s, int[][] boxes) {
		Map<Integer, Integer> counts = new HashMap<>();
		Map<Integer, Integer> sample = new HashMap<>();
		for (int[] bx : boxes) {
			for (int y = bx[1] * s; y < bx[3] * s; y++) {
				for (int x = bx[0] * s; x < bx[2] * s; x++) {
					int c = img.getColor(x, y);
					if (a(c) < 128) continue;
					int key = (r(c) >> 4) << 8 | (g(c) >> 4) << 4 | (b(c) >> 4);
					counts.merge(key, 1, Integer::sum);
					sample.putIfAbsent(key, c);
				}
			}
		}
		int best = -1, bestN = 0;
		for (var en : counts.entrySet()) if (en.getValue() > bestN) { bestN = en.getValue(); best = en.getKey(); }
		return best < 0 ? 0 : sample.get(best);
	}

	private static void recolor(NativeImage img, NativeImage eyes, int s) {
		// Skin tone: lower half of the face and the chin.
		int skin = mode(img, s, new int[][]{{8, 13, 16, 16}, {0, 12, 8, 16}, {16, 12, 24, 16}});
		// Hair: top of the head, the back of the head and the forehead row.
		int hair = mode(img, s, new int[][]{{8, 0, 16, 8}, {24, 8, 32, 11}, {8, 8, 16, 9}});
		boolean hairDistinct = dist(hair, skin) > 40;

		// Find the eyes: a row on the face (rows 10..13) with whites or strongly colored pixels.
		boolean[][] eye = new boolean[8 * s][8 * s];
		int eyeRow = -1, bestScore = 0;
		for (int ry = 1; ry <= 5; ry++) {
			int score = 0;
			for (int rx = 1; rx < 7; rx++) {
				int c = img.getColor((8 + rx) * s, (8 + ry) * s);
				if (a(c) < 128) continue;
				if (nearWhite(c) && !nearWhite(skin)) score += 3;
				else if (dist(c, skin) > 70 && (!hairDistinct || dist(c, hair) > 60)) score += 2;
			}
			if (score > bestScore) { bestScore = score; eyeRow = ry; }
		}
		if (eyeRow < 0 || bestScore < 4) eyeRow = 4; // default Steve/Alex eye row
		for (int y = 0; y < s; y++) {
			for (int x = 0; x < 8 * s; x++) {
				int px = 8 * s + x, py = (8 + eyeRow) * s + y;
				int c = img.getColor(px, py);
				if (a(c) < 128) continue;
				boolean isEye = dist(c, skin) > 55 && (!hairDistinct || dist(c, hair) > 50);
				int fx = x / s;
				if (bestScore < 4) isEye = fx == 1 || fx == 2 || fx == 5 || fx == 6; // couldn't tell: classic spots
				eye[x][eyeRow * s + y] = isEye;
			}
		}

		// Recolor hair on the head (base layer) and hair in the hat layer.
		int[][] headBoxes = {{0, 0, 32, 16}, {32, 0, 64, 16}};
		for (int[] bx : headBoxes) {
			boolean hat = bx[0] == 32;
			for (int y = bx[1] * s; y < bx[3] * s; y++) {
				for (int x = bx[0] * s; x < bx[2] * s; x++) {
					int c = img.getColor(x, y);
					if (a(c) < 16) continue;
					// Face front eye cells
					int fx = hat ? x - 32 * s : x;
					if (fx >= 8 * s && fx < 16 * s && y >= 8 * s && y < 16 * s && eye[fx - 8 * s][y - 8 * s]) continue;
					boolean isHair;
					if (hairDistinct) isHair = dist(c, hair) < 75 && dist(c, skin) > 30;
					else isHair = (y < 8 * s) && !hat; // can't tell hair from skin: only the top of the head
					// Hair-colored strands right below the eye row stay as they are (beards, mouths)
					if (isHair && !hat && fx >= 8 * s && fx < 16 * s && y >= (8 + eyeRow + 1) * s && y < 16 * s) {
						isHair = false;
					}
					if (!isHair) continue;
					int v = 228 + (lum(c) - lum(hair)) / 2;
					v = Math.max(170, Math.min(255, v));
					img.setColor(x, y, abgr(a(c), v, v, Math.min(255, v + 10)));
				}
			}
		}

		// Eyes: glowing red, brighter where the whites were.
		for (int y = 0; y < 8 * s; y++) {
			for (int x = 0; x < 8 * s; x++) {
				if (!eye[x][y]) continue;
				int px = 8 * s + x, py = 8 * s + y;
				int c = img.getColor(px, py);
				int red = nearWhite(c) || lum(c) > 150 ? abgr(255, 255, 70, 60) : abgr(255, 215, 10, 10);
				img.setColor(px, py, red);
				eyes.setColor(px, py, red);
				// Hat layer over the eyes would hide them: clear it there
				int hc = img.getColor(px + 32 * s, py);
				if (a(hc) > 0) img.setColor(px + 32 * s, py, 0);
			}
		}
	}
}
