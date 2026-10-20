package com.phantompowers.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** An invisible seat for chairs, stools and sofas. Vanishes as soon as nobody is sitting on it. */
public class SeatEntity extends Entity {
	public SeatEntity(EntityType<? extends SeatEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
		setInvisible(true);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) return;
		BlockPos below = BlockPos.ofFloored(getX(), getY() + 0.2, getZ());
		if (!hasPassengers() || !(getWorld().getBlockState(below).getBlock() instanceof com.phantompowers.FurnitureBlock)) discard();
	}

	@Override
	protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
		return Vec3d.ZERO;
	}

	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		Vec3d front = Vec3d.fromPolar(0, getYaw()).multiply(0.8);
		Vec3d at = getPos().add(front.x, 0.3, front.z);
		BlockPos bp = BlockPos.ofFloored(at);
		if (!getWorld().getBlockState(bp).getCollisionShape(getWorld(), bp).isEmpty()) at = getPos().add(0, 0.9, 0);
		return at;
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
