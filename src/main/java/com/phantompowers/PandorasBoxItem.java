package com.phantompowers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
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

/** The Crate King's prize. Crack the lid to unleash a dark shockwave on every hostile mob and other player around you. */
public class PandorasBoxItem extends Item {
	private static final DustParticleEffect DARK = new DustParticleEffect(new Vector3f(0.6f, 0.15f, 0.9f), 1.5f);

	public PandorasBoxItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack, true);
		ServerWorld w = (ServerWorld) world;
		int hit = 0;
		for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, user.getBoundingBox().expand(10), e -> e != user && e.isAlive() && (e instanceof Monster || (e instanceof net.minecraft.entity.player.PlayerEntity pl && !pl.isCreative() && !pl.isSpectator())))) {
			e.damage(w.getDamageSources().indirectMagic(user, user), 10f);
			Vec3d out = e.getPos().subtract(user.getPos()).normalize();
			e.addVelocity(out.x * 1.4, 0.5, out.z * 1.4);
			e.velocityModified = true;
			hit++;
		}
		for (int i = 0; i < 3; i++) {
			double r = 2 + i * 3;
			for (int k = 0; k < 40; k++) {
				double a = k * Math.PI * 2 / 40;
				w.spawnParticles(DARK, user.getX() + Math.cos(a) * r, user.getY() + 0.5, user.getZ() + Math.sin(a) * r, 1, 0, 0.1, 0, 0);
			}
		}
		w.spawnParticles(ParticleTypes.WITCH, user.getX(), user.getY() + 1.5, user.getZ(), 40, 0.4, 0.6, 0.4, 0.1);
		w.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.2f, 0.5f);
		w.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.PLAYERS, 0.6f, 0.5f);
		user.sendMessage(Text.literal("The box creaks open... " + hit + " foe" + (hit == 1 ? "" : "s") + " blasted!").formatted(Formatting.DARK_PURPLE), true);
		user.getItemCooldownManager().set(this, 400);
		return TypedActionResult.success(stack);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Right-click: dark shockwave hits all hostile mobs and other players nearby").formatted(Formatting.DARK_PURPLE));
		tooltip.add(Text.literal("20 second recharge").formatted(Formatting.DARK_GRAY));
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}
}
