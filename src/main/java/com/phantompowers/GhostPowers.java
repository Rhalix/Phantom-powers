package com.phantompowers;

import com.phantompowers.entity.GliderEntity;
import com.phantompowers.network.BeamPayload;
import com.phantompowers.network.InvisiblePayload;
import com.phantompowers.network.RingPayload;
import com.phantompowers.network.SyncPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GhostPowers {
	public static final float MAX_ENERGY = 100f;
	public static final int CUTSCENE_TICKS = 50;
	private static final int MIN_WAIL_TICKS = 20;
	private static final int MAX_WAIL_TICKS = 200;
	/** Hold the wail this long and it turns into the finale cutscene. */
	private static final int WAIL_FINALE_AT = 100;
	public static final int WAIL_FINALE_TICKS = 80;
	/** Ticks spent in ghost form per stat point (600 ticks = 30 seconds). */
	public static final int POINT_TICKS = 600;

	public static final int BLAST = 0, INTANGIBLE = 1, POSSESS = 2, WAIL = 3, TORNADO = 4, INVISIBLE = 5, FROST = 6, FISTS = 7, SHIELD = 8, DUPLICATE = 9,
			CONSTRUCT = 10, SPIN = 11, STINGER = 12, ABSORB = 13;
	public static final int SKILL_COUNT = 14;
	/** How many powers fit in your loadout at once. */
	public static final int LOADOUT_MAX = Slots.COUNT;
	public static final String[] NAMES = {"Ecto Blast", "Intangibility", "Possession", "Ghostly Wail", "Wail Tornado", "Invisibility", "Frost Breath", "Ecto Fists", "Ecto Shield", "Duplication",
			"Ecto Constructs", "Tornado Spin", "Ghost Stinger", "Energy Absorption"};
	public static final int[] COSTS = {0, 3, 5, 8, 12, 2, 6, 4, 6, 9, 8, 10, 7, 6};

	/** Powers only the dark half-ghost can use. */
	public static boolean darkOnly(int skill) {
		return skill >= CONSTRUCT;
	}
	/** Ghost suit looks you can pick in the suit customizer. */
	public static final String[] SUIT_VARIANTS = {"Classic", "Shadow", "Ecto Glow", "Crimson", "Frostbite", "Royal"};
	public static final String[] DESCRIPTIONS = {
			"Fire a beam of green ecto energy.",
			"Walk through walls. Sneak to sink, fly to rise. Sneak+hold to phase others.",
			"Take over a mob or player and control where it goes.",
			"A huge scream that breaks blocks and blasts mobs away.",
			"A giant tornado of wail energy that rips up the ground.",
			"Turn completely invisible, armor and all.",
			"Hold to breathe ice. Freeze enemies solid, then punch to shatter.",
			"Wrap your fists in glowing ecto energy. Punches hit way harder and blast enemies back.",
			"Hold to raise a dome of green energy all around you that blocks attacks and bounces arrows back.",
			"Split into 3 ghost clones. They wail at whatever you attack or whatever hits you, and wail with you.",
			"DARK FORM: conjure a giant ecto weapon (sword, hammer or fist) that smashes down on your target.",
			"DARK FORM: while it's on, move or fly fast and you whip up a tornado around yourself.",
			"DARK FORM: grab whoever you're facing and shock them with ecto lightning. Half-ghosts get forced human.",
			"DARK FORM: for a few seconds, soak up magic, fire, blasts and lightning as ghost energy and health."
	};

	public static final int A_TRANSFORM = 0, A_BLAST = 1, A_INTANGIBLE = 2, A_POSSESS = 3, A_WAIL = 4, A_TORNADO = 5,
			A_INVISIBLE = 6, A_INTANGIBLE_RELEASE = 7, A_TRANSFORM_CUTSCENE = 8,
			A_WAIL_RELEASE = 9, A_SUIT = 10, A_SUIT_CUTSCENE = 11, A_GLIDER_FIRE = 12,
			A_FROST = 13, A_FROST_RELEASE = 14, A_STRUGGLE = 15, A_FISTS = 16, A_SHIELD = 17, A_SHIELD_RELEASE = 18, A_DUPLICATE = 19, A_CONSTRUCT = 20, A_SPIN = 21, A_STINGER = 22, A_ABSORB = 23, A_DARK_TOGGLE = 24, A_BLAST_RELEASE = 25, A_SKIMMER_FIRE = 26, A_SKIMMER_PHASE_ON = 27, A_SKIMMER_PHASE_OFF = 28, A_CANNON = 29, A_CANNON_STOW = 30, A_CANNON_MODE = 31, A_EQUIP = 500, A_UNLOCK = 100, A_SUIT_VARIANT = 300;

	private static final float BLAST_COST = 12f;
	private static final float PHASE_TARGET_COST = 25f;
	private static final float POSSESS_START_COST = 15f;
	private static final float WAIL_COST = 60f;
	private static final float TORNADO_COST = 80f;

	public static final DustParticleEffect GREEN = new DustParticleEffect(new Vector3f(0.25f, 1.0f, 0.4f), 1.3f);
	private static final DustParticleEffect PALE = new DustParticleEffect(new Vector3f(0.75f, 1.0f, 0.85f), 1.0f);

	private static final List<PhasedBlock> PHASED = new ArrayList<>();
	/** Blocks a player is currently holding intangible (restored when they let go). */
	private static final Map<UUID, List<PhasedBlock>> HELD_BLOCKS = new HashMap<>();
	private static final List<WailWave> WAVES = new ArrayList<>();
	private static final List<Tornado> TORNADOES = new ArrayList<>();
	private static int tornadoIds;

	private GhostPowers() {}

	// ------------------------------------------------------------------ input

	public static void handleAction(ServerPlayerEntity p, int action) {
		if (!p.isAlive()) return;
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		switch (action) {
			case A_TRANSFORM -> { if (d.cutscene == 0) toggleTransform(p, d, true); }
			case A_TRANSFORM_CUTSCENE -> startCutscene(p, d);
			case A_BLAST -> {
				if (d.possessing != null) HostPowers.active(p, d);
				else if (d.suitOn && !d.transformed) HunterGear.startAim(p, d);
				else if (ready(p, d, st, BLAST)) blast(p, d);
			}
			case A_CONSTRUCT -> { if (ready(p, d, st, CONSTRUCT)) DarkPowers.construct(p, d); }
			case A_SPIN -> { if (ready(p, d, st, SPIN)) DarkPowers.toggleSpin(p, d); }
			case A_STINGER -> { if (ready(p, d, st, STINGER)) DarkPowers.stinger(p, d); }
			case A_ABSORB -> { if (ready(p, d, st, ABSORB)) DarkPowers.absorb(p, d); }
			case A_DARK_TOGGLE -> DarkPowers.toggleDarkMode(p, d);
			case A_BLAST_RELEASE -> { if (d.missileAim) HunterGear.releaseAim(p, d); }
			case A_FROST -> { if (ready(p, d, st, FROST)) FrostLogic.start(p, d); }
			case A_FROST_RELEASE -> d.frostHeld = false;
			case A_STRUGGLE -> {
				ThermosItem.struggle(p);
				GhostNet.struggle(p);
			}
			case A_FISTS -> { if (ready(p, d, st, FISTS)) toggleFists(p, d); }
			case A_SHIELD -> { if (ready(p, d, st, SHIELD)) d.shieldHeld = true; }
			case A_SHIELD_RELEASE -> d.shieldHeld = false;
			case A_DUPLICATE -> { if (ready(p, d, st, DUPLICATE)) com.phantompowers.entity.GhostCloneEntity.toggle(p, d); }
			case A_INTANGIBLE -> { if (ready(p, d, st, INTANGIBLE)) intangibility(p, d); }
			case A_POSSESS -> { if (ready(p, d, st, POSSESS)) possession(p, d, st); }
			case A_WAIL -> {
				if (d.suitOn && !d.transformed) HunterGear.toggleCannons(p, d);
				else if (ready(p, d, st, WAIL)) wail(p, d);
			}
			case A_WAIL_RELEASE -> d.wailHeld = false;
			case A_SUIT -> { if (d.cutscene == 0) HunterSuit.toggle(p, d, true); }
			case A_SUIT_CUTSCENE -> startSuitCutscene(p, d);
			case A_GLIDER_FIRE -> { if (p.getVehicle() instanceof GliderEntity glider) glider.fire(p); }
			case A_CANNON -> HunterGear.cannonKey(p, d);
			case A_CANNON_STOW -> { if (d.cannons) HunterGear.toggleCannons(p, d); }
			case A_CANNON_MODE -> HunterGear.toggleCannonMode(p, d);
			case A_SKIMMER_FIRE -> { if (p.getVehicle() instanceof com.phantompowers.entity.SkimmerEntity sk) sk.fire(p); }
			case A_SKIMMER_PHASE_ON -> { if (p.getVehicle() instanceof com.phantompowers.entity.SkimmerEntity sk) sk.setPhasing(true); }
			case A_SKIMMER_PHASE_OFF -> { if (p.getVehicle() instanceof com.phantompowers.entity.SkimmerEntity sk) sk.setPhasing(false); }
			case A_TORNADO -> { if (ready(p, d, st, TORNADO)) tornado(p, d); }
			case A_INVISIBLE -> { if (ready(p, d, st, INVISIBLE)) toggleInvisible(p, d); }
			case A_INTANGIBLE_RELEASE -> releaseHold(p, d, true);
			default -> {
				if (action >= A_UNLOCK && action < A_UNLOCK + SKILL_COUNT) tryUnlock(p, d, st, action - A_UNLOCK);
				if (action >= A_EQUIP && action < A_EQUIP + SKILL_COUNT) toggleEquip(p, d, action - A_EQUIP);
				if (action >= A_SUIT_VARIANT && action < A_SUIT_VARIANT + SUIT_VARIANTS.length) {
					d.suitVariant = action - A_SUIT_VARIANT;
					st.markDirty();
					broadcastStates(p.getServer());
					bar(p, "Ghost suit: " + SUIT_VARIANTS[d.suitVariant], Formatting.GREEN);
				}
			}
		}
		st.markDirty();
		d.syncTimer = 0;
	}

	private static boolean ready(ServerPlayerEntity p, GhostData d, GhostState st, int skill) {
		if (!d.powers) {
			bar(p, "You don't have ghost powers yet. Activate a Ghost Portal!", Formatting.RED);
			return false;
		}
		if (!d.has(skill)) {
			bar(p, NAMES[skill] + " is locked. Unlock it in Ghost Skills (press K or open your inventory).", Formatting.YELLOW);
			return false;
		}
		initLoadout(d);
		if (!d.equipped(skill)) {
			bar(p, NAMES[skill] + " isn't in a Power Slot. Put it in one from the Phantom Menu (Power Slots).", Formatting.YELLOW);
			return false;
		}
		// Intangibility and invisibility work in human form too
		if (!d.transformed && skill != INTANGIBLE && skill != INVISIBLE) {
			bar(p, "Go ghost first! Press V", Formatting.YELLOW);
			return false;
		}
		if (darkOnly(skill) && !(d.dark && d.darkMode)) {
			bar(p, NAMES[skill] + " only works in your dark form.", Formatting.DARK_RED);
			return false;
		}
		return true;
	}

	private static void tryUnlock(ServerPlayerEntity p, GhostData d, GhostState st, int skill) {
		if (!d.powers) {
			bar(p, "You don't have ghost powers yet. Activate a Ghost Portal!", Formatting.RED);
			return;
		}
		if (d.has(skill)) return;
		if (d.points < COSTS[skill]) {
			bar(p, NAMES[skill] + " needs " + COSTS[skill] + " stat points (you have " + d.points
					+ "). Stay in ghost form to earn more!", Formatting.YELLOW);
			return;
		}
		if (darkOnly(skill) && !d.dark) {
			bar(p, NAMES[skill] + " belongs to the dark half-ghost. Use a Prototype Ghost Portal first...", Formatting.DARK_RED);
			return;
		}
		d.points -= COSTS[skill];
		d.unlocked |= 1 << skill;
		initLoadout(d);
		Slots.autoAdd(d, skill);
		st.markDirty();
		p.sendMessage(Text.literal("New ghost power unlocked: " + NAMES[skill] + "!").formatted(Formatting.GREEN, Formatting.BOLD), false);
		syncNow(p);
		sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
	}

	// ---------------------------------------------------------------- loadout

	/** First time: fill the loadout with the first powers you already have. */
	public static void initLoadout(GhostData d) {
		if (!d.loadoutInit) {
			d.loadoutInit = true;
			d.loadout = 0;
			for (int i = 0; i < SKILL_COUNT && Integer.bitCount(d.loadout) < LOADOUT_MAX; i++) if (d.has(i)) d.loadout |= 1 << i;
		}
		Slots.ensure(d);
	}

	private static void toggleEquip(ServerPlayerEntity p, GhostData d, int skill) {
		initLoadout(d);
		Slots.toggle(p, d, skill);
	}

	/** The customizer sent new suit colours. */
	public static void setSuitColors(ServerPlayerEntity p, int[] colors) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		d.suitColors = SuitColors.sanitize(colors);
		st.markDirty();
		broadcastStates(p.getServer());
		bar(p, "Ghost suit updated!", Formatting.GREEN);
	}

	// -------------------------------------------------------------- transform

	/** Change back to human right now (used by the bigger cutscenes). */
	public static void forceHuman(ServerPlayerEntity p, GhostData d) {
		if (d.transformed) toggleTransform(p, d, false);
	}

	/** Change into ghost form right now (used by the bigger cutscenes). */
	public static void forceTransform(ServerPlayerEntity p, GhostData d) {
		if (!d.transformed) toggleTransform(p, d, false);
	}

	private static void toggleTransform(ServerPlayerEntity p, GhostData d, boolean rings) {
		if (!d.powers) {
			bar(p, "You don't have ghost powers yet. Activate a Ghost Portal!", Formatting.RED);
			return;
		}
		if (d.transformed) {
			endForm(p, d);
			if (rings) sendRings(p, 20, 4);
			bar(p, "Back to human form.", Formatting.GRAY);
			sound(p, SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.4f);
			return;
		}
		if (d.exhaust > 0) {
			bar(p, "Too drained from the wail... (" + (d.exhaust / 20 + 1) + "s)", Formatting.RED);
			return;
		}
		if (d.suitOn) HunterSuit.toggle(p, d, false);
		d.transformed = true;
		setFlight(p, true);
		applyStats(p, d, true);
		p.heal(statBonusHealth(d));
		if (rings) sendRings(p, 20);
		broadcastStates(p.getServer());
		bar(p, "I'm going ghost!", Formatting.GREEN);
		sound(p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.6f);
	}

	/** Leave ghost form and switch off any active power. */
	public static void endForm(ServerPlayerEntity p, GhostData d) {
		d.transformed = false;
		stopIntangible(p, d, false);
		stopPossess(p, d, false);
		releaseHold(p, d, false);
		stopInvisible(p, d);
		d.fists = false;
		d.shieldHeld = false;
		if (d.shieldUp) setShield(p, d, false);
		com.phantompowers.entity.GhostCloneEntity.dismiss(p);
		DarkPowers.endForm(p, d);
		fistMods(p, d);
		setFlight(p, false);
		applyStats(p, d, false);
		broadcastStates(p.getServer());
	}

	/** Where the player is aiming: the creature under the crosshair, or the block, or a point far ahead. */
	public static Vec3d aimPoint(ServerPlayerEntity p, double range) {
		Entity e = aimEntity(p, GhostState.of(p.getServer()).data(p.getUuid()), (int) range);
		if (e != null) return e.getPos().add(0, e.getHeight() * 0.6, 0);
		return aimBlockPos(p, (int) range);
	}

	// ------------------------------------------------------------ ecto shield

	private static final float SHIELD_DRAIN = 0.3f;
	public static final double SHIELD_RADIUS = 1.7;

	private static void setShield(ServerPlayerEntity p, GhostData d, boolean up) {
		if (d.shieldUp == up) return;
		d.shieldUp = up;
		ServerWorld w = p.getServerWorld();
		if (up) {
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.9f, 1.9f);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.6f, 1.6f);
		} else {
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.7f, 1.9f);
		}
		broadcastStates(p.getServer());
		d.syncTimer = 0;
	}

	private static void tickShield(ServerPlayerEntity p, GhostData d) {
		boolean want = d.shieldHeld && d.transformed && p.isAlive() && d.energy > 0 && d.possessing == null && d.cutscene == 0;
		if (want != d.shieldUp) {
			if (!want && d.shieldHeld && d.energy <= 0) bar(p, "Out of ghost energy, your shield collapsed!", Formatting.RED);
			setShield(p, d, want);
		}
		if (!d.shieldUp) return;
		d.energy = Math.max(0, d.energy - SHIELD_DRAIN);
		// Bounce incoming projectiles straight back
		ServerWorld w = p.getServerWorld();
		Vec3d c = p.getPos().add(0, p.getHeight() / 2, 0);
		for (net.minecraft.entity.projectile.ProjectileEntity pr : w.getEntitiesByClass(net.minecraft.entity.projectile.ProjectileEntity.class,
				p.getBoundingBox().expand(SHIELD_RADIUS + 1.5), pr -> pr.getOwner() != p)) {
			Vec3d to = c.subtract(pr.getPos());
			Vec3d v = pr.getVelocity();
			if (to.length() > SHIELD_RADIUS + 1.2 || v.dotProduct(to) <= 0) continue;
			pr.setVelocity(v.multiply(-0.8));
			pr.velocityModified = true;
			pr.setOwner(p);
			shieldHit(p, pr.getPos());
		}
	}

	/** Called when something hits a raised shield: green ripple, a zap, and some energy drained. */
	public static void shieldHit(ServerPlayerEntity p, Vec3d at) {
		ServerWorld w = p.getServerWorld();
		w.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.2f, 1.6f);
		w.playSound(null, at.x, at.y, at.z, SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.8f, 1.3f);
		com.phantompowers.network.ShieldHitPayload msg = new com.phantompowers.network.ShieldHitPayload(p.getId(), (float) at.x, (float) at.y, (float) at.z);
		for (ServerPlayerEntity viewer : PlayerLookup.around(w, at, 64)) {
			if (ServerPlayNetworking.canSend(viewer, com.phantompowers.network.ShieldHitPayload.ID)) ServerPlayNetworking.send(viewer, msg);
		}
	}

	/** Blocks a hit if the player's Ecto Shield is up. Returns true when the damage was stopped. */
	public static boolean shieldBlocks(ServerPlayerEntity p, net.minecraft.entity.damage.DamageSource source, float amount) {
		GhostData d = GhostState.of(p.getServer()).data(p.getUuid());
		if (!d.shieldUp || amount <= 0) return false;
		if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY)
				|| source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_FALL)
				|| source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_DROWNING)
				|| source.isOf(net.minecraft.entity.damage.DamageTypes.STARVE)
				|| source.isOf(net.minecraft.entity.damage.DamageTypes.IN_WALL)) return false;
		d.energy = Math.max(0, d.energy - amount * 1.5f);
		Vec3d from = source.getPosition() != null ? source.getPosition() : p.getEyePos().add(p.getRotationVec(1f));
		Vec3d c = p.getPos().add(0, p.getHeight() / 2, 0);
		Vec3d dir = from.subtract(c);
		dir = dir.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : dir.normalize();
		shieldHit(p, c.add(dir.multiply(SHIELD_RADIUS)));
		// Shove melee attackers back off the shield
		if (source.getAttacker() instanceof LivingEntity att && source.getSource() == att && att.squaredDistanceTo(p) < 16) {
			att.takeKnockback(0.9, -dir.x, -dir.z);
		}
		d.syncTimer = 0;
		return true;
	}

	// ------------------------------------------------------------ ecto fists

	private static final float FIST_COST = 4f;

	private static final Identifier FIST_DAMAGE = PhantomMod.id("ecto_fists_damage");
	private static final Identifier FIST_KNOCK = PhantomMod.id("ecto_fists_knockback");

	public static void fistMods(ServerPlayerEntity p, GhostData d) {
		boolean on = d.fists && d.transformed;
		setMod(p, EntityAttributes.GENERIC_ATTACK_DAMAGE, FIST_DAMAGE, on ? 5.0 + level(d) * 0.6 : 0);
		setMod(p, EntityAttributes.GENERIC_ATTACK_KNOCKBACK, FIST_KNOCK, on ? 2.0 : 0);
	}

	private static void toggleFists(ServerPlayerEntity p, GhostData d) {
		d.fists = !d.fists;
		fistMods(p, d);
		ServerWorld w = p.getServerWorld();
		if (d.fists) {
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.9f, 1.8f);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.6f, 1.4f);
			fistSparks(p);
			bar(p, "Ecto Fists charged! Punch to blast.", Formatting.GREEN);
		} else {
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.6f, 1.8f);
			bar(p, "Ecto Fists off.", Formatting.GRAY);
		}
		broadcastStates(p.getServer());
	}

	/** Green energy crackling around both hands. */
	private static void fistSparks(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		float yaw = (float) Math.toRadians(p.bodyYaw);
		double cos = Math.cos(yaw), sin = Math.sin(yaw);
		for (int side = -1; side <= 1; side += 2) {
			double hx = p.getX() + cos * 0.38 * side - sin * 0.15;
			double hz = p.getZ() + sin * 0.38 * side + cos * 0.15;
			w.spawnParticles(GREEN, hx, p.getY() + 0.75, hz, 1, 0.06, 0.08, 0.06, 0);
		}
	}

	/** A punch with Ecto Fists on: big extra damage and a green blast that knocks them flying. */
	public static void onFistPunch(ServerPlayerEntity p, Entity target) {
		GhostData d = GhostState.of(p.getServer()).data(p.getUuid());
		if (!d.fists || !d.transformed || !(target instanceof LivingEntity le) || target == p) return;
		if (d.energy < FIST_COST) {
			d.fists = false;
			fistMods(p, d);
			broadcastStates(p.getServer());
			bar(p, "Out of ghost energy, your Ecto Fists fizzled out!", Formatting.RED);
			return;
		}
		d.energy -= FIST_COST;
		ServerWorld w = p.getServerWorld();
		// (the extra damage and knockback come from attribute bonuses, so the punch itself lands harder)
		target.addVelocity(0, 0.3, 0);
		target.velocityModified = true;
		Vec3d at = target.getPos().add(0, target.getHeight() * 0.55, 0);
		w.spawnParticles(GREEN, at.x, at.y, at.z, 26, 0.35, 0.35, 0.35, 0.25);
		w.spawnParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		w.spawnParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.5f, 1.7f);
		w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 0.8f);
		d.syncTimer = 0;
	}

	// ------------------------------------------------------- devoured essences

	public static final int ESS_HOSTILE = 0, ESS_FRIENDLY = 1, ESS_JAILER = 2, ESS_CRATE = 3, ESS_VOLTAGE = 4, ESS_WISH = 5, ESS_REV = 6, ESS_ROXY = 7, ESS_SHADE = 8, ESS_DOLORA = 9;
	public static final String[] ESSENCE_NAMES = {"Hostile Ghost", "Friendly Ghost", "Ghost Walker", "Box Ghost", "Voltage", "Desiree", "Johnny 13", "Kitty", "Shadow", "Penelope Spectra"};
	public static final String[] ESSENCE_POWERS = {
			"Strength and night vision",
			"Constant healing",
			"Toughness, and your hits chain enemies (slow them)",
			"A crate-armor shield that keeps coming back",
			"Speed, and every 4th hit calls down lightning",
			"Luck, and a free wish (a random blessing) every minute",
			"Speed and fire immunity, and your hits set enemies ablaze with ghost-fire",
			"Higher jumps, and your hits leave enemies lovestruck (slow and weak) while healing you",
			"See in the dark, and your hits blind enemies and drain their life into you",
			"Emotion feast: your hits drain your enemies' strength and feed you (healing and absorption)"};

	public static void eatEssence(ServerPlayerEntity p, int power) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		boolean already = (d.eaten & (1 << power)) != 0;
		d.eaten |= 1 << power;
		st.markDirty();
		applyEaten(p, d);
		p.sendMessage(Text.literal(already
				? "You already carry the " + ESSENCE_NAMES[power] + "'s power. It surges through you again!"
				: "You devoured the " + ESSENCE_NAMES[power] + "'s essence! You gained: " + ESSENCE_POWERS[power] + " (until you die).")
				.formatted(Formatting.LIGHT_PURPLE), false);
		ServerWorld w = p.getServerWorld();
		w.spawnParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1, p.getZ(), 25, 0.4, 0.6, 0.4, 0.05);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.GHOST_DEATH, SoundCategory.PLAYERS, 0.8f, 1.6f);
	}

	/** Refresh the powers of every essence this player has eaten. */
	private static void applyEaten(ServerPlayerEntity p, GhostData d) {
		if (d.eaten == 0) return;
		if ((d.eaten & (1 << ESS_HOSTILE)) != 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 120, 0, true, false, true));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 320, 0, true, false, true));
		}
		if ((d.eaten & (1 << ESS_FRIENDLY)) != 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 120, 0, true, false, true));
		}
		if ((d.eaten & (1 << ESS_JAILER)) != 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 120, 0, true, false, true));
		}
		if ((d.eaten & (1 << ESS_CRATE)) != 0 && p.getAbsorptionAmount() <= 0 && p.age % 600 < 40) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 1200, 1, true, false, true));
		}
		if ((d.eaten & (1 << ESS_VOLTAGE)) != 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 120, 1, true, false, true));
		}
		if ((d.eaten & (1 << ESS_REV)) != 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 120, 0, true, false, true));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 120, 0, true, false, true));
		}
		if ((d.eaten & (1 << ESS_ROXY)) != 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 120, 1, true, false, true));
		}
		if ((d.eaten & (1 << ESS_SHADE)) != 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 320, 0, true, false, true));
		}
		if ((d.eaten & (1 << ESS_WISH)) != 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 120, 1, true, false, true));
			if (p.age % 1200 < 40) WishingLampItem.grantWish(p, false);
		}
	}

	/** Melee hits by a player who ate certain essences. */
	public static void onPlayerHit(ServerPlayerEntity p, LivingEntity target) {
		GhostData d = GhostState.of(p.getServer()).data(p.getUuid());
		if (d.eaten == 0) return;
		ServerWorld w = p.getServerWorld();
		if ((d.eaten & (1 << ESS_JAILER)) != 0) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), p);
			w.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
		}
		if ((d.eaten & (1 << ESS_REV)) != 0) {
			target.setOnFireFor(4);
			w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
		}
		if ((d.eaten & (1 << ESS_SHADE)) != 0) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0), p);
			p.heal(1.5f);
			w.spawnParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 1, target.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
		}
		if ((d.eaten & (1 << ESS_ROXY)) != 0) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), p);
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0), p);
			p.heal(1.0f);
			w.spawnParticles(ParticleTypes.HEART, target.getX(), target.getY() + target.getHeight(), target.getZ(), 3, 0.3, 0.2, 0.3, 0.05);
		}
		if ((d.eaten & (1 << ESS_DOLORA)) != 0) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 0), p);
			p.heal(1.0f);
			if (p.getAbsorptionAmount() < 6f) p.setAbsorptionAmount(Math.min(6f, p.getAbsorptionAmount() + 1f));
			w.spawnParticles(new net.minecraft.particle.DustParticleEffect(new org.joml.Vector3f(0.65f, 0.4f, 1f), 1.2f),
					target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
		}
		if ((d.eaten & (1 << ESS_VOLTAGE)) != 0 && ++d.hitCount % 4 == 0) {
			net.minecraft.entity.LightningEntity bolt = net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(w);
			if (bolt != null) {
				bolt.setCosmetic(true);
				bolt.refreshPositionAfterTeleport(target.getPos());
				w.spawnEntity(bolt);
			}
			target.damage(w.getDamageSources().indirectMagic(p, p), 6f);
		}
	}

	// ------------------------------------------------------------ ghost level

	/** Ghost level goes up as you earn stat points (one level per 2 points, max 30). */
	public static int level(GhostData d) {
		return Math.min(30, 1 + d.totalPoints / 2);
	}

	public static float statBonusHealth(GhostData d) {
		return Math.min(40f, 4f + 2f * (level(d) - 1));
	}

	public static double statBonusDamage(GhostData d) {
		return Math.min(18.0, 1.0 + 0.6 * (level(d) - 1));
	}

	public static int strengthAmp(GhostData d) {
		return Math.min(4, (level(d) - 1) / 7);
	}

	public static double statBonusArmor(GhostData d) {
		return Math.min(14.0, 2.0 + 0.5 * (level(d) - 1));
	}

	private static final Identifier MOD_HEALTH = PhantomMod.id("ghost_health");
	private static final Identifier MOD_DAMAGE = PhantomMod.id("ghost_strength");
	private static final Identifier MOD_ARMOR = PhantomMod.id("ghost_armor");
	private static final Identifier MOD_TOUGH = PhantomMod.id("ghost_toughness");
	private static final Identifier MOD_KNOCK = PhantomMod.id("ghost_knockback");

	/** Ghost form: extra health, strength and toughness, growing with your ghost level. */
	public static void applyStats(ServerPlayerEntity p, GhostData d, boolean on) {
		int lvl = level(d);
		setMod(p, EntityAttributes.GENERIC_MAX_HEALTH, MOD_HEALTH, on ? statBonusHealth(d) : 0);
		setMod(p, EntityAttributes.GENERIC_ATTACK_DAMAGE, MOD_DAMAGE, on ? statBonusDamage(d) : 0);
		setMod(p, EntityAttributes.GENERIC_ARMOR, MOD_ARMOR, on ? statBonusArmor(d) : 0);
		setMod(p, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, MOD_TOUGH, on ? Math.min(8.0, lvl * 0.3) : 0);
		setMod(p, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, MOD_KNOCK, on ? Math.min(0.6, 0.1 + lvl * 0.02) : 0);
		if (on && lvl >= 5) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, lvl >= 15 ? 1 : 0, true, false, true));
		}
		// Ghost strength: Strength I from the start, one level stronger every 7 ghost levels (up to Strength V)
		if (on) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, strengthAmp(d), true, false, true));
		} else {
			StatusEffectInstance st = p.getStatusEffect(StatusEffects.STRENGTH);
			if (st != null && st.isAmbient() && st.getDuration() <= 60) p.removeStatusEffect(StatusEffects.STRENGTH);
		}
		if (!on && p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
	}

	public static void setMod(ServerPlayerEntity p, RegistryEntry<EntityAttribute> attr, Identifier id, double value) {
		EntityAttributeInstance inst = p.getAttributeInstance(attr);
		if (inst == null) return;
		EntityAttributeModifier cur = inst.getModifier(id);
		if (cur != null && cur.value() == value) return;
		if (cur != null) inst.removeModifier(id);
		if (value != 0) inst.addTemporaryModifier(new EntityAttributeModifier(id, value, EntityAttributeModifier.Operation.ADD_VALUE));
	}

	private static final float GHOST_FLY_SPEED = 0.08f;

	private static void setFlight(ServerPlayerEntity p, boolean on) {
		PlayerAbilities a = p.getAbilities();
		if (on) {
			// Ghost flight is faster than creative flight: fly like Superman
			boolean changed = !a.allowFlying || (!p.isCreative() && a.getFlySpeed() != GHOST_FLY_SPEED);
			a.allowFlying = true;
			if (!p.isCreative()) a.setFlySpeed(GHOST_FLY_SPEED);
			if (changed) p.sendAbilitiesUpdate();
		} else if (!p.isCreative() && !p.isSpectator()) {
			a.allowFlying = false;
			a.flying = false;
			a.setFlySpeed(0.05f);
			p.sendAbilitiesUpdate();
		}
	}

	/** The two glowing transformation rings, drawn by every nearby client. */
	private static void sendRings(ServerPlayerEntity p, int duration) {
		sendRings(p, duration, 0);
	}

	public static void sendRings(ServerPlayerEntity p, int duration, int style) {
		RingPayload ring = new RingPayload(p.getId(), duration, style);
		for (ServerPlayerEntity viewer : PlayerLookup.around(p.getServerWorld(), p.getPos(), 96)) {
			if (ServerPlayNetworking.canSend(viewer, RingPayload.ID)) ServerPlayNetworking.send(viewer, ring);
		}
	}

	/** Hold V: a short cutscene. The camera turns to face you, the rings sweep slowly, then you change. */
	private static void startCutscene(ServerPlayerEntity p, GhostData d) {
		if (d.cutscene > 0) return;
		if (!d.powers) {
			bar(p, "You don't have ghost powers yet. Activate a Ghost Portal!", Formatting.RED);
			return;
		}
		if (!d.transformed && d.exhaust > 0) {
			bar(p, "Too drained from the wail... (" + (d.exhaust / 20 + 1) + "s)", Formatting.RED);
			return;
		}
		if (!d.transformed && d.suitOn) HunterSuit.toggle(p, d, false);
		// the dark half-ghost gets its own, much more sinister transformation
		if (!d.transformed && Cutscenes.startDark(p, d)) return;
		// ...and the hero gets an aura-farming one of his own
		if (!d.transformed) {
			Cutscenes.startHero(p, d);
			return;
		}
		// going back to human: each form has its own way of letting go
		if (d.dark && d.darkMode) Cutscenes.startDarkOff(p, d);
		else Cutscenes.startHeroOff(p, d);
		return;
	}

	@SuppressWarnings("unused")
	private static void oldCutscene(ServerPlayerEntity p, GhostData d) {
		d.cutscene = CUTSCENE_TICKS;
		d.cutsceneKind = 0;
		d.cutsceneToGhost = !d.transformed;
		d.cutsceneAnchor = p.getPos();
		sendRings(p, CUTSCENE_TICKS - 8, d.cutsceneToGhost ? 0 : 4);
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 0.8f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.0f, 1.2f);
		syncNow(p);
	}

	/**
	 * Hold the wail long enough and it becomes a cutscene: the camera swings round to face you, you rise up
	 * and unleash everything you've got in every direction... then it drains you back to human form.
	 */
	private static void startWailFinale(ServerPlayerEntity p, GhostData d) {
		d.wailTicks = 0;
		d.wailHeld = false;
		d.cutscene = WAIL_FINALE_TICKS;
		d.cutsceneKind = 2;
		d.cutsceneToGhost = false;
		d.cutsceneAnchor = p.getPos();
		sendRings(p, WAIL_FINALE_TICKS - 30, 2); // the screaming pose, seen by everyone nearby
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.PLAYERS, 6.0f, 0.85f);
		syncNow(p);
	}

	private static void tickWailFinale(ServerPlayerEntity p, GhostData d, ServerWorld w) {
		int t = WAIL_FINALE_TICKS - d.cutscene;
		if (d.cutscene > 30) {
			Vec3d eye = p.getEyePos();
			Vec3d look = p.getRotationVec(1f);
			if (t % 6 == 1) {
				// Waves blast out all around you, not just ahead
				for (int k = 0; k < 4; k++) {
					Vec3d dir = look.rotateY((float) (k * Math.PI / 2));
					WAVES.add(new WailWave(w, p.getUuid(), eye, dir));
				}
			}
			if (t % 8 == 0) {
				for (int k = 0; k < 4; k++) {
					Vec3d dir = look.rotateY((float) (k * Math.PI / 2 + t * 0.1));
					com.phantompowers.entity.BossMoves.wailWave(w, eye.add(dir.multiply(0.6)), dir, 22f);
				}
			}
			if (t % 18 == 0) {
				w.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.PLAYERS, 6.0f, 0.9f + w.random.nextFloat() * 0.15f);
			}
			// Kept light so the cutscene stays smooth and you can still see yourself
			if (t % 2 == 0) w.spawnParticles(GREEN, eye.x, eye.y, eye.z, 4, 1.4, 0.8, 1.4, 0.2);

		}
		if (d.cutscene == 30) {
			// Out of power: the rings come back and take you human again
			sendRings(p, 22, 3);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.5f, 0.6f);
		}
	}

	/** Hold U: the suit-up cutscene. */
	private static void startSuitCutscene(ServerPlayerEntity p, GhostData d) {
		if (d.cutscene > 0) return;
		if (!d.suitOn && !HunterSuit.canDeploy(p, d)) return;
		if (!d.suitOn) {
			Cutscenes.startSuit(p, d);
			return;
		}
		if (d.suitOn) {
			Cutscenes.startSuitOff(p, d);
			return;
		}
		d.cutscene = CUTSCENE_TICKS;
		d.cutsceneKind = 1;
		d.cutsceneToGhost = !d.suitOn;
		d.cutsceneAnchor = p.getPos();
		sendRings(p, CUTSCENE_TICKS - 8, d.cutsceneToGhost ? 1 : 5);
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 1.3f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(), SoundCategory.PLAYERS, 1.0f, 0.8f);
		syncNow(p);
	}

	private static void tickCutscene(ServerPlayerEntity p, GhostData d) {
		if (d.cutscene <= 0) return;
		if (d.cutsceneKind == 3) {
			DarkPowers.tickPortalCutscene(p, d);
			return;
		}
		if (d.cutsceneKind == 4) {
			DarkPowers.tickSpinCutscene(p, d);
			return;
		}
		if (d.cutsceneKind == 5) {
			Cutscenes.tickDark(p, d);
			return;
		}
		if (d.cutsceneKind == 6) {
			Cutscenes.tickOrigin(p, d);
			return;
		}
		if (d.cutsceneKind == 7) {
			Cutscenes.tickHero(p, d);
			return;
		}
		if (d.cutsceneKind == 8) {
			Cutscenes.tickSuit(p, d);
			return;
		}
		if (d.cutsceneKind == 9) {
			Cutscenes.tickHeroOff(p, d);
			return;
		}
		if (d.cutsceneKind == 10) {
			Cutscenes.tickDarkOff(p, d);
			return;
		}
		if (d.cutsceneKind == 11) {
			Cutscenes.tickSuitOff(p, d);
			return;
		}
		d.cutscene--;
		// Hold still (and float a tiny bit) while the rings pass over you
		if (d.cutsceneAnchor != null) {
			double lift = d.cutsceneKind == 2 ? (d.cutscene > 30 ? Math.min(1.2, (WAIL_FINALE_TICKS - d.cutscene) * 0.04) : 1.2 * d.cutscene / 30.0)
					: d.cutsceneToGhost ? Math.min(0.6, (CUTSCENE_TICKS - d.cutscene) * 0.015) : 0;
			// The client floats you smoothly; the server only steps in if you drift away
			Vec3d want = new Vec3d(d.cutsceneAnchor.x, d.cutsceneAnchor.y + lift, d.cutsceneAnchor.z);
			if (p.getPos().squaredDistanceTo(want) > 2.25) p.requestTeleport(want.x, want.y, want.z);
			p.fallDistance = 0;
		}
		ServerWorld w = p.getServerWorld();
		if (d.cutsceneKind == 2) tickWailFinale(p, d, w);
		if (d.cutscene % 4 == 0) {
			w.spawnParticles(d.cutsceneToGhost ? GREEN : PALE, p.getX(), p.getY() + 1, p.getZ(), 2, 0.7, 0.9, 0.7, 0);
		}
		if (d.cutscene == 8) {
			if (d.cutsceneKind == 1) {
				if (d.cutsceneToGhost != d.suitOn) HunterSuit.toggle(p, d, false);
			} else if (d.cutsceneToGhost != d.transformed) toggleTransform(p, d, false);
			if (d.cutsceneKind == 2) {
				// The wail took everything you had
				d.wailCooldown = 260;
				d.exhaust = Math.max(d.exhaust, 120);
				d.energy = 0;
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 120, 0, false, false, true));
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1, false, false, true));
				bar(p, "The wail drained every last drop of ghost energy...", Formatting.GRAY);
			}
			w.spawnParticles(d.cutsceneToGhost ? GREEN : PALE, p.getX(), p.getY() + 1, p.getZ(), 24, 0.8, 1.0, 0.8, 0.04);
			w.spawnParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.2f, 1.0f);
		}
		if (d.cutscene == 0) {
			d.cutsceneAnchor = null;
			syncNow(p);
		}
	}

	// ------------------------------------------------------------------ blast

	private static void blast(ServerPlayerEntity p, GhostData d) {
		if (d.blastCooldown > 0) return;
		if (!useEnergy(p, d, BLAST_COST)) return;
		d.blastCooldown = 8;

		ServerWorld w = p.getServerWorld();
		Vec3d start = p.getEyePos();
		Vec3d dir = p.getRotationVec(1f);
		Entity hit = aimEntity(p, d, 32);
		Vec3d end = hit != null ? hit.getBoundingBox().getCenter() : aimBlockPos(p, 32);

		Vec3d right = dir.crossProduct(new Vec3d(0, 1, 0));
		if (right.lengthSquared() < 1.0E-4) right = new Vec3d(1, 0, 0);
		Vec3d from = start.add(dir.multiply(0.6)).add(right.normalize().multiply(0.3)).add(0, -0.25, 0);
		BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) end.x, (float) end.y, (float) end.z, d.dark && d.darkMode ? DarkPowers.PINK : 0x33FF77);
		for (ServerPlayerEntity viewer : PlayerLookup.around(w, start, 96)) {
			if (ServerPlayNetworking.canSend(viewer, BeamPayload.ID)) ServerPlayNetworking.send(viewer, beam);
		}
		sound(p, SoundEvents.ENTITY_BLAZE_SHOOT, 0.8f, 1.7f);

		if (hit != null) {
			hit.damage(w.getDamageSources().indirectMagic(p, p), 7.0f);
			if (hit instanceof LivingEntity le) le.takeKnockback(0.6, -dir.x, -dir.z);
		}
	}

	// ---------------------------------------------------------- intangibility

	private static void intangibility(ServerPlayerEntity p, GhostData d) {
		if (p.isSneaking()) {
			if (d.holding == null && !HELD_BLOCKS.containsKey(p.getUuid())) phaseTarget(p, d);
			return;
		}
		if (d.intangible) {
			stopIntangible(p, d, true);
			return;
		}
		d.intangible = true;
		GhostFlags.SELF.add(p.getUuid());
		bar(p, "Intangible! Walk through walls. Sneak to sink through the floor, fly up to rise. G again to stop.", Formatting.AQUA);
		sound(p, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);
		p.getServerWorld().spawnParticles(PALE, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0);
	}

	public static void stopIntangible(ServerPlayerEntity p, GhostData d, boolean message) {
		if (!d.intangible) return;
		d.intangible = false;
		GhostFlags.SELF.remove(p.getUuid());
		if (!GhostFlags.TEMP.containsKey(p.getUuid())) pushOut(p);
		if (message) bar(p, "You're solid again.", Formatting.GRAY);
		d.syncTimer = 0;
	}

	/** Sneak + hold G: keep the mob, player or wall you're looking at intangible for as long as you hold on. */
	private static void phaseTarget(ServerPlayerEntity p, GhostData d) {
		ServerWorld w = p.getServerWorld();
		Entity target = aimEntity(p, d, 16);
		if (target != null && !(target instanceof EnderDragonEntity)) {
			GhostFlags.TEMP.put(target.getUuid(), new GhostFlags.TempPhase(target, 600));
			d.holding = target;
			GhostFlags.HOLDERS.add(p.getUuid());
			phaseSelfForHold(p, d);
			if (target instanceof MobEntity mob) {
				d.heldPrevAi = mob.isAiDisabled();
				mob.setAiDisabled(true);
			}
			w.spawnParticles(PALE, target.getX(), target.getY() + target.getHeight() / 2, target.getZ(), 30, 0.4, 0.6, 0.4, 0);
			sound(p, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);
			bar(p, "Carrying " + target.getName().getString() + " intangible. Let go of G to drop them.", Formatting.AQUA);
			if (target instanceof ServerPlayerEntity tp) {
				tp.sendMessage(Text.literal(p.getName().getString() + " turned you intangible!").formatted(Formatting.AQUA), true);
				syncNow(tp);
			}
			return;
		}

		BlockHitResult bh = w.raycast(new RaycastContext(p.getEyePos(), p.getEyePos().add(p.getRotationVec(1f).multiply(8)),
				RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, p));
		if (bh.getType() != HitResult.Type.BLOCK) {
			bar(p, "Look at a mob, player or wall to phase it.", Formatting.GRAY);
			return;
		}
		int phased = 0;
		List<PhasedBlock> held = new ArrayList<>();
		BlockPos hitPos = bh.getBlockPos();
		for (BlockPos pos : new BlockPos[]{hitPos.down(), hitPos, hitPos.up()}) {
			BlockState s = w.getBlockState(pos);
			if (s.isAir() || s.hasBlockEntity() || s.isOf(ModBlocks.GHOST_PORTAL)) continue;
			if (s.getHardness(w, pos) < 0) continue;
			held.add(new PhasedBlock(w, pos.toImmutable(), s, 600));
			w.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			w.spawnParticles(PALE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0);
			phased++;
		}
		if (phased > 0) {
			HELD_BLOCKS.put(p.getUuid(), held);
			GhostFlags.HOLDERS.add(p.getUuid());
			phaseSelfForHold(p, d);
			sound(p, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);
			bar(p, "Holding the wall intangible. Let go of the key to make it solid again.", Formatting.AQUA);
		}
	}

	/** Let go of whatever you're holding intangible. */
	public static void releaseHold(ServerPlayerEntity p, GhostData d, boolean message) {
		boolean released = false;
		GhostFlags.HOLDERS.remove(p.getUuid());
		d.syncTimer = 0;
		if (d.holding != null) {
			Entity e = d.holding;
			d.holding = null;
			if (e instanceof MobEntity mob) mob.setAiDisabled(d.heldPrevAi);
			GhostFlags.TempPhase tp = GhostFlags.TEMP.get(e.getUuid());
			if (tp != null) tp.ticks = 0; // expires next tick (pushes them out of walls)
			released = true;
		}
		List<PhasedBlock> blocks = HELD_BLOCKS.remove(p.getUuid());
		if (blocks != null) {
			for (PhasedBlock b : blocks) b.restore();
			released = true;
		}
		if (d.holdMadeIntangible) {
			d.holdMadeIntangible = false;
			stopIntangible(p, d, false);
		}
		if (released && message) bar(p, "Released.", Formatting.GRAY);
	}

	/** Whoever you phase, you phase with them. */
	private static void phaseSelfForHold(ServerPlayerEntity p, GhostData d) {
		d.syncTimer = 0;
		if (d.intangible) return;
		d.intangible = true;
		d.holdMadeIntangible = true;
		GhostFlags.SELF.add(p.getUuid());
		d.syncTimer = 0;
	}

	private static void tickHold(ServerPlayerEntity p, GhostData d) {
		boolean holding = d.holding != null || HELD_BLOCKS.containsKey(p.getUuid());
		if (!holding) return;
		if (d.holding != null) {
			Entity e = d.holding;
			if (e.isRemoved() || !e.isAlive() || e.getWorld() != p.getWorld() || e.squaredDistanceTo(p) > 20 * 20
					|| !GhostFlags.TEMP.containsKey(e.getUuid())) {
				releaseHold(p, d, true);
				return;
			}
			// Carry them along, floating just in front of you
			Vec3d look = p.getRotationVec(1f);
			Vec3d flat = new Vec3d(look.x, 0, look.z);
			flat = flat.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : flat.normalize();
			double dist = 1.2 + e.getWidth();
			Vec3d at = p.getPos().add(flat.multiply(dist)).add(0, Math.max(0, look.y) * 1.5, 0);
			if (e instanceof ServerPlayerEntity held) {
				held.requestTeleport(at.x, at.y, at.z);
			} else {
				e.refreshPositionAndAngles(at.x, at.y, at.z, p.getYaw() + 180f, 0f);
				e.setVelocity(Vec3d.ZERO);
			}
			e.fallDistance = 0;
			if (p.age % 3 == 0) {
				p.getServerWorld().spawnParticles(PALE, e.getX(), e.getY() + e.getHeight() / 2, e.getZ(), 2, 0.3, 0.4, 0.3, 0);
			}
		}
		List<PhasedBlock> blocks = HELD_BLOCKS.get(p.getUuid());
		if (blocks != null && (blocks.get(0).world() != p.getWorld() || blocks.get(0).pos().getSquaredDistance(p.getPos()) > 20 * 20)) {
			releaseHold(p, d, true);
			return;
		}
	}

	// ----------------------------------------------------------- invisibility

	private static void toggleInvisible(ServerPlayerEntity p, GhostData d) {
		if (d.invisible) {
			stopInvisible(p, d);
			bar(p, "You're visible again.", Formatting.GRAY);
			return;
		}
		d.invisible = true;
		GhostFlags.INVISIBLE.add(p.getUuid());
		broadcastStates(p.getServer());
		p.getServerWorld().spawnParticles(PALE, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0);
		sound(p, SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.3f);
		bar(p, "Invisible! Press J again to reappear.", Formatting.AQUA);
	}

	public static void stopInvisible(ServerPlayerEntity p, GhostData d) {
		if (!d.invisible) return;
		d.invisible = false;
		GhostFlags.INVISIBLE.remove(p.getUuid());
		p.removeStatusEffect(StatusEffects.INVISIBILITY);
		broadcastStates(p.getServer());
		d.syncTimer = 0;
	}

	/** Tells every client who is invisible and who is in ghost form (for the ghost look). */
	public static void broadcastStates(MinecraftServer server) {
		GhostState st = GhostState.of(server);
		List<UUID> ghosts = new ArrayList<>();
		for (ServerPlayerEntity pl : server.getPlayerManager().getPlayerList()) {
			if (st.data(pl.getUuid()).transformed) ghosts.add(pl.getUuid());
		}
		List<UUID> suited = new ArrayList<>();
		for (ServerPlayerEntity pl : server.getPlayerManager().getPlayerList()) {
			if (st.data(pl.getUuid()).suitOn) suited.add(pl.getUuid());
		}
		List<UUID> fists = new ArrayList<>();
		for (ServerPlayerEntity pl : server.getPlayerManager().getPlayerList()) {
			GhostData gd = st.data(pl.getUuid());
			if (gd.fists && gd.transformed) fists.add(pl.getUuid());
		}
		List<UUID> shielded = new ArrayList<>(), colorIds = new ArrayList<>(), dark = new ArrayList<>(), acne = new ArrayList<>(), cannons = new ArrayList<>(),
				aiming = new ArrayList<>();
		List<int[]> colors = new ArrayList<>();
		for (ServerPlayerEntity pl : server.getPlayerManager().getPlayerList()) {
			GhostData gd = st.data(pl.getUuid());
			if (gd.shieldUp) shielded.add(pl.getUuid());
			colorIds.add(pl.getUuid());
			colors.add(gd.suitColors);
			if (gd.dark && gd.darkMode) dark.add(pl.getUuid());
			if (gd.acneTicks > 0) acne.add(pl.getUuid());
			if (gd.cannons && gd.suitOn) cannons.add(pl.getUuid());
			if (gd.missileAim && gd.suitOn) aiming.add(pl.getUuid());
		}
		InvisiblePayload payload = new InvisiblePayload(new ArrayList<>(GhostFlags.INVISIBLE), ghosts, suited, new ArrayList<>(FLYING), fists,
				shielded, colorIds, colors, dark, acne, cannons, aiming);
		for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
			if (ServerPlayNetworking.canSend(viewer, InvisiblePayload.ID)) ServerPlayNetworking.send(viewer, payload);
		}
	}

	// ------------------------------------------------------------- possession

	private static void possession(ServerPlayerEntity p, GhostData d, GhostState st) {
		if (d.possessing != null) {
			stopPossess(p, d, true);
			return;
		}
		if (d.energy < POSSESS_START_COST + 5) {
			bar(p, "Not enough ghost energy!", Formatting.RED);
			return;
		}
		Entity target = aimEntity(p, d, 16);
		if (!(target instanceof LivingEntity) || target instanceof EnderDragonEntity) {
			bar(p, "Look at a mob or player to possess it.", Formatting.GRAY);
			return;
		}
		if (GhostFlags.HOSTS.contains(target.getUuid())) {
			bar(p, "Someone is already possessing that!", Formatting.RED);
			return;
		}
		if (target instanceof ServerPlayerEntity tp && st.data(tp.getUuid()).intangible) {
			bar(p, "They're intangible, you can't grab them!", Formatting.RED);
			return;
		}
		d.energy -= POSSESS_START_COST;
		beginPossess(p, d, target);
	}

	/** Overshadow someone straight away (the story uses this when you fly into somebody by accident). */
	public static void forcePossess(ServerPlayerEntity p, LivingEntity target) {
		GhostData d = GhostState.of(p.getServer()).data(p.getUuid());
		if (d.possessing != null || GhostFlags.HOSTS.contains(target.getUuid())) return;
		beginPossess(p, d, target);
		syncNow(p);
	}

	private static void beginPossess(ServerPlayerEntity p, GhostData d, Entity target) {
		stopIntangible(p, d, false);
		d.possessing = target;
		d.possessTicks = 0;
		GhostFlags.HOSTS.add(target.getUuid());
		GhostFlags.POSSESSORS.add(p.getUuid());
		calmHostiles(p, target);
		if (target instanceof MobEntity mob) {
			d.prevAiDisabled = mob.isAiDisabled();
			mob.setAiDisabled(true);
		}
		ServerWorld w = p.getServerWorld();
		p.teleport(w, target.getX(), target.getY(), target.getZ(), target.getYaw(), target.getPitch());
		w.spawnParticles(GREEN, target.getX(), target.getY() + target.getHeight() / 2, target.getZ(), 40, 0.4, 0.6, 0.4, 0);
		sound(p, SoundEvents.ENTITY_VEX_CHARGE, 1.0f, 0.7f);
		bar(p, "You possessed " + target.getName().getString() + "! R: " + HostPowers.NAMES[HostPowers.kind(target)] + ". Use Possession again to leave.", Formatting.GREEN);
		if (target instanceof ServerPlayerEntity tp) {
			tp.sendMessage(Text.literal("You've been possessed by " + p.getName().getString() + "!").formatted(Formatting.GREEN), true);
		}
	}

	private static void tickPossession(ServerPlayerEntity p, GhostData d) {
		Entity t = d.possessing;
		if (t == null) return;
		if (t.isRemoved() || !t.isAlive() || t.getWorld() != p.getWorld() || !p.isAlive()) {
			stopPossess(p, d, true);
			return;
		}
		d.possessTicks++;
		if (t instanceof PlayerEntity && d.possessTicks > 200) {
			stopPossess(p, d, false);
			bar(p, "They forced you out!", Formatting.YELLOW);
			return;
		}
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 10, 0, false, false, false));
		if (t instanceof ServerPlayerEntity tp) {
			if (tp.squaredDistanceTo(p) > 0.0025 || Math.abs(tp.getYaw() - p.getYaw()) > 1f)
				tp.networkHandler.requestTeleport(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
		} else {
			// The body goes wherever you go
			t.setVelocity(Vec3d.ZERO);
			t.setPosition(p.getX(), p.getY(), p.getZ());
			t.setYaw(p.getYaw());
			t.setPitch(p.getPitch());
			t.setHeadYaw(p.getHeadYaw());
			t.setBodyYaw(p.getYaw());
		}
		t.fallDistance = 0;
		if (d.possessTicks % 5 == 0) calmHostiles(p, t);
		if (d.possessTicks % 4 == 0) {
			p.getServerWorld().spawnParticles(GREEN, t.getX(), t.getY() + t.getHeight() * 0.6, t.getZ(), 3, 0.3, 0.4, 0.3, 0);
		}
	}

	private static boolean hostCanFly(Entity e) {
		return e instanceof net.minecraft.entity.mob.FlyingEntity || e instanceof net.minecraft.entity.passive.BatEntity
				|| e instanceof net.minecraft.entity.passive.BeeEntity || e instanceof net.minecraft.entity.mob.VexEntity
				|| e instanceof net.minecraft.entity.passive.ParrotEntity || e instanceof net.minecraft.entity.passive.AllayEntity
				|| e instanceof net.minecraft.entity.mob.BlazeEntity || e instanceof net.minecraft.entity.boss.WitherEntity
				|| e instanceof com.phantompowers.entity.AbstractGhostEntity || FrostLogic.isBoss(e);
	}

	/** Hostile mobs forget about you and the body you're hiding in. */
	private static void calmHostiles(ServerPlayerEntity p, Entity host) {
		Box box = p.getBoundingBox().expand(48);
		for (MobEntity m : p.getServerWorld().getEntitiesByClass(MobEntity.class, box, m -> m != host)) {
			LivingEntity target = m.getTarget();
			if (target == p || target == host) m.setTarget(null);
			if (m.getBrain().hasMemoryModule(MemoryModuleType.ATTACK_TARGET)) {
				m.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(t -> {
					if (t == p || t == host) m.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
				});
			}
		}
	}

	public static void stopPossess(ServerPlayerEntity p, GhostData d, boolean message) {
		Entity t = d.possessing;
		if (t == null) return;
		d.possessing = null;
		GhostFlags.HOSTS.remove(t.getUuid());
		GhostFlags.POSSESSORS.remove(p.getUuid());
		if (t instanceof MobEntity mob) mob.setAiDisabled(d.prevAiDisabled);
		if (!(t instanceof PlayerEntity)) t.noClip = false;
		if (t.isAlive() && !t.isRemoved()) pushOut(t);
		p.removeStatusEffect(StatusEffects.INVISIBILITY);
		if (p.isAlive()) pushOut(p);
		if (message) bar(p, "You left your host's body.", Formatting.GRAY);
		d.syncTimer = 0;
	}

	// ------------------------------------------------------------------- wail

	private static void wail(ServerPlayerEntity p, GhostData d) {
		if (d.wailTicks > 0) return;
		if (d.wailCooldown > 0) {
			bar(p, "Your voice needs to recover... (" + (d.wailCooldown / 20 + 1) + "s)", Formatting.RED);
			return;
		}
		if (d.energy < 20) {
			bar(p, "Not enough ghost energy!", Formatting.RED);
			return;
		}
		d.wailHeld = true;
		d.wailTicks = 1;
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.PLAYERS, 4.0f, 1.0f);
		bar(p, "GHOSTLY WAIL! (hold B to keep wailing)", Formatting.GREEN);
	}

	/** While B is held the wail keeps going: a new shockwave every few ticks, draining energy. */
	private static void tickWail(ServerPlayerEntity p, GhostData d) {
		if (d.wailTicks <= 0) return;
		ServerWorld w = p.getServerWorld();
		if (d.wailHeld && d.wailTicks >= WAIL_FINALE_AT && d.transformed && p.isAlive() && d.cutscene == 0) {
			startWailFinale(p, d);
			return;
		}
		boolean keepGoing = d.transformed && p.isAlive() && d.energy > 0
				&& (d.wailHeld || d.wailTicks < MIN_WAIL_TICKS) && d.wailTicks < MAX_WAIL_TICKS;
		if (!keepGoing) {
			int lasted = d.wailTicks;
			d.wailTicks = 0;
			d.wailHeld = false;
			d.wailCooldown = 160 + lasted;
			// Like the show: the wail drains you right back to human form.
			endForm(p, d);
			d.exhaust = 60 + lasted / 2;
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 120, 0, false, false, true));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 1, false, false, true));
			return;
		}
		if (d.wailTicks % 5 == 1) {
			WAVES.add(new WailWave(w, p.getUuid(), p.getEyePos(), p.getRotationVec(1f)));
			// your clones wail with you, all aimed at the spot you're aiming at
			if (d.wailTicks % 10 == 1) com.phantompowers.entity.GhostCloneEntity.wailWith(p, aimPoint(p, 24));
		}
		if (d.wailTicks % 56 == 0) {
			w.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.PLAYERS, 4.0f, 0.95f + w.random.nextFloat() * 0.1f);
		}
		d.energy = Math.max(0, d.energy - 0.55f);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5, 2, false, false, false));
		d.wailTicks++;
	}

	private static final class WailWave {
		final ServerWorld world;
		final UUID owner;
		final Vec3d origin;
		final Vec3d dir;
		final Set<UUID> hit = new HashSet<>();
		int radius;
		int broken;

		WailWave(ServerWorld world, UUID owner, Vec3d origin, Vec3d dir) {
			this.world = world;
			this.owner = owner;
			this.origin = origin;
			this.dir = dir.normalize();
			com.phantompowers.entity.BossMoves.wailWave(world, origin.add(this.dir.multiply(0.5)), this.dir, 16f);
		}

		/** Returns true when finished. */
		boolean tick() {
			radius++;
			if (radius > 16) return true;
			Entity ownerEntity = world.getServer().getPlayerManager().getPlayer(owner);
			double cone = 0.80;

			// (the glowing green rings are drawn by each client; see ClientShockwaves)

			// Break blocks in this shell of the cone
			BlockPos o = BlockPos.ofFloored(origin);
			BlockPos.Mutable m = new BlockPos.Mutable();
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dy = -radius; dy <= radius; dy++) {
					for (int dz = -radius; dz <= radius; dz++) {
						if (broken >= 150) break;
						m.set(o.getX() + dx, o.getY() + dy, o.getZ() + dz);
						Vec3d diff = new Vec3d(m.getX() + 0.5 - origin.x, m.getY() + 0.5 - origin.y, m.getZ() + 0.5 - origin.z);
						double dist = diff.length();
						if (dist < radius - 1 || dist >= radius || dist < 1.5) continue;
						if (diff.multiply(1.0 / dist).dotProduct(dir) < cone) continue;
						BlockState s = world.getBlockState(m);
						if (s.isAir() || s.hasBlockEntity()) continue;
						if (s.isOf(ModBlocks.GHOST_PORTAL) || s.isOf(Blocks.BLACK_CONCRETE) || s.isOf(Blocks.IRON_BLOCK)) continue;
						float hardness = s.getHardness(world, m);
						if (hardness < 0 || hardness > 3.5f) continue;
						world.breakBlock(m.toImmutable(), world.random.nextFloat() < 0.25f, ownerEntity);
						broken++;
					}
				}
			}

			// Blast away entities caught in this shell
			Box box = new Box(origin, origin).expand(radius + 1);
			for (Entity e : world.getOtherEntities(ownerEntity, box)) {
				if (hit.contains(e.getUuid()) || !(e instanceof LivingEntity)) continue;
				Vec3d diff = e.getBoundingBox().getCenter().subtract(origin);
				double dist = diff.length();
				if (dist > radius + 0.5 || dist < 0.1) continue;
				if (diff.multiply(1.0 / dist).dotProduct(dir) < cone - 0.1) continue;
				hit.add(e.getUuid());
				DamageSource src = ownerEntity != null
						? world.getDamageSources().sonicBoom(ownerEntity)
						: world.getDamageSources().magic();
				e.damage(src, 5.0f);
				e.addVelocity(dir.x * 2.0, 0.4 + Math.max(0, dir.y), dir.z * 2.0);
				e.velocityModified = true;
			}
			return false;
		}
	}

	// ---------------------------------------------------------------- tornado

	private static void tornado(ServerPlayerEntity p, GhostData d) {
		if (d.tornadoCooldown > 0) {
			bar(p, "The tornado needs to recharge... (" + (d.tornadoCooldown / 20 + 1) + "s)", Formatting.RED);
			return;
		}
		if (!useEnergy(p, d, TORNADO_COST)) return;
		d.tornadoCooldown = 1200;
		ServerWorld w = p.getServerWorld();
		Vec3d look = p.getRotationVec(1f);
		Vec3d flat = new Vec3d(look.x, 0, look.z);
		if (flat.lengthSquared() < 1.0E-4) flat = new Vec3d(0, 0, 1);
		flat = flat.normalize();
		TORNADOES.add(new Tornado(w, p.getUuid(), p.getPos().add(flat.multiply(6)), flat));
		w.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.PLAYERS, 4.0f, 0.85f);
		bar(p, "WAIL TORNADO!", Formatting.GREEN);

		endForm(p, d);
		d.exhaust = 160;
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 160, 0, false, false, true));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 140, 1, false, false, true));
	}

	/** A giant spinning column of wail energy that roams forward, sucking up mobs and ripping up blocks. */
	private static final class Tornado {
		final ServerWorld world;
		final UUID owner;
		final Vec3d dir;
		final int id = ++tornadoIds;
		Vec3d pos;
		int age;
		int lifted;

		/** Tell nearby players where to draw the funnel. */
		void send(boolean done) {
			com.phantompowers.network.TornadoPayload msg = new com.phantompowers.network.TornadoPayload(id, pos.x, pos.y, pos.z, age, done, 1f);
			for (ServerPlayerEntity viewer : PlayerLookup.around(world, pos, 128)) {
				if (ServerPlayNetworking.canSend(viewer, com.phantompowers.network.TornadoPayload.ID)) ServerPlayNetworking.send(viewer, msg);
			}
		}

		Tornado(ServerWorld world, UUID owner, Vec3d pos, Vec3d dir) {
			this.world = world;
			this.owner = owner;
			this.pos = pos;
			this.dir = dir;
		}

		boolean tick() {
			age++;
			Entity ownerEntity = world.getServer().getPlayerManager().getPlayer(owner);
			if (age > 200) {
				send(true);
				finalBurst(ownerEntity);
				return true;
			}

			// Drift forward and hug the ground
			pos = pos.add(dir.multiply(0.12));
			BlockPos bp = BlockPos.ofFloored(pos);
			for (int dy = 4; dy >= -10; dy--) {
				BlockPos q = bp.up(dy);
				if (!world.getBlockState(q).getCollisionShape(world, q).isEmpty()) {
					pos = new Vec3d(pos.x, q.getY() + 1, pos.z);
					break;
				}
			}

			// The funnel itself is drawn by each client (much smoother than particles); the server just sends where it is
			send(false);
			if (age % 6 == 0) {
				world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.3, pos.z, 4, 1.4, 0.1, 1.4, 0.06);
			}
			if (age % 40 == 1) {
				world.playSound(null, pos.x, pos.y, pos.z, ModSounds.WAIL_TORNADO, SoundCategory.PLAYERS, 3.0f, 0.95f + world.random.nextFloat() * 0.1f);
			}

			// Suck in and spin mobs, players and items
			Box box = new Box(pos.x - 9, pos.y - 2, pos.z - 9, pos.x + 9, pos.y + 18, pos.z + 9);
			for (Entity e : world.getOtherEntities(ownerEntity, box)) {
				if (e.isSpectator() || !(e instanceof LivingEntity || e instanceof ItemEntity)) continue;
				if (e instanceof PlayerEntity pl && pl.isCreative()) continue;
				Vec3d to = new Vec3d(pos.x - e.getX(), 0, pos.z - e.getZ());
				double dist = to.length();
				if (dist > 9) continue;
				Vec3d in = dist > 0.01 ? to.multiply(1.0 / dist) : Vec3d.ZERO;
				Vec3d around = new Vec3d(-in.z, 0, in.x);
				double pull = 0.14 * (1 - dist / 10);
				double lift = e.getY() < pos.y + 12 ? 0.13 : -0.03;
				Vec3d v = e.getVelocity().add(in.x * pull + around.x * 0.2 + dir.x * 0.04, lift, in.z * pull + around.z * 0.2 + dir.z * 0.04);
				if (v.length() > 1.4) v = v.normalize().multiply(1.4);
				e.setVelocity(v);
				e.velocityModified = true;
				e.fallDistance = 0;
				if (age % 8 == 0 && dist < 5.5 && e instanceof LivingEntity) {
					DamageSource src = ownerEntity != null
							? world.getDamageSources().indirectMagic(ownerEntity, ownerEntity)
							: world.getDamageSources().magic();
					e.damage(src, 5.0f);
				}
			}

			// Rip up the ground and fling it
			if (age % 2 == 0 && lifted < 90) {
				for (int i = 0; i < 3; i++) {
					BlockPos q = BlockPos.ofFloored(pos).add(world.random.nextInt(7) - 3, world.random.nextInt(3) - 1, world.random.nextInt(7) - 3);
					BlockState s = world.getBlockState(q);
					if (s.isAir() || s.hasBlockEntity() || !s.getFluidState().isEmpty()) continue;
					if (s.isOf(ModBlocks.GHOST_PORTAL) || s.isOf(Blocks.BLACK_CONCRETE) || s.isOf(Blocks.IRON_BLOCK)) continue;
					float hardness = s.getHardness(world, q);
					if (hardness < 0 || hardness > 2.0f) continue;
					FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, q, s);
					fb.setVelocity((world.random.nextDouble() - 0.5) * 0.6, 0.9 + world.random.nextDouble() * 0.5, (world.random.nextDouble() - 0.5) * 0.6);
					fb.velocityModified = true;
					lifted++;
				}
			}
			return false;
		}

		private void finalBurst(Entity ownerEntity) {
			world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 2, pos.z, 1, 0, 0, 0, 0);
			world.spawnParticles(GREEN, pos.x, pos.y + 4, pos.z, 120, 3, 4, 3, 0);
			world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 3.0f, 0.7f);
			Box box = new Box(pos.x - 9, pos.y - 2, pos.z - 9, pos.x + 9, pos.y + 20, pos.z + 9);
			for (Entity e : world.getOtherEntities(ownerEntity, box)) {
				if (e.isSpectator() || !(e instanceof LivingEntity || e instanceof ItemEntity)) continue;
				Vec3d out = new Vec3d(e.getX() - pos.x, 0, e.getZ() - pos.z);
				out = out.lengthSquared() < 1.0E-4 ? dir : out.normalize();
				e.setVelocity(out.x * 1.6, 0.7, out.z * 1.6);
				e.velocityModified = true;
				if (e instanceof LivingEntity) {
					e.damage(ownerEntity != null ? world.getDamageSources().indirectMagic(ownerEntity, ownerEntity)
							: world.getDamageSources().magic(), 8.0f);
				}
			}
		}
	}

	// ------------------------------------------------------------------- tick

	/** Ghost-form players who are actually flying (not just jumping), for the tail and the Superman pose. */
	private static final Set<UUID> FLYING = new HashSet<>();

	public static void tick(MinecraftServer server) {
		GhostState st = GhostState.of(server);
		boolean flyChanged = false;
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			tickPlayer(p, st.data(p.getUuid()), st);
			boolean fly = st.data(p.getUuid()).transformed && p.getAbilities().flying && p.isAlive();
			if (fly ? FLYING.add(p.getUuid()) : FLYING.remove(p.getUuid())) flyChanged = true;
		}
		if (FLYING.removeIf(id -> server.getPlayerManager().getPlayer(id) == null)) flyChanged = true;
		if (flyChanged) broadcastStates(server);
		tickTempPhases();
		tickPhasedBlocks();
		WAVES.removeIf(WailWave::tick);
		TORNADOES.removeIf(Tornado::tick);
		FrostLogic.tick(server);
	}

	private static void tickPlayer(ServerPlayerEntity p, GhostData d, GhostState st) {
		if (d.blastCooldown > 0) d.blastCooldown--;
		if (d.wailCooldown > 0) d.wailCooldown--;
		if (d.tornadoCooldown > 0) d.tornadoCooldown--;
		if (d.exhaust > 0) d.exhaust--;

		if (d.powers && d.transformed && p.isAlive()) {
			d.formTicks++;
			if (d.formTicks % POINT_TICKS == 0) {
				d.points++;
				int before = level(d);
				d.totalPoints++;
				if (level(d) > before) {
					p.sendMessage(Text.literal("Ghost Level " + level(d) + "! Your ghost form got stronger.").formatted(Formatting.GREEN, Formatting.BOLD), false);
					sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8f, 0.7f);
				}
				st.markDirty();
				p.sendMessage(Text.literal("+1 Stat Point! (" + d.points + " total)").formatted(Formatting.GREEN), true);
				sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
			}
			// Possessing something that can't fly: walk around in it (no drifting ghost-flight)
			if (d.possessing == null || hostCanFly(d.possessing)) setFlight(p, true);
			else if (p.getAbilities().allowFlying && !p.isCreative()) setFlight(p, false);
			if (p.age % 40 == 0) applyStats(p, d, true);

			// Intangibility and invisibility are free; energy refills while you're in ghost form
			d.energy = Math.min(MAX_ENERGY, d.energy + 0.3f);
			if (p.age % 6 == 0 && d.possessing == null && !d.invisible) {
				ServerWorld w = p.getServerWorld();
				w.spawnParticles(GREEN, p.getX(), p.getY() + p.getHeight() * 0.5, p.getZ(), 2, 0.3, 0.5, 0.3, 0);
			}
			if (d.fists && p.age % 3 == 0 && !d.invisible) fistSparks(p);
		} else {
			d.energy = Math.min(MAX_ENERGY, d.energy + 0.5f);
		}
		if (d.invisible && p.isAlive()) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 10, 0, false, false, false));
		}
		if (d.intangible && p.age % 3 == 0 && !d.invisible) {
			p.getServerWorld().spawnParticles(PALE, p.getX(), p.getY() + 1, p.getZ(), 2, 0.3, 0.6, 0.3, 0);
		}

		tickPossession(p, d);
		tickShield(p, d);
		DarkPowers.tick(p, d);
		HunterGear.tick(p, d);
		if (p.age % 40 == 0 && p.isAlive()) applyEaten(p, d);
		tickHold(p, d);
		tickCutscene(p, d);
		tickWail(p, d);
		FrostLogic.tickBreath(p, d);
		if (d.hostAbilityCooldown > 0) d.hostAbilityCooldown--;
		if (d.possessing != null && p.age % 20 == 0) HostPowers.passive(p, d.possessing);
		HunterSuit.tick(p, d);

		if (--d.syncTimer <= 0) {
			d.syncTimer = 2;
			sync(p, d);
		}
	}

	private static void tickTempPhases() {
		Iterator<Map.Entry<UUID, GhostFlags.TempPhase>> it = GhostFlags.TEMP.entrySet().iterator();
		while (it.hasNext()) {
			GhostFlags.TempPhase tp = it.next().getValue();
			Entity e = tp.entity;
			tp.ticks--;
			if (e.isRemoved() || tp.ticks <= 0) {
				it.remove();
				if (!(e instanceof PlayerEntity)) e.noClip = false;
				if (!e.isRemoved() && e.isAlive() && !GhostFlags.SELF.contains(e.getUuid())) pushOut(e);
				if (e instanceof ServerPlayerEntity sp) syncNow(sp);
			} else if (tp.ticks % 3 == 0 && e.getWorld() instanceof ServerWorld w) {
				w.spawnParticles(PALE, e.getX(), e.getY() + e.getHeight() / 2, e.getZ(), 2, 0.3, 0.4, 0.3, 0);
			}
		}
	}

	private record PhasedBlock(ServerWorld world, BlockPos pos, BlockState state, int[] ticks) {
		PhasedBlock(ServerWorld world, BlockPos pos, BlockState state, int ticks) {
			this(world, pos, state, new int[]{ticks});
		}

		void restore() {
			if (world.getBlockState(pos).isAir()) {
				world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
			}
		}
	}

	private static void tickPhasedBlocks() {
		HELD_BLOCKS.values().removeIf(list -> {
			if (--list.get(0).ticks()[0] > 0) return false;
			for (PhasedBlock b : list) b.restore();
			return true;
		});
		Iterator<PhasedBlock> it = PHASED.iterator();
		while (it.hasNext()) {
			PhasedBlock b = it.next();
			if (--b.ticks()[0] <= 0) {
				b.restore();
				it.remove();
			}
		}
	}

	// ----------------------------------------------------------------- events

	public static boolean allowDamage(LivingEntity entity, DamageSource source) {
		if (entity.getWorld().isClient) return true;
		UUID id = entity.getUuid();
		if (GhostFlags.SELF.contains(id) || GhostFlags.TEMP.containsKey(id)) return false;
		MinecraftServer server = entity.getServer();
		if (server == null) return true;
		GhostState st = GhostState.of(server);
		if (entity instanceof ServerPlayerEntity p && st.data(p.getUuid()).possessing != null) return false;
		if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
			GhostData ad = st.data(attacker.getUuid());
			if (ad.possessing == entity) return false;
		}
		return true;
	}

	public static void onDeath(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		d.transformed = false;
		d.intangible = false;
		if (d.eaten != 0) {
			d.eaten = 0;
			p.sendMessage(Text.literal("The ghost powers you devoured slip away...").formatted(Formatting.DARK_PURPLE), false);
		}
		GhostFlags.SELF.remove(p.getUuid());
		GhostFlags.TEMP.remove(p.getUuid());
		releaseHold(p, d, false);
		if (d.invisible) {
			d.invisible = false;
			GhostFlags.INVISIBLE.remove(p.getUuid());
			broadcastStates(p.getServer());
		}
		if (d.possessing != null) {
			Entity t = d.possessing;
			d.possessing = null;
			GhostFlags.HOSTS.remove(t.getUuid());
			GhostFlags.POSSESSORS.remove(p.getUuid());
			if (t instanceof MobEntity mob) mob.setAiDisabled(d.prevAiDisabled);
			if (!(t instanceof PlayerEntity)) t.noClip = false;
		}
		st.markDirty();
	}

	public static void onAnyDeath(LivingEntity entity) {
		GhostFlags.TEMP.remove(entity.getUuid());
	}

	public static void onRespawn(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		d.transformed = false;
		d.intangible = false;
		d.cutscene = 0;
		d.wailTicks = 0;
		d.suitOn = false;
		d.syncTimer = 0;
		applyStats(p, d, false);
		broadcastStates(p.getServer());
		if (d.pendingPowers) {
			UUID id = p.getUuid();
			MinecraftServer server = p.getServer();
			PortalLogic.later(() -> {
				ServerPlayerEntity cur = server.getPlayerManager().getPlayer(id);
				if (cur != null) PortalLogic.finishGhostRebirth(cur);
			});
		}
	}

	public static void onJoin(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		if (d.transformed) endForm(p, d);
		else applyStats(p, d, false);
		d.syncTimer = 0;
		broadcastStates(p.getServer());
		if (d.pendingPowers && p.isAlive()) {
			PortalLogic.later(() -> PortalLogic.finishGhostRebirth(p));
		}
	}

	public static void onLeave(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		endForm(p, d);
		GhostFlags.TEMP.remove(p.getUuid());
		GhostFlags.HOSTS.remove(p.getUuid());
		GhostFlags.POSSESSORS.remove(p.getUuid());
		d.cutscene = 0;
		d.wailTicks = 0;
		if (d.suitOn) HunterSuit.toggle(p, d, false);
		st.markDirty();
	}

	public static void onServerStopping() {
		for (PhasedBlock b : PHASED) b.restore();
		PHASED.clear();
		for (List<PhasedBlock> list : HELD_BLOCKS.values()) for (PhasedBlock b : list) b.restore();
		HELD_BLOCKS.clear();
		WAVES.clear();
		TORNADOES.clear();
		GhostFlags.clearAll();
	}

	// ---------------------------------------------------------------- helpers

	public static void grantPowers(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		d.powers = true;
		d.pendingPowers = false;
		d.unlocked |= 1 << BLAST;
		d.energy = MAX_ENERGY;
		st.markDirty();
		syncNow(p);
	}

	public static void syncNow(ServerPlayerEntity p) {
		GhostData d = GhostState.of(p.getServer()).data(p.getUuid());
		sync(p, d);
	}

	private static void sync(ServerPlayerEntity p, GhostData d) {
		if (!ServerPlayNetworking.canSend(p, SyncPayload.ID)) return;
		initLoadout(d);
		Slots.send(p, d);
		boolean intangible = d.intangible || GhostFlags.TEMP.containsKey(p.getUuid());
		ServerPlayNetworking.send(p, new SyncPayload(d.powers, d.transformed, intangible, d.possessing != null, d.invisible,
				d.energy, d.points, d.formTicks % POINT_TICKS, d.unlocked, d.wailCooldown, d.tornadoCooldown, d.exhaust, d.cutscene, d.cutsceneToGhost, level(d), d.cutsceneKind,
				d.holding != null || HELD_BLOCKS.containsKey(p.getUuid()), d.frostCooldown, d.possessing == null ? -1 : HostPowers.kind(d.possessing),
				d.possessing == null ? -1 : d.possessing.getId(), d.suitVariant, d.loadout, d.dark, d.darkMode, d.acneTicks));
	}

	private static boolean useEnergy(ServerPlayerEntity p, GhostData d, float amount) {
		if (d.energy < amount) {
			bar(p, "Not enough ghost energy!", Formatting.RED);
			return false;
		}
		d.energy -= amount;
		return true;
	}

	/** Finds the entity the player is aiming at (ignores the body they're possessing). */
	public static Entity aimEntity(ServerPlayerEntity p, GhostData d, double range) {
		Vec3d start = p.getEyePos();
		Vec3d end = aimBlockPos(p, range);
		Entity host = d.possessing;
		Box box = p.getBoundingBox().stretch(end.subtract(start)).expand(1.0);
		EntityHitResult eh = ProjectileUtil.raycast(p, start, end, box,
				e -> e != host && e != p && !e.isSpectator() && e.canHit(), start.squaredDistanceTo(end));
		return eh == null ? null : eh.getEntity();
	}

	private static Vec3d aimBlockPos(ServerPlayerEntity p, double range) {
		Vec3d start = p.getEyePos();
		Vec3d end = start.add(p.getRotationVec(1f).multiply(range));
		BlockHitResult bh = p.getServerWorld().raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
		return bh.getType() == HitResult.Type.MISS ? end : bh.getPos();
	}

	/** Moves an entity up until it isn't stuck inside blocks. */
	public static void pushOut(Entity e) {
		if (!(e.getWorld() instanceof ServerWorld w)) return;
		for (int i = 0; i < 16 && !w.isSpaceEmpty(e); i++) {
			e.requestTeleport(e.getX(), MathHelper.floor(e.getY()) + 1.0, e.getZ());
		}
	}

	public static void bar(ServerPlayerEntity p, String msg, Formatting color) {
		p.sendMessage(Text.literal(msg).formatted(color), true);
	}

	private static void sound(ServerPlayerEntity p, SoundEvent s, float volume, float pitch) {
		p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundCategory.PLAYERS, volume, pitch);
	}

	@SuppressWarnings("unused")
	private static void particles(ServerWorld w, ParticleEffect effect, Vec3d at, int count) {
		w.spawnParticles(effect, at.x, at.y, at.z, count, 0.3, 0.3, 0.3, 0);
	}
}
