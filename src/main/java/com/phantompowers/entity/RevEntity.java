package com.phantompowers.entity;

import com.phantompowers.ModEntities;
import com.phantompowers.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Rev: a ghost biker on a black sport bike. He circles you, revs up and rams straight through you,
 * leaves rings of ghost-fire with his burnouts, and once he's hurt badly enough his living shadow
 * tears loose and fights at his side. Roxy sometimes rides on the back (the Biker Couple fight).
 */
public class RevEntity extends AbstractGhostEntity implements Monster {
	private static final TrackedData<Boolean> SHADOW_PHASE = DataTracker.registerData(RevEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Integer> CHARGE = DataTracker.registerData(RevEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Rev").formatted(Formatting.GRAY),
			BossBar.Color.WHITE, BossBar.Style.NOTCHED_10);
	private int ramCooldown = 70, burnCooldown = 160, shadeCooldown;
	private int ramTicks;
	private Vec3d ramDir = Vec3d.ZERO;
	private double orbit;
	private UUID shade;

	public RevEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 150;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 250.0)
				.add(EntityAttributes.GENERIC_ARMOR, 8.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(SHADOW_PHASE, false);
		builder.add(CHARGE, 0);
	}

	public boolean shadowPhase() {
		return dataTracker.get(SHADOW_PHASE);
	}

