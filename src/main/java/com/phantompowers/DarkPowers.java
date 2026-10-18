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

/**
 * The dark half-ghost: how you become it (the prototype portal and its ecto acne) and its four powers:
 * Ecto Constructs, Tornado Spin, Ghost Stinger and Energy Absorption.
 */
public final class DarkPowers {
	public static final int ACNE_TICKS = 6000; // five minutes
	private static final float SPIN_DRAIN = 0.35f;

	private DarkPowers() {}

	// ------------------------------------------------------- becoming dark

	/** The prototype portal blasts you in the face. */
	public static void portalBlast(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		ServerWorld w = p.getServerWorld();
		Vec3d look = p.getRotationVec(1f);
		Vec3d at = p.getEyePos().add(look.multiply(0.8));
		w.spawnParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		w.spawnParticles(GhostPowers.GREEN, at.x, at.y, at.z, 80, 0.4, 0.4, 0.4, 0.3);
		w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 40, 0.3, 0.3, 0.3, 0.4);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 1.0f, 1.6f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.2f, 0.6f);
		p.addVelocity(-look.x * 1.2, 0.4, -look.z * 1.2);
		p.velocityModified = true;
		p.damage(w.getDamageSources().magic(), Math.min(4f, Math.max(0f, p.getHealth() - 1f)));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0));
		if (!d.powers) {
			p.sendMessage(Text.literal("ZAP! It hurt... but nothing else happened. It only reacts with someone who already has ghost powers.").formatted(Formatting.GRAY), false);
			return;
		}
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
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("The unstable blast is changing you... wait 5 minutes").formatted(Formatting.GRAY)));
		GhostPowers.broadcastStates(p.getServer());
	}

	private static void finishAcne(ServerPlayerEntity p, GhostData d) {
		d.dark = true;
		d.darkMode = true;
		d.suitColors = SuitColors.VILLAIN.clone();
		ServerWorld w = p.getServerWorld();
		w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 80, 0.6, 1, 0.6, 0.1);
		w.spawnParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1, 0.6, 0.05);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.4f);
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Your dark side awakens").formatted(Formatting.DARK_RED, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("New dark powers can be unlocked in Ghost Skills").formatted(Formatting.GRAY)));
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
		if (d.spinning) tickSpin(p, d, w);
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

	public static void toggleSpin(ServerPlayerEntity p, GhostData d) {
		d.spinning = !d.spinning;
		GhostPowers.bar(p, d.spinning ? "Tornado Spin on! Move or fly fast to whip up a twister." : "Tornado Spin off.",
				d.spinning ? Formatting.DARK_GREEN : Formatting.GRAY);
		if (!d.spinning) sendSpin(p, true);
	}

	private static void sendSpin(ServerPlayerEntity p, boolean done) {
		TornadoPayload msg = new TornadoPayload(1_000_000 + p.getId(), p.getX(), p.getY() - 0.5, p.getZ(), p.age, done, 0.32f);
		for (ServerPlayerEntity v : PlayerLookup.around(p.getServerWorld(), p.getPos(), 96)) {
			if (ServerPlayNetworking.canSend(v, TornadoPayload.ID)) ServerPlayNetworking.send(v, msg);
		}
	}

	private static void tickSpin(ServerPlayerEntity p, GhostData d, ServerWorld w) {
		double speed = Math.sqrt(Math.pow(p.getX() - p.prevX, 2) + Math.pow(p.getZ() - p.prevZ, 2) + Math.pow(p.getY() - p.prevY, 2));
		d.energy -= SPIN_DRAIN;
		if (d.energy <= 0) {
			d.energy = 0;
			d.spinning = false;
			sendSpin(p, true);
			GhostPowers.bar(p, "Out of ghost energy, your twister dies down.", Formatting.RED);
			return;
		}
		if (speed < 0.2) {
			if (p.age % 10 == 0) sendSpin(p, true);
			return;
		}
		sendSpin(p, false);
		if (p.age % 10 == 0) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.8f, 1.3f);
		for (Entity e : w.getOtherEntities(p, p.getBoundingBox().expand(4.5), e -> (e instanceof LivingEntity || e instanceof net.minecraft.entity.ItemEntity)
				&& !(e instanceof com.phantompowers.entity.GhostCloneEntity))) {
			if (e instanceof PlayerEntity pl && pl.isCreative()) continue;
			Vec3d to = new Vec3d(p.getX() - e.getX(), 0, p.getZ() - e.getZ());
			double dist = Math.max(0.3, to.length());
			Vec3d in = to.multiply(1 / dist), around = new Vec3d(-in.z, 0, in.x);
			e.setVelocity(e.getVelocity().multiply(0.6).add(in.x * 0.15 + around.x * 0.55, 0.22, in.z * 0.15 + around.z * 0.55));
			e.velocityModified = true;
			e.fallDistance = 0;
			if (p.age % 8 == 0 && e instanceof LivingEntity) e.damage(w.getDamageSources().indirectMagic(p, p), 4f);
		}
	}

	// ----------------------------------------------------------- ghost stinger

	public static void stinger(ServerPlayerEntity p, GhostData d) {
		if (d.stingCooldown > 0) {
			GhostPowers.bar(p, "The stinger is recharging... (" + (d.stingCooldown / 20 + 1) + "s)", Formatting.GRAY);
			return;
		}
		Entity t = GhostPowers.aimEntity(p, d, 7);
		if (!(t instanceof LivingEntity) || t.squaredDistanceTo(p) > 49) {
			GhostPowers.bar(p, "Get closer and look at someone to grab them.", Formatting.GRAY);
			return;
		}
		if (d.energy < 20) {
			GhostPowers.bar(p, "Not enough ghost energy!", Formatting.RED);
			return;
		}
		d.energy -= 20;
		d.stingTarget = t;
		d.stingTicks = 40;
		d.stingCooldown = 160;
		if (t instanceof MobEntity m) m.setAiDisabled(true);
		if (t instanceof ServerPlayerEntity tp) tp.sendMessage(Text.literal(p.getName().getString() + " grabbed you with a Ghost Stinger!").formatted(Formatting.RED, Formatting.BOLD), true);
		p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 1.2f, 1.6f);
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
			BeamPayload beam = new BeamPayload((float) hand.x, (float) hand.y, (float) hand.z, (float) mid.x, (float) mid.y, (float) mid.z, 0xA0FFD0);
			for (ServerPlayerEntity v : PlayerLookup.around(w, hand, 64)) if (ServerPlayNetworking.canSend(v, BeamPayload.ID)) ServerPlayNetworking.send(v, beam);
			w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, mid.x, mid.y, mid.z, 12, 0.4, 0.5, 0.4, 0.2);
			w.spawnParticles(GhostPowers.GREEN, mid.x, mid.y, mid.z, 6, 0.3, 0.4, 0.3, 0.05);
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
