package com.phantompowers;

import com.phantompowers.entity.GhostNetTrapEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/** Small entry point for the Ghost Net, so input code doesn't need to know about the trap entity. */
public final class GhostNet {
	private GhostNet() {}

	public static void struggle(ServerPlayerEntity p) {
		GhostNetTrapEntity.struggle(p);
	}
}
