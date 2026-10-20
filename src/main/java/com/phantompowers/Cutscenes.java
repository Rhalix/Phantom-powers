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
			for (int i = 0; i < 2 + (int) (k * 4); i++) {
				double ang = e * 0.35 + i * Math.PI * 2 / 3;
				double r = 1.4 - k * 0.6;
				double y = (e * 0.07 + i * 0.4) % 2.4;
				w.spawnParticles(i % 2 == 0 ? PINK : BLACK, p.getX() + Math.cos(ang) * r, p.getY() + y, p.getZ() + Math.sin(ang) * r, 1, 0, 0, 0, 0);
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
			w.spawnParticles(PINK, p.getX(), p.getY() + 1, p.getZ(), 150, 2.5, 1.2, 2.5, 0.2);
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
		if (p.getPos().squaredDistanceTo(want) > 9) p.requestTeleport(want.x, want.y, want.z);
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
			w.spawnParticles(GhostPowers.GREEN, mid.x, mid.y, mid.z, 150, 1.2, 1.5, 1.2, 0.3);
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
			p.sendMessage(Text.literal("V = go ghost / go human.  R = Ecto Blast.  K = Ghost Skills (unlock powers with stat points).").formatted(Formatting.GRAY), false);
			w.playSound(null, mid.x, mid.y, mid.z, SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 1.2f, 0.6f);
		}
		if (d.cutscene <= 0) {
			d.cutscene = 0;
			d.cutsceneAnchor = null;
			d.portalCenter = null;
			GhostPowers.syncNow(p);
		}
	}
}
