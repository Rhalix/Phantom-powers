package com.phantompowers;

import com.phantompowers.entity.BossMoves;
import com.phantompowers.entity.KissEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/** Roxy's Heart Charm (dropped by Roxy): right-click blows a homing kiss, sneak + right-click lets out her sonic shriek. */
public class HeartCharmItem extends Item {
	public HeartCharmItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack, true);
		ServerPlayerEntity p = (ServerPlayerEntity) user;
		ServerWorld w = p.getServerWorld();
		Vec3d look = p.getRotationVec(1f);
		if (p.isSneaking()) {
			Vec3d from = p.getEyePos();
			BossMoves.soundWave(w, from.add(look.multiply(0.6)), look, 14f, 0xFF66CC);
			BossMoves.coneHit(w, p, from, look, 14, 0.8, w.getDamageSources().sonicBoom(p), 8f, 1.3, e -> e != p);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.PLAYERS, 1.6f, 1.6f);
			p.getItemCooldownManager().set(this, 70);
			stack.damage(3, p, LivingEntity.getSlotForHand(hand));
			return TypedActionResult.success(stack);
		}
		KissEntity kiss = new KissEntity(w, p, false);
		kiss.setFromPlayer();
		Vec3d from = p.getEyePos().add(look.multiply(0.6)).add(0, -0.15, 0);
		kiss.setPosition(from.x, from.y, from.z);
		kiss.setVelocity(look.x, look.y, look.z, 0.9f, 0f);
		Vec3d end = p.getEyePos().add(look.multiply(32));
		EntityHitResult aimed = ProjectileUtil.raycast(p, p.getEyePos(), end, new Box(p.getEyePos(), end).expand(1.5),
				e -> e instanceof LivingEntity && e != p && !e.isSpectator(), 32 * 32);
		if (aimed != null && aimed.getEntity() instanceof LivingEntity le) kiss.setHoming(le);
		w.spawnEntity(kiss);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.KISS_SMOOCH, SoundCategory.PLAYERS, 1.2f, 1.05f);
		p.getItemCooldownManager().set(this, 14);
		stack.damage(1, p, LivingEntity.getSlotForHand(hand));
		return TypedActionResult.success(stack);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Right-click: blow a homing kiss").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.literal("Sneak + right-click: sonic shriek").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.literal("Kisses leave enemies lovestruck (slow and weak)").formatted(Formatting.DARK_GRAY));
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}
}
