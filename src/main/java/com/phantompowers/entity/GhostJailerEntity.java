package com.phantompowers.entity;

import com.phantompowers.ModEntities;
import com.phantompowers.ModSounds;
import net.minecraft.entity.Entity;
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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * The boss of the Ghost Prison: a huge hooded jailer wrapped in chains.
 * Drags runaways back with spectral chains, calls guard ghosts, slams the ground with shockwaves,
 * locks you in iron cages and blinks behind you. Below half health it goes into Lockdown.
 * Beat it to free the prisoners.
 */
public class GhostJailerEntity extends AbstractGhostEntity implements Monster {
	private static final DustParticleEffect CHAIN = new DustParticleEffect(new Vector3f(0.6f, 0.65f, 0.7f), 1.2f);
	private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Ghost Jailer").formatted(Formatting.DARK_GREEN),
			BossBar.Color.GREEN, BossBar.Style.NOTCHED_10);
	private int chainCooldown = 60;
	private int summonCooldown = 200;
	private int slamCooldown = 100;
	private int cageCooldown = 180;
	private int blinkCooldown = 120;
	private int lockdownCooldown = 160;
	private boolean enraged;

	public GhostJailerEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 120;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 220.0)
				.add(EntityAttributes.GENERIC_ARMOR, 8.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 11.0)
				.add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.5)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new HostileGhostEntity.ChargeGoal(this));
		this.goalSelector.add(6, new DriftGoal(this));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 20.0f));
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		bossBar.setPercent(getHealth() / getMaxHealth());
		LivingEntity target = getTarget();
		if (!(getWorld() instanceof ServerWorld w) || target == null) return;

		if (!enraged && getHealth() <= getMaxHealth() * 0.5f) enrage(w);
		float pace = enraged ? 0.65f : 1f;

		// Spectral chains: yank a fleeing target back
		if (--chainCooldown <= 0 && squaredDistanceTo(target) > 36 && squaredDistanceTo(target) < 900) {
			chainCooldown = (int) (80 * pace);
			chain(w, target);
		}

		// Prison Slam: marks the ground under you, leaps over, and smashes down in a shockwave
		if (--slamCooldown <= 0 && squaredDistanceTo(target) < 24 * 24) {
			slamCooldown = (int) (150 * pace);
			slam(w, target);
		}

		// Solitary Confinement: an iron-bar cage slams shut around you
		if (--cageCooldown <= 0 && squaredDistanceTo(target) < 20 * 20) {
			cageCooldown = (int) (260 * pace);
			lockUp(w, target);
		}

		// Shadow Step: vanishes and reappears right behind you
		if (--blinkCooldown <= 0 && (squaredDistanceTo(target) > 14 * 14 || random.nextInt(3) == 0)) {
			blinkCooldown = (int) (180 * pace);
			BossMoves.blinkBehind(w, this, target, 2.5);
		}

		// Lockdown (enraged): chains lash out at everyone nearby at once
		if (enraged && --lockdownCooldown <= 0) {
			lockdownCooldown = 200;
			for (ServerPlayerEntity p : w.getPlayers(p -> p.isAlive() && !p.isSpectator() && p.squaredDistanceTo(this) < 20 * 20)) chain(w, p);
			w.playSound(null, getX(), getY(), getZ(), ModSounds.JAILER_ROAR, SoundCategory.HOSTILE, 3.0f, 0.7f);
		}

		// Call the guards
		if (--summonCooldown <= 0) {
			summonCooldown = (int) (300 * pace);
			Box near = getBoundingBox().expand(24);
			if (w.getEntitiesByClass(HostileGhostEntity.class, near, g -> true).size() < 6) {
				for (int i = 0; i < 2; i++) {
					HostileGhostEntity g = ModEntities.HOSTILE_GHOST.create(w);
					if (g == null) continue;
					g.refreshPositionAndAngles(getX() + random.nextInt(7) - 3, getY() + 1, getZ() + random.nextInt(7) - 3, 0, 0);
					g.setTarget(target);
					w.spawnEntity(g);
					w.spawnParticles(ParticleTypes.SOUL, g.getX(), g.getY() + 1, g.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
				}
				w.playSound(null, getX(), getY(), getZ(), ModSounds.JAILER_ROAR, SoundCategory.HOSTILE, 3.0f, 1.0f);
			}
		}
	}

	private void chain(ServerWorld w, LivingEntity target) {
		Vec3d from = getPos().add(0, getHeight() * 0.6, 0);
		Vec3d to = target.getPos().add(0, target.getHeight() * 0.5, 0);
		Vec3d d = to.subtract(from);
		for (double t = 0; t < 1; t += 0.04) {
			Vec3d p = from.add(d.multiply(t));
			w.spawnParticles(CHAIN, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
		}
		Vec3d pull = d.normalize().multiply(-1.6);
		target.addVelocity(pull.x, 0.5, pull.z);
		target.velocityModified = true;
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2), this);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.HOSTILE, 2.0f, 0.6f);
	}

	public void slam(ServerWorld w, LivingEntity target) {
		Vec3d spot = BossMoves.ground(w, target.getPos());
		BossMoves.warn(w, spot, 3.0, 22, 0x55FF99);
		w.playSound(null, getX(), getY(), getZ(), ModSounds.JAILER_ROAR, SoundCategory.HOSTILE, 3.0f, 0.8f);
		// Rise up over the marked spot...
		BossMoves.later(8, () -> {
			if (!isAlive()) return;
			w.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.5, 1, 0.5, 0.02);
			refreshPositionAndAngles(spot.x, spot.y + 7, spot.z, getYaw(), 0);
		});
		// ...and come crashing down
		BossMoves.later(22, () -> {
			if (!isAlive()) return;
			refreshPositionAndAngles(spot.x, spot.y + 0.1, spot.z, getYaw(), 0);
			BossMoves.groundWave(w, this, spot, enraged ? 12 : 9, enraged ? 11f : 9f, 0x55FF99);
			w.spawnParticles(CHAIN, spot.x, spot.y + 0.5, spot.z, 80, 2.5, 0.4, 2.5, 0.1);
		});
	}

	public void lockUp(ServerWorld w, LivingEntity target) {
		if (BossMoves.cage(w, target, net.minecraft.block.Blocks.IRON_BARS, enraged ? 120 : 80) > 0 && target instanceof ServerPlayerEntity p) {
			p.sendMessage(Text.literal("Ghost Jailer: \"Back in your cell!\"").formatted(Formatting.DARK_GREEN, Formatting.BOLD), true);
		}
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 3), this);
	}

	private void enrage(ServerWorld w) {
		enraged = true;
		bossBar.setName(Text.literal("Ghost Jailer: LOCKDOWN").formatted(Formatting.DARK_GREEN, Formatting.BOLD));
		bossBar.setColor(BossBar.Color.RED);
		w.playSound(null, getX(), getY(), getZ(), ModSounds.JAILER_ROAR, SoundCategory.HOSTILE, 4.0f, 0.6f);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_BELL_USE, SoundCategory.HOSTILE, 3.0f, 0.5f);
		w.spawnParticles(CHAIN, getX(), getY() + 2, getZ(), 120, 2, 2, 2, 0.1);
		org.slf4j.LoggerFactory.getLogger("phantom").info("Ghost Jailer went into Lockdown (stage two)");
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 80 * 80)) {
			p.sendMessage(Text.literal("Ghost Jailer: \"LOCKDOWN! Nobody escapes my prison!\"").formatted(Formatting.DARK_GREEN, Formatting.BOLD), false);
		}
	}

	/** Used by the CI test: try every move once. */
	public void testMoves(ServerWorld w, LivingEntity target) {
		chain(w, target);
		slam(w, target);
		lockUp(w, target);
		BossMoves.blinkBehind(w, this, target, 2.5);
		enrage(w);
	}
	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit && target instanceof LivingEntity le) {
			le.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 0), this);
		}
		return hit;
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (getWorld() instanceof ServerWorld w) {
			// The prison falls: every caged ghost goes free
			for (AbstractGhostEntity g : w.getEntitiesByClass(AbstractGhostEntity.class, getBoundingBox().expand(48), g -> g != this)) {
				g.clearHome();
			}
			for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 96 * 96)) {
				p.sendMessage(Text.literal("The Ghost Jailer is defeated! The prison cells fall open...").formatted(Formatting.GREEN), false);
			}
		}
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
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (hasCustomName()) bossBar.setName(getDisplayName());
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
		return ModSounds.JAILER_ROAR;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.GHOST_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.GHOST_DEATH;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 300;
	}
}
