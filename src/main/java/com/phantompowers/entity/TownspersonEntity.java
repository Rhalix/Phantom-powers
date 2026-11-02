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
	/** Everyday townsfolk (variants 8 to 15): the people out shopping, eating and walking around Amity Park. */
	public static final int CITIZEN = 8, CITIZENS = 8;
	public static final String[] SKINS = {"lena", "pemberton", "paige", "marcus", "tiffany", "leon", "jasmine", "owen",
			"citizen_0", "citizen_1", "citizen_2", "citizen_3", "citizen_4", "citizen_5", "citizen_6", "citizen_7"};
	public static final String[] NAMES = {"Sam", "Mr. Lancer", "Princess Dorathea", "Marcus", "Tiffany", "Leon", "Jasmine", "Owen",
			"Townsperson", "Townsperson", "Townsperson", "Townsperson", "Townsperson", "Townsperson", "Townsperson", "Townsperson"};
	/** What a townsperson out in town is up to (story characters do nothing on their own). */
	public static final int IDLE = 0, WALKING = 1, SHOPPING = 2, EATING = 3;
	private int activity = IDLE, timer, nextBite = 40, stuck;
	private net.minecraft.util.math.BlockPos spot;
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
		this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.45) {
			@Override
			public boolean canStart() {
				return activity == IDLE && super.canStart();
			}
		});
		// shoppers browse the shelves near where they are
		this.goalSelector.add(5, new net.minecraft.entity.ai.goal.WanderAroundGoal(this, 0.35, 50) {
			@Override
			public boolean canStart() {
				return activity == SHOPPING && !hasVehicle() && super.canStart();
			}
		});
		this.goalSelector.add(5, new Stroll());
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
		return v == LENA || v == PAIGE || v == TIFFANY || v == JASMINE || (v >= CITIZEN && v % 2 == 1);
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
		int pick = random.nextInt(CITIZENS + 2);
		setVariant(pick == CITIZENS ? MARCUS : pick == CITIZENS + 1 ? JASMINE : CITIZEN + pick);
		return super.initialize(world, difficulty, reason, data);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Variant", variant());
		nbt.putBoolean("Dancing", dancing());
		nbt.putBoolean("FormalDanceOutfit", isFormalOutfit());
		if (activity != IDLE) {
			nbt.putInt("Activity", activity);
			nbt.putInt("ActivityTimer", timer);
			if (spot != null) nbt.putLong("ActivitySpot", spot.asLong());
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		setVariant(nbt.getInt("Variant"));
		setDancing(nbt.getBoolean("Dancing"));
		setFormalOutfit(nbt.getBoolean("FormalDanceOutfit") | dancing());
		if (nbt.contains("Activity")) {
			activity = nbt.getInt("Activity");
			timer = nbt.getInt("ActivityTimer");
			spot = nbt.contains("ActivitySpot") ? net.minecraft.util.math.BlockPos.fromLong(nbt.getLong("ActivitySpot")) : getBlockPos();
			if (activity == SHOPPING) setPositionTarget(spot, 5);
		}
	}

	public int activity() {
		return activity;
	}

	private static final String[] FOOD = {"cooked_beef", "bread", "baked_potato", "cooked_chicken", "cookie", "apple", "pumpkin_pie", "melon_slice", "cooked_salmon", "mushroom_stew"};
	private static final String[] GOODS = {"bread", "apple", "carrot", "potato", "cookie", "book", "sugar", "milk_bucket", "melon_slice", "paper", "glow_berries", "honey_bottle"};

	private static net.minecraft.item.ItemStack item(String name) {
		return new net.minecraft.item.ItemStack(net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.ofVanilla(name)));
	}

	/** Send this townsperson off to do something: walk around town, shop near {@code at}, or eat at the seat at {@code at}. */
	public void startActivity(int what, net.minecraft.util.math.BlockPos at) {
		activity = what;
		spot = at;
		stuck = 0;
		timer = switch (what) {
			case EATING -> 1200 + random.nextInt(2400);
			case SHOPPING -> 900 + random.nextInt(1800);
			default -> 0;
		};
		if (what == SHOPPING) setPositionTarget(at, 5);
		else clearPositionTarget();
		setStackInHand(net.minecraft.util.Hand.MAIN_HAND, switch (what) {
			case EATING -> item(FOOD[random.nextInt(FOOD.length)]);
			case SHOPPING -> item(GOODS[random.nextInt(GOODS.length)]);
			default -> net.minecraft.item.ItemStack.EMPTY;
		});
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient || activity == IDLE || !(getWorld() instanceof net.minecraft.server.world.ServerWorld sw)) return;
		if (com.phantompowers.GhostFlags.HOSTS.contains(getUuid())) return;   // someone is overshadowing them
		if (timer > 0 && --timer == 0) {
			// done here: back out onto the street
			stopRiding();
			startActivity(WALKING, getBlockPos());
			return;
		}
		if (activity == EATING) {
			if (!hasVehicle()) {
				if (spot == null) { startActivity(WALKING, getBlockPos()); return; }
				if (squaredDistanceTo(spot.toCenterPos()) < 2.6) {
					if (!com.phantompowers.FurnitureBlock.sit(sw, spot, this)) startActivity(WALKING, getBlockPos());
				} else if (getNavigation().isIdle()) {
					getNavigation().startMovingTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.4);
					if (++stuck > 12) startActivity(WALKING, getBlockPos());
				}
				return;
			}
			if (--nextBite <= 0) {
				nextBite = 50 + random.nextInt(90);
				net.minecraft.item.ItemStack food = getMainHandStack();
				if (food.isEmpty()) return;
				swingHand(net.minecraft.util.Hand.MAIN_HAND);
				playSound(net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EAT, 0.4f, 0.9f + random.nextFloat() * 0.2f);
				net.minecraft.util.math.Vec3d mouth = getEyePos().add(getRotationVector().multiply(0.3)).add(0, -0.15, 0);
				sw.spawnParticles(new net.minecraft.particle.ItemStackParticleEffect(net.minecraft.particle.ParticleTypes.ITEM, food),
						mouth.x, mouth.y, mouth.z, 5, 0.08, 0.05, 0.08, 0.04);
			}
		} else if (activity == SHOPPING) {
			if (random.nextInt(160) == 0) swingHand(net.minecraft.util.Hand.MAIN_HAND);   // taking something off the shelf
		}
	}

	/** Walkers stroll along the pavements and paths from one spot to another, stopping now and then. */
	private final class Stroll extends net.minecraft.entity.ai.goal.Goal {
		Stroll() {
			setControls(java.util.EnumSet.of(Control.MOVE));
		}

		@Override
		public boolean canStart() {
			if (activity != WALKING || hasVehicle() || random.nextInt(30) != 0) return false;
			net.minecraft.util.math.BlockPos to = pickPath();
			if (to == null) return false;
			return getNavigation().startMovingTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 0.4);
		}

		@Override
		public boolean shouldContinue() {
			return activity == WALKING && !hasVehicle() && !getNavigation().isIdle();
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		private net.minecraft.util.math.BlockPos pickPath() {
			World w = getWorld();
			for (int i = 0; i < 16; i++) {
				int dx = random.nextInt(41) - 20, dz = random.nextInt(41) - 20;
				if (dx * dx + dz * dz < 36) continue;
				for (int dy : new int[]{0, 1, -1}) {
					net.minecraft.util.math.BlockPos q = getBlockPos().add(dx, dy, dz);
					if (!w.isChunkLoaded(q)) break;
					net.minecraft.block.BlockState floor = w.getBlockState(q.down());
					if ((floor.isOf(net.minecraft.block.Blocks.SMOOTH_STONE) || floor.isOf(net.minecraft.block.Blocks.GRAVEL))
							&& w.getBlockState(q).getCollisionShape(w, q).isEmpty() && w.getBlockState(q.up()).isAir()) return q;
				}
			}
			return null;
		}
	}
}
