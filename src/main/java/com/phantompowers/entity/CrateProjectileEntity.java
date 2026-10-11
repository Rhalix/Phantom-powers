package com.phantompowers.entity;

import com.phantompowers.ModBlocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** A crate hurled by the Crate King. Smashes into splinters on impact. */
public class CrateProjectileEntity extends ThrownItemEntity {
	private float damage = 5f;

	public CrateProjectileEntity(EntityType<? extends CrateProjectileEntity> type, World world) {
		super(type, world);
	}

	public CrateProjectileEntity(EntityType<? extends CrateProjectileEntity> type, LivingEntity owner, World world, float damage) {
		super(type, owner, world);
		this.damage = damage;
	}

	@Override
	protected Item getDefaultItem() {
		return ModBlocks.GHOST_CRATE_ITEM;
	}

	@Override
	protected void onEntityHit(EntityHitResult hit) {
		super.onEntityHit(hit);
		LivingEntity owner = getOwner() instanceof LivingEntity le ? le : null;
		if (hit.getEntity() == owner) return;
		hit.getEntity().damage(getDamageSources().mobProjectile(this, owner), damage);
	}

	@Override
	protected void onCollision(HitResult hit) {
		super.onCollision(hit);
		if (getWorld() instanceof ServerWorld w) {
			if (hit instanceof EntityHitResult eh && eh.getEntity() == getOwner()) return;
			w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ModBlocks.GHOST_CRATE.getDefaultState()),
					getX(), getY(), getZ(), 25, 0.35, 0.35, 0.35, 0.15);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.8f, 1.3f);
			discard();
		}
	}

	@Override
	protected double getGravity() {
		return 0.02;
	}
}
