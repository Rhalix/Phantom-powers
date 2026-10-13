package com.phantompowers;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The Ghost Hunter Suit: wear the suit pack in your chest slot, then press U to deploy it.
 * Black armored bodysuit, white gloves and boots, and a helmet with a glowing visor.
 */
public final class HunterSuit {
	private static final Identifier ARMOR = PhantomMod.id("suit_armor");
	private static final Identifier TOUGH = PhantomMod.id("suit_toughness");
	private static final Identifier DAMAGE = PhantomMod.id("suit_damage");
	private static final Identifier SPEED = PhantomMod.id("suit_speed");
	private static final Identifier KNOCK = PhantomMod.id("suit_knockback");
	private static final Identifier HEALTH = PhantomMod.id("suit_health");

	private HunterSuit() {}

	/** Ghost-hunting gear is built to hurt ghosts: extra damage against ghosts, ghost bosses and half-ghosts in ghost form. */
	public static float ghostBonus(net.minecraft.entity.Entity target) {
		if (target instanceof com.phantompowers.entity.AbstractGhostEntity || FrostLogic.isBoss(target)
				|| target instanceof net.minecraft.entity.mob.VexEntity || target instanceof net.minecraft.entity.mob.PhantomEntity) return 1.8f;
		if (target instanceof ServerPlayerEntity sp && GhostState.of(sp.getServer()).data(sp.getUuid()).transformed) return 1.7f;
		return 1f;
	}

	public static boolean wearing(ServerPlayerEntity p) {
		return p.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.HUNTER_SUIT);
	}

	public static boolean canDeploy(ServerPlayerEntity p, GhostData d) {
		if (!wearing(p)) {
			GhostPowers.bar(p, "Put the Ghost Hunter Suit in your chest armor slot first.", Formatting.YELLOW);
			return false;
		}
		if (d.transformed) {
			GhostPowers.bar(p, "You can't suit up while you're in ghost form!", Formatting.RED);
			return false;
		}
		return true;
	}

	public static void toggle(ServerPlayerEntity p, GhostData d, boolean wrapEffect) {
		ServerWorld w = p.getServerWorld();
		if (d.suitOn) {
			d.suitOn = false;
			apply(p, false);
			if (wrapEffect) GhostPowers.sendRings(p, 18, 5);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.PLAYERS, 0.8f, 1.4f);
			GhostPowers.bar(p, "Suit stowed.", Formatting.GRAY);
		} else {
			if (!canDeploy(p, d)) return;
			d.suitOn = true;
			apply(p, true);
			if (wrapEffect) GhostPowers.sendRings(p, 24, 1);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.PLAYERS, 0.8f, 1.6f);
			GhostPowers.bar(p, "Ghost Hunter Suit online.", Formatting.AQUA);
		}
		GhostPowers.broadcastStates(p.getServer());
		d.syncTimer = 0;
	}

	private static void apply(ServerPlayerEntity p, boolean on) {
		// The pack counts as a netherite chestplate; deployed, the whole suit adds up to
		// a full set of netherite armor (20 armor, 12 toughness, 0.4 knockback resistance).
		GhostPowers.setMod(p, EntityAttributes.GENERIC_ARMOR, ARMOR, on ? 12.0 : 0);
		GhostPowers.setMod(p, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, TOUGH, on ? 9.0 : 0);
		GhostPowers.setMod(p, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCK, on ? 0.6 : 0);
		// Reinforced plating: 5 extra hearts
		GhostPowers.setMod(p, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH, on ? 10.0 : 0);
		if (!on && p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
		GhostPowers.setMod(p, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE, on ? 2.0 : 0);
		GhostPowers.setMod(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED, on ? 0.015 : 0);
		if (!on) {
			p.removeStatusEffect(StatusEffects.NIGHT_VISION);
			StatusEffectInstance res = p.getStatusEffect(StatusEffects.RESISTANCE);
			if (res != null && res.isAmbient() && res.getDuration() <= 60) p.removeStatusEffect(StatusEffects.RESISTANCE);
		}
	}

	/**
	 * The suit's real protection: ghost attacks are cut by more than half, everything else by a third,
	 * and no single hit can take more than a third of your health (so nothing one-shots you).
	 */
	public static float reduceDamage(ServerPlayerEntity p, net.minecraft.entity.damage.DamageSource source, float amount) {
		if (amount <= 0 || source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
		GhostData d = GhostState.of(p.getServer()).data(p.getUuid());
		if (!d.suitOn) return amount;
		net.minecraft.entity.Entity attacker = source.getAttacker();
		boolean ghost = attacker instanceof com.phantompowers.entity.AbstractGhostEntity
				|| (attacker instanceof ServerPlayerEntity ap && GhostState.of(p.getServer()).data(ap.getUuid()).transformed);
		float out = amount * (ghost ? 0.45f : 0.65f);
		return Math.min(out, p.getMaxHealth() * 0.34f);
	}

	public static void tick(ServerPlayerEntity p, GhostData d) {
		if (!d.suitOn) return;
		if (!wearing(p) || d.transformed) {
			toggle(p, d, false);
			return;
		}
		if (p.age % 40 == 0) {
			// The visor lets you see in the dark
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 260, 0, true, false, false));
			// Shock-absorbing armor
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0, true, false, true));
		}
	}
}
