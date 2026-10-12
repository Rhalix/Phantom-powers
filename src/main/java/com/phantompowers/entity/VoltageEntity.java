package com.phantompowers.entity;

import com.phantompowers.ModEntities;
import com.phantompowers.ModSounds;
import com.phantompowers.PhantomMod;
import com.phantompowers.network.BeamPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Voltage: a punk-rock ghost with crackling electric hair and a lightning guitar.
 * Power chords blast sound waves, her solos call down lightning, and her fans show up to fight for her.
 * Amp Blasts send rolling sound shockwaves along the ground, and she Stage Dives onto you with a lightning crash.
 * Below 40% health she goes into an Encore: faster, louder, wilder, and the floor itself gets electrified.
 */
public class VoltageEntity extends AbstractGhostEntity implements Monster {
	private static final TrackedData<Boolean> ENCORE = DataTracker.registerData(VoltageEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Voltage").formatted(Formatting.LIGHT_PURPLE),
			BossBar.Color.PINK, BossBar.Style.NOTCHED_10);
	private int chordCooldown = 40;
	private int boltCooldown = 80;
	private int fanCooldown = 200;
	private int strum;
	private int ampCooldown = 120;
	private int diveCooldown = 200;
	private int floorCooldown = 140;

	public VoltageEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 140;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 260.0)
				.add(EntityAttributes.GENERIC_ARMOR, 5.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(ENCORE, false);
	}

	public boolean isEncore() {
		return dataTracker.get(ENCORE);
	}

	/** 0..1, how hard she's strumming right now (client animation). */
	public float strum() {
		return Math.min(1f, strum / 8f);
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
		if (strum > 0) strum--;
		if (getWorld().isClient && random.nextInt(isEncore() ? 2 : 5) == 0) {
			getWorld().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + (random.nextDouble() - 0.5) * 1.5,
					getY() + getHeight() * 0.95, getZ() + (random.nextDouble() - 0.5) * 1.5, 0, 0.1, 0);
		}
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		bossBar.setPercent(getHealth() / getMaxHealth());
		if (!(getWorld() instanceof ServerWorld w)) return;
		if (!isEncore() && getHealth() <= getMaxHealth() * 0.4f) encore(w);
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) return;
		float speed = isEncore() ? 0.6f : 1f;

		if (--chordCooldown <= 0 && squaredDistanceTo(target) < 16 * 16) {
			chordCooldown = (int) (55 * speed);
			powerChord(w, target);
		}
		if (--boltCooldown <= 0) {
			boltCooldown = (int) (90 * speed);
			lightning(w, target.getPos());
			if (isEncore()) lightning(w, target.getPos().add(random.nextInt(7) - 3, 0, random.nextInt(7) - 3));
		}
		if (--ampCooldown <= 0 && squaredDistanceTo(target) < 18 * 18) {
			ampCooldown = (int) (170 * speed);
			ampBlast(w);
		}
		if (--diveCooldown <= 0 && squaredDistanceTo(target) < 24 * 24) {
			diveCooldown = (int) (220 * speed);
			stageDive(w, target);
		}
		if (isEncore() && --floorCooldown <= 0) {
			floorCooldown = 150;
			electricFloor(w);
		}
		if (--fanCooldown <= 0) {
			fanCooldown = (int) (260 * speed);
			for (int i = 0; i < 2; i++) {
				HostileGhostEntity fan = ModEntities.HOSTILE_GHOST.create(w);
				if (fan == null) continue;
				fan.refreshPositionAndAngles(getX() + random.nextInt(5) - 2, getY() + 1, getZ() + random.nextInt(5) - 2, 0, 0);
				fan.setCustomName(Text.literal("Screaming Fan"));
				fan.setTarget(target);
				w.spawnEntity(fan);
			}
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.HOSTILE, 1.5f, 0.6f);
		}
	}

	/** A sonic blast of sound straight at the target. */
	private void powerChord(ServerWorld w, LivingEntity target) {
		strum = 10;
		Vec3d from = getPos().add(0, getHeight() * 0.5, 0);
		Vec3d to = target.getPos().add(0, target.getHeight() * 0.5, 0);
		Vec3d dir = to.subtract(from).normalize();
		for (Entity e : w.getOtherEntities(this, getBoundingBox().expand(16), e -> e instanceof LivingEntity && !(e instanceof AbstractGhostEntity))) {
			Vec3d d = e.getPos().add(0, e.getHeight() / 2, 0).subtract(from);
			double len = d.length();
			if (len > 16 || len < 0.1 || d.multiply(1 / len).dotProduct(dir) < 0.85) continue;
			e.damage(getDamageSources().sonicBoom(this), isEncore() ? 9f : 7f);
			e.addVelocity(dir.x * 1.3, 0.35, dir.z * 1.3);
			e.velocityModified = true;
		}
		BossMoves.soundWave(w, from, dir, 16f, isEncore() ? 0xFF33CC : 0xFF66FF);
		playChord(w, 0.8f);
	}

	/** Her amps boom: three sound shockwaves roll out across the floor. Jump over them! */
	public void ampBlast(ServerWorld w) {
		strum = 12;
		for (int i = 0; i < 3; i++) {
			BossMoves.later(1 + i * 12, () -> {
				if (!isAlive()) return;
				Vec3d spot = BossMoves.ground(w, getPos());
				BossMoves.groundWave(w, this, spot, 13, isEncore() ? 7f : 5f, 0xFF66FF);
				playChord(w, 0.6f);
			});
		}
	}

	/** Leaps off the stage and crashes down on the target in a burst of lightning. */
	public void stageDive(ServerWorld w, LivingEntity target) {
		Vec3d spot = BossMoves.ground(w, target.getPos());
		BossMoves.warn(w, spot, 3.0, 20, 0xFFEE55);
		strum = 10;
		addVelocity(0, 1.4, 0);
		velocityModified = true;
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 48 * 48)) {
			p.sendMessage(Text.literal("Voltage: \"STAGE DIVE!\"").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), true);
		}
		BossMoves.later(20, () -> {
			if (!isAlive()) return;
			refreshPositionAndAngles(spot.x, spot.y + 0.1, spot.z, getYaw(), 0);
			lightning(w, spot);
			BossMoves.groundWave(w, this, spot, 8, isEncore() ? 11f : 9f, 0xFFEE55);
		});
	}

	/** Encore only: lightning bursts out of the floor in rings around her. */
	public void electricFloor(ServerWorld w) {
		strum = 14;
		for (int ring = 0; ring < 3; ring++) {
			final double r = 4 + ring * 4;
			final int n = 5 + ring * 3;
			BossMoves.later(1 + ring * 10, () -> {
				if (!isAlive()) return;
				double off = random.nextDouble() * Math.PI;
				for (int i = 0; i < n; i++) {
					double a = off + i * Math.PI * 2 / n;
					Vec3d at = BossMoves.ground(w, getPos().add(Math.cos(a) * r, 0, Math.sin(a) * r));
					lightning(w, at);
				}
			});
		}
	}

	/** Used by the CI test: try every move once. */
	public void testMoves(ServerWorld w, LivingEntity target) {
		powerChord(w, target);
		ampBlast(w);
		stageDive(w, target);
		electricFloor(w);
	}

	private void lightning(ServerWorld w, Vec3d at) {
		strum = 6;
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
		if (bolt != null) {
			bolt.setCosmetic(true);
			bolt.refreshPositionAfterTeleport(at);
			w.spawnEntity(bolt);
		}
		for (Entity e : w.getOtherEntities(this, new net.minecraft.util.math.Box(at, at).expand(2.5, 3, 2.5),
				e -> e instanceof LivingEntity && !(e instanceof AbstractGhostEntity))) {
			e.damage(getDamageSources().lightningBolt(), isEncore() ? 9f : 7f);
		}
		w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.5, at.z, 40, 1.0, 0.5, 1.0, 0.3);
		playChord(w, 1.2f);
	}

	private int musicCooldown;

	/** Real guitar riffs (not too often, so they don't pile up on each other). */
	private void playChord(ServerWorld w, float pitch) {
		if (age < musicCooldown) {
			w.playSound(null, getX(), getY(), getZ(), ModSounds.GUITAR_HIT, SoundCategory.HOSTILE, 2.0f, pitch < 1 ? 1f : 1.12f);
			return;
		}
		musicCooldown = age + 50;
		w.playSound(null, getX(), getY(), getZ(), pitch > 1 ? ModSounds.GUITAR_SOLO : ModSounds.GUITAR_RIFF, SoundCategory.HOSTILE, 3.0f, 1.0f);
	}

	private void encore(ServerWorld w) {
		dataTracker.set(ENCORE, true);
		heal(getMaxHealth() * 0.1f);
		bossBar.setName(Text.literal("Voltage: ENCORE!").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
		bossBar.setColor(BossBar.Color.PURPLE);
		EntityAttributeInstance speed = getAttributeInstance(EntityAttributes.GENERIC_FLYING_SPEED);
		if (speed != null && speed.getModifier(PhantomMod.id("encore")) == null) {
			speed.addPersistentModifier(new EntityAttributeModifier(PhantomMod.id("encore"), 0.25, EntityAttributeModifier.Operation.ADD_VALUE));
		}
		for (int i = 0; i < 4; i++) lightning(w, getPos().add(random.nextInt(11) - 5, 0, random.nextInt(11) - 5));
		w.playSound(null, getX(), getY(), getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.HOSTILE, 3.0f, 1.3f);
		org.slf4j.LoggerFactory.getLogger("phantom").info("Voltage started her Encore (stage two)");
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 80 * 80)) {
			p.sendMessage(Text.literal("Voltage: \"You want an ENCORE? You got one!\"").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Encore", isEncore());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("Encore")) dataTracker.set(ENCORE, true);
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
		return ModSounds.GUITAR_HIT;
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
