package com.phantompowers.entity;

import com.phantompowers.ModEntities;
import com.phantompowers.ModSounds;
import net.minecraft.block.Blocks;
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
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
 * The Collector: a gaunt ghost explorer in a pith helmet, with a backpack of glowing specimen jars, a giant net on a
 * pole and a lantern-trap, who hunts down rare creatures for his collection. A half-ghost is the rarest catch of all.
 * He throws sticky specimen jars, cages you in bars, yanks you towards him with his net and swipes with it up close.
 * At half health he plugs a stolen tablet into his gear and becomes Upgraded: armour plating, a shoulder rocket
 * launcher and blinking short-range teleports. In story mode he can't be destroyed: beat him until he's dazed, then
 * trap him. Round one of the story he's simply too well armoured to hurt.
 */
public class CollectorEntity extends AbstractGhostEntity implements Monster {
	public static final String STORY_TAG = "phantom_story_collector";
	/** Story round one: untouchable; he chases you until you reach safety. */
	public static final String CHASE_TAG = "phantom_story_collector_chase";
	private static final TrackedData<Boolean> UPGRADED = DataTracker.registerData(CollectorEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> DAZED = DataTracker.registerData(CollectorEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Integer> POSE = DataTracker.registerData(CollectorEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Slucker (stage 1)").formatted(Formatting.GOLD),
			BossBar.Color.YELLOW, BossBar.Style.NOTCHED_10);
	private int jarCooldown = 30, cageCooldown = 160, yankCooldown = 90, swipeCooldown = 20, rocketCooldown = 70, blinkCooldown = 120, repath, tauntCooldown = 60;
	private int throwAnim, swipeAnim;

	public CollectorEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 120;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0)
				.add(EntityAttributes.GENERIC_ARMOR, 6.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.7)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(UPGRADED, false);
		builder.add(DAZED, false);
		builder.add(POSE, 0);
	}

	public boolean upgraded() {
		return dataTracker.get(UPGRADED);
	}

	public boolean dazed() {
		return dataTracker.get(DAZED);
	}

	/** Client animation: 1..14 = throwing, 101..112 = net swipe. */
	public int pose() {
		return dataTracker.get(POSE);
	}

	public boolean storyMode() {
		return getCommandTags().contains(STORY_TAG);
	}

