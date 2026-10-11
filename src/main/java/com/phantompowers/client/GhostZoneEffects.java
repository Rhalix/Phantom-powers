package com.phantompowers.client;

import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.Vec3d;

/**
 * The Ghost Zone sky: no sun, moon, stars or clouds. The whole sky is the biome's
 * glowing green fog color, like the swirling green void in the show.
 */
public class GhostZoneEffects extends DimensionEffects {
	public GhostZoneEffects() {
		super(Float.NaN, false, DimensionEffects.SkyType.NONE, false, false);
	}

	@Override
	public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
		return color;
	}

	@Override
	public boolean useThickFog(int camX, int camY) {
		return false;
	}
}
