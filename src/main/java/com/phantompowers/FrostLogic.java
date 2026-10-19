package com.phantompowers;

import com.phantompowers.entity.BossMoves;
import com.phantompowers.entity.CrateKingEntity;
import com.phantompowers.entity.GhostJailerEntity;
import com.phantompowers.entity.VoltageEntity;
import com.phantompowers.entity.WishkeeperEntity;
import com.phantompowers.network.EntityFxPayload;
import com.phantompowers.network.ShockwavePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Frost Breath: hold the key to breathe an icy blast. Whatever stays in it freezes solid,
 * and a frozen enemy on low health shatters into pieces when you punch it.
 */
public final class FrostLogic {
	public static final float START_COST = 10f;
	private static final float DRAIN = 0.45f;
	private static final int MAX_TICKS = 160;
	private static final float FREEZE_AT = 40f;
	private static final DustColorTransitionParticleEffect FROST =
			new DustColorTransitionParticleEffect(new Vector3f(1f, 1f, 1f), new Vector3f(0.55f, 0.85f, 1f), 1.1f);

	private static final class Freeze {
		final LivingEntity e;
		float buildup;
		int frozen;
		boolean aiSaved;
		boolean prevAi;
		Vec3d at;

		Freeze(LivingEntity e) {
			this.e = e;
		}
	}

	private static final Map<UUID, Freeze> MAP = new HashMap<>();
	private static int[] lastHosts = new int[0];
	private static int[] lastFrozen = new int[0];
	private static int syncTimer;

	private FrostLogic() {}

	public static boolean isBoss(Entity e) {
		return e instanceof GhostJailerEntity || e instanceof CrateKingEntity || e instanceof VoltageEntity || e instanceof WishkeeperEntity
				|| e instanceof com.phantompowers.entity.RevEntity || e instanceof com.phantompowers.entity.RoxyEntity
				|| (e instanceof com.phantompowers.entity.ShadeEntity s && s.isBoss()) || e instanceof com.phantompowers.entity.DoloraEntity;
	}

	public static boolean isFrozen(Entity e) {
		Freeze f = MAP.get(e.getUuid());
		return f != null && f.frozen > 0;
	}

	// ------------------------------------------------------------------ breath

