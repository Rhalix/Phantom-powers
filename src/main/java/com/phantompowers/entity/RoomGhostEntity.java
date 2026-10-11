package com.phantompowers.entity;

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
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * The ghost that haunts a Ghost Zone room. Each room theme has its own ghost with its own look,
 * temper and trick. Variant numbers match the room themes in RoomBuilder.
 */
public class RoomGhostEntity extends AbstractGhostEntity {
	public static final String[] NAMES = {"Librarian Specter", "Frost Wraith", "Cinder Spirit", "Bloom Sprite",
			"Miser Ghost", "Shackled Soul", "Gearwork Ghost", "Box Hoarder", "Cloud Wisp", "Drowned Phantom", "Spirit Monk"};
	/** Which variants are hostile. The rest are friendly. */
	public static final boolean[] HOSTILE = {false, true, true, false, true, true, true, true, false, false, false};
	private static final String[] GREETINGS = {
			"Shhh! ...Oh, a reader. Take this, and keep your voice down.",
			"", "", "The garden is happy you came! Have something sweet.", "", "", "", "",
			"Floaty floaty... here, you'll need this up here!",
			"Blub... you can breathe again with this.",
			"Peace, traveler. May fortune walk beside you."};

	private static final TrackedData<Integer> VARIANT = DataTracker.registerData(RoomGhostEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private boolean gifted;

	public RoomGhostEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.55)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VARIANT, 0);
	}

	public int getVariant() {
		return Math.floorMod(dataTracker.get(VARIANT), NAMES.length);
	}

	public void setVariant(int v) {
		dataTracker.set(VARIANT, Math.floorMod(v, NAMES.length));
	}

	public boolean isHostile() {
		return HOSTILE[getVariant()];
	}

	@Override
	protected Text getDefaultName() {
		return Text.literal(NAMES[getVariant()]);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new HostileGhostEntity.ChargeGoal(this) {
			@Override
			public boolean canStart() {
				return isHostile() && super.canStart();
			}
		});
		this.goalSelector.add(3, new FriendlyGhostEntity.FollowPlayerGoal(this) {
			@Override
			public boolean canStart() {
				return !isHostile() && super.canStart();
			}
		});
		this.goalSelector.add(6, new DriftGoal(this));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, p -> isHostile()));
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (!hit || !(target instanceof LivingEntity le)) return hit;
		switch (getVariant()) {
			case 1 -> { le.setFrozenTicks(Math.min(le.getMinFreezeDamageTicks() + 60, le.getFrozenTicks() + 80)); le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), this); }
			case 2 -> le.setOnFireFor(4);
			case 4 -> { if (le instanceof PlayerEntity p && p.experienceLevel > 0) p.addExperience(-8); }
			case 5 -> le.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0), this);
			case 6 -> le.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 100, 1), this);
			case 7 -> { le.addVelocity(0, 0.6, 0); le.velocityModified = true; }
			default -> { }
		}
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld() instanceof ServerWorld w && age % 100 == 0 && getVariant() == 3) {
			// The Bloom Sprite heals players near it
			for (PlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(this) < 64)) {
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 0), this);
			}
			w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1, getZ(), 10, 1.5, 0.5, 1.5, 0);
		}
		if (getWorld().isClient && age % 4 == 0) {
			switch (getVariant()) {
				case 1 -> getWorld().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY() + 1, getZ(), 0, -0.02, 0);
				case 2 -> getWorld().addParticle(ParticleTypes.SMALL_FLAME, getX(), getY() + 1.6, getZ(), 0, 0.02, 0);
				case 9 -> getWorld().addParticle(ParticleTypes.BUBBLE_POP, getX(), getY() + 1.2, getZ(), 0, 0.03, 0);
				case 10 -> getWorld().addParticle(ParticleTypes.END_ROD, getX(), getY() + 2.1, getZ(), 0, 0.01, 0);
				default -> { }
			}
		}
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (isHostile() || hand != Hand.MAIN_HAND) return ActionResult.PASS;
		if (getWorld().isClient) return ActionResult.SUCCESS;
		if (gifted) {
			player.sendMessage(Text.literal(NAMES[getVariant()] + ": I've nothing more to give... but thank you for visiting.").formatted(Formatting.AQUA), false);
			return ActionResult.SUCCESS;
		}
		gifted = true;
		player.sendMessage(Text.literal(NAMES[getVariant()] + ": " + GREETINGS[getVariant()]).formatted(Formatting.AQUA), false);
		ItemStack gift = switch (getVariant()) {
			case 0 -> new ItemStack(Items.EXPERIENCE_BOTTLE, 5);
			case 3 -> new ItemStack(Items.GOLDEN_CARROT, 6);
			case 8 -> new ItemStack(Items.FEATHER, 8);
			case 9 -> new ItemStack(Items.HEART_OF_THE_SEA);
			case 10 -> new ItemStack(Items.AMETHYST_SHARD, 6);
			default -> new ItemStack(ModItems.ECTOPLASM, 3);
		};
		if (getVariant() == 8) player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 1200, 0));
		if (getVariant() == 9) player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 2400, 0));
		if (getVariant() == 10) player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 6000, 0));
		if (!player.giveItemStack(gift)) player.dropItem(gift, false);
		if (getWorld() instanceof ServerWorld sw) sw.spawnParticles(ParticleTypes.HEART, getX(), getY() + getHeight(), getZ(), 5, 0.4, 0.3, 0.4, 0);
		playSound(ModSounds.GHOST_FRIENDLY, 1.0f, 1.3f);
		return ActionResult.SUCCESS;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Variant", getVariant());
		nbt.putBoolean("Gifted", gifted);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		setVariant(nbt.getInt("Variant"));
		gifted = nbt.getBoolean("Gifted");
	}

	@Override
	protected void dropLoot(DamageSource source, boolean causedByPlayer) {
		super.dropLoot(source, causedByPlayer);
		if (random.nextFloat() < 0.3f) {
			dropStack(new ItemStack(isHostile() ? ModItems.ESSENCES[0] : ModItems.ESSENCES[1]));
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isHostile() ? ModSounds.GHOST_HOSTILE : ModSounds.GHOST_FRIENDLY;
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
