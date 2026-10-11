package com.phantompowers.entity;

import com.phantompowers.ModEntities;
import com.phantompowers.ModSounds;
import com.phantompowers.PhantomMod;
import com.phantompowers.network.BeamPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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

/**
 * The Crate King: a ghost obsessed with boxes, wearing a big cardboard box for a head.
 * Stage one he hurls crates at you. At half health he pulls out Pandora's Box and turns evil:
 * dark beams, a whirling crate storm, and ghosts pouring out of the box.
 */
public class CrateKingEntity extends AbstractGhostEntity implements Monster {
	private static final TrackedData<Boolean> EVIL = DataTracker.registerData(CrateKingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final DustParticleEffect DARK = new DustParticleEffect(new Vector3f(0.55f, 0.1f, 0.85f), 1.6f);

	private final ServerBossBar bossBar = new ServerBossBar(Text.literal("The Crate King").formatted(Formatting.GOLD),
			BossBar.Color.YELLOW, BossBar.Style.NOTCHED_10);
	private int throwCooldown = 40;
	private int volleyCooldown = 160;
	private int beamCooldown = 60;
	private int stormCooldown = 140;
	private int summonCooldown = 200;
	private int boxOpen;

	public CrateKingEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 150;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0)
				.add(EntityAttributes.GENERIC_ARMOR, 6.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(EVIL, false);
	}

	public boolean isEvil() {
		return dataTracker.get(EVIL);
	}

	/** 0..1 how far the lid of Pandora's Box is open (client animation). */
	public float lidOpen() {
		return isEvil() ? 0.35f + 0.65f * Math.min(1f, boxOpen / 10f) : 0f;
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
		if (boxOpen > 0) boxOpen--;
		if (getWorld().isClient && isEvil() && age % 2 == 0) {
			getWorld().addParticle(ParticleTypes.WITCH, getX() + (random.nextDouble() - 0.5) * 2, getY() + 2.5, getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.05, 0);
		}
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		bossBar.setPercent(getHealth() / getMaxHealth());
		if (!(getWorld() instanceof ServerWorld w)) return;
		if (!isEvil() && getHealth() <= getMaxHealth() * 0.5f) unbox(w);
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) return;

