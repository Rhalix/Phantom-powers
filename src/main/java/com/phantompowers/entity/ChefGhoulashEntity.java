package com.phantompowers.entity;

import com.phantompowers.ModEntities;
import com.phantompowers.ModSounds;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Chef Ghoulash: the ghost of a school cook who can't stand anyone changing his menu. He hurls school lunches
 * (meatloaf, drumsticks, pies; they splat and slow you down with gravy), smacks you with his giant ladle and
 * calls up kitchen spirits to help. At half health he pulls the whole cafeteria's meat around himself and
 * becomes a hulking Meat Monster: bigger, tougher, throwing food in barrages and slamming the floor.
 * In story mode he can't be killed: beat him down until he's dazed, then trap him in a Thermos.
 */
public class ChefGhoulashEntity extends AbstractGhostEntity implements Monster {
	public static final String STORY_TAG = "phantom_story_chef";
	private static final TrackedData<Boolean> MEAT = DataTracker.registerData(ChefGhoulashEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> DAZED = DataTracker.registerData(ChefGhoulashEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Integer> POSE = DataTracker.registerData(ChefGhoulashEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Chef Ghoulash (stage 1)").formatted(Formatting.GREEN),
			BossBar.Color.GREEN, BossBar.Style.NOTCHED_10);
	private int throwCooldown = 30, barrageCooldown = 160, smackCooldown = 20, helperCooldown = 120, slamCooldown = 140, repath, tauntCooldown;
	private int throwAnim, smackAnim;

	public ChefGhoulashEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 100;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 180.0)
				.add(EntityAttributes.GENERIC_ARMOR, 3.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(MEAT, false);
		builder.add(DAZED, false);
		builder.add(POSE, 0);
	}

	/** Meat Monster form (stage two). */
	public boolean meat() {
		return dataTracker.get(MEAT);
	}

	/** Beaten in story mode: floating there seeing stars, ready to be trapped. */
	public boolean dazed() {
		return dataTracker.get(DAZED);
	}

	/** Client animation: 1..14 = throwing, 101..112 = ladle smack. */
	public int pose() {
		return dataTracker.get(POSE);
	}

	public boolean storyMode() {
		return getCommandTags().contains(STORY_TAG);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (MEAT.equals(data)) {
			calculateDimensions();
			if (meat() && !isWalker()) makeWalker();   // the Meat Monster stomps around on its ham-hock legs
		}
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		EntityDimensions d = super.getBaseDimensions(pose);
		return meat() ? d.scaled(1.6f) : d;
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
		if (!getWorld().isClient) return;
		if (dazed()) {
			double a = age * 0.3;
			getWorld().addParticle(ParticleTypes.WAX_ON, getX() + Math.cos(a) * 0.6, getY() + getHeight() + 0.2, getZ() + Math.sin(a) * 0.6, 0, 0, 0);
		} else if (random.nextInt(meat() ? 3 : 8) == 0) {
			getWorld().addParticle(meat() ? ParticleTypes.FALLING_LAVA : ParticleTypes.SOUL_FIRE_FLAME, getX() + (random.nextDouble() - 0.5) * getWidth(),
					getY() + random.nextDouble() * getHeight(), getZ() + (random.nextDouble() - 0.5) * getWidth(), 0, 0.02, 0);
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (dazed() && !source.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL)) return false;
		if (storyMode() && !source.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL)) {
			// he can't be destroyed in the story: knocked down far enough, he gets dazed instead
			float floor = getMaxHealth() * 0.15f;
			if (getHealth() - amount <= floor) {
				boolean hit = super.damage(source, Math.max(0, getHealth() - floor));
				setHealth(Math.max(getHealth(), floor));
				if (getWorld() instanceof ServerWorld w) daze(w);
				return hit;
			}
		}
		return super.damage(source, amount);
	}

