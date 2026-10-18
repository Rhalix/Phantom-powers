package com.phantompowers.entity;

import com.phantompowers.HunterSuit;
import com.phantompowers.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** A hunter's wrist missile: a small homing rocket that bursts in a ghost-busting blast (no block damage). */
public class MissileEntity extends ThrownItemEntity {
	private LivingEntity target;
	private float power = 7f;

	public MissileEntity(EntityType<? extends MissileEntity> type, World world) {
		super(type, world);
	}

	public MissileEntity(World world, LivingEntity owner) {
		super(ModEntities.MISSILE, owner, world);
	}

	public void setTarget(LivingEntity t) {
		this.target = t;
	}

	public void setPower(float p) {
		this.power = p;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.FIREWORK_ROCKET;
	}

	@Override
	protected double getGravity() {
		return 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			Vec3d v = getVelocity();
			getWorld().addParticle(ParticleTypes.FLAME, getX() - v.x * 0.5, getY() - v.y * 0.5, getZ() - v.z * 0.5, 0, 0, 0);
			getWorld().addParticle(ParticleTypes.SMOKE, getX() - v.x, getY() - v.y, getZ() - v.z, 0, 0.01, 0);
			return;
		}
		if (age > 80) burst((ServerWorld) getWorld());
		if (target != null && target.isAlive() && age > 3) {
			Vec3d want = target.getPos().add(0, target.getHeight() * 0.6, 0).subtract(getPos()).normalize().multiply(Math.max(1.1, getVelocity().length()));
			setVelocity(getVelocity().lerp(want, 0.18));
			velocityModified = true;
		}
	}

	@Override
	protected void onCollision(HitResult hit) {
		if (hit instanceof EntityHitResult eh && eh.getEntity() == getOwner()) return;
		super.onCollision(hit);
		if (getWorld() instanceof ServerWorld w && !isRemoved()) burst(w);
	}

	private void burst(ServerWorld w) {
		Vec3d c = getPos();
		Entity owner = getOwner();
		for (Entity e : w.getOtherEntities(this, new Box(c, c).expand(2.6), e -> e instanceof LivingEntity && e.isAlive() && e != owner)) {
			e.damage(getDamageSources().explosion(this, owner), power * HunterSuit.ghostBonus(e));
			Vec3d push = e.getPos().subtract(c).normalize();
			e.addVelocity(push.x * 0.8, 0.35, push.z * 0.8);
			e.velocityModified = true;
		}
		w.spawnParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 2, 0.3, 0.3, 0.3, 0);
		w.spawnParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 20, 0.4, 0.4, 0.4, 0.08);
		w.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.9f, 1.4f);
		discard();
	}
}
