package com.phantompowers;

import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.entity.BossMoves;
import com.phantompowers.network.BeamPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * While you possess a body you get its abilities: a passive bonus while you're inside,
 * and an active power on the Ecto Blast key (R).
 */
public final class HostPowers {
	public static final int CREEPER = 0, ARCHER = 1, BLAZE = 2, GHAST = 3, ENDERMAN = 4, WITCH = 5, SNOW_GOLEM = 6,
			IRON_GOLEM = 7, SPIDER = 8, CHICKEN = 9, HORSE = 10, SWIMMER = 11, HUNTER = 12, ZOMBIE = 13, WARDEN = 14,
			EVOKER = 15, BREEZE = 16, GUARDIAN = 17, RAM = 18, GHOST = 19, SLIME = 20, FLYER = 21, PILLAGER = 22,
			BEE = 23, SHOVE = 24;

	public static final String[] NAMES = {
			"Explode!", "Shoot arrows", "Fireballs", "Big exploding fireball", "Teleport where you look", "Throw harming potions",
			"Snowball barrage", "Ground slam", "Pounce", "Throw eggs", "Gallop dash", "Ink cloud", "Pounce",
			"Rotting roar", "Sonic boom", "Fang line", "Wind charge", "Guardian laser", "Ram", "Ecto burst",
			"Bounce slam", "Dive attack", "Triple crossbow volley", "Poison sting", "Shove"};

	public static final String[] PASSIVES = {
			"", "", "fire resistance", "fire resistance", "", "", "", "strength + resistance", "night vision",
			"slow falling", "speed + high jumps", "water breathing + fast swimming", "speed + night vision",
			"strength", "strength + resistance", "", "", "water breathing", "high jumps", "", "huge jumps",
			"slow falling + speed", "", "slow falling", ""};

	private HostPowers() {}

	public static int kind(Entity e) {
		if (e instanceof CreeperEntity) return CREEPER;
		if (e instanceof AbstractSkeletonEntity) return ARCHER;
		if (e instanceof BlazeEntity) return BLAZE;
		if (e instanceof GhastEntity) return GHAST;
		if (e instanceof EndermanEntity || e instanceof ShulkerEntity) return ENDERMAN;
		if (e instanceof WitchEntity) return WITCH;
		if (e instanceof SnowGolemEntity) return SNOW_GOLEM;
		if (e instanceof IronGolemEntity || e instanceof RavagerEntity) return IRON_GOLEM;
		if (e instanceof SpiderEntity) return SPIDER;
		if (e instanceof ChickenEntity || e instanceof ParrotEntity) return CHICKEN;
		if (e instanceof AbstractHorseEntity || e instanceof CamelEntity) return HORSE;
		if (e instanceof GuardianEntity) return GUARDIAN;
		if (e instanceof WaterCreatureEntity || e instanceof AxolotlEntity || e instanceof TurtleEntity || e instanceof FrogEntity) return SWIMMER;
		if (e instanceof WolfEntity || e instanceof FoxEntity || e instanceof CatEntity || e instanceof OcelotEntity
				|| e instanceof PolarBearEntity || e instanceof PandaEntity) return HUNTER;
		if (e instanceof ZombieEntity || e instanceof ZoglinEntity || e instanceof HoglinEntity) return ZOMBIE;
		if (e instanceof WardenEntity) return WARDEN;
		if (e instanceof EvokerEntity || e instanceof IllusionerEntity) return EVOKER;
		if (e instanceof BreezeEntity) return BREEZE;
		if (e instanceof CowEntity || e instanceof GoatEntity || e instanceof SheepEntity || e instanceof PigEntity
				|| e instanceof SnifferEntity || e instanceof RabbitEntity) return RAM;
		if (e instanceof AbstractGhostEntity) return GHOST;
		if (e instanceof SlimeEntity) return SLIME;
		if (e instanceof PhantomEntity || e instanceof VexEntity || e instanceof AllayEntity || e instanceof BatEntity) return FLYER;
		if (e instanceof PillagerEntity || e instanceof PiglinEntity) return PILLAGER;
		if (e instanceof BeeEntity) return BEE;
		return SHOVE;
	}

