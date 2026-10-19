package com.phantompowers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.List;

/**
 * Dolora's Sorrow Locket: open it and it drinks the fear of every hostile creature around you, weakening them
 * and turning their dread into healing for you.
 */
public class SorrowLocketItem extends Item {
	private static final DustParticleEffect VIOLET = new DustParticleEffect(new Vector3f(0.65f, 0.4f, 1f), 1.3f);

	public SorrowLocketItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (!(world instanceof ServerWorld w)) return TypedActionResult.success(stack, true);
		List<LivingEntity> near = w.getEntitiesByClass(LivingEntity.class, user.getBoundingBox().expand(8),
				e -> e != user && e.isAlive() && (e instanceof Monster || com.phantompowers.FrostLogic.isBoss(e)));
		float healed = 0;
		Vec3d me = user.getPos().add(0, 1, 0);
		for (LivingEntity e : near) {
			e.damage(w.getDamageSources().indirectMagic(user, user), 4f);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 140, 1), user);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 0), user);
			Vec3d from = e.getPos().add(0, e.getHeight() * 0.6, 0);
			for (int i = 0; i <= 10; i++) {
				Vec3d p = from.lerp(me, i / 10.0);
				w.spawnParticles(VIOLET, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0);
			}
			healed += 2f;
		}
		user.heal(Math.min(10f, healed));
		w.spawnParticles(VIOLET, user.getX(), user.getY() + 1, user.getZ(), 40, 1.5, 0.8, 1.5, 0.02);
		w.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.5f, 1.2f);
		w.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.5f, 0.7f);
		user.getItemCooldownManager().set(this, 200);
		stack.damage(1, user, LivingEntity.getSlotForHand(hand));
		if (near.isEmpty()) user.sendMessage(Text.literal("The locket finds no fear to drink here...").formatted(Formatting.LIGHT_PURPLE), true);
		return TypedActionResult.success(stack);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Right-click: drink the fear of nearby monsters").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.literal("Weakens them and heals you").formatted(Formatting.DARK_GRAY));
	}
}
