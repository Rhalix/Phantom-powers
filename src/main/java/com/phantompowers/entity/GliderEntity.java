package com.phantompowers.entity;

import com.phantompowers.ModItems;
import com.phantompowers.network.BeamPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * The Hover Glider: stand on it and it flies where you look. W to fly, S to brake,
 * A/D to strafe, right-click to fire the twin lasers, sneak to hop off.
 */
public class GliderEntity extends Entity {
	private static final double MAX_SPEED = 1.1;
	private int fireCooldown;

	public GliderEntity(EntityType<? extends GliderEntity> type, World world) {
		super(type, world);
		this.setNoGravity(true);
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
	public boolean isCollidable() {
		return false;
	}

	public boolean shouldRiderSit() {
		return false;
	}

	@Override
	public LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof PlayerEntity p ? p : null;
	}

	@Override
	protected Vec3d getPassengerAttachmentPos(Entity passenger, net.minecraft.entity.EntityDimensions dimensions, float scaleFactor) {
		return new Vec3d(0, dimensions.height(), 0);
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		if (player.isSneaking()) {
			// Pick it back up
			if (!getWorld().isClient && !hasPassengers()) {
				ItemStack stack = new ItemStack(ModItems.HOVER_GLIDER);
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
		if (source.getAttacker() instanceof PlayerEntity player) {
			if (!player.getAbilities().creativeMode) dropStack(new ItemStack(ModItems.HOVER_GLIDER));
			discard();
			return true;
		}
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (fireCooldown > 0) fireCooldown--;
		LivingEntity rider = getControllingPassenger();
		if (isLogicalSideForUpdatingMovement()) {
			Vec3d v = getVelocity();
			if (rider instanceof PlayerEntity p) {
				setYaw(p.getYaw());
				setPitch(p.getPitch() * 0.4f);
				Vec3d look = p.getRotationVec(1f);
				Vec3d flat = new Vec3d(look.x, 0, look.z).normalize();
				Vec3d side = new Vec3d(-flat.z, 0, flat.x);
				double fwd = p.forwardSpeed;
				double strafe = p.sidewaysSpeed;
				Vec3d push = look.multiply(fwd * 0.09).add(side.multiply(-strafe * 0.05));
				v = v.add(push);
				if (fwd < 0) v = v.multiply(0.85);
				// Gentle hover bob when idle
				if (Math.abs(fwd) < 0.01 && Math.abs(strafe) < 0.01) v = v.add(0, Math.sin(age * 0.15) * 0.004, 0);
			} else {
				v = v.add(0, -0.02, 0); // drifts down slowly when nobody is riding
			}
			v = v.multiply(0.94);
			if (v.length() > MAX_SPEED) v = v.normalize().multiply(MAX_SPEED);
			setVelocity(v);
			move(MovementType.SELF, v);
			if (rider == null && isOnGround()) setVelocity(Vec3d.ZERO);
		} else {
			setVelocity(Vec3d.ZERO);
		}

		// Thruster exhaust
		if (getWorld().isClient && (hasPassengers() || age % 4 == 0)) {
			Vec3d back = getRotationVector(0, getYaw()).multiply(-0.95);
			Vec3d right = new Vec3d(-back.z, 0, back.x).normalize().multiply(0.38);
			for (int s = -1; s <= 1; s += 2) {
				Vec3d at = getPos().add(back).add(right.multiply(s)).add(0, 0.12, 0);
				getWorld().addParticle(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, back.x * 0.05, -0.02, back.z * 0.05);
				if (getVelocity().lengthSquared() > 0.05) getWorld().addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, back.x * 0.08, 0, back.z * 0.08);
			}
		}
	}

	/** Twin laser cannons on the nose. */
	public void fire(ServerPlayerEntity p) {
		if (fireCooldown > 0) return;
		fireCooldown = 6;
		ServerWorld w = p.getServerWorld();
		Vec3d look = p.getRotationVec(1f);
		Vec3d fwd = getRotationVector(0, getYaw());
		Vec3d right = new Vec3d(-fwd.z, 0, fwd.x).normalize();
		for (int s = -1; s <= 1; s += 2) {
			Vec3d from = getPos().add(fwd.multiply(1.0)).add(right.multiply(0.35 * s)).add(0, 0.2, 0);
			Vec3d end = from.add(look.multiply(48));
			BlockHitResult bh = w.raycast(new RaycastContext(from, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
			if (bh.getType() != HitResult.Type.MISS) end = bh.getPos();
			Box box = new Box(from, end).expand(1.0);
			EntityHitResult eh = ProjectileUtil.raycast(p, from, end, box,
					e -> e != p && e != this && !e.isSpectator() && e.canHit(), from.squaredDistanceTo(end));
			if (eh != null) {
				end = eh.getPos();
				eh.getEntity().damage(w.getDamageSources().indirectMagic(this, p), 6.0f);
				if (eh.getEntity() instanceof LivingEntity le) le.takeKnockback(0.3, -look.x, -look.z);
			}
			BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) end.x, (float) end.y, (float) end.z, 0xFF3344);
			for (ServerPlayerEntity viewer : PlayerLookup.around(w, from, 96)) {
				if (ServerPlayNetworking.canSend(viewer, BeamPayload.ID)) ServerPlayNetworking.send(viewer, beam);
			}
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 2.0f);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.4f, 2.0f);
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	public float bob(float tickDelta) {
		return MathHelper.sin((age + tickDelta) * 0.15f) * 0.04f;
	}
}
