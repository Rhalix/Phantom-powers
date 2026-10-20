package com.phantompowers.entity;

import com.phantompowers.HunterSuit;
import com.phantompowers.ModEntities;
import com.phantompowers.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

/** A ball of pink energy from the Ecto Launcher. Bursts on impact; built to hit ghosts especially hard. No block damage. */
public class PinkOrbEntity extends ThrownItemEntity {
	/** Damage multiplier from the launcher's upgrade level. */
	public float boost = 1f;

	public static final DustParticleEffect PINK = new DustParticleEffect(new Vector3f(1f, 0.35f, 0.75f), 1.6f);

	public PinkOrbEntity(EntityType<? extends PinkOrbEntity> type, World world) {
		super(type, world);
	}

	public PinkOrbEntity(World world, LivingEntity owner) {
		super(ModEntities.PINK_ORB, owner, world);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.ECTO_LAUNCHER;
	}

	@Override
	protected double getGravity() {
		return 0.006;
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && age > 80) burst((ServerWorld) getWorld());
		if (getWorld().isClient) {
			getWorld().addParticle(PINK, getX(), getY() + 0.1, getZ(), 0, 0, 0);
			if (age % 2 == 0) getWorld().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.1, getZ(), 0, 0, 0);
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
		for (Entity e : w.getOtherEntities(this, new Box(c, c).expand(3.5), e -> e instanceof LivingEntity && e.isAlive() && e != owner)) {
			double d = e.getPos().add(0, e.getHeight() / 2, 0).distanceTo(c);
			if (d > 3.5) continue;
			float dmg = (float) (10 * (1 - d / 5.0)) * HunterSuit.ghostBonus(e) * boost;
			e.damage(getDamageSources().indirectMagic(this, owner), dmg);
			Vec3d push = e.getPos().subtract(c).normalize();
			e.addVelocity(push.x * 1.1, 0.45, push.z * 1.1);
			e.velocityModified = true;
		}
		w.spawnParticles(PINK, c.x, c.y, c.z, 60, 1.0, 1.0, 1.0, 0.2);
		w.spawnParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 30, 0.2, 0.2, 0.2, 0.25);
		w.spawnParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
		w.spawnParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 2, 0.4, 0.4, 0.4, 0);
		w.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 1.2f, 1.5f);
		w.playSound(null, c.x, c.y, c.z, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.5f, 0.6f);
		discard();
	}
}
