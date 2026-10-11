package com.phantompowers.world;

import com.phantompowers.GhostSense;
import com.phantompowers.GhostState;
import com.phantompowers.ModEntities;
import com.phantompowers.PortalLogic;
import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.entity.CrateKingEntity;
import com.phantompowers.entity.GhostJailerEntity;
import com.phantompowers.entity.VoltageEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps the Ghost Zone alive: wandering ghosts, a Ghost Prison, a Crate King warehouse and a Voltage
 * concert stage in every region, bosses that come back to their lairs a day after being beaten,
 * and the occasional boss breaking out into the Overworld at night.
 */
public final class GhostZoneLife {
	public static final int REGION = 512;
	private static final int MAX_GHOSTS_NEAR = 7;
	/** A beaten boss returns after one Minecraft day. */
	private static final long RESPAWN_TIME = 24000;
	private static final Set<UUID> BREAKOUTS = new HashSet<>();

	private GhostZoneLife() {}

	private enum Lair { PRISON, CRATE_KEEP, STAGE }

	public static void tick(MinecraftServer server) {
		int t = server.getTicks();
		if (t % 40 == 0) tickZone(server);
		if (t % 1200 == 600) tickOverworld(server);
	}

	private static void tickZone(MinecraftServer server) {
		ServerWorld zone = server.getWorld(PortalLogic.GHOST_ZONE);
		if (zone == null) return;
		for (ServerPlayerEntity p : zone.getPlayers()) {
			if (!p.isAlive() || p.isSpectator()) continue;
			spawnAround(zone, p);
			for (Lair lair : Lair.values()) {
				BlockPos c = lairPos(zone, p.getBlockPos(), lair);
				double dx = c.getX() - p.getX(), dz = c.getZ() - p.getZ();
				double d2 = dx * dx + dz * dz;
				if (d2 < 150 * 150) ensureLair(zone, c, lair);
				if (d2 < 110 * 110) maybeReturnBoss(zone, c, lair);
			}
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

	// ----------------------------------------------------------------- lairs

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

	private static BlockPos lairPos(ServerWorld zone, BlockPos near, Lair lair) {
		return switch (lair) {
			case PRISON -> nearestPrison(zone, near);
			case CRATE_KEEP -> nearestCrateKeep(zone, near);
			case STAGE -> nearestStage(zone, near);
		};
	}

	/** The Crate King's warehouse sits across the region from its prison (east-west). */
	public static BlockPos nearestCrateKeep(ServerWorld zone, BlockPos pos) {
		BlockPos prison = nearestPrison(zone, pos);
		int cx = Math.floorDiv(prison.getX(), REGION) * REGION + REGION / 2;
		return new BlockPos(prison.getX() < cx ? prison.getX() + 220 : prison.getX() - 220, prison.getY() - 6, prison.getZ());
	}

	/** Voltage's stage sits across the region from its prison (north-south). */
	public static BlockPos nearestStage(ServerWorld zone, BlockPos pos) {
		BlockPos prison = nearestPrison(zone, pos);
		int cz = Math.floorDiv(prison.getZ(), REGION) * REGION + REGION / 2;
		return new BlockPos(prison.getX(), prison.getY() - 3, prison.getZ() < cz ? prison.getZ() + 220 : prison.getZ() - 220);
	}

	private static long key(BlockPos c, Lair lair) {
		return switch (lair) {
			case PRISON -> c.asLong();
			case CRATE_KEEP -> c.asLong() ^ 0x5C0FFEE5L;
			case STAGE -> c.asLong() ^ 0x5717A9E5L;
		};
	}

	private static boolean ensureLair(ServerWorld zone, BlockPos c, Lair lair) {
		RoomState rs = RoomState.of(zone.getServer());
		long key = key(c, lair);
		if (rs.prisonBuilt(key)) return false;
		rs.markPrison(key);
		rs.markBoss(key, zone.getTime());
		switch (lair) {
			case PRISON -> GhostPrison.build(zone, c, Random.create(key));
			case CRATE_KEEP -> CrateKeep.build(zone, c, Random.create(key));
			case STAGE -> ConcertStage.build(zone, c, Random.create(key));
		}
		return true;
	}

	public static boolean ensurePrison(ServerWorld zone, BlockPos c) {
		return ensureLair(zone, c, Lair.PRISON);
	}

	public static boolean ensureCrateKeep(ServerWorld zone, BlockPos c) {
		return ensureLair(zone, c, Lair.CRATE_KEEP);
	}

	public static boolean ensureStage(ServerWorld zone, BlockPos c) {
		return ensureLair(zone, c, Lair.STAGE);
	}

	/** A beaten boss comes home again a day later, so you can fight it again. */
	private static void maybeReturnBoss(ServerWorld zone, BlockPos c, Lair lair) {
		RoomState rs = RoomState.of(zone.getServer());
		long key = key(c, lair);
		if (!rs.prisonBuilt(key) || !rs.bossReady(key, zone.getTime(), RESPAWN_TIME)) return;
		Box area = new Box(c).expand(96);
		Class<? extends Entity> boss = switch (lair) {
			case PRISON -> GhostJailerEntity.class;
			case CRATE_KEEP -> CrateKingEntity.class;
			case STAGE -> VoltageEntity.class;
		};
		if (!zone.getEntitiesByClass(boss, area, e -> e.isAlive()).isEmpty()) {
			rs.markBoss(key, zone.getTime()); // still alive: check again later
			return;
		}
		String name;
		switch (lair) {
			case PRISON -> {
				GhostPrison.spawnJailer(zone, GhostPrison.bossSpot(c, GhostPrison.isSpire(key)), 20f, true);
				name = "The Ghost Jailer has returned to his prison!";
			}
			case CRATE_KEEP -> {
				CrateKingEntity k = ModEntities.CRATE_KING.create(zone);
				if (k != null) {
					k.refreshPositionAndAngles(c.getX() + 0.5, c.getY() + 2, c.getZ() + 0.5, 0, 0);
					k.setHome(c.add(0, 2, 0), 16f);
					k.setPersistent();
					zone.spawnEntity(k);
				}
				name = "The Crate King is back in his warehouse!";
			}
			default -> {
				VoltageEntity v = ModEntities.VOLTAGE.create(zone);
				if (v != null) {
					v.refreshPositionAndAngles(c.getX() + 0.5, c.getY() + 3, c.getZ() - 11.5, 0, 0);
					v.setHome(c.add(0, 3, -4), 16f);
					v.setPersistent();
					zone.spawnEntity(v);
				}
				name = "Voltage is back on stage for another show!";
			}
		}
		rs.markBoss(key, zone.getTime());
		for (ServerPlayerEntity p : zone.getPlayers(p -> p.squaredDistanceTo(Vec3d.ofCenter(c)) < 128 * 128)) {
			p.sendMessage(Text.literal(name).formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
		}
	}

	// ------------------------------------------------------------- breakouts

	/** Now and then at night, a boss breaks out of the Ghost Zone near a player in the Overworld. */
	private static void tickOverworld(MinecraftServer server) {
		ServerWorld ow = server.getOverworld();
		// Breakout bosses fade away when the sun comes up and nobody is around
		Iterator<UUID> it = BREAKOUTS.iterator();
		while (it.hasNext()) {
			Entity e = ow.getEntity(it.next());
			if (e == null || !e.isAlive()) {
				it.remove();
				continue;
			}
			if (ow.isDay() && ow.getClosestPlayer(e, 64) == null) {
				e.discard();
				it.remove();
			}
		}
		if (!ow.isNight() || ow.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
		for (ServerPlayerEntity p : ow.getPlayers()) {
			if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
			if (ow.getRandom().nextInt(25) != 0) continue;
			if (!ow.getEntitiesByClass(AbstractGhostEntity.class, p.getBoundingBox().expand(128),
					e -> e instanceof GhostJailerEntity || e instanceof CrateKingEntity || e instanceof VoltageEntity).isEmpty()) continue;
			breakout(ow, p.getBlockPos());
		}
	}

	/** Spawns a random boss near a spot in the Overworld and warns everyone nearby. */
	public static Entity breakout(ServerWorld ow, BlockPos near) {
		Random r = ow.getRandom();
		double a = r.nextDouble() * Math.PI * 2;
		int x = near.getX() + (int) (Math.cos(a) * (30 + r.nextInt(15)));
		int z = near.getZ() + (int) (Math.sin(a) * (30 + r.nextInt(15)));
		int y = ow.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) + 3;
		AbstractGhostEntity boss = switch (r.nextInt(3)) {
			case 0 -> ModEntities.GHOST_JAILER.create(ow);
			case 1 -> ModEntities.CRATE_KING.create(ow);
			default -> ModEntities.VOLTAGE.create(ow);
		};
		if (boss == null) return null;
		boss.refreshPositionAndAngles(x + 0.5, y, z + 0.5, r.nextFloat() * 360f, 0f);
		ow.spawnEntity(boss);
		BREAKOUTS.add(boss.getUuid());
		ow.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL, x + 0.5, y + 1, z + 0.5, 80, 1.5, 2, 1.5, 0.05);
		org.slf4j.LoggerFactory.getLogger("phantom").info("A ghost boss broke out into the Overworld: {}", boss.getName().getString());
		for (ServerPlayerEntity p : ow.getPlayers(p -> p.squaredDistanceTo(x, y, z) < 120 * 120)) {
			p.sendMessage(Text.literal(boss.getName().getString() + " has broken out of the Ghost Zone nearby!")
					.formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
			if (GhostState.of(ow.getServer()).data(p.getUuid()).powers) GhostSense.trigger(p);
		}
		return boss;
	}
}
