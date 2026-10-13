package com.phantompowers.entity;

import com.phantompowers.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Rev's ghost bike (from his Bike Keys): a black sport bike you can ride. It steers where you look,
 * W to throttle, S to brake, sprint for nitro, jump to hop. Ride into mobs at speed to bowl them over.
 */
public class GhostBikeEntity extends Entity {
	private static final double TOP = 0.85, NITRO = 1.35;
	/** Set on the client from the rider's keys. */
	public static volatile boolean clientNitro, clientHop;
	private double speed;
	private float wheelRoll, prevWheelRoll;
	private final Map<UUID, Integer> bumped = new HashMap<>();

	public GhostBikeEntity(EntityType<? extends GhostBikeEntity> type, World world) {
		super(type, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}

	@Override
	public boolean canHit() {
		return !isRemoved();
	}

	@Override
	public float getStepHeight() {
		return 1.1f;
	}

	@Override
	public LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof PlayerEntity p ? p : null;
	}

	@Override
	protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
		return new Vec3d(0, 0.92, -0.32);
	}

	public float wheelRoll(float delta) {
		return MathHelper.lerp(delta, prevWheelRoll, wheelRoll);
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		if (player.isSneaking()) {
			if (!getWorld().isClient && !hasPassengers()) {
				ItemStack stack = new ItemStack(ModItems.BIKE_KEYS);
				if (!player.giveItemStack(stack)) dropStack(stack);
				discard();
			}
			return ActionResult.success(getWorld().isClient);
		}
		if (hasPassengers()) return ActionResult.PASS;
		if (!getWorld().isClient) player.startRiding(this);
		return ActionResult.success(getWorld().isClient);
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (isInvulnerableTo(source) || getWorld().isClient || isRemoved()) return false;
		if (source.getAttacker() instanceof PlayerEntity player && !hasPassengers()) {
			if (!player.getAbilities().creativeMode) dropStack(new ItemStack(ModItems.BIKE_KEYS));
			discard();
			return true;
		}
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		prevWheelRoll = wheelRoll;
		LivingEntity rider = getControllingPassenger();
		if (isLogicalSideForUpdatingMovement()) {
			Vec3d v = getVelocity();
			if (rider instanceof PlayerEntity p) {
				// steer toward where the rider is looking
				float yaw = MathHelper.stepUnwrappedAngleTowards(getYaw(), p.getYaw(), 7f);
				setYaw(yaw);
				double fwd = p.forwardSpeed;
				boolean nitro = getWorld().isClient && p.isMainPlayer() && clientNitro && fwd > 0;
				double top = nitro ? NITRO : TOP;
				if (fwd > 0) speed = Math.min(top, speed + (nitro ? 0.06 : 0.035));
				else if (fwd < 0) speed = Math.max(-0.25, speed - 0.06);
				else speed *= 0.96;
				if (speed > top) speed *= 0.97;
				if (isOnGround() && getWorld().isClient && p.isMainPlayer() && clientHop) v = new Vec3d(v.x, 0.55, v.z);
				if (nitro && getWorld().isClient) {
					Vec3d back = Vec3d.fromPolar(0, getYaw()).multiply(-1.2);
					getWorld().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX() + back.x, getY() + 0.4, getZ() + back.z, back.x * 0.1, 0.01, back.z * 0.1);
				}
			} else {
				speed *= 0.9;
			}
			Vec3d dir = Vec3d.fromPolar(0, getYaw());
			double vy = isOnGround() && v.y < 0 ? 0 : v.y - 0.08;
			v = new Vec3d(dir.x * speed, vy * 0.98, dir.z * speed);
			setVelocity(v);
			move(MovementType.SELF, v);
			if (horizontalCollision) speed *= 0.5;
		} else {
			setVelocity(Vec3d.ZERO);
		}
		double moved = Math.sqrt(MathHelper.square(getX() - prevX) + MathHelper.square(getZ() - prevZ));
		wheelRoll += (float) moved * 2.6f;
		if (getWorld().isClient) {
			Vec3d back = Vec3d.fromPolar(0, getYaw()).multiply(-1.15);
			if (age % 3 == 0) getWorld().addParticle(ParticleTypes.SMOKE, getX() + back.x, getY() + 0.35, getZ() + back.z, 0, 0.02, 0);
		} else if (getWorld() instanceof ServerWorld w && rider != null && moved > 0.55) {
			// bowl over anything in the way
			bumped.replaceAll((k, t) -> t - 1);
			bumped.values().removeIf(t -> t <= 0);
			for (Entity e : w.getOtherEntities(this, getBoundingBox().expand(0.4), e -> e instanceof LivingEntity && e != rider && e.isAlive())) {
				if (bumped.containsKey(e.getUuid())) continue;
				bumped.put(e.getUuid(), 15);
				Vec3d dir = Vec3d.fromPolar(0, getYaw());
				e.damage(w.getDamageSources().mobAttack(rider), (float) (6 * moved / TOP) * com.phantompowers.HunterSuit.ghostBonus(e));
				e.addVelocity(dir.x * 1.2, 0.5, dir.z * 1.2);
				e.velocityModified = true;
				w.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.2f, 0.7f);
			}
		}
	}
}
