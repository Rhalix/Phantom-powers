package com.phantompowers;

import com.phantompowers.entity.BossMoves;
import com.phantompowers.network.BeamPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * The bigger cutscenes: the dark half-ghost's evil transformation (kind 5) and the origin story, getting
 * your powers inside the Ghost Portal (kind 6). Movement paths are shared with the client so it stays smooth.
 */
public final class Cutscenes {
	public static final int DARK_TICKS = 90, DARK_CHANGE = 80;
	public static final int ORIGIN_TICKS = 110, ORIGIN_BLAST = 58;
	/** 3D tumbling sparks spiralling round the player (drawn by the client). */
	static void sparkSwirl(ServerPlayerEntity p, float band, int color, int count) {
		sendSparks(p.getServerWorld(), p.getPos(), new com.phantompowers.network.SparkPayload(1, p.getId(), 0, 0, 0, color, count, band));
	}

	/** A burst of 3D shards flying out from a point. */
	static void sparkBurst(ServerWorld w, double x, double y, double z, int color, int count) {
		sendSparks(w, new net.minecraft.util.math.Vec3d(x, y, z), new com.phantompowers.network.SparkPayload(0, -1, (float) x, (float) y, (float) z, color, count, 0));
	}

	private static void sendSparks(ServerWorld w, net.minecraft.util.math.Vec3d at, com.phantompowers.network.SparkPayload pl) {
		for (ServerPlayerEntity v : net.fabricmc.fabric.api.networking.v1.PlayerLookup.around(w, at, 96)) {
			if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(v, com.phantompowers.network.SparkPayload.ID))
				net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(v, pl);
		}
	}

	private static final DustParticleEffect PINK = new DustParticleEffect(new Vector3f(1f, 0.31f, 0.78f), 1.6f);
	private static final DustParticleEffect BLACK = new DustParticleEffect(new Vector3f(0.08f, 0.02f, 0.1f), 2.0f);

	private Cutscenes() {}

	/** How high you float during the evil transformation. */
	public static double darkLift(int e) {
		return e < DARK_CHANGE ? Math.min(1.4, e * 0.02) : 1.4 * Math.max(0, 1 - (e - DARK_CHANGE) / 10.0);
	}

	/** Origin: lifted up by the surge, then blown out of the portal (distance forward, height). */
	public static double originForward(int e) {
		if (e < ORIGIN_BLAST) return 0;
		double t = e - ORIGIN_BLAST;
		return 2.6 * (1 - Math.exp(-t * 0.25));
	}

	public static double originLift(int e) {
		if (e < 18) return 0;
		if (e < ORIGIN_BLAST) return Math.min(0.7, (e - 18) * 0.03);
		double t = e - ORIGIN_BLAST;
		return Math.max(0, 0.7 + Math.sin(Math.min(Math.PI, t * 0.25)) * 0.5 - t * 0.04);
	}

	// ---------------------------------------------------------------- dark transformation

	public static boolean startDark(ServerPlayerEntity p, GhostData d) {
		if (!(d.dark && d.darkMode) || d.transformed) return false;
		d.cutscene = DARK_TICKS;
		if (p.isUsingItem()) p.stopUsingItem();
		d.cutsceneKind = 5;
		d.cutsceneToGhost = true;
		d.cutsceneAnchor = p.getPos();
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.5f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), SoundCategory.PLAYERS, 1.5f, 0.6f);
		GhostPowers.syncNow(p);
		return true;
	}

	public static void tickDark(ServerPlayerEntity p, GhostData d) {
		d.cutscene--;
		int e = DARK_TICKS - d.cutscene;
		ServerWorld w = p.getServerWorld();
		Vec3d a = d.cutsceneAnchor == null ? p.getPos() : d.cutsceneAnchor;
		Vec3d want = a.add(0, darkLift(e), 0);
		if (p.getPos().squaredDistanceTo(want) > 4) p.requestTeleport(want.x, want.y, want.z);
		p.fallDistance = 0;
		if (e < DARK_CHANGE) {
			// a dark aura winds up around you: black smoke and pink flames spiralling skyward
			float k = e / (float) DARK_CHANGE;
			sparkSwirl(p, (e * 0.045f) % 1f, DarkPowers.PINK, 1 + (int) (k * 3));
			if (e % 2 == 0) {
				double ang = e * 0.35;
				w.spawnParticles(BLACK, p.getX() + Math.cos(ang) * 1.2, p.getY() + (e * 0.07) % 2.4, p.getZ() + Math.sin(ang) * 1.2, 1, 0, 0, 0, 0);
			}
			if (e % 3 == 0) w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 0.1, p.getZ(), 4, 0.6, 0.05, 0.6, 0.03);
			if (e % 4 == 0) w.spawnParticles(ParticleTypes.LARGE_SMOKE, p.getX(), a.y + 0.1, p.getZ(), 6, 1.2, 0.05, 1.2, 0.02);
			if (e % 16 == 0) BossMoves.warn(w, a, 1.5 + k * 3.5, 14, DarkPowers.PINK);
			if (e == 20) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.4f, 0.6f);
			if (e == 45) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.4f, 0.5f);
			if (e == 34 || e == 56 || e == 70) strike(w, a.add((w.random.nextDouble() - 0.5) * 7, 0, (w.random.nextDouble() - 0.5) * 7));
			if (e == DARK_CHANGE - 6) GhostPowers.sendRings(p, 20, 0);
		}
		if (e == DARK_CHANGE) {
			// the change: a pink blast rolls outward and you're in your dark form
			GhostPowers.forceTransform(p, d);
			w.spawnParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
			w.spawnParticles(PINK, p.getX(), p.getY() + 1, p.getZ(), 50, 2.5, 1.2, 2.5, 0.2);
			sparkBurst(w, p.getX(), p.getY() + 1, p.getZ(), DarkPowers.PINK, 70);
			w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 60, 1.5, 1, 1.5, 0.15);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.9f, 1.3f);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.8f, 0.8f);
			for (Entity en : w.getOtherEntities(p, new Box(p.getBlockPos()).expand(5), x -> x instanceof LivingEntity)) {
				Vec3d push = en.getPos().subtract(p.getPos()).normalize();
				en.addVelocity(push.x * 1.2, 0.5, push.z * 1.2);
				en.velocityModified = true;
			}
		}
		if (d.cutscene <= 0) {
			d.cutscene = 0;
			d.cutsceneAnchor = null;
			GhostPowers.syncNow(p);
		}
	}

	private static void strike(ServerWorld w, Vec3d at) {
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
		if (bolt == null) return;
		bolt.setCosmetic(true);
		bolt.refreshPositionAfterTeleport(BossMoves.ground(w, at));
		w.spawnEntity(bolt);
	}

	// ---------------------------------------------------------------- hero transformation

	public static final int HERO_TICKS = 80, HERO_CHANGE = 64;
	private static final DustParticleEffect ECTO = new DustParticleEffect(new Vector3f(0.3f, 1f, 0.5f), 1.6f);
	private static final DustParticleEffect WHITE = new DustParticleEffect(new Vector3f(0.9f, 1f, 0.95f), 1.3f);

	public static double heroLift(int e) {
		return e < HERO_CHANGE ? Math.min(0.9, e * 0.016) : 0.9 * Math.max(0, 1 - (e - HERO_CHANGE) / 14.0);
	}

	/** The hero's own transformation: a calm, building aura, then "I'M GOING GHOST!" and the rings. */
	public static void startHero(ServerPlayerEntity p, GhostData d) {
		d.cutscene = HERO_TICKS;
		if (p.isUsingItem()) p.stopUsingItem();
		d.cutsceneKind = 7;
		d.cutsceneToGhost = true;
		d.cutsceneAnchor = p.getPos();
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 1.5f, 0.7f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.0f, 0.8f);
		GhostPowers.syncNow(p);
	}

	public static void tickHero(ServerPlayerEntity p, GhostData d) {
		d.cutscene--;
		int e = HERO_TICKS - d.cutscene;
		ServerWorld w = p.getServerWorld();
		Vec3d a = d.cutsceneAnchor == null ? p.getPos() : d.cutsceneAnchor;
		Vec3d want = a.add(0, heroLift(e), 0);
		if (p.getPos().squaredDistanceTo(want) > 4) p.requestTeleport(want.x, want.y, want.z);
		p.fallDistance = 0;
		if (e < HERO_CHANGE) {
			// green energy wells up from the ground and swirls up around you, the wind picking up
			float k = e / (float) HERO_CHANGE;
			sparkSwirl(p, (e * 0.05f) % 1f, 0x5AFF96, 1 + (int) (k * 3));
			if (e % 3 == 0) w.spawnParticles(ParticleTypes.END_ROD, p.getX(), a.y + 0.1, p.getZ(), 2, 0.8, 0.05, 0.8, 0.04);
			if (e % 5 == 0) w.spawnParticles(ParticleTypes.CLOUD, p.getX(), a.y + 0.1, p.getZ(), 4, 1.0, 0.02, 1.0, 0.03);
			if (e % 16 == 0) BossMoves.warn(w, a, 1.5 + k * 3, 14, 0x55FF88);
			if (e == 30) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.4f, 1.0f);
			if (e == HERO_CHANGE - 10) {
				GhostPowers.sendRings(p, 18, 0);
				w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.2f, 1.2f);
			}
		}
		if (e == HERO_CHANGE) {
			GhostPowers.forceTransform(p, d);
			w.spawnParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
			w.spawnParticles(ECTO, p.getX(), p.getY() + 1, p.getZ(), 45, 2.2, 1.2, 2.2, 0.2);
			sparkBurst(w, p.getX(), p.getY() + 1, p.getZ(), 0x5AFF96, 70);
			w.spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 1, 0.6, 0.3);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.4f, 1.0f);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_TRIDENT_THUNDER.value(), SoundCategory.PLAYERS, 0.7f, 1.4f);
		}
		if (d.cutscene <= 0) {
			d.cutscene = 0;
			d.cutsceneAnchor = null;
			GhostPowers.syncNow(p);
		}
	}

	// ---------------------------------------------------------------- suit up

	public static final int SUIT_TICKS = 84, SUIT_PLATES = 14, SUIT_DONE = 70;

	/** Suiting up: the pack hums to life on your back, then armour plates unfold out of it and lock on around you. */
	public static void startSuit(ServerPlayerEntity p, GhostData d) {
		d.cutscene = SUIT_TICKS;
		if (p.isUsingItem()) p.stopUsingItem();
		d.cutsceneKind = 8;
		d.cutsceneToGhost = true;
		d.cutsceneAnchor = p.getPos();
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 1.6f);
		GhostPowers.syncNow(p);
	}

	public static void tickSuit(ServerPlayerEntity p, GhostData d) {
		d.cutscene--;
		int e = SUIT_TICKS - d.cutscene;
		ServerWorld w = p.getServerWorld();
		Vec3d a = d.cutsceneAnchor == null ? p.getPos() : d.cutsceneAnchor;
		if (p.getPos().squaredDistanceTo(a) > 4) p.requestTeleport(a.x, a.y, a.z);
		p.fallDistance = 0;
		Vec3d back = p.getPos().add(Vec3d.fromPolar(0, p.getBodyYaw()).multiply(-0.35)).add(0, 1.2, 0);
		if (e < SUIT_PLATES) {
			// the pack powers up: whirring, sparks and cyan light
			if (e % 2 == 0) w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, back.x, back.y, back.z, 3, 0.15, 0.2, 0.15, 0.05);
			if (e == 2) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.PLAYERS, 1.0f, 1.4f);
			if (e == 8) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_CONDUIT_ACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.6f);
		}
		if (e == SUIT_PLATES) GhostPowers.sendRings(p, SUIT_DONE - SUIT_PLATES, 6);
		if (e > SUIT_PLATES && e < SUIT_DONE && e % 4 == 0) {
			// clank... clank... each plate locking into place
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_IRON.value(), SoundCategory.PLAYERS, 0.7f, 0.8f + (e % 12) * 0.05f);
			w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 0.3 + (e - SUIT_PLATES) / 30.0, p.getZ(), 3, 0.3, 0.1, 0.3, 0.05);
		}
		if (e == SUIT_DONE) {
			if (!d.suitOn) HunterSuit.toggle(p, d, false);
			w.spawnParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1.6, p.getZ(), 1, 0, 0, 0, 0);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.8f);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(), SoundCategory.PLAYERS, 1.2f, 0.7f);
		}
		if (d.cutscene <= 0) {
			d.cutscene = 0;
			d.cutsceneAnchor = null;
			GhostPowers.syncNow(p);
		}
	}

	// ---------------------------------------------------------------- changing back (hold V / hold U)

	public static final int HERO_OFF_TICKS = 60, HERO_OFF_CHANGE = 44;
	public static final int DARK_OFF_TICKS = 70, DARK_OFF_CHANGE = 54;
	public static final int SUIT_OFF_TICKS = 70, SUIT_OFF_DONE = 58;

	/** Hero going human: floating gently down while the ghost energy drains out into the ground. */
	public static double heroOffLift(int e) {
		return e < HERO_OFF_CHANGE ? 0.6 * (1 - e / (double) HERO_OFF_CHANGE) : 0;
	}

	/** Dark form letting go: hangs in the air fighting it, then drops. */
	public static double darkOffLift(int e) {
		if (e < 20) return Math.min(0.8, e * 0.04);
		if (e < DARK_OFF_CHANGE) return 0.8;
		return 0.8 * Math.max(0, 1 - (e - DARK_OFF_CHANGE) / 5.0);
	}

	private static void begin(ServerPlayerEntity p, GhostData d, int ticks, int kind) {
		d.cutscene = ticks;
		if (p.isUsingItem()) p.stopUsingItem();
		d.cutsceneKind = kind;
		d.cutsceneToGhost = false;
		d.cutsceneAnchor = p.getPos();
		GhostPowers.syncNow(p);
	}

	private static void hold(ServerPlayerEntity p, GhostData d, double lift) {
		Vec3d a = d.cutsceneAnchor == null ? p.getPos() : d.cutsceneAnchor;
		Vec3d want = a.add(0, lift, 0);
		if (p.getPos().squaredDistanceTo(want) > 4) p.requestTeleport(want.x, want.y, want.z);
		p.fallDistance = 0;
	}

	private static void finish(ServerPlayerEntity p, GhostData d) {
		if (d.cutscene <= 0) {
			d.cutscene = 0;
			d.cutsceneAnchor = null;
			GhostPowers.syncNow(p);
		}
	}

	public static void startHeroOff(ServerPlayerEntity p, GhostData d) {
		begin(p, d, HERO_OFF_TICKS, 9);
		p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.2f);
	}

	public static void tickHeroOff(ServerPlayerEntity p, GhostData d) {
		d.cutscene--;
		int e = HERO_OFF_TICKS - d.cutscene;
		ServerWorld w = p.getServerWorld();
		hold(p, d, heroOffLift(e));
		if (e < HERO_OFF_CHANGE) {
			// the glow drains down out of you and soaks into the ground
			if (e % 2 == 0) sparkSwirl(p, 1f - (e * 0.04f) % 1f, 0x5AFF96, 2);
			if (e % 4 == 0) w.spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 0.05, p.getZ(), 3, 0.6, 0.02, 0.6, 0.02);
			if (e == HERO_OFF_CHANGE - 18) GhostPowers.sendRings(p, 18, 4);
		}
		if (e == HERO_OFF_CHANGE) {
			GhostPowers.forceHuman(p, d);
			w.spawnParticles(WHITE, p.getX(), p.getY() + 1, p.getZ(), 15, 0.5, 0.9, 0.5, 0.02);
			sparkBurst(w, p.getX(), p.getY() + 1, p.getZ(), 0xE8FFF0, 30);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 0.8f, 0.6f);
		}
		finish(p, d);
	}

	public static void startDarkOff(ServerPlayerEntity p, GhostData d) {
		begin(p, d, DARK_OFF_TICKS, 10);
		p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.5f, 0.6f);
	}

	public static void tickDarkOff(ServerPlayerEntity p, GhostData d) {
		d.cutscene--;
		int e = DARK_OFF_TICKS - d.cutscene;
		ServerWorld w = p.getServerWorld();
		hold(p, d, darkOffLift(e));
		if (e < DARK_OFF_CHANGE) {
			// the darkness fights to stay: smoke and pink flame tearing off you, a heartbeat, flickers
			float k = e / (float) DARK_OFF_CHANGE;
			w.spawnParticles(BLACK, p.getX(), p.getY() + 1, p.getZ(), 3, 0.4, 0.8, 0.4, 0.04);
			w.spawnParticles(PINK, p.getX(), p.getY() + 1.2, p.getZ(), 2 + (int) (k * 4), 0.3 + k, 0.6, 0.3 + k, 0.08);
			if (e % 3 == 0) w.spawnParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1.6, p.getZ(), 2, 0.3, 0.3, 0.3, 0.05);
			if (e % 12 == 0) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.4f, 0.6f + k * 0.4f);
			if (e == 30) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.7f, 0.6f);
			if (e == DARK_OFF_CHANGE - 16) GhostPowers.sendRings(p, 16, 4);
		}
		if (e == DARK_OFF_CHANGE) {
			// it lets go all at once: the dark aura bursts off you and scatters
			GhostPowers.forceHuman(p, d);
			w.spawnParticles(PINK, p.getX(), p.getY() + 1, p.getZ(), 30, 1.5, 1, 1.5, 0.2);
			sparkBurst(w, p.getX(), p.getY() + 1, p.getZ(), DarkPowers.PINK, 50);
			w.spawnParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 40, 1, 1, 1, 0.08);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.8f, 0.5f);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 1.0f, 0.8f);
		}
		finish(p, d);
	}

	public static void startSuitOff(ServerPlayerEntity p, GhostData d) {
		begin(p, d, SUIT_OFF_TICKS, 11);
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.9f, 1.6f);
		GhostPowers.sendRings(p, SUIT_OFF_DONE - 6, 7);
	}

	public static void tickSuitOff(ServerPlayerEntity p, GhostData d) {
		d.cutscene--;
		int e = SUIT_OFF_TICKS - d.cutscene;
		ServerWorld w = p.getServerWorld();
		hold(p, d, 0);
		Vec3d back = p.getPos().add(Vec3d.fromPolar(0, p.getBodyYaw()).multiply(-0.35)).add(0, 1.2, 0);
		if (e < SUIT_OFF_DONE && e % 4 == 2) {
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_IRON.value(), SoundCategory.PLAYERS, 0.6f, 1.4f - (e % 12) * 0.04f);
		}
		if (e > SUIT_OFF_DONE - 14 && e < SUIT_OFF_DONE + 8 && e % 2 == 0) {
			// the pack vents steam as it folds everything away
			w.spawnParticles(ParticleTypes.CLOUD, back.x, back.y + 0.2, back.z, 2, 0.1, 0.05, 0.1, 0.03);
			w.spawnParticles(ParticleTypes.WHITE_SMOKE, back.x, back.y, back.z, 2, 0.12, 0.05, 0.12, 0.02);
		}
		if (e == SUIT_OFF_DONE) {
			if (d.suitOn) HunterSuit.toggle(p, d, false);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.PLAYERS, 1.0f, 1.2f);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.6f, 1.6f);
		}
		finish(p, d);
	}

	// ---------------------------------------------------------------- origin story

	/** Standing in the portal when it switches on: it surges through you... and you come out half ghost. */
	public static void startOrigin(ServerPlayerEntity p, Vec3d portalCenter, Vec3d front) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		if (d.cutscene > 0) return;
		Vec3d out = new Vec3d(front.x - portalCenter.x, 0, front.z - portalCenter.z);
		out = out.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : out.normalize();
		Vec3d stand = new Vec3d(portalCenter.x, Math.floor(p.getY()), portalCenter.z);
		float yaw = (float) (Math.toDegrees(Math.atan2(-out.x, out.z)));
		p.networkHandler.requestTeleport(stand.x, stand.y, stand.z, yaw, 0f);
		d.cutscene = ORIGIN_TICKS;
		if (p.isUsingItem()) p.stopUsingItem();
		d.cutsceneKind = 6;
		d.cutsceneToGhost = true;
		d.cutsceneAnchor = stand;
		d.portalCenter = portalCenter;
		st.markDirty();
		GhostPowers.syncNow(p);
	}

	public static void tickOrigin(ServerPlayerEntity p, GhostData d) {
		d.cutscene--;
		int e = ORIGIN_TICKS - d.cutscene;
		ServerWorld w = p.getServerWorld();
		Vec3d a = d.cutsceneAnchor == null ? p.getPos() : d.cutsceneAnchor;
		Vec3d look = Vec3d.fromPolar(0, p.getYaw());
		Vec3d want = a.add(look.multiply(originForward(e))).add(0, originLift(e), 0);
		if (e < ORIGIN_BLAST && p.getPos().squaredDistanceTo(want) > 9) p.requestTeleport(want.x, want.y, want.z);
		if (e == ORIGIN_BLAST || e == ORIGIN_BLAST + 2) {
			// the portal blasts you out: a real launch, so you fly across the lab (and stop if you hit something)
			PortalLogic.blockTravel(p, ORIGIN_TICKS);
			p.setVelocity(look.multiply(2.4).add(0, 0.85, 0));
			p.velocityModified = true;
		}
		p.fallDistance = 0;
		Vec3d mid = p.getPos().add(0, 1, 0);
		if (e < 18) {
			if (e % 3 == 0) w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, mid.x, mid.y, mid.z, 4, 0.8, 1, 0.8, 0.1);
			if (e == 1) w.playSound(null, mid.x, mid.y, mid.z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.5f, 0.6f);
		} else if (e < ORIGIN_BLAST) {
			// green lightning arcs from the frame into you
			if (e % 2 == 0) {
				Vec3d side = new Vec3d(-look.z, 0, look.x);
				for (int s = -1; s <= 1; s += 2) {
					Vec3d from = a.add(side.multiply(1.4 * s)).add(0, 0.3 + w.random.nextDouble() * 2.4, 0);
					BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) mid.x, (float) mid.y, (float) mid.z, 0x55FF88);
					for (ServerPlayerEntity v : PlayerLookup.around(w, mid, 64)) if (ServerPlayNetworking.canSend(v, BeamPayload.ID)) ServerPlayNetworking.send(v, beam);
				}
			}
			w.spawnParticles(GhostPowers.GREEN, mid.x, mid.y, mid.z, 6, 0.4, 0.8, 0.4, 0.05);
			if (e % 5 == 0) w.playSound(null, mid.x, mid.y, mid.z, ModSounds.NET_ZAP, SoundCategory.PLAYERS, 1.2f, 0.6f + w.random.nextFloat() * 0.6f);
			if (e == 22) w.playSound(null, mid.x, mid.y, mid.z, SoundEvents.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 1.4f, 0.8f);
		} else if (e == ORIGIN_BLAST) {
			w.spawnParticles(ParticleTypes.FLASH, mid.x, mid.y, mid.z, 1, 0, 0, 0, 0);
			w.spawnParticles(GhostPowers.GREEN, mid.x, mid.y, mid.z, 50, 1.2, 1.5, 1.2, 0.3);
			sparkBurst(w, mid.x, mid.y, mid.z, 0x5AFF96, 80);
			w.playSound(null, mid.x, mid.y, mid.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 1.2f, 1.4f);
			w.playSound(null, mid.x, mid.y, mid.z, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1.2f, 1.0f);
			strike(w, a);
		} else if (e == ORIGIN_BLAST + 18) {
			// you come out of it... different
			GhostPowers.grantPowers(p);
			GhostPowers.forceTransform(p, d);
			GhostPowers.sendRings(p, 20, 0);
			p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("You're half ghost!").formatted(Formatting.GREEN, Formatting.BOLD)));
			p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("Press V to go ghost / go human").formatted(Formatting.WHITE)));
			p.sendMessage(Text.literal("The portal's energy rewrote you. You have ghost powers!").formatted(Formatting.GREEN), false);
			p.sendMessage(Text.literal("V = go ghost / go human.  R, C, X, Z = your power slots.  G = lock on.  K = Ghost Skills (unlock powers with stat points).").formatted(Formatting.GRAY), false);
			w.playSound(null, mid.x, mid.y, mid.z, SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 1.2f, 0.6f);
		}
		if (d.cutscene <= 0) {
			d.cutscene = 0;
			PortalLogic.blockTravel(p, 60);
			p.fallDistance = 0;
			d.cutsceneAnchor = null;
			d.portalCenter = null;
			GhostPowers.syncNow(p);
		}
	}
}
