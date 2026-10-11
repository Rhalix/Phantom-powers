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
 * Drags runaways back with spectral chains and calls guard ghosts. Beat it to free the prisoners.
 */
public class GhostJailerEntity extends AbstractGhostEntity implements Monster {
	private static final DustParticleEffect CHAIN = new DustParticleEffect(new Vector3f(0.6f, 0.65f, 0.7f), 1.2f);
	private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Ghost Jailer").formatted(Formatting.DARK_GREEN),
			BossBar.Color.GREEN, BossBar.Style.NOTCHED_10);
	private int chainCooldown = 60;
	private int summonCooldown = 200;

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

		// Spectral chains: yank a fleeing target back
		if (--chainCooldown <= 0 && squaredDistanceTo(target) > 36 && squaredDistanceTo(target) < 900) {
			chainCooldown = 80;
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

		// Call the guards
		if (--summonCooldown <= 0) {
			summonCooldown = 300;
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
