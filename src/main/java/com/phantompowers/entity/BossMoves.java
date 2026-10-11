package com.phantompowers.entity;

import com.phantompowers.network.ShockwavePayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Shared boss attacks: delayed moves, rolling ground shockwaves, sound waves, temporary cages. */
public final class BossMoves {
	private BossMoves() {}

	// ------------------------------------------------------------------ scheduling

	private record Task(int[] ticks, Runnable run) {}

	private static final List<Task> TASKS = new ArrayList<>();
	private static final List<Task> ADD = new ArrayList<>();

	/** Run something a few ticks from now. */
	public static void later(int ticks, Runnable run) {
		ADD.add(new Task(new int[]{ticks}, run));
	}

	public static void tick(MinecraftServer server) {
		if (!ADD.isEmpty()) {
			TASKS.addAll(ADD);
			ADD.clear();
		}
		Iterator<Task> it = TASKS.iterator();
		while (it.hasNext()) {
			Task t = it.next();
			if (--t.ticks[0] > 0) continue;
			it.remove();
			try {
				t.run.run();
			} catch (RuntimeException e) {
				org.slf4j.LoggerFactory.getLogger("phantom").warn("Boss move failed", e);
			}
		}
		Iterator<Waves> wi = WAVES.iterator();
		while (wi.hasNext()) if (wi.next().step()) wi.remove();
		Iterator<Cage> ci = CAGES.iterator();
		while (ci.hasNext()) {
			Cage c = ci.next();
			if (--c.ticks <= 0) {
				c.restore();
				ci.remove();
			}
		}
	}

	// ------------------------------------------------------------------ visuals

	public static void send(ServerWorld w, ShockwavePayload p) {
		for (ServerPlayerEntity viewer : PlayerLookup.around(w, new Vec3d(p.x(), p.y(), p.z()), 128)) {
			if (ServerPlayNetworking.canSend(viewer, ShockwavePayload.ID)) ServerPlayNetworking.send(viewer, p);
		}
	}

	/** A cone of sound-wave rings flying from {@code from} along {@code dir}. */
	public static void soundWave(ServerWorld w, Vec3d from, Vec3d dir, float range, int color) {
		send(w, new ShockwavePayload((float) from.x, (float) from.y, (float) from.z,
				(float) dir.x, (float) dir.y, (float) dir.z, range, color, 0));
	}

	/** The ground straight below a point (or the point itself if there's nothing close). */
	public static Vec3d ground(ServerWorld w, Vec3d at) {
		BlockHitResult hit = w.raycast(new RaycastContext(at.add(0, 1, 0), at.add(0, -14, 0),
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, net.minecraft.block.ShapeContext.absent()));
		return hit.getType() == HitResult.Type.MISS ? at : hit.getPos();
	}

