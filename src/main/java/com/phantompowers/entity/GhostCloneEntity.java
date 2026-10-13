package com.phantompowers.entity;

import com.phantompowers.GhostData;
import com.phantompowers.GhostPowers;
import com.phantompowers.ModEntities;
import com.phantompowers.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * Duplication: a ghost copy of a half-ghost player. Clones float around their owner and fight only with the
 * Ghostly Wail: they wail at whatever their owner attacks or whatever hurts their owner, and when the owner
 * wails, every clone wails at the same spot. They fade after a while.
 */
public class GhostCloneEntity extends AbstractGhostEntity {
	private static final TrackedData<Optional<UUID>> OWNER = DataTracker.registerData(GhostCloneEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
	private static final TrackedData<Integer> WAILING = DataTracker.registerData(GhostCloneEntity.class, TrackedDataHandlerRegistry.INTEGER);
	public static final int COUNT = 3, LIFE = 600;
	private static final float COST = 35f;
	private int slot, life = LIFE, wailCooldown = 20;

	public GhostCloneEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 0;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.9)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(OWNER, Optional.empty());
		builder.add(WAILING, 0);
	}

	public Optional<UUID> owner() {
		return dataTracker.get(OWNER);
	}

	/** 0..1 how hard the clone is wailing right now (client animation). */
	public float wailing() {
		return Math.min(1f, dataTracker.get(WAILING) / 6f);
	}

	@Override
	protected void initGoals() {
	}

	// ------------------------------------------------------------- spawning

	/** Duplication key: split into clones, or call them back if they're out. */
	public static void toggle(ServerPlayerEntity p, GhostData d) {
		ServerWorld w = p.getServerWorld();
		if (dismiss(p) > 0) {
			GhostPowers.bar(p, "Your clones merge back into you.", Formatting.GRAY);
			return;
		}
		if (d.energy < COST) {
			GhostPowers.bar(p, "Not enough ghost energy to duplicate!", Formatting.RED);
			return;
		}
		d.energy -= COST;
		for (int i = 0; i < COUNT; i++) {
			GhostCloneEntity c = ModEntities.GHOST_CLONE.create(w);
			if (c == null) continue;
			c.dataTracker.set(OWNER, Optional.of(p.getUuid()));
			c.slot = i;
			c.refreshPositionAndAngles(p.getX(), p.getY(), p.getZ(), p.getYaw(), 0);
			c.setCustomName(p.getName().copy().append(Text.literal(" (clone)")));
			c.setCustomNameVisible(false);
			w.spawnEntity(c);
			w.spawnParticles(GhostPowers.GREEN, p.getX(), p.getY() + 1, p.getZ(), 20, 0.5, 0.8, 0.5, 0.05);
		}
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.2f, 1.0f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.8f, 1.6f);
		GhostPowers.bar(p, "Duplication! Your clones wail at whatever you fight. Press again to merge back.", Formatting.GREEN);
	}

	/** Removes this player's clones; returns how many there were. */
	public static int dismiss(ServerPlayerEntity p) {
		int n = 0;
		for (ServerWorld w : p.getServer().getWorlds()) {
			for (GhostCloneEntity c : w.getEntitiesByClass(GhostCloneEntity.class, p.getBoundingBox().expand(256),
					c -> c.owner().map(p.getUuid()::equals).orElse(false))) {
				c.poof(w);
				n++;
			}
		}
		return n;
	}

	/** The owner just wailed: every clone wails at the same spot. */
	public static void wailWith(ServerPlayerEntity p, Vec3d at) {
		ServerWorld w = p.getServerWorld();
		for (GhostCloneEntity c : w.getEntitiesByClass(GhostCloneEntity.class, p.getBoundingBox().expand(48),
				c -> c.owner().map(p.getUuid()::equals).orElse(false))) {
			c.wailAt(w, at);
		}
	}

	// ----------------------------------------------------------------- tick

	private ServerPlayerEntity ownerPlayer(ServerWorld w) {
		return owner().map(id -> w.getServer().getPlayerManager().getPlayer(id)).orElse(null);
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (!(getWorld() instanceof ServerWorld w)) return;
		int wl = dataTracker.get(WAILING);
		if (wl > 0) dataTracker.set(WAILING, wl - 1);
		ServerPlayerEntity p = ownerPlayer(w);
		if (p == null || !p.isAlive() || p.getWorld() != w || --life <= 0 || squaredDistanceTo(p) > 64 * 64) {
			poof(w);
			return;
		}
		// pick a fight: whatever you're attacking, otherwise whatever is attacking you
		LivingEntity target = null;
		LivingEntity hit = p.getAttacking();
		if (hit != null && hit.isAlive() && p.age - p.getLastAttackTime() < 200 && !(hit instanceof GhostCloneEntity)) target = hit;
		LivingEntity attacker = p.getAttacker();
		if (target == null && attacker != null && attacker.isAlive() && !(attacker instanceof GhostCloneEntity)) target = attacker;
		if (target != null && target.squaredDistanceTo(this) > 20 * 20) target = null;

		// float in a loose arc around you (or around the target, keeping a little distance)
		double ang = Math.toRadians(p.getYaw() + 90 + (slot - 1) * 55) + Math.sin(age * 0.05 + slot) * 0.15;
		Vec3d spot = p.getPos().add(Math.cos(ang) * 2.4, 0.4 + Math.sin(age * 0.08 + slot) * 0.25, Math.sin(ang) * 2.4);
		getMoveControl().moveTo(spot.x, spot.y, spot.z, 1.3);
		if (target != null) {
			getLookControl().lookAt(target, 30f, 30f);
			if (--wailCooldown <= 0) {
				wailCooldown = 30 + slot * 4;
				wailAt(w, target.getPos().add(0, target.getHeight() * 0.6, 0));
			}
		} else {
			setYaw(p.getYaw());
			setHeadYaw(p.getHeadYaw());
			setBodyYaw(p.getYaw());
		}
	}

	/** A small Ghostly Wail: a cone of green rings and a shove. Never hurts the owner or other clones. */
	public void wailAt(ServerWorld w, Vec3d at) {
		Vec3d from = getEyePos();
		Vec3d dir = at.subtract(from);
		if (dir.lengthSquared() < 1.0E-4) return;
		dir = dir.normalize();
		float yaw = (float) (MathHelper.atan2(dir.z, dir.x) * 180 / Math.PI) - 90f;
		setYaw(yaw);
		setHeadYaw(yaw);
		setBodyYaw(yaw);
		dataTracker.set(WAILING, 12);
		UUID ownerId = owner().orElse(null);
		ServerPlayerEntity p = ownerPlayer(w);
		BossMoves.wailWave(w, from.add(dir.multiply(0.4)), dir, 12f);
		DamageSource src = p != null ? w.getDamageSources().indirectMagic(this, p) : w.getDamageSources().magic();
		BossMoves.coneHit(w, this, from, dir, 12, 0.82, src, 5f, 1.1,
				e -> !(e instanceof GhostCloneEntity) && (ownerId == null || !e.getUuid().equals(ownerId)));
		w.playSound(null, getX(), getY(), getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.PLAYERS, 0.9f, 1.25f + random.nextFloat() * 0.15f);
	}

	private void poof(ServerWorld w) {
		w.spawnParticles(GhostPowers.GREEN, getX(), getY() + 1, getZ(), 25, 0.4, 0.7, 0.4, 0.05);
		w.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 6, 0.3, 0.5, 0.3, 0.05);
		discard();
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		// your own hits (and your other clones') pass straight through
		Entity a = source.getAttacker();
		if (a != null && (a instanceof GhostCloneEntity || owner().map(a.getUuid()::equals).orElse(false))) return false;
		return super.damage(source, amount);
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (getWorld() instanceof ServerWorld w) w.spawnParticles(GhostPowers.GREEN, getX(), getY() + 1, getZ(), 30, 0.4, 0.7, 0.4, 0.05);
	}

	@Override
	protected void dropLoot(DamageSource source, boolean causedByPlayer) {
	}

	@Override
	public boolean shouldSave() {
		return false;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
	}
}
