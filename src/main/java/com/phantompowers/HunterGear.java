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
				GhostPowers.bar(p, "Backpack cannons installed! With the suit on, press your Wail key (B) to deploy them.", Formatting.AQUA);
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

	public static void fireMissiles(ServerPlayerEntity p, GhostData d) {
		ItemStack suit = p.getEquippedStack(EquipmentSlot.CHEST);
		if (!hasMissiles(suit)) {
			GhostPowers.bar(p, "No wrist missiles installed on your suit.", Formatting.GRAY);
			return;
		}
		if (d.missileCooldown > 0) return;
		d.missileCooldown = 36 - tier(suit) * 6;
		ServerWorld w = p.getServerWorld();
		var target = GhostPowers.aimEntity(p, d, 40);
		Vec3d look = p.getRotationVec(1f);
		Vec3d right = look.crossProduct(new Vec3d(0, 1, 0));
		right = right.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : right.normalize();
		for (int s = -1; s <= 1; s += 2) {
			MissileEntity m = new MissileEntity(w, p);
			Vec3d from = p.getEyePos().add(look.multiply(0.6)).add(right.multiply(0.4 * s)).add(0, -0.45, 0);
			m.setPosition(from.x, from.y, from.z);
			m.setVelocity(look.x + right.x * 0.12 * s, look.y + 0.05, look.z + right.z * 0.12 * s, 1.3f, 0f);
			if (target instanceof LivingEntity le) m.setTarget(le);
			m.setPower(7f + tier(suit) * 2f);
			w.spawnEntity(m);
		}
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

	public static void tick(ServerPlayerEntity p, GhostData d) {
		if (d.missileCooldown > 0) d.missileCooldown--;
		if (d.cannons && (!d.suitOn || !hasCannons(p.getEquippedStack(EquipmentSlot.CHEST)))) {
			d.cannons = false;
			GhostPowers.broadcastStates(p.getServer());
		}
		if (!d.cannons || --d.cannonCooldown > 0) return;
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