	/** Warning circle of particles on the ground for a while before an attack lands. */
	public static void warn(ServerWorld w, Vec3d center, double radius, int ticks, int color) {
		DustParticleEffect dust = new DustParticleEffect(new Vector3f(((color >> 16) & 255) / 255f,
				((color >> 8) & 255) / 255f, (color & 255) / 255f), 1.4f);
		for (int t = 0; t < ticks; t += 3) {
			final int tt = t;
			later(Math.max(1, t), () -> {
				int n = (int) (radius * 7);
				for (int i = 0; i < n; i++) {
					double a = i * Math.PI * 2 / n + tt * 0.05;
					w.spawnParticles(dust, center.x + Math.cos(a) * radius, center.y + 0.15, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
				}
				w.spawnParticles(ParticleTypes.SMOKE, center.x, center.y + 0.1, center.z, 3, radius * 0.3, 0, radius * 0.3, 0);
			});
		}
	}

	// ------------------------------------------------------------------ ground shockwave

	private static final List<Waves> WAVES = new ArrayList<>();

	/** A ring that rolls outward along the ground and hits whatever it passes. Jump to dodge it! */
	private static final class Waves {
		final ServerWorld w;
		final Entity source;
		final Vec3d center;
		final double maxRadius;
		final float damage;
		final double speed = 0.9;
		final Set<UUID> hit = new HashSet<>();
		double r;

		Waves(ServerWorld w, Entity source, Vec3d center, double maxRadius, float damage) {
			this.w = w;
			this.source = source;
			this.center = center;
			this.maxRadius = maxRadius;
			this.damage = damage;
		}

		boolean step() {
			r += speed;
			Box box = new Box(center, center).expand(r + 1.5, 3, r + 1.5);
			for (Entity e : w.getOtherEntities(source, box, e -> e instanceof LivingEntity && e.isAlive() && !(e instanceof AbstractGhostEntity))) {
				if (hit.contains(e.getUuid())) continue;
				double dx = e.getX() - center.x, dz = e.getZ() - center.z;
				double d = Math.sqrt(dx * dx + dz * dz);
				double dy = e.getY() - center.y;
				if (Math.abs(d - r) > 1.3 || dy > 1.1 || dy < -2) continue; // in the air = dodged
				hit.add(e.getUuid());
				DamageSource src = source instanceof LivingEntity le ? w.getDamageSources().mobAttack(le) : w.getDamageSources().magic();
				e.damage(src, damage);
				double len = Math.max(0.1, d);
				e.addVelocity(dx / len * 1.1, 0.55, dz / len * 1.1);
				e.velocityModified = true;
			}
			return r >= maxRadius;
		}
	}

	/** Slam the ground: a visible ring rolls outward and damages everything it passes. */
	public static void groundWave(ServerWorld w, Entity source, Vec3d center, double radius, float damage, int color) {
		WAVES.add(new Waves(w, source, center, radius, damage));
		send(w, new ShockwavePayload((float) center.x, (float) center.y, (float) center.z, 0, 1, 0, (float) radius, color, 1));
		w.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 1.6f, 0.6f);
		w.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 1.4f, 0.5f);
		w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.3, center.z, 1, 0, 0, 0, 0);
	}

	/** Hit everything inside a cone in front of {@code from}. Returns how many were hit. */
	public static int coneHit(ServerWorld w, Entity source, Vec3d from, Vec3d dir, double range, double minDot,
			DamageSource dmg, float damage, double push, java.util.function.Predicate<Entity> filter) {
		int n = 0;
		for (Entity e : w.getOtherEntities(source, new Box(from, from).expand(range), e -> e instanceof LivingEntity && e.isAlive() && filter.test(e))) {
			Vec3d d = e.getPos().add(0, e.getHeight() / 2, 0).subtract(from);
			double len = d.length();
			if (len > range || len < 0.1 || d.multiply(1 / len).dotProduct(dir) < minDot) continue;
			e.damage(dmg, damage);
			e.addVelocity(dir.x * push, 0.35 + push * 0.1, dir.z * push);
			e.velocityModified = true;
			n++;
		}
		return n;
	}

	// ------------------------------------------------------------------ teleport

	/** Vanish in smoke and reappear behind the target. */
	public static void blinkBehind(ServerWorld w, Entity self, LivingEntity target, double dist) {
		Vec3d back = target.getRotationVec(1f).multiply(1, 0, 1);
		if (back.lengthSquared() < 1.0E-4) back = new Vec3d(1, 0, 0);
		Vec3d to = target.getPos().subtract(back.normalize().multiply(dist)).add(0, 0.5, 0);
		w.spawnParticles(ParticleTypes.LARGE_SMOKE, self.getX(), self.getY() + 1.5, self.getZ(), 40, 0.6, 1.0, 0.6, 0.02);
		w.spawnParticles(ParticleTypes.SOUL, self.getX(), self.getY() + 1.5, self.getZ(), 20, 0.6, 1.0, 0.6, 0.05);
		w.playSound(null, self.getX(), self.getY(), self.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.5f, 0.5f);
		self.refreshPositionAndAngles(to.x, to.y, to.z, target.getYaw(), 0);
		w.spawnParticles(ParticleTypes.LARGE_SMOKE, to.x, to.y + 1.5, to.z, 40, 0.6, 1.0, 0.6, 0.02);
		w.spawnParticles(ParticleTypes.SOUL, to.x, to.y + 1.5, to.z, 20, 0.6, 1.0, 0.6, 0.05);
		w.playSound(null, to.x, to.y, to.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.5f, 0.5f);
	}

	// ------------------------------------------------------------------ cages

	private static final List<Cage> CAGES = new ArrayList<>();

	private static final class Cage {
		final ServerWorld w;
		final List<BlockPos> placed = new ArrayList<>();
		final Block block;
		int ticks;

		Cage(ServerWorld w, Block block, int ticks) {
			this.w = w;
			this.block = block;
			this.ticks = ticks;
		}

		void restore() {
			for (BlockPos p : placed) {
				if (w.getBlockState(p).isOf(block)) {
					w.setBlockState(p, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
					w.spawnParticles(ParticleTypes.POOF, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
				}
			}
		}
	}

	/**
	 * Lock the target in a temporary cage (walls 3 high plus a roof). Only fills empty space,
	 * and the blocks vanish again after a few seconds. Ghost-form players can phase right out.
	 */
	public static int cage(ServerWorld w, LivingEntity target, Block block, int ticks) {
		BlockPos base = target.getBlockPos();
		Cage c = new Cage(w, block, ticks);
		for (int dy = 0; dy <= 3; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					boolean wall = dx != 0 || dz != 0;
					if (dy < 3 && !wall) continue;
					BlockPos p = base.add(dx, dy, dz);
					if (!w.getBlockState(p).isAir()) continue;
					w.setBlockState(p, block.getDefaultState(), Block.NOTIFY_ALL);
					c.placed.add(p);
				}
			}
		}
		// Let bars and panes connect to each other
		for (BlockPos p : c.placed) {
			BlockState s = w.getBlockState(p);
			BlockState fixed = Block.postProcessState(s, w, p);
			if (fixed != s) w.setBlockState(p, fixed, Block.NOTIFY_LISTENERS);
		}
		if (!c.placed.isEmpty()) CAGES.add(c);
		target.setVelocity(Vec3d.ZERO);
		target.velocityModified = true;
		w.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.HOSTILE, 1.5f, 0.6f);
		w.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.HOSTILE, 1.5f, 0.5f);
		return c.placed.size();
	}

	/** Put back every temporary block (server shutting down). */
	public static void restoreAll() {
		for (Cage c : CAGES) c.restore();
		CAGES.clear();
		TASKS.clear();
		ADD.clear();
		WAVES.clear();
	}
}
