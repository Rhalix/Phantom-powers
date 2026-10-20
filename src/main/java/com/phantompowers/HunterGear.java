package com.phantompowers;

import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.entity.MissileEntity;
import com.phantompowers.network.BeamPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;

/** Ghost hunter upgrades: suit tiers, wrist missiles and backpack cannons (stored on the suit pack). */
public final class HunterGear {
	public static final int MAX_TIER = 3;

	private HunterGear() {}

	private static NbtCompound data(ItemStack s) {
		return s.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
	}

	private static void put(ItemStack s, NbtCompound n) {
		s.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
	}

	public static int tier(ItemStack suit) {
		return suit.isOf(ModItems.HUNTER_SUIT) ? Math.min(MAX_TIER, data(suit).getInt("SuitTier")) : 0;
	}

	public static boolean hasMissiles(ItemStack suit) {
		return suit.isOf(ModItems.HUNTER_SUIT) && data(suit).getBoolean("WristMissiles");
	}

	public static boolean hasCannons(ItemStack suit) {
		return suit.isOf(ModItems.HUNTER_SUIT) && data(suit).getBoolean("BackpackCannons");
	}

	/** Use an upgrade item: applies it to the suit pack you're wearing. Returns true if it was used up. */
	public static boolean install(ServerPlayerEntity p, String what) {
		ItemStack suit = p.getEquippedStack(EquipmentSlot.CHEST);
		if (!suit.isOf(ModItems.HUNTER_SUIT)) {
			GhostPowers.bar(p, "Wear the Ghost Hunter Suit pack in your chest slot to install this.", Formatting.YELLOW);
			return false;
		}
		NbtCompound n = data(suit);
		switch (what) {
			case "tier" -> {
				int t = n.getInt("SuitTier");
				if (t >= MAX_TIER) {
					GhostPowers.bar(p, "Your suit is already fully upgraded (Mk " + (MAX_TIER + 1) + ").", Formatting.YELLOW);
					return false;
				}
				n.putInt("SuitTier", t + 1);
				GhostPowers.bar(p, "Suit upgraded to Mk " + (t + 2) + "! Tougher plating, more power.", Formatting.AQUA);
			}
			case "missiles" -> {
				if (n.getBoolean("WristMissiles")) return false;
				n.putBoolean("WristMissiles", true);
				GhostPowers.bar(p, "Wrist missiles installed! With the suit on, press your Ecto Blast key (R) to fire.", Formatting.AQUA);
			}
			case "cannons" -> {
				if (n.getBoolean("BackpackCannons")) return false;
				n.putBoolean("BackpackCannons", true);
				GhostPowers.bar(p, "Backpack cannons installed! With the suit on, press their Power Slot key to deploy them (gadget slots are in the Phantom Menu).", Formatting.AQUA);
			}
			default -> {
				return false;
			}
		}
		put(suit, n);
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_SMITHING_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.2f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(), SoundCategory.PLAYERS, 1.0f, 1.4f);
		HunterSuit.reapply(p, GhostState.of(p.getServer()).data(p.getUuid()));
		return true;
	}

	// ----------------------------------------------------------- wrist missiles

	/** Hold R: the wrist launcher flips out and you raise your arm to aim. */
	public static void startAim(ServerPlayerEntity p, GhostData d) {
		ItemStack suit = p.getEquippedStack(EquipmentSlot.CHEST);
		if (!hasMissiles(suit)) {
			GhostPowers.bar(p, "No wrist missiles installed on your suit.", Formatting.GRAY);
			return;
		}
		if (d.missileAim) return;
		d.missileAim = true;
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.PLAYERS, 0.7f, 1.8f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS, 0.6f, 1.6f);
		GhostPowers.broadcastStates(p.getServer());
	}

	/** Let go of R: fire. */
	public static void releaseAim(ServerPlayerEntity p, GhostData d) {
		d.missileAim = false;
		fireMissiles(p, d);
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.PLAYERS, 0.5f, 1.9f);
		GhostPowers.broadcastStates(p.getServer());
	}

	/** Where the launcher sits: on the back of your right wrist, with your arm held out where you're looking. */
	private static Vec3d wrist(ServerPlayerEntity p, Vec3d look) {
		Vec3d right = look.crossProduct(new Vec3d(0, 1, 0));
		right = right.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : right.normalize();
		return p.getEyePos().add(look.multiply(0.75)).add(right.multiply(0.36)).add(0, -0.32, 0);
	}

	public static void fireMissiles(ServerPlayerEntity p, GhostData d) {
		ItemStack suit = p.getEquippedStack(EquipmentSlot.CHEST);
		if (!hasMissiles(suit)) {
			GhostPowers.bar(p, "No wrist missiles installed on your suit.", Formatting.GRAY);
			return;
		}
		if (d.missileCooldown > 0) {
			GhostPowers.bar(p, "Reloading... (" + (d.missileCooldown / 20 + 1) + "s)", Formatting.GRAY);
			return;
		}
		int t = tier(suit);
		d.missileCooldown = 36 - t * 6;
		ServerWorld w = p.getServerWorld();
		var target = GhostPowers.aimEntity(p, d, 48);
		Vec3d look = p.getRotationVec(1f);
		Vec3d aim = target != null ? target.getBoundingBox().getCenter() : GhostPowers.aimPoint(p, 48);
		Vec3d from = wrist(p, look);
		Vec3d right = look.crossProduct(new Vec3d(0, 1, 0));
		right = right.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : right.normalize();
		Vec3d up = right.crossProduct(look).normalize();
		int count = 2 + (t >= 2 ? 1 : 0) + (t >= 3 ? 1 : 0);
		for (int i = 0; i < count; i++) {
			// the pod's tubes are in a little 2x2 block
			Vec3d off = right.multiply((i % 2 == 0 ? -0.07 : 0.07)).add(up.multiply(i < 2 ? 0.05 : -0.05));
			Vec3d start = from.add(off);
			Vec3d dir = aim.subtract(start).normalize();
			MissileEntity m = new MissileEntity(w, p);
			m.setPosition(start.x, start.y, start.z);
			// a little spread so the salvo fans out and then curls back in on the target
			m.setVelocity(dir.x + off.x * 1.4, dir.y + off.y * 1.4, dir.z + off.z * 1.4, 1.35f, 0f);
			if (target instanceof LivingEntity le) m.setTarget(le);
			m.setPower(7f + t * 2f);
			w.spawnEntity(m);
		}
		w.spawnParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 8, 0.05, 0.05, 0.05, 0.03);
		w.spawnParticles(ParticleTypes.FLAME, from.x, from.y, from.z, 4, 0.03, 0.03, 0.03, 0.02);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.2f, 1.4f);
	}

	// --------------------------------------------------------- backpack cannons

	public static void toggleCannons(ServerPlayerEntity p, GhostData d) {
		if (!hasCannons(p.getEquippedStack(EquipmentSlot.CHEST))) {
			GhostPowers.bar(p, "No backpack cannons installed on your suit.", Formatting.GRAY);
			return;
		}
		d.cannons = !d.cannons;
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), d.cannons ? SoundEvents.BLOCK_PISTON_EXTEND : SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.PLAYERS, 1.0f, 1.3f);
		GhostPowers.bar(p, d.cannons ? "Backpack cannons deployed! They lock onto ghosts and monsters." : "Cannons stowed.", Formatting.AQUA);
		GhostPowers.broadcastStates(p.getServer());
	}

	/** The cannon key: deploy the cannons; in manual mode, fire where you look; in auto mode, stow them again. */
	public static void cannonKey(ServerPlayerEntity p, GhostData d) {
		if (!d.suitOn) {
			GhostPowers.bar(p, "Put your Ghost Hunter Suit on first (U).", Formatting.GRAY);
			return;
		}
		if (!d.cannons) {
			toggleCannons(p, d);
			if (d.cannons) GhostPowers.bar(p, d.cannonManual ? "Cannons out (MANUAL): press your Cannon key (,) to fire where you look. Sneak + , stows them."
					: "Cannons out (AUTO): they lock onto ghosts and monsters. Press the key again to stow them.", Formatting.AQUA);
			return;
		}
		if (d.cannonManual) manualFire(p, d);
		else toggleCannons(p, d);
	}

	public static void toggleCannonMode(ServerPlayerEntity p, GhostData d) {
		d.cannonManual = !d.cannonManual;
		GhostPowers.bar(p, "Backpack cannons: " + (d.cannonManual ? "MANUAL (press the cannon key to fire)" : "AUTO (they aim and fire themselves)"), Formatting.AQUA);
		p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 0.6f, 1.6f);
	}

	private static void manualFire(ServerPlayerEntity p, GhostData d) {
		if (d.cannonCooldown > 0) return;
		int t = tier(p.getEquippedStack(EquipmentSlot.CHEST));
		d.cannonCooldown = 8 - t;
		ServerWorld w = p.getServerWorld();
		Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f), end = eye.add(look.multiply(48));
		net.minecraft.util.hit.BlockHitResult bh = w.raycast(new net.minecraft.world.RaycastContext(eye, end,
				net.minecraft.world.RaycastContext.ShapeType.COLLIDER, net.minecraft.world.RaycastContext.FluidHandling.NONE, p));
		if (bh.getType() != net.minecraft.util.hit.HitResult.Type.MISS) end = bh.getPos();
		net.minecraft.util.hit.EntityHitResult eh = net.minecraft.entity.projectile.ProjectileUtil.raycast(p, eye, end,
				new net.minecraft.util.math.Box(eye, end).expand(1.0), e -> e != p && !e.isSpectator() && e.canHit(), eye.squaredDistanceTo(end));
		if (eh != null) {
			end = eh.getPos();
			eh.getEntity().damage(w.getDamageSources().indirectMagic(p, p), (6f + t * 1.5f) * HunterSuit.ghostBonus(eh.getEntity()));
		}
		float yaw = (float) Math.toRadians(p.bodyYaw);
		for (int s = -1; s <= 1; s += 2) {
			Vec3d from = p.getPos().add(Math.cos(yaw) * 0.45 * s, 2.05, Math.sin(yaw) * 0.45 * s);
			BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) end.x, (float) end.y, (float) end.z, 0xFF55CC);
			for (ServerPlayerEntity v : PlayerLookup.around(w, from, 96)) if (ServerPlayNetworking.canSend(v, BeamPayload.ID)) ServerPlayNetworking.send(v, beam);
		}
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.8f, 1.5f);
	}

	public static void tick(ServerPlayerEntity p, GhostData d) {
		if (d.missileCooldown > 0) d.missileCooldown--;
		if (d.missileAim && (!d.suitOn || d.transformed || !hasMissiles(p.getEquippedStack(EquipmentSlot.CHEST)))) {
			d.missileAim = false;
			GhostPowers.broadcastStates(p.getServer());
		}
		if (d.cannons && (!d.suitOn || !hasCannons(p.getEquippedStack(EquipmentSlot.CHEST)))) {
			d.cannons = false;
			GhostPowers.broadcastStates(p.getServer());
		}
		if (d.cannonCooldown > 0 && d.cannonManual) d.cannonCooldown--;
		if (!d.cannons || d.cannonManual || --d.cannonCooldown > 0) return;
		int t = tier(p.getEquippedStack(EquipmentSlot.CHEST));
		d.cannonCooldown = 26 - t * 4;
		ServerWorld w = p.getServerWorld();
		LivingEntity target = w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(24),
						e -> e != p && e.isAlive() && (e instanceof AbstractGhostEntity && !(e instanceof com.phantompowers.entity.FriendlyGhostEntity)
								&& !(e instanceof com.phantompowers.entity.GhostCloneEntity) || e instanceof Monster) && p.canSee(e))
				.stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(p))).orElse(null);
		if (target == null) return;
		float yaw = (float) Math.toRadians(p.bodyYaw);
		Vec3d aim = target.getPos().add(0, target.getHeight() * 0.6, 0);
		for (int s = -1; s <= 1; s += 2) {
			Vec3d from = p.getPos().add(Math.cos(yaw) * 0.45 * s, 2.05, Math.sin(yaw) * 0.45 * s);
			BeamPayload beam = new BeamPayload((float) from.x, (float) from.y, (float) from.z, (float) aim.x, (float) aim.y, (float) aim.z, 0xFF55CC);
			for (ServerPlayerEntity v : PlayerLookup.around(w, from, 96)) if (ServerPlayNetworking.canSend(v, BeamPayload.ID)) ServerPlayNetworking.send(v, beam);
		}
		target.damage(w.getDamageSources().indirectMagic(p, p), (5f + t * 1.5f) * HunterSuit.ghostBonus(target));
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.8f);
	}
}
