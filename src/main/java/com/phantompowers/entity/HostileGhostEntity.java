package com.phantompowers.entity;

import com.phantompowers.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/** A mean ghost that hunts players, phasing through walls to get to them. */
public class HostileGhostEntity extends AbstractGhostEntity implements Monster {
	public HostileGhostEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 22.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new ChargeGoal(this));
		this.goalSelector.add(6, new DriftGoal(this));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit && target instanceof LivingEntity le) {
			le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0), this);
		}
		return hit;
	}

	@Override
	protected boolean isDisallowedInPeaceful() {
		return true;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.GHOST_HOSTILE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.GHOST_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.GHOST_DEATH;
	}

	/** Swoop at the target and claw at them up close. */
	static class ChargeGoal extends Goal {
		private final AbstractGhostEntity ghost;
		private int cooldown;

		ChargeGoal(AbstractGhostEntity ghost) {
			this.ghost = ghost;
			this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
		}

		@Override
		public boolean canStart() {
			LivingEntity t = ghost.getTarget();
			return t != null && t.isAlive();
		}

		@Override
		public void start() {
			ghost.setAttacking(true);
		}

		@Override
		public void stop() {
			ghost.setAttacking(false);
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity t = ghost.getTarget();
			if (t == null) return;
			ghost.getLookControl().lookAt(t, 30f, 30f);
			Vec3d aim = t.getPos().add(0, t.getHeight() * 0.5, 0);
			ghost.flyTo(aim, 1.0);
			if (cooldown > 0) cooldown--;
			if (cooldown == 0 && ghost.squaredDistanceTo(t) < 3.2 && ghost.canSee(t)) {
				ghost.swingHand(ghost.getActiveHand());
				ghost.tryAttack(t);
				cooldown = 20;
			}
		}
	}
}
