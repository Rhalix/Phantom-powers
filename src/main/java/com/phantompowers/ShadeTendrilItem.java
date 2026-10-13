package com.phantompowers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

/**
 * The Shade's Tendril (dropped by the Shade boss): a living strip of shadow.
 * Right-click a creature to yank it to you and blind it; sneak + right-click a block to whip yourself over to it.
 */
public class ShadeTendrilItem extends Item {
	public ShadeTendrilItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack, true);
		ServerPlayerEntity p = (ServerPlayerEntity) user;
		ServerWorld w = p.getServerWorld();
		Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f), end = eye.add(look.multiply(24));
		BlockHitResult bh = w.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
		if (bh.getType() != HitResult.Type.MISS) end = bh.getPos();
		Vec3d target;
		if (p.isSneaking()) {
			if (bh.getType() == HitResult.Type.MISS) return TypedActionResult.fail(stack);
			// grapple: fling yourself toward the spot
			target = end;
			Vec3d pull = end.subtract(p.getPos()).normalize();
			double dist = end.distanceTo(p.getPos());
			p.addVelocity(pull.x * Math.min(2.2, 0.5 + dist * 0.12), Math.max(0.4, pull.y * 1.2 + 0.35), pull.z * Math.min(2.2, 0.5 + dist * 0.12));
			p.velocityModified = true;
			p.fallDistance = 0;
		} else {
			EntityHitResult eh = ProjectileUtil.raycast(p, eye, end, new Box(eye, end).expand(1.5),
					e -> e instanceof LivingEntity && e != p && !e.isSpectator(), eye.squaredDistanceTo(end));
			if (eh == null || !(eh.getEntity() instanceof LivingEntity le)) return TypedActionResult.fail(stack);
			target = le.getPos().add(0, le.getHeight() / 2, 0);
			Vec3d pull = p.getPos().subtract(le.getPos()).normalize();
			le.addVelocity(pull.x * 1.5, 0.5, pull.z * 1.5);
			le.velocityModified = true;
			le.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0), p);
			le.damage(w.getDamageSources().indirectMagic(p, p), 3f * HunterSuit.ghostBonus(le));
		}
		for (int i = 0; i <= 20; i++) {
			Vec3d at = eye.add(0, -0.3, 0).lerp(target, i / 20.0);
			w.spawnParticles(ParticleTypes.SQUID_INK, at.x, at.y, at.z, 2, 0.04, 0.04, 0.04, 0.01);
		}
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.PLAYERS, 1.5f, 1.2f);
		p.getItemCooldownManager().set(this, 30);
		stack.damage(1, p, LivingEntity.getSlotForHand(hand));
		return TypedActionResult.success(stack);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Right-click a creature: yank it to you and blind it").formatted(Formatting.DARK_PURPLE));
		tooltip.add(Text.literal("Sneak + right-click a block: whip yourself over to it").formatted(Formatting.DARK_PURPLE));
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}
}