	public boolean chasing() {
		return getCommandTags().contains(CHASE_TAG);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(6, new DriftGoal(this));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 32.0f));
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
		} else if (upgraded() && random.nextInt(3) == 0) {
			getWorld().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + (random.nextDouble() - 0.5), getY() + 1.2 + random.nextDouble(), getZ() + (random.nextDouble() - 0.5), 0, 0, 0);
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.isOf(DamageTypes.GENERIC_KILL)) return super.damage(source, amount);
		if (dazed()) return false;
		if (chasing()) {
			// round one: his armour shrugs everything off
			if (source.getAttacker() instanceof ServerPlayerEntity p && age % 10 == 0)
				p.sendMessage(Text.literal("Your attacks bounce off his armour! Run for the Ghost Lab!").formatted(Formatting.RED), true);
			getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1f, 1.2f);
			return false;
		}
		if (storyMode()) {
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
		bossBar.setName(Text.literal("Slucker: DAZED (trap him with a Thermos!)").formatted(Formatting.YELLOW, Formatting.BOLD));
		bossBar.setColor(BossBar.Color.YELLOW);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.HOSTILE, 1.5f, 0.7f);
		say(w, "My armour... my beautiful collection...", Formatting.YELLOW, false);
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		bossBar.setPercent(getHealth() / getMaxHealth());
		if (!(getWorld() instanceof ServerWorld w)) return;
		if (throwAnim > 0) throwAnim--;
		if (swipeAnim > 0) swipeAnim--;
		dataTracker.set(POSE, swipeAnim > 0 ? 100 + swipeAnim : throwAnim);
		if (dazed()) {
			setTarget(null);
			setVelocity(getVelocity().multiply(0.5).add(0, Math.sin(age * 0.15) * 0.01, 0));
			return;
		}
		if (!upgraded() && !chasing() && getHealth() <= getMaxHealth() * 0.5f) upgrade(w);
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) return;
		getLookControl().lookAt(target, 30f, 30f);
		float speed = upgraded() ? 0.65f : 1f;
		double dist2 = squaredDistanceTo(target);
		if (--repath <= 0) {
			repath = 20;
			// stalk: circle at net's length, sometimes closing in
			boolean close = chasing() || random.nextInt(3) == 0;
			Vec3d away = getPos().subtract(target.getPos()).multiply(1, 0, 1);
			away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : away.normalize();
			away = away.rotateY((float) ((random.nextDouble() - 0.5) * 1.2));
			Vec3d spot = clampToHome(target.getPos().add(away.multiply(close ? 2.5 : 7.0)).add(0, 1.0, 0));
			getMoveControl().moveTo(spot.x, spot.y, spot.z, chasing() ? 0.9 : 1.1);
		}
		if (--swipeCooldown <= 0 && dist2 < 3.5 * 3.5) {
			swipeCooldown = (int) (28 * speed);
			netSwipe(w, target);
		}
		if (--jarCooldown <= 0 && dist2 < 24 * 24) {
			jarCooldown = (int) ((chasing() ? 50 : 36) * speed);
			throwJars(w, target, upgraded() ? 3 : 1);
		}
		if (!chasing() && --yankCooldown <= 0 && dist2 < 14 * 14 && dist2 > 4 * 4) {
			yankCooldown = (int) (120 * speed);
			yank(w, target);
		}
		if (!chasing() && --cageCooldown <= 0 && dist2 < 12 * 12) {
			cageCooldown = (int) (240 * speed);
			cage(w, target);
		}
		if (upgraded()) {
			if (--rocketCooldown <= 0 && dist2 < 30 * 30) {
				rocketCooldown = 70;
				rockets(w, target);
			}
			if (--blinkCooldown <= 0) {
				blinkCooldown = 110;
				blink(w, target);
			}
		}
		if (--tauntCooldown <= 0) {
			tauntCooldown = 240 + random.nextInt(200);
			String[] lines = chasing()
					? new String[]{"Run, little rarity! It only makes the hunt sweeter!", "A half-ghost! The jewel of my collection!", "Nowhere to hide, specimen!"}
					: upgraded()
					? new String[]{"Your friend's gadget makes a FINE upgrade!", "Scheduled capture in three... two...", "Upgraded and unstoppable!"}
					: new String[]{"Hold still! I only need you in one piece... mostly.", "Rare. Unique. MINE.", "You'll look splendid in a jar."};
			say(w, lines[random.nextInt(lines.length)], upgraded() ? Formatting.AQUA : Formatting.GOLD, true);
		}
	}

	/** Sticky specimen jars: they shatter into goo that slows you down. */
	public void throwJars(ServerWorld w, LivingEntity target, int count) {
		throwAnim = 14;
		for (int i = 0; i < count; i++) {
			FoodProjectileEntity jar = new FoodProjectileEntity(ModEntities.FOOD_PROJECTILE, this, w, upgraded() ? 5f : chasing() ? 2f : 4f);
			jar.setItem(new ItemStack(Items.GLASS_BOTTLE));
			Vec3d from = getEyePos().add(getRotationVec(1f).multiply(0.8));
			jar.setPosition(from.x, from.y - 0.3, from.z);
			Vec3d to = target.getPos().add(0, target.getHeight() * 0.6, 0).subtract(from);
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			Vec3d dir = new Vec3d(to.x, to.y + flat * 0.12, to.z).normalize().rotateY((float) ((i - (count - 1) / 2.0) * 0.2));
			jar.setVelocity(dir.x, dir.y, dir.z, 1.1f, 2f);
			w.spawnEntity(jar);
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_SPLASH_POTION_THROW, SoundCategory.HOSTILE, 1.4f, 0.8f);
	}

	/** A sweep of the big net. */
	public void netSwipe(ServerWorld w, LivingEntity target) {
		swipeAnim = 12;
		target.damage(getDamageSources().mobAttack(this), chasing() ? 3f : upgraded() ? 8f : 6f);
		Vec3d dir = target.getPos().subtract(getPos()).multiply(1, 0, 1);
		dir = dir.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : dir.normalize();
		target.takeKnockback(0.8, -dir.x, -dir.z);
		target.velocityModified = true;
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.2f, 0.8f);
	}

	/** Snags you in the net and hauls you in. */
	public void yank(ServerWorld w, LivingEntity target) {
		swipeAnim = 12;
		Vec3d pull = getPos().subtract(target.getPos()).normalize().multiply(1.6);
		target.setVelocity(pull.x, 0.5, pull.z);
		target.velocityModified = true;
		for (int i = 0; i < 12; i++) {
			Vec3d p = target.getPos().lerp(getPos(), i / 12.0).add(0, 1.2, 0);
			w.spawnParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.HOSTILE, 1.5f, 0.7f);
		say(w, "Gotcha!", upgraded() ? Formatting.AQUA : Formatting.GOLD, true);
	}

	/** Bars spring up around you, like a specimen cage. */
	public void cage(ServerWorld w, LivingEntity target) {
		BossMoves.cage(w, target, Blocks.IRON_BARS, 60);
		w.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.HOSTILE, 1.5f, 0.8f);
		say(w, "Into the cage with you!", upgraded() ? Formatting.AQUA : Formatting.GOLD, true);
	}

	/** Upgraded only: a volley from the shoulder launcher. */
	public void rockets(ServerWorld w, LivingEntity target) {
		Vec3d from = getEyePos().add(0, 0.3, 0);
		for (int i = 0; i < 2; i++) {
			WyvernFireballEntity r = new WyvernFireballEntity(ModEntities.WYVERN_FIREBALL, this, w, 5f);
			r.setPosition(from.x, from.y, from.z);
			Vec3d dir = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(from).normalize().rotateY((i - 0.5f) * 0.25f);
			r.setVelocity(dir.x, dir.y, dir.z, 1.2f, 1f);
			w.spawnEntity(r);
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 1.6f, 0.7f);
	}

	/** Upgraded only: the tablet glitches him to wherever "the schedule" says you'll be. */
	public void blink(ServerWorld w, LivingEntity target) {
		w.spawnParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.3);
		BossMoves.blinkBehind(w, this, target, 3.0);
		w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.2);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.2f, 1.4f);
	}

	/** Stage two. */
	private void upgrade(ServerWorld w) {
		dataTracker.set(UPGRADED, true);
		heal(getMaxHealth() * 0.1f);
		bossBar.setName(Text.literal("Slucker: UPGRADED (stage 2)").formatted(Formatting.AQUA, Formatting.BOLD));
		bossBar.setColor(BossBar.Color.BLUE);
		w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.2, getZ(), 80, 0.8, 1, 0.8, 0.3);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 2f, 1.4f);
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 48 * 48)) {
			p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(5, 50, 15));
			p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(Text.literal("STAGE 2").formatted(Formatting.RED, Formatting.BOLD)));
			p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(Text.literal("Slucker is Upgraded!").formatted(Formatting.AQUA)));
		}
		org.slf4j.LoggerFactory.getLogger("phantom").info("Slucker got Upgraded (stage two)");
		say(w, "Let's see what this little gadget can do... OH, how DELIGHTFUL!", Formatting.AQUA, false);
	}

	private void say(ServerWorld w, String line, Formatting color, boolean actionBar) {
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 48 * 48)) {
			p.sendMessage(Text.literal("The Collector: \"" + line + "\"").formatted(color, Formatting.BOLD), actionBar);
		}
	}

	/** Used by the CI test: try every move once. */
	public void testMoves(ServerWorld w, LivingEntity target) {
		throwJars(w, target, 2);
		netSwipe(w, target);
		yank(w, target);
		upgrade(w);
		rockets(w, target);
		blink(w, target);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Upgraded", upgraded());
		nbt.putBoolean("Dazed", dazed());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("Upgraded")) dataTracker.set(UPGRADED, true);
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
		return SoundEvents.ENTITY_PILLAGER_AMBIENT;
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
