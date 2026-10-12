package com.phantompowers.entity;

import com.phantompowers.ModEntities;
import com.phantompowers.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** A thrown Ghost Net: a small spinning glowing cube. Hits a ghost and springs open into a cage. */
public class GhostNetProjectile extends ThrownItemEntity {
	public GhostNetProjectile(EntityType<? extends GhostNetProjectile> type, World world) {
		super(type, world);
	}

	public GhostNetProjectile(World world, LivingEntity owner) {
		super(ModEntities.GHOST_NET_PROJECTILE, owner, world);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.GHOST_NET;
	}

	@Override
	protected double getGravity() {
		return 0.03;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient && age % 2 == 0) {
			getWorld().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.1, getZ(), 0, 0, 0);
		}
	}

	@Override
	protected void onCollision(HitResult hit) {
		super.onCollision(hit);
		if (!(getWorld() instanceof ServerWorld w) || isRemoved()) return;
		Entity owner = getOwner();
		if (hit instanceof EntityHitResult eh) {
			Entity target = eh.getEntity();
			if (target == owner) return;
			if (GhostNetTrapEntity.canNet(target) && GhostNetTrapEntity.trap(w, target, owner)) {
				discard();
				return;
			}
		}
		// Missed (or hit something that isn't a ghost): the net falls to the ground, ready to throw again
		w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.1);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_CHAIN_FALL, SoundCategory.PLAYERS, 0.8f, 1.4f);
		ItemStack drop = getStack().isEmpty() ? new ItemStack(ModItems.GHOST_NET) : getStack().copy();
		dropStack(drop, 0.1f);
		discard();
	}
}
