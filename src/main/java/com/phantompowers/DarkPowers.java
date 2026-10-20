package com.phantompowers;

import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.entity.BossMoves;
import com.phantompowers.entity.EctoConstructEntity;
import com.phantompowers.network.BeamPayload;
import com.phantompowers.network.TornadoPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * The dark half-ghost: how you become it (the prototype portal and its ecto acne) and its four powers:
 * Ecto Constructs, Tornado Spin, Ghost Stinger and Energy Absorption.
 */
public final class DarkPowers {
	public static final int ACNE_TICKS = 6000; // five minutes
	private static final float SPIN_DRAIN = 0.35f;

	private DarkPowers() {}

	// ------------------------------------------------------- becoming dark

	/** Prototype portal cutscene length, and the tick the blast goes off. */
	public static final int PORTAL_TICKS = 84, PORTAL_BLAST = 46;

	/** How far you've been pushed back from your starting spot (negative = leaning in toward the portal). Shared with the client. */
	public static double portalKnock(int elapsed) {
		if (elapsed < PORTAL_BLAST) return -Math.min(0.35, elapsed * 0.012);
		double t = elapsed - PORTAL_BLAST;
		return -0.35 + 3.0 * (1 - Math.exp(-t * 0.28));
	}

	/** The little hop as the blast throws you back. */
	public static double portalLift(int elapsed) {
		double t = elapsed - PORTAL_BLAST;
		return t > 0 && t < 11 ? Math.sin(Math.PI * t / 11.0) * 0.7 : 0;
	}

