package com.phantompowers.entity;

import com.phantompowers.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** A knight's ghost in spectral plate armour with a glowing blade. Now and then it phase-dashes straight at you. */
public class SpecterKnightEntity extends WalkerGhostEntity implements Monster {
	private int dashCooldown = 60;

	public SpecterKnightEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 12;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
				.add(EntityAttributes.GENERIC_ARMOR, 8.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void initGoals() {
		addIdleGoals();
		this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		LivingEntity t = getTarget();
		if (dashCooldown > 0) dashCooldown--;
		if (t != null && dashCooldown == 0) {
			double d = squaredDistanceTo(t);
			if (d > 25 && d < 196) {
				dashCooldown = 120 + random.nextInt(60);
				Vec3d dir = t.getPos().subtract(getPos()).normalize();
				startPhase(14);
				setVelocity(dir.x * 1.4, 0.15, dir.z * 1.4);
				velocityModified = true;
				if (getWorld() instanceof ServerWorld w) {
					w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.05);
					w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 1.0f, 0.7f);
				}
			}
		}
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
	public float getSoundPitch() {
		return 0.45f;
	}
}
