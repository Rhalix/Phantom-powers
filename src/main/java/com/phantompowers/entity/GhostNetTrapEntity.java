package com.phantompowers.entity;

import com.phantompowers.FrostLogic;
import com.phantompowers.GhostData;
import com.phantompowers.GhostFlags;
import com.phantompowers.GhostPowers;
import com.phantompowers.GhostState;
import com.phantompowers.ModEntities;
import com.phantompowers.ModItems;
import com.phantompowers.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A Ghost Net that has sprung open around a ghost: a crackling cube cage that holds them in place.
 * Ghosts stay stuck until it fizzles out; half-ghost players can spam jump to tear it apart.
 */
public class GhostNetTrapEntity extends Entity {
	private static final TrackedData<Integer> TARGET = DataTracker.registerData(GhostNetTrapEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> STRUGGLE = DataTracker.registerData(GhostNetTrapEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> BOX_W = DataTracker.registerData(GhostNetTrapEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> BOX_H = DataTracker.registerData(GhostNetTrapEntity.class, TrackedDataHandlerRegistry.FLOAT);

	public static final int PLAYER_FREE = 18;
	private Entity target;
	private Vec3d anchor;
	private boolean prevAi;
	private int life, maxLife, struggle;
	private Entity owner;

	public GhostNetTrapEntity(EntityType<? extends GhostNetTrapEntity> type, World world) {
		super(type, world);
		this.noClip = true;
	}

	// ------------------------------------------------------------------ rules

	public static boolean canNet(Entity e) {
		if (e instanceof AbstractGhostEntity || e instanceof VexEntity || e instanceof PhantomEntity || FrostLogic.isBoss(e)) return !isNetted(e);
		if (e instanceof ServerPlayerEntity sp) {
			GhostData d = GhostState.of(sp.getServer()).data(sp.getUuid());
			return d.transformed && d.possessing == null && !GhostFlags.HOSTS.contains(sp.getUuid()) && !isNetted(e) && !sp.isCreative();
		}
		return false;
	}

	public static boolean isNetted(Entity e) {
		return find(e) != null;
	}

	public static GhostNetTrapEntity find(Entity e) {
		if (!(e.getWorld() instanceof ServerWorld w)) return null;
		for (GhostNetTrapEntity t : w.getEntitiesByClass(GhostNetTrapEntity.class, e.getBoundingBox().expand(3), x -> x.target == e)) return t;
		return null;
	}

	public static boolean trap(ServerWorld w, Entity target, Entity owner) {
		GhostNetTrapEntity net = new GhostNetTrapEntity(ModEntities.GHOST_NET_TRAP, w);
		net.target = target;
		net.owner = owner;
		net.anchor = target.getPos();
		boolean boss = FrostLogic.isBoss(target);
		net.maxLife = target instanceof ServerPlayerEntity ? 260 : boss ? 70 : 220;
		net.dataTracker.set(TARGET, target.getId());
		net.dataTracker.set(BOX_W, target.getWidth() + 0.5f);
		net.dataTracker.set(BOX_H, target.getHeight() + 0.4f);
		net.refreshPositionAndAngles(target.getX(), target.getY(), target.getZ(), 0, 0);
		if (target instanceof MobEntity m) {
			net.prevAi = m.isAiDisabled();
			m.setAiDisabled(true);
		}
		if (!w.spawnEntity(net)) return false;
		Vec3d c = target.getPos().add(0, target.getHeight() / 2, 0);
		w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 40, 0.6, 0.8, 0.6, 0.3);
		w.spawnParticles(GhostPowers.GREEN, c.x, c.y, c.z, 30, 0.6, 0.8, 0.6, 0.05);
		w.spawnParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
		w.playSound(null, c.x, c.y, c.z, ModSounds.NET_ZAP, SoundCategory.PLAYERS, 1.4f, 1.0f);
		w.playSound(null, c.x, c.y, c.z, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 1.2f, 0.7f);
		if (target instanceof ServerPlayerEntity tp) {
			tp.sendMessage(Text.literal("You're caught in a Ghost Net! SPAM JUMP to tear free!").formatted(Formatting.RED, Formatting.BOLD), true);
		}
		if (owner instanceof ServerPlayerEntity op) {
			op.sendMessage(Text.literal("Netted " + target.getName().getString() + "!").formatted(Formatting.AQUA), true);
		}
		return true;
	}

	/** A netted half-ghost pressed jump. */
	public static void struggle(ServerPlayerEntity p) {
		GhostNetTrapEntity net = find(p);
		if (net == null) return;
		net.struggle++;
		ServerWorld w = p.getServerWorld();
		w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 6, 0.4, 0.6, 0.4, 0.15);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 0.7f, 0.9f + net.struggle * 0.04f);
	}

