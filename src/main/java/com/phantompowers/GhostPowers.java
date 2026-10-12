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

	public static final int BLAST = 0, INTANGIBLE = 1, POSSESS = 2, WAIL = 3, TORNADO = 4, INVISIBLE = 5, FROST = 6;
	public static final int SKILL_COUNT = 7;
	public static final String[] NAMES = {"Ecto Blast", "Intangibility", "Possession", "Ghostly Wail", "Wail Tornado", "Invisibility", "Frost Breath"};
	public static final int[] COSTS = {0, 3, 5, 8, 12, 2, 6};
	public static final String[] DESCRIPTIONS = {
			"Fire a beam of green ecto energy.",
			"Walk through walls. Sneak to sink, fly to rise. Sneak+hold to phase others.",
			"Take over a mob or player and control where it goes.",
			"A huge scream that breaks blocks and blasts mobs away.",
			"A giant tornado of wail energy that rips up the ground.",
			"Turn completely invisible, armor and all.",
			"Hold to breathe ice. Freeze enemies solid, then punch to shatter."
	};

	public static final int A_TRANSFORM = 0, A_BLAST = 1, A_INTANGIBLE = 2, A_POSSESS = 3, A_WAIL = 4, A_TORNADO = 5,
			A_INVISIBLE = 6, A_INTANGIBLE_RELEASE = 7, A_TRANSFORM_CUTSCENE = 8,
			A_WAIL_RELEASE = 9, A_SUIT = 10, A_SUIT_CUTSCENE = 11, A_GLIDER_FIRE = 12,
			A_FROST = 13, A_FROST_RELEASE = 14, A_UNLOCK = 100;

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
				else if (ready(p, d, st, BLAST)) blast(p, d);
			}
			case A_FROST -> { if (ready(p, d, st, FROST)) FrostLogic.start(p, d); }
			case A_FROST_RELEASE -> d.frostHeld = false;
			case A_INTANGIBLE -> { if (ready(p, d, st, INTANGIBLE)) intangibility(p, d); }
			case A_POSSESS -> { if (ready(p, d, st, POSSESS)) possession(p, d, st); }
			case A_WAIL -> { if (ready(p, d, st, WAIL)) wail(p, d); }
			case A_WAIL_RELEASE -> d.wailHeld = false;
			case A_SUIT -> { if (d.cutscene == 0) HunterSuit.toggle(p, d, true); }
			case A_SUIT_CUTSCENE -> startSuitCutscene(p, d);
			case A_GLIDER_FIRE -> { if (p.getVehicle() instanceof GliderEntity glider) glider.fire(p); }
			case A_TORNADO -> { if (ready(p, d, st, TORNADO)) tornado(p, d); }
			case A_INVISIBLE -> { if (ready(p, d, st, INVISIBLE)) toggleInvisible(p, d); }
			case A_INTANGIBLE_RELEASE -> releaseHold(p, d, true);
			default -> {
				if (action >= A_UNLOCK && action < A_UNLOCK + SKILL_COUNT) tryUnlock(p, d, st, action - A_UNLOCK);
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
		if (!d.transformed) {
			bar(p, "Go ghost first! Press V", Formatting.YELLOW);
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
		d.points -= COSTS[skill];
		d.unlocked |= 1 << skill;
		st.markDirty();
		p.sendMessage(Text.literal("New ghost power unlocked: " + NAMES[skill] + "!").formatted(Formatting.GREEN, Formatting.BOLD), false);
		syncNow(p);
		sound(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
	}

	// -------------------------------------------------------------- transform

	private static void toggleTransform(ServerPlayerEntity p, GhostData d, boolean rings) {
		if (!d.powers) {
			bar(p, "You don't have ghost powers yet. Activate a Ghost Portal!", Formatting.RED);
			return;
		}
		if (d.transformed) {
			endForm(p, d);
			if (rings) sendRings(p, 14);
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
		if (rings) sendRings(p, 14);
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
		setFlight(p, false);
		applyStats(p, d, false);
		broadcastStates(p.getServer());
	}

	// ------------------------------------------------------- devoured essences

	public static final int ESS_HOSTILE = 0, ESS_FRIENDLY = 1, ESS_JAILER = 2, ESS_CRATE = 3, ESS_VOLTAGE = 4, ESS_WISH = 5;
	public static final String[] ESSENCE_NAMES = {"Hostile Ghost", "Friendly Ghost", "Ghost Jailer", "Crate King", "Voltage", "Wishkeeper"};
	public static final String[] ESSENCE_POWERS = {
			"Strength and night vision",
			"Constant healing",
			"Toughness, and your hits chain enemies (slow them)",
			"A crate-armor shield that keeps coming back",
			"Speed, and every 4th hit calls down lightning",
			"Luck, and a free wish (a random blessing) every minute"};

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

	private static void setFlight(ServerPlayerEntity p, boolean on) {
		PlayerAbilities a = p.getAbilities();
		if (on) {
			if (!a.allowFlying) {
				a.allowFlying = true;
				p.sendAbilitiesUpdate();
			}
		} else if (!p.isCreative() && !p.isSpectator()) {
			a.allowFlying = false;
			a.flying = false;
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
		d.cutscene = CUTSCENE_TICKS;
		d.cutsceneKind = 0;
		d.cutsceneToGhost = !d.transformed;
		d.cutsceneAnchor = p.getPos();
		sendRings(p, CUTSCENE_TICKS - 8);
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
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 2.0f, 0.8f);
		syncNow(p);
	}

	private static void tickWailFinale(ServerPlayerEntity p, GhostData d, ServerWorld w) {
		int t = WAIL_FINALE_TICKS - d.cutscene;
		if (d.cutscene > 30) {
			Vec3d eye = p.getEyePos();
			Vec3d look = p.getRotationVec(1f);
			if (t % 4 == 1) {
				// Waves blast out all around you, not just ahead
				for (int k = 0; k < 4; k++) {
					Vec3d dir = look.rotateY((float) (k * Math.PI / 2));
					WAVES.add(new WailWave(w, p.getUuid(), eye, dir));
				}
			}
			if (t % 3 == 0) {
				for (int k = 0; k < 6; k++) {
					Vec3d dir = look.rotateY((float) (k * Math.PI / 3 + t * 0.1));
					com.phantompowers.entity.BossMoves.soundWave(w, eye.add(dir.multiply(0.6)), dir, 22f, 0x66FF99);
				}
			}
			if (t % 18 == 0) {
				w.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.PLAYERS, 6.0f, 0.9f + w.random.nextFloat() * 0.15f);
			}
			w.spawnParticles(GREEN, eye.x, eye.y, eye.z, 12, 1.2, 0.8, 1.2, 0.2);
			w.spawnParticles(ParticleTypes.SONIC_BOOM, eye.x + look.x * 2, eye.y + look.y * 2, eye.z + look.z * 2, 1, 0, 0, 0, 0);
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
		d.cutscene = CUTSCENE_TICKS;
		d.cutsceneKind = 1;
		d.cutsceneToGhost = !d.suitOn;
		d.cutsceneAnchor = p.getPos();
		sendRings(p, CUTSCENE_TICKS - 8, 1);
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 1.3f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(), SoundCategory.PLAYERS, 1.0f, 0.8f);
		syncNow(p);
	}

	private static void tickCutscene(ServerPlayerEntity p, GhostData d) {
		if (d.cutscene <= 0) return;
		d.cutscene--;
		// Hold still (and float a tiny bit) while the rings pass over you
		if (d.cutsceneAnchor != null) {
			double lift = d.cutsceneKind == 2 ? (d.cutscene > 30 ? Math.min(1.2, (WAIL_FINALE_TICKS - d.cutscene) * 0.04) : 1.2 * d.cutscene / 30.0)
					: d.cutsceneToGhost ? Math.min(0.6, (CUTSCENE_TICKS - d.cutscene) * 0.015) : 0;
			p.requestTeleport(d.cutsceneAnchor.x, d.cutsceneAnchor.y + lift, d.cutsceneAnchor.z);
			p.setVelocity(Vec3d.ZERO);
			p.fallDistance = 0;
		}
		ServerWorld w = p.getServerWorld();
		if (d.cutsceneKind == 2) tickWailFinale(p, d, w);
		if (d.cutscene % 3 == 0) {
			w.spawnParticles(d.cutsceneToGhost ? GREEN : PALE, p.getX(), p.getY() + 1, p.getZ(), 3, 0.5, 0.8, 0.5, 0);
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
			w.spawnParticles(d.cutsceneToGhost ? GREEN : PALE, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1.0, 0.6, 0.02);
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
		BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) end.x, (float) end.y, (float) end.z, 0x33FF77);
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
		if (d.energy < 10) {
			bar(p, "Not enough ghost energy!", Formatting.RED);
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
		if (d.energy < PHASE_TARGET_COST) {
			bar(p, "Not enough ghost energy!", Formatting.RED);
			return;
		}
		ServerWorld w = p.getServerWorld();
		Entity target = aimEntity(p, d, 16);
		if (target != null && !(target instanceof EnderDragonEntity)) {
			d.energy -= PHASE_TARGET_COST;
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
			d.energy -= PHASE_TARGET_COST;
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
		d.energy -= 0.12f;
		if (d.energy <= 0) {
			d.energy = 0;
			releaseHold(p, d, false);
			bar(p, "Out of ghost energy, you lost your grip!", Formatting.RED);
		}
	}

	// ----------------------------------------------------------- invisibility

	private static void toggleInvisible(ServerPlayerEntity p, GhostData d) {
		if (d.invisible) {
			stopInvisible(p, d);
			bar(p, "You're visible again.", Formatting.GRAY);
			return;
		}
		if (d.energy < 10) {
			bar(p, "Not enough ghost energy!", Formatting.RED);
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
		InvisiblePayload payload = new InvisiblePayload(new ArrayList<>(GhostFlags.INVISIBLE), ghosts, suited);
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
		stopIntangible(p, d, false);
		d.energy -= POSSESS_START_COST;
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
		bar(p, "You possessed " + target.getName().getString() + "! R: " + HostPowers.NAMES[HostPowers.kind(target)] + ". H to leave.", Formatting.GREEN);
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
			tp.teleport(p.getServerWorld(), p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
		} else {
			t.refreshPositionAndAngles(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
			t.setHeadYaw(p.getHeadYaw());
			t.setBodyYaw(p.getYaw());
			t.setVelocity(Vec3d.ZERO);
		}
		t.fallDistance = 0;
		if (d.possessTicks % 5 == 0) calmHostiles(p, t);
		if (d.possessTicks % 4 == 0) {
			p.getServerWorld().spawnParticles(GREEN, t.getX(), t.getY() + t.getHeight() * 0.6, t.getZ(), 3, 0.3, 0.4, 0.3, 0);
		}
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
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.0f, 1.4f);
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
		}

		/** Returns true when finished. */
		boolean tick() {
			radius++;
			if (radius > 16) return true;
			Entity ownerEntity = world.getServer().getPlayerManager().getPlayer(owner);
			double cone = 0.80;

			// Visuals: a sonic ring travelling outwards
			Vec3d center = origin.add(dir.multiply(radius));
			world.spawnParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, 0, 0, 0, 0);
			Vec3d u = dir.crossProduct(new Vec3d(0, 1, 0));
			if (u.lengthSquared() < 1.0E-4) u = new Vec3d(1, 0, 0);
			u = u.normalize();
			Vec3d v = dir.crossProduct(u).normalize();
			double ringRadius = radius * 0.6;
			int points = 10 + radius * 3;
			for (int i = 0; i < points; i++) {
				double a = i * Math.PI * 2 / points;
				Vec3d pt = center.add(u.multiply(Math.cos(a) * ringRadius)).add(v.multiply(Math.sin(a) * ringRadius));
				world.spawnParticles(GREEN, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
			}

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
		w.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.PLAYERS, 4.0f, 0.8f);
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
		Vec3d pos;
		int age;
		int lifted;

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

			// The spinning funnel
			double spin = age * 0.45;
			for (int h = 0; h < 15; h++) {
				double r = 0.5 + h * 0.38;
				for (int k = 0; k < 3; k++) {
					double a = spin + h * 0.55 + k * (Math.PI * 2 / 3);
					double x = pos.x + Math.cos(a) * r;
					double z = pos.z + Math.sin(a) * r;
					world.spawnParticles(k == 0 ? PALE : GREEN, x, pos.y + h, z, 1, 0, 0, 0, 0);
				}
			}
			if (age % 4 == 0) {
				world.spawnParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y + 1 + world.random.nextInt(12), pos.z, 1, 0, 0, 0, 0);
				world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 6, 1.2, 0.2, 1.2, 0.05);
			}
			if (age % 20 == 1) {
				world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 2.5f, 0.6f);
				if (age % 60 == 1) world.playSound(null, pos.x, pos.y, pos.z, ModSounds.GHOSTLY_WAIL, SoundCategory.PLAYERS, 2.0f, 0.7f);
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

	public static void tick(MinecraftServer server) {
		GhostState st = GhostState.of(server);
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			tickPlayer(p, st.data(p.getUuid()), st);
		}
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
			setFlight(p, true);
			if (p.age % 40 == 0) applyStats(p, d, true);

			float drain = 0;
			if (d.intangible) drain += 0.15f;
			if (d.invisible) drain += 0.1f;
			if (drain > 0) {
				d.energy -= drain;
				if (d.energy <= 0) {
					d.energy = 0;
					stopIntangible(p, d, false);
					stopPossess(p, d, false);
					stopInvisible(p, d);
					bar(p, "Out of ghost energy!", Formatting.RED);
				}
			} else {
				d.energy = Math.min(MAX_ENERGY, d.energy + 0.3f);
			}

			if (d.invisible) {
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 10, 0, false, false, false));
			}
			if (p.age % 6 == 0 && d.possessing == null && !d.invisible) {
				ServerWorld w = p.getServerWorld();
				w.spawnParticles(GREEN, p.getX(), p.getY() + p.getHeight() * 0.5, p.getZ(), 2, 0.3, 0.5, 0.3, 0);
			}
			if (d.intangible && p.age % 3 == 0) {
				p.getServerWorld().spawnParticles(PALE, p.getX(), p.getY() + 1, p.getZ(), 2, 0.3, 0.6, 0.3, 0);
			}
		} else {
			d.energy = Math.min(MAX_ENERGY, d.energy + 0.5f);
		}

		tickPossession(p, d);
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
		boolean intangible = d.intangible || GhostFlags.TEMP.containsKey(p.getUuid());
		ServerPlayNetworking.send(p, new SyncPayload(d.powers, d.transformed, intangible, d.possessing != null, d.invisible,
				d.energy, d.points, d.formTicks % POINT_TICKS, d.unlocked, d.wailCooldown, d.tornadoCooldown, d.exhaust, d.cutscene, d.cutsceneToGhost, level(d), d.cutsceneKind,
				d.holding != null || HELD_BLOCKS.containsKey(p.getUuid()), d.frostCooldown, d.possessing == null ? -1 : HostPowers.kind(d.possessing)));
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
	private static Entity aimEntity(ServerPlayerEntity p, GhostData d, double range) {
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