	// ------------------------------------------------------------------ passive

	public static void passive(ServerPlayerEntity p, Entity host) {
		switch (kind(host)) {
			case BLAZE, GHAST -> eff(p, StatusEffects.FIRE_RESISTANCE, 0);
			case IRON_GOLEM, WARDEN -> {
				eff(p, StatusEffects.STRENGTH, 1);
				eff(p, StatusEffects.RESISTANCE, 1);
			}
			case SPIDER -> eff(p, StatusEffects.NIGHT_VISION, 0);
			case CHICKEN, BEE -> eff(p, StatusEffects.SLOW_FALLING, 0);
			case HORSE -> {
				eff(p, StatusEffects.SPEED, 2);
				eff(p, StatusEffects.JUMP_BOOST, 2);
			}
			case SWIMMER, GUARDIAN -> {
				eff(p, StatusEffects.WATER_BREATHING, 0);
				eff(p, StatusEffects.DOLPHINS_GRACE, 0);
				eff(p, StatusEffects.NIGHT_VISION, 0);
			}
			case HUNTER -> {
				eff(p, StatusEffects.SPEED, 1);
				eff(p, StatusEffects.NIGHT_VISION, 0);
			}
			case ZOMBIE -> eff(p, StatusEffects.STRENGTH, 0);
			case RAM -> eff(p, StatusEffects.JUMP_BOOST, 1);
			case SLIME -> eff(p, StatusEffects.JUMP_BOOST, 3);
			case FLYER -> {
				eff(p, StatusEffects.SLOW_FALLING, 0);
				eff(p, StatusEffects.SPEED, 1);
			}
			default -> { }
		}
	}

	private static void eff(ServerPlayerEntity p, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> e, int amp) {
		p.addStatusEffect(new StatusEffectInstance(e, 50, amp, true, false, true));
	}

	// ------------------------------------------------------------------ active

