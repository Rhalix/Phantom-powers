package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Smooth look changes. While the transformation rings (or the suit bands) sweep over a player, everything the
 * rings have already passed shows the new look and everything else still shows the old one, so you can watch
 * your skin, suit, hair and eyes change as the rings move.
 */
public final class LookTransition {
	public static final int HUMAN = 0, GHOST = 1, SUIT = 2;

	public static final class T {
		public final int before, after, duration;
		final boolean suitSweep;
		int age;

		T(int before, int after, int duration, boolean suitSweep) {
			this.before = before;
			this.after = after;
			this.duration = Math.max(4, duration);
			this.suitSweep = suitSweep;
		}
	}

	private static final Map<Integer, T> ACTIVE = new ConcurrentHashMap<>();
	/** Extra ticks we keep showing the new look after the rings finish, until the server confirms the change. */
	private static final int GRACE = 12;

	private LookTransition() {}

	/** Ring styles: 0 = into ghost, 3/4 = back to human, 1 = suit on, 5 = suit off. */
	public static void start(int entityId, int style, int duration) {
		T t = switch (style) {
			case 0 -> new T(HUMAN, GHOST, duration, false);
			case 3, 4 -> new T(GHOST, HUMAN, duration, false);
			case 1, 6 -> new T(HUMAN, SUIT, duration, true);
			case 5 -> new T(SUIT, HUMAN, duration, true);
			default -> null;
		};
		if (t != null) ACTIVE.put(entityId, t);
	}

	public static void tick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		ACTIVE.entrySet().removeIf(en -> {
			T t = en.getValue();
			t.age++;
			if (t.age <= t.duration) return false;
			Entity e = mc.world == null ? null : mc.world.getEntityById(en.getKey());
			return e == null || t.age > t.duration + GRACE || current(e) == t.after;
		});
	}

	public static void clear() {
		ACTIVE.clear();
	}

	public static T get(Entity e) {
		return ACTIVE.isEmpty() ? null : ACTIVE.get(e.getId());
	}

	/** The look the server says this player has right now. */
	public static int current(Entity e) {
		if (GhostFlags.CLIENT_GHOSTS.contains(e.getUuid())) return GHOST;
		if (GhostFlags.CLIENT_SUITED.contains(e.getUuid())) return SUIT;
		return HUMAN;
	}

	public static Identifier texture(AbstractClientPlayerEntity player, int look) {
		Identifier own = player.getSkinTextures().texture();
		return switch (look) {
			case GHOST -> {
				Identifier g = GhostSkins.skin(own);
				yield g != null ? g : own;
			}
			case SUIT -> GhostFlags.SUIT_SKIN;
			default -> own;
		};
	}

	/**
	 * The world-height band [lo, hi] the rings have already swept over (shows the new look).
	 * Matches the ring animation in {@link ClientRings}.
	 */
	public static double[] band(Entity e, T t, float partial) {
		float p = Math.min(1f, (t.age + partial) / t.duration);
		Vec3d pos = e.getLerpedPos(partial);
		double h = e.getHeight();
		if (p >= 1f) return new double[]{-1e9, 1e9};
		if (t.suitSweep) {
			float lt = Math.min(1f, p / 0.55f);
			if (lt >= 1f) return new double[]{-1e9, 1e9};
			double top = pos.y + 0.05 + (h - 0.1) * lt;
			return new double[]{-1e9, top};
		}
		float split = Math.max(0f, (p - 0.2f) / 0.8f);
		float ease = split * split * (3 - 2 * split);
		double mid = h * 0.5;
		double up = mid + (h - mid + 0.1) * ease;
		double down = mid - (mid + 0.05) * ease;
		if (ease >= 0.999f) return new double[]{-1e9, 1e9};
		return new double[]{pos.y + down, pos.y + up};
	}

	/** Camera height, to turn world heights into the camera-relative heights the renderer works in. */
	public static double camY() {
		return MinecraftClient.getInstance().gameRenderer.getCamera().getPos().y;
	}
}
