package com.phantompowers.world;

import com.phantompowers.ModEntities;
import com.phantompowers.PortalLogic;
import com.phantompowers.entity.AbstractGhostEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/** Fills the Ghost Zone with ghosts and raises a Ghost Prison in each big region of it. */
public final class GhostZoneLife {
	public static final int REGION = 640;
	private static final int MAX_GHOSTS_NEAR = 7;

	private GhostZoneLife() {}

	public static void tick(MinecraftServer server) {
		if (server.getTicks() % 40 != 0) return;
		ServerWorld zone = server.getWorld(PortalLogic.GHOST_ZONE);
		if (zone == null) return;
		for (ServerPlayerEntity p : zone.getPlayers()) {
			if (!p.isAlive() || p.isSpectator()) continue;
			spawnAround(zone, p);
			BlockPos prison = nearestPrison(zone, p.getBlockPos());
			double dx = prison.getX() - p.getX(), dz = prison.getZ() - p.getZ();
			if (dx * dx + dz * dz < 150 * 150) ensurePrison(zone, prison);
		}
	}

	private static void spawnAround(ServerWorld zone, ServerPlayerEntity p) {
		Random r = zone.getRandom();
		if (r.nextInt(2) != 0) return;
		int count = zone.getEntitiesByClass(AbstractGhostEntity.class, p.getBoundingBox().expand(48), g -> !g.hasHome()).size();
		if (count >= MAX_GHOSTS_NEAR) return;
		double angle = r.nextDouble() * Math.PI * 2;
		double dist = 18 + r.nextDouble() * 18;
		Vec3d at = p.getPos().add(Math.cos(angle) * dist, r.nextInt(16) - 6, Math.sin(angle) * dist);
		BlockPos bp = BlockPos.ofFloored(at);
		if (!zone.isChunkLoaded(ChunkPos.toLong(bp.getX() >> 4, bp.getZ() >> 4))) return;
		if (bp.getY() < 5 || bp.getY() > 250) return;
		MobEntity ghost = r.nextInt(100) < 55 ? ModEntities.HOSTILE_GHOST.create(zone) : ModEntities.FRIENDLY_GHOST.create(zone);
		if (ghost == null) return;
		ghost.refreshPositionAndAngles(at.x, at.y, at.z, r.nextFloat() * 360f, 0f);
		zone.spawnEntity(ghost);
	}

	/** The prison for the region this position is in (a random spot inside that region). */
	public static BlockPos nearestPrison(ServerWorld zone, BlockPos pos) {
		BlockPos best = null;
		double bestD = Double.MAX_VALUE;
		int rx0 = Math.floorDiv(pos.getX(), REGION), rz0 = Math.floorDiv(pos.getZ(), REGION);
		for (int rx = rx0 - 1; rx <= rx0 + 1; rx++) {
			for (int rz = rz0 - 1; rz <= rz0 + 1; rz++) {
				BlockPos c = prisonIn(zone.getSeed(), rx, rz);
				double d = c.getSquaredDistance(pos);
				if (d < bestD) {
					bestD = d;
					best = c;
				}
			}
		}
		return best;
	}

	private static BlockPos prisonIn(long seed, int rx, int rz) {
		Random r = Random.create(seed ^ (rx * 341873128712L) ^ (rz * 132897987541L) ^ 0x6A7E11L);
		int x = rx * REGION + 150 + r.nextInt(REGION - 300);
		int z = rz * REGION + 150 + r.nextInt(REGION - 300);
		return new BlockPos(x, 108 + r.nextInt(16), z);
	}

	/** Builds the prison at this spot if nobody has seen it yet. Returns true if it was just built. */
	public static boolean ensurePrison(ServerWorld zone, BlockPos center) {
		RoomState rs = RoomState.of(zone.getServer());
		long key = center.asLong();
		if (rs.prisonBuilt(key)) return false;
		rs.markPrison(key);
		GhostPrison.build(zone, center, Random.create(key));
		return true;
	}
}