	private void daze(ServerWorld w) {
		if (dazed()) return;
		dataTracker.set(DAZED, true);
		setTarget(null);
		bossBar.setName(Text.literal("Chef Ghoulash: DAZED (trap him with a Thermos!)").formatted(Formatting.YELLOW, Formatting.BOLD));
		bossBar.setColor(BossBar.Color.YELLOW);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE, 1.5f, 0.6f);
		say(w, "Ooogh... my soufflé... it's fallen...", Formatting.YELLOW, false);
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		bossBar.setPercent(getHealth() / getMaxHealth());
		if (!(getWorld() instanceof ServerWorld w)) return;
		if (throwAnim > 0) throwAnim--;
		if (smackAnim > 0) smackAnim--;
		dataTracker.set(POSE, smackAnim > 0 ? 100 + smackAnim : throwAnim);
		if (dazed()) {
			setTarget(null);
			setVelocity(getVelocity().multiply(0.5).add(0, Math.sin(age * 0.15) * 0.01, 0));
			return;
		}
		if (!meat() && getHealth() <= getMaxHealth() * 0.5f) meatMonster(w);
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) return;
		getLookControl().lookAt(target, 30f, 30f);
		float speed = meat() ? 0.65f : 1f;
		double dist2 = squaredDistanceTo(target);
		// waddle around his "kitchen": close in for ladle smacks, back off to throw
		if (--repath <= 0) {
			repath = 25;
			boolean close = random.nextInt(3) == 0 || meat();
			Vec3d away = getPos().subtract(target.getPos()).multiply(1, 0, 1);
			away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : away.normalize();
			away = away.rotateY((float) ((random.nextDouble() - 0.5) * 1.4));
			Vec3d spot = clampToHome(target.getPos().add(away.multiply(close ? 2.0 : 7.0)).add(0, meat() ? 0 : close ? 0.2 : 1.5, 0));
			getMoveControl().moveTo(spot.x, spot.y, spot.z, close ? 1.1 : 0.9);
		}
		if (--smackCooldown <= 0 && dist2 < (meat() ? 4.5 * 4.5 : 3.3 * 3.3)) {
			smackCooldown = (int) (30 * speed);
			ladleSmack(w, target);
		}
		if (--throwCooldown <= 0 && dist2 < 26 * 26) {
			throwCooldown = (int) (34 * speed);
			throwFood(w, target, meat() ? 2 : 1);
		}
		if (--barrageCooldown <= 0 && dist2 < 22 * 22) {
			barrageCooldown = (int) (200 * speed);
			barrage(w);
		}
		if (--helperCooldown <= 0) {
			helperCooldown = 320;
			callHelpers(w);
		}
		if (meat() && --slamCooldown <= 0 && dist2 < 10 * 10) {
			slamCooldown = 150;
			slam(w);
		}
		if (--tauntCooldown <= 0) {
			tauntCooldown = 260 + random.nextInt(200);
			String[] lines = meat()
					? new String[]{"MEAT IS BACK ON THE MENU!", "Eat your protein!", "Seconds? THIRDS!"}
					: new String[]{"Who changed my MENU?!", "Salads? SALADS?! In MY cafeteria?!", "You'll eat what I serve, and you'll LIKE it!",
					"Nobody leaves until they've cleaned their plate!"};
			say(w, lines[random.nextInt(lines.length)], meat() ? Formatting.RED : Formatting.GREEN, true);
		}
	}

	/** Lobs school lunches at the target. */
	public void throwFood(ServerWorld w, LivingEntity target, int count) {
		throwAnim = 14;
		for (int i = 0; i < count; i++) {
			FoodProjectileEntity f = new FoodProjectileEntity(ModEntities.FOOD_PROJECTILE, this, w, meat() ? 6f : 4f);
			Vec3d from = getEyePos().add(getRotationVec(1f).multiply(0.8));
			f.setPosition(from.x, from.y - 0.3, from.z);
			Vec3d to = target.getPos().add(0, target.getHeight() * 0.6, 0).subtract(from);
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			Vec3d dir = new Vec3d(to.x, to.y + flat * 0.12, to.z).normalize().rotateY((float) ((i - (count - 1) / 2.0) * 0.18));
			f.setVelocity(dir.x, dir.y, dir.z, 1.1f, 2f);
			w.spawnEntity(f);
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.HOSTILE, 1.4f, 0.6f);
	}

	/** Food flies out in every direction. */
	public void barrage(ServerWorld w) {
		throwAnim = 14;
		int n = meat() ? 14 : 9;
		Vec3d from = getEyePos();
		for (int i = 0; i < n; i++) {
			double a = i * Math.PI * 2 / n + random.nextDouble() * 0.3;
			FoodProjectileEntity f = new FoodProjectileEntity(ModEntities.FOOD_PROJECTILE, this, w, 4f);
			f.setPosition(from.x, from.y - 0.3, from.z);
			f.setVelocity(Math.cos(a), 0.35, Math.sin(a), 0.75f, 4f);
			w.spawnEntity(f);
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 0.7f, 1.6f);
		say(w, "FOOD FIGHT!", meat() ? Formatting.RED : Formatting.GREEN, true);
	}

	/** A swing of the giant ladle. */
	public void ladleSmack(ServerWorld w, LivingEntity target) {
		smackAnim = 12;
		Vec3d dir = target.getPos().subtract(getPos()).multiply(1, 0, 1);
		dir = dir.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : dir.normalize();
		target.damage(getDamageSources().mobAttack(this), meat() ? 9f : 6f);
		target.takeKnockback(meat() ? 1.4 : 0.9, -dir.x, -dir.z);
		target.velocityModified = true;
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.7f, 1.8f);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.HOSTILE, 1.0f, 0.7f);
	}

	/** "Kitchen spirits, to me!" Two hostile ghosts come to help (never more than four around). */
	public void callHelpers(ServerWorld w) {
		int around = w.getEntitiesByClass(HostileGhostEntity.class, new Box(getBlockPos()).expand(24), e -> e.getCommandTags().contains("phantom_kitchen")).size();
		for (int i = around; i < Math.min(4, around + 2); i++) {
			HostileGhostEntity g = ModEntities.HOSTILE_GHOST.create(w);
			if (g == null) continue;
			double a = random.nextDouble() * Math.PI * 2;
			g.refreshPositionAndAngles(getX() + Math.cos(a) * 3, getY() + 0.5, getZ() + Math.sin(a) * 3, 0, 0);
			g.setCustomName(Text.literal("Kitchen Spirit"));
			g.addCommandTag("phantom_kitchen");
			if (storyMode()) g.addCommandTag("phantom_story");
			w.spawnEntity(g);
			w.spawnParticles(com.phantompowers.GhostPowers.GREEN, g.getX(), g.getY() + 1, g.getZ(), 25, 0.4, 0.6, 0.4, 0.05);
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1.2f, 1.3f);
		say(w, "Kitchen staff! Take their orders!", meat() ? Formatting.RED : Formatting.GREEN, true);
	}

	/** Meat Monster only: belly-flops onto the floor and sends a shockwave rolling out. */
	public void slam(ServerWorld w) {
		smackAnim = 12;
		Vec3d g = BossMoves.ground(w, getPos());
		BossMoves.groundWave(w, this, g, 9, 7f, 0xB04030);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 2f, 0.4f);
	}

	/** Stage two. */
	private void meatMonster(ServerWorld w) {
		// every scrap of food in the cafeteria comes flying in and piles onto him
		Vec3d mid = getPos().add(0, 1.2, 0);
		for (int i = 0; i < 40; i++) {
			double a = random.nextDouble() * Math.PI * 2, r = 4 + random.nextDouble() * 6;
			Vec3d from = mid.add(Math.cos(a) * r, random.nextDouble() * 3 - 1, Math.sin(a) * r);
			Vec3d v = mid.subtract(from).multiply(0.12);
			w.spawnParticles(new net.minecraft.particle.ItemStackParticleEffect(ParticleTypes.ITEM, new net.minecraft.item.ItemStack(
					i % 3 == 0 ? net.minecraft.item.Items.COOKED_BEEF : i % 3 == 1 ? net.minecraft.item.Items.COOKED_PORKCHOP : net.minecraft.item.Items.COOKED_CHICKEN)),
					from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
		}
		dataTracker.set(MEAT, true);
		heal(getMaxHealth() * 0.1f);
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 48 * 48)) {
			p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(5, 50, 15));
			p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(Text.literal("STAGE 2").formatted(Formatting.RED, Formatting.BOLD)));
			p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(Text.literal("The Meat Monster").formatted(Formatting.GOLD)));
		}
		bossBar.setName(Text.literal("Meat Monster (Chef Ghoulash, stage 2)").formatted(Formatting.RED, Formatting.BOLD));
		bossBar.setColor(BossBar.Color.RED);
		w.spawnParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.5, getZ(), 6, 1, 1, 1, 0);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 2f, 0.8f);
		org.slf4j.LoggerFactory.getLogger("phantom").info("Chef Ghoulash became the Meat Monster (stage two)");
		say(w, "You want to see MYSTERY MEAT?! I'll SHOW you mystery meat!", Formatting.RED, false);
	}

	private void say(ServerWorld w, String line, Formatting color, boolean actionBar) {
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 48 * 48)) {
			p.sendMessage(Text.literal("Chef Ghoulash: \"" + line + "\"").formatted(color, Formatting.BOLD), actionBar);
		}
	}

	/** Used by the CI test: try every move once. */
	public void testMoves(ServerWorld w, LivingEntity target) {
		throwFood(w, target, 2);
		barrage(w);
		ladleSmack(w, target);
		callHelpers(w);
		meatMonster(w);
		slam(w);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Meat", meat());
		nbt.putBoolean("Dazed", dazed());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("Meat")) dataTracker.set(MEAT, true);
		if (nbt.getBoolean("Dazed")) dataTracker.set(DAZED, true);
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
		return !storyMode();
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_VINDICATOR_AMBIENT;
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
