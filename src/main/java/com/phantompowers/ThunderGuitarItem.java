package com.phantompowers;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;

/**
 * Voltage's lightning guitar. Swing it like a weapon, or play it:
 * right-click for a Power Chord (a blast of sound in front of you),
 * sneak + right-click for a Lightning Riff (lightning strikes the nearest hostile mobs).
 */
public class ThunderGuitarItem extends Item {
	public ThunderGuitarItem(Settings settings) {
		super(settings);
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.damage(1, attacker, EquipmentSlot.MAINHAND);
		if (attacker.getWorld() instanceof ServerWorld w) {
			w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getHeight() / 2, target.getZ(), 10, 0.3, 0.4, 0.3, 0.2);
			strum(w, attacker, 1.0f);
		}
		return true;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack, true);
		ServerWorld w = (ServerWorld) world;
		if (user.isSneaking()) {
			List<LivingEntity> foes = w.getEntitiesByClass(LivingEntity.class, user.getBoundingBox().expand(18),
					e -> e instanceof Monster && e.isAlive());
			foes.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(user)));
			int n = 0;
			for (LivingEntity e : foes) {
				if (n >= 3) break;
				LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
				if (bolt != null) {
					bolt.setCosmetic(true);
					bolt.refreshPositionAfterTeleport(e.getPos());
					w.spawnEntity(bolt);
				}
				e.damage(w.getDamageSources().indirectMagic(user, user), 10f);
				e.setOnFireFor(3);
				n++;
			}
			strum(w, user, 1.5f);
			user.sendMessage(Text.literal(n > 0 ? "Lightning Riff!" : "No foes to strike.").formatted(Formatting.YELLOW), true);
			user.getItemCooldownManager().set(this, 100);
		} else {
			Vec3d look = user.getRotationVec(1f);
			Vec3d from = user.getEyePos();
			int hit = 0;
			for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, user.getBoundingBox().expand(9), e -> e != user && e.isAlive())) {
				Vec3d d = e.getPos().add(0, e.getHeight() / 2, 0).subtract(from);
				double len = d.length();
				if (len > 9 || len < 0.1 || d.multiply(1 / len).dotProduct(look) < 0.7) continue;
				e.damage(w.getDamageSources().indirectMagic(user, user), 8f);
				e.addVelocity(look.x * 1.5, 0.4, look.z * 1.5);
				e.velocityModified = true;
				hit++;
			}
			for (int i = 1; i <= 8; i++) {
				Vec3d p = from.add(look.multiply(i));
				double r = 0.3 + i * 0.25;
				w.spawnParticles(ParticleTypes.NOTE, p.x, p.y, p.z, 2, r * 0.5, r * 0.5, r * 0.5, 1.0);
				w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 3, r * 0.4, r * 0.4, r * 0.4, 0.05);
			}
			strum(w, user, 0.8f);
			user.getItemCooldownManager().set(this, 30);
		}
		stack.damage(1, user, LivingEntity.getSlotForHand(hand));
		return TypedActionResult.success(stack);
	}

	/** A three-note power chord. */
	private static void strum(ServerWorld w, LivingEntity at, float pitch) {
		SoundEvent guitar = SoundEvents.BLOCK_NOTE_BLOCK_GUITAR.value();
		w.playSound(null, at.getX(), at.getY(), at.getZ(), guitar, SoundCategory.PLAYERS, 1.5f, pitch);
		w.playSound(null, at.getX(), at.getY(), at.getZ(), guitar, SoundCategory.PLAYERS, 1.5f, pitch * 1.26f);
		w.playSound(null, at.getX(), at.getY(), at.getZ(), guitar, SoundCategory.PLAYERS, 1.5f, pitch * 1.5f);
	}

	@Override
	public boolean canRepair(ItemStack stack, ItemStack ingredient) {
		return ingredient.isOf(ModItems.ECTOPLASM);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Right-click: Power Chord (sound blast)").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.literal("Sneak + right-click: Lightning Riff").formatted(Formatting.YELLOW));
		tooltip.add(Text.literal("Hits hard as a melee weapon too").formatted(Formatting.DARK_GRAY));
	}
}
