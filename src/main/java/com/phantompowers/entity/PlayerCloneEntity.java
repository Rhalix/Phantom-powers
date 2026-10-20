package com.phantompowers.entity;

import com.phantompowers.GhostPowers;
import com.phantompowers.network.BeamPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * A clone grown from your DNA in a Cloning Pod: looks like you (as a girl or a boy, whichever you picked),
 * follows you around, and when a fight starts it goes ghost and joins in with punches and ecto blasts.
 */
public class PlayerCloneEntity extends PathAwareEntity {
	private static final TrackedData<Optional<UUID>> OWNER = DataTracker.registerData(PlayerCloneEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
	private static final TrackedData<Boolean> FEMALE = DataTracker.registerData(PlayerCloneEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> GHOST = DataTracker.registerData(PlayerCloneEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> DARK = DataTracker.registerData(PlayerCloneEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> STAYING = DataTracker.registerData(PlayerCloneEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final String[] LINES = {"Ready when you are!", "I've got your back.", "Weird... I remember things I never did.",
			"Going ghost? Me too!", "Do you think I'm the original?", "Race you!"};
	private int blastCooldown = 40;

	public PlayerCloneEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 0;
		setPersistent();
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(OWNER, Optional.empty());
		builder.add(FEMALE, false);
		builder.add(GHOST, false);
		builder.add(STAYING, false);
		builder.add(DARK, false);
	}

	public void setup(UUID owner, String name, boolean female) {
		setup(owner, name, female, false);
	}

	/** Grown from ghost DNA: the clone always comes out in ghost form (evil ghost form for evil half-ghost DNA). */
	public void setup(UUID owner, String name, boolean female, boolean dark) {
		dataTracker.set(OWNER, Optional.of(owner));
		dataTracker.set(FEMALE, female);
		dataTracker.set(DARK, dark);
		dataTracker.set(GHOST, true);
		formHold = 100;
		setCustomName(Text.literal(name + "'s " + (dark ? "Evil " : "") + "Clone"));
	}

	public boolean isDark() {
		return dataTracker.get(DARK);
	}

	public UUID ownerId() {
		return dataTracker.get(OWNER).orElse(null);
	}

	public boolean isFemale() {
		return dataTracker.get(FEMALE);
	}

	public boolean isGhostForm() {
		return dataTracker.get(GHOST);
	}

	public boolean isStaying() {
		return dataTracker.get(STAYING);
	}

	public LivingEntity getOwner() {
		UUID id = ownerId();
		if (id == null || !(getWorld() instanceof ServerWorld w)) return null;
		return w.getPlayerByUuid(id);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new StayGoal());
		goalSelector.add(2, new MeleeAttackGoal(this, 1.25, true) {
			@Override
			public boolean canStart() {
				return !isGhostForm() && super.canStart();
			}

			@Override
			public boolean shouldContinue() {
				return !isGhostForm() && super.shouldContinue();
			}
		});
		goalSelector.add(4, new FollowOwnerGoal());
		goalSelector.add(6, new WanderAroundFarGoal(this, 0.8) {
			@Override
			public boolean canStart() {
				return !isGhostForm() && super.canStart();
			}
		});
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new DefendOwnerGoal(false));
		targetSelector.add(3, new DefendOwnerGoal(true));
		targetSelector.add(4, new ActiveTargetGoal<>(this, LivingEntity.class, 10, true, false,
				e -> e instanceof Monster && ownerNear(e, 16)));
	}

	private boolean ownerNear(Entity e, double r) {
		LivingEntity o = getOwner();
		return o != null && o.squaredDistanceTo(e) < r * r;
	}

	@Override
	public void setTarget(LivingEntity target) {
		if (target != null && target.getUuid().equals(ownerId())) return;
		if (target instanceof PlayerCloneEntity c && c.ownerId() != null && c.ownerId().equals(ownerId())) return;
		if (target instanceof GhostCloneEntity gc && gc.owner().map(id -> id.equals(ownerId())).orElse(false)) return;
		super.setTarget(target);
	}

	private int formHold, skills, meleeCooldown, frostCd = 60, wailCd = 120, stingCd = 80, spinCd = 160, tornadoCd = 200, invisCd;
	private boolean ownerDark;

	private boolean can(int skill) {
		return (skills & (1 << skill)) != 0;
	}

	/** Evil powers if it was grown from evil DNA, or if its original is the evil half-ghost right now. */
	private boolean evil() {
		return isDark() || ownerDark;
	}

	private int color() {
		return isDark() ? 0xFF4FC8 : 0x33FF77;
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		ServerWorld w = (ServerWorld) getWorld();
		LivingEntity o = getOwner();
		// Copy the original: its powers, and whether it's in ghost form right now
		if (o instanceof ServerPlayerEntity sp && age % 20 == 0) {
			com.phantompowers.GhostData d = com.phantompowers.GhostState.of(sp.getServer()).data(sp.getUuid());
			skills = d.unlocked | 1;
			ownerDark = d.dark && d.darkMode;
			if (formHold > 0) formHold = Math.max(0, formHold - 20);
			else if (d.transformed != isGhostForm()) switchForm(w, d.transformed);
		}
		LivingEntity t = getTarget();
		if (t != null && !t.isAlive()) setTarget(t = null);
		if (meleeCooldown > 0) meleeCooldown--;
		if (blastCooldown > 0) blastCooldown--;
		if (frostCd > 0) frostCd--;
		if (wailCd > 0) wailCd--;
		if (stingCd > 0) stingCd--;
		if (spinCd > 0) spinCd--;
		if (tornadoCd > 0) tornadoCd--;
		if (invisCd > 0) invisCd--;
		setNoGravity(isGhostForm());
		if (isGhostForm()) {
			fallDistance = 0;
			fly(o, t);
			if (t != null) usePowers(w, t);
		}
		if (o != null && !isStaying() && squaredDistanceTo(o) > 32 * 32 && o.getWorld() == getWorld()) {
			Vec3d at = o.getPos().add(o.getRotationVector().multiply(-2));
			refreshPositionAndAngles(at.x, o.getY(), at.z, getYaw(), getPitch());
			getNavigation().stop();
		}
		if (age % 80 == 0 && getHealth() < getMaxHealth()) heal(1f);
	}

	/** Going ghost (or back to human) along with the original: the same rings sweep over the clone. */
	private void switchForm(ServerWorld w, boolean ghost) {
		dataTracker.set(GHOST, ghost);
		var ring = new com.phantompowers.network.RingPayload(getId(), 20, ghost ? 0 : 4);
		for (ServerPlayerEntity v : PlayerLookup.around(w, getPos(), 96)) {
			if (ServerPlayNetworking.canSend(v, com.phantompowers.network.RingPayload.ID)) ServerPlayNetworking.send(v, ring);
		}
		w.playSound(null, getX(), getY(), getZ(), ghost ? SoundEvents.BLOCK_BEACON_ACTIVATE : SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.NEUTRAL, 0.7f, 1.7f);
		if (!ghost) {
			setNoGravity(false);
			setInvisible(false);
		}
	}

	/** Ghost form flies: hovers beside you, follows you into the air, and swoops at whatever it's fighting. */
	private void fly(LivingEntity o, LivingEntity t) {
		getNavigation().stop();
		Vec3d dest = null;
		if (t != null) {
			Vec3d away = getPos().subtract(t.getPos());
			away = new Vec3d(away.x, 0, away.z);
			away = away.lengthSquared() < 1e-3 ? new Vec3d(1, 0, 0) : away.normalize();
			dest = t.getPos().add(away.multiply(1.6)).add(0, t.getHeight() * 0.25, 0);
			getLookControl().lookAt(t, 30f, 30f);
			if (squaredDistanceTo(t) < 4.5 && meleeCooldown == 0) {
				meleeCooldown = 12;
				swingHand(Hand.MAIN_HAND);
				tryAttack(t);
				if (can(com.phantompowers.GhostPowers.FISTS)) {
					t.damage(getDamageSources().mobAttack(this), 3f);
					((ServerWorld) getWorld()).spawnParticles(isDark() ? PINK : GhostPowers.GREEN, t.getX(), t.getY() + 1, t.getZ(), 10, 0.3, 0.3, 0.3, 0);
				}
			}
		} else if (o != null && !isStaying() && o.getWorld() == getWorld()) {
			float side = (getId() % 2 == 0 ? 120f : -120f);
			Vec3d off = Vec3d.fromPolar(0, o.getYaw() + side).multiply(2.0);
			dest = o.getPos().add(off).add(0, o.isOnGround() ? 0.3 : 0.2, 0);
			if (squaredDistanceTo(o) < 64) getLookControl().lookAt(o, 10f, 10f);
		}
		Vec3d v = getVelocity();
		if (dest != null) {
			Vec3d d = dest.subtract(getPos());
			double len = d.length();
			Vec3d want = len > 0.25 ? d.multiply(Math.min(0.75, len * 0.2) / len) : Vec3d.ZERO;
			v = v.lerp(want, 0.35);
			if (len > 0.5) {
				float yaw = (float) (MathHelper.atan2(d.z, d.x) * MathHelper.DEGREES_PER_RADIAN) - 90f;
				if (t == null) setYaw(MathHelper.stepUnwrappedAngleTowards(getYaw(), yaw, 15f));
			}
		} else {
			v = v.multiply(0.6);
		}
		v = v.add(0, MathHelper.sin(age * 0.15f) * 0.006, 0);
		setVelocity(v);
		bodyYaw = getYaw();
		if (age % 3 == 0) ((ServerWorld) getWorld()).spawnParticles(isDark() ? PINK : GhostPowers.GREEN, getX(), getY() + 0.2, getZ(), 1, 0.15, 0.05, 0.15, 0);
	}

	private static final net.minecraft.particle.DustParticleEffect PINK = new net.minecraft.particle.DustParticleEffect(new org.joml.Vector3f(1f, 0.31f, 0.78f), 1.3f);

	private void beam(ServerWorld w, Vec3d from, Vec3d to, int color) {
		BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) to.x, (float) to.y, (float) to.z, color);
		for (ServerPlayerEntity v : PlayerLookup.around(w, from, 64)) {
			if (ServerPlayNetworking.canSend(v, BeamPayload.ID)) ServerPlayNetworking.send(v, beam);
		}
	}