		if (!isEvil()) {
			if (--throwCooldown <= 0) {
				throwCooldown = 35;
				throwCrate(w, target, 0f, 5f);
			}
			if (--volleyCooldown <= 0) {
				volleyCooldown = 180;
				for (int i = -2; i <= 2; i++) throwCrate(w, target, i * 9f, 5f);
				w.playSound(null, getX(), getY(), getZ(), ModSounds.GHOST_HOSTILE, SoundCategory.HOSTILE, 2.0f, 1.4f);
			}
		} else {
			if (--throwCooldown <= 0) {
				throwCooldown = 25;
				throwCrate(w, target, 0f, 7f);
			}
			if (--beamCooldown <= 0) {
				beamCooldown = 45;
				darkBeam(w, target);
			}
			if (--stormCooldown <= 0) {
				stormCooldown = 160;
				crateStorm(w);
			}
			if (--summonCooldown <= 0) {
				summonCooldown = 260;
				boxOpen = 20;
				for (int i = 0; i < 2; i++) {
					HostileGhostEntity g = ModEntities.HOSTILE_GHOST.create(w);
					if (g == null) continue;
					g.refreshPositionAndAngles(getX(), getY() + 2, getZ(), random.nextFloat() * 360f, 0);
					g.setTarget(target);
					w.spawnEntity(g);
				}
				w.spawnParticles(DARK, getX(), getY() + 2, getZ(), 40, 0.6, 0.6, 0.6, 0.05);
				w.playSound(null, getX(), getY(), getZ(), ModSounds.JAILER_ROAR, SoundCategory.HOSTILE, 2.5f, 1.3f);
			}
		}
	}

	private void throwCrate(ServerWorld w, LivingEntity target, float yawOffset, float damage) {
		CrateProjectileEntity crate = new CrateProjectileEntity(ModEntities.CRATE_PROJECTILE, this, w, damage);
		crate.setPosition(getX(), getY() + getHeight() * 0.55, getZ());
		Vec3d to = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(crate.getPos());
		Vec3d dir = to.rotateY(yawOffset * 0.0174533f);
		crate.setVelocity(dir.x, dir.y + to.length() * 0.04, dir.z, isEvil() ? 1.5f : 1.2f, 2.0f);
		w.spawnEntity(crate);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WITCH_THROW, SoundCategory.HOSTILE, 1.5f, 0.6f);
	}

	private void darkBeam(ServerWorld w, LivingEntity target) {
		Vec3d from = getPos().add(0, getHeight() * 0.55, 0);
		Vec3d to = target.getPos().add(0, target.getHeight() * 0.5, 0);
		boxOpen = 10;
		if (canSee(target)) target.damage(getDamageSources().indirectMagic(this, this), 7f);
		BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) to.x, (float) to.y, (float) to.z, 0x9933FF);
		for (ServerPlayerEntity viewer : PlayerLookup.around(w, from, 96)) {
			if (ServerPlayNetworking.canSend(viewer, BeamPayload.ID)) ServerPlayNetworking.send(viewer, beam);
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.5f, 0.6f);
	}

	/** A ring of crates bursts out in every direction. */
	private void crateStorm(ServerWorld w) {
		boxOpen = 15;
		for (int i = 0; i < 10; i++) {
			double a = i * Math.PI * 2 / 10;
			CrateProjectileEntity crate = new CrateProjectileEntity(ModEntities.CRATE_PROJECTILE, this, w, 6f);
			crate.setPosition(getX(), getY() + getHeight() * 0.5, getZ());
			crate.setVelocity(Math.cos(a), 0.1, Math.sin(a), 1.0f, 0f);
			w.spawnEntity(crate);
		}
		w.spawnParticles(DARK, getX(), getY() + 2, getZ(), 60, 1.0, 0.8, 1.0, 0.05);
		w.playSound(null, getX(), getY(), getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.HOSTILE, 2.0f, 0.5f);
	}

	/** Stage two: he pulls out Pandora's Box and turns evil. */
	private void unbox(ServerWorld w) {
		org.slf4j.LoggerFactory.getLogger("phantom").info("The Crate King opened Pandora's Box (stage two)");
		dataTracker.set(EVIL, true);
		boxOpen = 30;
		heal(getMaxHealth() * 0.1f);
		bossBar.setName(Text.literal("The Crate King, Unboxed").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
		bossBar.setColor(BossBar.Color.PURPLE);
		addMod(EntityAttributes.GENERIC_ATTACK_DAMAGE, "evil_damage", 5.0);
		addMod(EntityAttributes.GENERIC_FLYING_SPEED, "evil_speed", 0.2);
		addMod(EntityAttributes.GENERIC_ARMOR, "evil_armor", 4.0);
		w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0);
		w.spawnParticles(DARK, getX(), getY() + 2, getZ(), 150, 1.5, 1.5, 1.5, 0.1);
		w.playSound(null, getX(), getY(), getZ(), ModSounds.JAILER_ROAR, SoundCategory.HOSTILE, 4.0f, 0.7f);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 2.0f, 0.6f);
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 80 * 80)) {
			p.sendMessage(Text.literal("The Crate King opened Pandora's Box... he's not playing anymore!").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
		}
	}

	private void addMod(net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attr, String id, double v) {
		if (attr == null) return;
		EntityAttributeInstance inst = getAttributeInstance(attr);
		if (inst == null || inst.getModifier(PhantomMod.id(id)) != null) return;
		inst.addPersistentModifier(new EntityAttributeModifier(PhantomMod.id(id), v, EntityAttributeModifier.Operation.ADD_VALUE));
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Evil", isEvil());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("Evil")) {
			dataTracker.set(EVIL, true);
			bossBar.setName(Text.literal("The Crate King, Unboxed").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
			bossBar.setColor(BossBar.Color.PURPLE);
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
	public boolean cannotDespawn() {
		return true;
	}

	@Override
	protected boolean isDisallowedInPeaceful() {
		return true;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isEvil() ? ModSounds.JAILER_ROAR : ModSounds.GHOST_HOSTILE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BLOCK_WOOD_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.GHOST_DEATH;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 200;
	}
}
