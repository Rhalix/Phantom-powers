package com.phantompowers.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** A ball of ghostly fire spat by the Rage Wyvern. Bursts in a small blast that sets you alight; never burns blocks. */
public class WyvernFireballEntity extends ThrownItemEntity {
	private float damage = 5f;

	public WyvernFireballEntity(EntityType<? extends WyvernFireballEntity> type, World world) {
		super(type, world);
	}

	public WyvernFireballEntity(EntityType<? extends WyvernFireballEntity> type, LivingEntity owner, World world, float damage) {
		super(type, owner, world);
		this.damage = damage;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.FIRE_CHARGE;
	}

	@Override
	protected double getGravity() {
		return 0.004;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			getWorld().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.1, getZ(), 0, 0, 0);
			if (age % 2 == 0) getWorld().addParticle(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
		} else if (age > 100) burst((ServerWorld) getWorld());
	}

	@Override
	protected void onCollision(HitResult hit) {
		if (hit instanceof EntityHitResult eh && (eh.getEntity() == getOwner() || eh.getEntity() instanceof AbstractGhostEntity)) return;
		super.onCollision(hit);
		if (getWorld() instanceof ServerWorld w) burst(w);
	}

	private void burst(ServerWorld w) {
		Entity owner = getOwner();
		for (Entity e : w.getOtherEntities(this, getBoundingBox().expand(2.2), e -> e instanceof LivingEntity && e != owner
				&& !(e instanceof AbstractGhostEntity) && !(e instanceof VillagerEntity) && !(e instanceof TownspersonEntity))) {
			e.damage(getDamageSources().mobProjectile(this, owner instanceof LivingEntity le ? le : null), damage);
			e.setOnFireFor(3);
		}
		w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 30, 0.6, 0.6, 0.6, 0.08);
		w.spawnParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.0f, 0.6f);
		discard();
	}
}
