package com.phantompowers.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Files;
import java.nio.file.Path;

/** Ghost radar settings: press M to switch between Off, Ghosts only, Mobs only, and Ghosts + Mobs (remembered between games). */
public final class RadarSettings {
	public static final int OFF = 0, GHOSTS = 1, MOBS = 2, BOTH = 3;
	private static final String[] NAMES = {"OFF", "Ghosts only", "Mobs only", "Ghosts + Mobs"};
	private static int mode = BOTH;
	private static boolean loaded;

	private RadarSettings() {}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("phantom_radar.txt");
	}

	public static int mode() {
		if (!loaded) {
			loaded = true;
			try {
				if (Files.exists(file())) mode = Math.floorMod(Integer.parseInt(Files.readString(file()).trim()), 4);
			} catch (Exception ignored) {
			}
		}
		return mode;
	}

	public static boolean ghosts() {
		return mode() == GHOSTS || mode() == BOTH;
	}

	public static boolean mobs() {
		return mode() == MOBS || mode() == BOTH;
	}

	public static void set(int m) {
		mode = Math.floorMod(m, 4);
		loaded = true;
		try {
			Files.writeString(file(), Integer.toString(mode));
		} catch (Exception ignored) {
		}
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player != null) mc.player.sendMessage(Text.literal("Radar: " + NAMES[mode]).formatted(mode == OFF ? Formatting.GRAY : Formatting.AQUA), true);
	}

	public static void cycle() {
		set(mode() + 1);
	}

	public static String name() {
		return NAMES[mode()];
	}
}
