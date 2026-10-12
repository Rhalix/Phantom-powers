package com.phantompowers.entity;

import com.phantompowers.ModEntities;
import com.phantompowers.ModSounds;
import com.phantompowers.PhantomMod;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.List;

/**
 * The Wishkeeper: an ancient genie ghost (long black hair, gold circlet, blue veil and silks) who grants
 * wishes... the wrong way. Say you want to fly and she'll throw you into the sky. Want to be strong? Enjoy being slow.
 * She fires homing wish orbs, vanishes in smoke, and below half health calls up a Wish Storm
 * and grants herself one last wish.
 */
public class WishkeeperEntity extends AbstractGhostEntity implements Monster {
	private static final TrackedData<Boolean> STORM = DataTracker.registerData(WishkeeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final DustParticleEffect GOLD = new DustParticleEffect(new Vector3f(1f, 0.82f, 0.25f), 1.4f);
	private static final DustParticleEffect TEAL = new DustParticleEffect(new Vector3f(0.25f, 0.95f, 0.85f), 1.6f);

	private final ServerBossBar bossBar = new ServerBossBar(Text.literal("The Wishkeeper").formatted(Formatting.GOLD),
			BossBar.Color.YELLOW, BossBar.Style.NOTCHED_10);
	private int wishCooldown = 60;
	private int orbCooldown = 40;
	private int blinkCooldown = 140;
	private int sandCooldown = 160;
	private int stormCooldown = 120;
	private int cast;
	private boolean selfWish;

	public WishkeeperEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 150;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 260.0)
				.add(EntityAttributes.GENERIC_ARMOR, 6.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.55)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(STORM, false);
	}

	public boolean isStorm() {
		return dataTracker.get(STORM);
	}

	/** 0..1, how hard she's casting right now (client animation). */
	public float casting() {
		return Math.min(1f, cast / 10f);
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
		if (cast > 0) cast--;
		if (getWorld().isClient) {
			// Smoke trail where her tail fades away, and glittering gold
			getWorld().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5) * 0.6, getY() + 0.3,
					getZ() + (random.nextDouble() - 0.5) * 0.6, 0, -0.02, 0);
			if (random.nextInt(3) == 0) {
				getWorld().addParticle(isStorm() ? ParticleTypes.ENCHANT : ParticleTypes.WAX_OFF, getX() + (random.nextDouble() - 0.5) * 1.5,
						getY() + 1 + random.nextDouble() * 2, getZ() + (random.nextDouble() - 0.5) * 1.5, 0, 0.05, 0);
			}
		}
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		bossBar.setPercent(getHealth() / getMaxHealth());
		if (!(getWorld() instanceof ServerWorld w)) return;
		if (!isStorm() && getHealth() <= getMaxHealth() * 0.5f) wishStorm(w);
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) return;
		float pace = isStorm() ? 0.65f : 1f;

		if (--wishCooldown <= 0) {
			wishCooldown = (int) (130 * pace);
			twistedWish(w, target);
		}
		if (--orbCooldown <= 0 && squaredDistanceTo(target) < 32 * 32) {
			orbCooldown = (int) (70 * pace);
			wishOrbs(w, target);
		}
		if (--blinkCooldown <= 0 && (squaredDistanceTo(target) > 16 * 16 || random.nextInt(3) == 0)) {
			blinkCooldown = (int) (170 * pace);
			BossMoves.blinkBehind(w, this, target, 3.0);
			w.spawnParticles(TEAL, getX(), getY() + 1.5, getZ(), 40, 0.6, 1, 0.6, 0.02);
		}
		if (--sandCooldown <= 0 && squaredDistanceTo(target) < 20 * 20) {
			sandCooldown = (int) (200 * pace);
			sandCyclone(w, target);
		}
		if (isStorm() && --stormCooldown <= 0) {
			stormCooldown = 180;
			summonPhantoms(w, target);
		}
		if (isStorm() && !selfWish && getHealth() <= getMaxHealth() * 0.25f) {
			selfWish = true;
			heal(getMaxHealth() * 0.2f);
			say(w, "\"I wish... to be WHOLE again!\"");
			w.spawnParticles(GOLD, getX(), getY() + 2, getZ(), 120, 1.2, 1.5, 1.2, 0.1);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 2.0f, 1.4f);
		}
	}

	private void say(ServerWorld w, String line) {
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 48 * 48)) {
			p.sendMessage(Text.literal("The Wishkeeper: ").formatted(Formatting.GOLD).append(Text.literal(line).formatted(Formatting.YELLOW, Formatting.ITALIC)), false);
		}
	}

	/** She "grants" a wish to the target, twisted into a curse. */
	public void twistedWish(ServerWorld w, LivingEntity target) {
		cast = 20;
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_WOLOLO, SoundCategory.HOSTILE, 2.0f, 1.3f);
		Vec3d from = getPos().add(0, getHeight() * 0.7, 0), to = target.getPos().add(0, target.getHeight() / 2, 0);
		for (double t = 0; t < 1; t += 0.05) {
			Vec3d p = from.lerp(to, t);
			w.spawnParticles(GOLD, p.x, p.y + Math.sin(t * Math.PI) * 1.5, p.z, 1, 0, 0, 0, 0);
		}
		switch (random.nextInt(5)) {
			case 0 -> {
				say(w, "\"You wish you could fly? Granted!\"");
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 70, 2), this);
			}
			case 1 -> {
				say(w, "\"You wish to be strong? Then carry THIS weight!\"");
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 3), this);
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 160, 1), this);
			}
			case 2 -> {
				say(w, "\"You wish for riches? Take them ALL!\"");
				Vec3d c = target.getPos();
				for (int i = 0; i < 6; i++) {
					final int k = i;
					BossMoves.later(6 + i * 5, () -> {
						if (!isAlive()) return;
						Vec3d at = c.add(random.nextGaussian() * 2.5, 0.2, random.nextGaussian() * 2.5);
						w.spawnParticles(new net.minecraft.particle.ItemStackParticleEffect(ParticleTypes.ITEM,
								new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLD_NUGGET)), at.x, at.y + 2, at.z, 20, 0.5, 0.5, 0.5, 0.15);
						w.createExplosion(this, at.x, at.y, at.z, 1.6f + k * 0.1f, World.ExplosionSourceType.NONE);
					});
				}
			}
			case 3 -> {
				say(w, "\"You wish to disappear? Now you can't see a thing!\"");
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0), this);
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 120, 0), this);
			}
			default -> {
				say(w, "\"You wish to be safe? Then stay in your golden cage!\"");
				BossMoves.cage(w, target, Blocks.GOLD_BLOCK, isStorm() ? 90 : 60);
			}
		}
	}

	/** Homing golden orbs that lift you into the air. */
	public void wishOrbs(ServerWorld w, LivingEntity target) {
		cast = 12;
		int n = isStorm() ? 3 : 2;
		for (int i = 0; i < n; i++) {
			ShulkerBulletEntity orb = new ShulkerBulletEntity(w, this, target, Direction.Axis.values()[i % 3]);
			orb.setPosition(getX() + (i - (n - 1) / 2.0) * 1.2, getY() + getHeight() * 0.75, getZ());
			w.spawnEntity(orb);
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_SHULKER_SHOOT, SoundCategory.HOSTILE, 2.0f, 0.8f);
	}

	/** A spinning cyclone of golden sand: blinds and blasts everyone around. */
	public void sandCyclone(ServerWorld w, LivingEntity target) {
		cast = 20;
		Vec3d c = BossMoves.ground(w, getPos());
		BossMoves.warn(w, c, 4.0, 14, 0xFFCC44);
		for (int t = 0; t < 30; t += 2) {
			final int tt = t;
			BossMoves.later(Math.max(1, t), () -> {
				if (!isAlive()) return;
				for (int i = 0; i < 20; i++) {
					double a = i * Math.PI * 2 / 20 + tt * 0.4;
					double r = 1 + tt * 0.25;
					w.spawnParticles(GOLD, getX() + Math.cos(a) * r, getY() + (i % 6) * 0.6, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
				}
				w.spawnParticles(new net.minecraft.particle.BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.SAND.getDefaultState()),
						getX(), getY() + 2, getZ(), 15, 3, 2, 3, 0);
			});
		}
		BossMoves.later(16, () -> {
			if (!isAlive()) return;
			BossMoves.groundWave(w, this, BossMoves.ground(w, getPos()), isStorm() ? 11 : 8, isStorm() ? 10f : 7f, 0xFFCC44);
			for (Entity e : w.getOtherEntities(this, getBoundingBox().expand(9), e -> e instanceof LivingEntity && !(e instanceof AbstractGhostEntity))) {
				((LivingEntity) e).addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 50, 0), this);
			}
		});
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BREEZE_WHIRL, SoundCategory.HOSTILE, 2.0f, 0.6f);
	}

	/** Wish Storm: smoky copies of herself appear to help. */
	public void summonPhantoms(ServerWorld w, LivingEntity target) {
		cast = 20;
		for (int i = 0; i < 2; i++) {
			HostileGhostEntity g = ModEntities.HOSTILE_GHOST.create(w);
			if (g == null) continue;
			g.refreshPositionAndAngles(getX() + random.nextInt(7) - 3, getY() + 1, getZ() + random.nextInt(7) - 3, 0, 0);
			g.setCustomName(Text.literal("Wish Phantom"));
			g.setTarget(target);
			w.spawnEntity(g);
			w.spawnParticles(TEAL, g.getX(), g.getY() + 1, g.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 2.0f, 0.7f);
	}

	private void wishStorm(ServerWorld w) {
		dataTracker.set(STORM, true);
		heal(getMaxHealth() * 0.1f);
		bossBar.setName(Text.literal("The Wishkeeper: WISH STORM").formatted(Formatting.GOLD, Formatting.BOLD));
		bossBar.setColor(BossBar.Color.PURPLE);
		EntityAttributeInstance speed = getAttributeInstance(EntityAttributes.GENERIC_FLYING_SPEED);
		if (speed != null && speed.getModifier(PhantomMod.id("wish_storm")) == null) {
			speed.addPersistentModifier(new EntityAttributeModifier(PhantomMod.id("wish_storm"), 0.2, EntityAttributeModifier.Operation.ADD_VALUE));
		}
		w.spawnParticles(GOLD, getX(), getY() + 2, getZ(), 200, 2, 2, 2, 0.2);
		w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0);
		w.playSound(null, getX(), getY(), getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.HOSTILE, 2.5f, 1.5f);
		org.slf4j.LoggerFactory.getLogger("phantom").info("The Wishkeeper called up a Wish Storm (stage two)");
		say(w, "\"Enough! I wish for a STORM!\"");
	}

	/** Used by the CI test: try every move once. */
	public void testMoves(ServerWorld w, LivingEntity target) {
		for (int i = 0; i < 5; i++) twistedWish(w, target);
		wishOrbs(w, target);
		sandCyclone(w, target);
		summonPhantoms(w, target);
		wishStorm(w);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Storm", isStorm());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("Storm")) dataTracker.set(STORM, true);
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
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (getWorld() instanceof ServerWorld w) say(w, "\"No... my wishes... all used up...\"");
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
		return SoundEvents.ENTITY_EVOKER_AMBIENT;
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
	public float getSoundPitch() {
		return 1.35f;
	}
}
