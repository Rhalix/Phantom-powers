package com.phantompowers.entity;

import com.phantompowers.HunterSuit;
import com.phantompowers.ModItems;
import com.phantompowers.PortalLogic;
import com.phantompowers.network.BeamPayload;
import com.phantompowers.network.SkimmerRadarPayload;
import com.phantompowers.world.GhostZoneLife;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * The Ecto-Skimmer: a hover-shuttle for exploring the Ghost Zone. Right-click to climb in (the canopy swings up,
 * you vault into the cockpit, it seals and the engines spool up). Fly where you look, jump to climb, sprint to
 * boost, right-click to fire the nose cannon, sneak to climb out. Its scanner points the way to ghost lairs,
 * and Skimmer upgrades bolt on new gear.
 */
public class SkimmerEntity extends Entity {
	public static final int UP_CANNONS = 1, UP_THRUSTERS = 2, UP_SHIELD = 4, UP_SCANNER = 8, UP_PHASE = 16;
	public static final String[] UP_NAMES = {"Plasma Wing Cannons", "Ecto-Thrusters", "Ecto-Shield", "Deep Scanner", "Phase Drive"};
	public static final int IDLE = 0, BOARDING = 1, FLYING = 2, EXITING = 3;
	/** Boarding timeline (ticks): canopy opens, you vault in, canopy closes, engines spool up. */
	public static final int B_OPEN = 10, B_GRAB = 6, B_SEATED = 22, B_CLOSED = 32, B_READY = 38;
	public static final int E_OPEN = 6, E_HOP = 6, E_CLOSE_START = 16, E_DONE = 26;
	public static final float MAX_HEALTH = 80f;

