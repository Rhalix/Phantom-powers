package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flying fast in ghost form, you level out like Superman: body stretched along your flight path,
 * one fist punched forward, the other arm along your side. Hover and you straighten back up.
 */
public final class SupermanFlight {
	private static final Map<UUID, float[]> LEAN = new ConcurrentHashMap<>(); // {previous, current}

	private SupermanFlight() {}

	public static boolean flying(PlayerEntity p) {
		if (GhostFlags.CLIENT_GHOSTS.isEmpty() || !GhostFlags.CLIENT_GHOSTS.contains(p.getUuid())) return false;
		if (p.hasVehicle() || p.isFallFlying()) return false;
		return p.isMainPlayer() ? p.getAbilities().flying : GhostFlags.CLIENT_FLYING.contains(p.getUuid());
	}

	public static void tick(MinecraftClient mc) {
		if (mc.world == null) {
			LEAN.clear();
			return;
		}
		for (PlayerEntity p : mc.world.getPlayers()) {
			float[] l = LEAN.computeIfAbsent(p.getUuid(), k -> new float[2]);
			double dx = p.getX() - p.prevX, dz = p.getZ() - p.prevZ, dy = p.getY() - p.prevY;
			double speed = Math.sqrt(dx * dx + dz * dz + dy * dy * 0.5);
			float target = flying(p) && speed > 0.14 ? 1f : 0f;
			l[0] = l[1];
			l[1] += (target - l[1]) * (target > l[1] ? 0.18f : 0.25f);
			if (l[1] < 0.001f && target == 0f) l[1] = 0f;
		}
		LEAN.keySet().removeIf(id -> mc.world.getPlayerByUuid(id) == null);
	}

	/** 0 = upright, 1 = fully stretched out. */
	public static float lean(Entity e, float tickDelta) {
		float[] l = LEAN.get(e.getUuid());
		return l == null ? 0f : MathHelper.lerp(tickDelta, l[0], l[1]);
	}
}