	/** 0..1: revving up (rising), then 1 while ramming (client animation). */
	public float charge() {
		return Math.min(1f, dataTracker.get(CHARGE) / 10f);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 24.0f));
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
	}

	// Roxy rides on the back seat, and she doesn't get to steer
	@Override
	public LivingEntity getControllingPassenger() {
		return null;
	}

	@Override
	protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
		return new Vec3d(0, 0.32, -0.62);
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return passenger instanceof RoxyEntity && getPassengerList().isEmpty();
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			// exhaust smoke, ghost-fire when ramming
			Vec3d back = Vec3d.fromPolar(0, getBodyYaw()).multiply(-1.1);
			getWorld().addParticle(charge() > 0.5f ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE,
					getX() + back.x + 0.15, getY() + 0.55, getZ() + back.z, back.x * 0.05, 0.02, back.z * 0.05);
		}
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		bossBar.setPercent(getHealth() / getMaxHealth());
		if (!(getWorld() instanceof ServerWorld w)) return;
		if (!shadowPhase() && getHealth() <= getMaxHealth() * 0.5f) startShadowPhase(w);
		if (shadowPhase() && --shadeCooldown <= 0 && shadeGone(w)) {
			shadeCooldown = 600;
			summonShade(w);
		}
		LivingEntity target = getTarget();
		Vec3d ground = BossMoves.ground(w, getPos());
		if (ramTicks > 0) {
			tickRam(w, ground);
			return;
		}
		int ch = dataTracker.get(CHARGE);
		if (ch > 0) {
			// revving: wheel spinning, smoke pouring, then GO
			dataTracker.set(CHARGE, ch + 1);
			setVelocity(Vec3d.ZERO);
			w.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 2, 0.4, 0.1, 0.4, 0.02);
			if (ch >= 18) {
				ramTicks = 22;
				w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 2f, 0.5f);
			}
			return;
		}
		if (target == null || !target.isAlive()) {
			setVelocity(getVelocity().multiply(0.8));
			return;
		}
		float speed = shadowPhase() ? 1.3f : 1f;
		// Circle the target at a distance, hugging the ground
		orbit += 0.035 * speed;
		Vec3d around = new Vec3d(Math.cos(orbit), 0, Math.sin(orbit)).multiply(9);
		Vec3d goal = target.getPos().add(around);
		Vec3d to = goal.subtract(getPos());
		Vec3d flat = new Vec3d(to.x, 0, to.z);
		Vec3d v = flat.lengthSquared() > 1 ? flat.normalize().multiply(0.42 * speed) : flat.multiply(0.3);
		double dy = (ground.y - getY()) * 0.3;
		setVelocity(v.x, dy, v.z);
		faceAlong(v);
		if (--ramCooldown <= 0 && squaredDistanceTo(target) < 30 * 30) {
			ramCooldown = (int) (70 / speed);
			startRam(w, target);
		}
		if (--burnCooldown <= 0 && squaredDistanceTo(target) < 16 * 16) {
			burnCooldown = (int) (200 / speed);
			burnout(w);
		}
	}

	private void faceAlong(Vec3d v) {
		if (v.horizontalLengthSquared() < 1.0E-4) return;
		float yaw = (float) (MathHelper.atan2(v.z, v.x) * (180 / Math.PI)) - 90f;
		setYaw(yaw);
		setBodyYaw(yaw);
		setHeadYaw(yaw);
	}

	/** Rev the engine, then floor it straight at the target. */
	public void startRam(ServerWorld w, LivingEntity target) {
		ramDir = new Vec3d(target.getX() - getX(), 0, target.getZ() - getZ());
		ramDir = ramDir.lengthSquared() < 1.0E-4 ? Vec3d.fromPolar(0, getYaw()) : ramDir.normalize();
		faceAlong(ramDir);
		dataTracker.set(CHARGE, 1);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.8f, 1.6f);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 1.5f, 0.5f);
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 48 * 48)) {
			p.sendMessage(Text.literal("Rev: \"Outta my way!\"").formatted(Formatting.GRAY, Formatting.BOLD), true);
		}
	}

	private void tickRam(ServerWorld w, Vec3d ground) {
		ramTicks--;
		double sp = shadowPhase() ? 1.15 : 0.95;
		setVelocity(ramDir.x * sp, (ground.y - getY()) * 0.4, ramDir.z * sp);
		faceAlong(ramDir);
		w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.2, getZ(), 4, 0.3, 0.05, 0.3, 0.01);
		for (Entity e : w.getOtherEntities(this, getBoundingBox().expand(0.6), e -> e instanceof LivingEntity && e.isAlive()
				&& !(e instanceof AbstractGhostEntity) && e != getFirstPassenger())) {
			e.damage(getDamageSources().mobAttack(this), shadowPhase() ? 13f : 10f);
			e.addVelocity(ramDir.x * 1.6, 0.6, ramDir.z * 1.6);
			e.velocityModified = true;
			w.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.HOSTILE, 1.5f, 0.6f);
		}
		if (ramTicks == 0) dataTracker.set(CHARGE, 0);
	}

	/** Spins the back wheel in a tight circle, leaving a ring of ghost-fire that burns anyone inside. */
	public void burnout(ServerWorld w) {
		Vec3d c = BossMoves.ground(w, getPos());
		BossMoves.warn(w, c, 6.0, 16, 0x66FFAA);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.5f, 1.9f);
		for (int i = 0; i < 4; i++) {
			BossMoves.later(16 + i * 8, () -> {
				if (!isAlive()) return;
				for (int k = 0; k < 24; k++) {
					double a = k * Math.PI * 2 / 24;
					w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + Math.cos(a) * 5.5, c.y + 0.2, c.z + Math.sin(a) * 5.5, 2, 0.1, 0.3, 0.1, 0.02);
				}
				w.spawnParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.3, c.z, 12, 3, 0.2, 3, 0.02);
				for (Entity e : w.getOtherEntities(this, new net.minecraft.util.math.Box(c, c).expand(6, 2, 6),
						e -> e instanceof LivingEntity && !(e instanceof AbstractGhostEntity))) {
					if (e.squaredDistanceTo(c) < 36) e.damage(getDamageSources().inFire(), 4f);
				}
			});
		}
	}

	// ---------------------------------------------------------------- shadow

	private void startShadowPhase(ServerWorld w) {
		dataTracker.set(SHADOW_PHASE, true);
		bossBar.setName(Text.literal("Rev & his Shade").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
		bossBar.setColor(BossBar.Color.PURPLE);
		heal(getMaxHealth() * 0.08f);
		summonShade(w);
		shadeCooldown = 600;
		// in the couple fight, Roxy hops off and fights on her own
		Entity rider = getFirstPassenger();
		if (rider instanceof RoxyEntity roxy) {
			roxy.stopRiding();
			roxy.addVelocity(0, 0.6, 0);
			roxy.velocityModified = true;
		}
		org.slf4j.LoggerFactory.getLogger("phantom").info("Rev let his Shade loose (stage two)");
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 80 * 80)) {
			p.sendMessage(Text.literal("Rev: \"Shade! Sic 'em!\"").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
		}
	}

	private boolean shadeGone(ServerWorld w) {
		if (shade == null) return true;
		Entity e = w.getEntity(shade);
		return e == null || !e.isAlive();
	}

	public void summonShade(ServerWorld w) {
		ShadeEntity s = ModEntities.SHADE.create(w);
		if (s == null) return;
		Vec3d at = getPos().add(0, 0.3, 0);
		s.refreshPositionAndAngles(at.x, at.y, at.z, getYaw(), 0);
		s.setOwner(this);
		if (getTarget() != null) s.setTarget(getTarget());
		w.spawnEntity(s);
		shade = s.getUuid();
		w.spawnParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1, at.z, 60, 0.6, 1.2, 0.6, 0.05);
		w.spawnParticles(ParticleTypes.SQUID_INK, at.x, at.y + 1, at.z, 40, 0.5, 1.0, 0.5, 0.1);
		w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 1.4f, 1.3f);
	}

	/** Used by the CI test: try every move once. */
	public void testMoves(ServerWorld w, LivingEntity target) {
		startRam(w, target);
		burnout(w);
		startShadowPhase(w);
	}

	public boolean hasShade(ServerWorld w) {
		return !shadeGone(w);
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (getWorld() instanceof ServerWorld w && shade != null) {
			Entity e = w.getEntity(shade);
			if (e != null) e.discard();
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		// the bike takes the brunt of hits from the front
		return super.damage(source, amount);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("ShadowPhase", shadowPhase());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("ShadowPhase")) dataTracker.set(SHADOW_PHASE, true);
	}

	@Override
	public void onStartedTrackingBy(ServerPlayerEntity player) {
		super.onStartedTrackingBy(player);
		bossBar.addPlayer(player);
	}

	@Override
	public void onStoppedTrackingBy(ServerPlayerEntity player) {
		super.onStoppedTrackingBy(player);
		bossBar.removePlayer(player);
	}

	@Override
	public boolean cannotDespawn() {
		return true;
	}

	@Override
	protected boolean isDisallowedInPeaceful() {
		return true;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_RAVAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.GHOST_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.GHOST_DEATH;
	}
}