	private static final TrackedData<Integer> UPGRADES = DataTracker.registerData(SkimmerEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> STATE = DataTracker.registerData(SkimmerEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Long> STATE_START = DataTracker.registerData(SkimmerEntity.class, TrackedDataHandlerRegistry.LONG);
	private static final TrackedData<Float> HEALTH = DataTracker.registerData(SkimmerEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Boolean> PHASING = DataTracker.registerData(SkimmerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	/** Client input from the pilot (set by PhantomClient each tick). */
	public static volatile boolean clientBoost, clientUp, clientDown;
	/** Set by the client: sends a skill/action packet to the server. */
	public static java.util.function.IntConsumer sendAction = a -> {};

	private int fireCooldown, radarTimer, ramCooldown, humTimer;
	private ServerPlayerEntity boarding, hopper;
	private float boostLevel, roll, prevRoll, thrust, prevThrust;
	private boolean boosting;

	public SkimmerEntity(EntityType<? extends SkimmerEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder b) {
		b.add(UPGRADES, 0);
		b.add(STATE, IDLE);
		b.add(STATE_START, 0L);
		b.add(HEALTH, MAX_HEALTH);
		b.add(PHASING, false);
	}

	// ------------------------------------------------------------------ data

	public int upgrades() {
		return dataTracker.get(UPGRADES);
	}

	public boolean has(int up) {
		return (upgrades() & up) != 0;
	}

	public void setUpgrades(int u) {
		dataTracker.set(UPGRADES, u);
	}

	public int state() {
		return dataTracker.get(STATE);
	}

	public float health() {
		return dataTracker.get(HEALTH);
	}

	public void setHealth(float h) {
		dataTracker.set(HEALTH, MathHelper.clamp(h, 0, MAX_HEALTH));
	}

	public boolean phasing() {
		return dataTracker.get(PHASING);
	}

	private void setState(int s) {
		dataTracker.set(STATE, s);
		dataTracker.set(STATE_START, getWorld().getTime());
	}

	/** Ticks (with partial) since the current state began. */
	public float stateTime(float partial) {
		return Math.max(0, getWorld().getTime() - dataTracker.get(STATE_START)) + partial;
	}

	/** 0 closed .. 1 fully open. */
	public float canopyOpen(float partial) {
		float t = stateTime(partial);
		return switch (state()) {
			case BOARDING -> t < B_OPEN ? ease(t / B_OPEN) : t < B_SEATED ? 1f : t < B_CLOSED ? 1f - ease((t - B_SEATED) / (B_CLOSED - B_SEATED)) : 0f;
			case EXITING -> t < E_OPEN ? ease(t / E_OPEN) : t < E_CLOSE_START ? 1f : t < E_DONE ? 1f - ease((t - E_CLOSE_START) / (E_DONE - E_CLOSE_START)) : 0f;
			default -> 0f;
		};
	}

	/** 0 engines cold .. 1 fully spun up. */
	public float enginePower(float partial) {
		float t = stateTime(partial);
		return switch (state()) {
			case FLYING -> 1f;
			case BOARDING -> MathHelper.clamp((t - (B_CLOSED - 6)) / (B_READY - B_CLOSED + 6), 0f, 1f);
			case EXITING -> Math.max(0f, 1f - t / 14f);
			default -> 0f;
		};
	}

	/** How far through vaulting into the seat the pilot is (0 at the side of the ship, 1 seated). */
	public float seatProgress(float partial) {
		if (state() != BOARDING) return 1f;
		float t = stateTime(partial);
		return MathHelper.clamp((t - B_GRAB) / (B_SEATED - B_GRAB), 0f, 1f);
	}

	private static float ease(float x) {
		x = MathHelper.clamp(x, 0f, 1f);
		return x * x * (3 - 2 * x);
	}

	public float roll(float partial) {
		return MathHelper.lerp(partial, prevRoll, roll);
	}

	public float thrust(float partial) {
		return MathHelper.lerp(partial, prevThrust, thrust);
	}

	public float boostLevel() {
		return boostLevel;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		setUpgrades(nbt.getInt("Upgrades"));
		if (nbt.contains("Hull")) setHealth(nbt.getFloat("Hull"));
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.putInt("Upgrades", upgrades());
		nbt.putFloat("Hull", health());
	}

	public ItemStack asItem() {
		ItemStack stack = new ItemStack(ModItems.ECTO_SKIMMER);
		NbtCompound n = new NbtCompound();
		n.putInt("Upgrades", upgrades());
		n.putFloat("Hull", health());
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
		return stack;
	}

	// ------------------------------------------------------------- riding

	@Override
	public boolean canHit() {
		return !isRemoved();
	}

	@Override
	public boolean isCollidable() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengerList().isEmpty() && passenger instanceof PlayerEntity;
	}

	@Override
	public LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof PlayerEntity p ? p : null;
	}

	/** Seat position in the ship's own frame: x = left, y = up, z = forward. */
	private static final Vec3d SEAT = new Vec3d(0, 0.72, 0.18), SIDE = new Vec3d(2.3, 0.0, 0.3);

	@Override
	protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
		float p = seatProgress(0f);
		Vec3d local;
		if (p >= 1f) {
			local = SEAT;
		} else {
			float e = ease(p);
			local = SIDE.lerp(SEAT, e).add(0, Math.sin(Math.PI * e) * 1.5, 0);
		}
		return local.rotateY(-getYaw() * MathHelper.RADIANS_PER_DEGREE);
	}

	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		Vec3d side = new Vec3d(2.4, 0.2, 0.2).rotateY(-getYaw() * MathHelper.RADIANS_PER_DEGREE);
		Vec3d at = getPos().add(side);
		BlockPos bp = BlockPos.ofFloored(at);
		if (!getWorld().getBlockState(bp).getCollisionShape(getWorld(), bp).isEmpty()) at = getPos().add(0, 2.1, 0);
		return at;
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (!getWorld().isClient && passenger instanceof ServerPlayerEntity p) {
			setState(EXITING);
			hopper = p;
			setPhasing(false);
			getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.NEUTRAL, 0.7f, 1.4f);
			getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.NEUTRAL, 0.5f, 1.6f);
			if (!isOnGround() && getY() - groundBelow() > 3) {
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 80, 0, false, false));
			}
		}
	}

	private double groundBelow() {
		BlockHitResult hit = getWorld().raycast(new RaycastContext(getPos(), getPos().add(0, -40, 0),
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, this));
		return hit.getType() == HitResult.Type.MISS ? getY() - 40 : hit.getPos().y;
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		ItemStack held = player.getStackInHand(hand);
		if (held.getItem() instanceof com.phantompowers.SkimmerUpgradeItem up) {
			if (!getWorld().isClient) {
				if (has(up.bit)) {
					player.sendMessage(Text.literal("This Skimmer already has " + up.title + ".").formatted(Formatting.YELLOW), true);
				} else {
					setUpgrades(upgrades() | up.bit);
					held.decrementUnlessCreative(1, player);
					player.sendMessage(Text.literal("Installed " + up.title + "! " + up.blurb).formatted(Formatting.AQUA), false);
					getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_SMITHING_TABLE_USE, SoundCategory.NEUTRAL, 1f, 1.1f);
					getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.NEUTRAL, 0.8f, 1.5f);
					((ServerWorld) getWorld()).spawnParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.2, getZ(), 40, 1.0, 0.6, 1.0, 0.3);
				}
			}
			return ActionResult.success(getWorld().isClient);
		}
		if (held.isOf(net.minecraft.item.Items.IRON_INGOT) && health() < MAX_HEALTH) {
			if (!getWorld().isClient) {
				setHealth(health() + 20);
				held.decrementUnlessCreative(1, player);
				getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.NEUTRAL, 0.6f, 1.5f);
				player.sendMessage(Text.literal("Hull repaired: " + Math.round(health() / MAX_HEALTH * 100) + "%").formatted(Formatting.GREEN), true);
			}
			return ActionResult.success(getWorld().isClient);
		}
		if (player.isSneaking()) {
			if (!getWorld().isClient && !hasPassengers() && state() != BOARDING) {
				ItemStack stack = asItem();
				if (!player.giveItemStack(stack)) dropStack(stack);
				getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(), SoundCategory.NEUTRAL, 1f, 0.8f);
				discard();
			}
			return ActionResult.success(getWorld().isClient);
		}
		if (hasPassengers() || state() == BOARDING) return ActionResult.PASS;
		if (!getWorld().isClient && player instanceof ServerPlayerEntity sp) {
			// Canopy up... then the pilot vaults in
			boarding = sp;
			setYaw(getYaw());
			setState(BOARDING);
			getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.NEUTRAL, 0.8f, 1.2f);
			getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.NEUTRAL, 0.6f, 1.8f);
		}
		return ActionResult.success(getWorld().isClient);
	}

	// ------------------------------------------------------------- damage

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (isInvulnerableTo(source) || getWorld().isClient || isRemoved()) return false;
		if (source.getAttacker() == getFirstPassenger()) return false;
		if (source.getAttacker() instanceof PlayerEntity p && p.getAbilities().creativeMode) {
			if (!hasPassengers()) discard();
			return true;
		}
		if (has(UP_SHIELD)) {
			amount *= 0.5f;
			((ServerWorld) getWorld()).spawnParticles(com.phantompowers.GhostPowers.GREEN, getX(), getY() + 1, getZ(), 20, 1.4, 0.8, 1.4, 0);
			getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.NEUTRAL, 0.8f, 1.4f);
		}
		setHealth(health() - amount);
		getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_IRON_GOLEM_HURT, SoundCategory.NEUTRAL, 0.7f, 1.3f);
		if (health() <= 0) {
			ServerWorld w = (ServerWorld) getWorld();
			w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
			w.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 40, 1.2, 0.8, 1.2, 0.05);
			w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.NEUTRAL, 1.2f, 1.0f);
			removeAllPassengers();
			// the wreck can be salvaged: you get the Skimmer back, upgrades and all, with a battered hull
			setHealth(MAX_HEALTH * 0.25f);
			dropStack(asItem());
			discard();
		}
		return true;
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	public static boolean pilotProtected(Entity e, DamageSource source) {
		if (!(e.getVehicle() instanceof SkimmerEntity s)) return false;
		if (source.isOf(net.minecraft.entity.damage.DamageTypes.IN_WALL)) return true;
		if (source.isOf(net.minecraft.entity.damage.DamageTypes.FLY_INTO_WALL)) return true;
		// the cockpit keeps the pilot safe: hits land on the hull instead
		if (source.getAttacker() != null || source.getSource() != null) {
			if (!e.getWorld().isClient) s.damage(source, 0.5f);
			return s.has(UP_SHIELD) || source.getSource() instanceof net.minecraft.entity.projectile.ProjectileEntity;
		}
		return false;
	}

	// --------------------------------------------------------------- tick

	@Override
	public void tick() {
		super.tick();
		if (fireCooldown > 0) fireCooldown--;
		if (ramCooldown > 0) ramCooldown--;
		prevRoll = roll;
		prevThrust = thrust;
		World w = getWorld();

		if (!w.isClient) serverStateTick((ServerWorld) w);

		LivingEntity rider = getControllingPassenger();
		boolean flying = state() == FLYING && rider instanceof PlayerEntity;
		if (isLogicalSideForUpdatingMovement()) {
			Vec3d v = getVelocity();
			if (flying) {
				PlayerEntity p = (PlayerEntity) rider;
				// Steer: the nose swings round toward where you look
				float want = p.getYaw();
				float diff = MathHelper.wrapDegrees(want - getYaw());
				float turn = MathHelper.clamp(diff, -7.5f, 7.5f) * (boostLevel > 0.5f ? 0.7f : 1f);
				setYaw(getYaw() + turn);
				roll = MathHelper.lerp(0.25f, roll, MathHelper.clamp(-turn * 4f, -28f, 28f));
				float pitchWant = MathHelper.clamp(p.getPitch(), -45f, 45f) * 0.5f;
				setPitch(MathHelper.lerp(0.2f, getPitch(), pitchWant));

				double fwd = p.forwardSpeed, strafe = p.sidewaysSpeed;
				boolean boost = w.isClient && p.isMainPlayer() && clientBoost && fwd > 0;
				if (boost && !boosting) {
					w.playSound(p, getX(), getY(), getZ(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.8f, 0.5f);
					w.playSound(p, getX(), getY(), getZ(), SoundEvents.ENTITY_BREEZE_WIND_BURST.value(), SoundCategory.PLAYERS, 0.8f, 0.7f);
				}
				if (w.isClient && p.isMainPlayer() && has(UP_PHASE) && boost != boosting) {
					sendAction.accept(boost ? com.phantompowers.GhostPowers.A_SKIMMER_PHASE_ON : com.phantompowers.GhostPowers.A_SKIMMER_PHASE_OFF);
				}
				boosting = boost;
				boostLevel = MathHelper.clamp(boostLevel + (boost ? 0.1f : -0.06f), 0f, 1f);
				double accel = 0.075 * (has(UP_THRUSTERS) ? 1.35 : 1.0) * (1 + 1.2 * boostLevel);
				Vec3d facing = Vec3d.fromPolar(MathHelper.clamp(p.getPitch(), -60f, 60f), getYaw());
				Vec3d side = Vec3d.fromPolar(0, getYaw() - 90f);
				v = v.add(facing.multiply(fwd * accel)).add(side.multiply(strafe * 0.05));
				if (w.isClient && p.isMainPlayer()) {
					if (clientUp) v = v.add(0, 0.06, 0);
					if (clientDown) v = v.add(0, -0.06, 0);
				}
				if (fwd < 0) v = v.multiply(0.88);
				thrust = MathHelper.lerp(0.15f, thrust, (float) Math.max(0, fwd) * (1 + boostLevel));
				if (Math.abs(fwd) < 0.01 && Math.abs(strafe) < 0.01) v = v.add(0, Math.sin(age * 0.12) * 0.003, 0);
				v = v.multiply(0.93);
				double max = 0.95 * (has(UP_THRUSTERS) ? 1.35 : 1.0) * (1 + 0.85 * boostLevel);
				if (v.length() > max) v = v.normalize().multiply(max);
			} else {
				roll = MathHelper.lerp(0.2f, roll, 0f);
				thrust = MathHelper.lerp(0.2f, thrust, 0f);
				boostLevel = Math.max(0f, boostLevel - 0.1f);
				setPitch(MathHelper.lerp(0.2f, getPitch(), 0f));
				// settle down gently onto its pads
				double ground = groundBelow();
				boolean holding = state() == BOARDING || state() == EXITING;
				if (holding) v = v.multiply(0.6);
				else if (getY() - ground > 0.05) v = new Vec3d(v.x * 0.8, Math.max(-0.25, v.y - 0.02), v.z * 0.8);
				else v = Vec3d.ZERO;
			}
			noClip = phasing() || (w.isClient && flying && has(UP_PHASE) && boostLevel > 0.3f);
			setVelocity(v);
			move(MovementType.SELF, v);
		}
		if (w.isClient) clientFx();
	}

	private void serverStateTick(ServerWorld w) {
		float t = stateTime(0);
		switch (state()) {
			case BOARDING -> {
				if (boarding == null || !boarding.isAlive() || boarding.getWorld() != w || boarding.squaredDistanceTo(this) > 64) {
					boarding = null;
					setState(IDLE);
					break;
				}
				if (t >= B_GRAB && !hasPassengers()) boarding.startRiding(this, true);
				if (t == B_SEATED) {
					w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.NEUTRAL, 0.8f, 1.1f);
				}
				if (t == B_CLOSED - 6) {
					w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.NEUTRAL, 1.0f, 1.6f);
					w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.NEUTRAL, 0.8f, 0.7f);
				}
				if (t >= B_READY) {
					boarding = null;
					setState(hasPassengers() ? FLYING : IDLE);
					if (getFirstPassenger() instanceof ServerPlayerEntity sp) welcome(sp);
				}
			}
			case EXITING -> {
				if (t == E_HOP && hopper != null && hopper.isAlive()) {
					Vec3d out = new Vec3d(0.35, 0.42, 0.0).rotateY(-getYaw() * MathHelper.RADIANS_PER_DEGREE);
					hopper.setVelocity(out);
					hopper.velocityModified = true;
				}
				if (t == E_CLOSE_START) w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.NEUTRAL, 0.7f, 1.2f);
				if (t >= E_DONE) {
					hopper = null;
					setState(IDLE);
				}
			}
			case FLYING -> {
				if (!hasPassengers()) setState(IDLE);
			}
			default -> {}
		}
		if (state() == FLYING && getFirstPassenger() instanceof ServerPlayerEntity pilot) {
			if (++humTimer >= 30) {
				humTimer = 0;
				w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.NEUTRAL, 1.2f, 0.6f + (float) getVelocity().length() * 0.6f);
			}
			if (++radarTimer >= 20) {
				radarTimer = 0;
				sendRadar(w, pilot);
			}
			if (has(UP_SHIELD) && ramCooldown == 0 && getVelocity().length() > 0.45) ram(w, pilot);
		}
	}

	private void welcome(ServerPlayerEntity p) {
		p.sendMessage(Text.literal("Ecto-Skimmer online.").formatted(Formatting.GREEN, Formatting.BOLD), true);
		p.sendMessage(Text.literal("W/S fly where you look, A/D strafe, Space up, Ctrl/sprint boost, Right-click fire, Sneak climb out.").formatted(Formatting.GRAY), false);
		if (!getWorld().getRegistryKey().equals(PortalLogic.GHOST_ZONE)) {
			p.sendMessage(Text.literal("The lair scanner only picks up signals inside the Ghost Zone.").formatted(Formatting.DARK_GRAY), false);
		}
	}

	public void setPhasing(boolean on) {
		if (on && !has(UP_PHASE)) return;
		if (phasing() == on) return;
		dataTracker.set(PHASING, on);
		noClip = on;
		getWorld().playSound(null, getX(), getY(), getZ(), on ? SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE : SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.NEUTRAL, 1f, on ? 1.4f : 0.9f);
	}

	/** With the Ecto-Shield up, ramming ghosts at speed knocks them flying. */
	private void ram(ServerWorld w, ServerPlayerEntity pilot) {
		Box box = getBoundingBox().expand(0.6);
		for (Entity e : w.getOtherEntities(this, box, e -> e != pilot && e instanceof LivingEntity && e.isAlive())) {
			LivingEntity le = (LivingEntity) e;
			le.damage(w.getDamageSources().mobAttack(pilot), 8f * HunterSuit.ghostBonus(le));
			Vec3d push = getVelocity().normalize();
			le.takeKnockback(1.6, -push.x, -push.z);
			w.spawnParticles(com.phantompowers.GhostPowers.GREEN, le.getX(), le.getY() + 1, le.getZ(), 25, 0.5, 0.6, 0.5, 0);
			w.playSound(null, le.getX(), le.getY(), le.getZ(), SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.NEUTRAL, 1f, 1.2f);
			ramCooldown = 10;
		}
	}

	private void sendRadar(ServerWorld w, ServerPlayerEntity pilot) {
		List<SkimmerRadarPayload.Blip> blips = new ArrayList<>();
		boolean zone = w.getRegistryKey().equals(PortalLogic.GHOST_ZONE);
		int ghosts = -1;
		if (zone) {
			List<GhostZoneLife.LairSignal> lairs = GhostZoneLife.lairSignals(w, getBlockPos());
			lairs.sort((a, b) -> Double.compare(a.pos().getSquaredDistance(getPos()), b.pos().getSquaredDistance(getPos())));
			int keep = has(UP_SCANNER) ? lairs.size() : Math.min(2, lairs.size());
			for (int i = 0; i < keep; i++) {
				GhostZoneLife.LairSignal l = lairs.get(i);
				blips.add(new SkimmerRadarPayload.Blip(l.name(), l.pos().getX(), l.pos().getY(), l.pos().getZ()));
			}
		}
		if (has(UP_SCANNER)) {
			ghosts = w.getOtherEntities(this, getBoundingBox().expand(64), e -> e instanceof AbstractGhostEntity && e.isAlive()).size();
		}
		SkimmerRadarPayload pl = new SkimmerRadarPayload(zone, ghosts, blips);
		if (ServerPlayNetworking.canSend(pilot, SkimmerRadarPayload.ID)) ServerPlayNetworking.send(pilot, pl);
	}

	/** Nose cannon (and the wing cannons, if fitted). */
	public void fire(ServerPlayerEntity p) {
		if (fireCooldown > 0 || state() != FLYING) return;
		fireCooldown = has(UP_CANNONS) ? 5 : 7;
		ServerWorld w = p.getServerWorld();
		Vec3d look = p.getRotationVec(1f);
		float rad = -getYaw() * MathHelper.RADIANS_PER_DEGREE;
		shoot(w, p, getPos().add(new Vec3d(0, 0.25, 2.6).rotateY(rad)), look, 8f, 0x55FF99);
		if (has(UP_CANNONS)) {
			for (int s = -1; s <= 1; s += 2) {
				shoot(w, p, getPos().add(new Vec3d(1.4 * s, 0.95, 1.6).rotateY(rad)), look, 6f, 0xAAFFE0);
			}
		}
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.8f, 1.6f);
		w.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.6f, 1.8f);
	}

	private void shoot(ServerWorld w, ServerPlayerEntity p, Vec3d from, Vec3d look, float dmg, int color) {
		Vec3d end = from.add(look.multiply(64));
		BlockHitResult bh = w.raycast(new RaycastContext(from, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
		if (bh.getType() != HitResult.Type.MISS) end = bh.getPos();
		Box box = new Box(from, end).expand(1.0);
		EntityHitResult eh = ProjectileUtil.raycast(p, from, end, box, e -> e != p && e != this && !e.isSpectator() && e.canHit(), from.squaredDistanceTo(end));
		if (eh != null) {
			end = eh.getPos();
			eh.getEntity().damage(w.getDamageSources().indirectMagic(this, p), dmg * HunterSuit.ghostBonus(eh.getEntity()));
			if (eh.getEntity() instanceof LivingEntity le) le.takeKnockback(0.4, -look.x, -look.z);
			w.spawnParticles(com.phantompowers.GhostPowers.GREEN, end.x, end.y, end.z, 12, 0.3, 0.3, 0.3, 0);
		} else if (bh.getType() != HitResult.Type.MISS) {
			w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 10, 0.2, 0.2, 0.2, 0.2);
		}
		BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) end.x, (float) end.y, (float) end.z, color);
		for (ServerPlayerEntity viewer : PlayerLookup.around(w, from, 128)) {
			if (ServerPlayNetworking.canSend(viewer, BeamPayload.ID)) ServerPlayNetworking.send(viewer, beam);
		}
	}

	// ------------------------------------------------------------ client fx

	private void clientFx() {
		World w = getWorld();
		float power = enginePower(0);
		float rad = -getYaw() * MathHelper.RADIANS_PER_DEGREE;
		if (power > 0.05f) {
			Vec3d back = new Vec3d(0, 0, -1).rotateY(rad);
			Vec3d[] nozzles = {new Vec3d(0.31, 1.0, -1.95), new Vec3d(-0.31, 1.0, -1.95), new Vec3d(1.06, 0.88, -1.45), new Vec3d(-1.06, 0.88, -1.45)};
			for (Vec3d n : nozzles) {
				Vec3d at = getPos().add(n.rotateY(rad));
				double sp = 0.05 + 0.25 * thrust;
				w.addParticle(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, back.x * sp, 0, back.z * sp);
				if (boostLevel > 0.2f) {
					w.addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, back.x * (sp + 0.2), 0, back.z * (sp + 0.2));
					if (age % 2 == 0) w.addParticle(ParticleTypes.FIREWORK, at.x, at.y, at.z, back.x * 0.15, 0, back.z * 0.15);
				}
			}
			// hover pads wash
			if (age % 2 == 0) {
				Vec3d pad = new Vec3d((random.nextBoolean() ? 0.56 : -0.56), 0.02, random.nextBoolean() ? 1.1 : -1.1).rotateY(rad);
				w.addParticle(com.phantompowers.GhostPowers.GREEN, getX() + pad.x, getY(), getZ() + pad.z, 0, -0.05, 0);
			}
		}
		// steam hissing out when the canopy pops
		float open = canopyOpen(0);
		if (open > 0.05f && open < 0.95f && age % 2 == 0) {
			Vec3d c = getPos().add(new Vec3d(0, 1.7, 0.6).rotateY(rad));
			w.addParticle(ParticleTypes.CLOUD, c.x + random.nextGaussian() * 0.4, c.y, c.z + random.nextGaussian() * 0.4, 0, 0.05, 0);
		}
		if (phasing() && age % 2 == 0) {
			w.addParticle(ParticleTypes.GLOW, getX() + random.nextGaussian(), getY() + 1 + random.nextGaussian() * 0.5, getZ() + random.nextGaussian(), 0, 0, 0);
		}
	}

	public float dishSpin(float partial) {
		return (age + partial) * (state() == FLYING ? 0.12f : 0.02f);
	}
}