	public static void start(ServerPlayerEntity p, GhostData d) {
		if (d.frostTicks > 0) return;
		if (d.frostCooldown > 0) {
			GhostPowers.bar(p, "Your breath needs to warm up... (" + (d.frostCooldown / 20 + 1) + "s)", Formatting.AQUA);
			return;
		}
		if (d.energy < START_COST) {
			GhostPowers.bar(p, "Not enough ghost energy!", Formatting.RED);
			return;
		}
		d.energy -= START_COST;
		d.frostHeld = true;
		d.frostTicks = 1;
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1.0f, 0.6f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BREEZE_INHALE, SoundCategory.PLAYERS, 1.0f, 0.7f);
		GhostPowers.bar(p, "FROST BREATH! (hold to keep breathing)", Formatting.AQUA);
	}

	public static void tickBreath(ServerPlayerEntity p, GhostData d) {
		if (d.frostCooldown > 0) d.frostCooldown--;
		if (d.frostTicks <= 0) return;
		boolean keep = d.transformed && p.isAlive() && d.energy > 0 && d.frostHeld && d.frostTicks < MAX_TICKS && d.possessing == null;
		if (!keep) {
			d.frostCooldown = 40 + d.frostTicks / 3;
			d.frostTicks = 0;
			d.frostHeld = false;
			return;
		}
		d.frostTicks++;
		d.energy = Math.max(0, d.energy - DRAIN);
		breathe(p.getServerWorld(), p, p.getEyePos().add(0, -0.18, 0), p.getRotationVec(1f), d.frostTicks);
	}

	/** One tick of icy breath from {@code mouth} along {@code dir}. Used by players (and anything else that breathes frost). */
	public static void breathe(ServerWorld w, LivingEntity src, Vec3d mouth, Vec3d dir, int t) {
		Vec3d from = mouth.add(dir.multiply(0.35));
		var rnd = w.random;
		// The breath: a widening cone of frost, snowflakes and icy mist
		for (int i = 0; i < 7; i++) {
			double d = rnd.nextDouble() * 8.0;
			double spread = 0.08 + d * 0.17;
			Vec3d at = from.add(dir.multiply(d)).add((rnd.nextDouble() - 0.5) * spread * 2, (rnd.nextDouble() - 0.5) * spread * 2, (rnd.nextDouble() - 0.5) * spread * 2);
			w.spawnParticles(FROST, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			w.spawnParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 0, dir.x, dir.y, dir.z, 0.25 + rnd.nextDouble() * 0.2);
		}
		w.spawnParticles(ParticleTypes.CLOUD, from.x, from.y, from.z, 0, dir.x, dir.y, dir.z, 0.15);
		if (t % 3 == 0) w.spawnParticles(ParticleTypes.WHITE_ASH, from.x, from.y, from.z, 6, 0.4, 0.4, 0.4, 0.02);
		if (t % 7 == 0) {
			w.playSound(null, from.x, from.y, from.z, SoundEvents.BLOCK_POWDER_SNOW_STEP, SoundCategory.PLAYERS, 1.2f, 0.6f);
			w.playSound(null, from.x, from.y, from.z, SoundEvents.ENTITY_BREEZE_WHIRL, SoundCategory.PLAYERS, 0.6f, 1.5f);
		}
		if (t % 2 != 0) return;
		// Freeze whatever is in the cone
		for (Entity e : w.getOtherEntities(src, new Box(from, from).expand(8.5), e -> e instanceof LivingEntity && e.isAlive())) {
			if (GhostFlags.HOSTS.contains(e.getUuid())) continue;
			Vec3d dv = e.getPos().add(0, e.getHeight() / 2, 0).subtract(from);
			double len = dv.length();
			if (len > 8.5 || len < 0.05 || dv.multiply(1 / len).dotProduct(dir) < 0.78) continue;
			addFreeze(w, (LivingEntity) e, src, isBoss(e) ? 1.5f : 5f);
		}
		// Water in the breath freezes over
		for (int i = 1; i <= 7; i += 2) {
			BlockPos bp = BlockPos.ofFloored(from.add(dir.multiply(i)));
			for (BlockPos q : new BlockPos[]{bp, bp.down()}) {
				if (w.getBlockState(q).isOf(Blocks.WATER) && w.getBlockState(q).get(FluidBlock.LEVEL) == 0 && w.getBlockState(q.up()).isAir()) {
					w.setBlockState(q, Blocks.FROSTED_ICE.getDefaultState());
				}
			}
		}
	}

	private static void addFreeze(ServerWorld w, LivingEntity e, LivingEntity by, float amount) {
		Freeze f = MAP.computeIfAbsent(e.getUuid(), k -> new Freeze(e));
		e.extinguish();
		e.setFrozenTicks(Math.max(e.getFrozenTicks(), Math.min(e.getMinFreezeDamageTicks() + 20, e.getFrozenTicks() + 12)));
		if (f.frozen > 0) {
			f.frozen = Math.max(f.frozen, isBoss(e) ? 30 : 120);
			return;
		}
		f.buildup += amount;
		e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, Math.min(4, (int) (f.buildup / 10))), by);
		if (f.buildup >= FREEZE_AT) freezeSolid(w, f, by);
	}

	private static void freezeSolid(ServerWorld w, Freeze f, LivingEntity by) {
		LivingEntity e = f.e;
		f.frozen = isBoss(e) ? 30 : 120;
		f.at = e.getPos();
		if (e instanceof MobEntity mob && !f.aiSaved) {
			f.prevAi = mob.isAiDisabled();
			f.aiSaved = true;
			mob.setAiDisabled(true);
		}
		w.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.PLAYERS, 1.5f, 0.6f);
		w.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.5f, 1.4f);
		w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.ICE.getDefaultState()),
				e.getX(), e.getY() + e.getHeight() / 2, e.getZ(), 30, e.getWidth() / 2, e.getHeight() / 2, e.getWidth() / 2, 0.1);
		if (e instanceof ServerPlayerEntity sp) {
			sp.sendMessage(Text.literal("You're frozen solid!").formatted(Formatting.AQUA, Formatting.BOLD), true);
		}
		if (by instanceof ServerPlayerEntity bp && !isBoss(e)) {
			GhostPowers.bar(bp, e.getName().getString() + " is frozen solid! Punch it when it's weak to shatter it.", Formatting.AQUA);
		}
	}

	private static void thaw(Freeze f) {
		LivingEntity e = f.e;
		if (e instanceof MobEntity mob && f.aiSaved) mob.setAiDisabled(f.prevAi);
		f.aiSaved = false;
		f.frozen = 0;
		f.buildup = 0;
		if (e.getWorld() instanceof ServerWorld w && e.isAlive()) {
			w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.ICE.getDefaultState()),
					e.getX(), e.getY() + e.getHeight() / 2, e.getZ(), 20, e.getWidth() / 2, e.getHeight() / 2, e.getWidth() / 2, 0.1);
			w.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.BLOCK_GLASS_HIT, SoundCategory.PLAYERS, 1.0f, 1.2f);
		}
	}

	// ------------------------------------------------------------------ shatter

	/** A punch on a frozen, badly hurt enemy shatters it. Returns true if it shattered. */
	public static boolean tryShatter(ServerPlayerEntity attacker, LivingEntity target) {
		Freeze f = MAP.get(target.getUuid());
		if (f == null || f.frozen <= 0 || isBoss(target)) return false;
		if (target.getHealth() > Math.max(6f, target.getMaxHealth() * 0.35f)) return false;
		shatter(attacker.getServerWorld(), target, attacker);
		return true;
	}

	public static void shatter(ServerWorld w, LivingEntity target, ServerPlayerEntity by) {
		Freeze f = MAP.remove(target.getUuid());
		if (f != null && target instanceof MobEntity mob && f.aiSaved) mob.setAiDisabled(f.prevAi);
		double x = target.getX(), y = target.getY() + target.getHeight() / 2, z = target.getZ();
		double wd = target.getWidth() / 2 + 0.2, ht = target.getHeight() / 2 + 0.2;
		// Burst of ice chunks, glittering frost and a ring of cold
		w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.ICE.getDefaultState()), x, y, z, 120, wd, ht, wd, 0.35);
		w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()), x, y, z, 60, wd, ht, wd, 0.5);
		w.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 80, wd, ht, wd, 0.35);
		w.spawnParticles(FROST, x, y, z, 60, wd * 1.5, ht, wd * 1.5, 0);
		w.spawnParticles(ParticleTypes.END_ROD, x, y, z, 20, wd, ht, wd, 0.15);
		w.spawnParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
		Vec3d ground = BossMoves.ground(w, target.getPos());
		BossMoves.send(w, new ShockwavePayload((float) ground.x, (float) ground.y, (float) ground.z, 0, 1, 0, 3.5f, 0xAEEBFF, 1));
		w.playSound(null, x, y, z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2.0f, 0.6f);
		w.playSound(null, x, y, z, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 2.0f, 0.8f);
		w.playSound(null, x, y, z, SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1.5f, 0.5f);
		target.setFrozenTicks(0);
		target.damage(by != null ? w.getDamageSources().playerAttack(by) : w.getDamageSources().freeze(), 10000f);
		if (by != null) GhostPowers.bar(by, "SHATTERED!", Formatting.AQUA);
	}

	// ------------------------------------------------------------------ tick

	public static void tick(MinecraftServer server) {
		Iterator<Freeze> it = MAP.values().iterator();
		while (it.hasNext()) {
			Freeze f = it.next();
			LivingEntity e = f.e;
			if (e.isRemoved() || !e.isAlive()) {
				if (e instanceof MobEntity mob && f.aiSaved) mob.setAiDisabled(f.prevAi);
				it.remove();
				continue;
			}
			if (f.frozen > 0) {
				f.frozen--;
				e.setFrozenTicks(Math.max(e.getFrozenTicks(), e.getMinFreezeDamageTicks()));
				Vec3d v = e.getVelocity();
				e.setVelocity(0, Math.min(0, v.y), 0);
				e.velocityModified = true;
				if (e instanceof ServerPlayerEntity sp && f.at != null) {
					sp.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5, 9, false, false, false));
					sp.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 5, 4, false, false, false));
					if (sp.getPos().squaredDistanceTo(f.at) > 0.04) sp.requestTeleport(f.at.x, Math.min(sp.getY(), f.at.y), f.at.z);
				}
				if (e.age % 10 == 0 && e.getWorld() instanceof ServerWorld w) {
					w.spawnParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + e.getHeight(), e.getZ(), 2, e.getWidth() / 2, 0.1, e.getWidth() / 2, 0.01);
				}
				if (f.frozen == 0) thaw(f);
			} else {
				f.buildup -= 0.5f;
				if (f.buildup <= 0) it.remove();
			}
		}
		syncFx(server);
	}

	/** Tells clients who is possessed (ghost eyes) and who is frozen (ice). */
	private static void syncFx(MinecraftServer server) {
		List<Integer> hosts = new ArrayList<>();
		GhostState st = GhostState.of(server);
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			Entity h = st.data(p.getUuid()).possessing;
			if (h != null) hosts.add(h.getId());
		}
		List<Integer> frozen = new ArrayList<>();
		for (Freeze f : MAP.values()) if (f.frozen > 0) frozen.add(f.e.getId());
		int[] h = hosts.stream().mapToInt(Integer::intValue).toArray();
		int[] fr = frozen.stream().mapToInt(Integer::intValue).toArray();
		Arrays.sort(h);
		Arrays.sort(fr);
		boolean changed = !Arrays.equals(h, lastHosts) || !Arrays.equals(fr, lastFrozen);
		if (!changed && (--syncTimer > 0 || (h.length == 0 && fr.length == 0))) return;
		syncTimer = 40;
		lastHosts = h;
		lastFrozen = fr;
		EntityFxPayload payload = new EntityFxPayload(h, fr);
		for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
			if (ServerPlayNetworking.canSend(viewer, EntityFxPayload.ID)) ServerPlayNetworking.send(viewer, payload);
		}
	}

	public static void clearAll() {
		for (Freeze f : MAP.values()) if (f.e instanceof MobEntity mob && f.aiSaved) mob.setAiDisabled(f.prevAi);
		MAP.clear();
	}

	/** Used by the CI test. */
	public static boolean testFreezeAndShatter(ServerWorld w, LivingEntity victim, LivingEntity by) {
		for (int i = 0; i < 20 && !isFrozen(victim); i++) addFreeze(w, victim, by, 5f);
		if (!isFrozen(victim)) return false;
		victim.setHealth(2f);
		shatter(w, victim, null);
		return !victim.isAlive();
	}
}
