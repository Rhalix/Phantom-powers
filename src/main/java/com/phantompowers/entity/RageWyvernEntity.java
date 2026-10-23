package com.phantompowers.entity;

import com.phantompowers.ModEntities;
import com.phantompowers.ModItems;
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
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The Rage Wyvern: what the Rage Amulet turns its wearer into when they lose their temper. A spectral wyvern of
 * teal and violet scales that circles overhead breathing ghost-fire, spitting fireballs, diving at you and lashing
 * its tail. At half health it goes Berserk: faster, hotter, and it roars to slow you down. In story mode it can't be
 * killed: beat it until it's dazed, then right-click it to pull the amulet off and turn it back into a person.
 */
public class RageWyvernEntity extends AbstractGhostEntity implements Monster {
	public static final String STORY_TAG = "phantom_story_wyvern";
	private static final TrackedData<Boolean> BERSERK = DataTracker.registerData(RageWyvernEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> DAZED = DataTracker.registerData(RageWyvernEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Integer> POSE = DataTracker.registerData(RageWyvernEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Princess Dorathea (stage 1)").formatted(Formatting.DARK_AQUA),
			BossBar.Color.BLUE, BossBar.Style.NOTCHED_10);
	private int breathCooldown = 60, fireballCooldown = 40, diveCooldown = 160, swipeCooldown = 30, repath;
	private int breathTicks, diveTicks, swipeAnim, roarAnim;
	private double orbit;
	private Vec3d breathDir = Vec3d.ZERO;
	/** Who the amulet turned into this wyvern (story mode), shown when it's pulled off. */
	private String wearer = "";
	private int wearerLook = TownspersonEntity.PAIGE;

	public RageWyvernEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 120;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 220.0)
				.add(EntityAttributes.GENERIC_ARMOR, 5.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.8)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(BERSERK, false);
		builder.add(DAZED, false);
		builder.add(POSE, 0);
	}

	public boolean berserk() {
		return dataTracker.get(BERSERK);
	}

	public boolean dazed() {
		return dataTracker.get(DAZED);
	}

	/** Client animation: 1..40 = breathing fire, 101..112 = tail swipe, 201..230 = roar, 301.. = diving. */
	public int pose() {
		return dataTracker.get(POSE);
	}

	public boolean storyMode() {
		return getCommandTags().contains(STORY_TAG);
	}

	@Override
	public void setCustomName(Text name) {
		super.setCustomName(name);
		if (name != null && bossBar != null && !berserk() && !dazed()) bossBar.setName(Text.literal(name.getString() + " (stage 1)").formatted(Formatting.DARK_AQUA));
	}

	public void setWearer(String name) {
		setWearer(name, TownspersonEntity.PAIGE);
	}

