package com.phantompowers.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

/**
 * An Amity Park local: the story's characters (your friend Lena, Mr. Pemberton the teacher, Paige and the kids at the
 * dance) and anyone else around town. Each has their own look. They run away from big ghosts and can dance.
 */
public class TownspersonEntity extends PathAwareEntity {
	public static final int LENA = 0, PEMBERTON = 1, PAIGE = 2, MARCUS = 3, TIFFANY = 4, LEON = 5, JASMINE = 6, OWEN = 7;
	public static final String[] SKINS = {"lena", "pemberton", "paige", "marcus", "tiffany", "leon", "jasmine", "owen"};
	public static final String[] NAMES = {"Sam", "Mr. Lancer", "Princess Dorathea", "Marcus", "Tiffany", "Leon", "Jasmine", "Owen"};
	private static final TrackedData<Integer> VARIANT = DataTracker.registerData(TownspersonEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Boolean> DANCING = DataTracker.registerData(TownspersonEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> FORMAL_OUTFIT = DataTracker.registerData(TownspersonEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	public TownspersonEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VARIANT, 0);
		builder.add(DANCING, false);
		builder.add(FORMAL_OUTFIT, false);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new FleeEntityGoal<>(this, RageWyvernEntity.class, 14f, 0.6, 0.8));
		this.goalSelector.add(1, new FleeEntityGoal<>(this, ChefGhoulashEntity.class, 10f, 0.6, 0.8));
		this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.45));
		this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
		this.goalSelector.add(7, new LookAroundGoal(this));
	}

	public int variant() {
		return Math.floorMod(dataTracker.get(VARIANT), SKINS.length);
	}

	public void setVariant(int v) {
		dataTracker.set(VARIANT, Math.floorMod(v, SKINS.length));
	}

	public boolean isFemale() {
		int v = variant();
		return v == LENA || v == PAIGE || v == TIFFANY || v == JASMINE;
	}

	public boolean dancing() {
		return dataTracker.get(DANCING);
	}

	public void setDancing(boolean d) {
		dataTracker.set(DANCING, d);
		com.phantompowers.DanceWardrobeState.mark(this, d);
	}

	/** Dressed up for the dance (Sam and Paulina get their dance outfits). */
	public boolean isFormalOutfit() {
		return dataTracker.get(FORMAL_OUTFIT);
	}

	public void setFormalOutfit(boolean formal) {
		dataTracker.set(FORMAL_OUTFIT, formal);
	}

	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason reason, EntityData data) {
		setVariant(random.nextInt(SKINS.length));
		return super.initialize(world, difficulty, reason, data);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Variant", variant());
		nbt.putBoolean("Dancing", dancing());
		nbt.putBoolean("FormalDanceOutfit", isFormalOutfit());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		setVariant(nbt.getInt("Variant"));
		setDancing(nbt.getBoolean("Dancing"));
		setFormalOutfit(nbt.getBoolean("FormalDanceOutfit") | dancing());
	}
}
