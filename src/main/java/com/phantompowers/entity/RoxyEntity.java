package com.phantompowers.entity;

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
 * Roxy: a punk ghost biker who fights with blown kisses (glowing hearts that leave you lovestruck and sluggish)
 * and ear-splitting sonic shrieks. Below 40% she turns Heartbreaker: faster, and her kisses explode.
 * In the Biker Couple fight she starts out riding on the back of Rev's bike.
 */
public class RoxyEntity extends AbstractGhostEntity implements Monster {
	private static final TrackedData<Boolean> HEARTBREAK = DataTracker.registerData(RoxyEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Integer> POSE = DataTracker.registerData(RoxyEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Roxy").formatted(Formatting.LIGHT_PURPLE),
			BossBar.Color.PINK, BossBar.Style.NOTCHED_10);
	private int kissCooldown = 40, volleyCooldown = 140, shriekCooldown = 90, repath;
	/** Animation timers (synced): kiss pose and shriek pose. */
	private int kissAnim, shriekAnim;
	/** Rev, if this is the Biker Couple fight. */
	private java.util.UUID partner;
	private boolean vengeful;

	public void setCoupleMode(java.util.UUID rev) {
		this.partner = rev;
	}

	/** Standing on the back seat during Ride or Die (client animation). */
	public boolean standingOnBike() {
		return getVehicle() instanceof RevEntity rev && rev.rideOrDie();
	}

	public RoxyEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 140;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 210.0)
				.add(EntityAttributes.GENERIC_ARMOR, 4.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.65)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(HEARTBREAK, false);
		builder.add(POSE, 0);
	}

	public boolean heartbreak() {
		return dataTracker.get(HEARTBREAK);
	}

	/** Client animation: 1..16 = blowing a kiss, 101..120 = shrieking. */
	public int pose() {
		return dataTracker.get(POSE);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(6, new DriftGoal(this));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 24.0f));
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient && random.nextInt(6) == 0) {
			getWorld().addParticle(ParticleTypes.HEART, getX() + (random.nextDouble() - 0.5), getY() + getHeight() * 0.9,
					getZ() + (random.nextDouble() - 0.5), 0, 0.05, 0);
		}
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		bossBar.setPercent(getHealth() / getMaxHealth());
		if (!(getWorld() instanceof ServerWorld w)) return;
		if (kissAnim > 0) kissAnim--;
		if (shriekAnim > 0) shriekAnim--;
		dataTracker.set(POSE, shriekAnim > 0 ? 100 + shriekAnim : kissAnim);
		if (partner == null && getVehicle() instanceof RevEntity rev) partner = rev.getUuid();
		boolean together = getVehicle() instanceof RevEntity rev2 && rev2.rideOrDie();
		if (partner != null && !vengeful && !(w.getEntity(partner) instanceof RevEntity r && r.isAlive())) goVengeful(w);
		if (!together && !heartbreak() && getHealth() <= getMaxHealth() * 0.4f) heartbreaker(w);
		// in the couple fight she stays on the bike until Ride or Die; Rev handles the combo moves then
		if (together) return;
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) return;
		float speed = heartbreak() ? 0.65f : 1f;
		getLookControl().lookAt(target, 30f, 30f);
		// Keep a flirty distance: hover 7-10 blocks away, a little above
		if (!hasVehicle() && --repath <= 0) {
			repath = 20;
			Vec3d away = getPos().subtract(target.getPos()).multiply(1, 0, 1);
			away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : away.normalize();
			away = away.rotateY((float) ((random.nextDouble() - 0.5) * 1.6));
			Vec3d spot = clampToHome(target.getPos().add(away.multiply(8.5)).add(0, 2.5, 0));
			getMoveControl().moveTo(spot.x, spot.y, spot.z, 1.0);
		}
		if (--kissCooldown <= 0 && squaredDistanceTo(target) < 28 * 28) {
			kissCooldown = (int) (45 * speed);
			blowKiss(w, target, heartbreak() ? 2 : 1);
		}
		if (--volleyCooldown <= 0 && squaredDistanceTo(target) < 24 * 24) {
			volleyCooldown = (int) (170 * speed);
			kissVolley(w, target);
		}
		if (--shriekCooldown <= 0 && squaredDistanceTo(target) < 14 * 14) {
			shriekCooldown = (int) (100 * speed);
			shriek(w, target);
		}
	}

	/** Blows a kiss: a glowing heart that homes in on the target. */
	public void blowKiss(ServerWorld w, LivingEntity target, int count) {
		kissAnim = 16;
		for (int i = 0; i < count; i++) {
			KissEntity kiss = new KissEntity(w, this, heartbreak());
			Vec3d from = getEyePos().add(getRotationVec(1f).multiply(0.6));
			kiss.setPosition(from.x, from.y - 0.2, from.z);
			Vec3d to = target.getPos().add(0, target.getHeight() * 0.6, 0).subtract(from).normalize();
			to = to.rotateY((float) ((i - (count - 1) / 2.0) * 0.25));
			kiss.setVelocity(to.x, to.y, to.z, 0.75f, 0f);
			kiss.setHoming(target);
			w.spawnEntity(kiss);
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ALLAY_ITEM_THROWN, SoundCategory.HOSTILE, 1.4f, 1.6f);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 1.4f, 1.8f);
	}

	/** A fan of hearts in every direction. */
	public void kissVolley(ServerWorld w, LivingEntity target) {
		kissAnim = 16;
		int n = heartbreak() ? 12 : 8;
		Vec3d from = getEyePos();
		for (int i = 0; i < n; i++) {
			Vec3d dir = new Vec3d(Math.cos(i * Math.PI * 2 / n), -0.15, Math.sin(i * Math.PI * 2 / n));
			KissEntity kiss = new KissEntity(w, this, heartbreak());
			kiss.setPosition(from.x, from.y - 0.2, from.z);
			kiss.setVelocity(dir.x, dir.y, dir.z, 0.55f, 0f);
			w.spawnEntity(kiss);
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ALLAY_AMBIENT_WITH_ITEM, SoundCategory.HOSTILE, 1.6f, 1.4f);
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 48 * 48)) {
			p.sendMessage(Text.literal("Roxy: \"Kisses for everyone!\"").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), true);
		}
	}

	/** An ear-splitting shriek: a cone of sound waves that blasts you back. */
	public void shriek(ServerWorld w, LivingEntity target) {
		shriekAnim = 20;
		Vec3d from = getEyePos();
		Vec3d dir = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(from).normalize();
		BossMoves.soundWave(w, from, dir, 14f, 0xFF66CC);
		BossMoves.later(6, () -> {
			if (isAlive()) BossMoves.soundWave(w, getEyePos(), dir, 14f, 0xFF99DD);
		});
		BossMoves.coneHit(w, this, from, dir, 14, 0.8, getDamageSources().sonicBoom(this), heartbreak() ? 9f : 7f, 1.3,
				e -> !(e instanceof AbstractGhostEntity));
		w.playSound(null, getX(), getY(), getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.HOSTILE, 2.5f, 1.55f);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 1.0f, 1.6f);
	}

	/** Rev fell: Roxy goes Vengeful. */
	private void goVengeful(ServerWorld w) {
		vengeful = true;
		if (hasVehicle()) stopRiding();
		heal(getMaxHealth() * 0.25f);
		kissCooldown = 10;
		bossBar.setName(Text.literal("Roxy: VENGEFUL").formatted(Formatting.RED, Formatting.BOLD));
		bossBar.setColor(BossBar.Color.RED);
		if (!heartbreak()) dataTracker.set(HEARTBREAK, true);
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 80 * 80)) {
			p.sendMessage(Text.literal("Roxy: \"REV!! ...Nobody hurts my guy!\"").formatted(Formatting.RED, Formatting.BOLD), false);
		}
	}

	private void heartbreaker(ServerWorld w) {
		dataTracker.set(HEARTBREAK, true);
		bossBar.setName(Text.literal("Roxy: Heartbreaker").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
		bossBar.setColor(BossBar.Color.RED);
		heal(getMaxHealth() * 0.08f);
		if (hasVehicle()) stopRiding();
		w.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 1, getZ(), 30, 0.8, 1, 0.8, 0.1);
		org.slf4j.LoggerFactory.getLogger("phantom").info("Roxy became the Heartbreaker (stage two)");
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 80 * 80)) {
			p.sendMessage(Text.literal("Roxy: \"Okay. Now you've broken my heart.\"").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
		}
	}

	/** Used by the CI test: try every move once. */
	public void testMoves(ServerWorld w, LivingEntity target) {
		blowKiss(w, target, 2);
		kissVolley(w, target);
		shriek(w, target);
		heartbreaker(w);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Heartbreak", heartbreak());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("Heartbreak")) dataTracker.set(HEARTBREAK, true);
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
		return SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM;
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
