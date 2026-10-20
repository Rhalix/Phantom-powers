package com.phantompowers;

import com.phantompowers.entity.PinkOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/** The Ecto Launcher: a shoulder-mounted ghost-hunting cannon that fires bursting balls of pink energy. */
public class EctoLauncherItem extends Item {
	public EctoLauncherItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack, true);
		ServerPlayerEntity p = (ServerPlayerEntity) user;
		boolean suited = GhostState.of(p.getServer()).data(p.getUuid()).suitOn;
		PinkOrbEntity orb = new PinkOrbEntity(world, p);
		Vec3d look = p.getRotationVec(1f);
		Vec3d muzzle = p.getEyePos().add(look.multiply(1.0)).add(0, -0.15, 0);
		orb.setPosition(muzzle.x, muzzle.y, muzzle.z);
		orb.boost = HunterPaint.weaponBoost(stack);
		orb.setVelocity(look.x, look.y, look.z, suited ? 2.0f : 1.6f, 0.4f);
		world.spawnEntity(orb);
		// recoil
		p.addVelocity(-look.x * 0.35, 0.05, -look.z * 0.35);
		p.velocityModified = true;
		world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.4f, 0.6f);
		world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.7f, 1.7f);
		p.getItemCooldownManager().set(this, suited ? 22 : 34);
		stack.damage(1, p, LivingEntity.getSlotForHand(hand));
		return TypedActionResult.success(stack);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Fires bursting balls of pink energy").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.literal("Extra damage to ghosts and half-ghosts").formatted(Formatting.DARK_GRAY));
	}
}
