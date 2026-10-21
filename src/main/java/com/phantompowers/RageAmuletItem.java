package com.phantompowers;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** The Rage Amulet: a ghost artifact that feeds on anger. Right-click to draw on its rage for a burst of strength and speed. */
public class RageAmuletItem extends Item {
	public RageAmuletItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world instanceof ServerWorld w) {
			user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1));
			user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 0));
			user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 0));
			w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, user.getX(), user.getY() + 1, user.getZ(), 40, 0.5, 0.8, 0.5, 0.06);
			w.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.8f, 1.6f);
			user.sendMessage(Text.literal("The amulet fills you with rage!").formatted(Formatting.RED), true);
			user.getItemCooldownManager().set(this, 20 * 60);
		}
		return TypedActionResult.success(stack, world.isClient);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("A ghost artifact that feeds on anger").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Right-click: rage boost (strength, speed, resistance)").formatted(Formatting.DARK_GRAY));
		tooltip.add(Text.literal("Cooldown: 60s").formatted(Formatting.DARK_GRAY));
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}
}
