package com.phantompowers.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Rev's Shade: his own shadow, torn loose and alive. A smoky, long-armed thing with ember eyes that lunges and
 * claws, snatches you and drags you toward Rev, and blinds you in a cloud of darkness. It fades when Rev falls.
 */
public class ShadeEntity extends AbstractGhostEntity implements Monster {
	private static final TrackedData<Integer> SWIPE = DataTracker.registerData(ShadeEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Boolean> BOSS = DataTracker.registerData(ShadeEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> CLONE = DataTracker.registerData(ShadeEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private final net.minecraft.entity.boss.ServerBossBar bossBar = new net.minecraft.entity.boss.ServerBossBar(
			net.minecraft.text.Text.literal("The Shade").formatted(net.minecraft.util.Formatting.DARK_PURPLE),
			net.minecraft.entity.boss.BossBar.Color.PURPLE, net.minecraft.entity.boss.BossBar.Style.NOTCHED_10);
	private boolean decided, eclipse;
	private int blinkCooldown = 120, eclipseCooldown = 200;
	private UUID owner;
	private int grabCooldown = 100, cloudCooldown = 160, swipe;

	public ShadeEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 30;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 90.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.8)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(SWIPE, 0);
		builder.add(BOSS, false);
		builder.add(CLONE, false);
	}

	/** Fought on its own (not as Rev's helper): a full boss. */
	public boolean isBoss() {
		return dataTracker.get(BOSS);
	}

	/** One of the boss's shadow clones (fades when the real one falls). */
	public boolean isClone() {
		return dataTracker.get(CLONE);
	}

	private void makeClone(Entity master) {
		this.owner = master.getUuid();
		dataTracker.set(CLONE, true);
		decided = true;
	}

	/** 0..1 claw swipe (client animation). */
	public float swipe() {
		return Math.min(1f, dataTracker.get(SWIPE) / 8f);
	}

	public void setOwner(RevEntity rev) {
		this.owner = rev.getUuid();
		decided = true;
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new HostileGhostEntity.ChargeGoal(this));
		this.goalSelector.add(6, new DriftGoal(this));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 24.0f));
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			if (random.nextInt(2) == 0) {
				getWorld().addParticle(ParticleTypes.LARGE_SMOKE, getX() + (random.nextDouble() - 0.5) * 0.8, getY() + random.nextDouble() * getHeight(),
						getZ() + (random.nextDouble() - 0.5) * 0.8, 0, 0.02, 0);
			}
			if (random.nextInt(5) == 0) {
				getWorld().addParticle(ParticleTypes.SMALL_FLAME, getX() + (random.nextDouble() - 0.5) * 0.6, getY() + getHeight() * 0.8,
						getZ() + (random.nextDouble() - 0.5) * 0.6, 0, 0.01, 0);
			}
		}
	}

	@Override
	public boolean tryAttack(Entity target) {
		swipe = 8;
		boolean hit = super.tryAttack(target);
		if (hit && getWorld() instanceof ServerWorld w) {
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PHANTOM_BITE, SoundCategory.HOSTILE, 1.2f, 0.6f);
		}
		return hit;
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (swipe > 0) swipe--;
		dataTracker.set(SWIPE, swipe);
		if (!(getWorld() instanceof ServerWorld w)) return;
		if (!decided) {
			decided = true;
			if (owner == null) becomeBoss();
		}
		if (isBoss()) {
			bossBar.setPercent(getHealth() / getMaxHealth());
			tickBoss(w);
		}
		Entity rev = owner == null ? null : w.getEntity(owner);
		if (owner != null && (rev == null || !rev.isAlive())) {
			fade(w);
			return;
		}
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) return;
		if (--grabCooldown <= 0 && squaredDistanceTo(target) < 12 * 12) {
			grabCooldown = 140;
			swipe = 8;
			// Shadow grab: a dark tendril yanks the target toward Rev (or toward the Shade)
			Vec3d toward = (rev != null ? rev.getPos() : getPos()).subtract(target.getPos()).normalize();
			target.addVelocity(toward.x * 1.4, 0.45, toward.z * 1.4);
			target.velocityModified = true;
			Vec3d a = getPos().add(0, getHeight() * 0.6, 0), b = target.getPos().add(0, target.getHeight() * 0.5, 0);
			for (int i = 0; i <= 16; i++) {
				Vec3d p = a.lerp(b, i / 16.0);
				w.spawnParticles(ParticleTypes.SQUID_INK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.01);
			}
			w.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.HOSTILE, 1.5f, 0.6f);
		}
		if (--cloudCooldown <= 0 && squaredDistanceTo(target) < 6 * 6) {
			cloudCooldown = 200;
			w.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 80, 2.5, 1.2, 2.5, 0.02);
			for (Entity e : w.getOtherEntities(this, getBoundingBox().expand(4), e -> e instanceof PlayerEntity)) {
				((PlayerEntity) e).addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 100, 0));
				((PlayerEntity) e).addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
			}
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 0.9f, 1.5f);
		}
	}

	// ------------------------------------------------------------- boss mode

	private void becomeBoss() {
		dataTracker.set(BOSS, true);
		var hp = getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		if (hp != null && hp.getModifier(com.phantompowers.PhantomMod.id("shade_boss")) == null) {
			hp.addPersistentModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(com.phantompowers.PhantomMod.id("shade_boss"), 170,
					net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE));
		}
		setHealth(getMaxHealth());
		experiencePoints = 120;
		setPersistent();
	}

	private void tickBoss(ServerWorld w) {
		if (age % 20 == 0) {
			for (net.minecraft.server.network.ServerPlayerEntity sp : w.getPlayers(sp -> sp.squaredDistanceTo(this) < 64 * 64)) bossBar.addPlayer(sp);
		}
		if (!eclipse && getHealth() <= getMaxHealth() * 0.5f) startEclipse(w);
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) return;
		float speed = eclipse ? 0.65f : 1f;
		if (--blinkCooldown <= 0 && squaredDistanceTo(target) < 24 * 24) {
			blinkCooldown = (int) (120 * speed);
			BossMoves.blinkBehind(w, this, target, 2.0);
			swipe = 8;
		}
		if (eclipse && --eclipseCooldown <= 0) {
			eclipseCooldown = 220;
			eclipsePulse(w);
		}
	}

	/** Stage two: the light goes out, and two shadow clones peel off it. */
	private void startEclipse(ServerWorld w) {
		eclipse = true;
		heal(getMaxHealth() * 0.08f);
		bossBar.setName(net.minecraft.text.Text.literal("The Shade: ECLIPSE").formatted(net.minecraft.util.Formatting.DARK_PURPLE, net.minecraft.util.Formatting.BOLD));
		bossBar.setColor(net.minecraft.entity.boss.BossBar.Color.RED);
		for (int i = 0; i < 2; i++) splitClone(w);
		eclipsePulse(w);
		org.slf4j.LoggerFactory.getLogger("phantom").info("The Shade split into the Eclipse (stage two)");
	}

	public void splitClone(ServerWorld w) {
		ShadeEntity c = com.phantompowers.ModEntities.SHADE.create(w);
		if (c == null) return;
		c.makeClone(this);
		c.refreshPositionAndAngles(getX() + random.nextInt(5) - 2, getY(), getZ() + random.nextInt(5) - 2, getYaw(), 0);
		var hp = c.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		if (hp != null) hp.setBaseValue(35);
		c.setHealth(35);
		if (getTarget() != null) c.setTarget(getTarget());
		w.spawnEntity(c);
		w.spawnParticles(ParticleTypes.SQUID_INK, c.getX(), c.getY() + 1, c.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
	}

	/** Darkness rolls out over everyone nearby and the shadows bite. */
	public void eclipsePulse(ServerWorld w) {
		w.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 120, 6, 1.5, 6, 0.02);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 2f, 0.7f);
		for (PlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 24 * 24 && !p.isCreative() && !p.isSpectator())) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 140, 0));
			p.damage(getDamageSources().indirectMagic(this, this), 4f);
		}
	}

	/** Used by the CI test: try the boss moves once. */
	public void testMoves(ServerWorld w, LivingEntity target) {
		if (!isBoss()) becomeBoss();
		setTarget(target);
		BossMoves.blinkBehind(w, this, target, 2.0);
		startEclipse(w);
	}

	@Override
	public void onStartedTrackingBy(net.minecraft.server.network.ServerPlayerEntity player) {
		super.onStartedTrackingBy(player);
		if (isBoss()) bossBar.addPlayer(player);
	}

	@Override
	public void onStoppedTrackingBy(net.minecraft.server.network.ServerPlayerEntity player) {
		super.onStoppedTrackingBy(player);
		bossBar.removePlayer(player);
	}

	@Override
	public boolean cannotDespawn() {
		return isBoss() || super.cannotDespawn();
	}

	@Override
	protected void dropLoot(DamageSource source, boolean causedByPlayer) {
		// Rev's helper and the clones only leave a little ectoplasm; the boss drops its full loot table
		if (isBoss()) super.dropLoot(source, causedByPlayer);
		else if (!isClone()) dropStack(new net.minecraft.item.ItemStack(com.phantompowers.ModItems.ECTOPLASM, 1 + random.nextInt(3)));
	}

	private void fade(ServerWorld w) {
		w.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 50, 0.5, 1.0, 0.5, 0.05);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WARDEN_DIG, SoundCategory.HOSTILE, 1.0f, 1.4f);
		discard();
	}

	@Override
	public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (owner != null) nbt.putUuid("Owner", owner);
		nbt.putBoolean("Boss", isBoss());
		nbt.putBoolean("Clone", isClone());
		nbt.putBoolean("Eclipse", eclipse);
		nbt.putBoolean("Decided", decided);
	}

	@Override
	public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.containsUuid("Owner")) owner = nbt.getUuid("Owner");
		if (nbt.getBoolean("Boss")) dataTracker.set(BOSS, true);
		if (nbt.getBoolean("Clone")) dataTracker.set(CLONE, true);
		eclipse = nbt.getBoolean("Eclipse");
		decided = nbt.getBoolean("Decided");
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_WARDEN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_WARDEN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WARDEN_DEATH;
	}
}