	public static void active(ServerPlayerEntity p, GhostData d) {
		Entity host = d.possessing;
		if (!(host instanceof LivingEntity body)) return;
		if (d.hostAbilityCooldown > 0) return;
		ServerWorld w = p.getServerWorld();
		Vec3d dir = p.getRotationVec(1f);
		Vec3d eye = new Vec3d(host.getX(), host.getEyeY(), host.getZ());
		Vec3d muzzle = eye.add(dir.multiply(host.getWidth() / 2 + 0.7));
		int k = kind(host);
		int cd = 20;
		switch (k) {
			case CREEPER -> {
				boolean charged = host instanceof CreeperEntity c && c.shouldRenderOverlay();
				w.playSound(null, host.getX(), host.getY(), host.getZ(), SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 1.0f, 0.5f);
				GhostPowers.stopPossess(p, d, false);
				w.createExplosion(host, host.getX(), host.getY(), host.getZ(), charged ? 6f : 3f, World.ExplosionSourceType.MOB);
				host.discard();
				GhostPowers.bar(p, "BOOM! You blew up your host and floated out.", Formatting.GREEN);
				d.hostAbilityCooldown = 20;
				return;
			}
			case ARCHER -> {
				arrow(w, p, muzzle, dir, 0f, 3.0f);
				sound(w, host, SoundEvents.ENTITY_SKELETON_SHOOT, 1.0f);
				cd = 12;
			}
			case PILLAGER -> {
				for (int i = -1; i <= 1; i++) arrow(w, p, muzzle, dir, i * 10f, 3.2f);
				sound(w, host, SoundEvents.ITEM_CROSSBOW_SHOOT, 1.0f);
				cd = 25;
			}
			case BLAZE -> {
				for (int i = 0; i < 3; i++) {
					final int n = i;
					BossMoves.later(1 + i * 4, () -> {
						if (!p.isAlive()) return;
						Vec3d dd = p.getRotationVec(1f);
						SmallFireballEntity fb = new SmallFireballEntity(w, p, dd.multiply(1.0).add(w.random.nextGaussian() * 0.04, 0, w.random.nextGaussian() * 0.04));
						Vec3d at = new Vec3d(host.getX(), host.getEyeY(), host.getZ()).add(dd.multiply(1.2));
						fb.setPosition(at.x, at.y, at.z);
						w.spawnEntity(fb);
						sound(w, host, SoundEvents.ENTITY_BLAZE_SHOOT, 1.0f + n * 0.1f);
					});
				}
				cd = 30;
			}
			case GHAST -> {
				FireballEntity fb = new FireballEntity(w, p, dir.multiply(1.2), 2);
				fb.setPosition(muzzle.x, muzzle.y, muzzle.z);
				w.spawnEntity(fb);
				sound(w, host, SoundEvents.ENTITY_GHAST_SHOOT, 1.0f);
				cd = 40;
			}
			case ENDERMAN -> {
				BlockHitResult hit = w.raycast(new RaycastContext(eye, eye.add(dir.multiply(32)), RaycastContext.ShapeType.COLLIDER,
						RaycastContext.FluidHandling.NONE, p));
				Vec3d to = hit.getType() == HitResult.Type.MISS ? eye.add(dir.multiply(32)) : hit.getPos().subtract(dir.multiply(0.8));
				to = BossMoves.ground(w, to);
				w.spawnParticles(ParticleTypes.PORTAL, host.getX(), host.getY() + 1, host.getZ(), 60, 0.4, 1, 0.4, 0.5);
				sound(w, host, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f);
				p.requestTeleport(to.x, to.y, to.z);
				host.refreshPositionAndAngles(to.x, to.y, to.z, host.getYaw(), host.getPitch());
				w.spawnParticles(ParticleTypes.PORTAL, to.x, to.y + 1, to.z, 60, 0.4, 1, 0.4, 0.5);
				cd = 30;
			}
			case WITCH -> {
				PotionEntity pot = new PotionEntity(w, p);
				pot.setItem(PotionContentsComponent.createStack(Items.SPLASH_POTION, w.random.nextBoolean() ? Potions.HARMING : Potions.POISON));
				pot.setPosition(muzzle.x, muzzle.y, muzzle.z);
				pot.setVelocity(p, p.getPitch(), p.getYaw(), -15f, 0.8f, 1f);
				w.spawnEntity(pot);
				sound(w, host, SoundEvents.ENTITY_WITCH_THROW, 1.0f);
				cd = 30;
			}
			case SNOW_GOLEM -> {
				for (int i = 0; i < 5; i++) {
					SnowballEntity s = new SnowballEntity(w, p);
					s.setPosition(muzzle.x, muzzle.y, muzzle.z);
					s.setVelocity(p, p.getPitch() + (i - 2) * 3f, p.getYaw() + (i - 2) * 4f, 0f, 1.8f, 2f);
					w.spawnEntity(s);
				}
				sound(w, host, SoundEvents.ENTITY_SNOW_GOLEM_SHOOT, 1.0f);
				cd = 15;
			}
			case CHICKEN -> {
				for (int i = 0; i < 3; i++) {
					EggEntity egg = new EggEntity(w, p);
					egg.setPosition(muzzle.x, muzzle.y, muzzle.z);
					egg.setVelocity(p, p.getPitch(), p.getYaw() + (i - 1) * 6f, 0f, 1.5f, 1f);
					w.spawnEntity(egg);
				}
				sound(w, host, SoundEvents.ENTITY_CHICKEN_EGG, 1.0f);
				cd = 15;
			}
			case IRON_GOLEM -> {
				p.addVelocity(0, 0.6, 0);
				p.velocityModified = true;
				BossMoves.later(10, () -> {
					if (!p.isAlive()) return;
					BossMoves.groundWave(w, p, BossMoves.ground(w, p.getPos()), 7, 9f, 0xDDDDDD);
				});
				sound(w, host, SoundEvents.ENTITY_IRON_GOLEM_ATTACK, 0.7f);
				cd = 50;
			}
			case WARDEN -> {
				Vec3d end = eye.add(dir.multiply(20));
				for (double t = 1; t < 20; t += 1) {
					Vec3d at = eye.add(dir.multiply(t));
					w.spawnParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
				}
				BossMoves.coneHit(w, p, eye, dir, 20, 0.95, w.getDamageSources().sonicBoom(p), 10f, 2.5, e -> e != host);
				sound(w, host, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.0f);
				cd = 60;
			}
			case EVOKER -> {
				Vec3d flat = new Vec3d(dir.x, 0, dir.z).normalize();
				for (int i = 1; i <= 12; i++) {
					Vec3d at = BossMoves.ground(w, host.getPos().add(flat.multiply(i * 1.2)));
					EvokerFangsEntity fang = new EvokerFangsEntity(w, at.x, at.y, at.z, p.getYaw() * MathHelper.RADIANS_PER_DEGREE, i, p);
					w.spawnEntity(fang);
				}
				sound(w, host, SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.0f);
				cd = 40;
			}
			case BREEZE -> {
				WindChargeEntity wc = new WindChargeEntity(p, w, muzzle.x, muzzle.y, muzzle.z);
				wc.setVelocity(p, p.getPitch(), p.getYaw(), 0f, 1.5f, 1f);
				w.spawnEntity(wc);
				sound(w, host, SoundEvents.ENTITY_BREEZE_SHOOT, 1.0f);
				cd = 15;
			}
			case GUARDIAN -> {
				Vec3d end = eye.add(dir.multiply(24));
				BlockHitResult hit = w.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
				if (hit.getType() != HitResult.Type.MISS) end = hit.getPos();
				BossMoves.coneHit(w, p, eye, dir, eye.distanceTo(end), 0.97, w.getDamageSources().indirectMagic(p, p), 8f, 0.3, e -> e != host);
				beam(w, muzzle, end, 0x66FFEE);
				sound(w, host, SoundEvents.ENTITY_GUARDIAN_ATTACK, 1.0f);
				cd = 30;
			}
			case GHOST -> {
				Vec3d c = host.getPos().add(0, host.getHeight() / 2, 0);
				w.spawnParticles(GhostPowers.GREEN, c.x, c.y, c.z, 120, 2, 1, 2, 0.1);
				w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z, 40, 1.5, 1, 1.5, 0.1);
				BossMoves.groundWave(w, p, BossMoves.ground(w, host.getPos()), 6, 8f, 0x33FF77);
				sound(w, host, ModSounds.GHOSTLY_WAIL, 1.4f);
				cd = 50;
			}
			case SLIME -> {
				p.addVelocity(0, 1.1, 0);
				p.velocityModified = true;
				BossMoves.later(18, () -> {
					if (!p.isAlive()) return;
					BossMoves.groundWave(w, p, BossMoves.ground(w, p.getPos()), 5, 6f, 0x77DD66);
				});
				sound(w, host, SoundEvents.ENTITY_SLIME_JUMP, 0.6f);
				cd = 40;
			}
			case SWIMMER -> {
				Vec3d c = muzzle.add(dir.multiply(2));
				w.spawnParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 80, 1.5, 1, 1.5, 0.05);
				for (Entity e : w.getOtherEntities(p, host.getBoundingBox().expand(6), e -> e instanceof LivingEntity && e != host)) {
					((LivingEntity) e).addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0), p);
				}
				sound(w, host, SoundEvents.ENTITY_SQUID_SQUIRT, 1.0f);
				cd = 40;
			}
			case ZOMBIE -> {
				for (Entity e : w.getOtherEntities(p, host.getBoundingBox().expand(8), e -> e instanceof LivingEntity && e != host)) {
					((LivingEntity) e).addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 120, 2), p);
					((LivingEntity) e).addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), p);
				}
				w.spawnParticles(ParticleTypes.SNEEZE, host.getX(), host.getEyeY(), host.getZ(), 60, 2, 0.5, 2, 0.05);
				sound(w, host, SoundEvents.ENTITY_ZOMBIE_AMBIENT, 0.6f);
				cd = 50;
			}
			case BEE -> {
				BossMoves.coneHit(w, p, eye, dir, 4, 0.6, w.getDamageSources().sting(body), 4f, 0.3, e -> e != host);
				for (Entity e : w.getOtherEntities(p, host.getBoundingBox().expand(4), e -> e instanceof LivingEntity && e != host)) {
					((LivingEntity) e).addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 1), p);
				}
				sound(w, host, SoundEvents.ENTITY_BEE_STING, 1.0f);
				cd = 30;
			}
			default -> {
				// Pounce / gallop / ram / dive / shove: lunge forward and bowl things over
				double power = switch (k) {
					case HORSE -> 2.2;
					case FLYER -> 1.8;
					case SPIDER, HUNTER -> 1.4;
					case RAM -> 1.3;
					default -> 1.0;
				};
				Vec3d push = (k == FLYER || k == SPIDER || k == HUNTER) ? dir.multiply(power) : new Vec3d(dir.x, 0.25, dir.z).normalize().multiply(power);
				p.setVelocity(push);
				p.velocityModified = true;
				float dmg = k == RAM ? 6f : k == HORSE ? 5f : 4f;
				BossMoves.later(6, () -> {
					if (!p.isAlive()) return;
					BossMoves.coneHit(w, p, p.getPos().add(0, 0.8, 0), p.getRotationVec(1f), 3.5, 0.2,
							w.getDamageSources().playerAttack(p), dmg, 1.4, e -> e != host);
				});
				SoundEvent s = switch (k) {
					case HORSE -> SoundEvents.ENTITY_HORSE_GALLOP;
					case SPIDER -> SoundEvents.ENTITY_SPIDER_AMBIENT;
					case HUNTER -> SoundEvents.ENTITY_WOLF_GROWL;
					case RAM -> SoundEvents.ENTITY_GOAT_RAM_IMPACT;
					case FLYER -> SoundEvents.ENTITY_PHANTOM_SWOOP;
					default -> SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK;
				};
				sound(w, host, s, 1.0f);
				cd = 25;
			}
		}
		d.hostAbilityCooldown = cd;
	}

	private static void arrow(ServerWorld w, ServerPlayerEntity p, Vec3d at, Vec3d dir, float yawOff, float speed) {
		ArrowEntity a = new ArrowEntity(w, p, new ItemStack(Items.ARROW), null);
		a.setPosition(at.x, at.y, at.z);
		a.setVelocity(p, p.getPitch(), p.getYaw() + yawOff, 0f, speed, 1f);
		a.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
		w.spawnEntity(a);
	}

	private static void beam(ServerWorld w, Vec3d from, Vec3d to, int color) {
		BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) to.x, (float) to.y, (float) to.z, color);
		for (ServerPlayerEntity viewer : PlayerLookup.around(w, from, 96)) {
			if (ServerPlayNetworking.canSend(viewer, BeamPayload.ID)) ServerPlayNetworking.send(viewer, beam);
		}
	}

	private static void sound(ServerWorld w, Entity at, SoundEvent s, float pitch) {
		w.playSound(null, at.getX(), at.getY(), at.getZ(), s, SoundCategory.PLAYERS, 1.2f, pitch);
	}

	/** Players can't have a host power that targets their own body. */
	public static boolean isPlayer(Entity e) {
		return e instanceof PlayerEntity;
	}
}
