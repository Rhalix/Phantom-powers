package com.phantompowers.entity;

import com.phantompowers.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

/** A ghost that walks on two legs (Ghouls, Specter Knights, Lost Souls, Ghost Yetis). */
public abstract class WalkerGhostEntity extends AbstractGhostEntity {
	protected WalkerGhostEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		makeWalker();
	}

	protected void addIdleGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
		this.goalSelector.add(8, new LookAroundGoal(this));
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.GHOST_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.GHOST_DEATH;
	}
}
