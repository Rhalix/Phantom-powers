package com.phantompowers;

/** Ghost suit colour sets. Index: body, stripes, gloves, boots, trail glow, flags (bit 0 = stripes glow). */
public final class SuitColors {
	public static final int BODY = 0, STRIPES = 1, GLOVES = 2, BOOTS = 3, GLOW = 4, FLAGS = 5, PARTS = 6;
	public static final String[] PART_NAMES = {"Suit", "Stripes", "Gloves", "Boots", "Trail & Glow"};
	public static final int[] CLASSIC = {0xF4F6FA, 0x16161E, 0xF4F6FA, 0xF4F6FA, 0x55FF88, 0};
	/** What your suit turns into when you become the dark half-ghost. */
	public static final int[] VILLAIN = {0x24202C, 0xFF4FC8, 0x0E0C12, 0x0E0C12, 0xFF4FC8, 1};
	public static final String[] PRESET_NAMES = {"Classic", "Shadow", "Ecto Glow", "Crimson", "Frostbite", "Royal", "Villain"};
	public static final int[][] PRESETS = {
			CLASSIC,
			{0x22222C, 0xEEF0F4, 0xEEF0F4, 0xEEF0F4, 0x9A88C8, 0},
			{0x18201C, 0x5AFF96, 0x18201C, 0x18201C, 0x40FF90, 1},
			{0x961A24, 0x16141A, 0x16141A, 0x16141A, 0xE02838, 0},
			{0xCDE8FC, 0x46AAFF, 0xFFFFFF, 0xFFFFFF, 0x46AAFF, 1},
			{0x48287C, 0xE8C04A, 0xE8C04A, 0xE8C04A, 0xB060FF, 0},
			VILLAIN};

	private SuitColors() {}

	public static int[] sanitize(int[] in) {
		int[] out = CLASSIC.clone();
		if (in == null) return out;
		for (int i = 0; i < Math.min(PARTS, in.length); i++) out[i] = i == FLAGS ? in[i] & 1 : in[i] & 0xFFFFFF;
		return out;
	}
}
