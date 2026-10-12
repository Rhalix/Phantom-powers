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

	private static int count(NativeImage img, int s, int[] box) {
		int n = 0;
		for (int y = box[1] * s; y < box[3] * s; y++)
			for (int x = box[0] * s; x < box[2] * s; x++)
				if (a(img.getColor(x, y)) >= 128) n++;
		return n;
	}

	/** Color of face pixel (fx, fy) in 8x8 face units: the hat layer if it covers it, otherwise the base layer. */
	private static int faceAt(NativeImage img, int s, int fx, int fy) {
		int hat = img.getColor((40 + fx) * s, (8 + fy) * s);
		if (a(hat) >= 128) return hat;
		return img.getColor((8 + fx) * s, (8 + fy) * s);
	}

	private static void recolor(NativeImage img, NativeImage eyes, int s) {
		// Skin tone: lower half of the face and the chin.
		int skin = mode(img, s, new int[][]{{9, 14, 15, 16}, {8, 13, 16, 16}});
		// Hair: the top of the head. Lots of skins draw the hair on the hat layer, so look there first.
		int[] hatTop = {40, 0, 48, 8};
		int hair = count(img, s, hatTop) > 20 * s * s
				? mode(img, s, new int[][]{hatTop, {56, 8, 64, 12}})
				: mode(img, s, new int[][]{{8, 0, 16, 8}, {24, 8, 32, 11}});
		boolean hairDistinct = dist(hair, skin) > 40;

		// ---- Eyes: on the face (rows 2..6), one per side, at most 2 pixels wide.
		// Each candidate pixel is scored by how much it stands out from both skin and hair;
		// whites of the eyes score extra. The best row wins.
		boolean[][] eye = new boolean[8][8];
		int bestRow = -1;
		int bestScore = 0;
		int[][] rowPicks = new int[8][];
		for (int fy = 2; fy <= 6; fy++) {
			int[] picks = new int[4];
			int score = 0;
			int k = 0;
			for (int side = 0; side < 2; side++) {
				int c1 = -1, c2 = -1, s1 = 0, s2 = 0;
				for (int fx = side == 0 ? 0 : 4; fx < (side == 0 ? 4 : 8); fx++) {
					int c = faceAt(img, s, fx, fy);
					if (a(c) < 128) continue;
					int sc = Math.min(dist(c, skin), hairDistinct ? dist(c, hair) : 999);
					if (nearWhite(c) && !nearWhite(skin)) sc += 60;
					boolean classic = fx == 1 || fx == 2 || fx == 5 || fx == 6;
					if (classic) sc += 12;
					if (fx == 0 || fx == 7) sc -= 25; // edges are usually hair or eyeliner
					if (sc > s1) { s2 = s1; c2 = c1; s1 = sc; c1 = fx; }
					else if (sc > s2) { s2 = sc; c2 = fx; }
				}
				int need = 55;
				picks[k++] = s1 >= need ? c1 : -1;
				// Second pixel only if it sits right next to the first and is just as strong
				picks[k++] = (s2 >= need && s2 >= s1 - 40 && Math.abs(c2 - c1) == 1) ? c2 : -1;
				if (s1 >= need) score += s1;
				if (picks[k - 1] >= 0) score += s2 / 2;
			}
			// A row where most of the face differs from skin is a hair fringe or a mask, not eyes
			int odd = 0;
			for (int fx = 0; fx < 8; fx++) {
				int c = faceAt(img, s, fx, fy);
				if (dist(c, skin) > 55) odd++;
			}
			if (odd >= 7) score /= 3;
			rowPicks[fy] = picks;
			if (score > bestScore) { bestScore = score; bestRow = fy; }
		}
		if (bestRow < 0 || bestScore < 120) {
			bestRow = 4; // couldn't find them: classic eye spots
			rowPicks[4] = new int[]{1, 2, 5, 6};
		}
		for (int fx : rowPicks[bestRow]) if (fx >= 0) eye[fx][bestRow] = true;
		// Taller eyes: include the same spots just below when they also stand out from the skin
		int below = bestRow + 1;
		if (below <= 6) {
			for (int fx = 0; fx < 8; fx++) {
				if (!eye[fx][bestRow]) continue;
				int c = faceAt(img, s, fx, below);
				if (dist(c, skin) > 70 && (!hairDistinct || dist(c, hair) > 50)) eye[fx][below] = true;
			}
		}

		// ---- Hair: anything on the head (base and hat layer) that looks more like hair than skin.
		int[][] headBoxes = {{0, 0, 32, 16}, {32, 0, 64, 16}};
		for (int[] bx : headBoxes) {
			boolean hat = bx[0] == 32;
			for (int y = bx[1] * s; y < bx[3] * s; y++) {
				for (int x = bx[0] * s; x < bx[2] * s; x++) {
					int c = img.getColor(x, y);
					if (a(c) < 16) continue;
					int fx = (hat ? x - 32 * s : x) / s - 8, fy = y / s - 8;
					boolean onFace = fx >= 0 && fx < 8 && fy >= 0 && fy < 8;
					if (onFace && eye[fx][fy]) continue;
					boolean isHair;
					if (hairDistinct) {
						isHair = dist(c, hair) < dist(c, skin) && dist(c, hair) < 110;
					} else {
						isHair = y < 8 * s && !hat;
					}
					// Below the eyes it's mouth/beard territory: leave those alone
					if (isHair && onFace && fy > bestRow) isHair = false;
					if (!isHair) continue;
					int v = 232 + (lum(c) - lum(hair)) / 2;
					v = Math.max(175, Math.min(255, v));
					img.setColor(x, y, abgr(a(c), v, v, Math.min(255, v + 10)));
				}
			}
		}

		// ---- Eyes: glowing red, brighter where the whites were. Clear the hat layer over them.
		for (int fy = 0; fy < 8; fy++) {
			for (int fx = 0; fx < 8; fx++) {
				if (!eye[fx][fy]) continue;
				int c = faceAt(img, s, fx, fy);
				for (int yy = 0; yy < s; yy++) {
					for (int xx = 0; xx < s; xx++) {
						int px = (8 + fx) * s + xx, py = (8 + fy) * s + yy;
						int red = nearWhite(c) || lum(c) > 150 ? abgr(255, 255, 80, 70) : abgr(255, 225, 15, 15);
						img.setColor(px, py, red);
						eyes.setColor(px, py, red);
						img.setColor(px + 32 * s, py, 0);
					}
				}
			}
		}
	}
}