	// ------------------------------------------------------------------ tick

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) return;
		ServerWorld w = (ServerWorld) getWorld();
		if (target == null || target.isRemoved() || !target.isAlive() || target.getWorld() != w) {
			end(w, false, true);
			return;
		}
		life++;
		// Hold them in place
		if (target instanceof ServerPlayerEntity tp) {
			if (tp.getPos().squaredDistanceTo(anchor) > 0.04) tp.requestTeleport(anchor.x, anchor.y, anchor.z);
			tp.setVelocity(Vec3d.ZERO);
			tp.fallDistance = 0;
			if (life % 10 == 0 && struggle > 0) struggle--; // stop struggling and it tightens again
			int bars = Math.min(20, struggle * 20 / PLAYER_FREE);
			tp.sendMessage(Text.literal("GHOST NET! SPAM JUMP  ").formatted(Formatting.RED, Formatting.BOLD)
					.append(Text.literal("|".repeat(bars)).formatted(Formatting.AQUA))
					.append(Text.literal("|".repeat(20 - bars)).formatted(Formatting.DARK_GRAY)), true);
			dataTracker.set(STRUGGLE, Math.min(100, struggle * 100 / PLAYER_FREE));
			if (struggle >= PLAYER_FREE) {
				tp.sendMessage(Text.literal("You tore the Ghost Net apart!").formatted(Formatting.GREEN, Formatting.BOLD), true);
				end(w, true, false);
				return;
			}
		} else {
			target.refreshPositionAndAngles(anchor.x, anchor.y, anchor.z, target.getYaw() + (w.random.nextFloat() - 0.5f) * 6f, target.getPitch());
			target.setVelocity(Vec3d.ZERO);
			// Ghosts thrash around in it
			if (life % 6 == 0) dataTracker.set(STRUGGLE, Math.min(100, life * 100 / maxLife));
		}
		refreshPositionAndAngles(target.getX(), target.getY(), target.getZ(), 0, 0);

		// The mesh zaps whatever's inside
		if (life % 20 == 0 && target instanceof LivingEntity le) {
			DamageSource src = owner instanceof LivingEntity o ? w.getDamageSources().indirectMagic(this, o) : w.getDamageSources().magic();
			le.damage(src, 1.5f * com.phantompowers.HunterSuit.ghostBonus(le));
			Vec3d c = target.getPos().add(0, target.getHeight() / 2, 0);
			w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 14, 0.5, 0.7, 0.5, 0.2);
			w.playSound(null, c.x, c.y, c.z, ModSounds.NET_ZAP, SoundCategory.PLAYERS, 0.6f, 1.2f + w.random.nextFloat() * 0.3f);
		}
		if (life >= maxLife) end(w, false, false);
	}

	/** Release whoever is inside. Torn apart = big burst; otherwise it fizzles (and drops back if the ghost is gone). */
	private void end(ServerWorld w, boolean torn, boolean targetGone) {
		if (target instanceof MobEntity m && !m.isRemoved()) m.setAiDisabled(prevAi);
		Vec3d c = getPos().add(0, dataTracker.get(BOX_H) / 2, 0);
		if (torn) {
			w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 60, 0.8, 1.0, 0.8, 0.4);
			w.spawnParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
			w.playSound(null, c.x, c.y, c.z, SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.4f, 0.7f);
			if (target != null) {
				target.addVelocity(0, 0.5, 0);
				target.velocityModified = true;
			}
		} else {
			w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 20, 0.6, 0.8, 0.6, 0.1);
			w.playSound(null, c.x, c.y, c.z, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.8f);
			if (targetGone && target != null) dropStack(new ItemStack(ModItems.GHOST_NET), 0.2f);
		}
		discard();
	}

	public int targetId() {
		return dataTracker.get(TARGET);
	}

	public float struggleAmount() {
		return dataTracker.get(STRUGGLE) / 100f;
	}

	public float boxWidth() {
		return dataTracker.get(BOX_W);
	}

	public float boxHeight() {
		return dataTracker.get(BOX_H);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(TARGET, -1);
		builder.add(STRUGGLE, 0);
		builder.add(BOX_W, 1f);
		builder.add(BOX_H, 2f);
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}

	@Override
	public boolean shouldSave() {
		return false;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		return false;
	}
}
