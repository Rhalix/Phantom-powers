package com.phantompowers;

import com.phantompowers.network.DomeHitPayload;
import com.phantompowers.network.DomePayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of the Ecto-Shield: every raised dome, and the barrier that shocks and shoves back any ghost
 * (ghost mobs, ghost bosses and half-ghosts in ghost form) that tries to cross it, from either side.
 */
public final class ShieldDomes {
	public static final int MIN_RADIUS = 4, MAX_RADIUS = 40, SINGLE_RADIUS = 6, MAX_EMITTERS = 16;

	public static final class Dome {
		public final int id;
		public final RegistryKey<World> world;
		public final List<BlockPos> emitters;
		public Vec3d center;
		public double radius;
		final Map<UUID, Boolean> inside = new HashMap<>();
		final Map<UUID, Integer> zapCooldown = new HashMap<>();
		int age;

		Dome(int id, RegistryKey<World> world, List<BlockPos> emitters) {
			this.id = id;
			this.world = world;
			this.emitters = emitters;
			reshape();
		}

		/** Centre over the middle of the emitters; big enough to cover all of them with room to spare. */
		void reshape() {
			int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE, minY = Integer.MAX_VALUE;
			for (BlockPos p : emitters) {
				minX = Math.min(minX, p.getX());
				maxX = Math.max(maxX, p.getX());
				minZ = Math.min(minZ, p.getZ());
				maxZ = Math.max(maxZ, p.getZ());
				minY = Math.min(minY, p.getY());
			}
			center = new Vec3d((minX + maxX) / 2.0 + 0.5, minY, (minZ + maxZ) / 2.0 + 0.5);
			double r = 0;
			for (BlockPos p : emitters) r = Math.max(r, Math.hypot(p.getX() + 0.5 - center.x, p.getZ() + 0.5 - center.z));
			radius = emitters.size() == 1 ? SINGLE_RADIUS : Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, r + 2.5));
		}
	}

	private static final Map<Integer, Dome> DOMES = new HashMap<>();

	private ShieldDomes() {}

	public static Dome get(int id) {
		return DOMES.get(id);
	}

	public static boolean isUp(int id) {
		return DOMES.containsKey(id);
	}

	/** The dome (if any) this emitter is part of. */
	public static Dome containing(ServerWorld w, BlockPos pos) {
		for (Dome d : DOMES.values()) if (d.world.equals(w.getRegistryKey()) && d.emitters.contains(pos)) return d;
		return null;
	}

	public static int count() {
		return DOMES.size();
	}

	/** Raise a dome over these emitters. Returns null (and does nothing) if none of them are usable. */
	public static Dome raise(ServerWorld w, int id, List<BlockPos> links) {
		lower(w.getServer(), id, false);
		List<BlockPos> ok = new ArrayList<>();
		for (BlockPos p : links) {
			if (ok.size() >= MAX_EMITTERS) break;
			if (w.isChunkLoaded(p) && w.getBlockState(p).isOf(ModBlocks.SHIELD_EMITTER)) ok.add(p.toImmutable());
		}
		if (ok.isEmpty()) return null;
		Dome d = new Dome(id, w.getRegistryKey(), ok);
		DOMES.put(id, d);
		for (BlockPos p : ok) w.setBlockState(p, w.getBlockState(p).with(ShieldEmitterBlock.ACTIVE, true));
		w.playSound(null, d.center.x, d.center.y, d.center.z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 2.0f, 1.3f);
		w.playSound(null, d.center.x, d.center.y, d.center.z, SoundEvents.BLOCK_CONDUIT_ACTIVATE, SoundCategory.BLOCKS, 1.6f, 1.2f);
		for (BlockPos p : ok) {
			w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 20, 0.2, 0.3, 0.2, 0.2);
		}
		sync(w, d, true);
		return d;
	}

	/** Drop a dome (if it's up). */
	public static void lower(MinecraftServer server, int id, boolean loud) {
		Dome d = DOMES.remove(id);
		if (d == null) return;
		ServerWorld w = server.getWorld(d.world);
		if (w == null) return;
		for (BlockPos p : d.emitters) {
			BlockState s = w.getBlockState(p);
			if (w.isChunkLoaded(p) && s.isOf(ModBlocks.SHIELD_EMITTER) && s.get(ShieldEmitterBlock.ACTIVE)) {
				w.setBlockState(p, s.with(ShieldEmitterBlock.ACTIVE, false));
			}
		}
		if (loud) {
			w.playSound(null, d.center.x, d.center.y, d.center.z, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 2.0f, 1.2f);
		}
		sync(w, d, false);
	}

	/** An emitter was broken: the dome it was part of collapses. */
	public static void onEmitterGone(ServerWorld w, BlockPos pos) {
		for (Dome d : new ArrayList<>(DOMES.values())) {
			if (d.world.equals(w.getRegistryKey()) && d.emitters.contains(pos)) {
				d.emitters.remove(pos);
				lower(w.getServer(), d.id, true);
				w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.3);
			}
		}
	}

	public static void clearAll(MinecraftServer server) {
		for (Integer id : new ArrayList<>(DOMES.keySet())) lower(server, id, false);
	}

	/** Is this entity something the dome keeps out? Ghost mobs, ghost bosses and half-ghosts in ghost form. */
	public static boolean isGhost(Entity e) {
		if (!(e instanceof LivingEntity) || !e.isAlive()) return false;
		if (e instanceof ServerPlayerEntity p) {
			if (p.isSpectator() || p.isCreative()) return false;
			return GhostState.of(p.getServer()).data(p.getUuid()).transformed;
		}
		return HunterSuit.ghostBonus(e) > 1f;
	}

	public static void tick(MinecraftServer server) {
		if (DOMES.isEmpty()) return;
		Iterator<Dome> it = DOMES.values().iterator();
		List<Integer> dead = new ArrayList<>();
		while (it.hasNext()) {
			Dome d = it.next();
			ServerWorld w = server.getWorld(d.world);
			if (w == null) {
				dead.add(d.id);
				continue;
			}
			d.age++;
			if (d.age % 20 == 0) sync(w, d, true);
			if (d.age % 10 == 0) {
				for (BlockPos p : d.emitters) {
					if (w.isChunkLoaded(p) && !w.getBlockState(p).isOf(ModBlocks.SHIELD_EMITTER)) {
						dead.add(d.id);
						break;
					}
				}
			}
			if (d.age % 50 == 0) {
				w.playSound(null, d.center.x, d.center.y + 1, d.center.z, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 0.8f, 1.4f);
			}
			barrier(w, d);
		}
		for (int id : dead) lower(server, id, true);
	}

	private static void barrier(ServerWorld w, Dome d) {
		double r = d.radius;
		Box box = new Box(d.center, d.center).expand(r + 4);
		d.zapCooldown.replaceAll((k, v) -> v - 1);
		d.zapCooldown.values().removeIf(v -> v <= 0);
		for (Entity e : w.getOtherEntities(null, box, ShieldDomes::isGhost)) {
			if (e.hasVehicle()) continue;
			Vec3d c = e.getBoundingBox().getCenter();
			Vec3d off = c.subtract(d.center);
			double dist = off.length();
			Boolean wasInside = d.inside.get(e.getUuid());
			if (wasInside == null) {
				// First look at it. Ghost mobs caught inside when the dome goes up get thrown out.
				wasInside = dist < r;
				if (wasInside && !(e instanceof ServerPlayerEntity)) wasInside = false;
				d.inside.put(e.getUuid(), wasInside);
			}
			Vec3d dir = dist < 1e-3 ? new Vec3d(1, 0, 0) : off.multiply(1 / dist);
			if (!wasInside && dist < r + 0.7) {
				repel(w, d, e, dir, r + 1.3, true);
			} else if (wasInside && dist > r - 0.7) {
				repel(w, d, e, dir, r - 1.3, false);
			}
		}
		if (d.age % 100 == 0) d.inside.keySet().removeIf(id -> w.getEntity(id) == null);
	}

	/** Zap! Put the ghost back on its own side and fling it away from the wall. */
	private static void repel(ServerWorld w, Dome d, Entity e, Vec3d dir, double keepAt, boolean outward) {
		Vec3d c = e.getBoundingBox().getCenter();
		Vec3d hit = d.center.add(dir.multiply(d.radius));
		Vec3d push = (outward ? dir : dir.negate());
		double dist = c.distanceTo(d.center);
		if (outward ? dist < keepAt : dist > keepAt) {
			Vec3d target = d.center.add(dir.multiply(keepAt)).subtract(0, e.getHeight() / 2, 0);
			if (target.y < e.getY() - 4) target = new Vec3d(target.x, e.getY(), target.z);
			if (e instanceof ServerPlayerEntity p) p.networkHandler.requestTeleport(target.x, target.y, target.z, p.getYaw(), p.getPitch());
			else e.refreshPositionAfterTeleport(target);
		}
		e.setVelocity(push.x * 1.1, Math.max(0.25, push.y * 0.8 + 0.3), push.z * 1.1);
		e.velocityModified = true;
		e.fallDistance = 0;
		if (!d.zapCooldown.containsKey(e.getUuid())) {
			d.zapCooldown.put(e.getUuid(), 10);
			e.damage(w.getDamageSources().lightningBolt(), 3.0f);
			if (e instanceof LivingEntity le) le.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS, 30, 2, false, false));
			w.playSound(null, hit.x, hit.y, hit.z, SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.BLOCKS, 0.7f, 2.0f);
			w.playSound(null, hit.x, hit.y, hit.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.BLOCKS, 0.8f, 1.8f);
			w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, hit.x, hit.y, hit.z, 40, 0.4, 0.6, 0.4, 0.6);
			w.spawnParticles(GhostPowers.GREEN, hit.x, hit.y, hit.z, 25, 0.5, 0.8, 0.5, 0);
			if (e instanceof ServerPlayerEntity p) {
				p.sendMessage(net.minecraft.text.Text.literal("ZAP! An Ecto-Shield blocks ghosts from passing.").formatted(net.minecraft.util.Formatting.GREEN), true);
			}
			DomeHitPayload hp = new DomeHitPayload(d.id, (float) hit.x, (float) hit.y, (float) hit.z);
			for (ServerPlayerEntity viewer : PlayerLookup.around(w, hit, 128)) {
				if (ServerPlayNetworking.canSend(viewer, DomeHitPayload.ID)) ServerPlayNetworking.send(viewer, hp);
			}
		}
	}

	private static void sync(ServerWorld w, Dome d, boolean up) {
		DomePayload pl = new DomePayload(d.id, (float) d.center.x, (float) d.center.y, (float) d.center.z, (float) d.radius, up, d.emitters);
		for (ServerPlayerEntity p : w.getPlayers()) {
			if (ServerPlayNetworking.canSend(p, DomePayload.ID)) ServerPlayNetworking.send(p, pl);
		}
	}
}
