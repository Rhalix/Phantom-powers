package com.phantompowers.entity;

import com.phantompowers.ModSounds;
import com.phantompowers.network.BeamPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.List;

/**
 * Dolora, the emotion eater. She starts out as "Counselor Dolora", a prim, smiling school counselor with a clipboard
 * and a lot of discouraging advice. Hurt her and the disguise tears away into THE MISERY, a shadow wraith in a cracked
 * porcelain mask that latches onto you and feeds on your despair, growing bigger the more it eats. Beat that and she
 * shows her true self: DOLORA, QUEEN OF SORROW, crowned and gowned, raining tears and breaking hearts.
 */
public class DoloraEntity extends AbstractGhostEntity implements Monster {
	public static final int HUMAN = 0, WRAITH = 1, QUEEN = 2;
	private static final TrackedData<Integer> PHASE = DataTracker.registerData(DoloraEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> CHANGING = DataTracker.registerData(DoloraEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> FED = DataTracker.registerData(DoloraEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Integer> DRAIN = DataTracker.registerData(DoloraEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final DustParticleEffect VIOLET = new DustParticleEffect(new Vector3f(0.65f, 0.4f, 1f), 1.4f);
	private static final DustParticleEffect TEAR = new DustParticleEffect(new Vector3f(0.45f, 0.75f, 1f), 1.2f);
	private static final String[] ADVICE = {"Have you considered just... giving up?", "Nobody will remember this, you know.",
			"Your friends only pretend to like you.", "Let's talk about your failures. All of them.", "That's a very small sword. Compensating?",
			"I'm writing down 'hopeless'. Is that spelled with one S?"};
	private static final String[] FEAST = {"Mmm... your despair is DELICIOUS!", "Feel sad for me. Feel SO sad.", "Your misery makes me stronger!"};

	private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Counselor Dolora").formatted(Formatting.LIGHT_PURPLE),
			BossBar.Color.PINK, BossBar.Style.NOTCHED_10);
	private int adviceCooldown = 60, drainCooldown = 80, wispCooldown = 200, rainCooldown = 100, waveCooldown = 160, pulseCooldown = 240, remarkCooldown = 40;
	private int drainTicks;
	private LivingEntity drainTarget;

	public DoloraEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		makeWalker();
		this.experiencePoints = 150;
		setPersistent();
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 260.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0)
				.add(EntityAttributes.GENERIC_ARMOR, 6.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(PHASE, HUMAN);
		builder.add(CHANGING, 0);
		builder.add(FED, 0f);
		builder.add(DRAIN, -1);
	}

	public int phase() {
		return dataTracker.get(PHASE);
	}

	/** Ticks left in a transformation (client: how far through the change she is). */
	public int changing() {
		return dataTracker.get(CHANGING);
	}

	/** How much misery she's eaten (0..1): the wraith swells as it feeds. */
	public float fed() {
		return dataTracker.get(FED);
	}

	/** Entity id she's draining right now, or -1 (the client draws the tether). */
	public int drainId() {
		return dataTracker.get(DRAIN);
	}

	@Override
	protected boolean alwaysPhases() {
		return false;
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1, true) {
			@Override
			public boolean canStart() {
				return phase() == HUMAN && changing() == 0 && super.canStart();
			}
		});
		this.goalSelector.add(3, new HostileGhostEntity.ChargeGoal(this) {
			@Override
			public boolean canStart() {
				return phase() != HUMAN && changing() == 0 && drainTicks == 0 && super.canStart();
			}
		});
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 24f));
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			int ph = phase();
			if (ph == WRAITH && random.nextInt(2) == 0) getWorld().addParticle(ParticleTypes.LARGE_SMOKE,
					getX() + (random.nextDouble() - 0.5), getY() + random.nextDouble() * getHeight(), getZ() + (random.nextDouble() - 0.5), 0, 0.02, 0);
			if (ph == QUEEN && random.nextInt(3) == 0) getWorld().addParticle(TEAR,
					getX() + (random.nextDouble() - 0.5) * 1.2, getY() + 1.5 + random.nextDouble(), getZ() + (random.nextDouble() - 0.5) * 1.2, 0, -0.05, 0);
			if (changing() > 0) for (int i = 0; i < 4; i++) getWorld().addParticle(VIOLET,
					getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 2.5, getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.05, 0);
		}
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (!(getWorld() instanceof ServerWorld w)) return;
		bossBar.setPercent(getHealth() / getMaxHealth());
		if (age % 20 == 0) for (ServerPlayerEntity sp : w.getPlayers(sp -> sp.squaredDistanceTo(this) < 64 * 64)) bossBar.addPlayer(sp);
		// transformations
		int ch = changing();
		if (ch > 0) {
			dataTracker.set(CHANGING, ch - 1);
			setVelocity(getVelocity().multiply(0.5));
			getNavigation().stop();
			if (ch % 5 == 0) w.spawnParticles(VIOLET, getX(), getY() + 1.2, getZ(), 20, 0.8, 1.2, 0.8, 0.05);
			if (ch == 1) finishChange(w);
			return;
		}
		int ph = phase();
		if (ph == HUMAN && getHealth() <= getMaxHealth() * 0.7f) {
			startChange(w, WRAITH);
			return;
		}
		if (ph == WRAITH && getHealth() <= getMaxHealth() * 0.35f) {
			startChange(w, QUEEN);
			return;
		}
		LivingEntity t = getTarget();
		tickDrain(w);
		if (t == null || !t.isAlive()) return;
		switch (ph) {
			case HUMAN -> human(w, t);
			case WRAITH -> wraith(w, t);
			default -> queen(w, t);
		}
	}

	// ------------------------------------------------------------- phase 1

	private void human(ServerWorld w, LivingEntity t) {
		if (--adviceCooldown <= 0 && squaredDistanceTo(t) < 20 * 20) {
			adviceCooldown = 90;
			say(w, ADVICE[random.nextInt(ADVICE.length)], Formatting.LIGHT_PURPLE);
			t.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 0), this);
			t.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0), this);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.HOSTILE, 1.0f, 1.5f);
		}
		// a "snide remark": a little bolt of gloom from her clipboard
		if (--remarkCooldown <= 0 && squaredDistanceTo(t) > 16 && squaredDistanceTo(t) < 18 * 18 && canSee(t)) {
			remarkCooldown = 50;
			Vec3d from = getEyePos().add(getRotationVector().multiply(0.4)).add(0, -0.4, 0);
			beam(w, from, t.getBoundingBox().getCenter(), 0xC080FF);
			t.damage(getDamageSources().indirectMagic(this, this), 4f);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.HOSTILE, 1.4f, 0.8f);
		}
	}

	// ------------------------------------------------------------- phase 2

	private void wraith(ServerWorld w, LivingEntity t) {
		if (drainTicks == 0 && --drainCooldown <= 0 && squaredDistanceTo(t) < 16 * 16 && canSee(t)) {
			drainCooldown = 140;
			startDrain(w, t, 60);
		}
		if (--wispCooldown <= 0) {
			wispCooldown = 260;
			for (int i = 0; i < 2 + random.nextInt(2); i++) {
				HostileGhostEntity g = com.phantompowers.ModEntities.HOSTILE_GHOST.create(w);
				if (g == null) continue;
				g.refreshPositionAndAngles(getX() + random.nextInt(5) - 2, getY() + 1, getZ() + random.nextInt(5) - 2, 0, 0);
				g.setTarget(t);
				w.spawnEntity(g);
				w.spawnParticles(ParticleTypes.SQUID_INK, g.getX(), g.getY() + 1, g.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
			}
			say(w, "Go, my little sorrows! Make them weep!", Formatting.DARK_PURPLE);
		}
	}

	// ------------------------------------------------------------- phase 3

	private void queen(ServerWorld w, LivingEntity t) {
		if (drainTicks == 0 && --drainCooldown <= 0 && squaredDistanceTo(t) < 18 * 18 && canSee(t)) {
			drainCooldown = 200;
			startDrain(w, t, 40);
		}
		if (--rainCooldown <= 0) {
			rainCooldown = 110;
			tearRain(w);
		}
		if (--waveCooldown <= 0 && squaredDistanceTo(t) < 10 * 10) {
			waveCooldown = 150;
			BossMoves.groundWave(w, this, BossMoves.ground(w, getPos()), 9, 7f, 0xB070FF);
			say(w, "Heartbreak!", Formatting.DARK_PURPLE);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 1.2f, 1.3f);
		}
		if (--pulseCooldown <= 0) {
			pulseCooldown = 260;
			despairPulse(w);
		}
	}

	/** Tears fall from the sky: warning circles under everyone nearby, then a splash of sorrow. */
	private void tearRain(ServerWorld w) {
		List<PlayerEntity> ps = w.getPlayers(p -> p.squaredDistanceTo(this) < 28 * 28 && !p.isSpectator() && !p.isCreative());
		for (PlayerEntity p : ps) {
			for (int i = 0; i < 3; i++) {
				Vec3d at = BossMoves.ground(w, p.getPos().add(random.nextGaussian() * 2.5, 0, random.nextGaussian() * 2.5));
				BossMoves.warn(w, at, 1.6, 22, 0x70B0FF);
				BossMoves.later(22, () -> {
					w.spawnParticles(TEAR, at.x, at.y + 0.3, at.z, 30, 0.5, 0.6, 0.5, 0.1);
					w.spawnParticles(ParticleTypes.SPLASH, at.x, at.y + 0.2, at.z, 30, 0.6, 0.1, 0.6, 0.1);
					w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED, SoundCategory.HOSTILE, 0.9f, 1.4f);
					for (Entity e : w.getOtherEntities(this, new net.minecraft.util.math.Box(at, at).expand(1.6, 2, 1.6), e -> e instanceof LivingEntity && !(e instanceof DoloraEntity))) {
						e.damage(getDamageSources().indirectMagic(this, this), 6f);
						((LivingEntity) e).addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), this);
					}
				});
			}
		}
		say(w, "Cry for me!", Formatting.BLUE);
	}

	/** A wave of despair: darkness and sluggishness for everyone, and she drinks it all in. */
	private void despairPulse(ServerWorld w) {
		int fed = 0;
		for (PlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 22 * 22 && !p.isSpectator() && !p.isCreative())) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 120, 0), this);
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 120, 0), this);
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 0), this);
			fed++;
		}
		heal(8f * fed);
		w.spawnParticles(VIOLET, getX(), getY() + 1.5, getZ(), 150, 5, 2, 5, 0.05);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 2f, 0.6f);
		if (fed > 0) say(w, FEAST[random.nextInt(FEAST.length)], Formatting.DARK_PURPLE);
	}

	// ----------------------------------------------------------- emotion drain

	private void startDrain(ServerWorld w, LivingEntity t, int ticks) {
		drainTarget = t;
		drainTicks = ticks;
		dataTracker.set(DRAIN, t.getId());
		say(w, FEAST[random.nextInt(FEAST.length)], Formatting.DARK_PURPLE);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.HOSTILE, 2f, 0.6f);
	}

	/** Latched on: every half second it draws out your feelings, hurting you and healing (and growing) her. */
	private void tickDrain(ServerWorld w) {
		if (drainTicks <= 0) return;
		LivingEntity t = drainTarget;
		if (t == null || !t.isAlive() || squaredDistanceTo(t) > 24 * 24) {
			stopDrain();
			return;
		}
		drainTicks--;
		setVelocity(getVelocity().multiply(0.6));
		getLookControl().lookAt(t);
		Vec3d a = getPos().add(0, getHeight() * 0.7, 0), b = t.getBoundingBox().getCenter();
		if (drainTicks % 2 == 0) {
			// misery flows from you to her
			for (int i = 0; i < 3; i++) {
				Vec3d p = b.lerp(a, random.nextDouble());
				w.spawnParticles(VIOLET, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
			}
		}
		if (drainTicks % 10 == 0) {
			// it hurts more when you're already low: she feeds hardest on the hopeless
			float hurt = t.getHealth() < t.getMaxHealth() * 0.4f ? 3f : 2f;
			t.damage(getDamageSources().indirectMagic(this, this), hurt);
			t.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0), this);
			if (t instanceof PlayerEntity p) p.getHungerManager().addExhaustion(4f);
			heal(hurt * 2f);
			dataTracker.set(FED, Math.min(1f, fed() + 0.05f));
			beam(w, b, a, 0x9060FF);
		}
		if (drainTicks == 0) stopDrain();
	}

	private void stopDrain() {
		drainTicks = 0;
		drainTarget = null;
		dataTracker.set(DRAIN, -1);
	}

	// --------------------------------------------------------- transformations

	private void startChange(ServerWorld w, int to) {
		stopDrain();
		dataTracker.set(CHANGING, 60);
		dataTracker.set(PHASE, to);
		setInvulnerable(true);
		if (to == WRAITH) {
			say(w, "You thought I was here to HELP you? I'm here to EAT. Your sadness, your fear... all of it!", Formatting.DARK_PURPLE);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.2f, 1.4f);
		} else {
			say(w, "Enough hiding. Behold Dolora, QUEEN OF SORROW!", Formatting.DARK_PURPLE);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1.5f, 0.8f);
		}
		org.slf4j.LoggerFactory.getLogger("phantom").info("Dolora changed to phase {}", to);
	}

	private void finishChange(ServerWorld w) {
		setInvulnerable(false);
		int ph = phase();
		// from here on she floats
		setFlying();
		heal(getMaxHealth() * 0.05f);
		if (ph == WRAITH) {
			bossBar.setName(Text.literal("The Misery").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
			bossBar.setColor(BossBar.Color.PURPLE);
		} else {
			bossBar.setName(Text.literal("Dolora, Queen of Sorrow").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
			bossBar.setColor(BossBar.Color.BLUE);
		}
		w.spawnParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
		w.spawnParticles(VIOLET, getX(), getY() + 1.2, getZ(), 120, 1.2, 1.5, 1.2, 0.1);
		despairPulse(w);
	}

	// ------------------------------------------------------------------ misc

	private void say(ServerWorld w, String line, Formatting color) {
		String who = phase() == HUMAN ? "Counselor Dolora" : phase() == WRAITH ? "The Misery" : "Dolora";
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 40 * 40)) {
			p.sendMessage(Text.literal(who + ": \"" + line + "\"").formatted(color), false);
		}
	}

	private static void beam(ServerWorld w, Vec3d from, Vec3d to, int color) {
		BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) to.x, (float) to.y, (float) to.z, color);
		for (ServerPlayerEntity v : PlayerLookup.around(w, from, 96)) {
			if (ServerPlayNetworking.canSend(v, BeamPayload.ID)) ServerPlayNetworking.send(v, beam);
		}
	}

	/** Used by the CI test: run through all three phases and every move once. */
	public void testMoves(ServerWorld w, LivingEntity target) {
		setTarget(target);
		human(w, target);
		startChange(w, WRAITH);
		finishChange(w);
		startDrain(w, target, 20);
		tickDrain(w);
		wraith(w, target);
		startChange(w, QUEEN);
		finishChange(w);
		tearRain(w);
		despairPulse(w);
		stopDrain();
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (changing() > 0) return false;
		return super.damage(source, amount);
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
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Phase", phase());
		nbt.putFloat("Fed", fed());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		dataTracker.set(PHASE, nbt.getInt("Phase"));
		dataTracker.set(FED, nbt.getFloat("Fed"));
		if (phase() != HUMAN) setFlying();
		if (phase() == WRAITH) bossBar.setName(Text.literal("The Misery").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
		if (phase() == QUEEN) bossBar.setName(Text.literal("Dolora, Queen of Sorrow").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return phase() == HUMAN ? SoundEvents.ENTITY_VILLAGER_AMBIENT : ModSounds.GHOST_HOSTILE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return phase() == HUMAN ? SoundEvents.ENTITY_PLAYER_HURT : ModSounds.GHOST_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.GHOST_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return phase() == HUMAN ? 1.3f : phase() == WRAITH ? 0.6f : 1.0f;
	}
}
