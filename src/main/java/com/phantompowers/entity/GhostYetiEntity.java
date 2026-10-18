package com.phantompowers.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Ghost Yeti: a huge shaggy yeti of the Frostbite Tundra. Most are gentle and only fight back if you hit them;
 * some are feral and charge on sight. Feed one raw fish to befriend it: it follows you, fights whatever
 * attacks you (or whatever you attack), and sneak-right-click tells it to stay or follow.
 */
public class GhostYetiEntity extends WalkerGhostEntity {
	private static final TrackedData<Boolean> FERAL = DataTracker.registerData(GhostYetiEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> SITTING = DataTracker.registerData(GhostYetiEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> TAMED = DataTracker.registerData(GhostYetiEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private UUID owner;
	private int slamCooldown = 60;

	public GhostYetiEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 10;
		if (!world.isClient) dataTracker.set(FERAL, random.nextFloat() < 0.25f);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9.0)
				.add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.2)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(FERAL, false);
		builder.add(SITTING, false);
		builder.add(TAMED, false);
	}

	public boolean isFeral() {
		return dataTracker.get(FERAL);
	}

	public boolean isSitting() {
		return dataTracker.get(SITTING);
	}

	public boolean isTamed() {
		return dataTracker.get(TAMED);
	}

	public LivingEntity getOwner() {
		if (owner == null || !(getWorld() instanceof ServerWorld w)) return null;
		return w.getPlayerByUuid(owner);
	}

