package com.phantompowers.entity;

import com.phantompowers.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** One of Roxy's blown kisses: a glowing pink heart. It leaves you lovestruck (slow and weak); heartbreaker kisses explode. */
public class KissEntity extends ThrownItemEntity {
	private boolean explosive;
	private LivingEntity homing;

	public KissEntity(EntityType<? extends KissEntity> type, World world) {
		super(type, world);
	}

	public KissEntity(World world, LivingEntity owner, boolean explosive) {
		super(ModEntities.KISS, owner, world);
		this.explosive = explosive;
	}

	public void setHoming(LivingEntity target) {
		this.homing = target;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.PINK_DYE;
	}

	@Override
	protected double getGravity() {
		return 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient) {
			if (age > 100) discard();
			// gently curve toward the target
			if (homing != null && homing.isAlive() && age > 4 && age < 50) {
				Vec3d want = homing.getPos().add(0, homing.getHeight() * 0.6, 0).subtract(getPos()).normalize().multiply(getVelocity().length());
				setVelocity(getVelocity().lerp(want, 0.08));
				velocityModified = true;
			}
		} else if (age % 2 == 0) {
			getWorld().addParticle(ParticleTypes.HEART, getX(), getY(), getZ(), 0, 0.02, 0);
		}
	}

	@Override
	protected void onEntityHit(EntityHitResult hit) {
		super.onEntityHit(hit);
		Entity t = hit.getEntity();
		if (t == getOwner() || t instanceof AbstractGhostEntity) return;
		Entity owner = getOwner();
		t.damage(getDamageSources().indirectMagic(this, owner), explosive ? 6f : 5f);
		if (t instanceof LivingEntity le) {
			le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 70, 1));
			le.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 70, 0));
			le.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, explosive ? 80 : 40, 0));
		}
	}

	@Override
	protected void onCollision(HitResult hit) {
		if (hit instanceof EntityHitResult eh && (eh.getEntity() == getOwner() || eh.getEntity() instanceof AbstractGhostEntity)) return;
		super.onCollision(hit);
		if (getWorld() instanceof ServerWorld w && !isRemoved()) {
			w.spawnParticles(ParticleTypes.HEART, getX(), getY(), getZ(), explosive ? 14 : 6, 0.4, 0.4, 0.4, 0.1);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.HOSTILE, 1.0f, 1.6f);
			if (explosive) {
				w.spawnParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
				for (Entity e : w.getOtherEntities(this, getBoundingBox().expand(2.5), e -> e instanceof LivingEntity && !(e instanceof AbstractGhostEntity))) {
					e.damage(getDamageSources().indirectMagic(this, getOwner()), 4f);
				}
			}
			discard();
		}
	}

	public boolean explosive() {
		return explosive;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Explosive", explosive);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		explosive = nbt.getBoolean("Explosive");
	}
}
