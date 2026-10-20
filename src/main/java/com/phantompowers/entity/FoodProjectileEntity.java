package com.phantompowers.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** School lunch, thrown by Chef Ghoulash: meatloaf, drumsticks, pies... It splats and leaves you slowed down with gravy. */
public class FoodProjectileEntity extends ThrownItemEntity {
	private static final Item[] FOOD = {Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.COOKED_PORKCHOP, Items.PUMPKIN_PIE,
			Items.BAKED_POTATO, Items.COOKED_MUTTON, Items.BREAD, Items.MUSHROOM_STEW};
	private float damage = 4f;

	public FoodProjectileEntity(EntityType<? extends FoodProjectileEntity> type, World world) {
		super(type, world);
	}

	public FoodProjectileEntity(EntityType<? extends FoodProjectileEntity> type, LivingEntity owner, World world, float damage) {
		super(type, owner, world);
		this.damage = damage;
		setItem(new ItemStack(FOOD[world.random.nextInt(FOOD.length)]));
	}

	@Override
	protected Item getDefaultItem() {
		return Items.COOKED_BEEF;
	}

	@Override
	protected void onEntityHit(EntityHitResult hit) {
		super.onEntityHit(hit);
		LivingEntity owner = getOwner() instanceof LivingEntity le ? le : null;
		if (hit.getEntity() == owner || hit.getEntity() instanceof AbstractGhostEntity) return;
		hit.getEntity().damage(getDamageSources().mobProjectile(this, owner), damage);
		if (hit.getEntity() instanceof LivingEntity le) le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 50, 1), owner);
	}

	@Override
	protected void onCollision(HitResult hit) {
		if (hit instanceof EntityHitResult eh && (eh.getEntity() == getOwner() || eh.getEntity() instanceof AbstractGhostEntity)) return;
		super.onCollision(hit);
		if (getWorld() instanceof ServerWorld w) {
			w.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, getStack()), getX(), getY(), getZ(), 18, 0.25, 0.25, 0.25, 0.12);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.HOSTILE, 1.0f, 0.8f);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 0.8f, 1.2f);
			discard();
		}
	}

	@Override
	protected double getGravity() {
		return 0.03;
	}
}
