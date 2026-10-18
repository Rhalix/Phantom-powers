package com.phantompowers.entity;

import com.phantompowers.GhostPowers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Ecto Constructs: a giant weapon of solid ghost energy conjured above the target. It rises, hangs there
 * crackling, then smashes down: a sword that cleaves, a hammer that quakes the ground, or a fist that pounds.
 */
public class EctoConstructEntity extends Entity {
	public static final int KINDS = 3, LIFE = 46, SLAM = 26;
	private static final TrackedData<Integer> KIND = DataTracker.registerData(EctoConstructEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private UUID owner;
	private LivingEntity target;
	private Vec3d spot;

	public EctoConstructEntity(EntityType<?> type, World world) {
		super(type, world);
		this.noClip = true;
	}

	public void setup(ServerPlayerEntity p, int kind, Vec3d at, LivingEntity target) {
		this.owner = p.getUuid();
		this.target = target;
		this.spot = at;
		dataTracker.set(KIND, kind);
		refreshPositionAndAngles(at.x, at.y, at.z, p.getYaw(), 0);
	}

	public int kind() {
		return dataTracker.get(KIND);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(KIND, 0);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			if (age < SLAM && age % 2 == 0) getWorld().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 2, getY() + 4 + random.nextDouble() * 3,
					getZ() + (random.nextDouble() - 0.5) * 2, 0, -0.05, 0);
			return;
		}
		ServerWorld w = (ServerWorld) getWorld();
		// track the target until the strike begins
		if (target != null && target.isAlive() && age < SLAM - 6) {
			spot = BossMoves.ground(w, target.getPos());
			refreshPositionAndAngles(spot.x, spot.y, spot.z, getYaw(), 0);
		}
		if (age == SLAM - 6) BossMoves.warn(w, getPos(), kind() == 1 ? 5.0 : 3.5, 6, 0x55FF88);
		if (age == SLAM) strike(w);
		if (age > LIFE) discard();
	}

	private void strike(ServerWorld w) {
		ServerPlayerEntity p = owner == null ? null : w.getServer().getPlayerManager().getPlayer(owner);
		DamageSource src = p != null ? w.getDamageSources().indirectMagic(this, p) : w.getDamageSources().magic();
		Vec3d c = getPos();
		double r = kind() == 1 ? 5.0 : kind() == 0 ? 3.5 : 3.0;
		float dmg = kind() == 2 ? 15f : kind() == 0 ? 13f : 11f;
		for (Entity e : w.getOtherEntities(this, new Box(c, c).expand(r, 3, r), e -> e instanceof LivingEntity && e.isAlive()
				&& (p == null || e != p) && !(e instanceof GhostCloneEntity))) {
			if (e.squaredDistanceTo(c) > r * r + 4) continue;
			e.damage(src, dmg);
			Vec3d push = e.getPos().subtract(c).normalize();
			e.addVelocity(push.x * 0.9, kind() == 1 ? 0.9 : 0.4, push.z * 0.9);
			e.velocityModified = true;
		}
		if (kind() == 1) BossMoves.groundWave(w, this, c, 7, 6f, 0x55FF88);
		w.spawnParticles(GhostPowers.GREEN, c.x, c.y + 0.5, c.z, 60, r * 0.5, 0.4, r * 0.5, 0.15);
		w.spawnParticles(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
		w.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 1.4f, kind() == 1 ? 0.6f : 1.1f);
		w.playSound(null, c.x, c.y, c.z, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.6f, 0.6f);
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}

	@Override
	public boolean shouldSave() {
		return false;
	}
}
