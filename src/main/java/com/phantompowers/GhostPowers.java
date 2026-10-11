package com.phantompowers;

import com.phantompowers.network.BeamPayload;
import com.phantompowers.network.InvisiblePayload;
import com.phantompowers.network.SyncPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GhostPowers {
	public static final float MAX_ENERGY = 100f;
	/** Ticks spent in ghost form per stat point (600 ticks = 30 seconds). */
	public static final int POINT_TICKS = 600;

	public static final int BLAST = 0, INTANGIBLE = 1, POSSESS = 2, WAIL = 3, TORNADO = 4, INVISIBLE = 5;
	public static final int SKILL_COUNT = 6;
	public static final String[] NAMES = {"Ecto Blast", "Intangibility", "Possession", "Ghostly Wail", "Wail Tornado", "Invisibility"};
	public static final int[] COSTS = {0, 3, 5, 8, 12, 2};
	public static final String[] DESCRIPTIONS = {
			"Fire a beam of green ecto energy.",
			"Walk through walls. Sneak to sink, fly to rise. Sneak+hold to phase others.",
			"Take over a mob or player and control where it goes.",
			"A huge scream that breaks blocks and blasts mobs away.",
			"A giant tornado of wail energy that rips up the ground.",
			"Turn completely invisible, armor and all."
	};

	public static final int A_TRANSFORM = 0, A_BLAST = 1, A_INTANGIBLE = 2, A_POSSESS = 3, A_WAIL = 4, A_TORNADO = 5,
			A_INVISIBLE = 6, A_INTANGIBLE_RELEASE = 7, A_UNLOCK = 100;

	private static final float BLAST_COST = 12f;
	private static final float PHASE_TARGET_COST = 25f;
	private static final float POSSESS_START_COST = 15f;
	private static final float WAIL_COST = 60f;
	private static final float TORNADO_COST = 80f;

	public static final DustParticleEffect GREEN = new DustParticleEffect(new Vector3f(0.25f, 1.0f, 0.4f), 1.3f);
	private static final DustParticleEffect PALE = new DustParticleEffect(new Vector3f(0.75f, 1.0f, 0.85f), 1.0f);

	private static final List<PhasedBlock> PHASED = new ArrayList<>();
	/** Blocks a player is currently holding intangible (restored when they let go). */
	private static final Map<UUID, List<PhasedBlock>> HELD_BLOCKS = new HashMap<>();
	private static final List<WailWave> WAVES = new ArrayList<>();
	private static final List<Tornado> TORNADOES = new ArrayList<>();

	private GhostPowers() {}

	// ------------------------------------------------------------------ input

	public static void handleAction(ServerPlayerEntity p, int action) {
		if (!p.isAlive()) return;
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		switch (action) {
			case A_TRANSFORM -> toggleTransform(p, d);
			case A_BLAST -> { if (ready(p, d, st, BLAST)) blast(p, d); }
			case A_INTANGIBLE -> { if (ready(p, d, st, INTANGIBLE)) intangibility(p, d); }
			case A_POSSESS -> { if (ready(p, d, st, POSSESS)) possession(p, d, st); }
			case A_WAIL -> { if (ready(p, d, st, WAIL)) wail(p, d); }
			case A_TORNADO -> { if (ready(p, d, st, TORNADO)) tornado(p, d); }
			case A_INVISIBLE -> { if (ready(p, d, st, INVISIBLE)) toggleInvisible(p, d); }
			case A_INTANGIBLE_RELEASE -> releaseHold(p, d, true);
			default -> {
				if (action >= A_UNLOCK && action < A_UNLOCK + SKILL_COUNT) tryUnlock(p, d, st, action - A_UNLOCK);
			}
		}
		st.markDirty();
		d.syncTimer = 0;
	}

	private static boolean ready(ServerPlayerEntity p, GhostData d, GhostState st, int skill) {
		if (!d.powers) {
			bar(p, "You don't have ghost powers yet. Activate a Ghost Portal!", Formatting.RED);
			return false;
		}
		if (!d.has(skill)) {
			bar(p, NAMES[skill] + " is locked. Unlock it in Ghost Skills (press K or open your inventory).", Formatting.YELLOW);
			return false;
		}
		if (!d.transformed) {
			bar(p, "Go ghost first! Press V", Formatting.YELLOW);
			return false;
		}
		return true;
	}

	private static void tryUnlock(ServerPlayerEntity p, GhostData d, GhostState st, int skill) {
		if (!d.powers) {
			bar(p, "You don't have ghost powers yet. Activate a Ghost Portal!", Formatting.RED);
			return;
		}
		if (d.has(skill)) return;
		if (d.points < COSTS[skill]) {
			bar(p, NAMES[skill] + " needs " + COSTS[skill] + " stat points (you have " + d.points
					+ "). Stay in ghost form to earn more!", Formatting.YELLOW);
			return;
		}
		d.points -= COSTS[skill];
		d.unlocked |= 1 << skill;
		st.markDirty();
		p.sendMessage(Text.literal("New ghost power unlocked: " + NAMES[skill] + "!").formatted(Formatting.GREEN, Formatting.BOLD), false);
		syncNow(p);
		sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
	}

	// -------------------------------------------------------------- transform

	private static void toggleTransform(ServerPlayerEntity p, GhostData d) {
		if (!d.powers) {
			bar(p, "You don't have ghost powers yet. Activate a Ghost Portal!", Formatting.RED);
			return;
		}
		if (d.transformed) {
			endForm(p, d);
			transformRings(p);
			bar(p, "Back to human form.", Formatting.GRAY);
			sound(p, SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.4f);
			return;
		}
		if (d.exhaust > 0) {
			bar(p, "Too drained from the wail... (" + (d.exhaust / 20 + 1) + "s)", Formatting.RED);
			return;
		}
		d.transformed = true;
		setFlight(p, true);
		transformRings(p);
		bar(p, "I'm going ghost!", Formatting.GREEN);
		sound(p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.6f);
	}

	/** Leave ghost form and switch off any active power. */
	public static void endForm(ServerPlayerEntity p, GhostData d) {
		d.transformed = false;
		stopIntangible(p, d, false);
		stopPossess(p, d, false);
		releaseHold(p, d, false);
		stopInvisible(p, d);
		setFlight(p, false);
	}

	private static void setFlight(ServerPlayerEntity p, boolean on) {
		PlayerAbilities a = p.getAbilities();
		if (on) {
			if (!a.allowFlying) {
				a.allowFlying = true;
				p.sendAbilitiesUpdate();
			}
		} else if (!p.isCreative() && !p.isSpectator()) {
			a.allowFlying = false;
			a.flying = false;
			p.sendAbilitiesUpdate();
		}
	}

	/** The two glowing rings that sweep over Danny when he changes. */
	private static void transformRings(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		for (int ring = 0; ring < 2; ring++) {
			double y = p.getY() + (ring == 0 ? 0.1 : p.getHeight() - 0.1);
			for (int i = 0; i < 28; i++) {
				double a = i * Math.PI * 2 / 28;
				double x = p.getX() + Math.cos(a) * 0.75;
				double z = p.getZ() + Math.sin(a) * 0.75;
				w.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
				w.spawnParticles(PALE, x, y, z, 1, 0, 0, 0, 0);
			}
		}
	}

	// ------------------------------------------------------------------ blast

	private static void blast(ServerPlayerEntity p, GhostData d) {
		if (d.blastCooldown > 0) return;
		if (!useEnergy(p, d, BLAST_COST)) return;
		d.blastCooldown = 8;

		ServerWorld w = p.getServerWorld();
		Vec3d start = p.getEyePos();
		Vec3d dir = p.getRotationVec(1f);
		Entity hit = aimEntity(p, d, 32);
		Vec3d end = hit != null ? hit.getBoundingBox().getCenter() : aimBlockPos(p, 32);

		Vec3d right = dir.crossProduct(new Vec3d(0, 1, 0));
		if (right.lengthSquared() < 1.0E-4) right = new Vec3d(1, 0, 0);
		Vec3d from = start.add(dir.multiply(0.6)).add(right.normalize().multiply(0.3)).add(0, -0.25, 0);
		BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) end.x, (float) end.y, (float) end.z);
		for (ServerPlayerEntity viewer : PlayerLookup.around(w, start, 96)) {
			if (ServerPlayNetworking.canSend(viewer, BeamPayload.ID)) ServerPlayNetworking.send(viewer, beam);
		}
		sound(p, SoundEvents.ENTITY_BLAZE_SHOOT, 0.8f, 1.7f);

		if (hit != null) {
			hit.damage(w.getDamageSources().indirectMagic(p, p), 7.0f);
			if (hit instanceof LivingEntity le) le.takeKnockback(0.6, -dir.x, -dir.z);
		}
	}

	// ---------------------------------------------------------- intangibility

	private static void intangibility(ServerPlayerEntity p, GhostData d) {
		if (p.isSneaking()) {
			if (d.holding == null && !HELD_BLOCKS.containsKey(p.getUuid())) phaseTarget(p, d);
			return;
		}
		if (d.intangible) {
			stopIntangible(p, d, true);
			return;
		}
		if (d.energy < 10) {
			bar(p, "Not enough ghost energy!", Formatting.RED);
			return;
		}
		d.intangible = true;
		GhostFlags.SELF.add(p.getUuid());
		bar(p, "Intangible! Walk through walls. Sneak to sink through the floor, fly up to rise. G again to stop.", Formatting.AQUA);
		sound(p, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);
		p.getServerWorld().spawnParticles(PALE, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0);
	}

	public static void stopIntangible(ServerPlayerEntity p, GhostData d, boolean message) {
		if (!d.intangible) return;
		d.intangible = false;
		GhostFlags.SELF.remove(p.getUuid());
		if (!GhostFlags.TEMP.containsKey(p.getUuid())) pushOut(p);
		if (message) bar(p, "You're solid again.", Formatting.GRAY);
		d.syncTimer = 0;
	}

	/** Sneak + hold G: keep the mob, player or wall you're looking at intangible for as long as you hold on. */
	private static void phaseTarget(ServerPlayerEntity p, GhostData d) {
		if (d.energy < PHASE_TARGET_COST) {
			bar(p, "Not enough ghost energy!", Formatting.RED);
			return;
		}
		ServerWorld w = p.getServerWorld();
		Entity target = aimEntity(p, d, 16);
		if (target != null && !(target instanceof EnderDragonEntity)) {
			d.energy -= PHASE_TARGET_COST;
			GhostFlags.TEMP.put(target.getUuid(), new GhostFlags.TempPhase(target, 600));
			d.holding = target;
			w.spawnParticles(PALE, target.getX(), target.getY() + target.getHeight() / 2, target.getZ(), 30, 0.4, 0.6, 0.4, 0);
			sound(p, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);
			bar(p, "Holding " + target.getName().getString() + " intangible. Let go of the key to release.", Formatting.AQUA);
			if (target instanceof ServerPlayerEntity tp) {
				tp.sendMessage(Text.literal(p.getName().getString() + " turned you intangible!").formatted(Formatting.AQUA), true);
				syncNow(tp);
			}
			return;
		}

		BlockHitResult bh = w.raycast(new RaycastContext(p.getEyePos(), p.getEyePos().add(p.getRotationVec(1f).multiply(8)),
				RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, p));
		if (bh.getType() != HitResult.Type.BLOCK) {
			bar(p, "Look at a mob, player or wall to phase it.", Formatting.GRAY);
			return;
		}
		int phased = 0;
		List<PhasedBlock> held = new ArrayList<>();
		BlockPos hitPos = bh.getBlockPos();
		for (BlockPos pos : new BlockPos[]{hitPos.down(), hitPos, hitPos.up()}) {
			BlockState s = w.getBlockState(pos);
			if (s.isAir() || s.hasBlockEntity() || s.isOf(ModBlocks.GHOST_PORTAL)) continue;
			if (s.getHardness(w, pos) < 0) continue;
			held.add(new PhasedBlock(w, pos.toImmutable(), s, 600));
			w.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			w.spawnParticles(PALE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0);
			phased++;
		}
		if (phased > 0) {
			HELD_BLOCKS.put(p.getUuid(), held);
			d.energy -= PHASE_TARGET_COST;
			sound(p, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);
			bar(p, "Holding the wall intangible. Let go of the key to make it solid again.", Formatting.AQUA);
		}
	}

	/** Let go of whatever you're holding intangible. */
	public static void releaseHold(ServerPlayerEntity p, GhostData d, boolean message) {
		boolean released = false;
		if (d.holding != null) {
			Entity e = d.holding;
			d.holding = null;
			GhostFlags.TempPhase tp = GhostFlags.TEMP.get(e.getUuid());
			if (tp != null) tp.ticks = 0; // expires next tick (pushes them out of walls)
			released = true;
		}
		List<PhasedBlock> blocks = HELD_BLOCKS.remove(p.getUuid());
		if (blocks != null) {
			for (PhasedBlock b : blocks) b.restore();
			released = true;
		}
		if (released && message) bar(p, "Released.", Formatting.GRAY);
	}

	private static void tickHold(ServerPlayerEntity p, GhostData d) {
		boolean holding = d.holding != null || HELD_BLOCKS.containsKey(p.getUuid());
		if (!holding) return;
		if (d.holding != null) {
			Entity e = d.holding;
			if (e.isRemoved() || !e.isAlive() || e.getWorld() != p.getWorld() || e.squaredDistanceTo(p) > 20 * 20
					|| !GhostFlags.TEMP.containsKey(e.getUuid())) {
				releaseHold(p, d, true);
				return;
			}
		}
		List<PhasedBlock> blocks = HELD_BLOCKS.get(p.getUuid());
		if (blocks != null && (blocks.get(0).world() != p.getWorld() || blocks.get(0).pos().getSquaredDistance(p.getPos()) > 20 * 20)) {
			releaseHold(p, d, true);
			return;
		}
		d.energy -= 0.3f;
		if (d.energy <= 0) {
			d.energy = 0;
			releaseHold(p, d, false);
			bar(p, "Out of ghost energy, you lost your grip!", Formatting.RED);
		}
	}

	// ----------------------------------------------------------- invisibility

	private static void toggleInvisible(ServerPlayerEntity p, GhostData d) {
		if (d.invisible) {
			stopInvisible(p, d);
			bar(p, "You're visible again.", Formatting.GRAY);
			return;
		}
		if (d.energy < 10) {
			bar(p, "Not enough ghost energy!", Formatting.RED);
			return;
		}
		d.invisible = true;
		GhostFlags.INVISIBLE.add(p.getUuid());
		broadcastInvisible(p.getServer());
		p.getServerWorld().spawnParticles(PALE, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0);
		sound(p, SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.3f);
		bar(p, "Invisible! Press J again to reappear.", Formatting.AQUA);
	}

	public static void stopInvisible(ServerPlayerEntity p, GhostData d) {
		if (!d.invisible) return;
		d.invisible = false;
		GhostFlags.INVISIBLE.remove(p.getUuid());
		p.removeStatusEffect(StatusEffects.INVISIBILITY);
		broadcastInvisible(p.getServer());
		d.syncTimer = 0;
	}

	public static void broadcastInvisible(MinecraftServer server) {
		InvisiblePayload payload = new InvisiblePayload(new ArrayList<>(GhostFlags.INVISIBLE));
		for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
			if (ServerPlayNetworking.canSend(viewer, InvisiblePayload.ID)) ServerPlayNetworking.send(viewer, payload);
		}
	}

	// ------------------------------------------------------------- possession

	private static void possession(ServerPlayerEntity p, GhostData d, GhostState st) {
		if (d.possessing != null) {
			stopPossess(p, d, true);
			return;
		}
		if (d.energy < POSSESS_START_COST + 5) {
			bar(p, "Not enough ghost energy!", Formatting.RED);
			return;
		}
		Entity target = aimEntity(p, d, 16);
		if (!(target instanceof LivingEntity) || target instanceof EnderDragonEntity) {
			bar(p, "Look at a mob or player to possess it.", Formatting.GRAY);
			return;
		}
		if (GhostFlags.HOSTS.contains(target.getUuid())) {
			bar(p, "Someone is already possessing that!", Formatting.RED);
			return;
		}
		if (target instanceof ServerPlayerEntity tp && st.data(tp.getUuid()).intangible) {
			bar(p, "They're intangible, you can't grab them!", Formatting.RED);
			return;
		}
		stopIntangible(p, d, false);
		d.energy -= POSSESS_START_COST;
		d.possessing = target;
		d.possessTicks = 0;
		GhostFlags.HOSTS.add(target.getUuid());
		if (target instanceof MobEntity mob) {
			d.prevAiDisabled = mob.isAiDisabled();
			mob.setAiDisabled(true);
		}
		ServerWorld w = p.getServerWorld();
		p.teleport(w, target.getX(), target.getY(), target.getZ(), target.getYaw(), target.getPitch());
		w.spawnParticles(GREEN, target.getX(), target.getY() + target.getHeight() / 2, target.getZ(), 40, 0.4, 0.6, 0.4, 0);
		sound(p, SoundEvents.ENTITY_VEX_CHARGE, 1.0f, 0.7f);
		bar(p, "You possessed " + target.getName().getString() + "! Move to control it. Press H to leave.", Formatting.GREEN);
		if (target instanceof ServerPlayerEntity tp) {
			tp.sendMessage(Text.literal("You've been possessed by " + p.getName().getString() + "!").formatted(Formatting.GREEN), true);
		}
	}

	private static void tickPossession(ServerPlayerEntity p, GhostData d) {
		Entity t = d.possessing;
		if (t == null) return;
		if (t.isRemoved() || !t.isAlive() || t.getWorld() != p.getWorld() || !p.isAlive()) {
			stopPossess(p, d, true);
			return;
		}
		d.possessTicks++;
		if (t instanceof PlayerEntity && d.possessTicks > 200) {
			stopPossess(p, d, false);
			bar(p, "They forced you out!", Formatting.YELLOW);
			return;
		}
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 10, 0, false, false, false));
		if (t instanceof ServerPlayerEntity tp) {
			tp.teleport(p.getServerWorld(), p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
		} else {
			t.refreshPositionAndAngles(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
			t.setHeadYaw(p.getHeadYaw());
			t.setBodyYaw(p.getYaw());
			t.setVelocity(Vec3d.ZERO);
		}
		t.fallDistance = 0;
		if (d.possessTicks % 4 == 0) {
			p.getServerWorld().spawnParticles(GREEN, t.getX(), t.getY() + t.getHeight() * 0.6, t.getZ(), 3, 0.3, 0.4, 0.3, 0);
		}
	}

	public static void stopPossess(ServerPlayerEntity p, GhostData d, boolean message) {
		Entity t = d.possessing;
		if (t == null) return;
		d.possessing = null;
		GhostFlags.HOSTS.remove(t.getUuid());
		if (t instanceof MobEntity mob) mob.setAiDisabled(d.prevAiDisabled);
		if (!(t instanceof PlayerEntity)) t.noClip = false;
		if (t.isAlive() && !t.isRemoved()) pushOut(t);
		p.removeStatusEffect(StatusEffects.INVISIBILITY);
		if (p.isAlive()) pushOut(p);
		if (message) bar(p, "You left your host's body.", Formatting.GRAY);
		d.syncTimer = 0;
	}

	// ------------------------------------------------------------------- wail

	private static void wail(ServerPlayerEntity p, GhostData d) {
		if (d.wailCooldown > 0) {
			bar(p, "Your voice needs to recover... (" + (d.wailCooldown / 20 + 1) + "s)", Formatting.RED);
			return;
		}
		if (!useEnergy(p, d, WAIL_COST)) return;
		d.wailCooldown = 400;
		WAVES.add(new WailWave(p.getServerWorld(), p.getUuid(), p.getEyePos(), p.getRotationVec(1f)));
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 3.0f, 0.8f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 3.0f, 0.6f);
		bar(p, "GHOSTLY WAIL!", Formatting.GREEN);

		// Like the show: the wail drains you right back to human form.
		endForm(p, d);
		d.exhaust = 100;
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 120, 0, false, false, true));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1, false, false, true));
	}

	private static final class WailWave {
		final ServerWorld world;
		final UUID owner;
		final Vec3d origin;
		final Vec3d dir;
		final Set<UUID> hit = new HashSet<>();
		int radius;
		int broken;

		WailWave(ServerWorld world, UUID owner, Vec3d origin, Vec3d dir) {
			this.world = world;
			this.owner = owner;
			this.origin = origin;
			this.dir = dir.normalize();
		}

		/** Returns true when finished. */
		boolean tick() {
			radius++;
			if (radius > 16) return true;
			Entity ownerEntity = world.getServer().getPlayerManager().getPlayer(owner);
			double cone = 0.80;

			// Visuals: a sonic ring travelling outwards
			Vec3d center = origin.add(dir.multiply(radius));
			world.spawnParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, 0, 0, 0, 0);
			Vec3d u = dir.crossProduct(new Vec3d(0, 1, 0));
			if (u.lengthSquared() < 1.0E-4) u = new Vec3d(1, 0, 0);
			u = u.normalize();
			Vec3d v = dir.crossProduct(u).normalize();
			double ringRadius = radius * 0.6;
			int points = 10 + radius * 3;
			for (int i = 0; i < points; i++) {
				double a = i * Math.PI * 2 / points;
				Vec3d pt = center.add(u.multiply(Math.cos(a) * ringRadius)).add(v.multiply(Math.sin(a) * ringRadius));
				world.spawnParticles(GREEN, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
			}

			// Break blocks in this shell of the cone
			BlockPos o = BlockPos.ofFloored(origin);
			BlockPos.Mutable m = new BlockPos.Mutable();
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dy = -radius; dy <= radius; dy++) {
					for (int dz = -radius; dz <= radius; dz++) {
						if (broken >= 500) break;
						m.set(o.getX() + dx, o.getY() + dy, o.getZ() + dz);
						Vec3d diff = new Vec3d(m.getX() + 0.5 - origin.x, m.getY() + 0.5 - origin.y, m.getZ() + 0.5 - origin.z);
						double dist = diff.length();
						if (dist < radius - 1 || dist >= radius || dist < 1.5) continue;
						if (diff.multiply(1.0 / dist).dotProduct(dir) < cone) continue;
						BlockState s = world.getBlockState(m);
						if (s.isAir() || s.hasBlockEntity()) continue;
						if (s.isOf(ModBlocks.GHOST_PORTAL) || s.isOf(Blocks.BLACK_CONCRETE) || s.isOf(Blocks.IRON_BLOCK)) continue;
						float hardness = s.getHardness(world, m);
						if (hardness < 0 || hardness > 3.5f) continue;
						world.breakBlock(m.toImmutable(), world.random.nextFloat() < 0.25f, ownerEntity);
						broken++;
					}
				}
			}

			// Blast away entities caught in this shell
			Box box = new Box(origin, origin).expand(radius + 1);
			for (Entity e : world.getOtherEntities(ownerEntity, box)) {
				if (hit.contains(e.getUuid()) || !(e instanceof LivingEntity)) continue;
				Vec3d diff = e.getBoundingBox().getCenter().subtract(origin);
				double dist = diff.length();
				if (dist > radius + 0.5 || dist < 0.1) continue;
				if (diff.multiply(1.0 / dist).dotProduct(dir) < cone - 0.1) continue;
				hit.add(e.getUuid());
				DamageSource src = ownerEntity != null
						? world.getDamageSources().sonicBoom(ownerEntity)
						: world.getDamageSources().magic();
				e.damage(src, 10.0f);
				e.addVelocity(dir.x * 2.0, 0.4 + Math.max(0, dir.y), dir.z * 2.0);
				e.velocityModified = true;
			}
			return false;
		}
	}

	// ---------------------------------------------------------------- tornado

	private static void tornado(ServerPlayerEntity p, GhostData d) {
		if (d.tornadoCooldown > 0) {
			bar(p, "The tornado needs to recharge... (" + (d.tornadoCooldown / 20 + 1) + "s)", Formatting.RED);
			return;
		}
		if (!useEnergy(p, d, TORNADO_COST)) return;
		d.tornadoCooldown = 1200;
		ServerWorld w = p.getServerWorld();
		Vec3d look = p.getRotationVec(1f);
		Vec3d flat = new Vec3d(look.x, 0, look.z);
		if (flat.lengthSquared() < 1.0E-4) flat = new Vec3d(0, 0, 1);
		flat = flat.normalize();
		TORNADOES.add(new Tornado(w, p.getUuid(), p.getPos().add(flat.multiply(6)), flat));
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 3.0f, 0.5f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 3.0f, 0.4f);
		bar(p, "WAIL TORNADO!", Formatting.GREEN);

		endForm(p, d);
		d.exhaust = 160;
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 160, 0, false, false, true));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 140, 1, false, false, true));
	}

	/** A giant spinning column of wail energy that roams forward, sucking up mobs and ripping up blocks. */
	private static final class Tornado {
		final ServerWorld world;
		final UUID owner;
		final Vec3d dir;
		Vec3d pos;
		int age;
		int lifted;

		Tornado(ServerWorld world, UUID owner, Vec3d pos, Vec3d dir) {
			this.world = world;
			this.owner = owner;
			this.pos = pos;
			this.dir = dir;
		}

		boolean tick() {
			age++;
			Entity ownerEntity = world.getServer().getPlayerManager().getPlayer(owner);
			if (age > 200) {
				finalBurst(ownerEntity);
				return true;
			}

			// Drift forward and hug the ground
			pos = pos.add(dir.multiply(0.12));
			BlockPos bp = BlockPos.ofFloored(pos);
			for (int dy = 4; dy >= -10; dy--) {
				BlockPos q = bp.up(dy);
				if (!world.getBlockState(q).getCollisionShape(world, q).isEmpty()) {
					pos = new Vec3d(pos.x, q.getY() + 1, pos.z);
					break;
				}
			}

			// The spinning funnel
			double spin = age * 0.45;
			for (int h = 0; h < 15; h++) {
				double r = 0.5 + h * 0.38;
				for (int k = 0; k < 3; k++) {
					double a = spin + h * 0.55 + k * (Math.PI * 2 / 3);
					double x = pos.x + Math.cos(a) * r;
					double z = pos.z + Math.sin(a) * r;
					world.spawnParticles(k == 0 ? PALE : GREEN, x, pos.y + h, z, 1, 0, 0, 0, 0);
				}
			}
			if (age % 4 == 0) {
				world.spawnParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y + 1 + world.random.nextInt(12), pos.z, 1, 0, 0, 0, 0);
				world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 6, 1.2, 0.2, 1.2, 0.05);
			}
			if (age % 20 == 1) {
				world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 2.5f, 0.6f);
				world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 1.5f, 0.5f);
			}

			// Suck in and spin mobs, players and items
			Box box = new Box(pos.x - 9, pos.y - 2, pos.z - 9, pos.x + 9, pos.y + 18, pos.z + 9);
			for (Entity e : world.getOtherEntities(ownerEntity, box)) {
				if (e.isSpectator() || !(e instanceof LivingEntity || e instanceof ItemEntity)) continue;
				if (e instanceof PlayerEntity pl && pl.isCreative()) continue;
				Vec3d to = new Vec3d(pos.x - e.getX(), 0, pos.z - e.getZ());
				double dist = to.length();
				if (dist > 9) continue;
				Vec3d in = dist > 0.01 ? to.multiply(1.0 / dist) : Vec3d.ZERO;
				Vec3d around = new Vec3d(-in.z, 0, in.x);
				double pull = 0.14 * (1 - dist / 10);
				double lift = e.getY() < pos.y + 12 ? 0.13 : -0.03;
				Vec3d v = e.getVelocity().add(in.x * pull + around.x * 0.2 + dir.x * 0.04, lift, in.z * pull + around.z * 0.2 + dir.z * 0.04);
				if (v.length() > 1.4) v = v.normalize().multiply(1.4);
				e.setVelocity(v);
				e.velocityModified = true;
				e.fallDistance = 0;
				if (age % 10 == 0 && dist < 4 && e instanceof LivingEntity) {
					DamageSource src = ownerEntity != null
							? world.getDamageSources().indirectMagic(ownerEntity, ownerEntity)
							: world.getDamageSources().magic();
					e.damage(src, 3.0f);
				}
			}

			// Rip up the ground and fling it
			if (age % 2 == 0 && lifted < 90) {
				for (int i = 0; i < 3; i++) {
					BlockPos q = BlockPos.ofFloored(pos).add(world.random.nextInt(7) - 3, world.random.nextInt(3) - 1, world.random.nextInt(7) - 3);
					BlockState s = world.getBlockState(q);
					if (s.isAir() || s.hasBlockEntity() || !s.getFluidState().isEmpty()) continue;
					if (s.isOf(ModBlocks.GHOST_PORTAL) || s.isOf(Blocks.BLACK_CONCRETE) || s.isOf(Blocks.IRON_BLOCK)) continue;
					float hardness = s.getHardness(world, q);
					if (hardness < 0 || hardness > 2.0f) continue;
					FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, q, s);
					fb.setVelocity((world.random.nextDouble() - 0.5) * 0.6, 0.9 + world.random.nextDouble() * 0.5, (world.random.nextDouble() - 0.5) * 0.6);
					fb.velocityModified = true;
					lifted++;
				}
			}
			return false;
		}

		private void finalBurst(Entity ownerEntity) {
			world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 2, pos.z, 1, 0, 0, 0, 0);
			world.spawnParticles(GREEN, pos.x, pos.y + 4, pos.z, 120, 3, 4, 3, 0);
			world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 3.0f, 0.7f);
			Box box = new Box(pos.x - 9, pos.y - 2, pos.z - 9, pos.x + 9, pos.y + 20, pos.z + 9);
			for (Entity e : world.getOtherEntities(ownerEntity, box)) {
				if (e.isSpectator() || !(e instanceof LivingEntity || e instanceof ItemEntity)) continue;
				Vec3d out = new Vec3d(e.getX() - pos.x, 0, e.getZ() - pos.z);
				out = out.lengthSquared() < 1.0E-4 ? dir : out.normalize();
				e.setVelocity(out.x * 1.6, 0.7, out.z * 1.6);
				e.velocityModified = true;
			}
		}
	}

	// ------------------------------------------------------------------- tick

	public static void tick(MinecraftServer server) {
		GhostState st = GhostState.of(server);
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			tickPlayer(p, st.data(p.getUuid()), st);
		}
		tickTempPhases();
		tickPhasedBlocks();
		WAVES.removeIf(WailWave::tick);
		TORNADOES.removeIf(Tornado::tick);
	}

	private static void tickPlayer(ServerPlayerEntity p, GhostData d, GhostState st) {
		if (d.blastCooldown > 0) d.blastCooldown--;
		if (d.wailCooldown > 0) d.wailCooldown--;
		if (d.tornadoCooldown > 0) d.tornadoCooldown--;
		if (d.exhaust > 0) d.exhaust--;

		if (d.powers && d.transformed && p.isAlive()) {
			d.formTicks++;
			if (d.formTicks % POINT_TICKS == 0) {
				d.points++;
				st.markDirty();
				p.sendMessage(Text.literal("+1 Stat Point! (" + d.points + " total)").formatted(Formatting.GREEN), true);
				sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
			}
			setFlight(p, true);

			float drain = 0;
			if (d.intangible) drain += 0.4f;
			if (d.invisible) drain += 0.25f;
			if (d.possessing != null) drain += 0.4f;
			if (drain > 0) {
				d.energy -= drain;
				if (d.energy <= 0) {
					d.energy = 0;
					stopIntangible(p, d, false);
					stopPossess(p, d, false);
					stopInvisible(p, d);
					bar(p, "Out of ghost energy!", Formatting.RED);
				}
			} else {
				d.energy = Math.min(MAX_ENERGY, d.energy + 0.3f);
			}

			if (d.invisible) {
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 10, 0, false, false, false));
			}
			if (p.age % 6 == 0 && d.possessing == null && !d.invisible) {
				ServerWorld w = p.getServerWorld();
				w.spawnParticles(GREEN, p.getX(), p.getY() + p.getHeight() * 0.5, p.getZ(), 2, 0.3, 0.5, 0.3, 0);
			}
			if (d.intangible && p.age % 3 == 0) {
				p.getServerWorld().spawnParticles(PALE, p.getX(), p.getY() + 1, p.getZ(), 2, 0.3, 0.6, 0.3, 0);
			}
		} else {
			d.energy = Math.min(MAX_ENERGY, d.energy + 0.5f);
		}

		tickPossession(p, d);
		tickHold(p, d);

		if (--d.syncTimer <= 0) {
			d.syncTimer = 2;
			sync(p, d);
		}
	}

	private static void tickTempPhases() {
		Iterator<Map.Entry<UUID, GhostFlags.TempPhase>> it = GhostFlags.TEMP.entrySet().iterator();
		while (it.hasNext()) {
			GhostFlags.TempPhase tp = it.next().getValue();
			Entity e = tp.entity;
			tp.ticks--;
			if (e.isRemoved() || tp.ticks <= 0) {
				it.remove();
				if (!(e instanceof PlayerEntity)) e.noClip = false;
				if (!e.isRemoved() && e.isAlive() && !GhostFlags.SELF.contains(e.getUuid())) pushOut(e);
				if (e instanceof ServerPlayerEntity sp) syncNow(sp);
			} else if (tp.ticks % 3 == 0 && e.getWorld() instanceof ServerWorld w) {
				w.spawnParticles(PALE, e.getX(), e.getY() + e.getHeight() / 2, e.getZ(), 2, 0.3, 0.4, 0.3, 0);
			}
		}
	}

	private record PhasedBlock(ServerWorld world, BlockPos pos, BlockState state, int[] ticks) {
		PhasedBlock(ServerWorld world, BlockPos pos, BlockState state, int ticks) {
			this(world, pos, state, new int[]{ticks});
		}

		void restore() {
			if (world.getBlockState(pos).isAir()) {
				world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
			}
		}
	}

	private static void tickPhasedBlocks() {
		HELD_BLOCKS.values().removeIf(list -> {
			if (--list.get(0).ticks()[0] > 0) return false;
			for (PhasedBlock b : list) b.restore();
			return true;
		});
		Iterator<PhasedBlock> it = PHASED.iterator();
		while (it.hasNext()) {
			PhasedBlock b = it.next();
			if (--b.ticks()[0] <= 0) {
				b.restore();
				it.remove();
			}
		}
	}

	// ----------------------------------------------------------------- events

	public static boolean allowDamage(LivingEntity entity, DamageSource source) {
		if (entity.getWorld().isClient) return true;
		UUID id = entity.getUuid();
		if (GhostFlags.SELF.contains(id) || GhostFlags.TEMP.containsKey(id)) return false;
		MinecraftServer server = entity.getServer();
		if (server == null) return true;
		GhostState st = GhostState.of(server);
		if (entity instanceof ServerPlayerEntity p && st.data(p.getUuid()).possessing != null) return false;
		if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
			GhostData ad = st.data(attacker.getUuid());
			if (ad.possessing == entity) return false;
		}
		return true;
	}

	public static void onDeath(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		d.transformed = false;
		d.intangible = false;
		GhostFlags.SELF.remove(p.getUuid());
		GhostFlags.TEMP.remove(p.getUuid());
		releaseHold(p, d, false);
		if (d.invisible) {
			d.invisible = false;
			GhostFlags.INVISIBLE.remove(p.getUuid());
			broadcastInvisible(p.getServer());
		}
		if (d.possessing != null) {
			Entity t = d.possessing;
			d.possessing = null;
			GhostFlags.HOSTS.remove(t.getUuid());
			if (t instanceof MobEntity mob) mob.setAiDisabled(d.prevAiDisabled);
			if (!(t instanceof PlayerEntity)) t.noClip = false;
		}
		st.markDirty();
	}

	public static void onAnyDeath(LivingEntity entity) {
		GhostFlags.TEMP.remove(entity.getUuid());
	}

	public static void onRespawn(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		d.transformed = false;
		d.intangible = false;
		d.syncTimer = 0;
		if (d.pendingPowers) {
			UUID id = p.getUuid();
			MinecraftServer server = p.getServer();
			PortalLogic.later(() -> {
				ServerPlayerEntity cur = server.getPlayerManager().getPlayer(id);
				if (cur != null) PortalLogic.finishGhostRebirth(cur);
			});
		}
	}

	public static void onJoin(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		if (d.transformed) endForm(p, d);
		d.syncTimer = 0;
		broadcastInvisible(p.getServer());
		if (d.pendingPowers && p.isAlive()) {
			PortalLogic.later(() -> PortalLogic.finishGhostRebirth(p));
		}
	}

	public static void onLeave(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		endForm(p, d);
		GhostFlags.TEMP.remove(p.getUuid());
		GhostFlags.HOSTS.remove(p.getUuid());
		st.markDirty();
	}

	public static void onServerStopping() {
		for (PhasedBlock b : PHASED) b.restore();
		PHASED.clear();
		for (List<PhasedBlock> list : HELD_BLOCKS.values()) for (PhasedBlock b : list) b.restore();
		HELD_BLOCKS.clear();
		WAVES.clear();
		TORNADOES.clear();
		GhostFlags.clearAll();
	}

	// ---------------------------------------------------------------- helpers

	public static void grantPowers(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		d.powers = true;
		d.pendingPowers = false;
		d.unlocked |= 1 << BLAST;
		d.energy = MAX_ENERGY;
		st.markDirty();
		syncNow(p);
	}

	public static void syncNow(ServerPlayerEntity p) {
		GhostData d = GhostState.of(p.getServer()).data(p.getUuid());
		sync(p, d);
	}

	private static void sync(ServerPlayerEntity p, GhostData d) {
		if (!ServerPlayNetworking.canSend(p, SyncPayload.ID)) return;
		boolean intangible = d.intangible || GhostFlags.TEMP.containsKey(p.getUuid());
		ServerPlayNetworking.send(p, new SyncPayload(d.powers, d.transformed, intangible, d.possessing != null, d.invisible,
				d.energy, d.points, d.formTicks % POINT_TICKS, d.unlocked, d.wailCooldown, d.tornadoCooldown, d.exhaust));
	}

	private static boolean useEnergy(ServerPlayerEntity p, GhostData d, float amount) {
		if (d.energy < amount) {
			bar(p, "Not enough ghost energy!", Formatting.RED);
			return false;
		}
		d.energy -= amount;
		return true;
	}

	/** Finds the entity the player is aiming at (ignores the body they're possessing). */
	private static Entity aimEntity(ServerPlayerEntity p, GhostData d, double range) {
		Vec3d start = p.getEyePos();
		Vec3d end = aimBlockPos(p, range);
		Entity host = d.possessing;
		Box box = p.getBoundingBox().stretch(end.subtract(start)).expand(1.0);
		EntityHitResult eh = ProjectileUtil.raycast(p, start, end, box,
				e -> e != host && e != p && !e.isSpectator() && e.canHit(), start.squaredDistanceTo(end));
		return eh == null ? null : eh.getEntity();
	}

	private static Vec3d aimBlockPos(ServerPlayerEntity p, double range) {
		Vec3d start = p.getEyePos();
		Vec3d end = start.add(p.getRotationVec(1f).multiply(range));
		BlockHitResult bh = p.getServerWorld().raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
		return bh.getType() == HitResult.Type.MISS ? end : bh.getPos();
	}

	/** Moves an entity up until it isn't stuck inside blocks. */
	public static void pushOut(Entity e) {
		if (!(e.getWorld() instanceof ServerWorld w)) return;
		for (int i = 0; i < 16 && !w.isSpaceEmpty(e); i++) {
			e.requestTeleport(e.getX(), MathHelper.floor(e.getY()) + 1.0, e.getZ());
		}
	}

	public static void bar(ServerPlayerEntity p, String msg, Formatting color) {
		p.sendMessage(Text.literal(msg).formatted(color), true);
	}

	private static void sound(ServerPlayerEntity p, SoundEvent s, float volume, float pitch) {
		p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundCategory.PLAYERS, volume, pitch);
	}

	@SuppressWarnings("unused")
	private static void particles(ServerWorld w, ParticleEffect effect, Vec3d at, int count) {
		w.spawnParticles(effect, at.x, at.y, at.z, count, 0.3, 0.3, 0.3, 0);
	}
}