	/** Right-click a placed prototype portal: step up to it, it hums and charges... and blasts you right in the face. */
	public static void startPortalCutscene(ServerPlayerEntity p, net.minecraft.util.math.BlockPos pos, net.minecraft.util.math.Direction facing) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		if (d.cutscene > 0) return;
		if (d.transformed) GhostPowers.endForm(p, d);
		Vec3d front = Vec3d.ofBottomCenter(pos).add(facing.getOffsetX() * 2.3, 0, facing.getOffsetZ() * 2.3);
		float yaw = facing.getOpposite().asRotation();
		p.stopRiding();
		p.networkHandler.requestTeleport(front.x, front.y, front.z, yaw, 12f);
		p.setVelocity(Vec3d.ZERO);
		d.cutscene = PORTAL_TICKS;
		d.cutsceneKind = 3;
		d.cutsceneToGhost = false;
		d.cutsceneAnchor = front;
		d.portalCenter = Vec3d.ofCenter(pos);
		GhostPowers.syncNow(p);
	}

	public static void tickPortalCutscene(ServerPlayerEntity p, GhostData d) {
		d.cutscene--;
		int e = PORTAL_TICKS - d.cutscene;
		Vec3d c = d.portalCenter, a = d.cutsceneAnchor;
		if (c == null || a == null) {
			d.cutscene = 0;
			GhostPowers.syncNow(p);
			return;
		}
		ServerWorld w = p.getServerWorld();
		Vec3d back = new Vec3d(a.x - c.x, 0, a.z - c.z);
		back = back.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : back.normalize();
		Vec3d side = new Vec3d(-back.z, 0, back.x);
		Vec3d want = a.add(back.multiply(portalKnock(e))).add(0, portalLift(e), 0);
		// the client moves you smoothly along the path; only step in if you really drift off it
		if (p.getPos().squaredDistanceTo(want) > 16) p.requestTeleport(want.x, want.y, want.z);
		p.fallDistance = 0;
		Vec3d face = p.getEyePos();
		if (e < PORTAL_BLAST) {
			// it powers up: a swirl of ecto in the frame, crackling sparks, a rising hum
			float k = e / (float) PORTAL_BLAST;
			int n = 2 + (int) (k * 8);
			for (int i = 0; i < n; i++) {
				double ang = e * 0.45 + i * Math.PI * 2 / n;
				double r = 0.12 + 0.22 * ((e + i * 3) % 10) / 10.0;
				Vec3d at = c.add(side.multiply(Math.cos(ang) * r)).add(0, Math.sin(ang) * r * 1.4, 0);
				w.spawnParticles(GhostPowers.GREEN, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			}
			if (e % 3 == 0) w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 0.3, c.z, 1 + (int) (k * 5), 0.35, 0.45, 0.35, 0.15);
			if (e == 1) w.playSound(null, c.x, c.y, c.z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.2f, 0.5f);
			if (e % 8 == 0) w.playSound(null, c.x, c.y, c.z, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 1.4f, 0.6f + k * 1.3f);
			if (e == PORTAL_BLAST - 14) w.playSound(null, c.x, c.y, c.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1.3f, 1.2f);
			if (e == PORTAL_BLAST - 6) w.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.BLOCKS, 1.0f, 1.4f);
		} else if (e < PORTAL_BLAST + 8) {
			// KA-BLAM: a beam of raw ghost energy right in the face
			Vec3d from = c.add(back.multiply(0.15));
			beam(w, from, face, 0x55FF88);
			beam(w, from.add(0, 0.12, 0), face.add(side.multiply(0.1)), 0xD0FFE0);
			beam(w, from.add(0, -0.12, 0), face.add(side.multiply(-0.1)), 0x22DD66);
			if (e == PORTAL_BLAST) {
				w.spawnParticles(ParticleTypes.FLASH, face.x, face.y, face.z, 1, 0, 0, 0, 0);
				w.spawnParticles(GhostPowers.GREEN, face.x, face.y, face.z, 120, 0.4, 0.4, 0.4, 0.35);
				w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, face.x, face.y, face.z, 50, 0.3, 0.3, 0.3, 0.5);
				w.playSound(null, face.x, face.y, face.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 1.2f, 1.5f);
				w.playSound(null, face.x, face.y, face.z, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1.0f, 1.3f);
				w.playSound(null, face.x, face.y, face.z, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.2f, 0.6f);
			}
		} else if (e < PORTAL_BLAST + 32 && e % 2 == 0) {
			// smoking, singed and glowing
			w.spawnParticles(ParticleTypes.LARGE_SMOKE, face.x, face.y + 0.1, face.z, 1, 0.15, 0.1, 0.15, 0.01);
			w.spawnParticles(GhostPowers.GREEN, face.x, face.y, face.z, 2, 0.2, 0.2, 0.2, 0.01);
			if (e % 6 == 0) w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 3, 0.3, 0.6, 0.3, 0.1);
		}
		if (e == PORTAL_BLAST + 22) applyBlast(p, d);
		if (d.cutscene <= 0) {
			d.cutscene = 0;
			d.cutsceneAnchor = null;
			d.portalCenter = null;
			GhostPowers.syncNow(p);
		}
	}

	private static void beam(ServerWorld w, Vec3d from, Vec3d to, int color) {
		BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) to.x, (float) to.y, (float) to.z, color);
		for (ServerPlayerEntity viewer : PlayerLookup.around(w, from, 96)) {
			if (ServerPlayNetworking.canSend(viewer, BeamPayload.ID)) ServerPlayNetworking.send(viewer, beam);
		}
	}

	/** What the blast does to you: humans and half-ghosts alike get ecto acne, and five minutes later turn into the dark half-ghost. */
	public static void applyBlast(ServerPlayerEntity p, GhostData d) {
		GhostState st = GhostState.of(p.getServer());
		ServerWorld w = p.getServerWorld();
		p.damage(w.getDamageSources().magic(), Math.min(4f, Math.max(0f, p.getHealth() - 1f)));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 30, 0));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0));
		if (d.dark) {
			p.sendMessage(Text.literal("The prototype crackles... your dark side is already awake.").formatted(Formatting.DARK_RED), false);
			return;
		}
		if (d.acneTicks > 0) {
			p.sendMessage(Text.literal("Your ecto acne flares up even worse... just wait it out.").formatted(Formatting.GREEN), false);
			return;
		}
		d.acneTicks = ACNE_TICKS;
		st.markDirty();
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("ECTO ACNE!").formatted(Formatting.GREEN, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(d.powers
				? "The unstable blast is changing you... wait 5 minutes"
				: "Ghost energy is seeping into you... wait 5 minutes").formatted(Formatting.GRAY)));
		if (!d.powers) p.sendMessage(Text.literal("Your face is covered in glowing green zits... and something inside you is changing.").formatted(Formatting.GREEN), false);
		GhostPowers.broadcastStates(p.getServer());
		GhostPowers.syncNow(p);
	}

	/** Clear ecto acne. finish = skip the wait and turn dark right now; otherwise it just goes away (no change). */
	public static void endAcne(ServerPlayerEntity p, boolean finish) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		if (d.acneTicks <= 0 && !finish) return;
		d.acneTicks = 0;
		st.markDirty();
		if (finish) {
			if (!d.dark) finishAcne(p, d);
		} else {
			ServerWorld w = p.getServerWorld();
			w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getEyeY(), p.getZ(), 12, 0.3, 0.3, 0.3, 0);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_HONEY_BOTTLE_DRINK, SoundCategory.PLAYERS, 1f, 1.2f);
			p.sendMessage(Text.literal("Your ecto acne clears right up. Whatever it was doing to you has stopped.").formatted(Formatting.GREEN), false);
			GhostPowers.broadcastStates(p.getServer());
		}
		GhostPowers.syncNow(p);
	}

	private static void finishAcne(ServerPlayerEntity p, GhostData d) {
		boolean wasHuman = !d.powers;
		if (wasHuman) GhostPowers.grantPowers(p);
		d.dark = true;
		d.darkMode = true;
		d.suitColors = SuitColors.VILLAIN.clone();
		ServerWorld w = p.getServerWorld();
		w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 80, 0.6, 1, 0.6, 0.1);
		w.spawnParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1, 0.6, 0.05);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.4f);
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Your dark side awakens").formatted(Formatting.DARK_RED, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("New dark powers can be unlocked in Ghost Skills").formatted(Formatting.GRAY)));
		if (wasHuman) p.sendMessage(Text.literal("The blast turned you into a half-ghost... a DARK one. Press V to go ghost!").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
		p.sendMessage(Text.literal("The ecto acne is gone... and so is your old self. Go ghost to take your dark form. "
				+ "(You can switch back in Ghost Skills.)").formatted(Formatting.DARK_RED), false);
		GhostPowers.broadcastStates(p.getServer());
	}

	public static void toggleDarkMode(ServerPlayerEntity p, GhostData d) {
		if (!d.dark) return;
		d.darkMode = !d.darkMode;
		if (d.transformed) GhostPowers.endForm(p, d);
		GhostPowers.bar(p, d.darkMode ? "You'll go ghost in your DARK form." : "You'll go ghost in your hero form.", d.darkMode ? Formatting.DARK_RED : Formatting.GREEN);
		GhostPowers.broadcastStates(p.getServer());
		GhostPowers.syncNow(p);
	}

	// ----------------------------------------------------------------- tick

	public static void tick(ServerPlayerEntity p, GhostData d) {
		if (d.constructCooldown > 0) d.constructCooldown--;
		if (d.stingCooldown > 0) d.stingCooldown--;
		if (d.absorbCooldown > 0) d.absorbCooldown--;
		ServerWorld w = p.getServerWorld();
		if (d.acneTicks > 0) {
			d.acneTicks--;
			// the acne flares: little bursts, dizzy spells, and your powers glitch
			if (p.age % 120 == 0) w.spawnParticles(GhostPowers.GREEN, p.getX(), p.getEyeY(), p.getZ(), 8, 0.2, 0.15, 0.2, 0.02);
			if (p.age % 900 == 450) {
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0, false, false));
				GhostPowers.bar(p, "Your ecto acne throbs... (" + (d.acneTicks / 1200 + 1) + " min left)", Formatting.GREEN);
			}
			if (d.acneTicks % 1200 == 0 && d.acneTicks > 0) GhostPowers.syncNow(p);
			if (d.acneTicks == 0) finishAcne(p, d);
		}
		if (d.spinCooldown > 0) d.spinCooldown--;
		if (d.dashTarget != null) tickDash(p, d, w);
		if (d.stingTarget != null) tickSting(p, d, w);
		if (d.absorbTicks > 0) {
			d.absorbTicks--;
			Vec3d c = p.getPos().add(0, 1, 0);
			for (int i = 0; i < 3; i++) {
				double a = w.random.nextDouble() * Math.PI * 2, b = (w.random.nextDouble() - 0.5) * Math.PI;
				Vec3d from = c.add(Math.cos(a) * Math.cos(b) * 2.2, Math.sin(b) * 2.2, Math.sin(a) * Math.cos(b) * 2.2);
				Vec3d v = c.subtract(from).multiply(0.15);
				w.spawnParticles(ParticleTypes.END_ROD, from.x, from.y, from.z, 0, v.x, v.y, v.z, 1);
			}
			// soak up projectiles flying at you
			for (ProjectileEntity pr : w.getEntitiesByClass(ProjectileEntity.class, p.getBoundingBox().expand(3), pr -> pr.getOwner() != p)) {
				w.spawnParticles(ParticleTypes.END_ROD, pr.getX(), pr.getY(), pr.getZ(), 6, 0.1, 0.1, 0.1, 0.05);
				pr.discard();
				gain(p, d, 4f);
			}
			if (d.absorbTicks == 0) GhostPowers.bar(p, "Energy absorption fades.", Formatting.GRAY);
		}
	}

	public static void endForm(ServerPlayerEntity p, GhostData d) {
		d.spinning = false;
		d.stingTarget = null;
		d.dashTarget = null;
		d.absorbTicks = 0;
	}

	// ------------------------------------------------------- ecto constructs

	public static void construct(ServerPlayerEntity p, GhostData d) {
		if (d.constructCooldown > 0) {
			GhostPowers.bar(p, "Your constructs need a moment... (" + (d.constructCooldown / 20 + 1) + "s)", Formatting.GRAY);
			return;
		}
		if (d.energy < 25) {
			GhostPowers.bar(p, "Not enough ghost energy!", Formatting.RED);
			return;
		}
		d.energy -= 25;
		d.constructCooldown = 60;
		ServerWorld w = p.getServerWorld();
		Entity target = GhostPowers.aimEntity(p, d, 28);
		Vec3d at = target != null ? target.getPos() : GhostPowers.aimPoint(p, 28);
		at = BossMoves.ground(w, at);
		EctoConstructEntity c = com.phantompowers.ModEntities.ECTO_CONSTRUCT.create(w);
		if (c == null) return;
		c.setup(p, d.constructKind, at, target instanceof LivingEntity le ? le : null);
		d.constructKind = (d.constructKind + 1) % EctoConstructEntity.KINDS;
		w.spawnEntity(c);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.2f, 0.7f);
		w.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.2f, 0.8f);
	}

	// ----------------------------------------------------------- tornado spin

	/** Dark Tornado: fly round and round in a tight circle to whip up a twister, then send it off. */
	public static final int SPIN_TICKS = 56;
	public static final double SPIN_RADIUS = 3.2;
	/** The pink colour of every dark-form beam and ring. */
	public static final int PINK = 0xFF4FC8;

	/** Where the circling player should be at a given tick (shared with the client so it moves smoothly). */
	public static Vec3d spinPos(Vec3d center, int elapsed) {
		double a = elapsed * 0.42;
		double r = SPIN_RADIUS * Math.min(1.0, 0.4 + elapsed / 10.0);
		double lift = Math.min(2.2, elapsed * 0.06);
		return center.add(Math.cos(a) * r, lift, Math.sin(a) * r);
	}

	public static void toggleSpin(ServerPlayerEntity p, GhostData d) {
		if (d.cutscene > 0) return;
		if (d.spinCooldown > 0) {
			GhostPowers.bar(p, "Your next tornado needs a moment... (" + (d.spinCooldown / 20 + 1) + "s)", Formatting.GRAY);
			return;
		}
		if (d.energy < 30) {
			GhostPowers.bar(p, "Not enough ghost energy!", Formatting.RED);
			return;
		}
		d.energy -= 30;
		d.spinCooldown = 240;
		Vec3d look = p.getRotationVec(1f);
		Vec3d flat = new Vec3d(look.x, 0, look.z);
		flat = flat.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : flat.normalize();
		d.spinDir = flat;
		// circle around a spot just in front of you
		d.cutsceneAnchor = p.getPos().add(flat.multiply(SPIN_RADIUS));
		d.cutscene = SPIN_TICKS;
		d.cutsceneKind = 4;
		d.cutsceneToGhost = false;
		p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.WAIL_TORNADO, SoundCategory.PLAYERS, 1.2f, 0.8f);
		GhostPowers.syncNow(p);
	}

	public static void tickSpinCutscene(ServerPlayerEntity p, GhostData d) {
		d.cutscene--;
		int e = SPIN_TICKS - d.cutscene;
		Vec3d c = d.cutsceneAnchor;
		if (c == null) {
			d.cutscene = 0;
			return;
		}
		ServerWorld w = p.getServerWorld();
		Vec3d want = spinPos(c, e);
		if (p.getPos().squaredDistanceTo(want) > 25) p.requestTeleport(want.x, want.y, want.z);
		p.fallDistance = 0;
		float scale = 0.25f + 0.75f * e / (float) SPIN_TICKS;
		sendTwister(w, 1_000_000 + p.getId(), c, e, false, scale);
		// a pink streak follows you round the circle
		w.spawnParticles(PINK_DUST, p.getX(), p.getY() + 1, p.getZ(), 3, 0.2, 0.3, 0.2, 0);
		if (e % 6 == 0) w.playSound(null, c.x, c.y, c.z, SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.6f, 0.8f + e / 80f);
		pull(w, p, c, 2.5 + 3.0 * scale, e % 8 == 0 ? 3f : 0f);
		if (d.cutscene <= 0) {
			d.cutscene = 0;
			d.cutsceneAnchor = null;
			TWISTERS.add(new Twister(p.getUuid(), w, 1_000_000 + p.getId(), c, d.spinDir, 110));
			w.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.2f, 0.6f);
			GhostPowers.bar(p, "Your tornado tears off across the ground!", Formatting.LIGHT_PURPLE);
			GhostPowers.syncNow(p);
		}
	}

	private static final net.minecraft.particle.DustParticleEffect PINK_DUST =
			new net.minecraft.particle.DustParticleEffect(new org.joml.Vector3f(1f, 0.31f, 0.78f), 1.4f);

	/** A launched tornado roaming forward on its own. */
	private static final class Twister {
		final UUID owner;
		final ServerWorld world;
		final int id;
		Vec3d pos, dir;
		int life, age;

		Twister(UUID owner, ServerWorld world, int id, Vec3d pos, Vec3d dir, int life) {
			this.owner = owner;
			this.world = world;
			this.id = id;
			this.pos = pos;
			this.dir = dir;
			this.life = life;
		}
	}

	private static final java.util.List<Twister> TWISTERS = new java.util.ArrayList<>();

	/** Moves the launched tornadoes (called every server tick). */
	public static void tickTwisters() {
		var it = TWISTERS.iterator();
		while (it.hasNext()) {
			Twister t = it.next();
			t.age++;
			// drift forward, weaving a little, hugging the ground
			Vec3d side = new Vec3d(-t.dir.z, 0, t.dir.x).multiply(Math.sin(t.age * 0.15) * 0.12);
			Vec3d next = t.pos.add(t.dir.multiply(0.32)).add(side);
			Vec3d ground = com.phantompowers.entity.BossMoves.ground(t.world, next);
			t.pos = new Vec3d(next.x, ground.y, next.z);
			boolean done = t.age >= t.life;
			sendTwister(t.world, t.id, t.pos, SPIN_TICKS + t.age, done, done ? 1f : 1f);
			ServerPlayerEntity owner = t.world.getServer().getPlayerManager().getPlayer(t.owner);
			pull(t.world, owner, t.pos, 5.5, t.age % 8 == 0 ? 4f : 0f);
			if (t.age % 10 == 0) t.world.playSound(null, t.pos.x, t.pos.y, t.pos.z, SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.9f, 0.7f);
			if (done) it.remove();
		}
	}

	private static void sendTwister(ServerWorld w, int id, Vec3d at, int age, boolean done, float scale) {
		TornadoPayload msg = new TornadoPayload(id, at.x, at.y - 0.3, at.z, age, done, scale);
		for (ServerPlayerEntity v : PlayerLookup.around(w, at, 128)) {
			if (ServerPlayNetworking.canSend(v, TornadoPayload.ID)) ServerPlayNetworking.send(v, msg);
		}
	}

	/** Suck things in and whirl them round (never the tornado's owner or their clones). */
	private static void pull(ServerWorld w, ServerPlayerEntity owner, Vec3d c, double radius, float damage) {
		for (Entity e : w.getOtherEntities(owner, new Box(c, c).expand(radius, 6, radius), e -> (e instanceof LivingEntity || e instanceof net.minecraft.entity.ItemEntity)
				&& !(e instanceof com.phantompowers.entity.GhostCloneEntity) && !(e instanceof com.phantompowers.entity.PlayerCloneEntity))) {
			if (e instanceof PlayerEntity pl && pl.isCreative()) continue;
			Vec3d to = new Vec3d(c.x - e.getX(), 0, c.z - e.getZ());
			double dist = Math.max(0.3, to.length());
			if (dist > radius) continue;
			Vec3d in = to.multiply(1 / dist), around = new Vec3d(-in.z, 0, in.x);
			e.setVelocity(e.getVelocity().multiply(0.6).add(in.x * 0.18 + around.x * 0.5, 0.2, in.z * 0.18 + around.z * 0.5));
			e.velocityModified = true;
			e.fallDistance = 0;
			if (damage > 0 && e instanceof LivingEntity) e.damage(owner != null ? w.getDamageSources().indirectMagic(owner, owner) : w.getDamageSources().magic(), damage);
		}
	}

	// ----------------------------------------------------------- ghost stinger

	/** Ghost Stinger: lock onto someone, dash straight at them, grab them and shock them with ecto-electricity. */
	public static void stinger(ServerPlayerEntity p, GhostData d) {
		if (d.stingCooldown > 0) {
			GhostPowers.bar(p, "The stinger is recharging... (" + (d.stingCooldown / 20 + 1) + "s)", Formatting.GRAY);
			return;
		}
		if (d.stingTarget != null || d.dashTarget != null) return;
		Entity t = GhostPowers.aimEntity(p, d, 20);
		if (!(t instanceof LivingEntity) || t.squaredDistanceTo(p) > 400) {
			GhostPowers.bar(p, "Look at someone to dash at them.", Formatting.GRAY);
			return;
		}
		if (d.energy < 20) {
			GhostPowers.bar(p, "Not enough ghost energy!", Formatting.RED);
			return;
		}
		d.energy -= 20;
		d.stingCooldown = 160;
		d.dashTarget = t;
		d.dashTicks = 16;
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.2f, 1.4f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PHANTOM_SWOOP, SoundCategory.PLAYERS, 1.2f, 1.2f);
	}

	/** The dash: rocket toward the target, leaving a pink streak; grab them when you get there. */
	private static void tickDash(ServerPlayerEntity p, GhostData d, ServerWorld w) {
		Entity t = d.dashTarget;
		if (t == null || t.isRemoved() || !t.isAlive() || !d.transformed || --d.dashTicks < 0) {
			d.dashTarget = null;
			if (t != null && d.dashTicks < 0) GhostPowers.bar(p, "They slipped away!", Formatting.GRAY);
			return;
		}
		Vec3d to = t.getPos().add(0, t.getHeight() * 0.4, 0).subtract(p.getPos().add(0, 0.9, 0));
		double dist = to.length();
		if (dist < 2.4) {
			d.dashTarget = null;
			grab(p, d, t);
			return;
		}
		Vec3d v = to.multiply(Math.min(1.9, dist) / dist);
		p.setVelocity(v);
		p.velocityModified = true;
		p.fallDistance = 0;
		w.spawnParticles(PINK_DUST, p.getX(), p.getY() + 1, p.getZ(), 6, 0.25, 0.4, 0.25, 0);
		w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 3, 0.2, 0.3, 0.2, 0.05);
	}

	private static void grab(ServerPlayerEntity p, GhostData d, Entity t) {
		d.stingTarget = t;
		d.stingTicks = 40;
		p.setVelocity(Vec3d.ZERO);
		p.velocityModified = true;
		if (t instanceof MobEntity m) m.setAiDisabled(true);
		if (t instanceof ServerPlayerEntity tp) tp.sendMessage(Text.literal(p.getName().getString() + " grabbed you with a Ghost Stinger!").formatted(Formatting.RED, Formatting.BOLD), true);
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 1.2f, 1.6f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.2f, 0.7f);
	}

	private static void tickSting(ServerPlayerEntity p, GhostData d, ServerWorld w) {
		Entity t = d.stingTarget;
		if (t.isRemoved() || !t.isAlive() || t.getWorld() != w || --d.stingTicks < 0 || !d.transformed) {
			release(p, d, t, false);
			return;
		}
		// hold them up in front of you
		Vec3d hold = p.getEyePos().add(p.getRotationVec(1f).multiply(2.0)).subtract(0, t.getHeight() * 0.6, 0);
		if (t instanceof ServerPlayerEntity tp) {
			if (tp.squaredDistanceTo(hold) > 0.09) tp.requestTeleport(hold.x, hold.y, hold.z);
		} else {
			t.refreshPositionAndAngles(hold.x, hold.y, hold.z, t.getYaw(), t.getPitch());
		}
		t.setVelocity(Vec3d.ZERO);
		t.fallDistance = 0;
		if (d.stingTicks % 4 == 0) {
			Vec3d hand = p.getEyePos().add(p.getRotationVec(1f).multiply(0.7)).add(0, -0.3, 0);
			Vec3d mid = t.getPos().add(0, t.getHeight() * 0.5, 0);
			BeamPayload beam = new BeamPayload((float) hand.x, (float) hand.y, (float) hand.z, (float) mid.x, (float) mid.y, (float) mid.z, PINK);
			for (ServerPlayerEntity v : PlayerLookup.around(w, hand, 64)) if (ServerPlayNetworking.canSend(v, BeamPayload.ID)) ServerPlayNetworking.send(v, beam);
			w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, mid.x, mid.y, mid.z, 12, 0.4, 0.5, 0.4, 0.2);
			w.spawnParticles(PINK_DUST, mid.x, mid.y, mid.z, 6, 0.3, 0.4, 0.3, 0.05);
			t.damage(w.getDamageSources().indirectMagic(p, p), 2.5f);
			w.playSound(null, mid.x, mid.y, mid.z, ModSounds.NET_ZAP, SoundCategory.PLAYERS, 0.9f, 0.8f + w.random.nextFloat() * 0.3f);
		}
		if (d.stingTicks == 0) release(p, d, t, true);
	}

	private static void release(ServerPlayerEntity p, GhostData d, Entity t, boolean finished) {
		d.stingTarget = null;
		if (t instanceof MobEntity m) m.setAiDisabled(false);
		if (!finished || t == null || !t.isAlive()) return;
		Vec3d push = t.getPos().subtract(p.getPos()).normalize();
		t.addVelocity(push.x * 1.2, 0.4, push.z * 1.2);
		t.velocityModified = true;
		// a half-ghost gets shocked right out of ghost form
		if (t instanceof ServerPlayerEntity tp) {
			GhostData td = GhostState.of(tp.getServer()).data(tp.getUuid());
			if (td.transformed) {
				GhostPowers.endForm(tp, td);
				td.exhaust = Math.max(td.exhaust, 200);
				tp.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Shocked human!").formatted(Formatting.RED, Formatting.BOLD)));
				p.sendMessage(Text.literal("You shocked " + tp.getName().getString() + " right out of ghost form!").formatted(Formatting.DARK_RED), true);
				GhostPowers.syncNow(tp);
			}
		}
	}

	// ------------------------------------------------------ energy absorption

	public static void absorb(ServerPlayerEntity p, GhostData d) {
		if (d.absorbCooldown > 0) {
			GhostPowers.bar(p, "Can't absorb again yet... (" + (d.absorbCooldown / 20 + 1) + "s)", Formatting.GRAY);
			return;
		}
		d.absorbTicks = 120;
		d.absorbCooldown = 360;
		GhostPowers.bar(p, "Energy Absorption! Magic, fire, blasts and lightning now feed you.", Formatting.DARK_GREEN);
		p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.2f, 0.5f);
	}

	private static void gain(ServerPlayerEntity p, GhostData d, float amount) {
		d.energy = Math.min(GhostPowers.MAX_ENERGY, d.energy + amount * 4f);
		p.heal(amount * 0.5f);
		d.syncTimer = 0;
	}

	/** While absorbing, energy-type hits are eaten instead of hurting. Returns true when the hit was absorbed. */
	public static boolean absorbs(ServerPlayerEntity p, DamageSource source, float amount) {
		GhostData d = GhostState.of(p.getServer()).data(p.getUuid());
		if (d.absorbTicks <= 0 || amount <= 0) return false;
		boolean energy = source.isIn(DamageTypeTags.IS_PROJECTILE) || source.isIn(DamageTypeTags.IS_FIRE) || source.isIn(DamageTypeTags.IS_EXPLOSION)
				|| source.isIn(DamageTypeTags.IS_LIGHTNING) || source.isIn(DamageTypeTags.WITCH_RESISTANT_TO)
				|| source.isOf(net.minecraft.entity.damage.DamageTypes.SONIC_BOOM) || source.isOf(net.minecraft.entity.damage.DamageTypes.INDIRECT_MAGIC)
				|| source.isOf(net.minecraft.entity.damage.DamageTypes.MAGIC);
		if (!energy) return false;
		gain(p, d, amount);
		ServerWorld w = p.getServerWorld();
		w.spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 12, 0.5, 0.6, 0.5, 0.05);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.2f, 0.6f);
		p.extinguish();
		return true;
	}

	@SuppressWarnings("unused")
	private static boolean isGhost(Entity e) {
		return e instanceof AbstractGhostEntity;
	}

	@SuppressWarnings("unused")
	private static Box box(Vec3d c, double r) {
		return new Box(c, c).expand(r);
	}
}