	/** The same powers as the original (whatever it has unlocked), hero or evil. */
	private void usePowers(ServerWorld w, LivingEntity t) {
		if (!canSee(t)) return;
		double d = squaredDistanceTo(t);
		Vec3d from = getEyePos().add(getRotationVector().multiply(0.5));
		Vec3d to = t.getBoundingBox().getCenter();
		float bonus = com.phantompowers.HunterSuit.ghostBonus(t);
		// Dark: stinger dash and shock
		if (evil() && can(com.phantompowers.GhostPowers.STINGER) && stingCd == 0 && d < 16 * 16) {
			stingCd = 140;
			Vec3d at = to.add(getPos().subtract(t.getPos()).normalize().multiply(1.2));
			refreshPositionAndAngles(at.x, t.getY(), at.z, getYaw(), getPitch());
			beam(w, getEyePos(), to, 0xFF4FC8);
			t.damage(getDamageSources().indirectMagic(this, getOwner()), 7f * bonus);
			t.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS, 40, 2));
			w.spawnParticles(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 25, 0.4, 0.6, 0.4, 0.3);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.NEUTRAL, 0.6f, 1.6f);
			return;
		}
		// Dark: spin, flinging everything nearby away
		if (evil() && can(com.phantompowers.GhostPowers.SPIN) && spinCd == 0 && d < 9) {
			spinCd = 260;
			for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(4.5), this::enemy)) {
				e.damage(getDamageSources().mobAttack(this), 5f * com.phantompowers.HunterSuit.ghostBonus(e));
				Vec3d push = e.getPos().subtract(getPos()).normalize();
				e.takeKnockback(1.4, -push.x, -push.z);
			}
			w.spawnParticles(PINK, getX(), getY() + 1, getZ(), 60, 2, 0.6, 2, 0.1);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BREEZE_WIND_BURST.value(), SoundCategory.NEUTRAL, 0.8f, 0.8f);
			return;
		}
		// Ghostly wail: a cone of sound that blasts enemies back
		if (can(com.phantompowers.GhostPowers.WAIL) && wailCd == 0 && d < 8 * 8) {
			wailCd = 280;
			Vec3d dir = to.subtract(from).normalize();
			BossMoves.send(w, new com.phantompowers.network.ShockwavePayload((float) from.x, (float) from.y, (float) from.z,
					(float) dir.x, (float) dir.y, (float) dir.z, 9f, color(), 0));
			for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(9), this::enemy)) {
				Vec3d to2 = e.getPos().subtract(getPos());
				if (to2.normalize().dotProduct(dir) < 0.6) continue;
				e.damage(getDamageSources().indirectMagic(this, getOwner()), 6f * com.phantompowers.HunterSuit.ghostBonus(e));
				e.takeKnockback(1.6, -dir.x, -dir.z);
			}
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.NEUTRAL, 0.6f, 1.6f);
			return;
		}
		// Tornado: a whirlwind that drags enemies round and hurts them
		if (can(com.phantompowers.GhostPowers.TORNADO) && tornadoCd == 0 && d < 12 * 12) {
			tornadoCd = 360;
			for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, t.getBoundingBox().expand(5), this::enemy)) {
				Vec3d pull = t.getPos().subtract(e.getPos());
				e.setVelocity(e.getVelocity().add(pull.multiply(0.15)).add(0, 0.6, 0));
				e.velocityModified = true;
				e.damage(getDamageSources().mobAttack(this), 4f);
			}
			w.spawnParticles(net.minecraft.particle.ParticleTypes.CLOUD, t.getX(), t.getY() + 1, t.getZ(), 50, 1.5, 1.5, 1.5, 0.15);
			w.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.ENTITY_BREEZE_WHIRL, SoundCategory.NEUTRAL, 1f, 0.8f);
			return;
		}
		// Ice beam
		if (can(com.phantompowers.GhostPowers.FROST) && frostCd == 0 && d < 16 * 16) {
			frostCd = 140;
			beam(w, from, to, 0xAEEBFF);
			t.damage(getDamageSources().freeze(), 4f * bonus);
			t.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS, 80, 3));
			t.setFrozenTicks(Math.max(t.getFrozenTicks(), 200));
			w.spawnParticles(net.minecraft.particle.ParticleTypes.SNOWFLAKE, to.x, to.y, to.z, 30, 0.4, 0.6, 0.4, 0.05);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.NEUTRAL, 0.6f, 1.6f);
			return;
		}
		// Ecto blast (everyone has this)
		if (blastCooldown == 0 && d > 9 && d < 18 * 18) {
			blastCooldown = 30;
			beam(w, from, to, color());
			t.damage(getDamageSources().indirectMagic(this, getOwner()), 5f * bonus);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.NEUTRAL, 0.6f, 1.6f);
		}
	}

	private boolean enemy(LivingEntity e) {
		if (e == this || !e.isAlive() || e.getUuid().equals(ownerId())) return false;
		if (e instanceof PlayerCloneEntity c && ownerId() != null && ownerId().equals(c.ownerId())) return false;
		if (e instanceof PlayerEntity) return e == getTarget();
		return e instanceof Monster || e == getTarget() || e instanceof AbstractGhostEntity;
	}

	@Override
	public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
		if (isGhostForm() && !getWorld().isClient) {
			if (can(com.phantompowers.GhostPowers.INTANGIBLE) && random.nextFloat() < 0.2f) return false;  // phased right through it
			if (can(com.phantompowers.GhostPowers.SHIELD)) amount *= 0.6f;
			if (evil() && can(com.phantompowers.GhostPowers.ABSORB)) heal(amount * 0.25f);
			if (can(com.phantompowers.GhostPowers.INVISIBLE) && invisCd == 0 && getHealth() - amount < getMaxHealth() * 0.4f) {
				invisCd = 600;
				addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.INVISIBILITY, 100, 0, false, false));
			}
		}
		return super.damage(source, amount);
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, net.minecraft.entity.damage.DamageSource damageSource) {
		return !isGhostForm() && super.handleFallDamage(fallDistance, damageMultiplier, damageSource);
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (getWorld().isClient) return ActionResult.SUCCESS;
		if (!player.getUuid().equals(ownerId())) {
			player.sendMessage(Text.literal(getName().getString() + ": \"You're not my original.\"").formatted(Formatting.GREEN), false);
			return ActionResult.CONSUME;
		}
		if (player.isSneaking()) {
			boolean stay = !isStaying();
			dataTracker.set(STAYING, stay);
			getNavigation().stop();
			setTarget(null);
			player.sendMessage(Text.literal(stay ? "Your clone waits here." : "Your clone follows you.").formatted(Formatting.GREEN), true);
		} else {
			player.sendMessage(Text.literal(getName().getString() + ": \"" + LINES[random.nextInt(LINES.length)] + "\"").formatted(Formatting.GREEN), false);
		}
		((ServerWorld) getWorld()).spawnParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 2, getZ(), 3, 0.3, 0.2, 0.3, 0);
		return ActionResult.CONSUME;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (ownerId() != null) nbt.putUuid("CloneOwner", ownerId());
		nbt.putBoolean("Female", isFemale());
		nbt.putBoolean("Staying", isStaying());
		nbt.putBoolean("DarkClone", isDark());
		nbt.putBoolean("GhostForm", isGhostForm());
		nbt.putInt("Skills", skills);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.containsUuid("CloneOwner")) dataTracker.set(OWNER, Optional.of(nbt.getUuid("CloneOwner")));
		dataTracker.set(FEMALE, nbt.getBoolean("Female"));
		dataTracker.set(STAYING, nbt.getBoolean("Staying"));
		dataTracker.set(DARK, nbt.getBoolean("DarkClone"));
		dataTracker.set(GHOST, !nbt.contains("GhostForm") || nbt.getBoolean("GhostForm"));
		skills = nbt.getInt("Skills");
	}

	// ---------------------------------------------------------------- goals

	class StayGoal extends Goal {
		StayGoal() {
			setControls(EnumSet.of(Control.MOVE, Control.JUMP));
		}

		@Override
		public boolean canStart() {
			return isStaying();
		}

		@Override
		public void start() {
			getNavigation().stop();
		}
	}

	class FollowOwnerGoal extends Goal {
		private int repath;

		FollowOwnerGoal() {
			setControls(EnumSet.of(Control.MOVE, Control.LOOK));
		}

		@Override
		public boolean canStart() {
			LivingEntity o = getOwner();
			return o != null && !isGhostForm() && !isStaying() && getTarget() == null && squaredDistanceTo(o) > 25;
		}

		@Override
		public boolean shouldContinue() {
			LivingEntity o = getOwner();
			return o != null && !isGhostForm() && !isStaying() && getTarget() == null && squaredDistanceTo(o) > 6;
		}

		@Override
		public void tick() {
			LivingEntity o = getOwner();
			if (o == null) return;
			getLookControl().lookAt(o, 10f, getMaxLookPitchChange());
			if (--repath <= 0) {
				repath = 10;
				getNavigation().startMovingTo(o, squaredDistanceTo(o) > 144 ? 1.45 : 1.15);
			}
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}
	}

	class DefendOwnerGoal extends TrackTargetGoal {
		private final boolean ownersTarget;
		private LivingEntity found;
		private int lastTime;

		DefendOwnerGoal(boolean ownersTarget) {
			super(PlayerCloneEntity.this, false);
			this.ownersTarget = ownersTarget;
			setControls(EnumSet.of(Control.TARGET));
		}

		@Override
		public boolean canStart() {
			LivingEntity o = getOwner();
			if (o == null || isStaying()) return false;
			found = ownersTarget ? o.getAttacking() : o.getAttacker();
			int time = ownersTarget ? o.getLastAttackTime() : o.getLastAttackedTime();
			if (found == null || found == PlayerCloneEntity.this || time == lastTime) return false;
			if (found instanceof PlayerCloneEntity c && ownerId() != null && ownerId().equals(c.ownerId())) return false;
			return canTrack(found, TargetPredicate.DEFAULT);
		}

		@Override
		public void start() {
			mob.setTarget(found);
			LivingEntity o = getOwner();
			if (o != null) lastTime = ownersTarget ? o.getLastAttackTime() : o.getLastAttackedTime();
			super.start();
		}
	}
}