	public void setWearer(String name, int look) {
		this.wearer = name;
		this.wearerLook = look;
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 32.0f));
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			if (dazed()) {
				double a = age * 0.3;
				getWorld().addParticle(ParticleTypes.WAX_ON, getX() + Math.cos(a) * 0.9, getY() + getHeight() + 0.3, getZ() + Math.sin(a) * 0.9, 0, 0, 0);
			} else if (random.nextInt(berserk() ? 2 : 5) == 0) {
				getWorld().addParticle(berserk() ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME, getX() + (random.nextDouble() - 0.5) * getWidth(),
						getY() + random.nextDouble() * getHeight(), getZ() + (random.nextDouble() - 0.5) * getWidth(), 0, 0.01, 0);
			}
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (dazed() && !source.isOf(DamageTypes.GENERIC_KILL)) return false;
		if (storyMode() && !source.isOf(DamageTypes.GENERIC_KILL)) {
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
		breathTicks = 0;
		diveTicks = 0;
		bossBar.setName(Text.literal(getName().getString() + ": DAZED (right-click it to pull off the amulet!)").formatted(Formatting.YELLOW, Formatting.BOLD));
		bossBar.setColor(BossBar.Color.YELLOW);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 1.6f, 1.3f);
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 48 * 48)) {
			p.sendMessage(Text.literal("The wyvern is dazed! The amulet on its chest is flickering. Right-click it to pull it off!").formatted(Formatting.YELLOW), false);
		}
	}

	/** Right-click a dazed wyvern: rip the Rage Amulet off its chest, and whoever was inside comes back. */
	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (!dazed()) return super.interactMob(player, hand);
		if (getWorld() instanceof ServerWorld w && player instanceof ServerPlayerEntity sp) pullAmulet(w, sp);
		return ActionResult.SUCCESS;
	}

	public void pullAmulet(ServerWorld w, ServerPlayerEntity by) {
		if (isRemoved()) return;
		w.spawnParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
		w.spawnParticles(com.phantompowers.GhostPowers.GREEN, getX(), getY() + 1, getZ(), 120, 1.4, 1.2, 1.4, 0.15);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.HOSTILE, 2f, 0.6f);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1.2f, 1.8f);
		if (!wearer.isEmpty()) {
			TownspersonEntity v = com.phantompowers.ModEntities.TOWNSPERSON.create(w);
			if (v != null) {
				v.setVariant(wearerLook);
				Vec3d g = BossMoves.ground(w, getPos());
				v.refreshPositionAndAngles(g.x, g.y, g.z, getYaw(), 0);
				v.setCustomName(Text.literal(wearer));
				v.setPersistent();
				v.addCommandTag("phantom_dance");
				w.spawnEntity(v);
				by.sendMessage(Text.literal(wearer + ": \"Ugh... my head. What happened? ...Did I just ruin the whole dance?\"").formatted(Formatting.LIGHT_PURPLE), false);
			}
		}
		ItemStack amulet = new ItemStack(ModItems.RAGE_AMULET);
		if (!by.giveItemStack(amulet)) by.dropItem(amulet, false);
		by.sendMessage(Text.literal("You pulled off the Rage Amulet!").formatted(Formatting.GOLD, Formatting.BOLD), true);
		if (storyMode()) com.phantompowers.Story.onAmuletPulled(w, by, this);
		discard();
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		bossBar.setPercent(getHealth() / getMaxHealth());
		if (!(getWorld() instanceof ServerWorld w)) return;
		if (swipeAnim > 0) swipeAnim--;
		if (roarAnim > 0) roarAnim--;
		dataTracker.set(POSE, breathTicks > 0 ? breathTicks : swipeAnim > 0 ? 100 + swipeAnim : roarAnim > 0 ? 200 + roarAnim : diveTicks > 0 ? 300 + diveTicks : 0);
		if (dazed()) {
			// sinks down and hovers just off the ground, wobbling
			setTarget(null);
			Vec3d g = BossMoves.ground(w, getPos());
			double dy = (g.y + 0.6) - getY();
			setVelocity(getVelocity().multiply(0.6).add(0, dy * 0.05, 0));
			return;
		}
		if (!berserk() && getHealth() <= getMaxHealth() * 0.5f) goBerserk(w);
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) return;
		getLookControl().lookAt(target, 30f, 30f);
		float speed = berserk() ? 0.65f : 1f;
		double dist2 = squaredDistanceTo(target);
		if (breathTicks > 0) {
			// hover in place and sweep the fire across the target
			breathTicks--;
			setVelocity(getVelocity().multiply(0.7));
			Vec3d mouth = getEyePos().add(getRotationVec(1f).multiply(1.8));
			Vec3d want = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(mouth).normalize();
			breathDir = breathDir.lengthSquared() < 0.01 ? want : breathDir.multiply(0.8).add(want.multiply(0.2)).normalize();
			for (int i = 0; i < 6; i++) {
				Vec3d v = breathDir.multiply(0.6 + random.nextDouble() * 0.5).add((random.nextDouble() - 0.5) * 0.25, (random.nextDouble() - 0.5) * 0.25, (random.nextDouble() - 0.5) * 0.25);
				w.spawnParticles(berserk() ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME, mouth.x, mouth.y, mouth.z, 0, v.x, v.y, v.z, 1.0);
			}
			if (breathTicks % 5 == 0) {
				BossMoves.coneHit(w, this, mouth, breathDir, berserk() ? 13 : 10, 0.86, getDamageSources().mobAttack(this), berserk() ? 4f : 3f, 0.2,
						e -> !(e instanceof AbstractGhostEntity) && !(e instanceof VillagerEntity) && !(e instanceof TownspersonEntity));
				for (Entity e : w.getOtherEntities(this, getBoundingBox().stretch(breathDir.multiply(12)).expand(1.5),
						e -> e instanceof LivingEntity && !(e instanceof AbstractGhostEntity) && !(e instanceof VillagerEntity) && !(e instanceof TownspersonEntity))) {
					Vec3d to = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(mouth);
					if (to.lengthSquared() < 13 * 13 && to.normalize().dotProduct(breathDir) > 0.86) e.setOnFireFor(4);
				}
			}
			if (breathTicks % 8 == 0) w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_BURN, SoundCategory.HOSTILE, 1.6f, 0.6f);
			return;
		}
		if (diveTicks > 0) {
			// swoop straight at the target, then pull back up
			diveTicks--;
			Vec3d to = target.getPos().add(0, 1, 0).subtract(getPos());
			if (diveTicks > 8) setVelocity(to.normalize().multiply(berserk() ? 1.25 : 1.0));
			else setVelocity(getVelocity().multiply(0.8).add(0, 0.12, 0));
			for (Entity e : w.getOtherEntities(this, getBoundingBox().expand(0.6), e -> e instanceof LivingEntity && !(e instanceof AbstractGhostEntity) && !(e instanceof VillagerEntity) && !(e instanceof TownspersonEntity))) {
				if (e.damage(getDamageSources().mobAttack(this), berserk() ? 11f : 8f)) {
					Vec3d push = e.getPos().subtract(getPos()).multiply(1, 0, 1).normalize();
					e.addVelocity(push.x * 1.4, 0.7, push.z * 1.4);
					e.velocityModified = true;
					diveTicks = Math.min(diveTicks, 8);
				}
			}
			return;
		}
		// circle overhead
		if (--repath <= 0) {
			repath = 12;
			orbit += (berserk() ? 0.55 : 0.4) * (random.nextBoolean() || orbit == 0 ? 1 : 1);
			double r = 8.5 + Math.sin(age * 0.03) * 1.5;
			Vec3d spot = clampToHome(target.getPos().add(Math.cos(orbit) * r, 5.5 + Math.sin(age * 0.05), Math.sin(orbit) * r));
			getMoveControl().moveTo(spot.x, spot.y, spot.z, 1.2);
		}
		if (--swipeCooldown <= 0 && dist2 < 4.5 * 4.5) {
			swipeCooldown = (int) (40 * speed);
			tailSwipe(w);
		}
		if (--breathCooldown <= 0 && dist2 < 16 * 16) {
			breathCooldown = (int) (150 * speed);
			breathe(w);
			return;
		}
		if (--fireballCooldown <= 0 && dist2 < 32 * 32) {
			fireballCooldown = (int) (45 * speed);
			fireballs(w, target, berserk() ? 3 : 1);
		}
		if (--diveCooldown <= 0) {
			diveCooldown = (int) (190 * speed);
			dive(w);
		}
	}

	/** A long, sweeping stream of ghost-fire. */
	public void breathe(ServerWorld w) {
		breathTicks = 40;
		breathDir = Vec3d.ZERO;
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.HOSTILE, 2f, 0.8f);
	}

	public void fireballs(ServerWorld w, LivingEntity target, int count) {
		Vec3d from = getEyePos().add(getRotationVec(1f).multiply(1.8));
		for (int i = 0; i < count; i++) {
			WyvernFireballEntity f = new WyvernFireballEntity(ModEntities.WYVERN_FIREBALL, this, w, berserk() ? 6f : 5f);
			f.setPosition(from.x, from.y, from.z);
			Vec3d dir = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(from).normalize().rotateY((float) ((i - (count - 1) / 2.0) * 0.22));
			f.setVelocity(dir.x, dir.y, dir.z, 1.1f, 1f);
			w.spawnEntity(f);
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.6f, 0.5f);
	}

	public void dive(ServerWorld w) {
		diveTicks = 30;
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PHANTOM_SWOOP, SoundCategory.HOSTILE, 2f, 0.6f);
	}

	/** Spins and lashes everything nearby with its tail. */
	public void tailSwipe(ServerWorld w) {
		swipeAnim = 12;
		for (Entity e : w.getOtherEntities(this, getBoundingBox().expand(3.5), e -> e instanceof LivingEntity && !(e instanceof AbstractGhostEntity) && !(e instanceof VillagerEntity) && !(e instanceof TownspersonEntity))) {
			e.damage(getDamageSources().mobAttack(this), berserk() ? 9f : 7f);
			Vec3d push = e.getPos().subtract(getPos()).multiply(1, 0, 1);
			push = push.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : push.normalize();
			e.addVelocity(push.x * 1.6, 0.5, push.z * 1.6);
			e.velocityModified = true;
		}
		w.spawnParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 0.8, getZ(), 6, 2, 0.3, 2, 0);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.6f, 0.5f);
	}

	/** Stage two: Berserk. */
	private void goBerserk(ServerWorld w) {
		dataTracker.set(BERSERK, true);
		roarAnim = 30;
		heal(getMaxHealth() * 0.1f);
		bossBar.setName(Text.literal(getName().getString() + ": BERSERK (stage 2)").formatted(Formatting.RED, Formatting.BOLD));
		bossBar.setColor(BossBar.Color.RED);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2.5f, 0.9f);
		w.spawnParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 80, 1.5, 1, 1.5, 0.12);
		for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 48 * 48)) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), this);
			p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(5, 50, 15));
			p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(Text.literal("STAGE 2").formatted(Formatting.RED, Formatting.BOLD)));
			p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(Text.literal("The wyvern goes Berserk!").formatted(Formatting.GOLD)));
		}
		org.slf4j.LoggerFactory.getLogger("phantom").info("The Princess Dorathea went Berserk (stage two)");
	}

	@Override
	protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
		super.dropEquipment(world, source, causedByPlayer);
		dropStack(new ItemStack(ModItems.RAGE_AMULET));
	}

	/** Used by the CI test: try every move once. */
	public void testMoves(ServerWorld w, LivingEntity target) {
		breathe(w);
		fireballs(w, target, 3);
		dive(w);
		tailSwipe(w);
		goBerserk(w);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Berserk", berserk());
		nbt.putBoolean("Dazed", dazed());
		nbt.putString("Wearer", wearer);
		nbt.putInt("WearerLook", wearerLook);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.getBoolean("Berserk")) dataTracker.set(BERSERK, true);
		if (nbt.getBoolean("Dazed")) dataTracker.set(DAZED, true);
		wearer = nbt.getString("Wearer");
		if (nbt.contains("WearerLook")) wearerLook = nbt.getInt("WearerLook");
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
		return SoundEvents.ENTITY_ENDER_DRAGON_AMBIENT;
	}

	@Override
	protected float getSoundVolume() {
		return 0.8f;
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
