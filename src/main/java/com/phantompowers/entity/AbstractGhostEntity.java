package com.phantompowers.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.Optional;

/**
 * A ghost. Floating ghosts fly; walking ghosts (see {@link #makeWalker()}) have legs and use normal pathfinding.
 * Bosses always drift through walls, but ordinary ghosts only phase through when they're stuck chasing you.
 * Can be tied to a "home" (a prison cell or a room).
 */
public abstract class AbstractGhostEntity extends PathAwareEntity {
	private BlockPos home;
	private float homeRadius;
	private boolean walker;
	private int phaseTicks, stuckTicks;

	protected AbstractGhostEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
		this.moveControl = new GhostMoveControl(this);
		this.experiencePoints = 5;
	}

	/** Walking ghosts: legs, gravity and pathfinding instead of flight. Call from the constructor. */
	protected void makeWalker() {
		this.walker = true;
		this.noClip = false;
		this.setNoGravity(false);
		this.moveControl = new MoveControl(this);
	}

	public boolean isWalker() {
		return walker;
	}

	/** Bosses and special ghosts drift through anything; ordinary ghosts mostly don't. */
	protected boolean alwaysPhases() {
		return true;
	}

	/** Phase through walls for a moment (a ghostly dash). */
	public void startPhase(int ticks) {
		this.phaseTicks = Math.max(phaseTicks, ticks);
	}

	/** Is this ghost passing through walls right now? */
	public boolean isPhasing() {
		return noClip;
	}

	@Override
	public void tick() {
		if (walker) {
			// a walking ghost can still slip through a wall now and then to reach you
			this.noClip = phaseTicks > 0;
			this.setNoGravity(phaseTicks > 0);
			super.tick();
			if (phaseTicks > 0 && --phaseTicks == 0) this.noClip = false;
			if (!getWorld().isClient && getTarget() != null && horizontalCollision && getNavigation().isIdle()) {
				if (++stuckTicks > 40 && random.nextInt(3) == 0) {
					phaseTicks = 25;
					stuckTicks = 0;
				}
			} else stuckTicks = 0;
			return;
		}
		boolean phase = alwaysPhases() || phaseTicks > 0;
		this.noClip = phase;
		super.tick();
		this.noClip = phase;
		this.setNoGravity(true);
		if (phaseTicks > 0) phaseTicks--;
		// stuck against a wall while chasing something: phase through for a moment
		if (!phase && !getWorld().isClient && getTarget() != null && horizontalCollision) {
			if (++stuckTicks > 30) {
				phaseTicks = 30;
				stuckTicks = 0;
			}
		} else if (!horizontalCollision) stuckTicks = 0;
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return walker && super.handleFallDamage(fallDistance, damageMultiplier, damageSource);
	}

	@Override
	public boolean isPushable() {
		return walker;
	}

	// ------------------------------------------------------------- home / cage

	public void setHome(BlockPos pos, float radius) {
		this.home = pos;
		this.homeRadius = radius;
	}

	public void clearHome() {
		this.home = null;
	}

	public boolean hasHome() {
		return home != null;
	}

	/** Keeps a target point inside the ghost's home area, if it has one. */
	public Vec3d clampToHome(Vec3d target) {
		if (home == null) return target;
		Vec3d c = Vec3d.ofCenter(home);
		Vec3d d = target.subtract(c);
		double flat = Math.sqrt(d.x * d.x + d.z * d.z);
		double x = d.x, z = d.z;
		if (flat > homeRadius) {
			x = d.x / flat * homeRadius;
			z = d.z / flat * homeRadius;
		}
		double y = MathHelper.clamp(d.y, -1.0, 1.5);
		return c.add(x, y, z);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (home != null) {
			nbt.put("GhostHome", NbtHelper.fromBlockPos(home));
			nbt.putFloat("GhostHomeRadius", homeRadius);
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		Optional<BlockPos> h = NbtHelper.toBlockPos(nbt, "GhostHome");
		if (h.isPresent()) {
			home = h.get();
			homeRadius = nbt.getFloat("GhostHomeRadius");
		}
	}

	/** Fly toward a point (kept inside the home area). */
	public void flyTo(Vec3d target, double speed) {
		Vec3d t = clampToHome(target);
		getMoveControl().moveTo(t.x, t.y, t.z, speed);
	}

	// ------------------------------------------------------------------ flight

	/** Straight-line flight through anything, like a Vex. */
	static class GhostMoveControl extends MoveControl {
		GhostMoveControl(MobEntity entity) {
			super(entity);
		}

		@Override
		public void tick() {
			if (this.state != MoveControl.State.MOVE_TO) return;
			Vec3d d = new Vec3d(targetX - entity.getX(), targetY - entity.getY(), targetZ - entity.getZ());
			double len = d.length();
			if (len < entity.getBoundingBox().getAverageSideLength() * 0.5) {
				this.state = MoveControl.State.WAIT;
				entity.setVelocity(entity.getVelocity().multiply(0.5));
				return;
			}
			entity.setVelocity(entity.getVelocity().add(d.multiply(speed * 0.04 / len)));
			Vec3d v = entity.getVelocity();
			if (v.lengthSquared() > 0.6 * 0.6 * speed * speed + 0.01) entity.setVelocity(v.normalize().multiply(0.6 * Math.max(0.3, speed)));
			if (entity.getTarget() == null) {
				entity.setYaw(-((float) MathHelper.atan2(v.x, v.z)) * MathHelper.DEGREES_PER_RADIAN);
			} else {
				double dx = entity.getTarget().getX() - entity.getX();
				double dz = entity.getTarget().getZ() - entity.getZ();
				entity.setYaw(-((float) MathHelper.atan2(dx, dz)) * MathHelper.DEGREES_PER_RADIAN);
			}
			entity.bodyYaw = entity.getYaw();
		}
	}

	/** Drift to a random nearby spot now and then. */
	static class DriftGoal extends Goal {
		private final AbstractGhostEntity ghost;

		DriftGoal(AbstractGhostEntity ghost) {
			this.ghost = ghost;
			this.setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			return !ghost.getMoveControl().isMoving() && ghost.getRandom().nextInt(6) == 0;
		}

		@Override
		public boolean shouldContinue() {
			return false;
		}

		@Override
		public void start() {
			var r = ghost.getRandom();
			Vec3d p = ghost.getPos().add(r.nextInt(15) - 7, r.nextInt(7) - 3, r.nextInt(15) - 7);
			ghost.flyTo(p, 0.35);
		}
	}
}
