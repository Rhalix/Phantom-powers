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
	}

	public void setup(UUID owner, String name, boolean female) {
		dataTracker.set(OWNER, Optional.of(owner));
		dataTracker.set(FEMALE, female);
		setCustomName(Text.literal(name + "'s Clone"));
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
		goalSelector.add(2, new MeleeAttackGoal(this, 1.25, true));
		goalSelector.add(4, new FollowOwnerGoal());
		goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
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

	@Override
	protected void mobTick() {
		super.mobTick();
		ServerWorld w = (ServerWorld) getWorld();
		LivingEntity t = getTarget();
		boolean ghost = t != null && t.isAlive();
		if (ghost != isGhostForm()) {
			dataTracker.set(GHOST, ghost);
			w.spawnParticles(GhostPowers.GREEN, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.NEUTRAL, 0.7f, 1.4f);
		}
		if (blastCooldown > 0) blastCooldown--;
		if (t != null && blastCooldown == 0 && canSee(t)) {
			double d = squaredDistanceTo(t);
			if (d > 12 && d < 18 * 18) {
				blastCooldown = 35;
				Vec3d from = getEyePos().add(getRotationVector().multiply(0.5));
				Vec3d to = t.getBoundingBox().getCenter();
				BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) to.x, (float) to.y, (float) to.z, 0x33FF77);
				for (ServerPlayerEntity v : PlayerLookup.around(w, from, 64)) {
					if (ServerPlayNetworking.canSend(v, BeamPayload.ID)) ServerPlayNetworking.send(v, beam);
				}
				t.damage(getDamageSources().indirectMagic(this, getOwner()), 5f);
				w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.NEUTRAL, 0.6f, 1.6f);
				getLookControl().lookAt(t);
			}
		}
		LivingEntity o = getOwner();
		if (o != null && !isStaying() && squaredDistanceTo(o) > 32 * 32 && o.getWorld() == getWorld()) {
			Vec3d at = o.getPos().add(o.getRotationVector().multiply(-2));
			refreshPositionAndAngles(at.x, o.getY(), at.z, getYaw(), getPitch());
			getNavigation().stop();
		}
		if (age % 80 == 0 && getHealth() < getMaxHealth()) heal(1f);
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
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.containsUuid("CloneOwner")) dataTracker.set(OWNER, Optional.of(nbt.getUuid("CloneOwner")));
		dataTracker.set(FEMALE, nbt.getBoolean("Female"));
		dataTracker.set(STAYING, nbt.getBoolean("Staying"));
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
			return o != null && !isStaying() && getTarget() == null && squaredDistanceTo(o) > 25;
		}

		@Override
		public boolean shouldContinue() {
			LivingEntity o = getOwner();
			return o != null && !isStaying() && getTarget() == null && squaredDistanceTo(o) > 6;
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