	@Override
	protected void initGoals() {
		addIdleGoals();
		this.goalSelector.add(1, new StayGoal());
		this.goalSelector.add(2, new MeleeAttackGoal(this, 1.25, true));
		this.goalSelector.add(4, new FollowOwnerGoal());
		this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
		this.targetSelector.add(2, new DefendOwnerGoal(false));
		this.targetSelector.add(3, new DefendOwnerGoal(true));
		this.targetSelector.add(4, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, e -> isFeral() && !isTamed()));
		this.targetSelector.add(5, new ActiveTargetGoal<>(this, LivingEntity.class, 10, true, false,
				e -> isTamed() && e instanceof Monster && !(e instanceof GhostYetiEntity) && ownerNear(e, 14)));
	}

	private boolean ownerNear(Entity e, double r) {
		LivingEntity o = getOwner();
		return o != null && o.squaredDistanceTo(e) < r * r;
	}

	@Override
	public void setTarget(LivingEntity target) {
		if (target != null && isTamed() && target.getUuid().equals(owner)) return;
		if (target instanceof GhostYetiEntity y && y.isTamed() && owner != null && owner.equals(y.owner)) return;
		super.setTarget(target);
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		boolean fish = stack.isOf(Items.COD) || stack.isOf(Items.SALMON) || stack.isOf(Items.TROPICAL_FISH) || stack.isOf(Items.COOKED_COD) || stack.isOf(Items.COOKED_SALMON);
		if (getWorld().isClient) return fish || isTamed() ? ActionResult.SUCCESS : ActionResult.PASS;
		ServerWorld w = (ServerWorld) getWorld();
		if (isTamed() && player.getUuid().equals(owner)) {
			if (fish && getHealth() < getMaxHealth()) {
				stack.decrementUnlessCreative(1, player);
				heal(12f);
				w.spawnParticles(ParticleTypes.HEART, getX(), getY() + 2.4, getZ(), 3, 0.4, 0.2, 0.4, 0);
				return ActionResult.CONSUME;
			}
			if (player.isSneaking()) {
				boolean sit = !isSitting();
				dataTracker.set(SITTING, sit);
				getNavigation().stop();
				setTarget(null);
				player.sendMessage(Text.literal(sit ? "Your yeti plops down and waits." : "Your yeti gets up and follows you.").formatted(Formatting.AQUA), true);
				return ActionResult.CONSUME;
			}
			return ActionResult.PASS;
		}
		if (fish && !isTamed()) {
			stack.decrementUnlessCreative(1, player);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL, 1.2f, 0.6f);
			if (random.nextInt(isFeral() ? 6 : 3) == 0) {
				owner = player.getUuid();
				dataTracker.set(TAMED, true);
				dataTracker.set(FERAL, false);
				setTarget(null);
				setAttacker(null);
				setPersistent();
				getNavigation().stop();
				w.spawnParticles(ParticleTypes.HEART, getX(), getY() + 2.4, getZ(), 8, 0.6, 0.3, 0.6, 0);
				w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_POLAR_BEAR_AMBIENT, SoundCategory.NEUTRAL, 1.5f, 0.6f);
				player.sendMessage(Text.literal("The Ghost Yeti is your friend now! It will follow you and fight by your side. (Sneak + right-click: stay / follow)")
						.formatted(Formatting.AQUA), false);
			} else {
				w.spawnParticles(ParticleTypes.SMOKE, getX(), getY() + 2.4, getZ(), 6, 0.4, 0.2, 0.4, 0.01);
			}
			return ActionResult.CONSUME;
		}
		return super.interactMob(player, hand);
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (slamCooldown > 0) slamCooldown--;
		LivingEntity t = getTarget();
		// ground slam: frost bursts out and everything nearby (but friends) is knocked back and slowed
		if (t != null && slamCooldown == 0 && squaredDistanceTo(t) < 12 && isOnGround()) {
			slamCooldown = 100 + random.nextInt(60);
			ServerWorld w = (ServerWorld) getWorld();
			swingHand(Hand.MAIN_HAND);
			w.spawnParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.2, getZ(), 60, 2.0, 0.2, 2.0, 0.08);
			w.spawnParticles(ParticleTypes.ITEM_SNOWBALL, getX(), getY() + 0.3, getZ(), 30, 1.5, 0.2, 1.5, 0.1);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_STUNNED, SoundCategory.HOSTILE, 1.2f, 0.7f);
			for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(3.5, 1, 3.5), e -> e != this && e.isAlive())) {
				if (isTamed() && (e.getUuid().equals(owner) || e instanceof GhostYetiEntity)) continue;
				if (!isTamed() && !isFeral() && e != t) continue;
				e.damage(getDamageSources().mobAttack(this), 6f);
				Vec3d push = e.getPos().subtract(getPos()).normalize();
				e.addVelocity(push.x * 1.1, 0.5, push.z * 1.1);
				e.velocityModified = true;
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 1), this);
			}
		}
		// keep up with the owner (even if they run far ahead)
		LivingEntity o = getOwner();
		if (o != null && !isSitting() && squaredDistanceTo(o) > 30 * 30 && o.getWorld() == getWorld()) {
			Vec3d at = o.getPos().add(o.getRotationVector().multiply(-2));
			refreshPositionAndAngles(at.x, o.getY(), at.z, getYaw(), getPitch());
			getNavigation().stop();
		}
		if (isTamed() && age % 100 == 0 && getHealth() < getMaxHealth()) heal(1f);
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit) {
			target.addVelocity(0, 0.35, 0);
			target.velocityModified = true;
		}
		return hit;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return !isTamed() && super.canImmediatelyDespawn(distanceSquared);
	}

	@Override
	protected boolean isDisallowedInPeaceful() {
		return false;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Feral", isFeral());
		nbt.putBoolean("Sitting", isSitting());
		if (owner != null) nbt.putUuid("Owner", owner);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		dataTracker.set(FERAL, nbt.getBoolean("Feral"));
		dataTracker.set(SITTING, nbt.getBoolean("Sitting"));
		if (nbt.containsUuid("Owner")) {
			owner = nbt.getUuid("Owner");
			dataTracker.set(TAMED, true);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_POLAR_BEAR_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
		return SoundEvents.ENTITY_POLAR_BEAR_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_POLAR_BEAR_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return 0.55f + random.nextFloat() * 0.1f;
	}

	// ---------------------------------------------------------------- goals

	/** Sitting yetis stay put. */
	class StayGoal extends Goal {
		StayGoal() {
			setControls(EnumSet.of(Control.MOVE, Control.JUMP));
		}

		@Override
		public boolean canStart() {
			return isTamed() && isSitting();
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
			return o != null && !isSitting() && getTarget() == null && squaredDistanceTo(o) > 36;
		}

		@Override
		public boolean shouldContinue() {
			LivingEntity o = getOwner();
			return o != null && !isSitting() && getTarget() == null && squaredDistanceTo(o) > 9;
		}

		@Override
		public void tick() {
			LivingEntity o = getOwner();
			if (o == null) return;
			getLookControl().lookAt(o, 10f, getMaxLookPitchChange());
			if (--repath <= 0) {
				repath = 10;
				getNavigation().startMovingTo(o, 1.2);
			}
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}
	}

	/** Attack whatever hurt the owner (or whatever the owner is attacking). */
	class DefendOwnerGoal extends TrackTargetGoal {
		private final boolean ownersTarget;
		private LivingEntity found;
		private int lastTime;

		DefendOwnerGoal(boolean ownersTarget) {
			super(GhostYetiEntity.this, false);
			this.ownersTarget = ownersTarget;
			setControls(EnumSet.of(Control.TARGET));
		}

		@Override
		public boolean canStart() {
			LivingEntity o = getOwner();
			if (o == null || isSitting()) return false;
			found = ownersTarget ? o.getAttacking() : o.getAttacker();
			int time = ownersTarget ? o.getLastAttackTime() : o.getLastAttackedTime();
			if (found == null || found == GhostYetiEntity.this || time == lastTime) return false;
			if (found instanceof GhostYetiEntity y && owner.equals(y.owner)) return false;
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
